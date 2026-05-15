package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.bean.BookingBean;
import it.ispw.tutora.bean.BookingTutorBean;
import it.ispw.tutora.bean.NotificationBean;
import it.ispw.tutora.controller.application.BookTutorController;
import it.ispw.tutora.controller.application.GetNotificationsController;
import it.ispw.tutora.enums.NotificationType;
import it.ispw.tutora.model.Notification;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.SceneManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

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
    private static Runnable paymentConfirmedCallback = null;

    public static void setPaymentConfirmedCallback(Runnable callback) {
        paymentConfirmedCallback = callback;
    }

    @FXML private Label  subtitleLabel;
    @FXML private VBox   contentContainer;
    @FXML private Button markAllReadBtn;

    private String  token;
    private Session session;

    /** Snapshot dell'ultima lista caricata — usata da handleMarkAllRead per
     *  sapere quali notifiche saltare (quelle che richiedono ancora un'azione). */
    private List<Notification> currentNotifications = List.of();

    private final GetNotificationsController notifController    = new GetNotificationsController();
    private final BookTutorController        bookTutorController = new BookTutorController();

    // ----------------------------------------------------------------
    // FXML init — called automatically by FXMLLoader
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        token   = SceneManager.getInstance().getSessionToken();
        session = SessionManager.getInstance().getSession(token);
        reload();
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
        // Tutor: LESSON_BOOKED è sempre actionable (deve ancora accettare/rifiutare)
        if (session.isTutor()   && n.getType() == NotificationType.LESSON_BOOKED) return true;
        // Student: LESSON_ACCEPTED è SEMPRE actionable finché non viene pagata
        // (indipendentemente dallo stato del pagamento in background)
        if (session.isStudent() && n.getType() == NotificationType.LESSON_ACCEPTED) return true;
        return false;
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

        List<Notification> all     = bean.getList() != null ? bean.getList() : List.of();
        currentNotifications = all; // snapshot per handleMarkAllRead
        List<Notification> pending = all.stream().filter(n -> !n.isRead()).toList();
        List<Notification> old     = all.stream().filter(Notification::isRead).toList();

        int unread = pending.size();
        if (unread == 0) {
            subtitleLabel.setText("All caught up!");
        } else {
            subtitleLabel.setText(unread + " unread notification" + (unread == 1 ? "" : "s"));
        }
        markAllReadBtn.setDisable(unread == 0);

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

            Button payBtn = new Button("💳  Pay Now");
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
        FontIcon icon = new FontIcon(iconLiteral(notif.getType()));
        icon.setIconSize(15);
        icon.setStyle("-fx-icon-color: " + iconColor(notif.getType()) + ";");
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
                payBtn.setText("💳  Pay Now");
            } else {
                // Pagamento riuscito: rimuove eventuale errore residuo e ricarica
                PAYMENT_ERRORS.remove(notif.getTargetId());
                // Aggiorna la sezione Upcoming Lessons nel dashboard (student e tutor)
                if (paymentConfirmedCallback != null) paymentConfirmedCallback.run();
                // Ricarica le notifiche: LESSON_ACCEPTED → Archive (già read),
                // PAYMENT_CONFIRMED → Pending (notifica di sistema per lo student).
                // Il tutor vedrà la propria PAYMENT_CONFIRMED alla prossima apertura.
                reload();
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            IN_PROGRESS_PAYMENTS.remove(notif.getTargetId());
            String errMsg = "An unexpected error occurred. Please try again.";
            PAYMENT_ERRORS.put(notif.getTargetId(), errMsg);
            showCardError(card, errMsg);
            payBtn.setDisable(false);
            payBtn.setText("💳  Pay Now");
        }));

        Thread t = new Thread(task, "notif-pay");
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

    private String iconLiteral(NotificationType type) {
        return switch (type) {
            case LESSON_BOOKED     -> "fas-calendar-plus";
            case LESSON_ACCEPTED   -> "fas-check-circle";
            case LESSON_REJECTED   -> "fas-times-circle";
            case APPLICATION_UPDATE -> "fas-file-alt";
            case EXPERTISE_OFFER   -> "fas-star";
            case PAYMENT_CONFIRMED -> "fas-credit-card";
            case NEW_REVIEW        -> "fas-star";
        };
    }

    private String iconColorClass(NotificationType type) {
        return switch (type) {
            case LESSON_BOOKED     -> "notif-icon-blue";
            case LESSON_ACCEPTED   -> "notif-icon-green";
            case LESSON_REJECTED   -> "notif-icon-red";
            case APPLICATION_UPDATE -> "notif-icon-amber";
            case EXPERTISE_OFFER   -> "notif-icon-purple";
            case PAYMENT_CONFIRMED -> "notif-icon-green";
            case NEW_REVIEW        -> "notif-icon-amber";
        };
    }

    private String iconColor(NotificationType type) {
        return switch (type) {
            case LESSON_BOOKED     -> "#3B82F6";
            case LESSON_ACCEPTED   -> "#27AE60";
            case LESSON_REJECTED   -> "#EF4444";
            case APPLICATION_UPDATE -> "#F59E0B";
            case EXPERTISE_OFFER   -> "#8B5CF6";
            case PAYMENT_CONFIRMED -> "#27AE60";
            case NEW_REVIEW        -> "#F59E0B";
        };
    }

    private String notifTitle(NotificationType type, boolean isArchived) {
        if (isArchived && type == NotificationType.LESSON_BOOKED) return "Old Booking Request";
        return switch (type) {
            case LESSON_BOOKED     -> "New Booking Request";
            case LESSON_ACCEPTED   -> "Lesson Accepted";
            case LESSON_REJECTED   -> "Lesson Declined";
            case APPLICATION_UPDATE -> "Application Update";
            case EXPERTISE_OFFER   -> "New Expertise Offer";
            case PAYMENT_CONFIRMED -> "Payment Confirmed";
            case NEW_REVIEW        -> "New Review";
        };
    }

    /**
     * Marca come lette tutte le notifiche visibili tranne le richieste di
     * prenotazione pendenti del tutor (LESSON_BOOKED non ancora gestite).
     * Chiamato da HomeGfxController quando il dialog viene chiuso.
     */
    public void markVisibleAsRead() {
        List<Notification> toMark = currentNotifications.stream()
                .filter(n -> !n.isRead())
                // Non marcare LESSON_BOOKED del tutor (richiede ancora accept/reject)
                .filter(n -> !(session.isTutor() && n.getType() == NotificationType.LESSON_BOOKED))
                // Non marcare LESSON_ACCEPTED dello student (deve ancora pagare,
                // indipendentemente dallo stato del pagamento in background)
                .filter(n -> !(session.isStudent() && n.getType() == NotificationType.LESSON_ACCEPTED))
                .toList();
        if (toMark.isEmpty()) return;
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
}
