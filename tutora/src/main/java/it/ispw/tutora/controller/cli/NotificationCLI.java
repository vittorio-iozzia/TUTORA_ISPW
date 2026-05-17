package it.ispw.tutora.controller.cli;

import it.ispw.tutora.bean.ApplicationReviewBean;
import it.ispw.tutora.bean.BookingBean;
import it.ispw.tutora.bean.BookingTutorBean;
import it.ispw.tutora.bean.NotificationBean;
import it.ispw.tutora.controller.application.ApplyToBecomeATutorController;
import it.ispw.tutora.controller.application.BookTutorController;
import it.ispw.tutora.controller.application.GetNotificationsController;
import it.ispw.tutora.enums.ApplicationStatus;
import it.ispw.tutora.enums.NotificationType;
import it.ispw.tutora.model.Notification;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

import static it.ispw.tutora.controller.cli.CLIUtils.*;

/**
 * Pannello notifiche CLI.
 * Supporta le azioni specifiche per ruolo:
 *  - Studente : Paga lezione accettata
 *  - Tutor    : Accetta / Rifiuta richiesta di prenotazione
 *  - Admin    : Approva / Rifiuta candidatura tutor
 */
@SuppressWarnings("java:S106") // System.out è intenzionale: classe boundary della CLI
public class NotificationCLI {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String INDENT        = "       ";
    private static final String PROMPT_SCELTA = "Scelta";
    private static final String LABEL_ANNULLA = "Annulla";

    private final GetNotificationsController    notifCtrl = new GetNotificationsController();
    private final BookTutorController           bookCtrl  = new BookTutorController();
    private final ApplyToBecomeATutorController appCtrl   = new ApplyToBecomeATutorController();

    public void show(Scanner sc, String token) {
        while (true) {
            printHeader("NOTIFICHE");

            NotificationBean bean = new NotificationBean();
            notifCtrl.loadNotifications(bean, token);

            if (bean.getErrorMessage() != null) {
                error(bean.getErrorMessage());
                pressEnter(sc);
                return;
            }

            List<Notification> all = resolvedList(bean);
            if (all.isEmpty()) {
                info("Nessuna notifica.");
                pressEnter(sc);
                return;
            }

            Session session = SessionManager.getInstance().getSession(token);
            printNotificationList(all, session);

            int scelta = readInt(sc, PROMPT_SCELTA, 0, 2);
            if (scelta == 0) return;
            if (scelta == 1) {
                int idx = readInt(sc, "Numero notifica", 1, all.size()) - 1;
                handleAction(sc, token, all.get(idx), session);
            } else {
                notifCtrl.markAllAsRead(token);
                success("Tutte le notifiche segnate come lette.");
                pressEnter(sc);
            }
        }
    }

    private List<Notification> resolvedList(NotificationBean bean) {
        return bean.getList() != null ? bean.getList() : List.of();
    }

    private void printNotificationList(List<Notification> all, Session session) {
        long unread = all.stream().filter(n -> !n.isRead()).count();
        if (unread > 0) info(unread + " notifiche non lette");
        System.out.println();
        for (int i = 0; i < all.size(); i++) {
            printNotificationItem(all.get(i), i + 1, session);
        }
        separator();
        menuItem(1, "Apri notifica / esegui azione");
        menuItem(2, "Segna tutte come lette");
        menuItem(0, "Torna indietro");
        System.out.println();
    }

    private void printNotificationItem(Notification n, int index, Session session) {
        String stato = n.isRead() ? DIM + "[letta]" + RESET : GREEN + BOLD + "[NUOVA]" + RESET;
        System.out.printf("  %s[%d]%s %s  %s%s%s%n",
                YELLOW + BOLD, index, RESET,
                stato,
                BOLD, typeName(n.getType()), RESET);
        System.out.println(INDENT + DIM + n.getMessage() + RESET);
        System.out.println(INDENT + DIM + n.getTimestamp().format(FMT) + RESET);
        if (isActionable(n, session)) {
            System.out.println(INDENT + YELLOW + "→ Azione disponibile" + RESET);
        }
        System.out.println();
    }

    // ----------------------------------------------------------------
    // Dispatch azione su singola notifica
    // ----------------------------------------------------------------

    private void handleAction(Scanner sc, String token,
                              Notification notif, Session session) {
        printHeader(typeName(notif.getType()));
        System.out.println();
        System.out.println("  " + notif.getMessage());
        System.out.println("  " + DIM + notif.getTimestamp().format(FMT) + RESET);
        System.out.println();

        if (!isActionable(notif, session)) {
            markRead(token, notif);
            info("Nessuna azione disponibile per questa notifica.");
            pressEnter(sc);
            return;
        }

        if (session.isStudent() && notif.getType() == NotificationType.LESSON_ACCEPTED) {
            handleStudentAction(sc, token, notif);
            return;
        }
        if (session.isTutor() && notif.getType() == NotificationType.LESSON_BOOKED) {
            handleTutorAction(sc, token, notif);
            return;
        }
        if (session.isAdmin() && notif.getType() == NotificationType.APPLICATION_UPDATE) {
            handleAdminAction(sc, token, notif);
        }
    }

