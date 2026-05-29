package it.ispw.tutora.dao.db;

import it.ispw.tutora.dao.TutorExpertiseDao;
import it.ispw.tutora.enums.Status;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateTutorExpertiseException;
import it.ispw.tutora.exception.TutorExpertiseNotFoundException;
import it.ispw.tutora.model.Category;
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

    /** Aggiorna solo il prezzo orario nella tabella tutor_expertise. */
    @Language("SQL")
    private static final String SQL_UPDATE_PRICE =
            "UPDATE tutor_expertise " +
                    "SET hourly_price = ? " +
                    "WHERE tutor_username = ? AND subcategory_name = ?";

    /** Elimina tutti i tag esistenti prima di reinserirli (DELETE + INSERT pattern). */
    @Language("SQL")
    private static final String SQL_DELETE_TAGS =
            "DELETE FROM expertise_tag " +
                    "WHERE tutor_username = ? AND subcategory_name = ?";

    /**
     * Assicura che il tag esista nella tabella tag prima di inserirlo in expertise_tag.
     * INSERT IGNORE è idempotente: non fa nulla se il tag è già presente.
     */
    @Language("SQL")
    private static final String SQL_INSERT_TAG_LOOKUP =
            "INSERT IGNORE INTO tag (name) VALUES (?)";

    @Language("SQL")
    private static final String SQL_UPDATESTATUS =
            "UPDATE tutor_expertise " +
                    "SET status=? " +
                    "WHERE tutor_username=? AND subcategory_name=? ";

    @Language("SQL")
    private static final String SQL_INSERT =
            "INSERT INTO tutor_expertise (tutor_username, subcategory_name, hourly_price, status) " +
                    "VALUES (?, ?, ?, ?)";

    @Language("SQL")
    private static final String SQL_INSERT_TAG =
            "INSERT INTO expertise_tag (tutor_username, subcategory_name, tag_name) " +
                    "VALUES (?, ?, ?)";

    @Language("SQL")
    private static final String SQL_SELECT =
            "SELECT t.tutor_username, t.subcategory_name, t.hourly_price, t.status, " +
                    "t.created_at, e.tag_name, s.category_name " +
                    "FROM tutor_expertise t " +
                    "LEFT JOIN expertise_tag e ON t.tutor_username=e.tutor_username " +
                    "  AND t.subcategory_name=e.subcategory_name " +
                    "LEFT JOIN subcategory s ON t.subcategory_name=s.name " +
                    "WHERE t.tutor_username=? AND t.subcategory_name=?";

    @Language("SQL")
    private static final String SQL_FIND_BY_TUTOR =
            "SELECT t.tutor_username, t.subcategory_name, t.hourly_price, t.status, " +
                    "t.created_at, e.tag_name, s.category_name " +
                    "FROM tutor_expertise t " +
                    "LEFT JOIN expertise_tag e ON t.tutor_username=e.tutor_username " +
                    "  AND t.subcategory_name=e.subcategory_name " +
                    "LEFT JOIN subcategory s ON t.subcategory_name=s.name " +
                    "WHERE t.tutor_username=? " +
                    "ORDER BY t.subcategory_name ASC";

    /**
     * Inserisce una nuova expertise e i relativi tag in batch.
     * Prima dell'INSERT su expertise_tag assicura che ogni tag esista
     * nella tabella tag (INSERT IGNORE), evitando FK violations se
     * l'utente inserisce tag personalizzati non ancora presenti.
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
            ps.setString(4, toDbStatus(expertise.getStatus()));
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateTutorExpertiseException("Tutor Expertise already present.");
        } catch (SQLException e) {
            throw new DatabaseException("System Error: ", e);
        }
        try {
            ensureTagsExist(conn, expertise);
            insertTags(conn, expertise);
        } catch (SQLException e) {
            throw new DatabaseException("System Error: ", e);
        }
    }

    /**
     * Assicura che ogni tag dell'expertise esista nella tabella tag.
     * INSERT IGNORE è idempotente: non modifica i tag già presenti.
     * Necessario prima di ogni INSERT in expertise_tag per rispettare
     * il vincolo FK fk_etag_tag → tag(name).
     * Se la lista di tag è vuota, non esegue nessuna query.
     */
    private void ensureTagsExist(Connection conn, TutorExpertise expertise)
            throws SQLException {
        List<Tag> tags = expertise.getExpertiseTags();
        if (tags.isEmpty()) return;
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_TAG_LOOKUP)) {
            for (Tag tag : tags) {
                ps.setString(1, tag.getName());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Inserisce le associazioni (tutor, subcategory, tag) in expertise_tag
     * tramite batch. Presuppone che i tag esistano già nella tabella tag
     * (chiamare ensureTagsExist prima di questo metodo).
     * Se la lista di tag è vuota, non esegue nessuna query.
     */
    private void insertTags(Connection conn, TutorExpertise expertise)
            throws SQLException {
        List<Tag> tags = expertise.getExpertiseTags();
        if (tags.isEmpty()) return;
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_TAG)) {
            ps.setString(1, expertise.getTutor().getUsername());
            ps.setString(2, expertise.getSubcategory().getName());
            for (Tag tag : tags) {
                ps.setString(3, tag.getName());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Aggiorna hourly_price e tag di un'expertise esistente.
     *
     * Strategia in tre passi (nella stessa transazione gestita dal Controller):
     *   1. UPDATE tutor_expertise: aggiorna hourly_price; se nessuna riga
     *      corrisponde lancia TutorExpertiseNotFoundException.
     *   2. DELETE expertise_tag: rimuove tutti i tag esistenti per questa
     *      expertise (ON DELETE CASCADE garantisce l'integrità referenziale).
     *   3. INSERT tag + INSERT expertise_tag in batch: reinserisce i nuovi tag.
     *      INSERT IGNORE INTO tag garantisce che il tag esista nella tabella
     *      di lookup prima di referenziarlo in expertise_tag (evita FK violation).
     *
     * Il pattern DELETE + INSERT è preferibile al JOIN UPDATE perché:
     *   - supporta aggiunta/rimozione arbitraria di tag (non solo rinomina)
     *   - non dipende dalla cardinalità dei tag (funziona con 0, 1 o N tag)
     *   - è semanticamente più chiaro (replace totale dei tag)
     *
     * @throws TutorExpertiseNotFoundException se l'expertise non esiste
     * @throws DatabaseException               per errori JDBC
     */
    @Override
    public void updateExpertise(Connection conn, TutorExpertise expertise)
            throws DatabaseException, TutorExpertiseNotFoundException {

        String tutorUsername   = expertise.getTutor().getUsername();
        String subcategoryName = expertise.getSubcategory().getName();

        // Passo 1: aggiorna hourly_price
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PRICE)) {
            ps.setBigDecimal(1, expertise.getHourlyPrice());
            ps.setString(2, tutorUsername);
            ps.setString(3, subcategoryName);
            if (ps.executeUpdate() == 0)
                throw new TutorExpertiseNotFoundException("Tutor Expertise not found.");
        } catch (TutorExpertiseNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating expertise price for "
                    + tutorUsername + "/" + subcategoryName, e);
        }

        // Passo 2: elimina tutti i tag esistenti
        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_TAGS)) {
            ps.setString(1, tutorUsername);
            ps.setString(2, subcategoryName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting existing tags for "
                    + tutorUsername + "/" + subcategoryName, e);
        }

        // Passo 3: reinserisce i nuovi tag (INSERT IGNORE su tag + batch su expertise_tag)
        try {
            ensureTagsExist(conn, expertise);
            insertTags(conn, expertise);
        } catch (SQLException e) {
            throw new DatabaseException("Error reinserting tags for "
                    + tutorUsername + "/" + subcategoryName, e);
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
        } catch (TutorExpertiseNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("System Error: ", e);
        }
    }

    /**
     * Carica tutte le expertise del tutor ordinate per subcategory_name.
     *
     * La LEFT JOIN restituisce una riga per tag (o una riga con tag_name NULL
     * se l'expertise non ha tag). Il ResultSet è ordinato per subcategory_name,
     * quindi le righe della stessa expertise sono contigue: aggrega tutti i tag
     * in un unico oggetto TutorExpertise per subcategory, evitando duplicati.
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
                TutorExpertise current = null;
                while (rs.next()) {
                    String subName = rs.getString("subcategory_name");
                    if (current == null || !current.getSubcategory().getName().equals(subName)) {
                        current = mapExpertiseBase(rs);
                        list.add(current);
                    }
                    String tagName = rs.getString("tag_name");
                    if (tagName != null) current.addTag(new Tag(tagName));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DatabaseException("System Error: ", e);
        }
    }

    /**
     * Carica una singola expertise per chiave composita (tutor, subcategory).
     * Accumula tutti i tag dalle righe della LEFT JOIN prima di restituire.
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
                TutorExpertise expertise = null;
                while (rs.next()) {
                    if (expertise == null) expertise = mapExpertiseBase(rs);
                    String tagName = rs.getString("tag_name");
                    if (tagName != null) expertise.addTag(new Tag(tagName));
                }
                if (expertise == null) throw new TutorExpertiseNotFoundException("Tutor Expertise not found.");
                return expertise;
            }
        } catch (TutorExpertiseNotFoundException e) {
            throw e;
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
     * Costruisce un TutorExpertise dai campi scalari della riga corrente,
     * senza aggiungere tag. I tag vengono aggiunti dal chiamante dopo
     * aver iterato tutte le righe della LEFT JOIN per la stessa expertise.
     */
    private TutorExpertise mapExpertiseBase(ResultSet rs) throws SQLException {
        String tutorUsername   = rs.getString("tutor_username");
        String subcategoryName = rs.getString("subcategory_name");
        BigDecimal hourlyPrice = rs.getBigDecimal("hourly_price");
        Status status          = fromDbStatus(rs.getString("status"));
        LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
        String categoryName    = rs.getString("category_name");

        Tutor tutor         = new Tutor.Builder().username(tutorUsername).build();
        // parentCategory è necessario per filterByCategory() (confronto per nome categoria)
        Category parentCat  = categoryName != null ? new Category(categoryName, "") : null;
        SubCategory subCategory = new SubCategory(subcategoryName, parentCat, null);
        return new TutorExpertise(tutor, subCategory, hourlyPrice, status, createdAt);
    }
}
