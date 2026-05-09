package it.ispw.tutora.dao.db;

import it.ispw.tutora.dao.LessonDao;
import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateLessonException;
import it.ispw.tutora.exception.LessonNotFoundException;
import it.ispw.tutora.model.Lesson;
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
 * Implementazione JDBC di LessonDao.
 *
 * -----------------------------------------------------------------------
 * Nota sull'anti-overlap
 * -----------------------------------------------------------------------
 * Il DB non impedisce sovrapposizioni temporali tra lezioni dello stesso
 * tutor (MySQL non supporta indici parziali su range di date).
 * La guardia anti-overlap è implementata in questo DAO con una SELECT
 * COUNT(*) prima di ogni INSERT e UPDATE. L'indice idx_lesson_overlap_check
 * definito nel DB copre esattamente questa query rendendola O(log n).
 *
 * Due intervalli [A_start, A_end] e [B_start, B_end] si sovrappongono se:
 *   A_start < B_end  AND  A_end > B_start
 *
 * -----------------------------------------------------------------------
 * Nota sulla conversione dello status
 * -----------------------------------------------------------------------
 * Il DB memorizza lo status come ENUM('Available','Booked','Completed','Cancelled').
 * I metodi toDbStatus / fromDbStatus gestiscono la conversione bidirezionale
 * rispetto all'enum Java LessonStatus (AVAILABLE, BOOKED, COMPLETED, CANCELLED).
 * Non si usa setObject() direttamente sull'enum Java perché MySQL non lo
 * riconosce — serve sempre la conversione esplicita in stringa.
 *
 * -----------------------------------------------------------------------
 * Nota sul mapping di TutorExpertise in selectLesson
 * -----------------------------------------------------------------------
 * La tabella lesson memorizza solo tutor_username e subcategory_name.
 * Per ricostruire un oggetto TutorExpertise completo (con Tutor e SubCategory)
 * sarebbe necessario un JOIN con tutor_expertise, user e subcategory.
 * In questa implementazione selectLesson restituisce una Lesson con un
 * TutorExpertise parziale contenente solo i dati presenti nella tabella lesson.
 * Se serve un oggetto completo il Controller deve eseguire una query aggiuntiva
 * tramite il DAO appropriato.
 */
public class LessonDaoDb implements LessonDao {

    // ----------------------------------------------------------------
    // Costanti SQL
    // ----------------------------------------------------------------

    /**
     * UPDATE di start_time, end_time e is_remote per una lezione esistente.
     * Filtra anche per tutor_username oltre che per id come ulteriore
     * garanzia di sicurezza: un tutor non può modificare lezioni altrui.
     */
    @Language("SQL")
    private static final String SQL_UPDATE =
            "UPDATE lesson " +
                    "SET start_time = ?, end_time = ?, is_remote = ? " +
                    "WHERE id = ? AND tutor_username = ?";

    /**
     * INSERT di una nuova lezione.
     * status e created_at usano i DEFAULT definiti nel DB
     * (rispettivamente 'Available' e CURRENT_TIMESTAMP).
     */
    @Language("SQL")
    private static final String SQL_INSERT =
            "INSERT INTO lesson (tutor_username, subcategory_name, start_time, end_time, " +
                    "                    is_remote, listed_price) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

    /**
     * SELECT di una lezione per id con colonne esplicite.
     * Non usa SELECT * per stabilità rispetto a future modifiche dello schema.
     */
    @Language("SQL")
    private static final String SQL_SELECT =
            "SELECT id, tutor_username, subcategory_name, start_time, end_time, " +
                    "       is_remote, listed_price, status, created_at " +
                    "FROM lesson " +
                    "WHERE id = ?";

    /**
     * SELECT di tutte le lezioni di un tutor ordinate per data crescente.
     * Usata per visualizzare il calendario del tutor.
     */
    @Language("SQL")
    private static final String SQL_FIND_BY_TUTOR =
            "SELECT id, tutor_username, subcategory_name, start_time, end_time, " +
                    "       is_remote, listed_price, status, created_at " +
                    "FROM lesson " +
                    "WHERE tutor_username = ? " +
                    "ORDER BY start_time ASC";

    /**
     * SELECT di tutte le lezioni di un tutor filtrate per status.
     * Usata ad esempio per vedere solo le lezioni AVAILABLE.
     */
    @Language("SQL")
    private static final String SQL_FIND_BY_TUTOR_AND_STATUS =
            "SELECT id, tutor_username, subcategory_name, start_time, end_time, " +
                    "       is_remote, listed_price, status, created_at " +
                    "FROM lesson " +
                    "WHERE tutor_username = ? AND status = ? " +
                    "ORDER BY start_time ASC";

