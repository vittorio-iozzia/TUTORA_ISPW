package it.ispw.tutora.controller.cli;

import it.ispw.tutora.controller.application.GetNotificationsController;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;

import java.util.Scanner;

import static it.ispw.tutora.controller.cli.CLIUtils.*;

/**
 * Menu principale per il ruolo STUDENTE.
 *
 * Funzionalita' disponibili:
 *  - Dashboard (riepilogo account)
 *  - Cerca tutor e prenota lezione
 *  - Le mie lezioni (storico prenotazioni)
 *  - Notifiche
 *  - Diventa tutor (candidatura)
 *  - Profilo
 *  - Logout
 */
@SuppressWarnings("java:S106") // System.out e' intenzionale: classe boundary della CLI
public class StudentCLI {

    private final BookTutorCLI              bookTutorCLI = new BookTutorCLI();
    private final MyLessonsCLI              myLessonsCLI = new MyLessonsCLI();
    private final NotificationCLI           notifCLI     = new NotificationCLI();
    private final ApplyToBecomeATutorCLI    applyTutorCLI = new ApplyToBecomeATutorCLI();
    private final GetNotificationsController notifCtrl   = new GetNotificationsController();

    /** Avvia il menu studente. Ritorna quando l'utente fa logout. */
    public void run(Scanner sc, String token) {
        while (true) {
            Session session = SessionManager.getInstance().getSession(token);
            Student student = (Student) session.getUser();

            // Badge notifiche non lette
            int unread = notifCtrl.getUnreadCount(token);
            String notifLabel = "Notifiche"
                    + (unread > 0 ? " " + RED + BOLD + "[" + unread + " nuove]" + RESET : "");

            printHeader("DASHBOARD STUDENTE");
            System.out.println();
            System.out.println("  Ciao, " + BOLD + student.getName() + " " + student.getSurname() + RESET + "!");
            System.out.println("  " + DIM + "@" + student.getUsername() + "  |  " + student.getEmail() + RESET);
            System.out.println("  Budget: " + BOLD + GREEN
                    + (student.getBudget() != null ? "EUR" + student.getBudget().toPlainString() : "EUR0,00")
                    + RESET);
            System.out.println();
            separator();
            System.out.println();

            menuItem(1, "Cerca tutor e prenota lezione");
            menuItem(2, "Le mie lezioni");
            menuItem(3, notifLabel);
            menuItem(4, "Diventa tutor");
            menuItem(5, "Il mio profilo");
            menuItem(0, "Logout");
            System.out.println();

            int scelta = readInt(sc, "Scelta", 0, 5);
            switch (scelta) {
                case 1 -> bookTutorCLI.show(sc, token);
                case 2 -> myLessonsCLI.show(sc, token);
                case 3 -> notifCLI.show(sc, token);
                case 4 -> applyTutorCLI.show(sc, token);
                case 5 -> showProfile(sc, student);
                case 0 -> {
                    info("Logout effettuato. Arrivederci, " + student.getName() + "!");
                    return;
                }
                default -> error("Scelta non valida.");
            }
        }
    }

    // ----------------------------------------------------------------
    // Profilo studente (sola lettura)
    // ----------------------------------------------------------------

    private void showProfile(Scanner sc, Student student) {
        printHeader("IL MIO PROFILO");
        System.out.println();
        field("Nome:",        student.getName());
        field("Cognome:",     student.getSurname());
        field("Username:",    "@" + student.getUsername());
        field("Email:",       student.getEmail());
        field("Ruolo:",       CYAN + "Studente" + RESET);
        field("Stato:",       student.isActive()
                ? GREEN + "Attivo" + RESET : RED + "Inattivo" + RESET);
        field("Budget:",      student.getBudget() != null
                ? "EUR" + student.getBudget().toPlainString() : "EUR0,00");
        field("Membro dal:", student.getCreatedAt() != null
                ? student.getCreatedAt().toLocalDate().toString() : "-");

        if (student.getDescription() != null && !student.getDescription().isBlank()) {
            System.out.println();
            System.out.println("  " + BOLD + "Descrizione:" + RESET);
            System.out.println("  " + DIM + student.getDescription() + RESET);
        }

        if (!student.getInterests().isEmpty()) {
            System.out.println();
            System.out.println("  " + BOLD + "Interessi:" + RESET);
            student.getInterests().forEach(cat ->
                    System.out.println("  " + CYAN + "- " + cat.getName() + RESET));
        }

        if (!student.getPreferredTutors().isEmpty()) {
            System.out.println();
            System.out.println("  " + BOLD + "Tutor preferiti:" + RESET);
            student.getPreferredTutors().forEach(t ->
                    System.out.println("  * " + t.getName() + " " + t.getSurname()
                            + DIM + " (@" + t.getUsername() + ")" + RESET));
        }

        pressEnter(sc);
    }
}
