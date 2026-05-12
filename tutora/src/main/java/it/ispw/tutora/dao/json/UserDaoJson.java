package it.ispw.tutora.dao.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.ispw.tutora.dao.UserDao;
import it.ispw.tutora.enums.Role;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateUserException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.Admin;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.User;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementazione di UserDao basata su file JSON.
 * Usata quando DAO_TYPE=JSON in app.properties.
 *
 * -----------------------------------------------------------------------
 * Persistenza
 * -----------------------------------------------------------------------
 * Legge e scrive su ../tutora_data/users.json.
 * Il file contiene tutti i ruoli (Student, Tutor, Admin) in un unico array.
 * Ogni operazione di scrittura applica il pattern read-modify-write.
 *
 * -----------------------------------------------------------------------
 * Polimorfismo Student / Tutor / Admin
 * -----------------------------------------------------------------------
 * Il campo role ("STUDENT", "TUTOR", "ADMIN") discrimina il sottotipo.
 * In lettura, toUser() istanzia la sottoclasse corretta tramite switch.
 * I campi specifici del ruolo (budget per STUDENT, rating/ratingCount per
 * TUTOR) sono sempre presenti nel record JSON ma sono null/0 se non
 * applicabili al ruolo corrente.
 *
 * -----------------------------------------------------------------------
 * Gestione password
 * -----------------------------------------------------------------------
 * updatePassword() aggiorna direttamente il campo passwordHash nel file JSON.
 * Alla successiva chiamata di findByUsername() il nuovo hash è già disponibile.
 * Questo è superiore all'approccio Demo (pendingPasswordHashes in memoria)
 * perché la modifica è persistente attraverso i riavvii dell'applicazione.
 *
 * -----------------------------------------------------------------------
 * Nota sul parametro Connection
 * -----------------------------------------------------------------------
 * Il parametro Connection viene ignorato: non c'è nessun DB.
 * È presente solo per rispettare il contratto dell'interfaccia UserDao,
 * pensata per la gestione delle transazioni JDBC.
 *
 * -----------------------------------------------------------------------
 * Nota sull'ereditarietà
 * -----------------------------------------------------------------------
 * readAll(), writeAll() e UserRecord sono protected per consentire a
 * TutorDaoJson e StudentDaoJson di estendere questa classe senza duplicare la logica
 * di accesso al file, seguendo lo stesso pattern di TutorDaoDb
 * che estende UserDaoDb.
 */
public class UserDaoJson implements UserDao {

    private static final String JSON_PATH = "../tutora_data/users.json";
    private final ObjectMapper mapper = new ObjectMapper();

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    @Override
    public void insert(Connection conn, User user)
            throws DatabaseException, DuplicateUserException {

        List<UserRecord> records = readAll();

        for (UserRecord r : records) {
            if (r.username.equals(user.getUsername())) {
                throw new DuplicateUserException(
                        "User already exists with username: " + user.getUsername());
            }
            if (r.email.equals(user.getEmail())) {
                throw new DuplicateUserException(
                        "User already exists with email: " + user.getEmail());
            }
        }

        records.add(toRecord(user));
        writeAll(records);
    }

    // ----------------------------------------------------------------
    // findByUsername
    // ----------------------------------------------------------------

    @Override
    public User findByUsername(Connection conn, String username)
            throws DatabaseException, UserNotFoundException {

        for (UserRecord r : readAll()) {
            if (r.username.equals(username)) return toUser(r);
        }
        throw new UserNotFoundException(username);
    }

    // ----------------------------------------------------------------
    // updatePassword
    // ----------------------------------------------------------------

    @Override
    public void updatePassword(Connection conn, String username, String newPasswordHash)
            throws DatabaseException, UserNotFoundException {

        List<UserRecord> records = readAll();
        for (UserRecord r : records) {
            if (r.username.equals(username)) {
                r.passwordHash = newPasswordHash;
                writeAll(records);
                return;
            }
        }
        throw new UserNotFoundException(username);
    }

