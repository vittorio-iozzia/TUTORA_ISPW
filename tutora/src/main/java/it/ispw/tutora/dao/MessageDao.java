package it.ispw.tutora.dao;

import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Message;

import java.sql.Connection;
import java.util.List;

/**
 * Contratto DAO per la tabella message (chat tra utenti).
 */
public interface MessageDao {

    /**
     * Persiste un nuovo messaggio.
     *
     * @return l'id generato
     */
    int insert(Connection conn, Message message) throws DatabaseException;

    /**
     * Restituisce tutti i messaggi tra due utenti, ordinati per sentAt ASC.
     */
    List<Message> getConversation(Connection conn, String user1, String user2)
            throws DatabaseException;

    /**
     * Segna come letti tutti i messaggi inviati da {@code senderUsername}
     * al {@code recipientUsername} che risultano non ancora letti.
     */
    void markConversationRead(Connection conn, String senderUsername, String recipientUsername)
            throws DatabaseException;
}
