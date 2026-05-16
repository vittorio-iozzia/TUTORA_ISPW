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
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.concurrent.Callable;

public class LoginGfxController extends AuthGfxController {

    @FXML private VBox loginCard;

    @FXML private ToggleButton studentTab;
    @FXML private ToggleButton tutorTab;
    @FXML private ToggleButton adminTab;

    @FXML private TextField    usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button       loginButton;

    private final LoginController       loginController       = new LoginController();
    private final SocialLoginController socialLoginController = new SocialLoginController();

    @FXML
    public void initialize() {
        bindHeroImage();
        loadHeroImageAsync();
        setupRoleGroup();
        setupSocialButtons();
        passwordField.setOnAction(e -> handleLogin());
        playEntryAnimation(loginCard);
    }

    private void setupSocialButtons() {
        googleBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        googleBtn.setGraphic(buildSocialRow(buildGoogleIcon(), "Continue with Google", 12));
        metaBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        metaBtn.setGraphic(buildSocialRow(buildMetaIcon(), "Continue with Meta", 12));
    }

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
        runSocialLoginAsync("Google", socialLoginController::loginWithGoogle);
    }

    @FXML
    public void handleMetaLogin() {
        runSocialLoginAsync("Meta", socialLoginController::loginWithMeta);
    }

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

    @Override
    protected void showError(String message) {
        showMessage(message, "error-label", "info-label");
    }

    protected void showInfo(String message) {
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
}
