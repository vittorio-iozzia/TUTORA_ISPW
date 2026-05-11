package it.ispw.tutora.dao.db;

import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.exception.BookingNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Lesson;
import it.ispw.tutora.model.Student;
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
     * SELECT di tutte le booking di uno student ordinate per data decrescente.
     * Restituisce le prenotazioni più recenti per prime.
     */
    @Language("SQL")
    private static final String SQL_FIND_BY_STUDENT =
            "SELECT id, lesson_id, student_username, booked_at, price_paid, payment_status, " +
                    " payment_ref " +
                    "FROM booking " +
                    "WHERE student_username = ? " +
                    "ORDER BY booked_at DESC";

    // ----------------------------------------------------------------
    // updateStatus
    // ----------------------------------------------------------------

    /**
     * Aggiorna il payment_status di una booking.
     * Usato per segnare una prenotazione come PAID o REFUNDED.
     *
     * @throws BookingNotFoundException se l'id non corrisponde
     *         ad alcuna riga in booking
     * @throws DatabaseException        per errori JDBC
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
     *
     * @return id AUTO_INCREMENT assegnato dal DB
     * @throws DatabaseException per errori JDBC
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
     * Carica tutte le prenotazioni di uno student ordinate per
     * data decrescente. Restituisce una lista vuota se non ce ne sono.
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
                // while per scorrere tutte le righe del ResultSet
                while (rs.next()) {
                    list.add(mapBooking(rs));
                }
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
     *
     * @throws BookingNotFoundException se l'id non corrisponde
     *         ad alcuna riga in booking
     * @throws DatabaseException        per errori JDBC
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
        } catch (SQLException e) {
            throw new DatabaseException("System Error: ", e);
        }
    }

    // ----------------------------------------------------------------
    // Helper privato — mapping ResultSet → Booking
    // ----------------------------------------------------------------

    /**
     * Costruisce una Booking dal ResultSet corrente.
     *
     * Oggetti parziali:
     * Lesson e Student vengono costruiti con il solo id/username
     * recuperato dalla tabella booking. I campi aggiuntivi (es. orario
     * della lesson, budget dello student) non vengono caricati.
     * Per oggetti completi il Controller deve eseguire query aggiuntive
     * tramite i DAO appropriati.
     *
     * @param rs ResultSet posizionato sulla riga corrente
     * @return istanza di Booking con Lesson e Student parziali
     */
    private Booking mapBooking(ResultSet rs) throws SQLException {
        int id= rs.getInt("id");
        int lessonId = rs.getInt("lesson_id");
        String studentUsername = rs.getString("student_username");
        LocalDateTime bookedAt = rs.getObject("booked_at", LocalDateTime.class);
        BigDecimal pricePaid = rs.getBigDecimal("price_paid");
        PaymentStatus paymentStatus = fromDbStatus(rs.getString("payment_status"));
        String paymentRef = rs.getString("payment_ref");

        // Oggetti parziali — contengono solo i campi presenti in booking
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
