package it.ispw.tutora.controller.cli;

import it.ispw.tutora.bean.BookingBean;
import it.ispw.tutora.bean.LessonStudentBean;
import it.ispw.tutora.controller.application.BookTutorController;
import it.ispw.tutora.controller.application.SearchTutorController;
import it.ispw.tutora.dao.TutorDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Category;
import it.ispw.tutora.model.Lesson;
import it.ispw.tutora.model.Tutor;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static it.ispw.tutora.controller.cli.CLIUtils.*;

/**
 * Schermata "Cerca tutor" per lo studente.
 * Permette di:
 *  1. Filtrare i tutor per categoria
 *  2. Visualizzare il profilo di un tutor
 *  3. Vedere le lezioni disponibili
 *  4. Richiedere una prenotazione
 */
@SuppressWarnings("java:S106") // System.out è intenzionale: classe boundary della CLI
public class BookTutorCLI {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SearchTutorController searchCtrl = new SearchTutorController();
    private final BookTutorController   bookCtrl   = new BookTutorController();

    public void show(Scanner sc, String token) {
        while (true) {
            printHeader("CERCA TUTOR");
            List<Tutor> allTutors = loadAllTutors();
            if (allTutors.isEmpty()) {
                info("Nessun tutor disponibile al momento.");
                pressEnter(sc);
                return;
            }
            List<Category> categories = loadCategoriesQuietly();
            printFilterMenu(categories);
            int filtro = readInt(sc, "Categoria", 0, categories.size() + 1);
            if (filtro == 0) return;
            List<Tutor> filtered = filterTutors(allTutors, categories, filtro);
            if (filtered.isEmpty()) {
                info("Nessun tutor trovato per la categoria selezionata.");
                pressEnter(sc);
                continue;
            }
            printTutorList(filtered);
            int scelta = readInt(sc, "Scegli tutor (0 per tornare)", 0, filtered.size());
            if (scelta == 0) return;
            showTutorDetail(sc, token, filtered.get(scelta - 1));
        }
    }

