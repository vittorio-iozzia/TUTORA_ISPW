package it.ispw.tutora.exception;

/**
 * Lanciata quando si cerca un Document che non esiste nel DB.
 * Tipicamente sollevata dal DAO quando una query per ID
 * non restituisce righe.
 */
public class DocumentNotFoundException extends TutoraException {

    public DocumentNotFoundException(int documentId) {
        super("Document not found with id: " + documentId);
    }

    public DocumentNotFoundException(String message) {
        super(message);
    }
}
