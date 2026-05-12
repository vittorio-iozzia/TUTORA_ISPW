package it.ispw.tutora.dao.json;

import it.ispw.tutora.dao.StudentDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateUserException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

/**
 * Implementazione JSON di StudentDao.
 *
 * Estende UserDaoJson per ereditare readAll(), writeAll() e UserRecord,
 * evitando di duplicare la logica di accesso al file users.json.
 * La Connection viene accettata nelle firme per rispettare l'interfaccia
 * ma non viene usata: non c'è nessun DB.
 *
 * -----------------------------------------------------------------------
 * Nota su insert
 * -----------------------------------------------------------------------
 * insert() verifica che user sia uno Student prima di delegare a
 * super.insert(). Il ruolo ("STUDENT") viene scritto nel record JSON
 * tramite user.getRole().name(), rendendo il file auto-discriminante.
 */
public class StudentDaoJson extends UserDaoJson implements StudentDao {

    /**
     * Inserisce uno Student nel file JSON.
     * Verifica il tipo prima di delegare a UserDaoJson.insert(),
     * che controlla duplicati su username ed email.
     *
     * @throws IllegalArgumentException se user non è un'istanza di Student
     * @throws DuplicateUserException   se username o email già presenti
     */
    @Override
    public void insert(Connection conn, User user)
            throws DatabaseException, DuplicateUserException {
        if (!(user instanceof Student))
            throw new IllegalArgumentException("Expected Student instance.");
        super.insert(conn, user);
    }

    /**
     * Carica uno Student dal file JSON verificando che il ruolo
     * dell'utente trovato sia effettivamente STUDENT.
     *
     * @throws UserNotFoundException se lo username non esiste o
     *         l'utente trovato non è uno Student
     */
    @Override
    public Student selectStudent(Connection conn, String username)
            throws DatabaseException, UserNotFoundException {
        User user = findByUsername(conn, username);
        if (!(user instanceof Student student))
            throw new UserNotFoundException(username);
        return student;
    }

    /**
     * Aggiorna il campo budget nel record JSON dello student.
     * Applica il pattern read-modify-write sull'intero file users.json.
     *
     * @throws UserNotFoundException se lo username non è presente nel file
     */
    @Override
    public void updateStudentBudget(Connection conn, String username, BigDecimal budget)
            throws DatabaseException, UserNotFoundException {
        List<UserRecord> records = readAll();
        for (UserRecord r : records) {
            if (r.username.equals(username)) {
                r.budget = budget.toPlainString();
                writeAll(records);
                return;
            }
        }
        throw new UserNotFoundException(username);
    }
}
