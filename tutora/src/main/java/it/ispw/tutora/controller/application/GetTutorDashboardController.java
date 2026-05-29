package it.ispw.tutora.controller.application;

import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.dao.LessonDao;
import it.ispw.tutora.dao.NotificationDao;
import it.ispw.tutora.dao.TutorExpertiseDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.NotificationType;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.enums.Status;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Lesson;
import it.ispw.tutora.model.Notification;
import it.ispw.tutora.model.TutorExpertise;
import it.ispw.tutora.model.session.SessionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Application controller per il dashboard del tutor.
 *
 * Responsabilità:
 *  - date delle lezioni (per il calendario)
 *  - lezioni imminenti (prenotazioni PAID con startTime futuro)
 *  - richieste di prenotazione pendenti (notifiche LESSON_BOOKED non lette)
 *  - lezione per ID (per il dettaglio della booking card)
 *  - expertise approvate del tutor (per le statistiche del profilo)
 *  - conteggio lezioni pagate (per le statistiche del profilo)
 */
public class GetTutorDashboardController {

    private static final Logger LOGGER =
            Logger.getLogger(GetTutorDashboardController.class.getName());

    private final BookingDao      bookingDao;
    private final LessonDao       lessonDao;
    private final NotificationDao notificationDao;
    private final TutorExpertiseDao expertiseDao;

    public GetTutorDashboardController() {
        DaoFactory factory = DaoFactory.getInstance();
        this.bookingDao      = factory.createBookingDao();
        this.lessonDao       = factory.createLessonDao();
        this.notificationDao = factory.createNotificationDao();
        this.expertiseDao    = factory.createTutorExpertiseDao();
    }

    /**
     * Restituisce l'insieme delle date in cui il tutor ha almeno una lezione.
     * Usato per evidenziare i giorni nel calendario.
     */
    public Set<LocalDate> getLessonDates(String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return Collections.emptySet();
        String username = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            return lessonDao.findByTutor(conn, username).stream()
                    .filter(l -> l.getStartTime() != null)
                    .map(l -> l.getStartTime().toLocalDate())
                    .collect(Collectors.toSet());
        } catch (DatabaseException | SQLException e) {
            LOGGER.warning("Cannot load lesson dates: " + e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * Restituisce le lezioni imminenti del tutor
     * (prenotazioni PAID con startTime futuro), ordinate per startTime crescente.
     */
    public List<Lesson> getUpcomingLessons(String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return Collections.emptyList();
        String username = sm.getCurrentUser(token).getUsername();
        LocalDateTime now = LocalDateTime.now();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            return bookingDao.findByTutor(conn, username).stream()
                    .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                    .filter(b -> b.getLesson().getStartTime() != null)
                    .filter(b -> b.getLesson().getStartTime().isAfter(now))
                    .sorted((a, b) -> a.getLesson().getStartTime()
                            .compareTo(b.getLesson().getStartTime()))
                    .map(Booking::getLesson)
                    .toList();
        } catch (DatabaseException | SQLException | RuntimeException e) {
            LOGGER.warning("Cannot load upcoming lessons: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Restituisce le richieste di prenotazione pendenti del tutor:
     * notifiche LESSON_BOOKED non ancora lette.
     */
    public List<Notification> getPendingBookingRequests(String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return Collections.emptyList();
        String username = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            return notificationDao.findByRecipient(conn, username).stream()
                    .filter(n -> n.getType() == NotificationType.LESSON_BOOKED && !n.isRead())
                    .toList();
        } catch (DatabaseException | SQLException e) {
            LOGGER.warning("Cannot load booking requests: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Carica una singola lezione per ID.
     * Restituisce null se non trovata o in caso di errore.
     */
    public Lesson getLessonById(int lessonId) {
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            return lessonDao.selectLesson(conn, lessonId);
        } catch (Exception e) {
            LOGGER.warning("Cannot load lesson " + lessonId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Restituisce le expertise approvate del tutor autenticato.
     * Usato nelle statistiche del profilo tutor.
     */
    public List<TutorExpertise> getApprovedExpertises(String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return Collections.emptyList();
        String username = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            return expertiseDao.findByTutor(conn, username).stream()
                    .filter(e -> e.getStatus() == Status.APPROVED)
                    .toList();
        } catch (DatabaseException | SQLException e) {
            LOGGER.warning("Cannot load expertises: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Restituisce il numero di lezioni pagate (prenotazioni con stato PAID) del tutor.
     * Usato nelle statistiche del profilo tutor.
     */
    public long getPaidBookingsCount(String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return 0;
        String username = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            return bookingDao.findByTutor(conn, username).stream()
                    .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                    .count();
        } catch (DatabaseException | SQLException e) {
            LOGGER.warning("Cannot load bookings count: " + e.getMessage());
            return 0;
        }
    }
}