    /** UPDATE del solo campo status. */
    @Language("SQL")
    private static final String SQL_UPDATE_STATUS =
            "UPDATE lesson " +
                    "SET status = ? " +
                    "WHERE id = ?";

    /**
     * Guardia anti-overlap per UPDATE: conta le lezioni del tutor
     * che si sovrappongono al nuovo intervallo, escludendo la lezione
     * stessa (AND id != ?) per evitare che si auto-sovrapponga.
     * Ignora le lezioni CANCELLED perché non occupano più slot.
     */
    @Language("SQL")
    private static final String SQL_CHECK_OVERLAP_UPDATE =
            "SELECT COUNT(*) FROM lesson " +
                    "WHERE tutor_username = ? " +
                    "  AND status != 'Cancelled' " +
                    "  AND start_time < ? " +
                    "  AND end_time   > ? " +
                    "  AND id != ?";

    /**
     * Guardia anti-overlap per INSERT: conta le lezioni del tutor
     * che si sovrappongono al nuovo intervallo.
     * Ignora le lezioni CANCELLED perché non occupano più slot.
     */
    @Language("SQL")
    private static final String SQL_CHECK_OVERLAP_INSERT =
            "SELECT COUNT(*) FROM lesson " +
                    "WHERE tutor_username = ? " +
                    "  AND status != 'Cancelled' " +
                    "  AND start_time < ? " +
                    "  AND end_time   > ?";

    // ----------------------------------------------------------------
    // updateLesson
    // ----------------------------------------------------------------

