package it.ispw.tutora.exception;

/**
 * Lanciata quando si richiede una categoria che non esiste nel DB.
 * Tipicamente sollevata dal DAO quando una query per nome
 * non restituisce righe.
 */
public class CategoryNotFoundException extends TutoraException {

    public CategoryNotFoundException(String categoryName) {
        super("Category not found: " + categoryName);
    }
}