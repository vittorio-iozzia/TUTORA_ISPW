package it.ispw.tutora.exception;

/**
 * Lanciata quando si cerca un User che non esiste nel DB.
 * Tipicamente sollevata dal DAO quando una query per username
 * non restituisce righe.
 */
public class UserNotFoundException extends TutoraException {

    public UserNotFoundException(String username) {
        super("User not found with username: " + username);
    }
}
