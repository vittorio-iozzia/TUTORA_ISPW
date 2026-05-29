package it.ispw.tutora.exception;

import java.time.LocalDateTime;

/**
 * Lanciata quando lo studente tenta di prenotare una lezione
 * che entra in conflitto con una prenotazione attiva esistente.
 *
 * Due costruttori distinti per i due casi di utilizzo:
 *  - time-overlap: la nuova lezione si sovrappone orariamente a una già prenotata
 *  - pending duplicate: lo student ha già inviato una richiesta per lo stesso slot
 */
public class DuplicateBookingException extends TutoraException {

    /**
     * Usato da checkNoDuplicateBooking (controllo sovrapposizione oraria).
     */
    public DuplicateBookingException(String studentUsername,
                                     LocalDateTime newStart,
                                     LocalDateTime newEnd) {
        super("Student '" + studentUsername
                + "' already has an active booking overlapping with ["
                + newStart + " – " + newEnd + "]");
    }

    /**
     * Usato da checkNoPendingRequest (stessa richiesta già pendente).
     */
    public DuplicateBookingException(String studentUsername,
                                     String tutorUsername,
                                     String subcategoryName) {
        super("Student '" + studentUsername
                + "' already has a pending request with tutor '"
                + tutorUsername + "' for '" + subcategoryName + "'");
    }
}
