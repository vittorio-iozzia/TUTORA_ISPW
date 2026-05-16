package it.ispw.tutora.boundary;

import it.ispw.tutora.exception.OAuthException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Minimal HTTP server per ricevere il callback OAuth2.
 *
 * -----------------------------------------------------------------------
 * Flusso
 * -----------------------------------------------------------------------
 * 1. Il browser dell'utente viene reindirizzato a http://localhost:PORT/callback?code=...
 * 2. Questo server accetta la connessione, estrae il codice e risponde
 *    con una pagina HTML di conferma.
 * 3. Dopo aver catturato il codice il server si chiude automaticamente.
 *
 * -----------------------------------------------------------------------
 * Timeout
 * -----------------------------------------------------------------------
 * Se l'utente non completa il login entro TIMEOUT_SECONDS, il socket
 * scade e viene lanciata OAuthException.
 */
public class OAuthCallbackServer {

    private static final Logger LOGGER = Logger.getLogger(OAuthCallbackServer.class.getName());
    private static final int TIMEOUT_SECONDS = 300;

    private static final String HTML_SUCCESS =
            "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
            "<title>TUTORA – Login</title></head>" +
            "<body style='font-family:sans-serif;text-align:center;padding:60px;background:#f0f4f0;'>" +
            "<h2 style='color:#2e7d32;'>&#10003; Login succeeded!</h2>" +
            "<p>You may close this tab and return to TUTORA.</p>" +
            "</body></html>";

    private static final String HTML_ERROR =
            "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
            "<title>TUTORA – Error</title></head>" +
            "<body style='font-family:sans-serif;text-align:center;padding:60px;background:#fdf0f0;'>" +
            "<h2 style='color:#c62828;'>Login failed. Please try again from the app.</h2>" +
            "</body></html>";

    private final int port;

    public OAuthCallbackServer(int port) {
        this.port = port;
    }

    /**
     * Avvia il server, attende il callback OAuth e restituisce il codice di autorizzazione.
     */
    public String waitForCode() throws OAuthException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));

            try (Socket socket = serverSocket.accept()) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                // Prima riga HTTP: GET /callback?code=...&state=... HTTP/1.1
                String requestLine = reader.readLine();
                if (requestLine == null || requestLine.isBlank()) {
                    throw new OAuthException("Empty OAuth callback.");
                }
                LOGGER.fine("OAuth callback request: %s".formatted(requestLine));

                String code  = extractParam(requestLine, "code");
                String error = extractParam(requestLine, "error");

                sendHtml(socket, code != null ? HTML_SUCCESS : HTML_ERROR);

                if (error != null) {
                    throw new OAuthException("The OAuth provider returned an error: " + error);
                }
                if (code == null) {
                    throw new OAuthException("No authorization code in the OAuth callback.");
                }
                return code;
            }

        } catch (OAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new OAuthException("Error in OAuth callback server.", e);
        }
    }

    // ----------------------------------------------------------------
    // Helper privati
    // ----------------------------------------------------------------

    private String extractParam(String requestLine, String param) {
        // requestLine: GET /callback?key=value&key2=value2 HTTP/1.1
        int queryStart = requestLine.indexOf('?');
        int pathEnd    = requestLine.lastIndexOf(' ');
        if (queryStart < 0 || pathEnd <= queryStart) return null;

        String query = requestLine.substring(queryStart + 1, pathEnd);
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            if (key.equals(param)) {
                return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private void sendHtml(Socket socket, String html) {
        String response = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/html; charset=UTF-8\r\n"
                + "Connection: close\r\n\r\n"
                + html;
        try (OutputStream out = socket.getOutputStream()) {
            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (Exception e) {
            LOGGER.warning("Cannot send HTML response to browser: " + e.getMessage());
        }
    }
}
