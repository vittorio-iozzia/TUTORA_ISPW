package it.ispw.tutora.dao.demo;

import it.ispw.tutora.dao.StudentDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateUserException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.User;

import java.math.BigDecimal;
import java.sql.Connection;

/**
 * Implementazione in-memory di StudentDao per l'ambiente demo.
 *
 * Estende UserDaoDemo per la propria cache student-specifica e riceve
 * in costruzione un riferimento alla UserDaoDemo condivisa (quella usata
 * dal LoginController). In questo modo insert() scrive in entrambe le
 * cache: il login trova l'utente in userDao, le operazioni student-specific
 * lo trovano in questa cache.
 *
 * In DB mode la stessa coerenza è garantita da StudentDaoDb.insert()
 * che chiama super.insert() (riga in user) + INSERT in student.
 *
 * La Connection viene accettata nelle firme per rispettare l'interfaccia
 * ma non viene usata: in demo non esiste una transazione reale.
 */
public class StudentDaoDemo extends UserDaoDemo implements StudentDao {

    private final UserDaoDemo sharedUserDao;

    public StudentDaoDemo(UserDaoDemo sharedUserDao) {
        this.sharedUserDao = sharedUserDao;
    }

    /**
     * Aggiorna il budget dello student in cache.
     *
     * Poiché Student non espone setBudget() (rimosso per impedire valori
     * inconsistenti), il nuovo valore viene raggiunto calcolando il delta
     * rispetto al budget attuale e applicando addBudget() o deductBudget().
     *
     * @throws UserNotFoundException se lo username non è in cache o
     *         l'utente trovato non è uno Student
     */
    @Override
    public void updateStudentBudget(Connection conn, String username, BigDecimal budget)
            throws DatabaseException, UserNotFoundException {
        User user = findByUsername(conn, username);
        if (!(user instanceof Student student))
            throw new UserNotFoundException(username);
        // Calcola il delta per raggiungere il valore target senza setBudget().
        BigDecimal diff = budget.subtract(student.getBudget());
        if (diff.compareTo(BigDecimal.ZERO) > 0) student.addBudget(diff);
        else if (diff.compareTo(BigDecimal.ZERO) < 0) student.deductBudget(diff.negate());
    }

    /**
     * Carica lo Student dalla cache verificando che l'utente trovato
     * sia effettivamente uno Student.
     *
     * @throws UserNotFoundException se lo username non è in cache o
     *         l'utente trovato non è uno Student (es. è un Tutor)
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
     * Inserisce uno Student in entrambe le cache:
     *   1. sharedUserDao (UserDaoDemo) → trovato dal LoginController
     *   2. questa cache (StudentDaoDemo) → trovato da selectStudent()
     *
     * Specchia il comportamento di StudentDaoDb.insert() che esegue
     * super.insert() (tabella user) + INSERT in student.
     *
     * @throws IllegalArgumentException se user non è un'istanza di Student
     */
    @Override
    public void insert(Connection conn, User user)
            throws DatabaseException, DuplicateUserException {
        if (!(user instanceof Student))
            throw new IllegalArgumentException("Expected Student instance.");
        sharedUserDao.insert(conn, user);
        super.insert(conn, user);
    }
}
