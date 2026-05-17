package it.ispw.tutora.dao.db;

import it.ispw.tutora.dao.NotificationDao;
import it.ispw.tutora.enums.NotificationType;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Notification;
import org.intellij.lang.annotations.Language;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementazione JDBC di NotificationDao.
 *
 * -----------------------------------------------------------------------
 * Nota sulle transazioni
 * -----------------------------------------------------------------------
 * Tutti i metodi ricevono la Connection come parametro.
 * Il Controller applicativo gestisce commit e rollback — il DAO
 * non chiama mai conn.commit() o conn.rollback().
 * insert() viene tipicamente chiamato nella stessa transazione
 * dell'operazione che genera la notifica.
 *
 * -----------------------------------------------------------------------
 * Nota su senderUsername
 * -----------------------------------------------------------------------
 * Il campo sender_username può essere NULL nel DB per le notifiche
 * generate automaticamente dal sistema. Il PreparedStatement usa
 * setNull() quando senderUsername è null.
 */
public class NotificationDaoDb implements NotificationDao {

    @Language("SQL")
    private static final String SQL_INSERT =
            "INSERT INTO notification " +
            "(recipient_username, sender_username, message, type, target_id) " +
            "VALUES (?, ?, ?, ?, ?)";

    @Language("SQL")
    private static final String SQL_FIND_BY_RECIPIENT =
            "SELECT id, recipient_username, sender_username, message, " +
            "type, target_id, timestamp, is_read " +
            "FROM notification " +
            "WHERE recipient_username = ? " +
            "ORDER BY timestamp DESC";

    @Language("SQL")
    private static final String SQL_MARK_AS_READ =
            "UPDATE notification " +
            "SET is_read = TRUE " +
            "WHERE id = ?";

    @Language("SQL")
    private static final String SQL_MARK_ALL_AS_READ =
            "UPDATE notification " +
            "SET is_read = TRUE " +
            "WHERE recipient_username = ? AND is_read = FALSE";

    @Language("SQL")
    private static final String SQL_COUNT_UNREAD =
            "SELECT COUNT(*) " +
            "FROM notification " +
            "WHERE recipient_username = ? AND is_read = FALSE";

    @Language("SQL")
    private static final String SQL_MARK_READ_BY_TARGET =
            "UPDATE notification " +
            "SET is_read = TRUE " +
            "WHERE target_id = ? AND recipient_username = ? AND is_read = FALSE";

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    @Override
    public int insert(Connection conn, Notification notification)
            throws DatabaseException {

        try (PreparedStatement ps = conn.prepareStatement(
                SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, notification.getRecipientUsername());

            // senderUsername può essere null — notifica di sistema
            if (notification.getSenderUsername() != null) {
                ps.setString(2, notification.getSenderUsername());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }

            ps.setString(3, notification.getMessage());
            ps.setString(4, toDbType(notification.getType()));

            // targetId può essere null
            if (notification.getTargetId() != null) {
                ps.setInt(5, notification.getTargetId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("No generated ID for the new notification.");
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error inserting notification.", e);
        }
    }

    // ----------------------------------------------------------------
    // findByRecipient
    // ----------------------------------------------------------------

    @Override
    public List<Notification> findByRecipient(Connection conn, String recipientUsername)
            throws DatabaseException {

        List<Notification> notifications = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_RECIPIENT)) {

            ps.setString(1, recipientUsername);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapNotification(rs));
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error retrieving notifications for: " + recipientUsername, e);
        }

        return notifications;
    }

    // ----------------------------------------------------------------
    // markAsRead
    // ----------------------------------------------------------------

    @Override
    public void markAsRead(Connection conn, int notificationId)
            throws DatabaseException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_MARK_AS_READ)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error marking notification as read: " + notificationId, e);
        }
    }

    // ----------------------------------------------------------------
    // markAllAsRead
    // ----------------------------------------------------------------

    @Override
    public void markAllAsRead(Connection conn, String recipientUsername)
            throws DatabaseException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_MARK_ALL_AS_READ)) {
            ps.setString(1, recipientUsername);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error marking all notifications as read for: " + recipientUsername, e);
        }
    }

    // ----------------------------------------------------------------
    // countUnread
    // ----------------------------------------------------------------

    @Override
    public int countUnread(Connection conn, String recipientUsername)
            throws DatabaseException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_UNREAD)) {

            ps.setString(1, recipientUsername);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                return 0;
            }

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error counting unread notifications for: " + recipientUsername, e);
        }
    }

    // ----------------------------------------------------------------
    // markReadByTargetIdAndRecipient
    // ----------------------------------------------------------------

    @Override
    public void markReadByTargetIdAndRecipient(Connection conn, int targetId,
                                               String recipientUsername)
            throws DatabaseException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_MARK_READ_BY_TARGET)) {
            ps.setInt(1, targetId);
            ps.setString(2, recipientUsername);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Error marking notification read by targetId=" + targetId, e);
        }
    }

    // ----------------------------------------------------------------
    // Helper privati
    // ----------------------------------------------------------------

    private Notification mapNotification(ResultSet rs) throws SQLException {
        return new Notification.Builder()
                .id(rs.getInt("id"))
                .recipientUsername(rs.getString("recipient_username"))
                .senderUsername(rs.getString("sender_username")) // null se sistema
                .message(rs.getString("message"))
                .type(fromDbType(rs.getString("type")))
                .targetId(rs.getObject("target_id", Integer.class)) // null-safe
                .timestamp(rs.getObject("timestamp", LocalDateTime.class))
                .read(rs.getBoolean("is_read"))
                .build();
    }

    private static String toDbType(NotificationType type) {
        return switch (type) {
            case APPLICATION_UPDATE -> "Application_Update";
            case EXPERTISE_OFFER    -> "Expertise_Offer";
            case LESSON_BOOKED      -> "Lesson_Booked";
            case LESSON_ACCEPTED    -> "Lesson_Accepted";
            case LESSON_REJECTED    -> "Lesson_Rejected";
            case PAYMENT_CONFIRMED  -> "Payment_Confirmed";
            case NEW_REVIEW         -> "New_Review";
        };
    }

    private static NotificationType fromDbType(String s) {
        return switch (s) {
            case "Application_Update" -> NotificationType.APPLICATION_UPDATE;
            case "Expertise_Offer"    -> NotificationType.EXPERTISE_OFFER;
            case "Lesson_Booked"      -> NotificationType.LESSON_BOOKED;
            case "Lesson_Accepted"    -> NotificationType.LESSON_ACCEPTED;
            case "Lesson_Rejected"    -> NotificationType.LESSON_REJECTED;
            case "Payment_Confirmed"  -> NotificationType.PAYMENT_CONFIRMED;
            case "New_Review"         -> NotificationType.NEW_REVIEW;
            default -> throw new IllegalArgumentException(
                    "Unknown notification type from DB: " + s);
        };
    }
}