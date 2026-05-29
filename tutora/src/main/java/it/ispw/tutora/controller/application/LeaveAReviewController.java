package it.ispw.tutora.controller.application;

import it.ispw.tutora.bean.ReviewBean;
import it.ispw.tutora.dao.NotificationDao;
import it.ispw.tutora.dao.ReviewDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.NotificationType;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateReviewException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Notification;
import it.ispw.tutora.model.Review;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.User;
import it.ispw.tutora.model.session.SessionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Controller applicativo per UC: Leave a Review.
 *
 * Responsabilità:
 *   1. Verifica che la sessione sia valida
 *   2. Costruisce il model Review dai dati del bean
 *   3. Persiste la recensione tramite ReviewDao
 *   4. Invia una notifica al tutor tramite NotificationDao
 *
 * Separato da ReviewGfxController per rispettare il pattern BCE:
 * la boundary raccoglie i dati dalla UI e delega interamente
 * a questo controller senza mai toccare il layer DAO.
 */
public class LeaveAReviewController {

    private static final Logger LOGGER =
            Logger.getLogger(LeaveAReviewController.class.getName());

    /**
     * Persiste una recensione e notifica il tutor.
     *
     * @param bean  contiene bookingId, tutorUsername, rating, comment
     * @param token token di sessione dello studente
     */
    public void submitReview(ReviewBean bean, String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) {
            bean.setErrorMessage("Invalid or expired session.");
            return;
        }

        User user = sm.getCurrentUser(token);
        if (!(user instanceof Student student)) {
            bean.setErrorMessage("Only students can leave reviews.");
            return;
        }

        DaoFactory factory = DaoFactory.getInstance();
        ReviewDao   reviewDao = factory.createReviewDao();
        NotificationDao notifDao = factory.createNotificationDao();

        // Booking parziale: serve solo l'id come chiave esterna nel DB.
        // Il fix di checkPayment (valida solo se non-null) garantisce che
        // new Booking.Builder().id(x).build() non lanci IllegalArgumentException.
        Booking booking = new Booking.Builder()
                .id(bean.getBookingId())
                .build();

        // Tutor parziale: serve solo lo username per la notifica e il DAO.
        Tutor tutor = new Tutor.Builder()
                .username(bean.getTutorUsername())
                .build();

        Review review = new Review.Builder()
                .id(0)
                .booking(booking)
                .student(student)
                .tutor(tutor)
                .rating(bean.getRating())
                .comment(bean.getComment() != null ? bean.getComment().trim() : "")
                .createdAt(LocalDateTime.now())
                .build();

        try (Connection conn = factory.getConnection()) {
            if (conn != null) conn.setAutoCommit(false);
            executeReview(conn, bean, review, reviewDao, notifDao, student);
        } catch (DatabaseException | SQLException e) {
            bean.setErrorMessage("Database connection error.");
            LOGGER.warning("Connection error submitting review: " + e.getMessage());
        }
    }

    private void executeReview(Connection conn, ReviewBean bean, Review review,
                               ReviewDao reviewDao, NotificationDao notifDao,
                               Student student) {
        try {
            reviewDao.insertReview(conn, review);

            String comment   = bean.getComment();
            String truncated = (comment != null && comment.length() > 60)
                    ? comment.substring(0, 60) + "…" : comment;
            String preview = (comment != null && !comment.isBlank())
                    ? " — \"" + truncated + "\"" : "";

            Notification notification = new Notification.Builder()
                    .id(0)
                    .recipientUsername(bean.getTutorUsername())
                    .senderUsername(student.getUsername())
                    .message(student.getUsername() + " left you a "
                            + bean.getRating() + "★ review" + preview)
                    .type(NotificationType.NEW_REVIEW)
                    .targetId(bean.getBookingId())
                    .timestamp(LocalDateTime.now())
                    .read(false)
                    .build();

            notifDao.insert(conn, notification);

            if (conn != null) conn.commit();

        } catch (DuplicateReviewException e) {
            safeRollback(conn);
            bean.setErrorMessage("You have already submitted a review for this lesson.");
        } catch (DatabaseException e) {
            safeRollback(conn);
            bean.setErrorMessage("Failed to submit review. Please try again.");
            LOGGER.warning("Review submission failed: " + e.getMessage());
        } catch (Exception e) {
            safeRollback(conn);
            bean.setErrorMessage("Unexpected error. Please try again.");
            LOGGER.warning("Unexpected error submitting review: " + e.getMessage());
        }
    }

    private void safeRollback(Connection conn) {
        if (conn == null) return;
        try { conn.rollback(); }
        catch (SQLException e) { LOGGER.warning("Rollback failed: " + e.getMessage()); }
    }
}
