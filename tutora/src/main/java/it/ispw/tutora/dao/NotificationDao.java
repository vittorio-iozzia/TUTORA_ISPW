package it.ispw.tutora.dao;

import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Notification;

import java.sql.Connection;
import java.util.List;

/**
 * Contratto DAO per la tabella notification.
 *
 * -----------------------------------------------------------------------
 * Nota sulle transazioni
 * -----------------------------------------------------------------------
 * I metodi di scrittura (insert, markAsRead, markAllAsRead) ricevono
 * la Connection come parametro: è il Controller applicativo a gestire
 * commit e rollback, non il DAO.
 * insert viene tipicamente invocato nella stessa transazione in cui
 * avviene l'operazione che genera la notifica (es. cambio di stato
 * di una TutorApplication).
 *
 * -----------------------------------------------------------------------
 * Nota su senderUsername
 * -----------------------------------------------------------------------
 * Il campo senderUsername può essere null per le notifiche generate
 * automaticamente dal sistema. L'implementazione concreta deve
 * gestire correttamente il NULL nel PreparedStatement e nel ResultSet.
 */
public interface NotificationDao {

    /**
     * Persiste una nuova notifica.
     */
    int insert(Connection conn, Notification notification)
            throws DatabaseException;

    /**
     * Carica tutte le notifiche di un destinatario,
     * ordinate per timestamp decrescente (più recenti prima).
     */
    List<Notification> findByRecipient(Connection conn, String recipientUsername)
            throws DatabaseException;

    /**
     * Segna una singola notifica come letta.
     * Se la notifica è già letta o non esiste, non solleva eccezioni:
     * l'operazione è idempotente.
     */
    void markAsRead(Connection conn, int notificationId)
            throws DatabaseException;

    /**
     * Segna come lette tutte le notifiche non lette di un destinatario.
     * Operazione bulk — usata quando l'utente apre il pannello notifiche.
     */
    void markAllAsRead(Connection conn, String recipientUsername)
            throws DatabaseException;

    /**
     * Restituisce il numero di notifiche non lette di un destinatario.
     * Usata per aggiornare il badge nell'interfaccia.
     */
    int countUnread(Connection conn, String recipientUsername)
            throws DatabaseException;
}
