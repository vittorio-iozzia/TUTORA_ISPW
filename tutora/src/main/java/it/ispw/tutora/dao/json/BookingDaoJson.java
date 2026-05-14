package it.ispw.tutora.dao.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.exception.BookingNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Lesson;
import it.ispw.tutora.model.Student;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementazione JSON di BookingDao.
 *
 * Legge e scrive su bookings.json applicando il pattern read-modify-write
 * ad ogni operazione di scrittura.
 * La Connection viene accettata nelle firme per rispettare l'interfaccia
 * ma non viene usata: non c'è nessun DB.
 *
 * -----------------------------------------------------------------------
 * Nota su toBooking (oggetti parziali)
 * -----------------------------------------------------------------------
 * Il file JSON conserva solo lessonId e studentUsername come riferimenti.
 * toBooking() ricostruisce oggetti Lesson e Student parziali (solo id /
 * username popolati) sufficienti per il dominio applicativo.
 * Se servono oggetti completi, il Controller deve fare un secondo fetch
 * tramite LessonDao / StudentDao.
 *
 * -----------------------------------------------------------------------
 * Nota su insertBooking
 * -----------------------------------------------------------------------
 * L'id viene generato leggendo il massimo id presente nel file e
 * incrementandolo di 1, emulando l'AUTO_INCREMENT del DB.
 * In ambiente multi-thread questo approccio non è thread-safe —
 * accettabile per un'implementazione JSON di sviluppo/test.
 */
public class BookingDaoJson implements BookingDao {

    private static final String JSON_PATH = "../tutora_data/bookings.json";
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Aggiorna il paymentStatus della booking con l'id dato.
     *
     * @throws BookingNotFoundException se nessuna booking ha quell'id
     */
    @Override
    public void updateStatus(Connection conn, int id, PaymentStatus status)
            throws DatabaseException, BookingNotFoundException {
        List<BookingRecord> list = readAll();
        for (BookingRecord r : list) {
            if (r.id == id) {
                r.paymentStatus = toJsonStatus(status);
                writeAll(list);
                return;
            }
        }
        throw new BookingNotFoundException(id);
    }

    /**
     * Inserisce una nuova booking nel file JSON.
     * L'id è calcolato come max(id esistenti) + 1.
     *
     * @return id assegnato alla nuova booking
     */
    @Override
    public int insertBooking(Connection conn, Booking booking) throws DatabaseException {
        List<BookingRecord> list = readAll();
        int nextId = list.stream()
                .mapToInt(r -> r.id)
                .max()
                .orElse(0) + 1;
        BookingRecord bookingRecord = toRecord(booking);
        bookingRecord.id = nextId;
        list.add(bookingRecord);
        writeAll(list);
        return nextId;
    }

    /**
     * Restituisce tutte le booking dello student ordinate per data
     * (l'ordinamento finale è responsabilità del Controller).
     * Lista vuota se lo student non ha prenotazioni.
     */
    @Override
    public List<Booking> findByStudent(Connection conn, String username) throws DatabaseException {
        List<BookingRecord> records = readAll();
        List<Booking> list = new ArrayList<>();
        for (BookingRecord r : records) {
            if (r.studentUsername.equals(username))
                list.add(toBooking(r));
        }
        return list;
    }

    /**
     * Carica una booking per id.
     *
     * @throws BookingNotFoundException se nessuna booking ha quell'id
     */
    @Override
    public Booking selectBooking(Connection conn, int id)
            throws DatabaseException, BookingNotFoundException {
        for (BookingRecord r : readAll()) {
            if (r.id == id) return toBooking(r);
        }
        throw new BookingNotFoundException(id);
    }

    // ----------------------------------------------------------------
    // Mapping record ↔ model
    // ----------------------------------------------------------------

    /** Ricostruisce un oggetto Booking da un record JSON. */
    private Booking toBooking(BookingRecord r) {
        return new Booking.Builder()
                .id(r.id)
                .lesson(new Lesson.Builder().id(r.lessonId).build())
                .student(new Student.Builder().username(r.studentUsername).build())
                .bookedAt(LocalDateTime.parse(r.bookedAt))
                .pricePaid(new BigDecimal(r.pricePaid))
                .paymentStatus(fromJsonStatus(r.paymentStatus))
                .paymentRef(r.paymentRef)
                .build();
    }

    /** Converte un oggetto Booking nel corrispondente record JSON. */
    private BookingRecord toRecord(Booking booking) {
        BookingRecord b = new BookingRecord();
        b.id = booking.getId();
        b.lessonId = booking.getLesson().getId();
        b.studentUsername = booking.getStudent().getUsername();
        b.bookedAt = booking.getBookedAt().toString();
        b.pricePaid = booking.getPricePaid().toPlainString();
        b.paymentStatus = toJsonStatus(booking.getPaymentStatus());
        b.paymentRef = booking.getPaymentRef();
        return b;
    }

    // ----------------------------------------------------------------
    // POJO interno per la serializzazione Jackson
    // ----------------------------------------------------------------
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static class BookingRecord {
        int id;
        int lessonId;
        String studentUsername;
        String bookedAt;
        String pricePaid;
        String paymentStatus;
        String paymentRef;
    }

    // ----------------------------------------------------------------
    // I/O JSON
    // ----------------------------------------------------------------

    private List<BookingRecord> readAll() throws DatabaseException {
        try {
            BookingRecord[] records = mapper.readValue(
                    new File(JSON_PATH), BookingRecord[].class);
            return new ArrayList<>(Arrays.asList(records));
        } catch (IOException e) {
            throw new DatabaseException("Error reading JSON file: " + JSON_PATH, e);
        }
    }

    private void writeAll(List<BookingRecord> records) throws DatabaseException {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(JSON_PATH), records);
        } catch (IOException e) {
            throw new DatabaseException("Error writing JSON file: " + JSON_PATH, e);
        }
    }

    // ----------------------------------------------------------------
    // Conversione PaymentStatus ↔ stringa JSON
    // ----------------------------------------------------------------

    private static String toJsonStatus(PaymentStatus status) {
        return switch (status) {
            case PENDING  -> "Pending";
            case PAID     -> "Paid";
            case REFUNDED -> "Refunded";
        };
    }

    private static PaymentStatus fromJsonStatus(String s) {
        return switch (s) {
            case "Pending"  -> PaymentStatus.PENDING;
            case "Paid"     -> PaymentStatus.PAID;
            case "Refunded" -> PaymentStatus.REFUNDED;
            default -> throw new IllegalArgumentException("Unknown payment status: " + s);
        };
    }
}
