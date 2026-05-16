package it.ispw.tutora.model.session;

import it.ispw.tutora.model.User;

import java.util.Map;
import java.util.Set;
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
    private final Set<String> newlyPromotedTutors = ConcurrentHashMap.newKeySet();

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

    /**
     * Segna un utente come neotutor per mostrare il popup di benvenuto
     * al primo accesso dopo la promozione.
     */
    public void markAsNewlyPromotedTutor(String username) {
        newlyPromotedTutors.add(username);
    }

    /**
     * Controlla e consuma il flag di neotutor (one-shot).
     * Restituisce true solo la prima volta dopo la promozione.
     */
    public boolean consumeNewlyPromotedTutor(String username) {
        return newlyPromotedTutors.remove(username);
    }

    /**
     * Invalida tutte le sessioni attive per un utente specifico.
     * Chiamato quando un utente viene promosso a un nuovo ruolo,
     * forzando il re-login per aggiornare la sessione.
     */
    public void invalidateSessionsForUser(String username) {
        activeSessions.entrySet().removeIf(
                e -> e.getValue().getUser().getUsername().equals(username));
    }
}