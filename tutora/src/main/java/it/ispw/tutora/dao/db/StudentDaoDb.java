package it.ispw.tutora.dao.db;

import it.ispw.tutora.dao.StudentDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.Student;
import org.intellij.lang.annotations.Language;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Implementazione JDBC di StudentDao.
 * Estende UserDaoDb per ereditare insert(), findByUsername(),
 * updatePassword() e updateProfile() sulla tabella user.
 *
 * -----------------------------------------------------------------------
 * Responsabilità aggiuntive rispetto a UserDaoDb
 * -----------------------------------------------------------------------
 * Questa classe aggiunge due operazioni specifiche dello Student:
 *
 *   updateStudentBudget — aggiorna il solo campo budget nella
 *     tabella student, senza toccare la tabella user.
 *     Chiamato dal Controller dopo ogni operazione finanziaria
 *     (prenotazione, rimborso) nell'ambito della stessa transazione.
 *
 *   selectStudent — carica uno Student COMPLETO tramite JOIN
 *     user + student, popolando tutti i campi incluso budget.
 *     Da usare quando serve l'oggetto Student con tutti i suoi dati
 *     (es. visualizzazione profilo, controllo budget prima del pagamento).
 *
 * -----------------------------------------------------------------------
 * Nota sul pattern Table-Per-Subclass
 * -----------------------------------------------------------------------
 * Student ha la propria tabella figlia (student) collegata via FK
 * a user. selectStudent esegue il JOIN per ricostruire un oggetto
 * Student completo, mentre findByUsername() (ereditato da UserDaoDb)
 * legge solo user ed è sufficiente per il login.
 */
public class StudentDaoDb extends UserDaoDb implements StudentDao {

    /** UPDATE del solo campo budget nella tabella student. */
    @Language("SQL")
    private static final String SQL_UPDATEBUDGET =
            "UPDATE student " +
            "SET budget = ? " +
            "WHERE username = ?";

    /**
     * SELECT con JOIN user + student per ricostruire uno Student completo.
     * u.* è accettabile perché la tabella user non contiene colonne
     * pesanti (nessun BLOB): tutte le colonne servono a mapStudent().
     * Filtra per PK (username): restituisce al più una riga.
     */
    @Language("SQL")
    private static final String SQL_SELECT =
            "SELECT u.*, s.budget " +
            "FROM student s JOIN user u " +
            "ON u.username = s.username " +
            "WHERE u.username = ?";

    // ----------------------------------------------------------------
    // updateStudentBudget
    // ----------------------------------------------------------------

    /**
     * Aggiorna il budget dello studente nella tabella student.
     *
     * Il valore budget deve essere >= 0: il vincolo CHECK nel DB
     * (chk_student_budget) lo garantisce a livello di storage.
     * La logica di dominio (deductBudget / addBudget) su Student
     * garantisce la correttezza prima di arrivare al DAO.
     */
    @Override
    public void updateStudentBudget(Connection conn, String username, BigDecimal budget)
            throws DatabaseException, UserNotFoundException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATEBUDGET)) {
            ps.setBigDecimal(1, budget);
            ps.setString(2, username);

            // executeUpdate() restituisce 0 se nessuna riga è stata aggiornata:
            // significa che lo username non esiste nella tabella student
            if (ps.executeUpdate() == 0) throw new UserNotFoundException("Unknown user.");
        } catch (SQLException e) {
            throw new DatabaseException("System Error. Try later.");
        }
    }

    // ----------------------------------------------------------------
    // selectStudent
    // ----------------------------------------------------------------

    /**
     * Carica uno Student completo tramite JOIN user + student.
     * Popola tutti i campi: quelli comuni di user (username, email,
     * name, surname, passwordHash, description, isActive, createdAt)
     * e il campo specifico di student (budget).
     *
     * Da preferire a findByUsername() quando serve un oggetto Student
     * con tutti i suoi dati (es. visualizzazione profilo, controllo
     * budget prima di autorizzare un pagamento).
     */
    @Override
    public Student selectStudent(Connection conn, String username)
            throws DatabaseException, UserNotFoundException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new UserNotFoundException("Unknown user.");
                return mapStudent(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("System Error. Try later.");
        }
    }

    // ----------------------------------------------------------------
    // Helper privato — mapping ResultSet → Student completo
    // ----------------------------------------------------------------

    /**
     * Costruisce uno Student completo dal ResultSet corrente.
     * Il ResultSet deve contenere sia le colonne di user che il
     * campo budget di student (prodotto dal JOIN in SQL_SELECT).
     */
    private Student mapStudent(ResultSet rs) throws SQLException {
        String username = rs.getString("username");
        BigDecimal budget = rs.getBigDecimal("budget");
        String email = rs.getString("email");
        String name = rs.getString("name");
        String surname = rs.getString("surname");
        String description = rs.getString("description");
        String passwordHash = rs.getString("password_hash");
        boolean isActive = rs.getBoolean("is_active");
        LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);

        return new Student.Builder()
                .username(username)
                .budget(budget)
                .email(email)
                .name(name)
                .surname(surname)
                .description(description)
                .passwordHash(passwordHash)
                .createdAt(createdAt)
                .active(isActive)
                .build();
    }
}
