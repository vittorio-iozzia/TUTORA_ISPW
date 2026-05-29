package it.ispw.tutora.controller.application;

import it.ispw.tutora.bean.ChatContactBean;
import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.dao.MessageDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Message;
import it.ispw.tutora.model.User;
import it.ispw.tutora.model.session.SessionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Application controller per la chat.
 *
 * Responsabilità:
 *  - caricamento dei contatti (dai booking come studente e come tutor)
 *  - lettura e invio messaggi
 *  - segna messaggi come letti
 *  - preview, timestamp e contatore non letti per ogni conversazione
 *  - contatore totale non letti (per il badge nella sidebar)
 */
public class ChatController {

    private static final Logger LOGGER = Logger.getLogger(ChatController.class.getName());

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.ENGLISH);

    private final BookingDao bookingDao;
    private final MessageDao messageDao;

    public ChatController() {
        DaoFactory factory = DaoFactory.getInstance();
        this.bookingDao = factory.createBookingDao();
        this.messageDao = factory.createMessageDao();
    }

    /**
     * Restituisce la lista dei contatti dell'utente autenticato.
     * I contatti vengono estratti dalle prenotazioni (come studente e come tutor).
     * L'ordine di inserimento è preservato; i duplicati sono ignorati.
     */
    public List<ChatContactBean> getContactsForUser(String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return Collections.emptyList();
        String currentUsername = sm.getCurrentUser(token).getUsername();

        LinkedHashMap<String, ChatContactBean> contacts = new LinkedHashMap<>();

        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            loadStudentContacts(conn, currentUsername, contacts);
            loadTutorContacts(conn, currentUsername, contacts);
        } catch (DatabaseException | SQLException e) {
            LOGGER.warning("Cannot open connection for contacts: " + e.getMessage());
        }

        return List.copyOf(contacts.values());
    }

    /**
     * Restituisce tutti i messaggi di una conversazione tra due utenti,
     * ordinati per sentAt crescente.
     */
    public List<Message> getConversation(String user1, String user2) {
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            return messageDao.getConversation(conn, user1, user2);
        } catch (DatabaseException | SQLException e) {
            LOGGER.warning("Cannot load conversation: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Invia un messaggio dal mittente autenticato al destinatario indicato.
     *
     * @return {@code true} se l'invio è riuscito, {@code false} altrimenti.
     */
    public boolean sendMessage(String token, String recipientUsername, String content) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return false;
        String senderUsername = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            messageDao.insert(conn,
                    new Message.Builder()
                            .senderUsername(senderUsername)
                            .recipientUsername(recipientUsername)
                            .content(content)
                            .sentAt(LocalDateTime.now())
                            .build());
            return true;
        } catch (DatabaseException | SQLException e) {
            LOGGER.warning("Cannot send message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Segna come letti tutti i messaggi inviati da {@code contactUsername}
     * all'utente autenticato (identificato tramite token).
     */
    public void markConversationRead(String token, String contactUsername) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return;
        String recipientUsername = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            messageDao.markConversationRead(conn, contactUsername, recipientUsername);
        } catch (DatabaseException | SQLException e) {
            LOGGER.warning("Cannot mark conversation as read: " + e.getMessage());
        }
    }

    /**
     * Restituisce una breve anteprima dell'ultimo messaggio tra i due utenti.
     */
    public String getLastMessagePreview(String currentUser, String contactUser) {
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            List<Message> msgs = messageDao.getConversation(conn, currentUser, contactUser);
            if (msgs.isEmpty()) return "No messages yet";
            Message last   = msgs.get(msgs.size() - 1);
            String prefix  = last.getSenderUsername().equals(currentUser) ? "You: " : "";
            String body    = last.getContent();
            return prefix + (body.length() > 34 ? body.substring(0, 34) + "…" : body);
        } catch (DatabaseException | SQLException e) {
            return "";
        }
    }

    /**
     * Restituisce il timestamp formattato dell'ultimo messaggio tra i due utenti.
     */
    public String getLastMessageTime(String currentUser, String contactUser) {
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            List<Message> msgs = messageDao.getConversation(conn, currentUser, contactUser);
            if (msgs.isEmpty()) return "";
            return formatTime(msgs.get(msgs.size() - 1).getSentAt());
        } catch (DatabaseException | SQLException e) {
            return "";
        }
    }

    /**
     * Conta i messaggi non letti inviati da {@code contactUser} a {@code currentUser}.
     */
    public long countUnreadFrom(String currentUser, String contactUser) {
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            return messageDao.getConversation(conn, currentUser, contactUser).stream()
                    .filter(m -> m.getRecipientUsername().equals(currentUser) && !m.isRead())
                    .count();
        } catch (DatabaseException | SQLException e) {
            return 0;
        }
    }

    /**
     * Conta il totale dei messaggi non letti ricevuti dall'utente autenticato.
     * Usato per il badge nella sidebar.
     */
    public int countTotalUnread(String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return 0;
        String username = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            return messageDao.countTotalUnread(conn, username);
        } catch (DatabaseException | SQLException e) {
            return 0;
        }
    }

    // ----------------------------------------------------------------
    // Helpers — caricamento contatti
    // ----------------------------------------------------------------

    private void loadStudentContacts(Connection conn, String username,
                                     LinkedHashMap<String, ChatContactBean> contacts) {
        try {
            for (var b : bookingDao.findByStudent(conn, username)) {
                User tutor = b.getLesson() != null && b.getLesson().getExpertise() != null
                        ? b.getLesson().getExpertise().getTutor() : null;
                if (tutor == null || tutor.getUsername() == null) continue;
                contacts.putIfAbsent(tutor.getUsername(),
                        new ChatContactBean(tutor.getUsername(), tutor.getFullName(), "Tutor"));
            }
        } catch (DatabaseException | RuntimeException e) {
            LOGGER.warning("Cannot load student contacts: " + e.getMessage());
        }
    }

    private void loadTutorContacts(Connection conn, String username,
                                   LinkedHashMap<String, ChatContactBean> contacts) {
        try {
            for (var b : bookingDao.findByTutor(conn, username)) {
                User student = b.getStudent();
                if (student == null || student.getUsername() == null) continue;
                contacts.putIfAbsent(student.getUsername(),
                        new ChatContactBean(student.getUsername(), student.getFullName(), "Student"));
            }
        } catch (DatabaseException | RuntimeException e) {
            LOGGER.warning("Cannot load tutor contacts: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Utility
    // ----------------------------------------------------------------

    private String formatTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.toLocalDate().equals(LocalDate.now())
                ? dt.format(TIME_FMT)
                : dt.format(DATE_FMT);
    }
}
