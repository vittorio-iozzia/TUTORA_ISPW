package it.ispw.tutora.exception;

import it.ispw.tutora.model.TutorExpertise;

public class DuplicateTutorExpertiseException extends TutoraException {
    public DuplicateTutorExpertiseException(String message) {
        super(message);
    }
}
