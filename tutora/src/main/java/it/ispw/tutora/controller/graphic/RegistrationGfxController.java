package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.bean.RegistrationBean;
import it.ispw.tutora.controller.application.RegistrationController;
import it.ispw.tutora.view.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class RegistrationGfxController extends AuthGfxController {

    @FXML private VBox registerCard;
    @FXML private ToggleButton studentTab;

    @FXML private TextField     firstNameField;
    @FXML private TextField     lastNameField;
    @FXML private TextField     usernameField;
    @FXML private TextField     emailField;

    @FXML private PasswordField passwordField;
    @FXML private TextField     passwordVisible;
    @FXML private FontIcon      pwEyeIcon;
    @FXML private Button        togglePasswordBtn;

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField     confirmPasswordVisible;
    @FXML private FontIcon      confirmEyeIcon;
    @FXML private Button        toggleConfirmPasswordBtn;

    @FXML private CheckBox termsCheckBox;
    @FXML private Button   registerButton;

    private boolean showPassword        = false;
    private boolean showConfirmPassword = false;

    private final RegistrationController registrationController = new RegistrationController();

    @FXML
    public void initialize() {
        bindHeroImage();
        loadHeroImageAsync();
        setupRoleGroup();
        setupPasswordSync();
        setupTermsCheckBox();
        setupSocialButtons();
        playEntryAnimation(registerCard);
    }

    private void setupSocialButtons() {
        googleBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        googleBtn.setGraphic(buildSocialRow(buildGoogleIcon(), "Google", 10));
        metaBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        metaBtn.setGraphic(buildSocialRow(buildMetaIcon(), "Meta", 10));
    }

    private void setupPasswordSync() {
        passwordField.textProperty().addListener((obs, old, nw) -> {
            if (!passwordVisible.isFocused()) passwordVisible.setText(nw);
        });
        passwordVisible.textProperty().addListener((obs, old, nw) -> {
            if (!passwordField.isFocused()) passwordField.setText(nw);
        });
        confirmPasswordField.textProperty().addListener((obs, old, nw) -> {
            if (!confirmPasswordVisible.isFocused()) confirmPasswordVisible.setText(nw);
        });
        confirmPasswordVisible.textProperty().addListener((obs, old, nw) -> {
            if (!confirmPasswordField.isFocused()) confirmPasswordField.setText(nw);
        });
    }

    private void setupTermsCheckBox() {
        registerButton.disableProperty().bind(termsCheckBox.selectedProperty().not());
    }

    @FXML
    public void handleTogglePassword() {
        showPassword = !showPassword;
        swapPasswordVisibility(passwordField, passwordVisible, pwEyeIcon, showPassword);
    }

    @FXML
    public void handleToggleConfirmPassword() {
        showConfirmPassword = !showConfirmPassword;
        swapPasswordVisibility(confirmPasswordField, confirmPasswordVisible,
                confirmEyeIcon, showConfirmPassword);
    }

    private void swapPasswordVisibility(PasswordField pw, TextField plain,
                                        FontIcon icon, boolean reveal) {
        if (reveal) {
            plain.setText(pw.getText());
            pw.setVisible(false); pw.setManaged(false);
            plain.setVisible(true); plain.setManaged(true);
            plain.requestFocus();
            plain.positionCaret(plain.getText().length());
            icon.setIconLiteral("fas-eye-slash");
        } else {
            pw.setText(plain.getText());
            plain.setVisible(false); plain.setManaged(false);
            pw.setVisible(true); pw.setManaged(true);
            pw.requestFocus();
            pw.positionCaret(pw.getText().length());
            icon.setIconLiteral("fas-eye");
        }
    }

    @FXML
    public void handleRegister() {
        RegistrationBean bean = new RegistrationBean();
        bean.setName(firstNameField.getText().trim());
        bean.setSurname(lastNameField.getText().trim());
        bean.setUsername(usernameField.getText().trim());
        bean.setEmail(emailField.getText().trim());
        bean.setPassword(showPassword
                ? passwordVisible.getText()
                : passwordField.getText());
        bean.setConfirmPassword(showConfirmPassword
                ? confirmPasswordVisible.getText()
                : confirmPasswordField.getText());

        hideError();
        registrationController.register(bean);

        if (bean.isSuccess()) {
            SceneManager.getInstance().showLogin();
        } else {
            showError(bean.getErrorMessage());
        }
    }

    @FXML
    public void handleGoogleSignUp() {
        showError("Social sign-up is not available in this build.");
    }

    @FXML
    public void handleMetaSignUp() {
        showError("Social sign-up is not available in this build.");
    }

    @FXML
    public void handleTermsLink() {
        // Not yet implemented in this release
    }

    @FXML
    public void handlePrivacyLink() {
        // Not yet implemented in this release
    }

    @FXML
    public void handleGoToLogin() {
        SceneManager.getInstance().showLogin();
    }
}