    /**
     * Aggiorna start_time, end_time e is_remote di una lezione esistente.
     *
     * Prima di aggiornare esegue la guardia anti-overlap escludendo
     * la lezione stessa dal controllo (AND id != ?) per evitare
     * che si auto-sovrapponga con il proprio vecchio intervallo.
     *
     * @throws DuplicateLessonException se il nuovo intervallo si sovrappone
     *         a un'altra lezione attiva dello stesso tutor
     * @throws LessonNotFoundException  se l'id non corrisponde ad alcuna lezione
     * @throws DatabaseException        per errori JDBC
     */
    @Override
    public void updateLesson(Connection conn, Lesson lesson)
            throws DatabaseException, LessonNotFoundException, DuplicateLessonException {

        // Passo 1: controllo anti-overlap (SELECT COUNT)
        checkOverlapForUpdate(conn, lesson);

        // Passo 2: esegui l'UPDATE
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setObject(1, lesson.getStartTime());
            ps.setObject(2, lesson.getEndTime());
            ps.setBoolean(3, lesson.isRemote());
            ps.setInt(4, lesson.getId());
            ps.setString(5, lesson.getExpertise().getTutor().getUsername());
            // executeUpdate() restituisce 0 se nessuna riga corrisponde a id + tutor_username
            if (ps.executeUpdate() == 0) throw new LessonNotFoundException(lesson.getId());
        } catch (SQLException e) {
            throw new DatabaseException("System Error. Try later.");
        }
    }

    // ----------------------------------------------------------------
    // insertLesson
    // ----------------------------------------------------------------

    /**
     * Inserisce una nuova lezione.
     *
     * Prima dell'INSERT esegue la guardia anti-overlap per verificare
     * che il tutor non abbia già lezioni attive nello stesso intervallo.
     *
     * @return id AUTO_INCREMENT assegnato dal DB
     * @throws DuplicateLessonException se l'intervallo si sovrappone
     *         a un'altra lezione attiva dello stesso tutor
     * @throws DatabaseException        per errori JDBC
     */
    @Override
    public int insertLesson(Connection conn, Lesson newLesson)
            throws DatabaseException, DuplicateLessonException {

        // Passo 1: controllo anti-overlap (SELECT COUNT)
        checkOverlapForInsert(conn, newLesson);

        // Passo 2: esegui l'INSERT e recupera la chiave generata
        try (PreparedStatement ps = conn.prepareStatement(
                SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, newLesson.getExpertise().getTutor().getUsername());
            ps.setString(2, newLesson.getExpertise().getSubcategory().getName());
            ps.setObject(3, newLesson.getStartTime());
            ps.setObject(4, newLesson.getEndTime());
            ps.setBoolean(5, newLesson.isRemote());
            ps.setBigDecimal(6, newLesson.getListedPrice());
            ps.executeUpdate();

            try (ResultSet key = ps.getGeneratedKeys()) {
                if (key.next()) return key.getInt(1);
                throw new DatabaseException("No generated ID error.");
            }

        } catch (SQLException e) {
            throw new DatabaseException("System Error. Try later.");
        }
    }

    // ----------------------------------------------------------------
    // selectLesson
    // ----------------------------------------------------------------

    /**
     * Carica una lezione per id.
     * Il TutorExpertise restituito è parziale: contiene solo
     * tutor_username e subcategory_name letti dalla tabella lesson.
     * Per un TutorExpertise completo il Controller deve eseguire
     * una query aggiuntiva tramite il DAO appropriato.
     *
     * @throws LessonNotFoundException se l'id non corrisponde ad alcuna lezione
     * @throws DatabaseException       per errori JDBC
     */
    @Override
    public Lesson selectLesson(Connection conn, int id)
            throws DatabaseException, LessonNotFoundException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new LessonNotFoundException(id);
                return mapLesson(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("System Error. Try later.");
        }
    }

    // ----------------------------------------------------------------
    // findByTutor
    // ----------------------------------------------------------------

    /**
     * Carica tutte le lezioni di un tutor ordinate per data crescente.
     * Usata per visualizzare il calendario completo del tutor.
     *
     * @throws DatabaseException per errori JDBC
     */
    @Override
    public List<Lesson> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException {

        return queryList(conn, SQL_FIND_BY_TUTOR, tutorUsername, null);
    }

    // ----------------------------------------------------------------
    // findByTutorAndStatus
    // ----------------------------------------------------------------

    /**
     * Carica le lezioni di un tutor filtrate per status.
     * Usata ad esempio per vedere solo le lezioni AVAILABLE o BOOKED.
     *
     * @throws DatabaseException per errori JDBC
     */
    @Override
    public List<Lesson> findByTutorAndStatus(Connection conn, String tutorUsername,
                                             LessonStatus status)
            throws DatabaseException {

        return queryList(conn, SQL_FIND_BY_TUTOR_AND_STATUS, tutorUsername, status);
    }

    // ----------------------------------------------------------------
    // updateStatus
    // ----------------------------------------------------------------

    /**
     * Aggiorna lo status di una lezione.
     * La validazione della transizione di stato (macchina a stati finiti)
     * è responsabilità del model (Lesson.updateLessonStatus()).
     * Il DAO si limita a persistere il nuovo stato già validato.
     *
     * @throws LessonNotFoundException se l'id non corrisponde ad alcuna lezione
     * @throws DatabaseException       per errori JDBC
     */
    @Override
    public void updateStatus(Connection conn, int id, LessonStatus newStatus)
            throws DatabaseException, LessonNotFoundException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {
            // Conversione esplicita in stringa: MySQL non riconosce enum Java
            ps.setString(1, toDbStatus(newStatus));
            ps.setInt(2, id);
            if (ps.executeUpdate() == 0) throw new LessonNotFoundException(id);
        } catch (SQLException e) {
            throw new DatabaseException("System Error. Try later.");
        }
    }

    // ----------------------------------------------------------------
    // Helper privati — guardie anti-overlap
    // ----------------------------------------------------------------

    /**
     * Verifica che il nuovo intervallo non si sovrapponga ad altre lezioni
     * attive dello stesso tutor, escludendo la lezione stessa dal controllo.
     * Usata da updateLesson().
     *
     * Due intervalli si sovrappongono se: start_A < end_B AND end_A > start_B.
     * Quindi i parametri sono: end del nuovo slot e start del nuovo slot.
     *
     * @throws DuplicateLessonException se viene rilevata una sovrapposizione
     * @throws DatabaseException        per errori JDBC
     */
    private void checkOverlapForUpdate(Connection conn, Lesson lesson)
            throws DatabaseException, DuplicateLessonException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_CHECK_OVERLAP_UPDATE)) {
            ps.setString(1, lesson.getExpertise().getTutor().getUsername());
            ps.setObject(2, lesson.getEndTime());    // start_time < end del nuovo slot
            ps.setObject(3, lesson.getStartTime());  // end_time   > start del nuovo slot
            ps.setInt(4, lesson.getId());            // escludi la lezione stessa
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new DuplicateLessonException(lesson.getId());
                }
            }
        } catch (DuplicateLessonException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("System Error. Try later.");
        }
    }

    /**
     * Verifica che il nuovo intervallo non si sovrapponga ad altre lezioni
     * attive dello stesso tutor. Usata da insertLesson().
     *
     * @throws DuplicateLessonException se viene rilevata una sovrapposizione
     * @throws DatabaseException        per errori JDBC
     */
    private void checkOverlapForInsert(Connection conn, Lesson newLesson)
            throws DatabaseException, DuplicateLessonException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_CHECK_OVERLAP_INSERT)) {
            ps.setString(1, newLesson.getExpertise().getTutor().getUsername());
            ps.setObject(2, newLesson.getEndTime());    // start_time < end del nuovo slot
            ps.setObject(3, newLesson.getStartTime());  // end_time   > start del nuovo slot
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new DuplicateLessonException(
                            "Lesson already present for this range of time.");
                }
            }
        } catch (DuplicateLessonException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("System Error. Try later.");
        }
    }

    // ----------------------------------------------------------------
    // Helper privato — query lista lezioni
    // ----------------------------------------------------------------

    /**
     * Esegue una query che restituisce una lista di lezioni.
     * Se status è null esegue la query senza filtro su status (findByTutor),
     * altrimenti aggiunge il filtro (findByTutorAndStatus).
     */
    private List<Lesson> queryList(Connection conn, String sql,
                                   String tutorUsername, LessonStatus status)
            throws DatabaseException {

        List<Lesson> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tutorUsername);
            if (status != null) ps.setString(2, toDbStatus(status));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapLesson(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("System Error. Try later.");
        }
        return results;
    }

    // ----------------------------------------------------------------
    // Helper privato — mapping ResultSet → Lesson
    // ----------------------------------------------------------------

    /**
     * Costruisce una Lesson dal ResultSet corrente.
     * Il TutorExpertise è parziale: contiene solo i dati presenti
     * nella tabella lesson (tutor_username e subcategory_name).
     * Per un oggetto completo il Controller deve eseguire query aggiuntive.
     *
     * @param rs ResultSet posizionato sulla riga corrente
     */
    private Lesson mapLesson(ResultSet rs) throws SQLException {
        int id                  = rs.getInt("id");
        String tutorUsername    = rs.getString("tutor_username");
        String subcategoryName  = rs.getString("subcategory_name");
        LocalDateTime startTime = rs.getObject("start_time", LocalDateTime.class);
        LocalDateTime endTime   = rs.getObject("end_time", LocalDateTime.class);
        boolean isRemote        = rs.getBoolean("is_remote");
        BigDecimal listedPrice  = rs.getBigDecimal("listed_price");
        LessonStatus status     = fromDbStatus(rs.getString("status"));
        LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);

        // TutorExpertise parziale: solo i campi recuperabili da lesson
        // senza JOIN aggiuntivi. Tutor e SubCategory sono oggetti minimi
        // con il solo username/name valorizzato.
        Tutor partialTutor = new Tutor.Builder()
                .username(tutorUsername)
                .build();
        SubCategory partialSubCategory = new SubCategory(subcategoryName, null, null);
        TutorExpertise partialExpertise = new TutorExpertise(
                partialTutor, partialSubCategory, listedPrice, null, createdAt);

        return new Lesson.Builder()
                .id(id)
                .expertise(partialExpertise)
                .startTime(startTime)
                .endTime(endTime)
                .remote(isRemote)
                .listedPrice(listedPrice)
                .lessonStatus(status)
                .createdAt(createdAt)
                .build();
    }

    // ----------------------------------------------------------------
    // Helper privati — conversione status DB ↔ Java
    // ----------------------------------------------------------------

    /**
     * Converte l'enum Java LessonStatus nel valore stringa del DB.
     * Il DB usa ENUM('Available','Booked','Completed','Cancelled').
     */
    private static String toDbStatus(LessonStatus status) {
        return switch (status) {
            case AVAILABLE  -> "Available";
            case BOOKED     -> "Booked";
            case COMPLETED  -> "Completed";
            case CANCELLED  -> "Cancelled";
        };
    }

    /**
     * Converte il valore stringa del DB nell'enum Java LessonStatus.
     *
     * @throws IllegalArgumentException se il valore non è riconosciuto
     */
    private static LessonStatus fromDbStatus(String s) {
        return switch (s) {
            case "Available"  -> LessonStatus.AVAILABLE;
            case "Booked"     -> LessonStatus.BOOKED;
            case "Completed"  -> LessonStatus.COMPLETED;
            case "Cancelled"  -> LessonStatus.CANCELLED;
            default -> throw new IllegalArgumentException("Unknown DB status: " + s);
        };
    }
}
