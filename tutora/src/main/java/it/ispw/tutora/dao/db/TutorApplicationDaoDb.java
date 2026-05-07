package it.ispw.tutora.dao.db;

import it.ispw.tutora.dao.TutorApplicationDao;
import it.ispw.tutora.enums.ApplicationStatus;
import it.ispw.tutora.exception.ApplicationNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateApplicationException;
import it.ispw.tutora.model.TutorApplication;
import org.intellij.lang.annotations.Language;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementazione JDBC di TutorApplicationDao.
 *
 * -----------------------------------------------------------------------
 * Nota su active_key
 * -----------------------------------------------------------------------
 * Il vincolo UNIQUE uq_one_active_application è su (category_name, active_key).
 * Per le application attive (DRAFT / SUBMITTED) active_key = studentUsername,
 * impedendo duplicati per la stessa coppia studente+categoria.
 * Alla chiusura (ACCEPTED / REJECTED) active_key diventa CONCAT(id, '_closed'),
 * liberando il vincolo per eventuali future candidature dello stesso studente.
 *
 * -----------------------------------------------------------------------
 * Nota sulla conversione dello status
 * -----------------------------------------------------------------------
 * Il DB memorizza lo status come ENUM('Draft','Submitted','Accepted','Rejected').
 * I metodi toDbStatus / fromDbStatus gestiscono la conversione bidirezionale
 * rispetto all'enum Java ApplicationStatus (DRAFT, SUBMITTED, ACCEPTED, REJECTED).
 */
public class TutorApplicationDaoDb implements TutorApplicationDao {

    @Language("SQL")
    private static final String SQL_INSERT =
            "INSERT INTO tutor_application (category_name, student_username, active_key, status) " +
            "VALUES (?, ?, ?, ?)";

    @Language("SQL")
    private static final String SQL_UPDATE_STATUS =
            "UPDATE tutor_application SET status = ? WHERE id = ?";

    @Language("SQL")
    private static final String SQL_UPDATE_STATUS_TERMINAL =
            "UPDATE tutor_application " +
            "SET status = ?, active_key = CONCAT(id, '_closed'), admin_notes = ?, evaluated_at = ? " +
            "WHERE id = ?";

    @Language("SQL")
    private static final String SQL_FIND_BY_ID =
            "SELECT id, category_name, student_username, creation_date, " +
            "status, admin_notes, evaluated_at " +
            "FROM tutor_application " +
            "WHERE id = ?";

    @Language("SQL")
    private static final String SQL_FIND_BY_STUDENT =
            "SELECT id, category_name, student_username, creation_date, " +
            "status, admin_notes, evaluated_at " +
            "FROM tutor_application " +
            "WHERE student_username = ? " +
            "ORDER BY creation_date DESC";

    @Language("SQL")
    private static final String SQL_FIND_BY_STATUS =
            "SELECT id, category_name, student_username, creation_date, " +
            "status, admin_notes, evaluated_at " +
            "FROM tutor_application " +
            "WHERE status = ? " +
            "ORDER BY creation_date ASC";

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    @Override
    public int insert(Connection conn, TutorApplication application)
            throws DatabaseException, DuplicateApplicationException {

        try (PreparedStatement ps = conn.prepareStatement(
                SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, application.getCategoryName());
            ps.setString(2, application.getStudentUsername());
            ps.setString(3, application.getStudentUsername()); // active_key per DRAFT
            ps.setString(4, toDbStatus(application.getStatus()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("No generated ID for the new application.");
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateApplicationException(
                    application.getStudentUsername(), application.getCategoryName());
        } catch (SQLException e) {
            throw new DatabaseException("Error inserting application.", e);
        }
    }

    // ----------------------------------------------------------------
    // updateStatus
    // ----------------------------------------------------------------

    @Override
    public void updateStatus(Connection conn, TutorApplication application)
            throws DatabaseException, ApplicationNotFoundException {

        String sql = application.getStatus().isTerminal()
                ? SQL_UPDATE_STATUS_TERMINAL
                : SQL_UPDATE_STATUS;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            if (application.getStatus().isTerminal()) {
                ps.setString(1, toDbStatus(application.getStatus()));
                ps.setString(2, application.getAdminNotes());
                ps.setObject(3, application.getEvaluatedAt());
                ps.setInt(4, application.getId());
            } else {
                ps.setString(1, toDbStatus(application.getStatus()));
                ps.setInt(2, application.getId());
            }

            if (ps.executeUpdate() == 0) {
                throw new ApplicationNotFoundException(application.getId());
            }

        } catch (ApplicationNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error updating status for application id=" + application.getId(), e);
        }
    }

    // ----------------------------------------------------------------
    // findById
    // ----------------------------------------------------------------

    @Override
    public TutorApplication findById(Connection conn, int id)
            throws DatabaseException, ApplicationNotFoundException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ApplicationNotFoundException(id);
                return mapApplication(rs);
            }
        } catch (ApplicationNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving application id=" + id, e);
        }
    }

    // ----------------------------------------------------------------
    // findByStudent
    // ----------------------------------------------------------------

    @Override
    public List<TutorApplication> findByStudent(Connection conn, String studentUsername)
            throws DatabaseException {

        return queryList(conn, SQL_FIND_BY_STUDENT, studentUsername);
    }

    // ----------------------------------------------------------------
    // findByStatus
    // ----------------------------------------------------------------

    @Override
    public List<TutorApplication> findByStatus(Connection conn, ApplicationStatus status)
            throws DatabaseException {

        return queryList(conn, SQL_FIND_BY_STATUS, toDbStatus(status));
    }

    // ----------------------------------------------------------------
    // Helper privati
    // ----------------------------------------------------------------

    private List<TutorApplication> queryList(Connection conn, String sql, String param)
            throws DatabaseException {

        List<TutorApplication> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapApplication(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving applications.", e);
        }
        return results;
    }

    private TutorApplication mapApplication(ResultSet rs) throws SQLException {
        TutorApplication app = new TutorApplication(
                rs.getInt("id"),
                rs.getString("category_name"),
                rs.getString("student_username"),
                rs.getObject("creation_date", LocalDateTime.class),
                fromDbStatus(rs.getString("status"))
        );
        app.setAdminNotes(rs.getString("admin_notes"));
        app.setEvaluatedAt(rs.getObject("evaluated_at", LocalDateTime.class));
        return app;
    }

    private static String toDbStatus(ApplicationStatus status) {
        return switch (status) {
            case DRAFT     -> "Draft";
            case SUBMITTED -> "Submitted";
            case ACCEPTED  -> "Accepted";
            case REJECTED  -> "Rejected";
        };
    }

    private static ApplicationStatus fromDbStatus(String s) {
        return switch (s) {
            case "Draft"     -> ApplicationStatus.DRAFT;
            case "Submitted" -> ApplicationStatus.SUBMITTED;
            case "Accepted"  -> ApplicationStatus.ACCEPTED;
            case "Rejected"  -> ApplicationStatus.REJECTED;
            default -> throw new IllegalArgumentException("Unknown DB status: " + s);
        };
    }
}
