package it.ispw.tutora.exception;

/**
 * Lanciata quando la validazione dei documenti tramite API esterna
 * non si completa entro il timeout di 3 minuti.
 * Come da activity diagram UC-2: il caso d'uso termina al timeout.
 */
public class ValidationTimeoutException extends TutoraException {

    public ValidationTimeoutException(String message) {
        super(message);
    }
}