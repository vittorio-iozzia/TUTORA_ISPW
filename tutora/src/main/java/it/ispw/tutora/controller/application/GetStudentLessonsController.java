package it.ispw.tutora.controller.application;

import it.ispw.tutora.bean.BookingBean;
import it.ispw.tutora.bean.MyLessonsBean;
import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.dao.ReviewDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.session.SessionManager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class GetStudentLessonsController {

    private static final Logger LOGGER =
            Logger.getLogger(GetStudentLessonsController.class.getName());

    private final BookingDao bookingDao;
    private final ReviewDao  reviewDao;

    public GetStudentLessonsController() {
        DaoFactory factory = DaoFactory.getInstance();
        this.bookingDao = factory.createBookingDao();
        this.reviewDao  = factory.createReviewDao();
    }

    /**
     * Carica tutte le prenotazioni dello student autenticato.
     * Se c'è un errore viene restituita una lista vuota.
     */
    public List<Booking> loadBookings(BookingBean bean, String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token) || !sm.getSession(token).isStudent()) {
            bean.setErrorMessage("Unauthorized.");
            return Collections.emptyList();
        }
        String username = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            return bookingDao.findByStudent(conn, username);
        } catch (DatabaseException | SQLException e) {
            bean.setErrorMessage("Could not load lessons.");
            return Collections.emptyList();
        }
    }

    /**
     * Carica le lezioni imminenti dello student (pagate, non ancora iniziate).
     * Ordinate per startTime crescente.
     */
    public List<Booking> loadUpcomingLessons(String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return Collections.emptyList();
        String username = sm.getCurrentUser(token).getUsername();
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            return bookingDao.findByStudent(conn, username).stream()
                    .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                    .filter(b -> b.getLesson().getStartTime() != null)
                    .filter(b -> b.getLesson().getStartTime().isAfter(now))
                    .sorted((a, b) -> a.getLesson().getStartTime()
                            .compareTo(b.getLesson().getStartTime()))
                    .toList();
        } catch (DatabaseException | SQLException | RuntimeException e) {
            LOGGER.warning("Cannot load upcoming lessons: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Carica la cronologia lezioni dello student (pagate, già concluse).
     * Ordinate per startTime decrescente (più recenti prima).
     */
    public List<Booking> loadLessonHistory(String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return Collections.emptyList();
        String username = sm.getCurrentUser(token).getUsername();
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            return bookingDao.findByStudent(conn, username).stream()
                    .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                    .filter(b -> b.getLesson().getStartTime() != null)
                    .filter(b -> !b.getLesson().getStartTime().isAfter(now))
                    .sorted((a, b) -> b.getLesson().getStartTime()
                            .compareTo(a.getLesson().getStartTime()))
                    .toList();
        } catch (DatabaseException | SQLException | RuntimeException e) {
            LOGGER.warning("Cannot load lesson history: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Carica e aggrega tutti i dati necessari alla pagina "My Lessons".
     *
     * Responsabilità di questo metodo (logica applicativa):
     *   - filtra upcoming per LessonStatus.BOOKED, ordinate per startTime crescente
     *   - filtra past per LessonStatus.COMPLETED, ordinate per startTime decrescente
     *   - calcola totalMinutes (somma durate lezioni passate)
     *   - calcola totalSpent (somma pricePaid di tutte le booking)
     *
     * Il boundary (MyLessonsGfxController) riceve un MyLessonsBean già pronto
     * e si occupa solo della resa grafica, senza conoscere LessonStatus.
     */
    public MyLessonsBean buildMyLessonsData(String token) {
        MyLessonsBean bean = new MyLessonsBean();
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token) || !sm.getSession(token).isStudent()) return bean;
        String username = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            List<Booking> all = bookingDao.findByStudent(conn, username);

            List<Booking> upcoming = all.stream()
                    .filter(b -> b.getLesson().getLessonStatus() == LessonStatus.BOOKED)
                    .filter(b -> b.getLesson().getStartTime() != null)
                    .sorted((a, b) -> a.getLesson().getStartTime()
                            .compareTo(b.getLesson().getStartTime()))
                    .toList();

            List<Booking> past = all.stream()
                    .filter(b -> b.getLesson().getLessonStatus() == LessonStatus.COMPLETED)
                    .filter(b -> b.getLesson().getStartTime() != null)
                    .sorted((a, b) -> b.getLesson().getStartTime()
                            .compareTo(a.getLesson().getStartTime()))
                    .toList();

            long totalMinutes = past.stream()
                    .filter(b -> b.getLesson().getEndTime() != null)
                    .mapToLong(b -> java.time.Duration
                            .between(b.getLesson().getStartTime().atZone(ZoneId.systemDefault()),
                                     b.getLesson().getEndTime().atZone(ZoneId.systemDefault()))
                            .toMinutes())
                    .sum();

            BigDecimal totalSpent = all.stream()
                    .map(Booking::getPricePaid)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            bean.setUpcoming(upcoming);
            bean.setPast(past);
            bean.setTotalMinutes(totalMinutes);
            bean.setTotalSpent(totalSpent);
        } catch (DatabaseException | SQLException | RuntimeException e) {
            LOGGER.warning("Cannot build my lessons data: " + e.getMessage());
        }
        return bean;
    }

    /**
     * Restituisce l'insieme degli id di booking per cui esiste già una recensione.
     * Usato dalla view per disabilitare il pulsante "Review" sulle lezioni già recensite.
     */
    public Set<Integer> getReviewedBookingIds(List<Booking> bookings, String token) {
        if (bookings == null || bookings.isEmpty()) return Collections.emptySet();
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return Collections.emptySet();
        Set<Integer> reviewed = new HashSet<>();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            for (Booking b : bookings) {
                String tutorUsername = b.getLesson().getExpertise().getTutor().getUsername();
                boolean hasReview = reviewDao.findByTutor(conn, tutorUsername).stream()
                        .anyMatch(r -> r.getBooking().getId() == b.getId());
                if (hasReview) reviewed.add(b.getId());
            }
        } catch (DatabaseException | SQLException e) {
            LOGGER.warning("Cannot check reviewed bookings: " + e.getMessage());
        }
        return reviewed;
    }
}