    private List<Category> loadCategoriesQuietly() {
        try {
            return searchCtrl.loadCategories();
        } catch (DatabaseException e) {
            warn("Impossibile caricare le categorie: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void printFilterMenu(List<Category> categories) {
        System.out.println("  " + BOLD + "Filtra per categoria:" + RESET);
        menuItem(0, "Torna al menu principale");
        menuItem(1, "Tutti i tutor");
        for (int i = 0; i < categories.size(); i++) {
            menuItem(i + 2, categories.get(i).getName());
        }
        System.out.println();
    }

    private List<Tutor> filterTutors(List<Tutor> all, List<Category> categories, int filtro) {
        if (filtro == 1) return all;
        String catName = categories.get(filtro - 2).getName();
        return searchCtrl.filterByCategory(all, catName);
    }

    private void printTutorList(List<Tutor> filtered) {
        separator();
        System.out.println("  " + BOLD + filtered.size() + " tutor trovati:" + RESET);
        System.out.println();
        for (int i = 0; i < filtered.size(); i++) {
            Tutor t = filtered.get(i);
            String rating = buildRatingLabel(t);
            System.out.printf("  %s[%d]%s %-20s  %s%n",
                    YELLOW + BOLD, i + 1, RESET,
                    BOLD + t.getName() + " " + t.getSurname() + RESET,
                    DIM + "@" + t.getUsername() + "  " + rating + RESET);
        }
        System.out.println();
        menuItem(0, "Torna al menu principale");
        System.out.println();
    }

    private String buildRatingLabel(Tutor t) {
        if (t.getRating() != null && t.getRating().doubleValue() > 0) {
            return String.format("%.1f ★ (%d rec.)", t.getRating().doubleValue(), t.getRatingCount());
        }
        return "Nessuna recensione";
    }

    // ----------------------------------------------------------------
    // Dettaglio tutor + prenotazione
    // ----------------------------------------------------------------

    private void showTutorDetail(Scanner sc, String token, Tutor tutor) {
        printHeader("PROFILO TUTOR");
        System.out.println();
        field("Nome:",        tutor.getName() + " " + tutor.getSurname());
        field("Username:",    "@" + tutor.getUsername());
        field("Email:",       tutor.getEmail());
        field("Stato:",       tutor.isActive() ? GREEN + "Attivo" + RESET : RED + "Inattivo" + RESET);
        if (tutor.getRating() != null && tutor.getRating().doubleValue() > 0) {
            field("Rating:",  String.format("%.1f ★  (%d recensioni)",
                    tutor.getRating().doubleValue(), tutor.getRatingCount()));
        } else {
            field("Rating:",  "Nessuna recensione ancora");
        }
        if (tutor.getDescription() != null && !tutor.getDescription().isBlank()) {
            System.out.println();
            System.out.println("  " + BOLD + "Descrizione:" + RESET);
            System.out.println("  " + DIM + tutor.getDescription() + RESET);
        }
        System.out.println();

        // Lezioni disponibili
        LessonStudentBean lb = new LessonStudentBean();
        lb.setTutorUsername(tutor.getUsername());
        bookCtrl.searchAvailableLessons(lb, token);

        List<Lesson> lezioni = lb.getList();
        if (lb.getErrorMessage() != null || lezioni == null || lezioni.isEmpty()) {
            info("Nessuna lezione disponibile per questo tutor al momento.");
            pressEnter(sc);
            return;
        }

        System.out.println("  " + BOLD + "Lezioni disponibili:" + RESET);
        System.out.println();
        for (int i = 0; i < lezioni.size(); i++) {
            Lesson l = lezioni.get(i);
            String subcatName = (l.getExpertise() != null && l.getExpertise().getSubcategory() != null)
                    ? l.getExpertise().getSubcategory().getName() : "—";
            System.out.printf("  %s[%d]%s %-20s  %s – %s  %s  €%s%n",
                    YELLOW + BOLD, i + 1, RESET,
                    BOLD + subcatName + RESET,
                    l.getStartTime().format(DT_FMT),
                    l.getEndTime().format(DT_FMT),
                    l.isRemote() ? CYAN + "Remoto" + RESET : "Presenza",
                    l.getListedPrice().toPlainString());
        }

        System.out.println();
        menuItem(0, "Torna indietro");
        System.out.println();

        int scelta = readInt(sc, "Prenota lezione (0 per tornare)", 0, lezioni.size());
        if (scelta == 0) return;

        requestBooking(sc, token, lezioni.get(scelta - 1));
    }

    // ----------------------------------------------------------------
    // Richiesta prenotazione
    // ----------------------------------------------------------------

    private void requestBooking(Scanner sc, String token, Lesson lezione) {
        printHeader("PRENOTA LEZIONE");
        System.out.println();
        String subcatName = (lezione.getExpertise() != null && lezione.getExpertise().getSubcategory() != null)
                ? lezione.getExpertise().getSubcategory().getName() : "—";
        field("Materia:",   subcatName);
        field("Inizio:",    lezione.getStartTime().format(DT_FMT));
        field("Fine:",      lezione.getEndTime().format(DT_FMT));
        field("Modalità:",  lezione.isRemote() ? "Remoto" : "In presenza");
        field("Prezzo:",    "€" + lezione.getListedPrice().toPlainString());
        System.out.println();
        warn("Il pagamento sarà richiesto solo dopo che il tutor accetta la richiesta.");
        System.out.println();

        if (!confirm(sc, "Inviare la richiesta di prenotazione?")) {
            info("Prenotazione annullata.");
            pressEnter(sc);
            return;
        }

        BookingBean bean = new BookingBean();
        bean.setLessonId(lezione.getId());
        bookCtrl.requestBooking(bean, token);

        if (bean.getErrorMessage() == null) {
            success("Richiesta inviata! Il tutor riceverà una notifica.");
        } else {
            error("Impossibile prenotare: " + bean.getErrorMessage());
        }
        pressEnter(sc);
    }

    // ----------------------------------------------------------------
    // Carica tutti i tutor dal DAO
    // ----------------------------------------------------------------

    private List<Tutor> loadAllTutors() {
        try {
            TutorDao dao = DaoFactory.getInstance().createTutorDao();
            try (Connection conn = DaoFactory.getInstance().getConnection()) {
                return dao.selectAllTutors(conn);
            }
        } catch (DatabaseException | SQLException e) {
            return List.of();
        }
    }
}
