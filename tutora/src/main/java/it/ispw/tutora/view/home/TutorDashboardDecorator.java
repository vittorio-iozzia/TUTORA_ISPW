package it.ispw.tutora.view.home;

import it.ispw.tutora.controller.graphic.TutorContentController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Decoratore concreto per gli utenti con ruolo {@code Tutor}.
 *
 * Carica il frammento FXML {@code tutor_content.fxml} nella
 * {@code contentArea} della home condivisa.
 */
public class TutorDashboardDecorator extends DashboardDecorator {

    private static final Logger LOGGER =
            Logger.getLogger(TutorDashboardDecorator.class.getName());

    /** Ultimo controller caricato — aggiornato ogni volta che viene caricato tutor_content.fxml. */
    private static TutorContentController lastController;

    /**
     * Ricarica la sezione "Upcoming Lessons" dell'ultimo dashboard tutor caricato.
     * Thread-safe: viene chiamato dal thread JavaFX tramite il callback di pagamento.
     */
    public static void refreshUpcoming() {
        if (lastController != null) lastController.refreshUpcomingLessons();
    }

    public TutorDashboardDecorator(DashboardComponent wrapped) {
        super(wrapped);
    }

    @Override
    public void decorateContent(VBox contentArea) {
        super.decorateContent(contentArea);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tutor_content.fxml"));
            Node content = loader.load();
            lastController = loader.getController();
            VBox.setVgrow(content, Priority.ALWAYS);
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            LOGGER.severe("Cannot load tutor_content.fxml: " + e.getMessage());
        }
    }
}
