package it.ispw.tutora.dao.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.enums.Status;
import it.ispw.tutora.exception.BookingNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Implementazione JSON di BookingDao.
 *
 * Legge e scrive su bookings.json applicando il pattern read-modify-write
 * ad ogni operazione di scrittura.
 * La Connection viene accettata nelle firme per rispettare l'interfaccia
 * ma non viene usata: non c'è nessun DB.
 *
 * -----------------------------------------------------------------------
 * Nota su toRecord / toBooking (campi denormalizzati)
 * -----------------------------------------------------------------------
 * Oltre ai campi relazionali essenziali (lessonId, studentUsername),
 * il record JSON memorizza anche i dati necessari alla visualizzazione
 * (subjectName, lessonStartTime, lessonRemote, tutorUsername/Name/Surname).
 * Questo evita di dover caricare Lesson e TutorExpertise completi
 * tramite DAO aggiuntivi — accettabile in un'implementazione JSON di test.
 *
 * Backward compatibility: i record esistenti senza questi campi
 * vengono deserializzati con valori null/false; toBooking() li gestisce
 * costruendo un Lesson parziale o completo a seconda di ciò che è disponibile.
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

    private static final Logger LOGGER = Logger.getLogger(BookingDaoJson.class.getName());
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
            if (username.equals(r.studentUsername))
                list.add(toBooking(r));
        }
        return list;
    }

    /**
     * Restituisce le booking relative alle lezioni di un tutor,
     * ordinate per data di inizio lezione crescente.
     * Il filtro usa il campo tutorUsername salvato in fase di insertBooking.
     * I record legacy senza tutorUsername vengono ignorati.
     */
    @Override
    public List<Booking> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException {
        List<BookingRecord> records = readAll();
        List<Booking> list = new ArrayList<>();
        for (BookingRecord r : records) {
            if (tutorUsername.equals(r.tutorUsername))
                list.add(toBooking(r));
        }
        // Ordina per data di inizio lezione crescente; null va in coda
        list.sort((a, b) -> {
            LocalDateTime ta = a.getLesson().getStartTime();
            LocalDateTime tb = b.getLesson().getStartTime();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return ta.compareTo(tb);
        });
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

    /**
     * Ricostruisce un oggetto Booking da un record JSON.
     *
     * Se il record contiene i campi denormalizzati (subjectName, lessonStartTime
     * ecc.) costruisce una Lesson con TutorExpertise parziale ma sufficiente
     * per la visualizzazione nella UI. I record legacy senza questi campi
     * producono una Lesson con solo l'id (comportamento precedente).
     */
    private Booking toBooking(BookingRecord r) {
        // Costruzione della Lesson — parziale o arricchita a seconda dei campi disponibili
        Lesson.Builder lessonBuilder = new Lesson.Builder()
                .id(r.lessonId)
                .remote(r.lessonRemote);

        if (r.lessonStartTime != null) {
            lessonBuilder.startTime(LocalDateTime.parse(r.lessonStartTime));
        }

        if (r.subjectName != null) {
            // SubCategory con parentCategory null: toString() la gestisce con "N/A"
            SubCategory sub = new SubCategory(r.subjectName, null, "");

            // Tutor ricostruito con username, name e surname per getFullName()
            Tutor tutor = null;
            if (r.tutorUsername != null) {
                tutor = new Tutor.Builder()
                        .username(r.tutorUsername)
                        .name(r.tutorName != null ? r.tutorName : "")
                        .surname(r.tutorSurname != null ? r.tutorSurname : "")
                        .build();
            }
            // TutorExpertise richiede price > 0: BigDecimal.ONE è un placeholder
            TutorExpertise exp = new TutorExpertise(
                    tutor, sub, BigDecimal.ONE, Status.APPROVED, LocalDateTime.now());
            lessonBuilder.expertise(exp);
        }
        return new Booking.Builder()
                .id(r.id)
                .lesson(lessonBuilder.build())
                .student(new Student.Builder().username(r.studentUsername).build())
                .bookedAt(LocalDateTime.parse(r.bookedAt))
                .pricePaid(new BigDecimal(r.pricePaid))
                .paymentStatus(fromJsonStatus(r.paymentStatus))
                .paymentRef(r.paymentRef)
                .build();
    }

    /**
     * Converte un oggetto Booking nel corrispondente record JSON.
     * Salva i campi denormalizzati dalla Lesson quando disponibili,
     * in modo che toBooking() e findByTutor() possano usarli al reload.
     */
    private BookingRecord toRecord(Booking booking) {
        BookingRecord b = new BookingRecord();
        b.id = booking.getId();
        b.lessonId = booking.getLesson().getId();
        b.studentUsername = booking.getStudent().getUsername();
        b.bookedAt = booking.getBookedAt().toString();
        b.pricePaid = booking.getPricePaid().toPlainString();
        b.paymentStatus = toJsonStatus(booking.getPaymentStatus());
        b.paymentRef = booking.getPaymentRef();

        // Campi denormalizzati — disponibili quando insertBooking riceve
        // una Booking con Lesson completa (es. da BookTutorController.payment)
        Lesson lesson = booking.getLesson();
        if (lesson != null) {
            if (lesson.getStartTime() != null)
                b.lessonStartTime = lesson.getStartTime().toString();
            b.lessonRemote = lesson.isRemote();

            TutorExpertise exp = lesson.getExpertise();
            if (exp != null) {
                if (exp.getSubcategory() != null)
                    b.subjectName = exp.getSubcategory().getName();
                Tutor tutor = exp.getTutor();
                if (tutor != null) {
                    b.tutorUsername = tutor.getUsername();
                    b.tutorName = tutor.getName();
                    b.tutorSurname = tutor.getSurname();
                }
            }
        }
        return b;
    }
    // ----------------------------------------------------------------
    // POJO interno per la serializzazione Jackson
    // ----------------------------------------------------------------

    /**
     * Record JSON di una booking.
     *
     * Campi core (sempre presenti):
     *   id, lessonId, studentUsername, bookedAt, pricePaid, paymentStatus, paymentRef
     *
     * Campi denormalizzati (null/false nei record legacy):
     *   tutorUsername, tutorName, tutorSurname — per findByTutor() e display tutor
     *   subjectName                             — nome della sotto-categoria
     *   lessonStartTime                         — ISO string del startTime della lezione
     *   lessonRemote                            — true se la lezione è da remoto
     */
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static class BookingRecord {
        // Core
        int    id;
        int    lessonId;
        String studentUsername;
        String bookedAt;
        String pricePaid;
        String paymentStatus;
        String paymentRef;
        // Denormalizzati — null/false per record legacy
        String  tutorUsername;
        String  tutorName;
        String  tutorSurname;
        String  subjectName;
        String  lessonStartTime;
        boolean lessonRemote;
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
