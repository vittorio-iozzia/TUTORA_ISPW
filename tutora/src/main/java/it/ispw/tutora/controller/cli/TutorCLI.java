package it.ispw.tutora.controller.cli;

import it.ispw.tutora.controller.application.GetNotificationsController;
import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Lesson;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

import static it.ispw.tutora.controller.cli.CLIUtils.*;

/**
 * Menu principale per il ruolo TUTOR.
 *
 * Funzionalita' disponibili:
 *  - Dashboard (riepilogo lezioni e rating)
 *  - Le mie lezioni (calendario e prenotazioni)
 *  - Notifiche (accept/reject richieste)
 *  - Profilo
 *  - Logout
 */
@SuppressWarnings("java:S106") // System.out e' intenzionale: classe boundary della CLI
public class TutorCLI {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificationCLI            notifCLI  = new NotificationCLI();
    private final GetNotificationsController notifCtrl = new GetNotificationsController();

    /** Avvia il menu tutor. Ritorna quando l'utente fa logout. */
    public void run(Scanner sc, String token) {
        while (true) {
            Session session = SessionManager.getInstance().getSession(token);
            Tutor tutor = (Tutor) session.getUser();

            int unread = notifCtrl.getUnreadCount(token);
            String notifLabel = "Notifiche"
                    + (unread > 0 ? " " + RED + BOLD + "[" + unread + " nuove]" + RESET : "");

            printHeader("DASHBOARD TUTOR");
            System.out.println();
            System.out.println("  Ciao, " + BOLD + tutor.getName() + " " + tutor.getSurname() + RESET + "!");
            System.out.println("  " + DIM + "@" + tutor.getUsername() + "  |  " + tutor.getEmail() + RESET);
            if (tutor.getRating() != null && tutor.getRating().doubleValue() > 0) {
                System.out.printf("  Rating: %s%.1f *  (%d recensioni)%s%n",
                        BOLD + YELLOW, tutor.getRating().doubleValue(),
                        tutor.getRatingCount(), RESET);
            } else {
                System.out.println("  Rating: " + DIM + "Nessuna recensione ancora" + RESET);
            }
            System.out.println();
            separator();
            System.out.println();

            menuItem(1, "Le mie lezioni e prenotazioni");
            menuItem(2, notifLabel);
            menuItem(3, "Il mio profilo");
            menuItem(0, "Logout");
            System.out.println();

            int scelta = readInt(sc, "Scelta", 0, 3);
            switch (scelta) {
                case 1 -> showLessons(sc, tutor);
                case 2 -> notifCLI.show(sc, token);
                case 3 -> showProfile(sc, tutor);
                case 0 -> {
                    info("Logout effettuato. Arrivederci, " + tutor.getName() + "!");
                    return;
                }
                default -> error("Scelta non valida.");
            }
        }
    }

    // ----------------------------------------------------------------
    // Lezioni del tutor (storico prenotazioni ricevute)
    // ----------------------------------------------------------------

    private void showLessons(Scanner sc, Tutor tutor) {
        printHeader("LE MIE LEZIONI");

        List<Booking> bookings = loadTutorBookings(tutor.getUsername());

        if (bookings.isEmpty()) {
            info("Nessuna prenotazione ricevuta finora.");
            pressEnter(sc);
            return;
        }

        info(bookings.size() + " prenotazione/i trovata/e");
        System.out.println();

        long paidCount    = bookings.stream().filter(b -> b.getPaymentStatus() == PaymentStatus.PAID).count();
        long pendingCount = bookings.stream().filter(b -> b.getPaymentStatus() == PaymentStatus.PENDING).count();
        field("Lezioni pagate:",      String.valueOf(paidCount));
        field("In attesa pagamento:", String.valueOf(pendingCount));
        System.out.println();
        separator();

        for (Booking b : bookings) {
            printTutorBooking(b);
        }

        pressEnter(sc);
    }

