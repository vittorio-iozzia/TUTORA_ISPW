package it.ispw.tutora.exception;

/**
 * Eccezione base del dominio TUTORA.
 * Tutte le eccezioni personalizzate del progetto estendono questa classe.
 *
 * È checked (extends Exception) per forzare la gestione esplicita
 * nei Controller: il compilatore segnala ogni punto in cui un'eccezione
 * di dominio potrebbe sfuggire senza essere trattata.
 */
public class TutoraException extends Exception {

    public TutoraException(String message) {
        super(message);
    }

    public TutoraException(String message, Throwable cause) {
        super(message, cause);
    }
}