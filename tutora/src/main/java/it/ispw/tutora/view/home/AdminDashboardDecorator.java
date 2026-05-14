package it.ispw.tutora.view.home;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Decoratore concreto per gli utenti con ruolo {@code Admin}.
 *
 * Carica il frammento FXML {@code admin_content.fxml} nella
 * {@code contentArea} della home condivisa.
 */
public class AdminDashboardDecorator extends DashboardDecorator {

    private static final Logger LOGGER =
            Logger.getLogger(AdminDashboardDecorator.class.getName());

    public AdminDashboardDecorator(DashboardComponent wrapped) {
        super(wrapped);
    }

    @Override
    public void decorateContent(VBox contentArea) {
        super.decorateContent(contentArea);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin_content.fxml"));
            Node content = loader.load();
            VBox.setVgrow(content, Priority.ALWAYS);
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            LOGGER.severe("Cannot load admin_content.fxml: " + e.getMessage());
        }
    }
}
