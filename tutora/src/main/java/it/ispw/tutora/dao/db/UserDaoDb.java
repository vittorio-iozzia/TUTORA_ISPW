package it.ispw.tutora.dao.db;

import it.ispw.tutora.dao.UserDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateUserException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.Admin;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.User;
import org.intellij.lang.annotations.Language;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * Implementazione JDBC astratta di UserDao.
 *
 * -----------------------------------------------------------------------
 * Perché è astratta
 * -----------------------------------------------------------------------
 * UserDaoDb non può essere istanziata direttamente perché non sa come
 * inserire la riga nella tabella figlia (student, tutor, admin).
 * Le sottoclassi concrete (StudentDaoDb, TutorDaoDb, AdminDaoDb)
 * fanno l'override di insert() per aggiungere quella riga,
 * poi delegano a super.insert() per la riga in user.
 *
 * -----------------------------------------------------------------------
 * Responsabilità di questa classe
 * -----------------------------------------------------------------------
 * Gestisce le operazioni sulla sola tabella user:
 *   - insert         → riga in user (la riga figlia è compito della sottoclasse)
 *   - findByUsername → usato principalmente per il login; restituisce
 *                      un oggetto User con i soli campi di user.
 *                      I campi specifici del ruolo (budget, rating…)
 *                      vengono caricati dalle sottoclassi DAO.
 *   - updatePassword → aggiorna password_hash
 *   - updateProfile  → aggiorna description e is_active
 *
 * -----------------------------------------------------------------------
 * Nota sul pattern Table-Per-Subclass
 * -----------------------------------------------------------------------
 * Lo schema usa Table-Per-Subclass: ogni ruolo ha la propria tabella
 * (student, tutor, admin) collegata via FK a user.
 * findByUsername legge solo la tabella user e restituisce il
 * sottotipo corretto (Student, Tutor o Admin) basandosi sulla
 * colonna role, ma senza i campi specifici del ruolo.
 * Questo è sufficiente per il login: matchesPassword() opera solo
 * su passwordHash che sta in user.
 */
public abstract class UserDaoDb implements UserDao {

    @Language("SQL")
    private static final String SQL_REGISTRATION =
            "INSERT INTO user (username, email, name, surname, password_hash, role, description) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    @Language("SQL")
    private static final String SQL_CHECK =
            "SELECT * " +
            "FROM user " +
            "WHERE username = ?";

    @Language("SQL")
    private static final String SQL_FIND_BY_EMAIL =
            "SELECT * " +
            "FROM user " +
            "WHERE email = ?";

    @Language("SQL")
    private static final String SQL_UPDATEPASS =
            "UPDATE user " +
            "SET password_hash = ? " +
            "WHERE username = ?";

    @Language("SQL")
    private static final String SQL_UPDATE =
            "UPDATE user " +
            "SET description = ?, is_active = ? " +
            "WHERE username = ?";

    @Language("SQL")
    private static final String SQL_PROMOTE_ROLE =
            "UPDATE user SET role = 'TUTOR' WHERE username = ?";

    @Language("SQL")
    private static final String SQL_FIND_ADMIN =
            "SELECT username FROM user WHERE UPPER(role) = 'ADMIN' LIMIT 1";

