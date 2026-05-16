package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.bean.LoginBean;
import it.ispw.tutora.controller.application.LoginController;
import it.ispw.tutora.controller.application.SocialLoginController;
import it.ispw.tutora.exception.AuthenticationException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.SceneManager;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Controller grafico per Login
 *
 * -----------------------------------------------------------------------
 * Layout split-screen 50/50
 * -----------------------------------------------------------------------
 *   Sinistra: hero panel con immagine Unsplash + overlay gradient verde
 *             scuro + logo TUTORA + 3 stat items con icone FontAwesome 5.
 *   Destra:   login card con role-selector (Student/Tutor/Admin),
 *             social login (Google + Meta), form username/password,
 *             toggle sign-up / sign-in.
 *
 * -----------------------------------------------------------------------
 * Animazioni
 * -----------------------------------------------------------------------
 *   - Ingresso card: FadeTransition + TranslateTransition parallele (420 ms)
 *     che replicano l'animation animate-fade-in del progetto web.
 *   - Toggle sign-up/sign-in: mini FadeTransition (180 ms).
 *   - Errore: FadeTransition in (200 ms).
 *
 * -----------------------------------------------------------------------
 * Immagine hero
 * -----------------------------------------------------------------------
 *   Caricata in background da Unsplash con JavaFX async Image loading.
 *   fitWidth/fitHeight legati alla dimensione dello StackPane tramite
 *   binding bidirezionale così l'immagine copre sempre l'intera metà
 *   sinistra indipendentemente dal ridimensionamento della finestra.
 *   Se non c'è connessione il gradient overlay assicura leggibilità.
 */
public class LoginGfxController {

    private static final Logger LOGGER = Logger.getLogger(LoginGfxController.class.getName());

    private static final String HERO_IMAGE_URL =
            "https://images.unsplash.com/photo-1766117651759-640cbc09369c" +
            "?crop=entropy&cs=srgb&fm=jpg&q=85&w=1200";

    // ----------------------------------------------------------------
    // FXML – pannello sinistro
    // ----------------------------------------------------------------

    @FXML private StackPane heroPane;
    @FXML private ImageView heroImage;

    // ----------------------------------------------------------------
    // FXML – pannello destro
    // ----------------------------------------------------------------

    @FXML private VBox loginCard;
    @FXML private Label cardTitle;
    @FXML private Label cardSubtitle;

    @FXML private ToggleGroup roleGroup;
    @FXML private ToggleButton studentTab;
    @FXML private ToggleButton tutorTab;
    @FXML private ToggleButton adminTab;

    @FXML private Button googleBtn;
    @FXML private Button metaBtn;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    @FXML private Label toggleLabel;
    @FXML private Button toggleAuthBtn;

    // ----------------------------------------------------------------
    // Dipendenza applicativa
    // ----------------------------------------------------------------

    private final LoginController       loginController       = new LoginController();
    private final SocialLoginController socialLoginController = new SocialLoginController();

    //  ----------------------------------------------------------------
    // Inizializzazione
    //  ----------------------------------------------------------------

    @FXML
    public void initialize() {
        bindHeroImage();
        loadHeroImageAsync();
        setupRoleGroup();
        setupSocialButtons();
        passwordField.setOnAction(e -> handleLogin());
        playEntryAnimation();
    }

    // ----------------------------------------------------------------
    // Hero image
    // ----------------------------------------------------------------

    private void bindHeroImage() {
        // fitWidth/fitHeight seguono sempre le dimensioni dello StackPane
        heroImage.fitWidthProperty().bind(heroPane.widthProperty());
        heroImage.fitHeightProperty().bind(heroPane.heightProperty());
        heroImage.setPreserveRatio(false);
        heroImage.setSmooth(true);
    }

    private void loadHeroImageAsync() {
        // Il secondo parametro true = caricamento in background thread
        heroImage.setImage(new Image(HERO_IMAGE_URL, true));
    }

    // ----------------------------------------------------------------
    // Role tabs – impedisce deselezione totale
    // ----------------------------------------------------------------

