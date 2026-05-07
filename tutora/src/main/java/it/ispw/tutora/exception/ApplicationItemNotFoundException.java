package it.ispw.tutora.exception;

/**
 * Lanciata quando si cerca un ApplicationItem che non esiste nel DB.
 * Tipicamente sollevata dal DAO quando una query per ID
 * non restituisce righe.
 */
public class ApplicationItemNotFoundException extends TutoraException {

    public ApplicationItemNotFoundException(int itemId) {
        super("ApplicationItem not found with id: " + itemId);
    }

    public ApplicationItemNotFoundException(String message) {
        super(message);
    }
}