    @Language("SQL")
    private static final String SQL_INSERT_TUTOR =
            "INSERT INTO tutor (username, rating, rating_count) VALUES (?, 0, 0)";

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    /**
     * Persiste la riga nella tabella base user.
     *
     * Le sottoclassi concrete fanno override di questo metodo per
     * inserire anche la riga nella propria tabella figlia:
     *   super.insert(conn, user);   // riga in user
     *   // poi INSERT in student/tutor/admin
     *
     * Le due INSERT devono essere nella stessa transazione,
     * gestita dal Controller applicativo (non dal DAO).
     */
    @Override
    public void insert(Connection conn, User user)
            throws DatabaseException, DuplicateUserException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_REGISTRATION)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getName());
            ps.setString(4, user.getSurname());
            ps.setString(5, user.getPasswordHash());
            ps.setString(6, user.getRole().name());
            ps.setString(7, user.getDescription());
            ps.executeUpdate();

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateUserException(
                    "User already exists with username or email: " + user.getUsername());
        } catch (SQLException e) {
            throw new DatabaseException("Error inserting user: " + user.getUsername(), e);
        }
    }

    // ----------------------------------------------------------------
    // findByUsername
    // ----------------------------------------------------------------

    /**
     * Carica un utente per username leggendo solo la tabella user.
     *
     * Restituisce il sottotipo corretto (Student, Tutor o Admin)
     * basandosi sulla colonna role, ma i campi specifici del ruolo
     * (es. budget per Student, rating per Tutor) NON vengono caricati:
     * per quelli usare i metodi delle sottoclassi DAO.
     *
     * Usato principalmente durante il login: matchesPassword() opera
     * solo su passwordHash che sta nella tabella user, quindi
     * questo metodo è sufficiente per autenticare l'utente.
     */
    @Override
    public User findByUsername(Connection conn, String username)
            throws UserNotFoundException, DatabaseException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_CHECK)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new UserNotFoundException(username);
                return mapUser(rs);
            }
        } catch (UserNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving user: " + username, e);
        }
    }

    // ----------------------------------------------------------------
    // updatePassword
    // ----------------------------------------------------------------

    /**
     * Aggiorna il passwordHash nella tabella user.
     * Il chiamante è responsabile di calcolare l'hash BCrypt
     * prima di invocare questo metodo.
     */
    @Override
    public void updatePassword(Connection conn, String username, String newPasswordHash)
            throws DatabaseException, UserNotFoundException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATEPASS)) {
            ps.setString(1, newPasswordHash);
            ps.setString(2, username);

            // executeUpdate() restituisce 0 se nessuna riga è stata aggiornata:
            // significa che lo username non esiste nella tabella user
            if (ps.executeUpdate() == 0) throw new UserNotFoundException(username);
        } catch (UserNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating password for user: " + username, e);
        }
    }

    // ----------------------------------------------------------------
    // updateProfile
    // ----------------------------------------------------------------

    /**
     * Aggiorna i campi mutabili del profilo: description e is_active.
     * is_active = false implementa il soft-delete: l'utente viene
     * disabilitato senza cancellazione fisica, preservando lo storico
     * di booking e recensioni.
     *
     */
    @Override
    public void updateProfile(Connection conn, String username, String description, boolean isActive)
            throws DatabaseException, UserNotFoundException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, description);
            ps.setBoolean(2, isActive);
            ps.setString(3, username);
            if (ps.executeUpdate() == 0) throw new UserNotFoundException(username);
        } catch (UserNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating profile for user: " + username, e);
        }
    }

    // ----------------------------------------------------------------
    // findByEmail
    // ----------------------------------------------------------------

    @Override
    public User findByEmail(Connection conn, String email)
            throws UserNotFoundException, DatabaseException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new UserNotFoundException("email: " + email);
                return mapUser(rs);
            }
        } catch (UserNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("Errore nel recupero utente per email: " + email, e);
        }
    }

    // ----------------------------------------------------------------
    // promoteToTutor
    // ----------------------------------------------------------------

    @Override
    public Tutor promoteToTutor(Connection conn, String studentUsername)
            throws DatabaseException, UserNotFoundException {
        try {
            // 1. aggiorna il ruolo nella tabella user
            try (PreparedStatement ps = conn.prepareStatement(SQL_PROMOTE_ROLE)) {
                ps.setString(1, studentUsername);
                if (ps.executeUpdate() == 0) throw new UserNotFoundException(studentUsername);
            }
            // 2. inserisce la riga nella tabella tutor
            //    La riga in student NON viene eliminata: fk_booking_student usa
            //    ON DELETE RESTRICT, quindi la DELETE fallirebbe se lo studente
            //    ha prenotazioni. Lo storico finanziario deve essere preservato
            //    (vedi commento schema: "non eliminare mai fisicamente un utente
            //    che ha booking o review"). Il role='TUTOR' in user è sufficiente
            //    per determinare il ruolo in tutta l'applicazione.
            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_TUTOR)) {
                ps.setString(1, studentUsername);
                ps.executeUpdate();
            }
            return (Tutor) findByUsername(conn, studentUsername);
        } catch (UserNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("Error promoting student to tutor: " + studentUsername, e);
        }
    }

    // ----------------------------------------------------------------
    // findFirstAdminUsername
    // ----------------------------------------------------------------

    /**
     * Restituisce lo username del primo utente Admin trovato nel DB.
     * Risolve il problema del username admin hardcodato ("admin") che
     * non corrisponde al valore reale nel DB ("admin_ale" nei dati seed).
     */
    @Override
    public String findFirstAdminUsername(Connection conn) throws DatabaseException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_ADMIN);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("username");
            return "admin"; // fallback se nessun admin trovato
        } catch (SQLException e) {
            throw new DatabaseException("Error finding admin username.", e);
        }
    }

    // ----------------------------------------------------------------
    // Helper privato
    // ----------------------------------------------------------------

    /**
     * Costruisce il sottotipo corretto di User dal ResultSet corrente.
     *
     * Legge la colonna role per decidere quale Builder istanziare:
     *   "STUDENT" → Student.Builder  (budget non popolato: sta in student)
     *   "TUTOR"   → Tutor.Builder    (rating non popolato: sta in tutor)
     *   altro     → Admin.Builder    (nessun campo aggiuntivo)
     *
     * I campi specifici del ruolo (budget, rating, ratingCount) non
     * vengono letti qui perché questa query tocca solo la tabella user.
     * Per oggetti completi usare i metodi delle sottoclassi DAO.
     */
    private User mapUser(ResultSet rs) throws SQLException {
        String username = rs.getString("username");
        String email = rs.getString("email");
        String name = rs.getString("name");
        String surname = rs.getString("surname");
        String passwordHash = rs.getString("password_hash");
        String role = rs.getString("role");
        String description = rs.getString("description");
        boolean isActive = rs.getBoolean("is_active");
        LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);

        // Sceglie il Builder corretto in base al ruolo letto dal DB
        User.Builder<?> builder;
        if (role.equalsIgnoreCase("STUDENT")) {
            builder = new Student.Builder();
        } else if (role.equalsIgnoreCase("TUTOR")) {
            builder = new Tutor.Builder();
        } else {
            builder = new Admin.Builder();
        }

        return builder
                .username(username)
                .email(email)
                .name(name)
                .surname(surname)
                .passwordHash(passwordHash)
                .description(description)
                .active(isActive)
                .createdAt(createdAt)
                .build();
    }
}