    // ---- Studente: paga lezione accettata ----
    private void handleStudentAction(Scanner sc, String token, Notification notif) {
        warn("Per confermare la prenotazione è richiesto il pagamento.");
        System.out.println();
        menuItem(1, "Paga ora (PayPal)");
        menuItem(0, LABEL_ANNULLA);
        int scelta = readInt(sc, PROMPT_SCELTA, 0, 1);
        if (scelta == 1) {
            BookingBean bean = new BookingBean();
            bean.setLessonId(notif.getTargetId());
            info("Elaborazione pagamento in corso...");
            bookCtrl.payment(bean, token);
            if (bean.getErrorMessage() == null) {
                markRead(token, notif);
                success("Pagamento completato! Ref: " + bean.getPaymentRef());
            } else {
                error("Pagamento fallito: " + bean.getErrorMessage());
                info("Puoi riprovare dalla lista notifiche.");
            }
        }
        pressEnter(sc);
    }

    // ---- Tutor: accetta/rifiuta prenotazione ----
    private void handleTutorAction(Scanner sc, String token, Notification notif) {
        menuItem(1, "Accetta richiesta");
        menuItem(2, "Rifiuta richiesta");
        menuItem(0, LABEL_ANNULLA + " (decidi dopo)");
        int scelta = readInt(sc, PROMPT_SCELTA, 0, 2);
        if (scelta == 1 || scelta == 2) {
            BookingTutorBean bean = new BookingTutorBean();
            bean.setLessonId(notif.getTargetId());
            bean.setAccepted(scelta == 1);
            bean.setStudentUsername(notif.getSenderUsername());
            bookCtrl.respondToRequest(bean, token);
            if (bean.getErrorMessage() == null) {
                markRead(token, notif);
                success(scelta == 1 ? "Richiesta accettata!" : "Richiesta rifiutata.");
            } else {
                error(bean.getErrorMessage());
                info("Puoi riprovare dalla lista notifiche.");
            }
        }
        // Se scelta == 0 (Annulla) non si marca come letta: l'azione resta disponibile
        pressEnter(sc);
    }

    // ---- Admin: approva/rifiuta candidatura ----
    private void handleAdminAction(Scanner sc, String token, Notification notif) {
        menuItem(1, "Approva candidatura");
        menuItem(2, "Rifiuta candidatura");
        menuItem(0, LABEL_ANNULLA);
        int scelta = readInt(sc, PROMPT_SCELTA, 0, 2);
        if (scelta == 1 || scelta == 2) {
            ApplicationReviewBean bean = new ApplicationReviewBean();
            bean.setApplicationId(notif.getTargetId());
            bean.setStatus(scelta == 1 ? ApplicationStatus.ACCEPTED : ApplicationStatus.REJECTED);
            String notes = readOptionalLine(sc, "Note admin");
            bean.setAdminNotes(notes.isEmpty() ? "" : notes);
            try {
                appCtrl.evaluateApplication(bean, token);
                markRead(token, notif);
                success(scelta == 1 ? "Candidatura approvata!" : "Candidatura rifiutata.");
            } catch (Exception e) {
                error("Errore: " + e.getMessage());
            }
        }
        pressEnter(sc);
    }

    /** Segna la notifica come letta tramite il controller. */
    private void markRead(String token, Notification notif) {
        NotificationBean mb = new NotificationBean();
        mb.setNotificationId(notif.getId());
        notifCtrl.markAsRead(mb, token);
    }

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------

    private boolean isActionable(Notification n, Session session) {
        return (session.isStudent() && n.getType() == NotificationType.LESSON_ACCEPTED   && !n.isRead())
            || (session.isTutor()   && n.getType() == NotificationType.LESSON_BOOKED     && !n.isRead())
            || (session.isAdmin()   && n.getType() == NotificationType.APPLICATION_UPDATE && !n.isRead());
    }

    private String typeName(NotificationType t) {
        return switch (t) {
            case LESSON_BOOKED      -> "Nuova richiesta prenotazione";
            case LESSON_ACCEPTED    -> "Lezione accettata — pagamento richiesto";
            case LESSON_REJECTED    -> "Lezione rifiutata";
            case APPLICATION_UPDATE -> "Candidatura tutor";
            case EXPERTISE_OFFER    -> "Nuova expertise";
            case PAYMENT_CONFIRMED  -> "Pagamento confermato";
            case NEW_REVIEW         -> "Nuova recensione";
        };
    }
}
