package it.ispw.tutora.model.session;

import it.ispw.tutora.model.User;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestore centralizzato delle sessioni utente attive.
 *
 * -----------------------------------------------------------------------
 * Pattern Singleton – Bill Pugh (Initialization-on-demand Holder)
 * -----------------------------------------------------------------------
 * Thread-safe senza synchronized — garantito dal class-loading della JVM.
 *
 * -----------------------------------------------------------------------
 * Thread-safety
 * -----------------------------------------------------------------------
 * Le sessioni sono indicizzate per token in una ConcurrentHashMap
 * che gestisce internamente la concorrenza — nessun synchronized
 * aggiuntivo necessario sui metodi pubblici.
 *
 * -----------------------------------------------------------------------
 * Token
 * -----------------------------------------------------------------------
 * Ogni sessione ha un token UUID generato casualmente.
 * Usato per identificare la sessione corrente e per l'integrazione
 * con provider OAuth2 (Google, Meta).
 */
public class SessionManager {

    private final Map<String, Session> activeSessions = new ConcurrentHashMap<>();

    // ----------------------------------------------------------------
    // Bill Pugh Holder
    // ----------------------------------------------------------------

    private SessionManager() {}

    private static class Holder {
        private static final SessionManager INSTANCE = new SessionManager();
    }

    public static SessionManager getInstance() {
        return Holder.INSTANCE;
    }

    // ----------------------------------------------------------------
    // Gestione sessioni
    // ----------------------------------------------------------------

    /**
     * Crea una nuova sessione per l'utente specificato.
     */
    public String createSession(User user) {
        String token = UUID.randomUUID().toString();
        activeSessions.put(token, new Session(user, token));
        return token;
    }

    /**
     * Recupera la sessione associata al token.
     */
    public Session getSession(String token) {
        return activeSessions.get(token);
    }

    /**
     * Restituisce l'utente associato al token.
     */
    public User getCurrentUser(String token) {
        Session session = activeSessions.get(token);
        return session != null ? session.getUser() : null;
    }

    /**
     * Verifica se il token corrisponde a una sessione attiva.
     */
    public boolean isSessionValid(String token) {
        return token != null && activeSessions.containsKey(token);
    }

    /**
     * Invalida e rimuove la sessione associata al token.
     * Chiamato al logout.
     */
    public void invalidateSession(String token) {
        activeSessions.remove(token);
    }

    /**
     * Invalida tutte le sessioni attive.
     * Utile per il reset completo in fase di test.
     */
    public void invalidateAllSessions() {
        activeSessions.clear();
    }
}