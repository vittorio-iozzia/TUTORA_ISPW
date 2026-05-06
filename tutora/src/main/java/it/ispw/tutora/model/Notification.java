package it.ispw.tutora.model;

import it.ispw.tutora.enums.NotificationType;
import java.time.LocalDateTime;

/**
 * Entity che rappresenta una notifica inviata tra utenti o
 * generata automaticamente dal sistema (senderUsername = null).
 *
 * Corrisponde alla tabella notification del DB.
 */
public class Notification {

    private final int id;
    private final String recipientUsername;
    private final String senderUsername;
    private final String message;
    private final NotificationType type;
    private final Integer targetId;
    private final LocalDateTime timestamp;
    private boolean read;

    public Notification (int id,
                         String recipientUsername,
                         String senderUsername,
                         String message,
                         NotificationType type,
                         Integer targetId,
                         LocalDateTime timestamp,
                         boolean read){
        this.id = id;
        this.recipientUsername = recipientUsername;
        this.senderUsername = senderUsername;
        this.message = message;
        this.type = type;
        this.targetId = targetId;
        this.timestamp = timestamp;
        this.read = read;

    }

    public int getId() { return id; }
    public String getRecipientUsername() { return recipientUsername; }
    public String getSenderUsername() { return senderUsername; }
    public String getMessage() { return message; }
    public NotificationType getType() { return type; }
    public Integer getTargetId() { return targetId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) {this.read = read; }

}
