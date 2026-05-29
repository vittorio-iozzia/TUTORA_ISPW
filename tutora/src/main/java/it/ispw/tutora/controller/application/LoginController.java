package it.ispw.tutora.controller.application;

import it.ispw.tutora.bean.LoginBean;
import it.ispw.tutora.dao.UserDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.exception.AuthenticationException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.User;
import it.ispw.tutora.model.session.SessionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Controller applicativo responsabile del processo di autenticazione.
 *
 * -----------------------------------------------------------------------
 * Flusso
 * -----------------------------------------------------------------------
 * 1. GfxController raccoglie username e password nel LoginBean
 * 2. Chiama login(bean)
 * 3. LoginController recupera l'utente tramite UserDao
 * 4. Verifica che l'account sia attivo
 * 5. Verifica la password con BCrypt (User.matchesPassword)
 * 6. Crea la sessione tramite SessionManager
 * 7. Restituisce il token al GfxController
 * 8. GfxController redirige alla dashboard in base al ruolo
 *
 * -----------------------------------------------------------------------
 * Sicurezza
 * -----------------------------------------------------------------------
 * La password in chiaro viene svuotata dal Bean dopo la verifica.
 * AuthenticationException non rivela se lo username esiste o meno —
 * messaggio generico per entrambi i casi di errore.
 */
public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    /**
     * Esegue il login dell'utente con le credenziali contenute nel bean.
     */
    public String login(LoginBean login)
            throws AuthenticationException, DatabaseException {

        DaoFactory factory = DaoFactory.getInstance();
        UserDao userDao = factory.createUserDao();

        try (Connection conn = factory.getConnection()) {

            // 1. carica l'utente — UserNotFoundException diventa AuthenticationException
            User user = resolveUser(userDao, conn, login.getUsername());

            // 2. verifica che l'account sia attivo
            if (!user.isActive()) {
                throw new AuthenticationException(
                        "Account disabled. Please contact administrator.");
            }

            // 3. verifica la password con BCrypt
            if (!user.matchesPassword(login.getPassword())) {
                throw new AuthenticationException(
                        "Invalid username or password.");
            }

            // 4. se lo user è uno Student, ricarica l'oggetto completo (con budget)
            //    perché findByUsername legge solo la tabella user, non student.budget
            if (user instanceof Student) {
                user = reloadFullStudent(factory, conn, user);
            }

            // 5. crea la sessione e restituisce il token
            String token = SessionManager.getInstance().createSession(user);

            // 6. se il tutor ha il flag "neotutor" persistito (JSON mode),
            //    ripopola il flag in-memory e azzera quello su disco:
            //    il LoginGfxController potrà controllare isNewlyPromotedTutor()
            //    e TutorContentController mostrerà il popup di benvenuto.
            if (user instanceof it.ispw.tutora.model.Tutor
                    && factory.isNewlyPromotedTutor(user.getUsername())) {
                SessionManager.getInstance().markAsNewlyPromotedTutor(user.getUsername());
                factory.clearNewlyPromotedTutor(user.getUsername());
            }

            // 7. svuota la password dal Bean
            login.clearPassword();

            return token;
        } catch (SQLException e) {
            throw new DatabaseException("Login error.", e);
        }
    }

    private User resolveUser(UserDao dao, Connection conn, String username)
            throws AuthenticationException, DatabaseException {
        try {
            return dao.findByUsername(conn, username);
        } catch (UserNotFoundException e) {
            throw new AuthenticationException("Invalid username or password.");
        }
    }

    private User reloadFullStudent(DaoFactory factory, Connection conn, User user) {
        try {
            return factory.createStudentDao().selectStudent(conn, user.getUsername());
        } catch (UserNotFoundException | DatabaseException e) {
            LOGGER.warning("Could not load full student data for: " + user.getUsername());
            return user;
        }
    }
}