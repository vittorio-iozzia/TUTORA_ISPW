package it.ispw.tutora.exception;

/**
 * Lanciata quando il servizio esterno di validazione documenti
 * restituisce un errore interno (ExecutionException) — distinto dal
 * timeout ({@link ValidationTimeoutException}) in cui il servizio
 * non risponde entro il limite di tempo.
 */
public class ValidationServiceException extends TutoraException {

    public ValidationServiceException(String message) {
        super(message);
    }

    public ValidationServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
