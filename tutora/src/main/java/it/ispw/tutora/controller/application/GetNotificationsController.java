package it.ispw.tutora.controller.application;

import it.ispw.tutora.bean.NotificationBean;
import it.ispw.tutora.dao.NotificationDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.session.SessionManager;

import java.sql.Connection;
import java.sql.SQLException;
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
        String username = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            bean.setList(notificationDao.findByRecipient(conn, username));
            bean.setUnreadCount(notificationDao.countUnread(conn, username));
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

    public void markAllAsRead(NotificationBean bean, String token) {
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
}
