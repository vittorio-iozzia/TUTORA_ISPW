package it.ispw.tutora.dao.db;

import it.ispw.tutora.dao.TutorDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateUserException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.User;
import org.intellij.lang.annotations.Language;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * Implementazione JDBC di TutorDao.
 * Estende UserDaoDb per ereditare insert(), findByUsername(),
 * updatePassword() e updateProfile() sulla tabella user.
 *
 * -----------------------------------------------------------------------
 * Responsabilità aggiuntive rispetto a UserDaoDb
 * -----------------------------------------------------------------------
 *   insert (override) — INSERT in user (via super) + INSERT in tutor.
 *     rating e rating_count sono omessi: il DB li inizializza a 0
 *     e i trigger li mantengono aggiornati automaticamente.
 *
 *   selectTutor — carica un Tutor COMPLETO tramite JOIN user + tutor,
 *     popolando tutti i campi inclusi rating e ratingCount.
 *     Da usare quando serve l'oggetto Tutor con tutti i suoi dati
 *     (es. visualizzazione profilo, rilettura rating dopo una recensione).
 *
 * -----------------------------------------------------------------------
 * Nota sul rating
 * -----------------------------------------------------------------------
 * rating e rating_count sono aggiornati esclusivamente dai trigger SQL
 * (trg_review_after_insert / update / delete): il layer Java non li
 * scrive mai. selectTutor li rilegge dal DB quando servono.
 *
 * -----------------------------------------------------------------------
 * Nota sul pattern Table-Per-Subclass
 * -----------------------------------------------------------------------
 * La tabella tutor è collegata via FK a user.
 * selectTutor esegue il JOIN per ricostruire un oggetto Tutor completo,
 * mentre findByUsername() (ereditato da UserDaoDb) legge solo user
 * ed è sufficiente per il login.
 */
public class TutorDaoDb extends UserDaoDb implements TutorDao {

    @Language("SQL")
    private static final String SQL_INSERT_TUTOR =
            "INSERT INTO tutor (username) " +
            "VALUES (?)";

    @Language("SQL")
    private static final String SQL_SELECT_TUTOR =
            "SELECT u.*, t.rating, t.rating_count " +
            "FROM tutor t JOIN user u " +
            "ON u.username = t.username " +
            "WHERE u.username = ?";

    /**
     * Persiste un nuovo Tutor: INSERT in user (via super) + INSERT in tutor.
     * Le due INSERT devono essere nella stessa transazione,
     * gestita dal Controller applicativo (non dal DAO).
     */
    @Override
    public void insert(Connection conn, User user)
            throws DatabaseException, DuplicateUserException {

        super.insert(conn, user);

        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_TUTOR)) {
            ps.setString(1, user.getUsername());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error inserting tutor: " + user.getUsername(), e);
        }
    }

    // ----------------------------------------------------------------
    // selectTutor
    // ----------------------------------------------------------------

    /**
     * Carica un Tutor completo tramite JOIN user + tutor.
     * Popola tutti i campi: quelli comuni di user e i campi
     * specifici di tutor (rating, ratingCount).
     *
     * Da preferire a findByUsername() quando serve un oggetto Tutor
     * con tutti i suoi dati (es. visualizzazione profilo, rilettura
     * rating dopo che un trigger SQL lo ha ricalcolato).
     */
    @Override
    public Tutor selectTutor(Connection conn, String username)
            throws DatabaseException, UserNotFoundException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_TUTOR)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new UserNotFoundException(username);
                return mapTutor(rs);
            }
        } catch (UserNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving tutor: " + username, e);
        }
    }

    // ----------------------------------------------------------------
    // Helper privato
    // ----------------------------------------------------------------

    private Tutor mapTutor(ResultSet rs) throws SQLException {
        return new Tutor.Builder()
                .username(rs.getString("username"))
                .email(rs.getString("email"))
                .name(rs.getString("name"))
                .surname(rs.getString("surname"))
                .passwordHash(rs.getString("password_hash"))
                .description(rs.getString("description"))
                .active(rs.getBoolean("is_active"))
                .createdAt(rs.getObject("created_at", LocalDateTime.class))
                .rating(rs.getBigDecimal("rating"))
                .ratingCount(rs.getInt("rating_count"))
                .build();
    }
}
