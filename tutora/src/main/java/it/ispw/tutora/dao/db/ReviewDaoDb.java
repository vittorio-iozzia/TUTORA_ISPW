package it.ispw.tutora.dao.db;

import it.ispw.tutora.dao.ReviewDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateReviewException;
import it.ispw.tutora.exception.ReviewNotFoundException;
import it.ispw.tutora.model.*;
import org.intellij.lang.annotations.Language;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementazione JDBC di ReviewDao.
 *
 * -----------------------------------------------------------------------
 * Nota sui trigger rating
 * -----------------------------------------------------------------------
 * INSERT, UPDATE e DELETE su review aggiornano automaticamente
 * tutor.rating e tutor.rating_count tramite i trigger SQL
 * trg_review_after_insert / update / delete.
 * Questo DAO non ricalcola mai il rating manualmente: è sufficiente
 * eseguire l'operazione su review e il trigger farà il resto.
 *
 * -----------------------------------------------------------------------
 * Nota sul controllo duplicati
 * -----------------------------------------------------------------------
 * La tabella review ha il vincolo UNIQUE uq_review_booking su booking_id:
 * per ogni booking può esistere al massimo una recensione.
 * checkOverlap() verifica questo vincolo prima dell'INSERT tramite
 * SELECT COUNT(*) per dare un messaggio di errore più chiaro rispetto
 * alla SQLIntegrityConstraintViolationException del DB.
 *
 * -----------------------------------------------------------------------
 * Nota sul mapping parziale
 * -----------------------------------------------------------------------
 * mapReview() costruisce oggetti Booking, Student e Tutor parziali,
 * contenenti solo i campi presenti nella tabella review (id/username).
 * Per oggetti completi il Controller deve eseguire query aggiuntive
 * tramite i DAO appropriati.
 */
public class ReviewDaoDb implements ReviewDao {

    // ----------------------------------------------------------------
    // Costanti SQL
    // ----------------------------------------------------------------

    /**
     * INSERT di una nuova recensione.
     * created_at usa il DEFAULT CURRENT_TIMESTAMP definito nel DB.
     */
    @Language("SQL")
    private static final String SQL_INSERT =
            "INSERT INTO review (booking_id, student_username, tutor_username, rating, comment) " +
                    "VALUES (?, ?, ?, ?, ?)";

    /** UPDATE di rating e comment per una recensione esistente. */
    @Language("SQL")
    private static final String SQL_UPDATE =
            "UPDATE review " +
                    "SET rating = ?, comment = ? " +
                    "WHERE id = ?";

    /** DELETE fisico di una recensione per id. */
    @Language("SQL")
    private static final String SQL_DELETE =
            "DELETE FROM review " +
                    "WHERE id = ?";

    /**
     * SELECT di una recensione per id con colonne esplicite.
     * Non usa SELECT * per stabilità rispetto a future modifiche dello schema.
     */
    @Language("SQL")
    private static final String SQL_SELECT =
            "SELECT id, booking_id, student_username, tutor_username, rating, comment, created_at " +
                    "FROM review " +
                    "WHERE id = ?";

    /**
     * SELECT di tutte le recensioni di un tutor ordinate per data decrescente.
     * Restituisce le recensioni più recenti per prime.
     */
    @Language("SQL")
    private static final String SQL_FIND_BY_TUTOR =
            "SELECT id, booking_id, student_username, tutor_username, rating, comment, created_at " +
                    "FROM review " +
                    "WHERE tutor_username = ? " +
                    "ORDER BY created_at DESC";

    /**
     * Controllo duplicati: verifica che non esista già una recensione
     * per la stessa booking. COUNT(*) restituisce sempre una riga
     * (0 se non trovato, >0 se esiste già).
     */
    @Language("SQL")
    private static final String SQL_CHECK_DUPLICATE =
            "SELECT COUNT(*) " +
                    "FROM review " +
                    "WHERE booking_id = ?";

    // ----------------------------------------------------------------
    // insertReview
    // ----------------------------------------------------------------

    /**
     * Persiste una nuova recensione.
     * Prima dell'INSERT verifica tramite checkOverlap() che non esista
     * già una recensione per la stessa booking.
     * Il trigger trg_review_after_insert aggiorna automaticamente
     * rating e rating_count del tutor.
     *
     * @return id AUTO_INCREMENT assegnato dal DB
     * @throws DuplicateReviewException se esiste già una recensione
     *         per la stessa booking
     * @throws DatabaseException        per errori JDBC
     */
    @Override
    public int insertReview(Connection conn, Review rev)
            throws DatabaseException, DuplicateReviewException {

        // Passo 1: verifica che non esista già una recensione per questa booking
        checkOverlap(conn, rev);

        // Passo 2: esegui l'INSERT e recupera la chiave generata
        try (PreparedStatement ps = conn.prepareStatement(
                SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, rev.getBooking().getId());
            ps.setString(2, rev.getStudent().getUsername());
            ps.setString(3, rev.getTutor().getUsername());
            ps.setInt(4, rev.getRating());
            ps.setString(5, rev.getComment());
            ps.executeUpdate();

            try (ResultSet key = ps.getGeneratedKeys()) {
                if (key.next()) return key.getInt(1);
                // Se non viene restituita la chiave c'è un problema interno
                throw new DatabaseException("No generated ID error.");
            }

        } catch (SQLException e) {
            throw new DatabaseException("System Error. Try later.");
        }
    }

    // ----------------------------------------------------------------
    // updateReview
    // ----------------------------------------------------------------

