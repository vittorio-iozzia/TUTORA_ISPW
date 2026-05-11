package it.ispw.tutora.dao.db;

import it.ispw.tutora.dao.TutorExpertiseDao;
import it.ispw.tutora.enums.Status;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateTutorExpertiseException;
import it.ispw.tutora.exception.TutorExpertiseNotFoundException;
import it.ispw.tutora.model.SubCategory;
import it.ispw.tutora.model.Tag;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.TutorExpertise;
import org.intellij.lang.annotations.Language;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementazione JDBC di TutorExpertiseDao.
 *
 * Gestisce due tabelle correlate:
 *   - tutor_expertise (tutor_username PK+FK, subcategory_name PK+FK,
 *                      hourly_price, status, created_at)
 *   - expertise_tag   (tutor_username FK, subcategory_name FK, tag_name)
 *
 * Le query di lettura usano una JOIN tra le due tabelle per recuperare
 * in un unico round-trip sia i dati dell'expertise che i tag associati.
 * Ogni riga del ResultSet corrisponde a un singolo tag: mapExpertise()
 * mappa una riga → un oggetto TutorExpertise con quel tag aggiunto.
 *
 * La gestione delle transazioni (commit/rollback) è delegata al Controller
 * applicativo: i metodi ricevono la Connection come parametro.
 */
public class TutorExpertiseDaoDb implements TutorExpertiseDao {

    @Language("SQL")
    private static final String SQL_UPDATE =
            "UPDATE tutor_expertise t JOIN expertise_tag e ON t.tutor_username=e.tutor_username " +
                    "AND t.subcategory_name=e.subcategory_name " +
                    "SET t.hourly_price=?, e.tag_name=? " +
                    "WHERE t.tutor_username=? AND t.subcategory_name=? ";

    @Language("SQL")
    private static final String SQL_UPDATESTATUS =
            "UPDATE tutor_expertise " +
                    "SET status=? " +
                    "WHERE tutor_username=? AND subcategory_name=? ";

    @Language("SQL")
    private static final String SQL_INSERT =
            "INSERT INTO tutor_expertise (tutor_username, subcategory_name, hourly_price) " +
                    "VALUES (?, ?, ?)";

    @Language("SQL")
    private static final String SQL_INSERT_TAG =
            "INSERT INTO expertise_tag (tutor_username, subcategory_name, tag_name) " +
                    "VALUES (?, ?, ?)";

    @Language("SQL")
    private static final String SQL_SELECT =
            "SELECT t.tutor_username, t.subcategory_name, t.hourly_price, t.status, " +
                    "t.created_at, e.tag_name " +
                    "FROM tutor_expertise t JOIN expertise_tag e ON t.tutor_username=e.tutor_username " +
                    "AND t.subcategory_name=e.subcategory_name " +
                    "WHERE t.tutor_username=? AND t.subcategory_name=?";

    @Language("SQL")
    private static final String SQL_FIND_BY_TUTOR =
            "SELECT t.tutor_username, t.subcategory_name, t.hourly_price, t.status, " +
                    "t.created_at, e.tag_name " +
                    "FROM tutor_expertise t JOIN expertise_tag e ON t.tutor_username=e.tutor_username " +
                    "AND t.subcategory_name=e.subcategory_name " +
                    "WHERE t.tutor_username=? " +
                    "ORDER BY t.subcategory_name ASC";

