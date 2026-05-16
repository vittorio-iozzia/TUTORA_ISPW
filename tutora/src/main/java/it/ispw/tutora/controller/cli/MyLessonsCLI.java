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
            Booking b = bookings.get(i);
            Lesson  l = b.getLesson();

            String payBadge = paymentBadge(b.getPaymentStatus());
            String subcatName = (l != null && l.getExpertise() != null
                    && l.getExpertise().getSubcategory() != null)
                    ? l.getExpertise().getSubcategory().getName() : "—";
            String tutorName  = (l != null && l.getExpertise() != null
                    && l.getExpertise().getTutor() != null)
                    ? l.getExpertise().getTutor().getUsername() : "—";
            String start = (l != null && l.getStartTime() != null)
                    ? l.getStartTime().format(DT_FMT) : "—";
            String end   = (l != null && l.getEndTime() != null)
                    ? l.getEndTime().format(DT_FMT) : "—";
            String mode  = (l != null) ? (l.isRemote() ? "Remoto" : "In presenza") : "—";
            String price = b.getPricePaid() != null
                    ? "€" + b.getPricePaid().toPlainString() : "—";

            System.out.printf("  %s── Prenotazione #%d %s%n", BOLD, i + 1, RESET);
            field("Materia:",   subcatName);
            field("Tutor:",     tutorName);
            field("Inizio:",    start);
            field("Fine:",      end);
            field("Modalità:",  mode);
            field("Prezzo:",    price);
            field("Pagamento:", payBadge);
            separator();
        }

        pressEnter(sc);
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
