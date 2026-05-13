package it.ispw.tutora.exception;

/**
 * Lanciata quando le credenziali fornite non superano la verifica di autenticazione.
 *
 * Distingue i fallimenti di autenticazione (password errata, account disabilitato)
 * dalle eccezioni di infrastruttura e di ricerca, permettendo alla View di gestire ciascun caso
 * in modo specifico senza fare {@code instanceof} su eccezioni generiche.
 */
public class AuthenticationException extends TutoraException {

    public AuthenticationException(String message) {
        super(message);
    }
}
