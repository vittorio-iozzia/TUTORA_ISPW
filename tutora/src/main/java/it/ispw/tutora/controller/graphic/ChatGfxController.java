package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.dao.MessageDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Message;
import it.ispw.tutora.model.User;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.SceneManager;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

/**
 * Controller del frammento messages_content.fxml.
 *
 * Gestisce la chat locale tra studenti e tutor.
 * I contatti provengono dalle prenotazioni esistenti.
 * Le immagini dei profili vengono caricate da Unsplash in modo asincrono.
 */
public class ChatGfxController {

    private static final Logger LOGGER = Logger.getLogger(ChatGfxController.class.getName());

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.ENGLISH);

    // Per-username portrait URLs — must match StudentContentController.PHOTO_URLS (same person, same photo)
    private static final Map<String, String> PHOTO_URLS = Map.of(
        "tutor_vitto",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=120&h=120&fit=crop&crop=faces"
    );

    // Fallback pool for usernames not in PHOTO_URLS
    private static final List<String> PORTRAIT_POOL = List.of(
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=120&h=120&fit=crop&crop=faces",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=120&h=120&fit=crop&crop=faces",
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&h=120&fit=crop&crop=faces",
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=120&h=120&fit=crop&crop=faces"
    );

    // ----------------------------------------------------------------
    // FXML
    // ----------------------------------------------------------------

    @FXML private VBox       chatSidebar;
    @FXML private VBox       contactList;
    @FXML private HBox       chatHeader;
    @FXML private StackPane  chatHeaderAvatar;
    @FXML private Label      chatHeaderInitial;
    @FXML private ImageView  chatHeaderImageView;
    @FXML private Label      chatHeaderName;
    @FXML private Label      chatHeaderRole;
    @FXML private Button     callBtn;
    @FXML private Button     videoCallBtn;
    @FXML private Button     moreBtn;
    @FXML private VBox       emptyState;
    @FXML private ScrollPane messagesScroll;
    @FXML private VBox       messagesContainer;
    @FXML private HBox       inputArea;
    @FXML private TextField  messageInput;
    @FXML private Button     sendBtn;

    // ----------------------------------------------------------------
    // State
    // ----------------------------------------------------------------

    private String  currentUsername;
    private boolean isStudent;

    private String selectedContactUsername;

    private Button activeContactBtn;
    private Timeline pollTimeline;

    // Maps contact username → Unsplash pool index (stable per session)
    private final Map<String, Integer> contactIndexMap = new LinkedHashMap<>();

    // ----------------------------------------------------------------
    // Initialize
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        String token    = SceneManager.getInstance().getSessionToken();
        Session session = SessionManager.getInstance().getSession(token);
        currentUsername = session.getUser().getUsername();
        isStudent       = session.isStudent();

        clipToRoundedRect(chatSidebar, 12);
        loadContacts();
        startPolling();
    }

    // ----------------------------------------------------------------
    // Clip a Region to a rounded rectangle so hover states don't bleed
    // outside the card corners
    // ----------------------------------------------------------------

    private void clipToRoundedRect(javafx.scene.layout.Region node, double radius) {
        if (node == null) return;
        Platform.runLater(() -> {
            Rectangle clip = new Rectangle();
            clip.setArcWidth(radius * 2);
            clip.setArcHeight(radius * 2);
            clip.widthProperty().bind(node.widthProperty());
            clip.heightProperty().bind(node.heightProperty());
            node.setClip(clip);
        });
    }

    // ----------------------------------------------------------------
    // Load contacts from bookings
    // ----------------------------------------------------------------

    private void loadContacts() {
        try {
            BookingDao bookingDao = DaoFactory.getInstance().createBookingDao();
            List<Booking> bookings = isStudent
                    ? bookingDao.findByStudent(DaoFactory.getInstance().getConnection(), currentUsername)
                    : bookingDao.findByTutor(DaoFactory.getInstance().getConnection(), currentUsername);

            LinkedHashMap<String, User> contacts = new LinkedHashMap<>();
            for (Booking b : bookings) {
                User contact = isStudent
                        ? b.getLesson().getExpertise().getTutor()
                        : b.getStudent();
                contacts.putIfAbsent(contact.getUsername(), contact);
            }

            if (contacts.isEmpty()) {
                Label empty = new Label(isStudent
                        ? "No tutors yet.\nBook a lesson to start chatting!"
                        : "No students have booked yet.");
                empty.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:14px;-fx-padding:24;-fx-wrap-text:true;-fx-text-alignment:center;");
                empty.setWrapText(true);
                contactList.getChildren().add(empty);
                return;
            }

            String contactRole = isStudent ? "Tutor" : "Student";
            int idx = 0;
            for (Map.Entry<String, User> e : contacts.entrySet()) {
                String uname = e.getKey();
                if (!PHOTO_URLS.containsKey(uname)) {
                    contactIndexMap.put(uname, idx);
                    idx++;
                }
                contactList.getChildren().add(
                        buildContactItem(uname, e.getValue().getFullName(), contactRole));
            }

        } catch (DatabaseException e) {
            LOGGER.warning("Cannot load contacts: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Build a contact list item
    // ----------------------------------------------------------------

    private Button buildContactItem(String username, String fullName, String role) {

        // ── Avatar (circle with Unsplash photo) ──
        StackPane avatar = buildAvatarPane(username, fullName, 42);

        // ── Right side: name + preview ──
        VBox textBox = new VBox(3);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox nameRow = new HBox(6);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(fullName);
        nameLabel.getStyleClass().add("chat-contact-name");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label timeLabel = new Label(getLastMessageTime(username));
        timeLabel.getStyleClass().add("chat-contact-time");
        nameRow.getChildren().addAll(nameLabel, spacer, timeLabel);

        Label preview = new Label(getLastMessagePreview(username));
        preview.getStyleClass().add("chat-contact-preview");
        preview.setMaxWidth(160);

        textBox.getChildren().addAll(nameRow, preview);

        // ── Unread dot ──
        Region unreadDot = new Region();
        unreadDot.getStyleClass().add("chat-unread-dot");
        long unread = countUnread(username);
        unreadDot.setVisible(unread > 0);
        unreadDot.setManaged(unread > 0);

        // ── Row ──
        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().addAll(avatar, textBox, unreadDot);

        Button btn = new Button();
        btn.setUserData(username);
        btn.getStyleClass().add("chat-contact-item");
        btn.setGraphic(content);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> selectContact(btn, username, fullName, role));

        return btn;
    }

    // ----------------------------------------------------------------
    // Build a circular avatar StackPane with async Unsplash photo
    // ----------------------------------------------------------------

    private StackPane buildAvatarPane(String username, String fullName, double size) {
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("chat-contact-avatar");
        avatar.setMinSize(size, size);
        avatar.setMaxSize(size, size);

        // Fallback letter
        Label initial = new Label(String.valueOf(fullName.charAt(0)).toUpperCase());
        initial.getStyleClass().add("chat-contact-initial");
        avatar.getChildren().add(initial);

        // Photo overlay (clipped to circle)
        ImageView imgView = new ImageView();
        imgView.setFitWidth(size);
        imgView.setFitHeight(size);
        imgView.setPreserveRatio(false);
        imgView.setSmooth(true);
        imgView.setVisible(false);

        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(size);
        clip.setArcHeight(size);
        imgView.setClip(clip);

        avatar.getChildren().add(imgView);

        // Prefer the per-username photo; fall back to pool
        String url = PHOTO_URLS.containsKey(username)
                ? PHOTO_URLS.get(username)
                : PORTRAIT_POOL.get(contactIndexMap.getOrDefault(username, 0) % PORTRAIT_POOL.size());

        Image img = new Image(url, size, size, false, true, true);
        img.progressProperty().addListener((obs, oldV, newV) -> {
            if (newV.doubleValue() >= 1.0 && !img.isError()) {
                imgView.setImage(img);
                imgView.setVisible(true);
            }
        });

        return avatar;
    }

    private String resolvePhotoUrl(String username, double size) {
        if (PHOTO_URLS.containsKey(username)) return PHOTO_URLS.get(username);
        int idx = contactIndexMap.getOrDefault(username, 0) % PORTRAIT_POOL.size();
        return PORTRAIT_POOL.get(idx);
    }

    // ----------------------------------------------------------------
    // Select a conversation
    // ----------------------------------------------------------------

    private void selectContact(Button btn, String username, String fullName, String role) {
        if (activeContactBtn != null) activeContactBtn.getStyleClass().remove("chat-contact-active");
        activeContactBtn = btn;
        btn.getStyleClass().add("chat-contact-active");

        selectedContactUsername = username;

        // Update header text
        chatHeaderInitial.setText(String.valueOf(fullName.charAt(0)).toUpperCase());
        chatHeaderName.setText(fullName);
        chatHeaderRole.setText(role);

        // Reload header avatar photo (use same URL logic as the contact list)
        chatHeaderImageView.setVisible(false);
        String url = resolvePhotoUrl(username, 44);
        Image img = new Image(url, 44, 44, false, true, true);
        img.progressProperty().addListener((obs, oldV, newV) -> {
            if (newV.doubleValue() >= 1.0 && !img.isError()) {
                chatHeaderImageView.setImage(img);
                chatHeaderImageView.setVisible(true);
                chatHeaderInitial.setVisible(false);
            }
        });

        // Clip header image to circle
        Rectangle clip = new Rectangle(44, 44);
        clip.setArcWidth(44);
        clip.setArcHeight(44);
        chatHeaderImageView.setClip(clip);

        // Show chat panel
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        chatHeader.setVisible(true);
        chatHeader.setManaged(true);
        messagesScroll.setVisible(true);
        messagesScroll.setManaged(true);
        inputArea.setVisible(true);
        inputArea.setManaged(true);

        loadMessages();
        markMessagesRead();
        refreshContactItem(btn, username);
    }

    // ----------------------------------------------------------------
    // Load messages
    // ----------------------------------------------------------------

    private void loadMessages() {
        if (selectedContactUsername == null) return;
        try {
            MessageDao msgDao = DaoFactory.getInstance().createMessageDao();
            List<Message> messages = msgDao.getConversation(
                    DaoFactory.getInstance().getConnection(),
                    currentUsername, selectedContactUsername);

            messagesContainer.getChildren().clear();

            if (messages.isEmpty()) {
                Label hint = new Label("No messages yet. Say hello! 👋");
                hint.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:14px;-fx-padding:12;");
                messagesContainer.getChildren().add(hint);
            } else {
                String prevSender = null;
                for (int i = 0; i < messages.size(); i++) {
                    Message msg  = messages.get(i);
                    boolean last = (i == messages.size() - 1) || !messages.get(i + 1).getSenderUsername().equals(msg.getSenderUsername());
                    boolean first = (prevSender == null || !prevSender.equals(msg.getSenderUsername()));
                    messagesContainer.getChildren().add(buildMessageBubble(msg, first, last));
                    prevSender = msg.getSenderUsername();
                }
            }

            Platform.runLater(() -> messagesScroll.setVvalue(1.0));

        } catch (DatabaseException e) {
            LOGGER.warning("Cannot load messages: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Build a message bubble
    // ----------------------------------------------------------------

    private HBox buildMessageBubble(Message msg, boolean isFirst, boolean isLast) {
        boolean isMine = msg.getSenderUsername().equals(currentUsername);

        // Content label
        Label contentLabel = new Label(msg.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(420);
        contentLabel.getStyleClass().add(isMine ? "chat-bubble-text-sent" : "chat-bubble-text-recv");

        // Timestamp
        Label timeLabel = new Label(formatTime(msg.getSentAt()));
        timeLabel.getStyleClass().add(isMine ? "chat-time-sent" : "chat-time-recv");

        // Bubble VBox
        VBox bubble = new VBox(5);
        bubble.getChildren().addAll(contentLabel, timeLabel);
        bubble.setMaxWidth(460);

        // Radius: first in a group has the "tail" corner; last shows time; inner is fully rounded
        if (isMine) {
            bubble.getStyleClass().add("chat-bubble-sent");
            if (isFirst && isLast)     bubble.getStyleClass().add("chat-bubble-sent-solo");
            else if (isFirst)          bubble.getStyleClass().add("chat-bubble-sent-first");
            else if (isLast)           bubble.getStyleClass().add("chat-bubble-sent-last");
            else                       bubble.getStyleClass().add("chat-bubble-sent-mid");
        } else {
            bubble.getStyleClass().add("chat-bubble-recv");
            if (isFirst && isLast)     bubble.getStyleClass().add("chat-bubble-recv-solo");
            else if (isFirst)          bubble.getStyleClass().add("chat-bubble-recv-first");
            else if (isLast)           bubble.getStyleClass().add("chat-bubble-recv-last");
            else                       bubble.getStyleClass().add("chat-bubble-recv-mid");
        }

        HBox row = new HBox();
        row.setMaxWidth(Double.MAX_VALUE);

        VBox.setMargin(bubble, isMine
                ? new Insets(isFirst ? 6 : 2, 0, 0, 0)
                : new Insets(isFirst ? 6 : 2, 0, 0, 0));

        if (isMine) {
            row.setAlignment(Pos.CENTER_RIGHT);
            HBox.setMargin(bubble, new Insets(0, 0, 0, 80));
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setMargin(bubble, new Insets(0, 80, 0, 0));
        }
        row.getChildren().add(bubble);
        return row;
    }

    // ----------------------------------------------------------------
    // Send
    // ----------------------------------------------------------------

    @FXML
    private void handleSend() {
        if (selectedContactUsername == null) return;
        String text = messageInput.getText().strip();
        if (text.isEmpty()) return;
        messageInput.clear();

        try {
            MessageDao msgDao = DaoFactory.getInstance().createMessageDao();
            msgDao.insert(DaoFactory.getInstance().getConnection(),
                    new Message.Builder()
                            .senderUsername(currentUsername)
                            .recipientUsername(selectedContactUsername)
                            .content(text)
                            .sentAt(LocalDateTime.now())
                            .build());
        } catch (DatabaseException e) {
            LOGGER.warning("Cannot send message: " + e.getMessage());
            return;
        }

        loadMessages();
        refreshContactItem(activeContactBtn, selectedContactUsername);
    }

    // ----------------------------------------------------------------
    // Header action handlers (call / video / more)
    // ----------------------------------------------------------------

    @FXML
    private void handleCall() {
        // Placeholder — voice call not yet implemented
        LOGGER.info("Voice call requested for: " + selectedContactUsername);
    }

    @FXML
    private void handleVideo() {
        // Placeholder — video call not yet implemented
        LOGGER.info("Video call requested for: " + selectedContactUsername);
    }

    @FXML
    private void handleMore() {
        // Placeholder — context menu (mute, block, clear) not yet implemented
        LOGGER.info("More options requested for: " + selectedContactUsername);
    }

    // ----------------------------------------------------------------
    // Mark read
    // ----------------------------------------------------------------

    private void markMessagesRead() {
        if (selectedContactUsername == null) return;
        try {
            DaoFactory.getInstance().createMessageDao()
                    .markConversationRead(DaoFactory.getInstance().getConnection(),
                            selectedContactUsername, currentUsername);
        } catch (DatabaseException e) {
            LOGGER.warning("Cannot mark as read: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Polling
    // ----------------------------------------------------------------

    private void startPolling() {
        pollTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (selectedContactUsername == null) return;
            refreshMessagesIfNeeded();
            markMessagesRead();
            refreshAllContactItems();
        }));
        pollTimeline.setCycleCount(Timeline.INDEFINITE);
        pollTimeline.play();
    }

    private void refreshMessagesIfNeeded() {
        try {
            MessageDao msgDao = DaoFactory.getInstance().createMessageDao();
            List<Message> messages = msgDao.getConversation(
                    DaoFactory.getInstance().getConnection(),
                    currentUsername, selectedContactUsername);
            long rendered = messagesContainer.getChildren().stream()
                    .filter(n -> n instanceof HBox).count();
            if (messages.size() != rendered) loadMessages();
        } catch (DatabaseException e) {
            LOGGER.warning("Poll error: " + e.getMessage());
        }
    }

    private void refreshAllContactItems() {
        for (var node : contactList.getChildren()) {
            if (node instanceof Button btn && btn.getUserData() instanceof String username) {
                refreshContactItem(btn, username);
            }
        }
    }

    // ----------------------------------------------------------------
    // Refresh a contact item's preview / unread dot / time
    // ----------------------------------------------------------------

    private void refreshContactItem(Button btn, String username) {
        if (btn == null || !(btn.getGraphic() instanceof HBox row)) return;

        for (var child : row.getChildren()) {
            if (child instanceof VBox textBox && textBox.getChildren().size() >= 2) {
                // nameRow → update time label
                if (textBox.getChildren().get(0) instanceof HBox nameRow) {
                    for (var nr : nameRow.getChildren()) {
                        if (nr instanceof Label lbl && lbl.getStyleClass().contains("chat-contact-time")) {
                            lbl.setText(getLastMessageTime(username));
                        }
                    }
                }
                // preview
                if (textBox.getChildren().get(1) instanceof Label previewLabel) {
                    previewLabel.setText(getLastMessagePreview(username));
                }
            }
            if (child instanceof Region dot && dot.getStyleClass().contains("chat-unread-dot")) {
                long unread = countUnread(username);
                dot.setVisible(unread > 0);
                dot.setManaged(unread > 0);
            }
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private String getLastMessagePreview(String contactUsername) {
        try {
            List<Message> msgs = DaoFactory.getInstance().createMessageDao()
                    .getConversation(DaoFactory.getInstance().getConnection(),
                            currentUsername, contactUsername);
            if (msgs.isEmpty()) return "No messages yet";
            Message last = msgs.get(msgs.size() - 1);
            String prefix = last.getSenderUsername().equals(currentUsername) ? "You: " : "";
            String body   = last.getContent();
            return prefix + (body.length() > 34 ? body.substring(0, 34) + "…" : body);
        } catch (DatabaseException e) {
            return "";
        }
    }

    private String getLastMessageTime(String contactUsername) {
        try {
            List<Message> msgs = DaoFactory.getInstance().createMessageDao()
                    .getConversation(DaoFactory.getInstance().getConnection(),
                            currentUsername, contactUsername);
            if (msgs.isEmpty()) return "";
            return formatTime(msgs.get(msgs.size() - 1).getSentAt());
        } catch (DatabaseException e) {
            return "";
        }
    }

    private long countUnread(String contactUsername) {
        try {
            return DaoFactory.getInstance().createMessageDao()
                    .getConversation(DaoFactory.getInstance().getConnection(),
                            currentUsername, contactUsername)
                    .stream()
                    .filter(m -> m.getRecipientUsername().equals(currentUsername) && !m.isRead())
                    .count();
        } catch (DatabaseException e) {
            return 0;
        }
    }

    private String formatTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.toLocalDate().equals(java.time.LocalDate.now())
                ? dt.format(TIME_FMT)
                : dt.format(DATE_FMT);
    }
}
