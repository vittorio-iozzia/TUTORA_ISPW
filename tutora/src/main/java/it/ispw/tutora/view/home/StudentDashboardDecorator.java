package it.ispw.tutora.view.home;

import it.ispw.tutora.controller.graphic.StudentContentController;
import javafx.scene.layout.VBox;

/**
 * Decoratore concreto per gli utenti con ruolo {@code Student}.
 *
 * Carica il frammento FXML {@code student_content.fxml} nella
 * {@code contentArea} della home condivisa.
 */
public class StudentDashboardDecorator extends DashboardDecorator {

    private static StudentContentController lastController;

    public static void refreshUpcoming() {
        if (lastController != null) lastController.refreshUpcomingLessons();
    }

    private static void setLastController(StudentContentController ctrl) {
        lastController = ctrl;
    }

    public StudentDashboardDecorator(DashboardComponent wrapped) {
        super(wrapped);
    }

    @Override
    public void decorateContent(VBox contentArea) {
        super.decorateContent(contentArea);
        loadFxmlContent(contentArea, "/fxml/student_content.fxml",
                ctrl -> setLastController((StudentContentController) ctrl));
    }
}
