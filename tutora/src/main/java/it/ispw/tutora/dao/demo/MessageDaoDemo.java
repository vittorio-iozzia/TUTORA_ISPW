package it.ispw.tutora.dao.demo;

import it.ispw.tutora.dao.MessageDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Message;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Implementazione in memoria di MessageDao.
 * I dati persistono per tutta la durata dell'esecuzione dell'applicazione,
 * permettendo l'invio di messaggi tra sessioni diverse nello stesso processo.
 *
 * Il parametro Connection è ignorato — non c'è nessun DB.
 * Il ciclo di vita è gestito da DemoDaoFactory.
 */
public class MessageDaoDemo implements MessageDao {

    private final List<Message> cache = new ArrayList<>();
    private int nextId = 1;

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    @Override
    public int insert(Connection conn, Message message) throws DatabaseException {
        int id = nextId++;
        cache.add(new Message.Builder()
                .id(id)
                .senderUsername(message.getSenderUsername())
                .recipientUsername(message.getRecipientUsername())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .read(message.isRead())
                .build());
        return id;
    }

    // ----------------------------------------------------------------
    // getConversation
    // ----------------------------------------------------------------

    @Override
    public List<Message> getConversation(Connection conn, String user1, String user2)
            throws DatabaseException {

        return cache.stream()
                .filter(m -> (m.getSenderUsername().equals(user1) && m.getRecipientUsername().equals(user2))
                          || (m.getSenderUsername().equals(user2) && m.getRecipientUsername().equals(user1)))
                .sorted(Comparator.comparing(Message::getSentAt))
                .toList();
    }

    // ----------------------------------------------------------------
    // markConversationRead
    // ----------------------------------------------------------------

    @Override
    public void markConversationRead(Connection conn, String senderUsername, String recipientUsername)
            throws DatabaseException {

        cache.stream()
                .filter(m -> m.getSenderUsername().equals(senderUsername)
                        && m.getRecipientUsername().equals(recipientUsername)
                        && !m.isRead())
                .forEach(m -> m.setRead(true));
    }

    // ----------------------------------------------------------------
    // countTotalUnread
    // ----------------------------------------------------------------

    @Override
    public int countTotalUnread(Connection conn, String recipientUsername) throws DatabaseException {
        return (int) cache.stream()
                .filter(m -> m.getRecipientUsername().equals(recipientUsername) && !m.isRead())
                .count();
    }
}
