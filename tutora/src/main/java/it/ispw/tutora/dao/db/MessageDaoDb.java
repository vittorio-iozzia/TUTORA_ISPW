package it.ispw.tutora.dao.db;

import it.ispw.tutora.dao.MessageDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Message;
import org.intellij.lang.annotations.Language;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementazione JDBC di MessageDao.
 *
 * Schema richiesto:
 * <pre>
 * CREATE TABLE message (
 *   id                 INT AUTO_INCREMENT PRIMARY KEY,
 *   sender_username    VARCHAR(100) NOT NULL,
 *   recipient_username VARCHAR(100) NOT NULL,
 *   content            TEXT NOT NULL,
 *   sent_at            DATETIME DEFAULT CURRENT_TIMESTAMP,
 *   is_read            BOOLEAN DEFAULT FALSE,
 *   CONSTRAINT fk_msg_sender    FOREIGN KEY (sender_username)    REFERENCES user(username),
 *   CONSTRAINT fk_msg_recipient FOREIGN KEY (recipient_username) REFERENCES user(username)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 * </pre>
 */
public class MessageDaoDb implements MessageDao {

    @Language("SQL")
    private static final String SQL_INSERT =
            "INSERT INTO message (sender_username, recipient_username, content, sent_at) " +
            "VALUES (?, ?, ?, ?)";

    @Language("SQL")
    private static final String SQL_GET_CONVERSATION =
            "SELECT id, sender_username, recipient_username, content, sent_at, is_read " +
            "FROM message " +
            "WHERE (sender_username = ? AND recipient_username = ?) " +
            "   OR (sender_username = ? AND recipient_username = ?) " +
            "ORDER BY sent_at ASC";

    @Language("SQL")
    private static final String SQL_MARK_READ =
            "UPDATE message " +
            "SET is_read = TRUE " +
            "WHERE sender_username = ? AND recipient_username = ? AND is_read = FALSE";

    @Language("SQL")
    private static final String SQL_COUNT_UNREAD =
            "SELECT COUNT(*) FROM message WHERE recipient_username = ? AND is_read = FALSE";

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    @Override
    public int insert(Connection conn, Message message) throws DatabaseException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, message.getSenderUsername());
            ps.setString(2, message.getRecipientUsername());
            ps.setString(3, message.getContent());
            ps.setObject(4, message.getSentAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("No generated ID for new message.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error inserting message.", e);
        }
    }

    // ----------------------------------------------------------------
    // getConversation
    // ----------------------------------------------------------------

    @Override
    public List<Message> getConversation(Connection conn, String user1, String user2)
            throws DatabaseException {

        List<Message> messages = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_GET_CONVERSATION)) {
            ps.setString(1, user1);
            ps.setString(2, user2);
            ps.setString(3, user2);
            ps.setString(4, user1);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(new Message.Builder()
                            .id(rs.getInt("id"))
                            .senderUsername(rs.getString("sender_username"))
                            .recipientUsername(rs.getString("recipient_username"))
                            .content(rs.getString("content"))
                            .sentAt(rs.getObject("sent_at", LocalDateTime.class))
                            .read(rs.getBoolean("is_read"))
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error loading conversation.", e);
        }
        return messages;
    }

    // ----------------------------------------------------------------
    // markConversationRead
    // ----------------------------------------------------------------

    @Override
    public void markConversationRead(Connection conn, String senderUsername, String recipientUsername)
            throws DatabaseException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_MARK_READ)) {
            ps.setString(1, senderUsername);
            ps.setString(2, recipientUsername);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error marking messages as read.", e);
        }
    }

    // ----------------------------------------------------------------
    // countTotalUnread
    // ----------------------------------------------------------------

    @Override
    public int countTotalUnread(Connection conn, String recipientUsername) throws DatabaseException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_COUNT_UNREAD)) {
            ps.setString(1, recipientUsername);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error counting unread messages.", e);
        }
    }
}
