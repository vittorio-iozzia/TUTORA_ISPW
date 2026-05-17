package it.ispw.tutora.dao.demo;

import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.exception.BookingNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateBookingException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.SubCategory;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.TutorExpertise;

import java.sql.Connection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementazione in-memory di BookingDao per l'ambiente demo.
 *
 * Sostituisce il DB con una Map<Integer, Booking> (cache) che vive
 * per tutta la durata della sessione applicativa.
 * La Connection viene accettata nelle firme per rispettare l'interfaccia
 * ma non viene usata: in demo non esiste una transazione reale.
 *
 * nextId emula l'AUTO_INCREMENT del DB: ogni insertBooking restituisce
 * un id progressivo crescente.
 */
public class BookingDaoDemo implements BookingDao {

    private final Map<Integer, Booking> cache = new HashMap<>();
    private int nextId = 1;

    /**
     * Aggiorna il paymentStatus della booking in cache applicando
     * la macchina a stati finiti definita in Booking.updatePaymentStatus().
     * La modifica è in-place: il riferimento in cache non cambia.
     *
     * Guard: se lo status è già quello richiesto il metodo torna subito.
     * In Demo mode payment() chiama booking1.updatePaymentStatus(PAID) prima
     * di invocare questo metodo sullo stesso oggetto in cache → senza guard
     * la FSM riceverebbe PAID → PAID e lancerebbe IllegalArgumentException.
     *
     * @throws BookingNotFoundException se l'id non è presente in cache
     */
    @Override
    public void updateStatus(Connection conn, int id, PaymentStatus status)
            throws DatabaseException, BookingNotFoundException {
        if (!cache.containsKey(id)) throw new BookingNotFoundException(id);
        Booking booking = cache.get(id);
        if (booking.getPaymentStatus() == status) return;
        booking.updatePaymentStatus(status);
    }

    /**
     * Inserisce la booking in cache assegnandole un id progressivo.
     *
     * @return id assegnato (emula AUTO_INCREMENT del DB)
     */
    @Override
    public int insertBooking(Connection conn, Booking booking)
            throws DatabaseException {
        int id = nextId++;
        cache.put(id, booking);
        return id;
    }

    /**
     * Restituisce le booking dello student ordinate per data decrescente
     * (più recenti prima). Lista vuota se lo student non ha prenotazioni.
     */
    @Override
    public List<Booking> findByStudent(Connection conn, String username)
            throws DatabaseException {
        return cache.values().stream()
                .filter(booking -> booking.getStudent().getUsername().equals(username))
                .sorted(Comparator.comparing(Booking::getBookedAt).reversed())
                .toList();
    }

    /**
     * Carica una booking per id.
     *
     * @throws BookingNotFoundException se l'id non è presente in cache
     */
    @Override
    public Booking selectBooking(Connection conn, int id)
            throws DatabaseException, BookingNotFoundException {
        if (!cache.containsKey(id)) throw new BookingNotFoundException(id);
        return cache.get(id);
    }

    /**
     * Restituisce le booking relative alle lezioni di un tutor,
     * ordinate per data di inizio lezione crescente.
     * Lista vuota se il tutor non ha prenotazioni.
     */
    @Override
    public List<Booking> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException {
        return cache.values().stream()
                .filter(b -> b.getLesson()
                              .getExpertise()
                              .getTutor()
                              .getUsername()
                              .equals(tutorUsername))
                .sorted(Comparator.comparing(b -> b.getLesson().getStartTime()))
                .toList();
    }

    /**
     * Verifica che lo student non abbia già una booking attiva (Pending o Paid)
     * con il tutor e la sottocategoria dati, per una lezione non ancora terminata.
     * Naviga il grafo degli oggetti in-memory già presenti in cache.
     *
     * @throws DuplicateBookingException se esiste già una booking attiva
     */
    @Override
    public void checkNoDuplicateBooking(Connection conn,
                                        String studentUsername,
                                        String tutorUsername,
                                        String subcategoryName)
            throws DatabaseException, DuplicateBookingException {
        for (Booking b : cache.values()) {
            if (b.getPaymentStatus() == PaymentStatus.REFUNDED) continue;
            if (!studentUsername.equals(b.getStudent().getUsername())) continue;
            if (b.getLesson() == null) continue;
            LessonStatus ls = b.getLesson().getLessonStatus();
            if (ls == LessonStatus.COMPLETED || ls == LessonStatus.CANCELLED) continue;
            TutorExpertise exp = b.getLesson().getExpertise();
            if (exp == null) continue;
            Tutor tutor = exp.getTutor();
            SubCategory sub = exp.getSubcategory();
            if (tutor != null && tutorUsername.equals(tutor.getUsername())
                    && sub != null && subcategoryName.equals(sub.getName())) {
                throw new DuplicateBookingException(studentUsername, tutorUsername, subcategoryName);
            }
        }
    }
}
