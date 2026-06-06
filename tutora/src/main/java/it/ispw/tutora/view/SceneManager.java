package it.ispw.tutora.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Logger;

/**
 * Gestore centralizzato della navigazione tra scene JavaFX.
 *
 * -----------------------------------------------------------------------
 * Pattern Singleton – Bill Pugh (Initialization-on-demand Holder)
 * -----------------------------------------------------------------------
 * Mantiene il riferimento allo Stage primario e al token di sessione
 * corrente, rendendoli accessibili ai GfxController senza accoppiamento
 * diretto tra schermate.
 */
public class SceneManager {

    private static final Logger LOGGER = Logger.getLogger(SceneManager.class.getName());
    private static final String FXML_BASE = "/fxml/";
    private static final String HOME_FXML = "home.fxml";

    private Stage primaryStage;
    private String sessionToken;

    // ----------------------------------------------------------------
    // Bill Pugh Holder
    // ----------------------------------------------------------------

    private SceneManager() {}

    private static class Holder {
        private static final SceneManager INSTANCE = new SceneManager();
    }

    public static SceneManager getInstance() {
        return Holder.INSTANCE;
    }

    // ----------------------------------------------------------------
    // Inizializzazione
    // ----------------------------------------------------------------

    public void init(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("TUTORA");
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(620);
    }

    // ----------------------------------------------------------------
    // Navigazione
    // ----------------------------------------------------------------

    public void showLogin() {
        sessionToken = null;
        loadScene("login.fxml", 1100, 700);
    }

    public void showRegister() {
        loadScene("register.fxml", 1100, 760);
    }

    public void showStudentHome() {
        loadScene(HOME_FXML, 1100, 700);
    }

    public void showTutorHome() {
        loadScene(HOME_FXML, 1100, 700);
    }

    public void showAdminHome() {
        loadScene(HOME_FXML, 1100, 700);
    }

    public void showSearchTutor() {
        loadScene("search_tutor.fxml", 1100, 700);
    }

    public void showStudentLessons() {
        loadScene("student_lessons.fxml", 1100, 700);
    }

    public void showTutorLessons() {
        loadScene("tutor_lessons.fxml", 1100, 700);
    }

    public void showTutorExpertise() {
        loadScene("tutor_expertise.fxml", 1100, 700);
    }

    public void showNotifications() {
        loadScene("notifications.fxml", 1100, 700);
    }

    // ----------------------------------------------------------------
    // Token di sessione
    // ----------------------------------------------------------------

    public String getSessionToken() { return sessionToken; }

    public void setSessionToken(String token) { this.sessionToken = token; }

    public Stage getStage() { return primaryStage; }

    // ----------------------------------------------------------------
    // Utility interna
    // ----------------------------------------------------------------

    private void loadScene(String fxmlFile, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(FXML_BASE + fxmlFile));
            Parent root = loader.load();
            Scene current = primaryStage.getScene();
            if (current != null && primaryStage.isShowing()) {
                // Swap only the root — the Stage (size, position, maximized state) is untouched.
                current.setRoot(root);
            } else {
                primaryStage.setScene(new Scene(root, width, height));
            }
            primaryStage.show();
        } catch (Exception e) {
            LOGGER.severe("Cannot load scene: " + fxmlFile + " — " + e.getMessage());
        }
    }
}
