package it.ispw.tutora.view.home;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Decoratore astratto della dashboard.
 *
 * Mantiene il riferimento al componente avvolto e delega la chiamata
 * a {@link #decorateContent} prima che le sottoclassi aggiungano i
 * propri controlli, garantendo la catena di decorazione.
 */
public abstract class DashboardDecorator implements DashboardComponent {

    private static final Logger LOGGER = Logger.getLogger(DashboardDecorator.class.getName());

    /** Componente avvolto da questo decoratore. */
    protected final DashboardComponent wrapped;

    protected DashboardDecorator(DashboardComponent wrapped) {
        this.wrapped = wrapped;
    }

    /**
     * Delega al componente avvolto, poi le sottoclassi aggiungono
     * i propri elementi sovrascrivendo questo metodo con {@code super}.
     */
    @Override
    public void decorateContent(VBox contentArea) {
        wrapped.decorateContent(contentArea);
    }

    /**
     * Carica un frammento FXML e lo inietta nella {@code contentArea}.
     * Passa il controller al {@code controllerConsumer} prima di aggiungerlo.
     */
    protected void loadFxmlContent(VBox contentArea, String fxmlPath,
                                   Consumer<Object> controllerConsumer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            controllerConsumer.accept(loader.getController());
            VBox.setVgrow(content, Priority.ALWAYS);
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            LOGGER.severe("Cannot load " + fxmlPath + ": " + e.getMessage());
        }
    }
}
