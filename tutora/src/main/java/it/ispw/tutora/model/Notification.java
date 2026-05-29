package it.ispw.tutora.model;

import it.ispw.tutora.enums.NotificationType;
import java.time.LocalDateTime;

/**
 * Entity che rappresenta una notifica inviata tra utenti o
 * generata automaticamente dal sistema (senderUsername = null).
 *
 * Corrisponde alla tabella notification del DB.
 *
 * -----------------------------------------------------------------------
 * Pattern Builder (GoF – Creazionale)
 * -----------------------------------------------------------------------
 * Il costruttore originale aveva 8 parametri, superando la soglia
 * raccomandata di 7 (regola SonarQube). Il Builder pattern risolve
 * il problema rendendo la costruzione dell'oggetto leggibile e
 * a prova di errore — ogni parametro è nominato esplicitamente
 * nella chiamata, eliminando il rischio di scambiare argomenti
 * dello stesso tipo (es. due String consecutive).
 *
 */
public class Notification {

    /**
     * Separatore convenzionale usato dal layer applicativo per incorporare
     * le note admin nel campo {@code message} delle notifiche APPLICATION_UPDATE.
     * Definito qui perché {@code Notification} è il proprietario del formato
     * del proprio campo — i consumer chiamano {@link #getMainMessage()} e
     * {@link #getAdminNotes()} invece di parsare la stringa direttamente.
     */
    private static final String ADMIN_NOTES_SEPARATOR = "\n\nAdmin notes: ";

    private final int id;
    private final String recipientUsername;
    private final String senderUsername;
    private final String message;
    private final NotificationType type;
    private final Integer targetId;
    private final LocalDateTime timestamp;
    private boolean read;

    // Costruttore privato: accessibile solo tramite Builder
    private Notification(Builder builder) {
        this.id = builder.id;
        this.recipientUsername = builder.recipientUsername;
        this.senderUsername = builder.senderUsername;
        this.message = builder.message;
        this.type = builder.type;
        this.targetId = builder.targetId;
        this.timestamp = builder.timestamp;
        this.read = builder.read;
    }

    // ----------------------------------------------------------------
    // Builder – classe interna statica
    // ----------------------------------------------------------------

    public static class Builder {
        private int id;
        private String recipientUsername;
        private String senderUsername;
        private String message;
        private NotificationType type;
        private Integer targetId;
        private LocalDateTime timestamp;
        private boolean read;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder recipientUsername(String recipientUsername) {
            this.recipientUsername = recipientUsername;
            return this;
        }

        public Builder senderUsername(String senderUsername) {
            this.senderUsername = senderUsername;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public Builder targetId(Integer targetId) {
            this.targetId = targetId;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder read(boolean read) {
            this.read = read;
            return this;
        }

        public Notification build() {
            return new Notification(this);
        }
    }

    // ----------------------------------------------------------------
    // Getter
    // ----------------------------------------------------------------

    public int getId() { return id; }
    public String getRecipientUsername() { return recipientUsername; }
    public String getSenderUsername() { return senderUsername; }
    public String getMessage() { return message; }
    public NotificationType getType() { return type; }
    public Integer getTargetId() { return targetId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    /**
     * Restituisce la parte principale del messaggio, senza le eventuali note admin.
     * Per notifiche senza note incorporates restituisce il messaggio intero.
     */
    public String getMainMessage() {
        if (message == null) return null;
        int idx = message.indexOf(ADMIN_NOTES_SEPARATOR);
        return idx >= 0 ? message.substring(0, idx) : message;
    }

    /**
     * Restituisce le note admin incorporate nel messaggio, oppure {@code null}
     * se non sono presenti. Rilevante per le notifiche di tipo APPLICATION_UPDATE.
     */
    public String getAdminNotes() {
        if (message == null) return null;
        int idx = message.indexOf(ADMIN_NOTES_SEPARATOR);
        return idx >= 0 ? message.substring(idx + ADMIN_NOTES_SEPARATOR.length()) : null;
    }
}