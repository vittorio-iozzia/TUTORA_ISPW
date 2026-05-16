package it.ispw.tutora.boundary;

import com.fasterxml.jackson.databind.JsonNode;
import it.ispw.tutora.bean.SocialLoginBean;
import it.ispw.tutora.exception.OAuthException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Boundary per Google OAuth2 (Authorization Code Flow).
 *
 * -----------------------------------------------------------------------
 * Pattern Adapter (GoF – Strutturale)
 * -----------------------------------------------------------------------
 * Adatta le API HTTP di Google OAuth2 all'interfaccia attesa dal
 * SocialLoginController, che riceve un SocialLoginBean senza
 * conoscere alcun dettaglio del protocollo OAuth o dell'API Google.
 *
 * -----------------------------------------------------------------------
 * Flusso
 * -----------------------------------------------------------------------
 * 1. getAuthorizationUrl()  → URL da aprire nel browser
 * 2. fetchUserProfile(code) → scambia il codice per un token,
 *                             poi recupera nome/email dall'API Google
 *
 * -----------------------------------------------------------------------
 * Configurazione
 * -----------------------------------------------------------------------
 * Client ID e Client Secret si ottengono dalla Google Cloud Console:
 *   https://console.cloud.google.com/apis/credentials
 * Tipo applicazione: "Desktop app"
 * Redirect URI autorizzato: http://localhost:8888/callback
 */
public class GoogleAuthBoundary extends AbstractOAuthBoundary {

    private static final Logger LOGGER = Logger.getLogger(GoogleAuthBoundary.class.getName());

    private static final String AUTH_URL      = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL     = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String PROVIDER      = "google";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GoogleAuthBoundary(String clientId, String clientSecret, String redirectUri) {
        super();
        this.clientId     = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri  = redirectUri;
    }

    @Override
    public String getAuthorizationUrl(String state) {
        return AUTH_URL
                + "?client_id="     + enc(clientId)
                + "&redirect_uri="  + enc(redirectUri)
                + "&response_type=code"
                + "&scope="         + enc("openid email profile")
                + "&state="         + enc(state)
                + "&access_type=offline"
                + "&prompt=select_account";
    }

    @Override
    protected String exchangeCodeForToken(String code) throws OAuthException {
        String body = "grant_type=authorization_code"
                + "&code="          + enc(code)
                + "&redirect_uri="  + enc(redirectUri)
                + "&client_id="     + enc(clientId)
                + "&client_secret=" + enc(clientSecret);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new OAuthException(
                        "Google token exchange fallito (HTTP " + response.statusCode() + "): "
                        + response.body());
            }

            JsonNode json      = objectMapper.readTree(response.body());
            JsonNode tokenNode = json.get("access_token");
            if (tokenNode == null || tokenNode.asText().isBlank()) {
                throw new OAuthException("Nessun access_token nella risposta Google.");
            }
            return tokenNode.asText();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OAuthException("Errore durante il token exchange Google.", e);
        } catch (IOException e) {
            throw new OAuthException("Errore durante il token exchange Google.", e);
        }
    }

    @Override
    protected SocialLoginBean fetchProfile(String accessToken) throws OAuthException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USER_INFO_URL))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new OAuthException(
                        "Google user info fallito (HTTP " + response.statusCode() + ").");
            }

            JsonNode json  = objectMapper.readTree(response.body());
            String oauthId = json.path("id").asText();
            String email   = json.path("email").asText();
            String name    = json.path("given_name").asText("Unknown");
            String surname = json.path("family_name").asText("");

            if (email.isBlank()) {
                throw new OAuthException(
                        "Google non ha restituito un'email. Verifica i permessi OAuth.");
            }

            LOGGER.log(Level.INFO, "Google OAuth: profilo ricevuto per {0}", email);
            return new SocialLoginBean(PROVIDER, oauthId, email, name, surname);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OAuthException("Errore nel recupero del profilo Google.", e);
        } catch (IOException e) {
            throw new OAuthException("Errore nel recupero del profilo Google.", e);
        }
    }
}
