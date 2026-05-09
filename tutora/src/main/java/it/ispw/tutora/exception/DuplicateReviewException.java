package it.ispw.tutora.exception;

public class DuplicateReviewException extends TutoraException {
    public DuplicateReviewException(int id) {
        super("Review: " + id + "already present ");
    }
    public DuplicateReviewException(String mes) {
        super(mes);
    }
}
