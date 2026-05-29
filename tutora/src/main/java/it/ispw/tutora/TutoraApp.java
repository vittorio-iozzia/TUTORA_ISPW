package it.ispw.tutora;

import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.view.AvatarManager;
import it.ispw.tutora.view.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Entry point dell'applicazione JavaFX.
 *
 * Inizializza lo SceneManager con lo Stage primario e
 * mostra la schermata di login all'avvio.
 */
public class TutoraApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // In demo mode i dati sono sempre freschi (in-memory): disabilita la
        // persistenza su disco degli avatar per evitare che immagini da sessioni
        // precedenti ricompaiano al prossimo avvio.
        if (DaoFactory.getInstance().isDemo()) {
            AvatarManager.setDiskPersistenceEnabled(false);
        }

        SceneManager.getInstance().init(primaryStage);
        SceneManager.getInstance().showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