    private void printTutorBooking(Booking b) {
        Lesson l   = b.getLesson();
        String start = (l != null && l.getStartTime() != null) ? l.getStartTime().format(DT_FMT) : "-";
        String end   = (l != null && l.getEndTime()   != null) ? l.getEndTime().format(DT_FMT)   : "-";
        String price = b.getPricePaid() != null ? "EUR" + b.getPricePaid().toPlainString() : "-";

        System.out.println();
        field("Studente:",  extractStudentLabel(b));
        field("Materia:",   extractSubcatName(l));
        field("Orario:",    start + " - " + end);
        field("Modalita':",  lessonModeLabel(l));
        field("Lezione:",   lessonStatusLabel(l));
        field("Pagamento:", paymentStatusLabel(b.getPaymentStatus()));
        field("Prezzo:",    price);
        separator();
    }

    private String extractSubcatName(Lesson l) {
        if (l != null && l.getExpertise() != null && l.getExpertise().getSubcategory() != null) {
            return l.getExpertise().getSubcategory().getName();
        }
        return "-";
    }

    private String extractStudentLabel(Booking b) {
        if (b.getStudent() == null) return "-";
        return b.getStudent().getName() + " " + b.getStudent().getSurname()
                + " (@" + b.getStudent().getUsername() + ")";
    }

    /** Modalita' di erogazione - metodo dedicato per evitare ternario annidato (SonarQube S3358). */
    private String lessonModeLabel(Lesson l) {
        if (l == null) return "In presenza";
        return l.isRemote() ? CYAN + "Remoto" + RESET : "In presenza";
    }

    // ----------------------------------------------------------------
    // Profilo tutor (sola lettura)
    // ----------------------------------------------------------------

    private void showProfile(Scanner sc, Tutor tutor) {
        printHeader("IL MIO PROFILO");
        System.out.println();
        field("Nome:",        tutor.getName());
        field("Cognome:",     tutor.getSurname());
        field("Username:",    "@" + tutor.getUsername());
        field("Email:",       tutor.getEmail());
        field("Ruolo:",       MAGENTA + "Tutor" + RESET);
        field("Stato:",       tutor.isActive()
                ? GREEN + "Attivo" + RESET : RED + "Inattivo" + RESET);
        field("Membro dal:",  tutor.getCreatedAt() != null
                ? tutor.getCreatedAt().toLocalDate().toString() : "-");
        if (tutor.getRating() != null && tutor.getRating().doubleValue() > 0) {
            field("Rating:",  String.format("%.1f *  (%d recensioni)",
                    tutor.getRating().doubleValue(), tutor.getRatingCount()));
        }

        if (tutor.getDescription() != null && !tutor.getDescription().isBlank()) {
            System.out.println();
            System.out.println("  " + BOLD + "Descrizione:" + RESET);
            System.out.println("  " + DIM + tutor.getDescription() + RESET);
        }

        pressEnter(sc);
    }

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------

    private List<Booking> loadTutorBookings(String tutorUsername) {
        try {
            BookingDao dao = DaoFactory.getInstance().createBookingDao();
            try (Connection conn = DaoFactory.getInstance().getConnection()) {
                return dao.findByTutor(conn, tutorUsername);
            }
        } catch (DatabaseException | SQLException e) {
            return List.of();
        }
    }

    private String lessonStatusLabel(Lesson l) {
        if (l == null) return DIM + "-" + RESET;
        LessonStatus s = l.getLessonStatus();
        if (s == null) return DIM + "-" + RESET;
        return switch (s) {
            case AVAILABLE  -> GREEN  + "Disponibile" + RESET;
            case BOOKED     -> YELLOW + "Prenotata"   + RESET;
            case COMPLETED  -> CYAN   + "Completata"  + RESET;
            case CANCELLED  -> RED    + "Cancellata"  + RESET;
        };
    }

    private String paymentStatusLabel(PaymentStatus ps) {
        if (ps == null) return DIM + "-" + RESET;
        return switch (ps) {
            case PENDING  -> YELLOW  + "In attesa"  + RESET;
            case PAID     -> GREEN   + "Pagato"      + RESET;
            case REFUNDED -> MAGENTA + "Rimborsato"  + RESET;
        };
    }
}
