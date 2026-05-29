package it.ispw.tutora.controller.application;

import it.ispw.tutora.bean.NotificationBean;
import it.ispw.tutora.dao.NotificationDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.NotificationType;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Notification;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

public class GetNotificationsController {

    private static final Logger LOGGER = Logger.getLogger(GetNotificationsController.class.getName());

    private final NotificationDao notificationDao;

    public GetNotificationsController() {
        this.notificationDao = DaoFactory.getInstance().createNotificationDao();
    }

    public void loadNotifications(NotificationBean bean, String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) {
            bean.setErrorMessage("Invalid or expired session.");
            return;
        }
        Session session = sm.getSession(token);
        String username = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            List<Notification> list = notificationDao.findByRecipient(conn, username);
            if (session.isStudent()) {
                for (Notification n : list) {
                    if (n.getType() == NotificationType.LESSON_REJECTED && !n.isRead()) {
                        notificationDao.markAsRead(conn, n.getId());
                        n.setRead(true);
                    }
                }
            }
            bean.setList(list);
            bean.setUnreadCount((int) list.stream().filter(n -> !n.isRead()).count());
        } catch (DatabaseException | SQLException e) {
            bean.setErrorMessage("Could not load notifications.");
        }
    }

    public void markAsRead(NotificationBean bean, String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return;
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            notificationDao.markAsRead(conn, bean.getNotificationId());
        } catch (DatabaseException | SQLException e) {
            LOGGER.warning("markAsRead failed: " + e.getMessage());
        }
    }

    public void markAllAsRead(String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return;
        String username = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            notificationDao.markAllAsRead(conn, username);
        } catch (DatabaseException | SQLException e) {
            LOGGER.warning("markAllAsRead failed: " + e.getMessage());
        }
    }

    public int getUnreadCount(String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return 0;
        String username = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            return notificationDao.countUnread(conn, username);
        } catch (DatabaseException | SQLException e) {
            return 0;
        }
    }

    /**
     * Determina se una notifica richiede ancora un'azione esplicita dall'utente,
     * in base al tipo di notifica e al ruolo della sessione corrente.
     *
     * Regole di dominio:
     *   - Tutor:   LESSON_BOOKED   → deve ancora accettare/rifiutare
     *   - Student: LESSON_ACCEPTED → deve ancora pagare
     *   - Admin:   APPLICATION_UPDATE → deve ancora valutare la candidatura
     *
     * Spostato da NotificationGfxController per rispettare BCE:
     * la decisione su "quale notifica richiede azione" è logica applicativa,
     * non di presentazione.
     */
    public boolean isActionable(Notification notification, Session session) {
        return (session.isTutor()   && notification.getType() == NotificationType.LESSON_BOOKED)
            || (session.isStudent() && notification.getType() == NotificationType.LESSON_ACCEPTED)
            || (session.isAdmin()   && notification.getType() == NotificationType.APPLICATION_UPDATE);
    }
}
