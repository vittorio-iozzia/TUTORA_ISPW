package it.ispw.tutora.dao.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.ispw.tutora.dao.NotificationDao;
import it.ispw.tutora.enums.NotificationType;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Notification;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementazione di NotificationDao basata su file JSON.
 * Usata quando DAO_TYPE=JSON in app.properties.
 *
 * -----------------------------------------------------------------------
 * Persistenza
 * -----------------------------------------------------------------------
 * Legge e scrive su ../tutora_data/notifications.json.
 * Ogni operazione di scrittura (insert, markAsRead, markAllAsRead) applica
 * il pattern read-modify-write: legge il file, modifica la lista in memoria,
 * riscrive il file aggiornato.
 *
 * -----------------------------------------------------------------------
 * Nota sul parametro Connection
 * -----------------------------------------------------------------------
 * Il parametro Connection viene ignorato: non c'è nessun DB.
 * È presente solo per rispettare il contratto dell'interfaccia NotificationDao,
 * pensata per la gestione delle transazioni JDBC.
 *
 * -----------------------------------------------------------------------
 * LocalDateTime e senderUsername nullable
 * -----------------------------------------------------------------------
 * Le date vengono serializzate come stringhe ISO-8601 e ricostruite con
 * LocalDateTime.parse(). Il campo senderUsername può essere null per le
 * notifiche generate automaticamente dal sistema.
 */
public class NotificationDaoJson implements NotificationDao {

    private static final String JSON_PATH = "../tutora_data/notifications.json";
    private final ObjectMapper mapper = new ObjectMapper();

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    @Override
    public int insert(Connection conn, Notification notification) throws DatabaseException {
        List<NotifRecord> records = readAll();

        // Simulo in Json un ID auto-incremented
        int id = records.stream()                   // Crea uno stream di AppRecord
                .mapToInt(r -> r.id)     // Trasforma ogni AppRecord nel suo id (IntStream)
                .max()                              // Trova il valore massimo
                .orElse(0)                    // Se la lista è vuota restituisce zero
                + 1;                                // Aggiunge uno per otterene il prossimo id

        NotifRecord r = new NotifRecord();
        r.id = id;
        r.recipientUsername = notification.getRecipientUsername();
        r.senderUsername = notification.getSenderUsername();
        r.message = notification.getMessage();
        r.type = notification.getType().name();
        r.targetId = notification.getTargetId();
        r.timestamp = notification.getTimestamp().toString();
        r.read = notification.isRead();

        records.add(r);
        writeAll(records);
        return id;
    }

    // ----------------------------------------------------------------
    // findByRecipient
    // ----------------------------------------------------------------

    @Override
    public List<Notification> findByRecipient(Connection conn, String recipientUsername)
            throws DatabaseException {

        List<Notification> result = new ArrayList<>();
        for (NotifRecord r : readAll()) {
            if (r.recipientUsername.equals(recipientUsername)) result.add(toNotification(r));
        }
        // Ordine decrescente per timestamp (più recenti prima)
        result.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return result;
    }

    // ----------------------------------------------------------------
    // markAsRead
    // ----------------------------------------------------------------

    @Override
    public void markAsRead(Connection conn, int notificationId) throws DatabaseException {
        List<NotifRecord> records = readAll();
        for (NotifRecord r : records) {
            if (r.id == notificationId) {
                r.read = true;
                writeAll(records);
                return;
            }
        }
        // Idempotente: se l'id non esiste non solleva eccezioni
    }

    // ----------------------------------------------------------------
    // markAllAsRead
    // ----------------------------------------------------------------

    @Override
    public void markAllAsRead(Connection conn, String recipientUsername)
            throws DatabaseException {

        List<NotifRecord> records = readAll();
        for (NotifRecord r : records) {
            if (r.recipientUsername.equals(recipientUsername) && !r.read) {
                r.read = true;
            }
        }
        writeAll(records);
    }

    // ----------------------------------------------------------------
    // countUnread
    // ----------------------------------------------------------------

    @Override
    public int countUnread(Connection conn, String recipientUsername)
            throws DatabaseException {

        int count = 0;
        for (NotifRecord r : readAll()) {
            if (r.recipientUsername.equals(recipientUsername) && !r.read) count++;
        }
        return count;
    }

    // ----------------------------------------------------------------
    // Helper privati
    // ----------------------------------------------------------------

    private List<NotifRecord> readAll() throws DatabaseException {
        try {
            NotifRecord[] records = mapper.readValue(new File(JSON_PATH), NotifRecord[].class);
            return new ArrayList<>(Arrays.asList(records));
        } catch (IOException e) {
            throw new DatabaseException("Error reading JSON file: " + JSON_PATH, e);
        }
    }

    private void writeAll(List<NotifRecord> records) throws DatabaseException {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(JSON_PATH), records);
        } catch (IOException e) {
            throw new DatabaseException("Error writing JSON file: " + JSON_PATH, e);
        }
    }

    private Notification toNotification(NotifRecord r) {
        return new Notification.Builder()
                .id(r.id)
                .recipientUsername(r.recipientUsername)
                .senderUsername(r.senderUsername)
                .message(r.message)
                .type(NotificationType.valueOf(r.type))
                .targetId(r.targetId)
                .timestamp(LocalDateTime.parse(r.timestamp))
                .read(r.read)
                .build();
    }

    // ----------------------------------------------------------------
    // POJO interno per la deserializzazione Jackson
    // ----------------------------------------------------------------

    private static class NotifRecord {
        public int     id;
        public String  recipientUsername;
        public String  senderUsername;
        public String  message;
        public String  type;
        public Integer targetId;
        public String  timestamp;
        public boolean read;
    }
}
