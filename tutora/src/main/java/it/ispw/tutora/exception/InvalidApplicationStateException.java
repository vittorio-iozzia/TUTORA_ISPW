package it.ispw.tutora.exception;

/**
 * Lanciata quando si tenta una transizione di stato non valida
 * su una {@code TutorApplication} (es. da ACCEPTED a SUBMITTED).
 *
 * <p>Converte l'{@link IllegalStateException} unchecked lanciata dal Model
 * in un'eccezione checked del dominio, così il Controller può dichiararla
 * esplicitamente nella firma e la View può gestirla in modo specifico.</p>
 */
public class InvalidApplicationStateException extends TutoraException {

    public InvalidApplicationStateException(String message) {
        super(message);
    }
}
