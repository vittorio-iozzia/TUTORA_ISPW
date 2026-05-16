package it.ispw.tutora.boundary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.ispw.tutora.bean.SocialLoginBean;
import it.ispw.tutora.exception.OAuthException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Boundary per Meta (Facebook) OAuth2 (Authorization Code Flow).
 *
 * -----------------------------------------------------------------------
 * Pattern Adapter (GoF – Strutturale)
 * -----------------------------------------------------------------------
 * Adatta le API HTTP di Meta Graph API all'interfaccia attesa dal
 * SocialLoginController, che riceve un SocialLoginBean senza
 * conoscere alcun dettaglio del protocollo OAuth o dell'API Meta.
 *
 * -----------------------------------------------------------------------
 * Flusso
 * -----------------------------------------------------------------------
 * 1. getAuthorizationUrl()  → URL da aprire nel browser
 * 2. fetchUserProfile(code) → scambia il codice per un token,
 *                             poi recupera nome/email dalla Graph API
 *
 * -----------------------------------------------------------------------
 * Configurazione
 * -----------------------------------------------------------------------
 * App ID e App Secret si ottengono da Meta for Developers:
 *   https://developers.facebook.com/apps
 * Nella sezione "Facebook Login" → "Settings":
 *   - Aggiungi http://localhost:8888/callback come Valid OAuth Redirect URI
 *   - Permessi richiesti: email, public_profile
 */
public class MetaAuthBoundary {

    private static final Logger LOGGER = Logger.getLogger(MetaAuthBoundary.class.getName());

    private static final String GRAPH_VERSION = "v18.0";
    private static final String AUTH_URL      =
            "https://www.facebook.com/" + GRAPH_VERSION + "/dialog/oauth";
    private static final String TOKEN_URL     =
            "https://graph.facebook.com/" + GRAPH_VERSION + "/oauth/access_token";
    private static final String USER_INFO_URL =
            "https://graph.facebook.com/me?fields=id,email,first_name,last_name";
    private static final String PROVIDER      = "meta";

    private final String appId;
    private final String appSecret;
    private final String redirectUri;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MetaAuthBoundary(String appId, String appSecret, String redirectUri) {
        this.appId       = appId;
        this.appSecret   = appSecret;
        this.redirectUri = redirectUri;
        this.httpClient  = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    // ----------------------------------------------------------------
    // Step 1 – URL di autorizzazione
    // ----------------------------------------------------------------

    /**
     * Costruisce l'URL di autorizzazione Meta da aprire nel browser dell'utente.
     *
     * @param state stringa casuale per protezione CSRF
     */
    public String getAuthorizationUrl(String state) {
        return AUTH_URL
                + "?client_id="    + enc(appId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&response_type=code"
                + "&scope="        + enc("email,public_profile")
                + "&state="        + enc(state);
    }

    // ----------------------------------------------------------------
    // Step 2 – scambio codice → token → profilo
    // ----------------------------------------------------------------

    /**
     * Scambia il codice di autorizzazione per un access token,
     * poi recupera il profilo utente dalla Meta Graph API.
     *
     * @param code codice restituito dal callback OAuth
     * @return bean con provider="meta", oauthId, email, name, surname
     */
    public SocialLoginBean fetchUserProfile(String code) throws OAuthException {
        String accessToken = exchangeCodeForToken(code);
        return fetchProfile(accessToken);
    }

    // ----------------------------------------------------------------
    // Metodi privati
    // ----------------------------------------------------------------

    private String exchangeCodeForToken(String code) throws OAuthException {
        String url = TOKEN_URL
                + "?client_id="     + enc(appId)
                + "&redirect_uri="  + enc(redirectUri)
                + "&client_secret=" + enc(appSecret)
                + "&code="          + enc(code);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new OAuthException(
                        "Meta token exchange fallito (HTTP " + response.statusCode() + "): "
                        + response.body());
            }

            JsonNode json      = objectMapper.readTree(response.body());
            JsonNode tokenNode = json.get("access_token");
            if (tokenNode == null || tokenNode.asText().isBlank()) {
                throw new OAuthException("Nessun access_token nella risposta Meta.");
            }
            return tokenNode.asText();

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new OAuthException("Errore durante il token exchange Meta.", e);
        }
    }

    private SocialLoginBean fetchProfile(String accessToken) throws OAuthException {
        String url = USER_INFO_URL + "&access_token=" + enc(accessToken);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new OAuthException(
                        "Meta user info fallito (HTTP " + response.statusCode() + ").");
            }

            JsonNode json  = objectMapper.readTree(response.body());
            String oauthId = json.path("id").asText();
            String email   = json.path("email").asText();
            String name    = json.path("first_name").asText("Unknown");
            String surname = json.path("last_name").asText("");

            if (email.isBlank()) {
                throw new OAuthException(
                        "Meta non ha restituito un'email. " +
                        "Verifica che il permesso 'email' sia abilitato nell'app Meta.");
            }

            LOGGER.info("Meta OAuth: profilo ricevuto per " + email);
            return new SocialLoginBean(PROVIDER, oauthId, email, name, surname);

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new OAuthException("Errore nel recupero del profilo Meta.", e);
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
