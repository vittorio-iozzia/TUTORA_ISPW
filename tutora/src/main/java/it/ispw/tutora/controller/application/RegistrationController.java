package it.ispw.tutora.controller.application;

import it.ispw.tutora.bean.RegistrationBean;
import it.ispw.tutora.dao.StudentDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateUserException;
import it.ispw.tutora.model.Student;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Controller per il use case Registrazione Student.
 *
 * -----------------------------------------------------------------------
 * Responsabilita'
 * -----------------------------------------------------------------------
 * 1. Valida i dati in ingresso dalla bean (completezza e coincidenza password)
 * 2. Costruisce l'oggetto Student tramite Builder
 * 3. Persiste lo Student tramite StudentDao in una transazione atomica
 * 4. Comunica l'esito alla View tramite la bean
 *
 * -----------------------------------------------------------------------
 * Nota sulla connessione
 * -----------------------------------------------------------------------
 * La connessione viene ottenuta tramite DaoFactory.getConnection():
 *   - In modalita' Demo/Json restituisce null senza eccezioni — i DAO
 *     la ignorano e il flusso procede normalmente.
 *   - In modalita' DB restituisce la connessione reale o lancia
 *     DatabaseException se non disponibile.
 * Il try-with-resources chiude automaticamente la connessione alla fine
 * del blocco — anche in caso di eccezione. Se conn e' null non viene
 * chiamata close() (comportamento garantito da Java).
 *
 * -----------------------------------------------------------------------
 * Nota sulla transazione
 * -----------------------------------------------------------------------
 * UserDao.insert() esegue due INSERT atomiche (tabella user + tabella student).
 * Il Controller gestisce esplicitamente commit e rollback come richiesto
 * dal contratto di UserDao. I null-check su conn garantiscono il
 * funzionamento anche in modalita' Demo/Json dove conn e' null.
 *
 * -----------------------------------------------------------------------
 * Nota su active e createdAt
 * -----------------------------------------------------------------------
 * active=true e createdAt=LocalDateTime.now() vengono impostati
 * esplicitamente nel Builder anche se nel DB hanno DEFAULT — nel model
 * Java i DEFAULT del DB non esistono, quindi vanno valorizzati qui.
 */
public class RegistrationController {

    private static final Logger LOGGER = Logger.getLogger(RegistrationController.class.getName());

    private final StudentDao studentDao;

    public RegistrationController() {
        DaoFactory daoFactory = DaoFactory.getInstance();
        this.studentDao = daoFactory.createStudentDao();
    }

    public void register(RegistrationBean bean) {
        // Passo 1: validazione sintattica prima di qualsiasi elaborazione
        if (!bean.isComplete()) {
            bean.setErrorMessage("All fields are required.");
            return;
        }
        if (!bean.isPasswordValid()) {
            bean.setErrorMessage("Password must be at least 8 characters.");
            return;
        }
        if (!bean.passwordsMatch()) {
            bean.setErrorMessage("Passwords do not match.");
            return;
        }
        // Passo 2: hash calcolato solo dopo la validazione
        // BCrypt e' volutamente costoso — non va chiamato prima del necessario
        String passwordHash = BCrypt.hashpw(bean.getPassword(), BCrypt.gensalt());
        // La password in chiaro non serve piu': viene svuotata dalla bean
        bean.clearPassword();
        // Passo 3: costruisci l'oggetto Student
        // active e createdAt vanno impostati esplicitamente nel model anche se il DB li ha come DEFAULT
        // budget inizializzato a zero: un nuovo studente non ha credito al momento della registrazione
        Student student = new Student.Builder()
                .username(bean.getUsername())
                .email(bean.getEmail())
                .name(bean.getName())
                .surname(bean.getSurname())
                .passwordHash(passwordHash)
                .active(true)
                .createdAt(LocalDateTime.now())
                .budget(BigDecimal.ZERO)
                .build();
        // Passo 4: ottieni la connessione e persisti in una transazione atomica
        // UserDao.insert() esegue INSERT in user + INSERT in student: devono essere atomiche
        // null-check su conn: in modalita' Demo/Json getConnection() restituisce null
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            if (conn != null) conn.setAutoCommit(false);
            try {
                studentDao.insert(conn, student);
                if (conn != null) conn.commit();
                bean.setSuccess(true);
            } catch (DuplicateUserException e) {
                safeRollback(conn);
                bean.setErrorMessage("Username or email already present.");
            } catch (DatabaseException | SQLException e) {
                safeRollback(conn);
                bean.setErrorMessage("System Error. Try later.");
            }
        } catch (DatabaseException | SQLException e) {
            bean.setErrorMessage("System Error. Try later.");
        }
    }

    /**
     * Esegue il rollback loggando eventuali errori senza propagarli:
     * se rollback() lanciasse SQLException inghiottirebbe l'eccezione originale del chiamante.
     * Il null-check gestisce la modalita' Demo/Json dove conn e' null.
     */
    private void safeRollback(Connection conn) {
        if (conn == null) return;
        try {
            conn.rollback();
        } catch (SQLException e) {
            LOGGER.warning("Rollback failed: " + e.getMessage());
        }
    }
}