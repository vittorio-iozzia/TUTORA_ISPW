package it.ispw.tutora.dao.demo;

import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.exception.BookingNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Booking;

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
     * @throws BookingNotFoundException se l'id non è presente in cache
     */
    @Override
    public void updateStatus(Connection conn, int id, PaymentStatus status)
            throws DatabaseException, BookingNotFoundException {
        if (!cache.containsKey(id)) throw new BookingNotFoundException(id);
        cache.get(id).updatePaymentStatus(status);
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
                .sorted(Comparator.comparing(Booking::getBookedAt))
                .toList()
                .reversed();
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
}
