package it.ispw.tutora.dao.demo;

import it.ispw.tutora.dao.UserDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateUserException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementazione in memoria di UserDao.
 * Usata al posto di UserDaoDb quando il DB non è disponibile.
 *
 * -----------------------------------------------------------------------
 * Nota sulla gestione della password
 * -----------------------------------------------------------------------
 * passwordHash è final in User: updatePassword() non può modificare
 * l'oggetto già in cache. Per simulare l'operazione, il nuovo hash
 * viene conservato in una Map secondaria (pendingPasswordHashes).
 * matchesPassword() sull'oggetto restituito da findByUsername()
 * continuerà a usare l'hash originale — limitazione accettabile
 * per una demo, dove la verifica delle credenziali non è critica.
 *
 * -----------------------------------------------------------------------
 * Nota
 * -----------------------------------------------------------------------
 * Il parametro Connection viene ignorato — non c'è nessun DB.
 * I dati vivono in memoria per tutta la durata dell'esecuzione
 * e spariscono alla chiusura dell'applicazione.
 * Il ciclo di vita è gestito da DemoDaoFactory — non serve Singleton.
 */
public class UserDaoDemo implements UserDao {

    protected final Map<String, User> cache = new HashMap<>();
    private final Map<String, String> pendingPasswordHashes = new HashMap<>();

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    @Override
    public void insert(Connection conn, User user)
            throws DatabaseException, DuplicateUserException {
        if (cache.containsKey(user.getUsername())) {
            throw new DuplicateUserException(
                    "User already exists with username: " + user.getUsername());
        }
        for (User existing : cache.values()) {
            if (existing.getEmail().equals(user.getEmail())) {
                throw new DuplicateUserException(
                        "User already exists with email: " + user.getEmail());
            }
        }
        cache.put(user.getUsername(), user);
    }

    // ----------------------------------------------------------------
    // findByUsername
    // ----------------------------------------------------------------

    @Override
    public User findByUsername(Connection conn, String username)
            throws DatabaseException, UserNotFoundException {
        User user = cache.get(username);
        if (user == null) throw new UserNotFoundException(username);
        return user;
    }

    // ----------------------------------------------------------------
    // updatePassword
    // ----------------------------------------------------------------

    /**
     * Salva il nuovo hash in pendingPasswordHashes.
     * Non modifica l'oggetto in cache perché passwordHash è final in User.
     */
    @Override
    public void updatePassword(Connection conn, String username, String newPasswordHash)
            throws DatabaseException, UserNotFoundException {
        if (!cache.containsKey(username)) throw new UserNotFoundException(username);

        // La password hashata viene inserita nella Map corrispondente poichè l'attributo è final e non può essere modificato
        pendingPasswordHashes.put(username, newPasswordHash);
    }

    // ----------------------------------------------------------------
    // updateProfile
    // ----------------------------------------------------------------

    @Override
    public void updateProfile(Connection conn, String username, String description, boolean isActive)
            throws DatabaseException, UserNotFoundException {
        User existing = cache.get(username);
        if (existing == null) throw new UserNotFoundException(username);
        existing.setDescription(description);
        existing.setActive(isActive);
    }

    // ----------------------------------------------------------------
    // findByEmail
    // ----------------------------------------------------------------

    @Override
    public User findByEmail(Connection conn, String email)
            throws DatabaseException, UserNotFoundException {
        return cache.values().stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst()
                .orElseThrow(() -> new UserNotFoundException("email: " + email));
    }

    // ----------------------------------------------------------------
    // promoteToTutor
    // ----------------------------------------------------------------

    @Override
    public Tutor promoteToTutor(Connection conn, String studentUsername)
            throws DatabaseException, UserNotFoundException {
        User user = cache.get(studentUsername);
        if (!(user instanceof Student student)) throw new UserNotFoundException(studentUsername);
        Tutor tutor = new Tutor.Builder()
                .username(student.getUsername())
                .email(student.getEmail())
                .name(student.getName())
                .surname(student.getSurname())
                .passwordHash(student.getPasswordHash())
                .description(student.getDescription())
                .active(student.isActive())
                .createdAt(student.getCreatedAt())
                .rating(BigDecimal.ZERO)
                .ratingCount(0)
                .build();
        cache.put(studentUsername, tutor);
        return tutor;
    }
}
