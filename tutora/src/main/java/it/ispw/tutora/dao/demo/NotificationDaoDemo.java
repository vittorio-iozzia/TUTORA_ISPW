package it.ispw.tutora.dao.demo;

import it.ispw.tutora.dao.NotificationDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Notification;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Implementazione in memoria di NotificationDao.
 * Usata al posto di NotificationDaoDb quando il DB non è disponibile.
 *
 * -----------------------------------------------------------------------
 * Nota
 * -----------------------------------------------------------------------
 * Il parametro Connection viene ignorato — non c'è nessun DB.
 * I dati vivono in memoria per tutta la durata dell'esecuzione
 * e spariscono alla chiusura dell'applicazione.
 * Il ciclo di vita è gestito da DemoDaoFactory — non serve Singleton.
 */
public class NotificationDaoDemo implements NotificationDao {

    private final List<Notification> cache = new ArrayList<>();
    private int nextId = 1;

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    @Override
    public int insert(Connection conn, Notification notification)
            throws DatabaseException {
        int id = nextId++;
        cache.add(new Notification.Builder()
                .id(id)
                .recipientUsername(notification.getRecipientUsername())
                .senderUsername(notification.getSenderUsername())
                .message(notification.getMessage())
                .type(notification.getType())
                .targetId(notification.getTargetId())
                .timestamp(notification.getTimestamp())
                .read(notification.isRead())
                .build());
        return id;
    }

    // ----------------------------------------------------------------
    // findByRecipient
    // ----------------------------------------------------------------

    @Override
    public List<Notification> findByRecipient(Connection conn, String recipientUsername)
            throws DatabaseException {

        // Trasformo la lista in stream, applico il filtro in base al RecipientUsername, raccolgo gli elementi in una List immutabile
        return cache.stream()
                .filter(n -> n.getRecipientUsername().equals(recipientUsername))
                .sorted(Comparator.comparing(Notification::getTimestamp).reversed())
                .toList();
    }

    // ----------------------------------------------------------------
    // markAsRead
    // ----------------------------------------------------------------

    @Override
    public void markAsRead(Connection conn, int notificationId)
            throws DatabaseException {
        cache.stream()
                .filter(n -> n.getId() == notificationId)
                .findFirst()
                .ifPresent(n -> n.setRead(true));
    }

    // ----------------------------------------------------------------
    // markAllAsRead
    // ----------------------------------------------------------------

    @Override
    public void markAllAsRead(Connection conn, String recipientUsername)
            throws DatabaseException {
        cache.stream()
                .filter(n -> n.getRecipientUsername().equals(recipientUsername)
                        && !n.isRead())
                .forEach(n -> n.setRead(true));
    }

    // ----------------------------------------------------------------
    // countUnread
    // ----------------------------------------------------------------

    @Override
    public int countUnread(Connection conn, String recipientUsername)
            throws DatabaseException {
        return (int) cache.stream()
                .filter(n -> n.getRecipientUsername().equals(recipientUsername)
                        && !n.isRead())
                .count();
    }

    // ----------------------------------------------------------------
    // markReadByTargetIdAndRecipient
    // ----------------------------------------------------------------

    @Override
    public void markReadByTargetIdAndRecipient(Connection conn, int targetId,
                                               String recipientUsername) {
        cache.stream()
                .filter(n -> n.getRecipientUsername().equals(recipientUsername)
                        && n.getTargetId() != null
                        && n.getTargetId() == targetId
                        && !n.isRead())
                .forEach(n -> n.setRead(true));
    }
}