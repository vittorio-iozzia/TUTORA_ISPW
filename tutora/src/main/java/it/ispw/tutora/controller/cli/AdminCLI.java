package it.ispw.tutora.controller.cli;

import it.ispw.tutora.bean.TutorApplicationBean;
import it.ispw.tutora.controller.application.ApplyToBecomeATutorController;
import it.ispw.tutora.controller.application.GetNotificationsController;
import it.ispw.tutora.exception.AuthenticationException;
import it.ispw.tutora.exception.AuthorizationException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Admin;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

import static it.ispw.tutora.controller.cli.CLIUtils.*;

/**
 * Menu principale per il ruolo ADMIN.
 *
 * Funzionalità disponibili:
 *  - Candidature tutor in attesa (elenco + approva/rifiuta)
 *  - Notifiche
 *  - Logout
 */
public class AdminCLI {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificationCLI            notifCLI   = new NotificationCLI();
    private final GetNotificationsController notifCtrl  = new GetNotificationsController();
    private final ApplyToBecomeATutorController appCtrl = new ApplyToBecomeATutorController();

    /** Avvia il menu admin. Ritorna quando l'utente fa logout. */
    public void run(Scanner sc, String token) {
        while (true) {
            Session session = SessionManager.getInstance().getSession(token);
            Admin admin = (Admin) session.getUser();

            int unread = notifCtrl.getUnreadCount(token);
            String notifLabel = "Notifiche"
                    + (unread > 0 ? " " + RED + BOLD + "[" + unread + " nuove]" + RESET : "");

            printHeader("DASHBOARD ADMIN");
            System.out.println();
            System.out.println("  Ciao, " + BOLD + admin.getName() + " " + admin.getSurname() + RESET + "!");
            System.out.println("  " + DIM + "@" + admin.getUsername() + "  |  Admin" + RESET);
            System.out.println();
            separator();
            System.out.println();

            menuItem(1, "Candidature tutor in attesa");
            menuItem(2, notifLabel);
            menuItem(3, "Il mio profilo");
            menuItem(0, "Logout");
            System.out.println();

            int scelta = readInt(sc, "Scelta", 0, 3);
            switch (scelta) {
                case 1 -> showPendingApplications(sc, token);
                case 2 -> notifCLI.show(sc, token);
                case 3 -> showProfile(sc, admin);
                case 0 -> {
                    info("Logout effettuato. Arrivederci, " + admin.getName() + "!");
                    return;
                }
                default -> error("Scelta non valida.");
            }
        }
    }

    // ----------------------------------------------------------------
    // Candidature in attesa
    // ----------------------------------------------------------------

    private void showPendingApplications(Scanner sc, String token) {
        printHeader("CANDIDATURE IN ATTESA");

        List<TutorApplicationBean> applications;
        try {
            applications = appCtrl.loadPendingApplications(token);
        } catch (AuthenticationException | AuthorizationException e) {
            error("Accesso negato: " + e.getMessage());
            pressEnter(sc);
            return;
        } catch (DatabaseException e) {
            error("Errore di sistema: " + e.getMessage());
            pressEnter(sc);
            return;
        }

        if (applications.isEmpty()) {
            info("Nessuna candidatura in attesa di revisione.");
            pressEnter(sc);
            return;
        }

        info(applications.size() + " candidatura/e da esaminare");
        System.out.println();

        for (int i = 0; i < applications.size(); i++) {
            TutorApplicationBean app = applications.get(i);
            System.out.printf("  %s[%d]%s  %-20s  Categoria: %s%s%s  Data: %s%n",
                    YELLOW + BOLD, i + 1, RESET,
                    BOLD + app.getStudentUsername() + RESET,
                    CYAN, app.getCategoryName(), RESET,
                    app.getCreationDate() != null ? app.getCreationDate().format(DT_FMT) : "—");
        }

        System.out.println();
        info("Le azioni di approvazione/rifiuto si eseguono dalla sezione Notifiche,");
        info("oppure seleziona una candidatura qui sotto per un riepilogo.");
        System.out.println();
        menuItem(0, "Torna indietro");
        System.out.println();

        int scelta = readInt(sc, "Seleziona candidatura (0 per tornare)", 0, applications.size());
        if (scelta == 0) return;

        TutorApplicationBean app = applications.get(scelta - 1);
        showApplicationDetail(sc, app);
    }

    private void showApplicationDetail(Scanner sc, TutorApplicationBean app) {
        printHeader("DETTAGLIO CANDIDATURA");
        System.out.println();
        field("Studente:",   app.getStudentUsername());
        field("Categoria:",  app.getCategoryName());
        field("Stato:",      YELLOW + app.getStatus().toString() + RESET);
        field("Inviata il:", app.getCreationDate() != null
                ? app.getCreationDate().format(DT_FMT) : "—");
        if (app.getAdminNotes() != null && !app.getAdminNotes().isBlank()) {
            System.out.println();
            System.out.println("  " + BOLD + "Note admin:" + RESET);
            System.out.println("  " + DIM + app.getAdminNotes() + RESET);
        }
        System.out.println();
        info("Per approvare o rifiutare usa la sezione Notifiche (più completa).");
        pressEnter(sc);
    }

    // ----------------------------------------------------------------
    // Profilo admin
    // ----------------------------------------------------------------

    private void showProfile(Scanner sc, Admin admin) {
        printHeader("IL MIO PROFILO");
        System.out.println();
        field("Nome:",       admin.getName());
        field("Cognome:",    admin.getSurname());
        field("Username:",   "@" + admin.getUsername());
        field("Email:",      admin.getEmail());
        field("Ruolo:",      RED + BOLD + "Amministratore" + RESET);
        field("Stato:",      admin.isActive()
                ? GREEN + "Attivo" + RESET : RED + "Inattivo" + RESET);
        pressEnter(sc);
    }
}
