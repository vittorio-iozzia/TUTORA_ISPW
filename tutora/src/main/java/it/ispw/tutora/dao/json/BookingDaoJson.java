package it.ispw.tutora.dao.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.enums.Status;
import it.ispw.tutora.exception.BookingNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateBookingException;
import it.ispw.tutora.exception.LessonNotFoundException;
import it.ispw.tutora.model.*;

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

    private static final String JSON_PATH = "../tutora_data/bookings.json";
    private static final String REFUNDED  = "REFUNDED";
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

    /**
     * Verifica che lo student non abbia già una booking attiva (Pending o Paid)
     * il cui orario si sovrappone all'intervallo [newLessonStart, newLessonEnd).
     * Il lesson status corrente viene letto in tempo reale da lessons.json
     * tramite {@link #isActiveBooking} per evitare dati denormalizzati stale.
     * Due intervalli si sovrappongono se: existingStart &lt; newEnd AND existingEnd &gt; newStart.
     *
     * @throws DuplicateBookingException se esiste già una booking con orario sovrapposto
     */
    @Override
    public void checkNoDuplicateBooking(Connection conn,
                                        String studentUsername,
                                        LocalDateTime newLessonStart,
                                        LocalDateTime newLessonEnd)
            throws DatabaseException, DuplicateBookingException {
        LessonDaoJson lessonDao = new LessonDaoJson();
        for (BookingRecord r : readAll()) {
            if (!isActiveBooking(r, studentUsername, lessonDao)) continue;
            LocalDateTime existingStart = LocalDateTime.parse(r.lessonStartTime);
            LocalDateTime existingEnd   = LocalDateTime.parse(r.lessonEndTime);
            if (existingStart.isBefore(newLessonEnd) && existingEnd.isAfter(newLessonStart)) {
                throw new DuplicateBookingException(studentUsername, newLessonStart, newLessonEnd);
            }
        }
    }

    private boolean isActiveBooking(BookingRecord r, String studentUsername, LessonDaoJson lessonDao) {
        if (!studentUsername.equals(r.studentUsername)) return false;
        if (r.lessonStartTime == null || r.lessonEndTime == null) return false;
        if (REFUNDED.equals(r.paymentStatus)) return false;
        try {
            LessonStatus status = lessonDao.selectLesson(null, r.lessonId).getLessonStatus();
            return status != LessonStatus.COMPLETED && status != LessonStatus.CANCELLED;
        } catch (DatabaseException | LessonNotFoundException e) {
            return false;
        }
    }

    // ----------------------------------------------------------------
    // Mapping record ↔ model
    // ----------------------------------------------------------------

    /**
     * Ricostruisce un oggetto Booking da un record JSON.
     *
     * Se il record contiene i campi denormalizzati (subjectName, lessonStartTime
     * ecc.) costruisce una Lesson con TutorExpertise parziale ma sufficiente
     * per la visualizzazione nella UI.
     *
     * Lesson.build() richiede listedPrice > 0 e startTime/endTime non null:
     * i valori vengono risolti con fallback sicuri per i record legacy che
     * non hanno lessonEndTime/lessonListedPrice persistiti.
     */
    private Booking toBooking(BookingRecord r) {
        // startTime — legacy records senza il campo: dummy passato (filtrato da isAfter)
        LocalDateTime startTime = r.lessonStartTime != null
                ? LocalDateTime.parse(r.lessonStartTime)
                : LocalDateTime.of(2000, 1, 1, 0, 0);

        // endTime — richiesto da Lesson.checkTime(); fallback startTime+1h
        LocalDateTime endTime = r.lessonEndTime != null
                ? LocalDateTime.parse(r.lessonEndTime)
                : startTime.plusHours(1);

        // listedPrice — richiesto da Lesson.checkPrice(); fallback pricePaid
        String priceStr = r.lessonListedPrice != null ? r.lessonListedPrice : r.pricePaid;
        BigDecimal listedPrice = priceStr != null ? new BigDecimal(priceStr) : BigDecimal.ONE;

        Lesson.Builder lessonBuilder = new Lesson.Builder()
                .id(r.lessonId)
                .remote(r.lessonRemote)
                .startTime(startTime)
                .endTime(endTime)
                .listedPrice(listedPrice);

        // Ripristina lessonStatus dal record — necessario per i filtri in MyLessonsGfxController
        if (r.lessonStatus != null) {
            try {
                lessonBuilder.lessonStatus(LessonStatus.valueOf(r.lessonStatus));
            } catch (IllegalArgumentException ignored) { /* valore sconosciuto — lascia null */ }
        }

        if (r.subjectName != null) {
            lessonBuilder.expertise(buildExpertiseFromRecord(r));
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
            populateLessonFields(b, lesson);
            if (lesson.getExpertise() != null) populateExpertiseFields(b, lesson.getExpertise());
        }
        return b;
    }

    /** Popola i campi denormalizzati di tempo/prezzo/stato dalla Lesson. */
    private static void populateLessonFields(BookingRecord b, Lesson lesson) {
        if (lesson.getStartTime() != null)   b.lessonStartTime  = lesson.getStartTime().toString();
        if (lesson.getEndTime() != null)     b.lessonEndTime    = lesson.getEndTime().toString();
        if (lesson.getLessonStatus() != null) b.lessonStatus    = lesson.getLessonStatus().name();
        if (lesson.getListedPrice() != null) b.lessonListedPrice = lesson.getListedPrice().toPlainString();
        b.lessonRemote = lesson.isRemote();
    }

    /** Popola i campi denormalizzati relativi a tutor e sotto-categoria dall'expertise. */
    private static void populateExpertiseFields(BookingRecord b, TutorExpertise exp) {
        if (exp.getSubcategory() != null) b.subjectName = exp.getSubcategory().getName();
        Tutor tutor = exp.getTutor();
        if (tutor != null) {
            b.tutorUsername = tutor.getUsername();
            b.tutorName     = tutor.getName();
            b.tutorSurname  = tutor.getSurname();
        }
    }

    /**
     * Ricostruisce la TutorExpertise denormalizzata da un record JSON.
     * SubCategory con parentCategory null è accettabile per la visualizzazione.
     * BigDecimal.ONE come hourlyPrice è un placeholder (il valore reale non è persistito).
     */
    private static TutorExpertise buildExpertiseFromRecord(BookingRecord r) {
        SubCategory sub = new SubCategory(r.subjectName, null, "");
        Tutor tutor = null;
        if (r.tutorUsername != null) {
            tutor = new Tutor.Builder()
                    .username(r.tutorUsername)
                    .name(r.tutorName != null ? r.tutorName : "")
                    .surname(r.tutorSurname != null ? r.tutorSurname : "")
                    .build();
        }
        return new TutorExpertise(tutor, sub, BigDecimal.ONE, Status.APPROVED, LocalDateTime.now());
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
        String  lessonEndTime;      // null nei record legacy → fallback startTime+1h
        boolean lessonRemote;
        String  lessonStatus;       // LessonStatus.name() — null nei record legacy
        String  lessonListedPrice;  // null nei record legacy → fallback pricePaid
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
            case REFUNDED -> REFUNDED;
        };
    }

    private static PaymentStatus fromJsonStatus(String s) {
        return switch (s) {
            case "Pending"  -> PaymentStatus.PENDING;
            case "Paid"     -> PaymentStatus.PAID;
            case REFUNDED -> PaymentStatus.REFUNDED;
            default -> throw new IllegalArgumentException("Unknown payment status: " + s);
        };
    }
}
