package it.ispw.tutora.boundary;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.ispw.tutora.bean.SocialLoginBean;
import it.ispw.tutora.exception.OAuthException;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

/**
 * Base condivisa tra {@link GoogleAuthBoundary} e {@link MetaAuthBoundary}.
 *
 * Fornisce {@code httpClient}, {@code objectMapper} e il metodo {@code enc()};
 * delegando alle sottoclassi lo scambio del codice, il recupero del profilo
 * e la costruzione dell'URL di autorizzazione.
 */
public abstract class AbstractOAuthBoundary {

    protected final HttpClient   httpClient;
    protected final ObjectMapper objectMapper;

    protected AbstractOAuthBoundary() {
        this.httpClient   = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public abstract String getAuthorizationUrl(String state);

    public SocialLoginBean fetchUserProfile(String code) throws OAuthException {
        String accessToken = exchangeCodeForToken(code);
        return fetchProfile(accessToken);
    }

    protected abstract String         exchangeCodeForToken(String code)        throws OAuthException;
    protected abstract SocialLoginBean fetchProfile(String accessToken)         throws OAuthException;

    protected static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
