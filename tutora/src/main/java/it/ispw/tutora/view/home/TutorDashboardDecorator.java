package it.ispw.tutora.view.home;

import it.ispw.tutora.controller.graphic.TutorContentController;
import javafx.scene.layout.VBox;

/**
 * Decoratore concreto per gli utenti con ruolo {@code Tutor}.
 *
 * Carica il frammento FXML {@code tutor_content.fxml} nella
 * {@code contentArea} della home condivisa.
 */
public class TutorDashboardDecorator extends DashboardDecorator {

    private static TutorContentController lastController;

    public static void refreshUpcoming() {
        if (lastController != null) lastController.refreshUpcomingLessons();
    }

    private static void setLastController(TutorContentController ctrl) {
        lastController = ctrl;
    }

    public TutorDashboardDecorator(DashboardComponent wrapped) {
        super(wrapped);
    }

    @Override
    public void decorateContent(VBox contentArea) {
        super.decorateContent(contentArea);
        loadFxmlContent(contentArea, "/fxml/tutor_content.fxml",
                ctrl -> setLastController((TutorContentController) ctrl));
    }
}
