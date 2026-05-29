package it.ispw.tutora.controller.application;

import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.dao.StudentDao;
import it.ispw.tutora.dao.TutorExpertiseDao;
import it.ispw.tutora.dao.UserDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.enums.Status;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.TutorExpertise;
import it.ispw.tutora.model.User;
import it.ispw.tutora.model.session.SessionManager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Application controller per le operazioni sul profilo utente.
 *
 * Responsabilità:
 *  - aggiornamento della descrizione (tutor e studente)
 *  - aggiornamento del budget dello studente
 *  - statistiche prenotazioni dello studente
 *  - expertise approvate di un qualsiasi tutor (profilo pubblico)
 */
public class UserProfileController {

    private static final Logger LOGGER =
            Logger.getLogger(UserProfileController.class.getName());

    private final UserDao          userDao;
    private final StudentDao       studentDao;
    private final BookingDao       bookingDao;
    private final TutorExpertiseDao expertiseDao;

    public UserProfileController() {
        DaoFactory factory = DaoFactory.getInstance();
        this.userDao      = factory.createUserDao();
        this.studentDao   = factory.createStudentDao();
        this.bookingDao   = factory.createBookingDao();
        this.expertiseDao = factory.createTutorExpertiseDao();
    }

    /**
     * Persiste la descrizione dell'utente nel datastore e aggiorna il modello in-memory.
     * Best-effort: registra un warning in caso di errore, non rilancia.
     */
    public void updateDescription(String token, String description) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return;
        User user = sm.getCurrentUser(token);
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            userDao.updateProfile(conn, user.getUsername(), description, user.isActive());
            user.setDescription(description);
        } catch (DatabaseException | UserNotFoundException | SQLException e) {
            LOGGER.warning("Cannot persist description for '" + user.getUsername() + "': " + e.getMessage());
        }
    }

    /**
     * Persiste il nuovo budget dello studente autenticato e aggiorna il modello in-memory.
     *
     * @return {@code true} se l'operazione è andata a buon fine, {@code false} altrimenti.
     */
    public boolean updateBudget(String token, BigDecimal newBudget) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return false;
        User user = sm.getCurrentUser(token);
        if (!(user instanceof Student student)) return false;
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            studentDao.updateStudentBudget(conn, user.getUsername(), newBudget);
            BigDecimal current = student.getBudget() != null ? student.getBudget() : BigDecimal.ZERO;
            BigDecimal delta = newBudget.subtract(current);
            if (delta.compareTo(BigDecimal.ZERO) > 0)      student.addBudget(delta);
            else if (delta.compareTo(BigDecimal.ZERO) < 0) student.deductBudget(delta.abs());
            return true;
        } catch (DatabaseException | UserNotFoundException | SQLException e) {
            LOGGER.warning("Cannot update budget for '" + user.getUsername() + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Restituisce le statistiche di prenotazione dello studente autenticato:
     * indice 0 = numero di lezioni pagate, indice 1 = numero di tutor distinti.
     */
    public long[] getStudentStats(String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return new long[]{0, 0};
        String username = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            List<Booking> bookings = bookingDao.findByStudent(conn, username);
            long paidCount = bookings.stream()
                    .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                    .count();
            Set<String> tutors = bookings.stream()
                    .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                    .map(b -> {
                        try {
                            return b.getLesson().getExpertise().getTutor().getUsername();
                        } catch (Exception ex) {
                            return null;
                        }
                    })
                    .filter(t -> t != null)
                    .collect(Collectors.toSet());
            return new long[]{ paidCount, tutors.size() };
        } catch (DatabaseException | SQLException e) {
            LOGGER.warning("Cannot load student stats: " + e.getMessage());
            return new long[]{0, 0};
        }
    }

    /**
     * Restituisce le expertise approvate di un qualsiasi tutor identificato per username.
     * Non richiede token: si tratta di informazioni pubbliche del profilo.
     */
    public List<TutorExpertise> getPublicExpertises(String tutorUsername) {
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            return expertiseDao.findByTutor(conn, tutorUsername).stream()
                    .filter(e -> e.getStatus() == Status.APPROVED)
                    .toList();
        } catch (DatabaseException | SQLException e) {
            LOGGER.warning("Cannot load public expertises for '" + tutorUsername + "': " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
