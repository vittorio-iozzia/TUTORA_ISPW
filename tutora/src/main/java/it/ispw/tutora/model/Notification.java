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
}