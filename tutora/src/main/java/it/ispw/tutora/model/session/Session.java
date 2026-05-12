package it.ispw.tutora.model.session;

import it.ispw.tutora.model.Admin;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.User;

import java.time.LocalDateTime;

/**
 * Rappresenta una sessione utente attiva.
 *
 * Una sessione viene creata al momento del login e mantiene il riferimento
 * all'utente autenticato, il token univoco di identificazione e il timestamp
 * di accesso. Tutti i campi sono immutabili dopo la costruzione.
 *
 * Il costruttore è package-private per limitare la creazione di istanze
 * al solo SessionManager, che ha visibilità di package.
 *
 * -----------------------------------------------------------------------
 * Token
 * -----------------------------------------------------------------------
 * Il token UUID identifica univocamente la sessione.
 * Viene usato dal GfxController per recuperare la sessione corrente
 * tramite SessionManager.getSession(token) e per l'integrazione
 * con provider OAuth2 (Google, Meta).
 */
public class Session {

    private final User user;
    private final String token;
    private final LocalDateTime loginTime;

    // ----------------------------------------------------------------
    // Costruttore package-private
    // ----------------------------------------------------------------

    /**
     * Crea una nuova sessione per l'utente specificato.
     * Visibilità package-private: solo SessionManager può creare sessioni.
     */
    Session(User user, String token) {
        this.user = user;
        this.token = token;
        this.loginTime = LocalDateTime.now();
    }

    // ----------------------------------------------------------------
    // Getter
    // ----------------------------------------------------------------

    public User getUser() { return user; }
    public String getToken() { return token; }
    public LocalDateTime getLoginTime() { return loginTime; }

    // ----------------------------------------------------------------
    // Utility — verifica ruolo con instanceof
    // ----------------------------------------------------------------

    /**
     * Verifica il ruolo tramite instanceof — più robusto del confronto
     * con l'enum Role perché garantisce coerenza con il tipo effettivo
     * dell'oggetto User.
     */
    public boolean isStudent() { return user instanceof Student; }
    public boolean isTutor() { return user instanceof Tutor; }
    public boolean isAdmin() { return user instanceof Admin; }
}