    /**
     * Aggiorna rating e comment di una recensione esistente.
     * Il trigger trg_review_after_update ricalcola automaticamente
     * il rating del tutor.
     *
     * @throws ReviewNotFoundException se l'id non corrisponde
     *         ad alcuna riga in review
     * @throws DatabaseException       per errori JDBC
     */
    @Override
    public void updateReview(Connection conn, Review rev)
            throws DatabaseException, ReviewNotFoundException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setInt(1, rev.getRating());
            ps.setString(2, rev.getComment());
            ps.setInt(3, rev.getId());
            // executeUpdate() restituisce 0 se nessuna riga corrisponde all'id
            if (ps.executeUpdate() == 0) throw new ReviewNotFoundException(rev.getId());
        } catch (ReviewNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("System Error. Try later.");
        }
    }

    // ----------------------------------------------------------------
    // deleteReview
    // ----------------------------------------------------------------

    /**
     * Elimina fisicamente una recensione.
     * Il trigger trg_review_after_delete ricalcola automaticamente
     * rating e rating_count del tutor — non serve aggiornarlo manualmente.
     *
     * @throws ReviewNotFoundException se l'id non corrisponde
     *         ad alcuna riga in review
     * @throws DatabaseException       per errori JDBC
     */
    @Override
    public void deleteReview(Connection conn, int id)
            throws DatabaseException, ReviewNotFoundException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            // executeUpdate() restituisce 0 se nessuna riga corrisponde all'id
            if (ps.executeUpdate() == 0) throw new ReviewNotFoundException(id);
        } catch (ReviewNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("System Error. Try later.");
        }
    }

    // ----------------------------------------------------------------
    // findByTutor
    // ----------------------------------------------------------------

    /**
     * Carica tutte le recensioni di un tutor ordinate per data decrescente.
     * Restituisce una lista vuota se il tutor non ha recensioni:
     * l'assenza di recensioni non è un errore ma un caso legittimo.
     *
     * @throws DatabaseException per errori JDBC
     */
    @Override
    public List<Review> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException {

        List<Review> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_TUTOR)) {
            ps.setString(1, tutorUsername);
            try (ResultSet rs = ps.executeQuery()) {
                // while per scorrere tutte le righe del ResultSet
                while (rs.next()) {
                    list.add(mapReview(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("System Error. Try later.");
        }
        return list;
    }

    // ----------------------------------------------------------------
    // selectReview
    // ----------------------------------------------------------------

    /**
     * Carica una recensione per id.
     *
     * @throws ReviewNotFoundException se l'id non corrisponde
     *         ad alcuna riga in review
     * @throws DatabaseException       per errori JDBC
     */
    @Override
    public Review selectReview(Connection conn, int id)
            throws DatabaseException, ReviewNotFoundException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ReviewNotFoundException(id);
                return mapReview(rs);
            }
        } catch (ReviewNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("System Error. Try later.");
        }
    }

    // ----------------------------------------------------------------
    // Helper privato — controllo duplicati
    // ----------------------------------------------------------------

    /**
     * Verifica che non esista già una recensione per la stessa booking.
     * COUNT(*) restituisce sempre una riga: 0 se non esiste, >0 se esiste.
     * Usato da insertReview() prima dell'INSERT.
     *
     * @throws DuplicateReviewException se esiste già una recensione
     *         per la stessa booking
     * @throws DatabaseException        per errori JDBC
     */
    private void checkOverlap(Connection conn, Review review)
            throws DuplicateReviewException, DatabaseException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_CHECK_DUPLICATE)) {
            ps.setInt(1, review.getBooking().getId());
            try (ResultSet rs = ps.executeQuery()) {
                // getInt(1): legge la prima colonna della riga, che è COUNT(*)
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new DuplicateReviewException(review.getId());
                }
            }
        } catch (DuplicateReviewException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("System Error. Try later.");
        }
    }

    // ----------------------------------------------------------------
    // Helper privato — mapping ResultSet → Review
    // ----------------------------------------------------------------

    /**
     * Costruisce una Review dal ResultSet corrente.
     * ⚠ Oggetti parziali:
     * Booking, Student e Tutor vengono costruiti con il solo id/username
     * recuperato dalla tabella review. I campi aggiuntivi (es. budget
     * dello student, rating del tutor, prezzo della booking) non vengono
     * caricati. Per oggetti completi il Controller deve eseguire query
     * aggiuntive tramite i DAO appropriati.
     *
     * @param rs ResultSet posizionato sulla riga corrente
     * @return istanza di Review con Booking, Student e Tutor parziali
     */
    private Review mapReview(ResultSet rs) throws SQLException {
        int id              = rs.getInt("id");
        int bookingId       = rs.getInt("booking_id");
        String studentUsername = rs.getString("student_username");
        String tutorUsername   = rs.getString("tutor_username");
        int rating          = rs.getInt("rating");
        String comment      = rs.getString("comment");
        LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);

        // Oggetti parziali — contengono solo i campi presenti in review
        Booking partialBooking = new Booking.Builder()
                .id(bookingId)
                .build();
        Student partialStudent = new Student.Builder()
                .username(studentUsername)
                .build();
        Tutor partialTutor = new Tutor.Builder()
                .username(tutorUsername)
                .build();

        return new Review.Builder()
                .id(id)
                .booking(partialBooking)
                .student(partialStudent)
                .tutor(partialTutor)
                .rating(rating)
                .comment(comment)
                .createdAt(createdAt)
                .build();
    }
}
