package it.ispw.tutora.exception;

/**
 * Lanciata quando si tenta di inserire un User con uno username
 * o un'email già presenti nel DB (violazione del vincolo UNIQUE).
 */
public class DuplicateUserException extends TutoraException {

    public DuplicateUserException(String message) {
        super(message);
    }
}
