package it.ispw.tutora.dao.demo;

import it.ispw.tutora.dao.TutorDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateUserException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.User;

import java.sql.Connection;
import java.util.List;

/**
 * Implementazione in memoria di TutorDao.
 * Estende UserDaoDemo per rispecchiare la gerarchia dell'interfaccia
 * (TutorDao extends UserDao) e quella DB (TutorDaoDb extends UserDaoDb),
 * ed eliminare la duplicazione dei metodi comuni.
 *
 * -----------------------------------------------------------------------
 * Nota sulla cache
 * -----------------------------------------------------------------------
 * La cache è ereditata da UserDaoDemo (Map<String, User>).
 * insert() verifica che l'oggetto sia un Tutor prima di delegare a super.
 * selectTutor() recupera l'utente dalla cache ed esegue il cast a Tutor.
 *
 * -----------------------------------------------------------------------
 * Nota
 * -----------------------------------------------------------------------
 * Il parametro Connection viene ignorato — non c'è nessun DB.
 * I dati vivono in memoria per tutta la durata dell'esecuzione
 * e spariscono alla chiusura dell'applicazione.
 * Il ciclo di vita è gestito da DemoDaoFactory — non serve Singleton.
 */
public class TutorDaoDemo extends UserDaoDemo implements TutorDao {

    // ----------------------------------------------------------------
    // insert — override per imporre il tipo Tutor
    // ----------------------------------------------------------------

    @Override
    public void insert(Connection conn, User user)
            throws DatabaseException, DuplicateUserException {
        if (!(user instanceof Tutor)) {
            throw new IllegalArgumentException("Expected Tutor instance.");
        }
        super.insert(conn, user);
    }

    // ----------------------------------------------------------------
    // selectTutor
    // ----------------------------------------------------------------

    @Override
    public Tutor selectTutor(Connection conn, String username)
            throws DatabaseException, UserNotFoundException {
        User user = findByUsername(conn, username);
        if (!(user instanceof Tutor tutor)) {
            throw new UserNotFoundException(username);
        }
        return tutor;
    }

    @Override
    public List<Tutor> selectAllTutors(Connection conn) throws DatabaseException {
        return cache.values().stream()
                .filter(Tutor.class::isInstance)
                .map(Tutor.class::cast)
                .toList();
    }
}
