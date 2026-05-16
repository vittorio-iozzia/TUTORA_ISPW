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
public class NotificationCLI {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final GetNotificationsController    notifCtrl  = new GetNotificationsController();
    private final BookTutorController           bookCtrl   = new BookTutorController();
    private final ApplyToBecomeATutorController appCtrl    = new ApplyToBecomeATutorController();

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

            List<Notification> all = bean.getList() != null ? bean.getList() : List.of();
            if (all.isEmpty()) {
                info("Nessuna notifica.");
                pressEnter(sc);
                return;
            }

            // --- Stampa lista ---
            Session session = SessionManager.getInstance().getSession(token);
            long unread = all.stream().filter(n -> !n.isRead()).count();
            if (unread > 0) info(unread + " notifiche non lette");

            System.out.println();
            for (int i = 0; i < all.size(); i++) {
                Notification n = all.get(i);
                String stato = n.isRead() ? DIM + "[letta]" + RESET : GREEN + BOLD + "[NUOVA]" + RESET;
                System.out.printf("  %s[%d]%s %s  %s%s%s%n",
                        YELLOW + BOLD, i + 1, RESET,
                        stato,
                        BOLD, typeName(n.getType()), RESET);
                System.out.println("       " + DIM + n.getMessage() + RESET);
                System.out.println("       " + DIM + n.getTimestamp().format(FMT) + RESET);
                if (isActionable(n, session)) {
                    System.out.println("       " + YELLOW + "→ Azione disponibile" + RESET);
                }
                System.out.println();
            }

            separator();
            menuItem(1, "Apri notifica / esegui azione");
            menuItem(2, "Segna tutte come lette");
            menuItem(0, "Torna indietro");
            System.out.println();

            int scelta = readInt(sc, "Scelta", 0, 2);
            switch (scelta) {
                case 1 -> {
                    int idx = readInt(sc, "Numero notifica", 1, all.size()) - 1;
                    handleAction(sc, token, all.get(idx), session);
                }
                case 2 -> {
                    NotificationBean mb = new NotificationBean();
                    notifCtrl.markAllAsRead(mb, token);
                    success("Tutte le notifiche segnate come lette.");
                    pressEnter(sc);
                }
                case 0 -> { return; }
                default -> { /* loop */ }
            }
        }
    }

    // ----------------------------------------------------------------
    // Azione su singola notifica
    // ----------------------------------------------------------------

    private void handleAction(Scanner sc, String token,
                              Notification notif, Session session) {
        // Marca come letta
        NotificationBean mb = new NotificationBean();
        mb.setNotificationId(notif.getId());
        notifCtrl.markAsRead(mb, token);

        printHeader(typeName(notif.getType()));
        System.out.println();
        System.out.println("  " + notif.getMessage());
        System.out.println("  " + DIM + notif.getTimestamp().format(FMT) + RESET);
        System.out.println();

        if (!isActionable(notif, session)) {
            info("Nessuna azione disponibile per questa notifica.");
            pressEnter(sc);
            return;
        }

        // ---- Studente: paga lezione accettata ----
        if (session.isStudent() && notif.getType() == NotificationType.LESSON_ACCEPTED) {
            warn("Per confermare la prenotazione è richiesto il pagamento.");
            System.out.println();
            menuItem(1, "Paga ora (PayPal)");
            menuItem(0, "Annulla");
            int sc2 = readInt(sc, "Scelta", 0, 1);
            if (sc2 == 1) {
                BookingBean bean = new BookingBean();
                bean.setLessonId(notif.getTargetId());
                info("Elaborazione pagamento in corso...");
                bookCtrl.payment(bean, token);
                if (bean.getErrorMessage() == null) {
                    success("Pagamento completato! Ref: " + bean.getPaymentRef());
                } else {
                    error("Pagamento fallito: " + bean.getErrorMessage());
                }
            }
            pressEnter(sc);
            return;
        }

        // ---- Tutor: accetta/rifiuta prenotazione ----
        if (session.isTutor() && notif.getType() == NotificationType.LESSON_BOOKED) {
            menuItem(1, "Accetta richiesta");
            menuItem(2, "Rifiuta richiesta");
            menuItem(0, "Annulla");
            int sc2 = readInt(sc, "Scelta", 0, 2);
            if (sc2 == 1 || sc2 == 2) {
                BookingTutorBean bean = new BookingTutorBean();
                bean.setLessonId(notif.getTargetId());
                bean.setAccepted(sc2 == 1);
                bean.setStudentUsername(notif.getSenderUsername());
                bookCtrl.respondToRequest(bean, token);
                if (bean.getErrorMessage() == null) {
                    success(sc2 == 1 ? "Richiesta accettata!" : "Richiesta rifiutata.");
                } else {
                    error(bean.getErrorMessage());
                }
            }
            pressEnter(sc);
            return;
        }

        // ---- Admin: approva/rifiuta candidatura ----
        if (session.isAdmin() && notif.getType() == NotificationType.APPLICATION_UPDATE) {
            menuItem(1, "Approva candidatura");
            menuItem(2, "Rifiuta candidatura");
            menuItem(0, "Annulla");
            int sc2 = readInt(sc, "Scelta", 0, 2);
            if (sc2 == 1 || sc2 == 2) {
                ApplicationReviewBean bean = new ApplicationReviewBean();
                bean.setApplicationId(notif.getTargetId());
                bean.setStatus(sc2 == 1
                        ? ApplicationStatus.ACCEPTED : ApplicationStatus.REJECTED);
                String notes = readOptionalLine(sc, "Note admin");
                bean.setAdminNotes(notes.isEmpty() ? "" : notes);
                try {
                    appCtrl.evaluateApplication(bean, token);
                    success(sc2 == 1 ? "Candidatura approvata!" : "Candidatura rifiutata.");
                } catch (Exception e) {
                    error("Errore: " + e.getMessage());
                }
            }
            pressEnter(sc);
        }
    }

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------

    private boolean isActionable(Notification n, Session session) {
        if (session.isStudent() && n.getType() == NotificationType.LESSON_ACCEPTED
                && !n.isRead()) return true;
        if (session.isTutor()   && n.getType() == NotificationType.LESSON_BOOKED
                && !n.isRead()) return true;
        if (session.isAdmin()   && n.getType() == NotificationType.APPLICATION_UPDATE
                && !n.isRead()) return true;
        return false;
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
