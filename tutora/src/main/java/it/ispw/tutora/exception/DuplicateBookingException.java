package it.ispw.tutora.exception;

/**
 * Lanciata quando lo studente tenta di prenotare una lezione
 * con un tutor per cui ha già una prenotazione attiva (Pending o Paid)
 * nella stessa sotto-categoria.
 */
public class DuplicateBookingException extends TutoraException {

    /**
     * Usato da checkNoDuplicateBooking e da checkNoPendingRequest.
     */
    public DuplicateBookingException(String studentUsername,
                                     String tutorUsername,
                                     String subcategoryName) {
        super("Student '" + studentUsername
                + "' already has an active booking with tutor '"
                + tutorUsername + "' for '" + subcategoryName + "'");
    }
}
