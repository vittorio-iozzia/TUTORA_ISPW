package it.ispw.tutora.dao.db;

import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.exception.BookingNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateBookingException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Lesson;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.SubCategory;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.TutorExpertise;
import org.intellij.lang.annotations.Language;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementazione JDBC di BookingDao.
 *
 * -----------------------------------------------------------------------
 * Nota sulla conversione dello status
 * -----------------------------------------------------------------------
 * Il DB memorizza payment_status come ENUM('Pending','Paid','Refunded').
 * toDbStatus / fromDbStatus gestiscono la conversione bidirezionale
 * rispetto all'enum Java PaymentStatus (PENDING, PAID, REFUNDED).
 * Non si usa setObject() direttamente sull'enum Java perché MySQL
 * non lo riconosce — serve sempre la conversione esplicita in stringa.
 *
 * -----------------------------------------------------------------------
 * Nota sul mapping parziale
 * -----------------------------------------------------------------------
 * mapBooking() costruisce oggetti Lesson e Student parziali,
 * contenenti solo i campi presenti nella tabella booking
 * (lesson_id e student_username). Per oggetti completi il Controller
 * deve eseguire query aggiuntive tramite i DAO appropriati.
 *
 * -----------------------------------------------------------------------
 * Nota su findByStudent
 * -----------------------------------------------------------------------
 * findByStudent restituisce sempre una lista, mai null.
 * Se lo student non ha prenotazioni la lista è vuota —
 * il chiamante non deve gestire il caso null.
 */
public class BookingDaoDb implements BookingDao {

    // ----------------------------------------------------------------
    // Costanti SQL
    // ----------------------------------------------------------------

    /** UPDATE del solo campo payment_status per una booking esistente. */
    @Language("SQL")
    private static final String SQL_UPDATE =
            "UPDATE booking " +
                    "SET payment_status = ? " +
                    "WHERE id = ?";

    /**
     * INSERT di una nuova prenotazione.
     * booked_at usa il DEFAULT CURRENT_TIMESTAMP definito nel DB.
     * payment_status usa il DEFAULT 'Pending' definito nel DB.
     */
    @Language("SQL")
    private static final String SQL_INSERT =
            "INSERT INTO booking (lesson_id, student_username, price_paid, payment_ref) " +
                    "VALUES (?, ?, ?, ?)";

    /**
     * SELECT di una booking per id con colonne esplicite.
     * Non usa SELECT * per stabilità rispetto a future modifiche dello schema.
     */
    @Language("SQL")
    private static final String SQL_SELECT =
            "SELECT id, lesson_id, student_username, booked_at, price_paid, payment_status, " +
                    " payment_ref " +
                    "FROM booking " +
                    "WHERE id = ?";

    /**
     * SELECT di tutte le booking di uno student con JOIN sulla tabella lesson,
     * così da restituire oggetti Lesson completi (startTime, endTime, expertise…)
     * richiesti da StudentContentController per le lesson card.
     * Ordinate per data di prenotazione decrescente.
     */
    @Language("SQL")
    private static final String SQL_FIND_BY_STUDENT =
            "SELECT b.id, b.lesson_id, b.student_username, b.booked_at, b.price_paid, " +
            "       b.payment_status, b.payment_ref, " +
            "       l.tutor_username, l.subcategory_name, l.start_time, l.end_time, " +
            "       l.is_remote, l.listed_price, l.status AS lesson_status, " +
            "       l.created_at AS lesson_created_at " +
            "FROM booking b " +
            "JOIN lesson l ON b.lesson_id = l.id " +
            "WHERE b.student_username = ? " +
            "ORDER BY b.booked_at DESC";

    /**
     * Verifica se lo student ha già una prenotazione attiva (Pending o Paid)
     * il cui orario si sovrappone all'intervallo della nuova lezione.
     * La condizione di sovrapposizione è: existingStart &lt; newEnd AND existingEnd &gt; newStart.
     * I parametri sono: studentUsername, newLessonEnd, newLessonStart.
     */
    @Language("SQL")
    private static final String SQL_HAS_OVERLAPPING_BOOKING =
            "SELECT COUNT(*) " +
            "FROM booking b " +
            "JOIN lesson l ON b.lesson_id = l.id " +
            "WHERE b.student_username = ? " +
            "  AND b.payment_status IN ('Pending', 'Paid') " +
            "  AND l.status NOT IN ('Completed', 'Cancelled') " +
            "  AND l.start_time < ? " +
            "  AND l.end_time   > ?";

    /**
     * SELECT di tutte le booking per le lezioni di un tutor con JOIN su lesson,
     * ordinate per data di inizio lezione crescente.
     * Seleziona le colonne di lesson necessarie a mapBookingWithLesson()
     * per costruire oggetti Lesson semi-completi (startTime, endTime, expertise…).
     * Il campo student_username produce un oggetto Student parziale
     * (solo username, senza name/surname): sufficiente per identificare
     * il contatto, getFullName() degrada a username se name è null.
     */
    @Language("SQL")
    private static final String SQL_FIND_BY_TUTOR =
            "SELECT b.id, b.lesson_id, b.student_username, b.booked_at, " +
            "       b.price_paid, b.payment_status, b.payment_ref, " +
            "       l.tutor_username, l.subcategory_name, l.start_time, l.end_time, " +
            "       l.is_remote, l.listed_price, l.status AS lesson_status, " +
            "       l.created_at AS lesson_created_at " +
            "FROM booking b " +
            "JOIN lesson l ON b.lesson_id = l.id " +
            "WHERE l.tutor_username = ? " +
            "ORDER BY l.start_time ASC";

