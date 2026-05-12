package it.ispw.tutora.controller.application;

import it.ispw.tutora.bean.LoginBean;
import it.ispw.tutora.dao.UserDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.exception.AuthenticationException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.User;
import it.ispw.tutora.model.session.SessionManager;

import java.sql.Connection;
import java.sql.SQLException;

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

            // 4. crea la sessione e restituisce il token
            String token = SessionManager.getInstance().createSession(user);

            // 5. svuota la password dal Bean
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
}