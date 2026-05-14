package it.ispw.tutora;

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
        SceneManager.getInstance().init(primaryStage);
        SceneManager.getInstance().showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