    private void setupRoleGroup() {
        roleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
            }
        });
    }

    // ----------------------------------------------------------------
    // Social buttons – icone SVG programmatiche
    // ----------------------------------------------------------------

    private void setupSocialButtons() {
        googleBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        googleBtn.setGraphic(buildSocialRow(buildGoogleIcon(), "Continue with Google"));

        metaBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        metaBtn.setGraphic(buildSocialRow(buildMetaIcon(), "Continue with Meta"));
    }

    private HBox buildSocialRow(javafx.scene.Node icon, String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #1C2621;");
        HBox row = new HBox(12, icon, lbl);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    /** Google logo: 4 SVGPath con i quattro colori brand. */
    private Group buildGoogleIcon() {
        SVGPath p1 = new SVGPath();
        p1.setContent("M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z");
        p1.setFill(Color.web("#4285F4"));

        SVGPath p2 = new SVGPath();
        p2.setContent("M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z");
        p2.setFill(Color.web("#34A853"));

        SVGPath p3 = new SVGPath();
        p3.setContent("M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z");
        p3.setFill(Color.web("#FBBC05"));

        SVGPath p4 = new SVGPath();
        p4.setContent("M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z");
        p4.setFill(Color.web("#EA4335"));

        Group g = new Group(p1, p2, p3, p4);
        g.getTransforms().add(new Scale(0.83, 0.83, 0, 0));
        return g;
    }

    /** Meta logo: singolo path con blu brand #1877F2. */
    private SVGPath buildMetaIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z");
        path.setFill(Color.web("#1877F2"));
        path.getTransforms().add(new Scale(0.83, 0.83, 0, 0));
        return path;
    }

    // ----------------------------------------------------------------
    // Animazione d'ingresso
    // ----------------------------------------------------------------

    private void playEntryAnimation() {
        loginCard.setOpacity(0);
        loginCard.setTranslateY(12);

        FadeTransition fade = new FadeTransition(Duration.millis(420), loginCard);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(420), loginCard);
        slide.setFromY(12);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    //  ----------------------------------------------------------------
    // Handler FXML
    //  ----------------------------------------------------------------

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter your username and password.");
            return;
        }

        hideError();

        LoginBean bean = new LoginBean(username, password);
        try {
            String token = loginController.login(bean);
            SceneManager.getInstance().setSessionToken(token);
            navigateByRole(token);
        } catch (AuthenticationException e) {
            showError(e.getMessage());
            passwordField.clear();
            passwordField.requestFocus();
        } catch (DatabaseException e) {
            showError("Service unavailable. Please try again later.");
        }
    }

    @FXML
    public void handleGoogleLogin() {
        runSocialLoginAsync("Google", () -> socialLoginController.loginWithGoogle());
    }

    @FXML
    public void handleMetaLogin() {
        runSocialLoginAsync("Meta", () -> socialLoginController.loginWithMeta());
    }

    /**
     * Esegue il flusso OAuth in un thread separato per non bloccare l'UI.
     * Disabilita i bottoni social durante l'attesa e li riabilita in caso di errore.
     */
    private void runSocialLoginAsync(String provider, Callable<String> loginCallable) {
        googleBtn.setDisable(true);
        metaBtn.setDisable(true);
        showInfo("Opening " + provider + " login in your browser…");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return loginCallable.call();
            }
        };

        task.setOnSucceeded(e -> {
            String token = task.getValue();
            SceneManager.getInstance().setSessionToken(token);
            navigateByRole(token);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = (ex != null && ex.getMessage() != null)
                    ? ex.getMessage()
                    : provider + " login fallito. Riprova.";
            showError(msg);
            googleBtn.setDisable(false);
            metaBtn.setDisable(false);
        });

        Thread t = new Thread(task, provider.toLowerCase() + "-oauth");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void handleForgotPassword() {
        showError("Password recovery is not available in this build.");
    }

    @FXML
    public void handleToggleAuth() {
        SceneManager.getInstance().showRegister();
    }

    // ----------------------------------------------------------------
    // Navigazione post-login
    // ----------------------------------------------------------------

    private void navigateByRole(String token) {
        Session session = SessionManager.getInstance().getSession(token);
        if (session.isAdmin()) {
            SceneManager.getInstance().showAdminHome();
        } else if (session.isTutor()) {
            SceneManager.getInstance().showTutorHome();
        } else {
            SceneManager.getInstance().showStudentHome();
        }
    }

    // ----------------------------------------------------------------
    // Utility UI
    // ----------------------------------------------------------------

    private void showError(String message) {
        showMessage(message, "error-label", "info-label");
    }

    private void showInfo(String message) {
        showMessage(message, "info-label", "error-label");
    }

    private void showMessage(String message, String addStyle, String removeStyle) {
        errorLabel.getStyleClass().remove(removeStyle);
        if (!errorLabel.getStyleClass().contains(addStyle)) {
            errorLabel.getStyleClass().add(addStyle);
        }
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        FadeTransition ft = new FadeTransition(Duration.millis(200), errorLabel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