    // ----------------------------------------------------------------
    // updateStatus
    // ----------------------------------------------------------------

    /**
     * Aggiorna il payment_status di una booking.
     * Usato per segnare una prenotazione come PAID o REFUNDED.
     */
    @Override
    public void updateStatus(Connection conn, int id, PaymentStatus status)
            throws DatabaseException, BookingNotFoundException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            // Conversione esplicita in stringa: MySQL non riconosce enum Java
            ps.setString(1, toDbStatus(status));
            ps.setInt(2, id);
            // executeUpdate() restituisce 0 se nessuna riga corrisponde all'id
            if (ps.executeUpdate() == 0) throw new BookingNotFoundException(id);
        } catch (BookingNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating status: ", e);
        }
    }

    // ----------------------------------------------------------------
    // insertBooking
    // ----------------------------------------------------------------

    /**
     * Inserisce una nuova prenotazione e restituisce l'id generato.
     * booked_at e payment_status vengono impostati automaticamente
     * dai DEFAULT del DB (CURRENT_TIMESTAMP e 'Pending').
     */
    @Override
    public int insertBooking(Connection conn, Booking booking)
            throws DatabaseException {

        try (PreparedStatement ps = conn.prepareStatement(
                SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, booking.getLesson().getId());
            ps.setString(2, booking.getStudent().getUsername());
            ps.setBigDecimal(3, booking.getPricePaid());
            ps.setString(4, booking.getPaymentRef());
            ps.executeUpdate();

            try (ResultSet key = ps.getGeneratedKeys()) {
                if (key.next()) return key.getInt(1);
                // Se non viene restituita la chiave c'è un problema interno
                throw new DatabaseException("No generated ID for the new Booking.");
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error inserting booking: ", e);
        }
    }

    // ----------------------------------------------------------------
    // findByStudent
    // ----------------------------------------------------------------

    /**
     * Carica tutte le prenotazioni di uno student con JOIN sulla tabella lesson.
     * Restituisce oggetti Booking con Lesson completi (startTime, endTime,
     * TutorExpertise parziale con tutor_username e subcategory_name)
     * richiesti da StudentContentController per le lesson card.
     * Restituisce una lista vuota se lo student non ha prenotazioni.
     *
     * @throws DatabaseException per errori JDBC
     */
    @Override
    public List<Booking> findByStudent(Connection conn, String username)
            throws DatabaseException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_STUDENT)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                List<Booking> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapBookingWithLesson(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DatabaseException("System Error: ", e);
        }
    }

    // ----------------------------------------------------------------
    // findByTutor
    // ----------------------------------------------------------------

    /**
     * Carica tutte le booking relative alle lezioni di un tutor,
     * ordinate per data di inizio lezione crescente.
     * Usa mapBookingWithLesson() che costruisce oggetti Lesson semi-completi
     * (startTime, endTime, expertise) tramite il JOIN con la tabella lesson.
     * Lo Student risultante è parziale (solo username).
     */
    @Override
    public List<Booking> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_TUTOR)) {
            ps.setString(1, tutorUsername);
            try (ResultSet rs = ps.executeQuery()) {
                List<Booking> list = new ArrayList<>();
                while (rs.next()) list.add(mapBookingWithLesson(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new DatabaseException("System Error: ", e);
        }
    }

    // ----------------------------------------------------------------
    // selectBooking
    // ----------------------------------------------------------------

    /**
     * Carica una prenotazione per id.
     */
    @Override
    public Booking selectBooking(Connection conn, int id)
            throws DatabaseException, BookingNotFoundException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapBooking(rs);
                throw new BookingNotFoundException(id);
            }
        } catch (BookingNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("System Error: ", e);
        }
    }

    // ----------------------------------------------------------------
    // checkNoDuplicateBooking
    // ----------------------------------------------------------------

    /**
     * Verifica che lo student non abbia già una booking attiva (Pending o Paid)
     * il cui orario si sovrappone all'intervallo [newLessonStart, newLessonEnd).
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
        try (PreparedStatement ps = conn.prepareStatement(SQL_HAS_OVERLAPPING_BOOKING)) {
            ps.setString(1, studentUsername);
            ps.setObject(2, newLessonEnd);    // l.start_time < newLessonEnd
            ps.setObject(3, newLessonStart);  // l.end_time   > newLessonStart
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new DuplicateBookingException(studentUsername, newLessonStart, newLessonEnd);
                }
            }
        } catch (DuplicateBookingException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("Error checking overlapping booking: ", e);
        }
    }

    // ----------------------------------------------------------------
    // Helper privati — mapping ResultSet → Booking
    // ----------------------------------------------------------------

    /**
     * Costruisce una Booking dal ResultSet corrente (query semplice su booking).
     * Lesson e Student sono parziali: contengono solo id/username.
     * Usato da findByTutor() e selectBooking() dove la JOIN con lesson
     * non è presente.
     */
    private Booking mapBooking(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int lessonId = rs.getInt("lesson_id");
        String studentUsername = rs.getString("student_username");
        LocalDateTime bookedAt = rs.getObject("booked_at", LocalDateTime.class);
        BigDecimal pricePaid = rs.getBigDecimal("price_paid");
        PaymentStatus paymentStatus = fromDbStatus(rs.getString("payment_status"));
        String paymentRef = rs.getString("payment_ref");

        Lesson lesson = new Lesson.Builder().id(lessonId).build();
        Student student = new Student.Builder().username(studentUsername).build();

        return new Booking.Builder()
                .id(id)
                .lesson(lesson)
                .student(student)
                .bookedAt(bookedAt)
                .pricePaid(pricePaid)
                .paymentStatus(paymentStatus)
                .paymentRef(paymentRef)
                .build();
    }

    /**
     * Costruisce una Booking dal ResultSet di una query con JOIN su lesson.
     * La Lesson risultante è un oggetto semi-completo con startTime, endTime
     * e TutorExpertise parziale (tutor_username + subcategory_name).
     * Usato da findByStudent() per popolare correttamente le lesson card
     * nella StudentContentController senza query aggiuntive.
     */
    private Booking mapBookingWithLesson(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int lessonId = rs.getInt("lesson_id");
        String studentUsername = rs.getString("student_username");
        LocalDateTime bookedAt = rs.getObject("booked_at", LocalDateTime.class);
        BigDecimal pricePaid = rs.getBigDecimal("price_paid");
        PaymentStatus paymentStatus = fromDbStatus(rs.getString("payment_status"));
        String paymentRef = rs.getString("payment_ref");

        // Campi lesson dalla JOIN
        String tutorUsername    = rs.getString("tutor_username");
        String subcategoryName  = rs.getString("subcategory_name");
        LocalDateTime startTime = rs.getObject("start_time", LocalDateTime.class);
        LocalDateTime endTime   = rs.getObject("end_time",   LocalDateTime.class);
        boolean isRemote        = rs.getBoolean("is_remote");
        BigDecimal listedPrice  = rs.getBigDecimal("listed_price");
        LessonStatus lessonStatus = fromLessonStatus(rs.getString("lesson_status"));
        LocalDateTime createdAt = rs.getObject("lesson_created_at", LocalDateTime.class);

        Tutor partialTutor = new Tutor.Builder().username(tutorUsername).build();
        SubCategory partialSubCat = new SubCategory(subcategoryName, null, null);
        TutorExpertise partialExpertise =
                new TutorExpertise(partialTutor, partialSubCat, listedPrice, null, null);

        Lesson lesson = new Lesson.Builder()
                .id(lessonId)
                .expertise(partialExpertise)
                .startTime(startTime)
                .endTime(endTime)
                .remote(isRemote)
                .listedPrice(listedPrice)
                .lessonStatus(lessonStatus)
                .createdAt(createdAt)
                .build();

        Student student = new Student.Builder().username(studentUsername).build();

        return new Booking.Builder()
                .id(id)
                .lesson(lesson)
                .student(student)
                .bookedAt(bookedAt)
                .pricePaid(pricePaid)
                .paymentStatus(paymentStatus)
                .paymentRef(paymentRef)
                .build();
    }

    /** Converte il valore stringa del DB nell'enum Java LessonStatus. */
    private static LessonStatus fromLessonStatus(String s) {
        return switch (s) {
            case "Available" -> LessonStatus.AVAILABLE;
            case "Booked"    -> LessonStatus.BOOKED;
            case "Completed" -> LessonStatus.COMPLETED;
            case "Cancelled" -> LessonStatus.CANCELLED;
            default -> throw new IllegalArgumentException("Unknown lesson status: " + s);
        };
    }

    // ----------------------------------------------------------------
    // Helper privati — conversione status DB ↔ Java
    // ----------------------------------------------------------------

    /**
     * Converte l'enum Java PaymentStatus nel valore stringa del DB.
     * Il DB usa ENUM('Pending','Paid','Refunded').
     */
    private static String toDbStatus(PaymentStatus paymentStatus) {
        return switch (paymentStatus) {
            case PENDING -> "Pending";
            case PAID -> "Paid";
            case REFUNDED -> "Refunded";
        };
    }

    /**
     * Converte il valore stringa del DB nell'enum Java PaymentStatus.
     *
     * @throws IllegalArgumentException se il valore non è riconosciuto
     */
    private static PaymentStatus fromDbStatus(String s) {
        return switch (s) {
            case "Pending" -> PaymentStatus.PENDING;
            case "Paid" -> PaymentStatus.PAID;
            case "Refunded" -> PaymentStatus.REFUNDED;
            default -> throw new IllegalArgumentException("Unknown DB status: " + s);
        };
    }
}
