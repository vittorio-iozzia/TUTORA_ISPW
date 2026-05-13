package it.ispw.tutora.controller.application;

import it.ispw.tutora.bean.RegistrationBean;
import it.ispw.tutora.dao.StudentDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateUserException;
import it.ispw.tutora.model.Student;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Controller per il use case Registrazione Student.
 *
 * -----------------------------------------------------------------------
 * Responsabilità
 * -----------------------------------------------------------------------
 * 1. Valida i dati in ingresso dalla bean (completezza e coincidenza password)
 * 2. Costruisce l'oggetto Student tramite Builder
 * 3. Persiste lo Student tramite StudentDao
 * 4. Comunica l'esito alla View tramite la bean
 *
 * -----------------------------------------------------------------------
 * Nota sulla connessione
 * -----------------------------------------------------------------------
 * La connessione viene ottenuta tramite DaoFactory.getConnection():
 *   - In modalità Demo/Json restituisce null senza eccezioni — i DAO
 *     la ignorano e il flusso procede normalmente.
 *   - In modalità DB restituisce la connessione reale o lancia
 *     DatabaseException se non disponibile.
 * Il try-with-resources chiude automaticamente la connessione alla fine
 * del blocco — anche in caso di eccezione. Se conn è null non viene
 * chiamata close() (comportamento garantito da Java).
 *
 * -----------------------------------------------------------------------
 * Nota su active e createdAt
 * -----------------------------------------------------------------------
 * active=true e createdAt=LocalDateTime.now() vengono impostati
 * esplicitamente nel Builder anche se nel DB hanno DEFAULT — nel model
 * Java i DEFAULT del DB non esistono, quindi vanno valorizzati qui.
 */
public class RegistrationController {

    private final StudentDao studentDao;

    public RegistrationController() {
        DaoFactory daoFactory = DaoFactory.getInstance();
        this.studentDao = daoFactory.createStudentDao();
    }
    public void register(RegistrationBean bean) {
        // Passo 1: validazione sintattica prima di qualsiasi elaborazione
        if (!bean.passwordsMatch() || !bean.isComplete()) {
            bean.setErrorMessage("All fields are required and passwords must match.");
            return;
        }
        // Passo 2: hash calcolato solo dopo la validazione
        // BCrypt è volutamente costoso — non va chiamato prima del necessario
        String passwordHash = BCrypt.hashpw(bean.getPassword(), BCrypt.gensalt());
        // La password in chiaro non serve più: viene svuotata dalla bean
        bean.clearPassword();
        // Passo 3: costruisci l'oggetto Student
        // active e createdAt vanno impostati esplicitamente nel model
        // anche se il DB li ha come DEFAULT
        Student student = new Student.Builder()
                .username(bean.getUsername())
                .email(bean.getEmail())
                .name(bean.getName())
                .surname(bean.getSurname())
                .passwordHash(passwordHash)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        // Passo 4: ottieni la connessione e persisti tramite DAO
        // try-with-resources chiude conn automaticamente alla fine del blocco
        // Demo/Json → conn=null, close() non viene chiamata
        // DB        → conn reale, close() chiamata automaticamente
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            studentDao.insert(conn, student);
            bean.setSuccess(true);
        } catch (DuplicateUserException e) {
            bean.setErrorMessage("Username or email already present.");
        } catch (DatabaseException | SQLException e) {
            bean.setErrorMessage("System Error. Try later.");
        }
    }
}
