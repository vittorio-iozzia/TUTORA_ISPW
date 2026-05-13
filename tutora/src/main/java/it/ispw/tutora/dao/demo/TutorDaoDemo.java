package it.ispw.tutora.dao.demo;

import it.ispw.tutora.dao.TutorDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateUserException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.User;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementazione in memoria di TutorDao.
 * Usata al posto di TutorDaoDb quando il DB non è disponibile.
 *
 * -----------------------------------------------------------------------
 * Nota sulla cache
 * -----------------------------------------------------------------------
 * La Map è tipizzata su Tutor (non su User) per evitare cast nelle
 * operazioni Tutor-specifiche. insert() accetta User per conformarsi
 * al contratto di UserDao, ma lancia IllegalArgumentException se
 * l'oggetto passato non è un Tutor — comportamento intenzionale e
 * consistente con il design Table-Per-Subclass.
 *
 * -----------------------------------------------------------------------
 * Nota sul rating
 * -----------------------------------------------------------------------
 * In produzione rating e ratingCount sono aggiornati dai trigger SQL.
 * In modalità demo, setRating() viene chiamato direttamente sull'oggetto
 * in cache quando una recensione demo viene inserita.
 *
 * -----------------------------------------------------------------------
 * Nota
 * -----------------------------------------------------------------------
 * Il parametro Connection viene ignorato — non c'è nessun DB.
 * I dati vivono in memoria per tutta la durata dell'esecuzione
 * e spariscono alla chiusura dell'applicazione.
 * Il ciclo di vita è gestito da DemoDaoFactory — non serve Singleton.
 */
public class TutorDaoDemo implements TutorDao {

    private final Map<String, Tutor> cache = new HashMap<>();
    private final Map<String, String> pendingPasswordHashes = new HashMap<>();

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    @Override
    public void insert(Connection conn, User user)
            throws DatabaseException, DuplicateUserException {

        // 1. verifica che sia un Tutor
        if (!(user instanceof Tutor tutor)) {
            throw new IllegalArgumentException("Expected Tutor instance.");
        }

        // 2. controllo duplicati
        if (cache.containsKey(tutor.getUsername())) {
            throw new DuplicateUserException(
                    "User already exists with username: " + tutor.getUsername());
        }
        for (Tutor existing : cache.values()) {
            if (existing.getEmail().equals(tutor.getEmail())) {
                throw new DuplicateUserException(
                        "User already exists with email: " + tutor.getEmail());
            }
        }

        // 3. inserimento
        cache.put(tutor.getUsername(), tutor);
    }

    // ----------------------------------------------------------------
    // findByUsername
    // ----------------------------------------------------------------

    @Override
    public User findByUsername(Connection conn, String username)
            throws DatabaseException, UserNotFoundException {
        Tutor tutor = cache.get(username);
        if (tutor == null) throw new UserNotFoundException(username);
        return tutor;
    }

    // ----------------------------------------------------------------
    // updatePassword
    // ----------------------------------------------------------------

    @Override
    public void updatePassword(Connection conn, String username,
                               String newPasswordHash)
            throws DatabaseException, UserNotFoundException {
        if (!cache.containsKey(username)) throw new UserNotFoundException(username);
        // La password hashata viene inserita nella Map corrispondente poiché passwordHash è final in User.
        // Limitazione demo: updatePassword() su tutorDao non aggiorna UserDaoDemo.pendingPasswordHashes,
        // quindi il login (che usa userDao) continuerà a usare l'hash originale.
        pendingPasswordHashes.put(username, newPasswordHash);
    }

    // ----------------------------------------------------------------
    // updateProfile
    // ----------------------------------------------------------------

    @Override
    public void updateProfile(Connection conn, String username,
                              String description, boolean isActive)
            throws DatabaseException, UserNotFoundException {
        Tutor tutor = cache.get(username);
        if (tutor == null) throw new UserNotFoundException(username);
        tutor.setDescription(description);
        tutor.setActive(isActive);
    }

    // ----------------------------------------------------------------
    // selectTutor
    // ----------------------------------------------------------------

    @Override
    public Tutor selectTutor(Connection conn, String username)
            throws DatabaseException, UserNotFoundException {
        return (Tutor) findByUsername(conn, username);
    }
}