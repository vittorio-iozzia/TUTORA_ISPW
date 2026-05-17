package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.bean.BookingBean;
import it.ispw.tutora.bean.BookingTutorBean;
import it.ispw.tutora.bean.NotificationBean;
import it.ispw.tutora.controller.application.ApplyToBecomeATutorController;
import it.ispw.tutora.controller.application.BookTutorController;
import it.ispw.tutora.controller.application.GetNotificationsController;
import it.ispw.tutora.view.home.TutorDashboardDecorator;
import it.ispw.tutora.enums.NotificationType;
import it.ispw.tutora.model.Notification;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.SceneManager;
import it.ispw.tutora.bean.TutorApplicationBean;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationGfxController {

    /**
     * Set statico (sopravvive alla chiusura/riapertura del dialog) dei lessonId
     * per cui è in corso un pagamento in background.
     * Thread-safe: scritto dal thread JavaFX e letto da buildPendingCard().
     */
    private static final Set<Integer> IN_PROGRESS_PAYMENTS = ConcurrentHashMap.newKeySet();

    /**
     * Map statica lessonId → messaggio d'errore dell'ultimo pagamento fallito.
     * Persiste tra chiusura e riapertura del dialog in modo che lo student
     * veda sempre l'errore anche se ha chiuso il pannello prima del completamento.
     */
    private static final Map<Integer, String> PAYMENT_ERRORS = new ConcurrentHashMap<>();

    /**
     * Callback opzionale registrato da HomeGfxController.
     * Viene invocato (sul thread JavaFX) subito dopo che un pagamento è andato a buon fine,
     * per aggiornare la sezione "Upcoming Lessons" del dashboard senza ricaricare la pagina.
     */
    private static final String PAY_NOW_LABEL = "💳  Pay Now";
    private static Runnable paymentConfirmedCallback = null;
    private static Runnable applicationEvaluatedCallback = null;

    public static void setPaymentConfirmedCallback(Runnable callback) {
        paymentConfirmedCallback = callback;
    }

    public static void setApplicationEvaluatedCallback(Runnable callback) {
        applicationEvaluatedCallback = callback;
    }

    @FXML private VBox dialogRoot;
    @FXML private Label subtitleLabel;
    @FXML private VBox contentContainer;
    @FXML private Button markAllReadBtn;
    @FXML private Button closeBtn;
    @FXML private StackPane headerIconWrap;
    @FXML private ImageView headerIconView;

    private String  token;
    private Session session;

    /** Snapshot dell'ultima lista caricata — usata da handleMarkAllRead per
     *  sapere quali notifiche saltare (quelle che richiedono ancora un'azione). */
    private List<Notification> currentNotifications = List.of();

    private final GetNotificationsController notifController = new GetNotificationsController();
    private final BookTutorController bookTutorController = new BookTutorController();
    private final ApplyToBecomeATutorController appController = new ApplyToBecomeATutorController();

    // ----------------------------------------------------------------
    // FXML init — called automatically by FXMLLoader
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        token   = SceneManager.getInstance().getSessionToken();
        session = SessionManager.getInstance().getSession(token);
        setupIconBox(headerIconWrap, headerIconView, "1f514", 22); // 🔔 bell
        applyRoundedClip(dialogRoot);
        reload();
    }

    private void applyRoundedClip(VBox root) {
        if (root == null) return;
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        root.layoutBoundsProperty().addListener((obs, o, n) -> {
            if (n.getWidth() > 0 && root.getClip() == null) {
                clip.setWidth(n.getWidth());
                clip.setHeight(n.getHeight());
                root.widthProperty().addListener((o2, ov, nv)  -> clip.setWidth(nv.doubleValue()));
                root.heightProperty().addListener((o2, ov, nv) -> clip.setHeight(nv.doubleValue()));
                root.setClip(clip);
            }
        });
    }

    @FXML
    private void handleClose() {
        ((Stage) contentContainer.getScene().getWindow()).close();
    }

    // ----------------------------------------------------------------
    // FXML handlers
    // ----------------------------------------------------------------

    @FXML
    private void handleMarkAllRead() {
        markAllReadBtn.setDisable(true);

        // Raccoglie solo le notifiche che NON richiedono più azione:
        // LESSON_BOOKED (tutor deve ancora accept/reject) e
        // LESSON_ACCEPTED (student deve ancora pagare) vengono saltate.
        List<Notification> toMark = currentNotifications.stream()
                .filter(n -> !n.isRead() && !isActionable(n))
                .toList();

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                for (Notification n : toMark) {
                    NotificationBean nb = new NotificationBean();
                    nb.setNotificationId(n.getId());
                    notifController.markAsRead(nb, token);
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(this::reload));
        task.setOnFailed(e   -> Platform.runLater(() -> markAllReadBtn.setDisable(false)));
        Thread t = new Thread(task, "notif-mark-all");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Restituisce true se la notifica ha ancora un'azione pendente che
     * l'utente deve compiere (Accept/Reject o Pay Now).
     * Queste notifiche vengono escluse dal "Mark all as read".
     */
    private boolean isActionable(Notification n) {
        return (session.isTutor() && n.getType() == NotificationType.LESSON_BOOKED)
            || (session.isStudent() && n.getType() == NotificationType.LESSON_ACCEPTED)
            || (session.isAdmin() && n.getType() == NotificationType.APPLICATION_UPDATE);
    }

    // ----------------------------------------------------------------
    // Core load + render
    // ----------------------------------------------------------------

    private void reload() {
        contentContainer.getChildren().clear();
        Label loading = new Label("Loading…");
        loading.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:13px;-fx-font-style:italic;");
        contentContainer.getChildren().add(loading);

        Task<NotificationBean> task = new Task<>() {
            @Override
            protected NotificationBean call() {
                NotificationBean bean = new NotificationBean();
                notifController.loadNotifications(bean, token);

                // Auto-mark LESSON_REJECTED as read for students (no action needed)
                if (session.isStudent() && bean.getList() != null) {
                    for (Notification n : bean.getList()) {
                        if (n.getType() == NotificationType.LESSON_REJECTED && !n.isRead()) {
                            NotificationBean mb = new NotificationBean();
                            mb.setNotificationId(n.getId());
                            notifController.markAsRead(mb, token);
                            n.setRead(true);
                        }
                    }
                }
                return bean;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> render(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> {
            contentContainer.getChildren().clear();
            Label err = new Label("Could not load notifications.");
            err.setStyle("-fx-text-fill:#9C2121;-fx-font-size:13px;");
            contentContainer.getChildren().add(err);
        }));

        Thread t = new Thread(task, "notif-load");
        t.setDaemon(true);
        t.start();
    }

    private void render(NotificationBean bean) {
        contentContainer.getChildren().clear();

        List<Notification> all = bean.getList() != null ? bean.getList() : List.of();
        currentNotifications = all; // snapshot per handleMarkAllRead
        List<Notification> pending = all.stream().filter(n -> !n.isRead()).toList();
        List<Notification> old = all.stream().filter(Notification::isRead).toList();

        int unread = pending.size();
        if (unread == 0) {
            subtitleLabel.setText("All caught up!");
        } else {
            subtitleLabel.setText(unread + " unread notification" + (unread == 1 ? "" : "s"));
        }
        markAllReadBtn.setDisable(unread == 0 || !IN_PROGRESS_PAYMENTS.isEmpty());

        if (all.isEmpty()) {
            contentContainer.getChildren().add(buildEmptyState());
            return;
        }

        if (!pending.isEmpty()) {
            contentContainer.getChildren().add(buildSectionHeader("Pending", pending.size(), true));
            VBox list = new VBox(10);
            VBox.setMargin(list, new Insets(0, 0, 4, 0));
            for (Notification n : pending) list.getChildren().add(buildPendingCard(n));
            contentContainer.getChildren().add(list);
        }

        if (!pending.isEmpty() && !old.isEmpty()) {
            Region gap = new Region();
            gap.setPrefHeight(16);
            contentContainer.getChildren().add(gap);
        }

        if (!old.isEmpty()) {
            contentContainer.getChildren().add(buildSectionHeader("Archive", old.size(), false));
            VBox list = new VBox(10);
            for (Notification n : old) list.getChildren().add(buildOldCard(n));
            contentContainer.getChildren().add(list);
        }
    }

    // ----------------------------------------------------------------
    // Section header
    // ----------------------------------------------------------------

    private HBox buildSectionHeader(String title, int count, boolean isPending) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(row, new Insets(0, 0, 8, 0));

        Label lbl = new Label(title);
        lbl.getStyleClass().add("notif-section-title");

        Label badge = new Label(String.valueOf(count));
        badge.getStyleClass().add(isPending ? "notif-count-badge-pending" : "notif-count-badge-old");

        row.getChildren().addAll(lbl, badge);
        return row;
    }

    // ----------------------------------------------------------------
    // Notification cards
    // ----------------------------------------------------------------

    private VBox buildPendingCard(Notification notif) {
        VBox card = buildBaseCard(notif, false);

        if (session.isTutor() && notif.getType() == NotificationType.LESSON_BOOKED) {
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_LEFT);
            VBox.setMargin(actions, new Insets(8, 0, 0, 0));

            Button acceptBtn = new Button("✓  Accept");
            acceptBtn.getStyleClass().add("accept-btn");
            acceptBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(acceptBtn, Priority.ALWAYS);

            Button rejectBtn = new Button("✗  Reject");
            rejectBtn.getStyleClass().add("decline-btn");
            rejectBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(rejectBtn, Priority.ALWAYS);

            acceptBtn.setOnAction(e -> {
                acceptBtn.setDisable(true);
                rejectBtn.setDisable(true);
                doRespond(notif, true, card, acceptBtn, rejectBtn);
            });
            rejectBtn.setOnAction(e -> {
                acceptBtn.setDisable(true);
                rejectBtn.setDisable(true);
                doRespond(notif, false, card, acceptBtn, rejectBtn);
            });

            actions.getChildren().addAll(acceptBtn, rejectBtn);
            card.getChildren().add(actions);

        } else if (session.isStudent() && notif.getType() == NotificationType.LESSON_ACCEPTED) {
            // Mostra eventuale errore persistente del pagamento precedente
            String prevErr = PAYMENT_ERRORS.get(notif.getTargetId());
            if (prevErr != null && !IN_PROGRESS_PAYMENTS.contains(notif.getTargetId())) {
                showCardError(card, prevErr);
            }

            Button payBtn = new Button(PAY_NOW_LABEL);
            payBtn.getStyleClass().add("notif-pay-btn");
            payBtn.setMaxWidth(Double.MAX_VALUE);
            VBox.setMargin(payBtn, new Insets(8, 0, 0, 0));
            if (IN_PROGRESS_PAYMENTS.contains(notif.getTargetId())) {
                // Pagamento già avviato in background (es. dialog riaperto durante processing)
                payBtn.setText("Processing…");
                payBtn.setDisable(true);
            } else {
                payBtn.setOnAction(e -> {
                    // Pulisce l'errore precedente e avvia il nuovo tentativo
                    PAYMENT_ERRORS.remove(notif.getTargetId());
                    payBtn.setDisable(true);
                    payBtn.setText("Processing…");
                    doPay(notif, card, payBtn);
                });
            }
            card.getChildren().add(payBtn);

        } else if (notif.getType() == NotificationType.PAYMENT_CONFIRMED) {
            // Banner verde di conferma — visibile sia per student che per tutor
            Label confirmed = new Label("✅  Payment confirmed");
            confirmed.getStyleClass().add("notif-success-label");
            confirmed.setMaxWidth(Double.MAX_VALUE);
            VBox.setMargin(confirmed, new Insets(10, 0, 0, 0));
            card.getChildren().add(confirmed);

        } else if (session.isAdmin() && notif.getType() == NotificationType.APPLICATION_UPDATE) {
            Button reviewBtn = new Button("📋  Review Application");
            reviewBtn.getStyleClass().add("notif-pay-btn");
            reviewBtn.setMaxWidth(Double.MAX_VALUE);
            VBox.setMargin(reviewBtn, new Insets(8, 0, 0, 0));
            reviewBtn.setOnAction(e -> openReviewDialog(notif, card));
            card.getChildren().add(reviewBtn);
        }

        return card;
    }

    private VBox buildOldCard(Notification notif) {
        return buildBaseCard(notif, true);
    }

    private VBox buildBaseCard(Notification notif, boolean isArchived) {
        VBox card = new VBox(0);
        card.getStyleClass().add("notif-card");
        if (!notif.isRead()) {
            // PAYMENT_CONFIRMED unread → tinta verde; tutti gli altri → tinta blu standard
            card.getStyleClass().add(
                    notif.getType() == NotificationType.PAYMENT_CONFIRMED
                            ? "notif-card-payment"
                            : "notif-card-unread");
        }

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Icon
        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().addAll("notif-icon-wrap", iconColorClass(notif.getType()));
        ImageView icon = loadTwemoji(emojiCodepoint(notif.getType()), 18);
        iconWrap.getChildren().add(icon);

        // Info
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label titleLbl = new Label(notifTitle(notif.getType(), isArchived));
        titleLbl.getStyleClass().add("notif-title");

        Label msgLbl = new Label(notif.getMessage());
        msgLbl.getStyleClass().add("notif-message");
        msgLbl.setWrapText(true);

        info.getChildren().addAll(titleLbl, msgLbl);

        // Show admin notes for students reviewing their application result
        if (session.isStudent() && notif.getType() == NotificationType.APPLICATION_UPDATE) {
            String fullMsg = notif.getMessage();
            final String SEPARATOR = "\n\nAdmin notes: ";
            int notesIdx = fullMsg.indexOf(SEPARATOR);
            if (notesIdx >= 0) {
                msgLbl.setText(fullMsg.substring(0, notesIdx));
                String notes = fullMsg.substring(notesIdx + SEPARATOR.length());

                VBox notesBox = new VBox(4);
                notesBox.getStyleClass().add("notif-notes-box");
                VBox.setMargin(notesBox, new Insets(8, 0, 0, 0));
                Label notesTitleLbl = new Label("Admin notes");
                notesTitleLbl.getStyleClass().add("notif-notes-title");
                Label notesLbl = new Label(notes);
                notesLbl.getStyleClass().add("notif-notes-content");
                notesLbl.setWrapText(true);
                notesBox.getChildren().addAll(notesTitleLbl, notesLbl);
                info.getChildren().add(notesBox);
            }
        }

        // Right column: time + unread dot
        VBox rightCol = new VBox(4);
        rightCol.setAlignment(Pos.TOP_RIGHT);

        Label timeLbl = new Label(relativeTime(notif.getTimestamp()));
        timeLbl.getStyleClass().add("notif-time");
        rightCol.getChildren().add(timeLbl);

        if (!notif.isRead()) {
            Region dot = new Region();
            dot.getStyleClass().add("notif-unread-dot");
            rightCol.getChildren().add(dot);
        }

        topRow.getChildren().addAll(iconWrap, info, rightCol);
        card.getChildren().add(topRow);
        return card;
    }

    // ----------------------------------------------------------------
    // Async actions
    // ----------------------------------------------------------------

    private void doRespond(Notification notif, boolean accepted,
                           VBox card, Button acceptBtn, Button rejectBtn) {
        BookingTutorBean bean = new BookingTutorBean();
        bean.setLessonId(notif.getTargetId());
        bean.setAccepted(accepted);
        bean.setStudentUsername(notif.getSenderUsername());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                bookTutorController.respondToRequest(bean, token);
                if (bean.getErrorMessage() == null) {
                    NotificationBean nb = new NotificationBean();
                    nb.setNotificationId(notif.getId());
                    notifController.markAsRead(nb, token);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (bean.getErrorMessage() != null) {
                showCardError(card, bean.getErrorMessage());
                acceptBtn.setDisable(false);
                rejectBtn.setDisable(false);
            } else {
                TutorDashboardDecorator.refreshBookingRequests();
                reload();
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            showCardError(card, "An unexpected error occurred.");
            acceptBtn.setDisable(false);
            rejectBtn.setDisable(false);
        }));

        Thread t = new Thread(task, accepted ? "notif-accept" : "notif-reject");
        t.setDaemon(true);
        t.start();
    }

    private void doPay(Notification notif, VBox card, Button payBtn) {
        // Registra il pagamento come "in corso" prima di avviare il thread.
        // Il set è statico: sopravvive alla chiusura del dialog e consente
        // a buildPendingCard() di mostrare "Processing…" se il dialog viene riaperto.
        IN_PROGRESS_PAYMENTS.add(notif.getTargetId());
        markAllReadBtn.setDisable(true);

        BookingBean bean = new BookingBean();
        bean.setLessonId(notif.getTargetId());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                bookTutorController.payment(bean, token);
                if (bean.getErrorMessage() == null) {
                    // Marca LESSON_ACCEPTED come letta SUBITO dopo il pagamento:
                    // al successivo reload() verrà spostata in Archive mentre la
                    // notifica PAYMENT_CONFIRMED comparirà in Pending per lo student.
                    NotificationBean nb = new NotificationBean();
                    nb.setNotificationId(notif.getId());
                    notifController.markAsRead(nb, token);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            IN_PROGRESS_PAYMENTS.remove(notif.getTargetId());
            if (bean.getErrorMessage() != null) {
                // Persiste l'errore nella map statica: sopravvive alla chiusura del dialog
                PAYMENT_ERRORS.put(notif.getTargetId(), bean.getErrorMessage());
                showCardError(card, bean.getErrorMessage());
                payBtn.setDisable(false);
                payBtn.setText(PAY_NOW_LABEL);
                // Ripristina markAllReadBtn se non ci sono altri pagamenti in corso
                long stillUnread = currentNotifications.stream().filter(n -> !n.isRead()).count();
                markAllReadBtn.setDisable(!IN_PROGRESS_PAYMENTS.isEmpty() || stillUnread == 0);
            } else {
                PAYMENT_ERRORS.remove(notif.getTargetId());
                if (paymentConfirmedCallback != null) paymentConfirmedCallback.run();
                reload();
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            IN_PROGRESS_PAYMENTS.remove(notif.getTargetId());
            String errMsg = "An unexpected error occurred. Please try again.";
            PAYMENT_ERRORS.put(notif.getTargetId(), errMsg);
            showCardError(card, errMsg);
            payBtn.setDisable(false);
            payBtn.setText(PAY_NOW_LABEL);
            long stillUnread = currentNotifications.stream().filter(n -> !n.isRead()).count();
            markAllReadBtn.setDisable(!IN_PROGRESS_PAYMENTS.isEmpty() || stillUnread == 0);
        }));

        Thread t = new Thread(task, "notif-pay");
        t.setDaemon(true);
        t.start();
    }

    private void openReviewDialog(Notification notif, VBox card) {
        Task<TutorApplicationBean> task = new Task<>() {
            @Override
            protected TutorApplicationBean call() throws Exception {
                return appController.loadApplicationDetail(notif.getTargetId(), token);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/application_review.fxml"));
                Parent root = loader.load();
                ApplicationReviewGfxController ctrl = loader.getController();
                ctrl.initApplication(task.getValue());
                ctrl.setOnEvaluated(() -> {
                    // Marca come letta solo dopo che l'admin ha approvato/rifiutato
                    markAsReadAsync(notif.getId());
                    if (applicationEvaluatedCallback != null) applicationEvaluatedCallback.run();
                    HomeGfxController.refreshBadgeStatic();
                    reload();
                });

                Parent parentRoot = card.getScene().getRoot();
                GaussianBlur blur = new GaussianBlur(10);
                ColorAdjust dim  = new ColorAdjust();
                dim.setBrightness(-0.35);
                dim.setInput(blur);
                parentRoot.setEffect(dim);

                Stage stage = new Stage();
                stage.initOwner(card.getScene().getWindow());
                stage.initModality(Modality.WINDOW_MODAL);
                stage.initStyle(StageStyle.TRANSPARENT);
                Scene scene = new Scene(root);
                scene.setFill(Color.TRANSPARENT);
                stage.setScene(scene);
                stage.setMinWidth(560);
                stage.setOnHiding(ev -> parentRoot.setEffect(null));
                stage.show();
            } catch (Exception ex) {
                showCardError(card, "Cannot open review: " + ex.getMessage());
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() ->
                showCardError(card, "Cannot load application: " +
                        (task.getException() != null ? task.getException().getMessage() : "error"))));

        Thread t = new Thread(task, "load-app-detail");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Marca la notifica come letta in background (thread JavaFX non bloccato).
     * Usato dopo che l'admin ha valutato una candidatura tramite il dialog di review.
     * Fire-and-forget: callback e reload sono già gestiti da openReviewDialog.
     */
    private void markAsReadAsync(int notifId) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                NotificationBean nb = new NotificationBean();
                nb.setNotificationId(notifId);
                notifController.markAsRead(nb, token);
                return null;
            }
        };
        Thread t = new Thread(task, "notif-mark-read");
        t.setDaemon(true);
        t.start();
    }

    // ----------------------------------------------------------------
    // UI helpers
    // ----------------------------------------------------------------

    private void showCardError(VBox card, String msg) {
        // Remove any previous error label
        card.getChildren().removeIf(n -> n instanceof Label lbl
                && lbl.getStyleClass().contains("notif-error-label"));
        Label err = new Label(msg);
        err.getStyleClass().add("notif-error-label");
        err.setWrapText(true);
        err.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(err, new Insets(8, 0, 0, 0));
        card.getChildren().add(err);
    }

    private VBox buildEmptyState() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(48, 0, 48, 0));

        Label icon = new Label("🔔");
        icon.setStyle("-fx-font-size: 36px;");

        Label title = new Label("No notifications yet");
        title.setStyle("-fx-font-size:16px;-fx-font-weight:700;-fx-text-fill:#374151;-fx-font-family:'Segoe UI';");

        Label sub = new Label("You're all caught up!");
        sub.setStyle("-fx-font-size:13px;-fx-text-fill:#9CA3AF;-fx-font-family:'Segoe UI';");

        box.getChildren().addAll(icon, title, sub);
        return box;
    }

    // ----------------------------------------------------------------
    // Type helpers
    // ----------------------------------------------------------------

    private String emojiCodepoint(NotificationType type) {
        return switch (type) {
            case LESSON_BOOKED -> "1f4c5";
            case LESSON_ACCEPTED -> "2705";
            case LESSON_REJECTED -> "274c";
            case APPLICATION_UPDATE -> "1f4cb";
            case EXPERTISE_OFFER -> "2b50";
            case PAYMENT_CONFIRMED -> "1f4b3";
            case NEW_REVIEW -> "1f31f";
        };
    }

    private String iconColorClass(NotificationType type) {
        return switch (type) {
            case LESSON_BOOKED -> "notif-icon-blue";
            case LESSON_ACCEPTED -> "notif-icon-green";
            case LESSON_REJECTED -> "notif-icon-red";
            case APPLICATION_UPDATE -> "notif-icon-amber";
            case EXPERTISE_OFFER -> "notif-icon-purple";
            case PAYMENT_CONFIRMED -> "notif-icon-green";
            case NEW_REVIEW -> "notif-icon-amber";
        };
    }

    private String notifTitle(NotificationType type, boolean isArchived) {
        if (isArchived && type == NotificationType.LESSON_BOOKED) return "Old Booking Request";
        if (isArchived && type == NotificationType.APPLICATION_UPDATE && session.isAdmin()) return "Reviewed Application";
        return switch (type) {
            case LESSON_BOOKED -> "New Booking Request";
            case LESSON_ACCEPTED -> "Lesson Accepted";
            case LESSON_REJECTED -> "Lesson Declined";
            case APPLICATION_UPDATE -> session.isAdmin() ? "New Tutor Application" : "Application Update";
            case EXPERTISE_OFFER -> "New Expertise Offer";
            case PAYMENT_CONFIRMED -> "Payment Confirmed";
            case NEW_REVIEW -> "New Review";
        };
    }

    /**
     * Marca come lette tutte le notifiche visibili tranne le richieste di
     * prenotazione pendenti del tutor (LESSON_BOOKED non ancora gestite).
     * Chiamato da HomeGfxController quando il dialog viene chiuso.
     */
    public void markVisibleAsRead(Runnable onComplete) {
        List<Notification> toMark = currentNotifications.stream()
                .filter(n -> !n.isRead())

                // Non marcare LESSON_BOOKED del tutor (richiede ancora accept/reject)
                .filter(n -> !(session.isTutor() && n.getType() == NotificationType.LESSON_BOOKED))

                // Non marcare LESSON_ACCEPTED dello student (deve ancora pagare,
                // indipendentemente dallo stato del pagamento in background)
                .filter(n -> !(session.isStudent() && n.getType() == NotificationType.LESSON_ACCEPTED))

                // Non marcare APPLICATION_UPDATE dell'admin (richiede valutazione esplicita)
                .filter(n -> !(session.isAdmin() && n.getType() == NotificationType.APPLICATION_UPDATE))
                .toList();
        if (toMark.isEmpty()) {
            if (onComplete != null) Platform.runLater(onComplete);
            return;
        }
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                for (Notification n : toMark) {
                    NotificationBean nb = new NotificationBean();
                    nb.setNotificationId(n.getId());
                    notifController.markAsRead(nb, token);
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> { if (onComplete != null) Platform.runLater(onComplete); });
        Thread t = new Thread(task, "notif-auto-read");
        t.setDaemon(true);
        t.start();
    }

    private String relativeTime(LocalDateTime ts) {
        long mins = ChronoUnit.MINUTES.between(ts, LocalDateTime.now());
        if (mins < 1)  return "Just now";
        if (mins < 60) return mins + " min ago";
        long hours = mins / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        if (days < 7)   return days + "d ago";
        return ts.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH));
    }

    // ----------------------------------------------------------------
    // Icon helpers
    // ----------------------------------------------------------------

    private ImageView loadTwemoji(String codepoint, double size) {
        ImageView iv = new ImageView();
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setSmooth(true);
        iv.setPreserveRatio(true);
        String url = "https://cdnjs.cloudflare.com/ajax/libs/twemoji/14.0.2/72x72/" + codepoint + ".png";
        Image img = new Image(url, size * 2, size * 2, true, true, true);
        img.progressProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() >= 1.0 && !img.isError()) iv.setImage(img);
        });
        return iv;
    }

    private void setupIconBox(StackPane wrap, ImageView iv, String codepoint, double size) {
        if (wrap == null) return;
        wrap.setEffect(new DropShadow(10, 0, 4, Color.web("#00000026")));
        if (iv != null) {
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            String url = "https://cdnjs.cloudflare.com/ajax/libs/twemoji/14.0.2/72x72/" + codepoint + ".png";
            Image img = new Image(url, size * 2, size * 2, true, true, true);
            img.progressProperty().addListener((obs, o, n) -> {
                if (n.doubleValue() >= 1.0 && !img.isError()) iv.setImage(img);
            });
        }
    }
}
