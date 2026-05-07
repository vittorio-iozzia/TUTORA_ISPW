package it.ispw.tutora.exception;

/**
 * Lanciata quando si cerca una TutorApplication che non esiste nel DB.
 * Tipicamente sollevata dal DAO quando una query per ID
 * non restituisce righe.
 */
public class ApplicationNotFoundException extends TutoraException {

    public ApplicationNotFoundException(int applicationId) {
        super("Application not found with id: " + applicationId);
    }

    public ApplicationNotFoundException(String message) {
        super(message);
    }
}