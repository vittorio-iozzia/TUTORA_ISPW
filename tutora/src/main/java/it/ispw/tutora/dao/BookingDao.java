package it.ispw.tutora.dao;

import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.exception.BookingNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateBookingException;
import it.ispw.tutora.model.Booking;

import java.sql.Connection;
import java.util.List;

/**
 * Contratto DAO per la tabella booking.
 *
 * -----------------------------------------------------------------------
 * Nota sulle responsabilità
 * -----------------------------------------------------------------------
 * BookingDao gestisce la persistenza delle prenotazioni.
 * Non esiste un metodo delete: una booking non viene mai cancellata
 * fisicamente — il suo stato viene aggiornato tramite updateStatus()
 * (es. REFUNDED in caso di rimborso).
 *
 * -----------------------------------------------------------------------
 * Nota sulle transazioni
 * -----------------------------------------------------------------------
 * I metodi di scrittura ricevono la Connection come parametro:
 * è il Controller applicativo a gestire commit e rollback.
 * insertBooking viene tipicamente invocato nella stessa transazione
 * in cui viene aggiornato il budget dello student e lo status della lesson.
 *
 * -----------------------------------------------------------------------
 * Nota su findByStudent
 * -----------------------------------------------------------------------
 * findByStudent restituisce una lista vuota se lo student non ha
 * prenotazioni — l'assenza di booking non è un errore ma un caso
 * legittimo (student nuovo o senza prenotazioni).
 */
public interface BookingDao {

    /**
     * Aggiorna il payment_status di una booking esistente.
     * Usato per segnare una prenotazione come PAID o REFUNDED.
     */
    void updateStatus(Connection conn, int id, PaymentStatus status)
            throws DatabaseException, BookingNotFoundException;

    /**
     * Inserisce una nuova prenotazione.
     * Il Controller deve aver già verificato che lo student abbia
     * budget sufficiente e aggiornato lo status della lesson a BOOKED
     * prima di invocare questo metodo.
     */
    int insertBooking(Connection conn, Booking booking)
            throws DatabaseException;

    /**
     * Carica tutte le prenotazioni di uno student ordinate per
     * data decrescente (più recenti prima).
     * Restituisce una lista vuota se lo student non ha prenotazioni.
     */
    List<Booking> findByStudent(Connection conn, String username)
            throws DatabaseException;

    /**
     * Carica una prenotazione per id.
     */
    Booking selectBooking(Connection conn, int id)
            throws DatabaseException, BookingNotFoundException;

    /**
     * Carica tutte le prenotazioni relative alle lezioni di un tutor,
     * ordinate per data di inizio lezione crescente.
     * Restituisce una lista vuota se il tutor non ha prenotazioni.
     */
    List<Booking> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException;

    /**
     * Verifica che lo student non abbia già una prenotazione attiva
     * (Pending o Paid, lezione non ancora terminata) con lo stesso tutor
     * per la stessa sotto-categoria.
     * Lancia DuplicateBookingException se il vincolo è violato;
     * ritorna normalmente se il controllo passa.
     */
    void checkNoDuplicateBooking(Connection conn,
                                 String studentUsername,
                                 String tutorUsername,
                                 String subcategoryName)
            throws DatabaseException, DuplicateBookingException;
}
