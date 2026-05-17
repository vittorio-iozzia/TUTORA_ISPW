package it.ispw.tutora.exception;

/**
 * Lanciata quando lo studente tenta di prenotare una lezione
 * con un tutor per una sottocategoria in cui ha già una prenotazione
 * attiva (Pending o Paid) per una lezione non ancora terminata.
 * Rispecchia il vincolo one-booking-per-(student, tutor, subcategory).
 */
public class DuplicateBookingException extends TutoraException {

    public DuplicateBookingException(String studentUsername,
                                     String tutorUsername,
                                     String subcategoryName) {
        super("Student '" + studentUsername
                + "' already has an active booking with tutor '"
                + tutorUsername + "' for '" + subcategoryName + "'");
    }
}
