package it.ispw.tutora.view.home;

import it.ispw.tutora.controller.graphic.StudentContentController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Decoratore concreto per gli utenti con ruolo {@code Student}.
 *
 * Carica il frammento FXML {@code student_content.fxml} nella
 * {@code contentArea} della home condivisa. Il frammento contiene
 * la dashboard completa dello studente: statistiche, griglia tutor
 * con ricerca e filtri, lezioni in programma e storico.
 */
public class StudentDashboardDecorator extends DashboardDecorator {

    private static final Logger LOGGER =
            Logger.getLogger(StudentDashboardDecorator.class.getName());

    /** Ultimo controller caricato — aggiornato ogni volta che viene caricato student_content.fxml. */
    private static StudentContentController lastController;

    /**
     * Ricarica la sezione "Upcoming Lessons" dell'ultimo dashboard student caricato.
     * Thread-safe: viene chiamato dal thread JavaFX tramite il callback di pagamento.
     */
    public static void refreshUpcoming() {
        if (lastController != null) lastController.refreshUpcomingLessons();
    }

    /**
     * @param wrapped il componente da avvolgere
     */
    public StudentDashboardDecorator(DashboardComponent wrapped) {
        super(wrapped);
    }

    /**
     * Svuota la {@code contentArea} tramite il componente base, poi
     * vi inietta il frammento FXML della dashboard studente.
     *
     * @param contentArea il {@link VBox} centrale della home
     */
    @Override
    public void decorateContent(VBox contentArea) {
        super.decorateContent(contentArea);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/student_content.fxml"));
            Node content = loader.load();
            lastController = loader.getController();
            VBox.setVgrow(content, Priority.ALWAYS);
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            LOGGER.severe("Cannot load student_content.fxml: " + e.getMessage());
        }
    }
}
