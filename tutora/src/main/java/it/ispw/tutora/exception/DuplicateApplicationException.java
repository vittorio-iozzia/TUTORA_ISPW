package it.ispw.tutora.exception;

/**
 * Lanciata quando lo studente tenta di aprire una seconda application
 * attiva (Draft o Submitted) per la stessa categoria.
 * Rispecchia il vincolo UNIQUE uq_one_active_application del DB.
 */
public class DuplicateApplicationException extends TutoraException {

    public DuplicateApplicationException(String studentUsername, String categoryName) {
        super("Student '" + studentUsername
                + "' already has an active application for category '"
                + categoryName + "'");
    }
}