    /**
     * Inserisce una nuova expertise e i relativi tag in batch.
     * Il batch su expertise_tag garantisce un unico round-trip verso il DB
     * per tutti i tag dell'expertise.
     *
     * @throws DuplicateTutorExpertiseException se la coppia (tutor, subcategory)
     *         viola il UNIQUE constraint del DB
     */
    @Override
    public void insertExpertise(Connection conn, TutorExpertise expertise)
            throws DatabaseException, DuplicateTutorExpertiseException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, expertise.getTutor().getUsername());
            ps.setString(2, expertise.getSubcategory().getName());
            ps.setBigDecimal(3, expertise.getHourlyPrice());
            ps.executeUpdate();
            try (PreparedStatement preparedStatement = conn.prepareStatement(SQL_INSERT_TAG)) {
                preparedStatement.setString(1, expertise.getTutor().getUsername());
                preparedStatement.setString(2, expertise.getSubcategory().getName());
                for (Tag tag : expertise.getExpertiseTags()) {
                    preparedStatement.setString(3, tag.getName());
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
            } catch (SQLIntegrityConstraintViolationException e) {
                throw new DuplicateTutorExpertiseException("Tutor Expertise already present.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("System Error: ", e);
        }
    }

    /**
     * Aggiorna hourly_price e tag di un'expertise esistente.
     * L'UPDATE in JOIN modifica entrambe le tabelle in un'unica query.
     *
     * @throws TutorExpertiseNotFoundException se nessuna riga viene aggiornata
     */
    @Override
    public void updateExpertise(Connection conn, TutorExpertise expertise)
            throws DatabaseException, TutorExpertiseNotFoundException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setBigDecimal(1, expertise.getHourlyPrice());
            for (Tag tag : expertise.getExpertiseTags()) {
                ps.setString(2, tag.getName());
                ps.setString(3, expertise.getTutor().getUsername());
                ps.setString(4, expertise.getSubcategory().getName());
            }
            if (ps.executeUpdate() == 0) throw new TutorExpertiseNotFoundException("Tutor Expertise not found.");
        } catch (SQLException e) {
            throw new DatabaseException("System Error: ", e);
        }
    }

    /**
     * Aggiorna solo la colonna status di un'expertise esistente.
     *
     * @throws TutorExpertiseNotFoundException se nessuna riga viene aggiornata
     */
    @Override
    public void updateExpertiseStatus(Connection conn, TutorExpertise tutorExpertise)
            throws DatabaseException, TutorExpertiseNotFoundException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATESTATUS)) {
            ps.setString(1, toDbStatus(tutorExpertise.getStatus()));
            ps.setString(2, tutorExpertise.getTutor().getUsername());
            ps.setString(3, tutorExpertise.getSubcategory().getName());
            if (ps.executeUpdate() == 0) throw new TutorExpertiseNotFoundException("Tutor Expertise not found.");
        } catch (SQLException e) {
            throw new DatabaseException("System Error: ", e);
        }
    }

    /**
     * Carica tutte le expertise del tutor ordinate per subcategory_name.
     * La JOIN restituisce una riga per tag: ogni riga produce un oggetto
     * TutorExpertise con il proprio tag aggiunto tramite mapExpertise().
     *
     * Restituisce una lista vuota se il tutor non ha expertise registrate.
     */
    @Override
    public List<TutorExpertise> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_TUTOR)) {
            ps.setString(1, tutorUsername);
            try (ResultSet rs = ps.executeQuery()) {
                List<TutorExpertise> list = new ArrayList<>();
                while (rs.next()) list.add(mapExpertise(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new DatabaseException("System Error: ", e);
        }
    }

    /**
     * Carica una singola expertise per chiave composita (tutor, subcategory).
     *
     * @throws TutorExpertiseNotFoundException se nessuna riga corrisponde
     */
    @Override
    public TutorExpertise selectExpertise(Connection conn, String tutorUsername, String subcategoryName)
            throws DatabaseException, TutorExpertiseNotFoundException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT)) {
            ps.setString(1, tutorUsername);
            ps.setString(2, subcategoryName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapExpertise(rs);
            }
            throw new TutorExpertiseNotFoundException("Tutor Expertise not found.");
        } catch (SQLException e) {
            throw new DatabaseException("System Error: ", e);
        }
    }

    /**
     * Converte lo Status enum nella stringa attesa dalla colonna DB.
     */
    private static String toDbStatus(Status status) {
        return switch (status) {
            case PENDING  -> "Pending";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
        };
    }
    /** Converte la stringa DB nello Status enum corrispondente. */
    private static Status fromDbStatus(String s) {
        return switch (s) {
            case "Pending"  -> Status.PENDING;
            case "Approved" -> Status.APPROVED;
            case "Rejected" -> Status.REJECTED;
            default -> throw new IllegalArgumentException("Unknown DB status: " + s);
        };
    }

    /**
     * Mappa una singola riga del ResultSet in un oggetto TutorExpertise.
     * Ogni riga contiene un tag: il tag viene aggiunto all'expertise tramite addTag().
     */
    private TutorExpertise mapExpertise(ResultSet rs) throws SQLException {
        String tutorUsername  = rs.getString("tutor_username");
        String subcategoryName = rs.getString("subcategory_name");
        BigDecimal hourlyPrice = rs.getBigDecimal("hourly_price");
        Status status  = fromDbStatus(rs.getString("status"));
        LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
        Tag tag = new Tag(rs.getString("tag_name"));
        Tutor tutor = new Tutor.Builder().username(tutorUsername).build();
        SubCategory subCategory = new SubCategory(subcategoryName, null, null);
        TutorExpertise expertise = new TutorExpertise(tutor, subCategory, hourlyPrice, status, createdAt);
        expertise.addTag(tag);
        return expertise;
    }
}