    // ----------------------------------------------------------------
    // updateProfile
    // ----------------------------------------------------------------

    @Override
    public void updateProfile(Connection conn, String username,
                              String description, boolean isActive)
            throws DatabaseException, UserNotFoundException {

        List<UserRecord> records = readAll();
        for (UserRecord r : records) {
            if (r.username.equals(username)) {
                r.description = description;
                r.active = isActive;
                writeAll(records);
                return;
            }
        }
        throw new UserNotFoundException(username);
    }

    // ----------------------------------------------------------------
    // Helper privati / protected
    // ----------------------------------------------------------------

    protected List<UserRecord> readAll() throws DatabaseException {
        try {
            UserRecord[] records = mapper.readValue(new File(JSON_PATH), UserRecord[].class);
            return new ArrayList<>(Arrays.asList(records));
        } catch (IOException e) {
            throw new DatabaseException("Error reading JSON file: " + JSON_PATH, e);
        }
    }

    protected void writeAll(List<UserRecord> records) throws DatabaseException {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(JSON_PATH), records);
        } catch (IOException e) {
            throw new DatabaseException("Error writing JSON file: " + JSON_PATH, e);
        }
    }

    private User toUser(UserRecord r) {
        return switch (Role.valueOf(r.role)) {
            case TUTOR -> new Tutor.Builder()
                    .username(r.username)
                    .email(r.email)
                    .name(r.name)
                    .surname(r.surname)
                    .passwordHash(r.passwordHash)
                    .description(r.description)
                    .active(r.active)
                    .createdAt(LocalDateTime.parse(r.createdAt))
                    .rating(r.rating != null ? new BigDecimal(r.rating) : BigDecimal.ZERO)
                    .ratingCount(r.ratingCount)
                    .build();
            case STUDENT -> new Student.Builder()
                    .username(r.username)
                    .email(r.email)
                    .name(r.name)
                    .surname(r.surname)
                    .passwordHash(r.passwordHash)
                    .description(r.description)
                    .active(r.active)
                    .createdAt(LocalDateTime.parse(r.createdAt))
                    .budget(r.budget != null ? new BigDecimal(r.budget) : BigDecimal.ZERO)
                    .build();
            case ADMIN -> new Admin.Builder()
                    .username(r.username)
                    .email(r.email)
                    .name(r.name)
                    .surname(r.surname)
                    .passwordHash(r.passwordHash)
                    .description(r.description)
                    .active(r.active)
                    .createdAt(LocalDateTime.parse(r.createdAt))
                    .build();
        };
    }

    private UserRecord toRecord(User user) {
        UserRecord r = new UserRecord();
        r.username = user.getUsername();
        r.email = user.getEmail();
        r.name = user.getName();
        r.surname = user.getSurname();
        r.passwordHash = user.getPasswordHash();
        r.role = user.getRole().name();
        r.description = user.getDescription();
        r.active = user.isActive();
        r.createdAt = user.getCreatedAt() != null
                ? user.getCreatedAt().toString()
                : LocalDateTime.now().toString();

        if (user instanceof Tutor tutor) {
            r.rating = tutor.getRating() != null
                    ? tutor.getRating().toPlainString()
                    : "0.00";
            r.ratingCount = tutor.getRatingCount();

        } else if (user instanceof Student student) {
            r.budget = student.getBudget() != null
                    ? student.getBudget().toPlainString()
                    : "0.00";
        }

        return r;
    }

    // ----------------------------------------------------------------
    // POJO interno per la deserializzazione Jackson
    // protected: accessibile da TutorDaoJson e StudentDaoJson
    // ----------------------------------------------------------------

    protected static class UserRecord {
        public String  username;
        public String  email;
        public String  name;
        public String  surname;
        public String  passwordHash;
        public String  role;
        public String  description;
        public boolean active;
        public String  createdAt;

        // Campi specifici del ruolo — null se non applicabili
        public String  budget;       // STUDENT
        public String  rating;       // TUTOR
        public int ratingCount;  // TUTOR
    }
}
