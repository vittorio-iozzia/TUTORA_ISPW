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
import it.ispw.tutora.view.AvatarManager;
import it.ispw.tutora.view.SceneManager;

import java.io.File;

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
import java.util.logging.Level;
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

    private String selectedContactUsername;

    private Button activeContactBtn;

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
        BookingDao bookingDao = DaoFactory.getInstance().createBookingDao();

            // Load from both student and tutor bookings — handles users who changed role
            LinkedHashMap<String, User>   contacts = new LinkedHashMap<>();
            LinkedHashMap<String, String> roles    = new LinkedHashMap<>();

            try {
                for (Booking b : bookingDao.findByStudent(
                        DaoFactory.getInstance().getConnection(), currentUsername)) {
                    User contact = b.getLesson().getExpertise().getTutor();
                    if (contacts.putIfAbsent(contact.getUsername(), contact) == null)
                        roles.put(contact.getUsername(), "Tutor");
                }
            } catch (DatabaseException ignored) { /* contact load is best-effort: partial failures are acceptable */ }

            try {
                for (Booking b : bookingDao.findByTutor(
                        DaoFactory.getInstance().getConnection(), currentUsername)) {
                    User contact = b.getStudent();
                    if (contacts.putIfAbsent(contact.getUsername(), contact) == null)
                        roles.put(contact.getUsername(), "Student");
                }
            } catch (DatabaseException ignored) { /* contact load is best-effort: partial failures are acceptable */ }

            if (contacts.isEmpty()) {
                Label empty = new Label("No conversations yet.\nBook a lesson to start chatting!");
                empty.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:14px;-fx-padding:24;-fx-wrap-text:true;-fx-text-alignment:center;");
                empty.setWrapText(true);
                contactList.getChildren().add(empty);
                return;
            }

            int idx = 0;
            for (Map.Entry<String, User> e : contacts.entrySet()) {
                String uname = e.getKey();
                if (!PHOTO_URLS.containsKey(uname)) {
                    contactIndexMap.put(uname, idx++);
                }
                contactList.getChildren().add(
                        buildContactItem(uname, e.getValue().getFullName(), roles.get(uname)));
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

        // Load photo only if the user has set one or has a dedicated URL — never use pool for real users
        String url = null;
        if (AvatarManager.hasAvatar(username)) {
            url = new File(AvatarManager.getAvatarPath(username)).toURI().toString();
        } else if (PHOTO_URLS.containsKey(username)) {
            url = PHOTO_URLS.get(username);
        }

        if (url != null) {
            Image img = new Image(url, size, size, false, true, true);
            img.progressProperty().addListener((obs, oldV, newV) -> {
                if (newV.doubleValue() >= 1.0 && !img.isError()) {
                    imgView.setImage(img);
                    imgView.setVisible(true);
                }
            });
        }

        return avatar;
    }

    private String resolvePhotoUrl(String username) {
        if (AvatarManager.hasAvatar(username))
            return new File(AvatarManager.getAvatarPath(username)).toURI().toString();
        if (PHOTO_URLS.containsKey(username))
            return PHOTO_URLS.get(username);
        return null;
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
        chatHeaderInitial.setVisible(true);
        String url = resolvePhotoUrl(username);
        if (url != null) {
            Image img = new Image(url, 44, 44, false, true, true);
            img.progressProperty().addListener((obs, oldV, newV) -> {
                if (newV.doubleValue() >= 1.0 && !img.isError()) {
                    chatHeaderImageView.setImage(img);
                    chatHeaderImageView.setVisible(true);
                    chatHeaderInitial.setVisible(false);
                }
            });
        }

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

        Label contentLabel = new Label(msg.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(420);
        contentLabel.getStyleClass().add(isMine ? "chat-bubble-text-sent" : "chat-bubble-text-recv");

        Label timeLabel = new Label(formatTime(msg.getSentAt()));
        timeLabel.getStyleClass().add(isMine ? "chat-time-sent" : "chat-time-recv");

        VBox bubble = new VBox(5);
        bubble.getChildren().addAll(contentLabel, timeLabel);
        bubble.setMaxWidth(460);
        applyBubbleStyle(bubble, isMine, isFirst, isLast);

        HBox row = new HBox();
        row.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(bubble, new Insets(isFirst ? 6 : 2, 0, 0, 0));

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

    private void applyBubbleStyle(VBox bubble, boolean isMine, boolean isFirst, boolean isLast) {
        String base    = isMine ? "chat-bubble-sent"  : "chat-bubble-recv";
        String variant = resolveBubbleVariant(isMine, isFirst, isLast);
        bubble.getStyleClass().add(base);
        bubble.getStyleClass().add(variant);
    }

    private String resolveBubbleVariant(boolean isMine, boolean isFirst, boolean isLast) {
        String prefix = isMine ? "chat-bubble-sent" : "chat-bubble-recv";
        if (isFirst && isLast) return prefix + "-solo";
        if (isFirst)           return prefix + "-first";
        if (isLast)            return prefix + "-last";
        return prefix + "-mid";
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
        LOGGER.log(Level.INFO, "Voice call requested for: {0}", selectedContactUsername);
    }

    @FXML
    private void handleVideo() {
        // Placeholder — video call not yet implemented
        LOGGER.log(Level.INFO, "Video call requested for: {0}", selectedContactUsername);
    }

    @FXML
    private void handleMore() {
        // Placeholder — context menu (mute, block, clear) not yet implemented
        LOGGER.log(Level.INFO, "More options requested for: {0}", selectedContactUsername);
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
        Timeline pollTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (selectedContactUsername == null) return;
            refreshMessagesIfNeeded();
            markMessagesRead();
            refreshAllContactItems();
        }));
        pollTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        pollTimeline.play();
    }

    private void refreshMessagesIfNeeded() {
        try {
            MessageDao msgDao = DaoFactory.getInstance().createMessageDao();
            List<Message> messages = msgDao.getConversation(
                    DaoFactory.getInstance().getConnection(),
                    currentUsername, selectedContactUsername);
            long rendered = messagesContainer.getChildren().stream()
                    .filter(HBox.class::isInstance).count();
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
                refreshTextBox(textBox, username);
            } else if (child instanceof Region dot && dot.getStyleClass().contains("chat-unread-dot")) {
                refreshUnreadDot(dot, username);
            }
        }
    }

    private void refreshTextBox(VBox textBox, String username) {
        if (textBox.getChildren().get(0) instanceof HBox nameRow) {
            refreshTimeLabel(nameRow, username);
        }
        if (textBox.getChildren().get(1) instanceof Label previewLabel) {
            previewLabel.setText(getLastMessagePreview(username));
        }
    }

    private void refreshTimeLabel(HBox nameRow, String username) {
        for (var nr : nameRow.getChildren()) {
            if (nr instanceof Label lbl && lbl.getStyleClass().contains("chat-contact-time")) {
                lbl.setText(getLastMessageTime(username));
            }
        }
    }

    private void refreshUnreadDot(Region dot, String username) {
        long unread = countUnread(username);
        dot.setVisible(unread > 0);
        dot.setManaged(unread > 0);
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
