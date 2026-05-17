package it.ispw.tutora.controller.cli;

import it.ispw.tutora.bean.BookingBean;
import it.ispw.tutora.controller.application.GetStudentLessonsController;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Lesson;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

import static it.ispw.tutora.controller.cli.CLIUtils.*;

/**
 * Schermata "Le mie lezioni" per lo studente.
 * Mostra l'elenco delle prenotazioni con stato e dettagli.
 */
@SuppressWarnings("java:S106") // System.out è intenzionale: classe boundary della CLI
public class MyLessonsCLI {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final GetStudentLessonsController ctrl = new GetStudentLessonsController();

    public void show(Scanner sc, String token) {
        printHeader("LE MIE LEZIONI");

        BookingBean bean = new BookingBean();
        List<Booking> bookings = ctrl.loadBookings(bean, token);

        if (bean.getErrorMessage() != null) {
            error(bean.getErrorMessage());
            pressEnter(sc);
            return;
        }

        if (bookings.isEmpty()) {
            info("Non hai ancora prenotato nessuna lezione.");
            pressEnter(sc);
            return;
        }

        info(bookings.size() + " prenotazione/i trovata/e");
        System.out.println();

        for (int i = 0; i < bookings.size(); i++) {
            printBooking(bookings.get(i), i + 1);
        }

        pressEnter(sc);
    }

    // ----------------------------------------------------------------
    // Stampa singola prenotazione
    // ----------------------------------------------------------------

    private void printBooking(Booking b, int n) {
        Lesson l = b.getLesson();
        String start = (l != null && l.getStartTime() != null) ? l.getStartTime().format(DT_FMT) : "—";
        String end   = (l != null && l.getEndTime()   != null) ? l.getEndTime().format(DT_FMT)   : "—";
        String price = b.getPricePaid() != null ? "€" + b.getPricePaid().toPlainString() : "—";

        System.out.printf("  %s── Prenotazione #%d %s%n", BOLD, n, RESET);
        field("Materia:",   extractSubcatName(l));
        field("Tutor:",     extractTutorName(l));
        field("Inizio:",    start);
        field("Fine:",      end);
        field("Modalità:",  extractMode(l));
        field("Prezzo:",    price);
        field("Pagamento:", paymentBadge(b.getPaymentStatus()));
        separator();
    }

    private String extractSubcatName(Lesson l) {
        if (l != null && l.getExpertise() != null && l.getExpertise().getSubcategory() != null) {
            return l.getExpertise().getSubcategory().getName();
        }
        return "—";
    }

    private String extractTutorName(Lesson l) {
        if (l != null && l.getExpertise() != null && l.getExpertise().getTutor() != null) {
            return l.getExpertise().getTutor().getUsername();
        }
        return "—";
    }

    /** Modalità di erogazione — metodo dedicato per evitare ternario annidato (SonarQube S3358). */
    private String extractMode(Lesson l) {
        if (l == null) return "—";
        return l.isRemote() ? "Remoto" : "In presenza";
    }

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------

    private String paymentBadge(PaymentStatus ps) {
        if (ps == null) return DIM + "—" + RESET;
        return switch (ps) {
            case PENDING  -> YELLOW + "In attesa"   + RESET;
            case PAID     -> GREEN  + "Pagato"       + RESET;
            case REFUNDED -> MAGENTA + "Rimborsato"  + RESET;
        };
    }
}
