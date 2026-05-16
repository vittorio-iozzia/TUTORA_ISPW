package it.ispw.tutora.model;

import java.time.LocalDateTime;

/**
 * Entity che rappresenta un messaggio di chat tra due utenti.
 *
 * Corrisponde alla tabella message del DB.
 *
 * -----------------------------------------------------------------------
 * Pattern Builder (GoF – Creazionale)
 * -----------------------------------------------------------------------
 */
public class Message {

    private final int id;
    private final String senderUsername;
    private final String recipientUsername;
    private final String content;
    private final LocalDateTime sentAt;
    private boolean read;

    private Message(Builder builder) {
        this.id              = builder.id;
        this.senderUsername  = builder.senderUsername;
        this.recipientUsername = builder.recipientUsername;
        this.content         = builder.content;
        this.sentAt          = builder.sentAt;
        this.read            = builder.read;
    }

    // ----------------------------------------------------------------
    // Builder
    // ----------------------------------------------------------------

    public static class Builder {
        private int id;
        private String senderUsername;
        private String recipientUsername;
        private String content;
        private LocalDateTime sentAt;
        private boolean read;

        public Builder id(int id)                             { this.id = id;                         return this; }
        public Builder senderUsername(String v)               { this.senderUsername = v;              return this; }
        public Builder recipientUsername(String v)            { this.recipientUsername = v;           return this; }
        public Builder content(String v)                      { this.content = v;                     return this; }
        public Builder sentAt(LocalDateTime v)                { this.sentAt = v;                      return this; }
        public Builder read(boolean v)                        { this.read = v;                        return this; }

        public Message build() { return new Message(this); }
    }

    // ----------------------------------------------------------------
    // Getter / Setter
    // ----------------------------------------------------------------

    public int            getId()                { return id; }
    public String         getSenderUsername()    { return senderUsername; }
    public String         getRecipientUsername() { return recipientUsername; }
    public String         getContent()           { return content; }
    public LocalDateTime  getSentAt()            { return sentAt; }
    public boolean        isRead()               { return read; }
    public void           setRead(boolean read)  { this.read = read; }
}
