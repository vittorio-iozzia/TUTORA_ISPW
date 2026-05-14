package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.bean.RegistrationBean;
import it.ispw.tutora.controller.application.RegistrationController;
import it.ispw.tutora.view.SceneManager;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleGroup;
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
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Controller grafico per la schermata di Registrazione.
 *
 * Replica fedele del form React in LoginPage.jsx:
 *   - Selettore ruolo (Student / Tutor / Admin)
 *   - First Name / Last Name affiancati
 *   - Username, Email, Phone
 *   - Date of Birth / Country affiancati
 *   - Password e Confirm Password con toggle visibilità (occhio)
 *   - Checkbox accettazione Terms & Privacy
 *   - Pulsante "Create Account" abilitato solo dopo accettazione termini
 *   - Divider + social buttons Google / Meta (2 colonne)
 *   - Link "Sign in" per tornare al login
 *
 * La registrazione è riservata agli studenti; il selettore ruolo
 * è presente per coerenza visiva ma non altera il flusso applicativo.
 */
public class RegistrationGfxController {

    private static final String HERO_IMAGE_URL =
            "https://images.unsplash.com/photo-1766117651759-640cbc09369c" +
            "?crop=entropy&cs=srgb&fm=jpg&q=85&w=1200";

    // ----------------------------------------------------------------
    // FXML – pannello sinistro
    // ----------------------------------------------------------------

    @FXML private StackPane heroPane;
    @FXML private ImageView heroImage;

    // ----------------------------------------------------------------
    // FXML – card
    // ----------------------------------------------------------------

    @FXML private VBox registerCard;

    // ----------------------------------------------------------------
    // FXML – role selector
    // ----------------------------------------------------------------

    @FXML private ToggleGroup roleGroup;
    @FXML private ToggleButton studentTab;

    // ----------------------------------------------------------------
    // FXML – campi form
    // ----------------------------------------------------------------

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;

    @FXML private PasswordField passwordField;
    @FXML private TextField     passwordVisible;
    @FXML private FontIcon      pwEyeIcon;
    @FXML private Button        togglePasswordBtn;

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField     confirmPasswordVisible;
    @FXML private FontIcon      confirmEyeIcon;
    @FXML private Button        toggleConfirmPasswordBtn;

    @FXML private CheckBox termsCheckBox;

    // ----------------------------------------------------------------
    // FXML – feedback e azioni
    // ----------------------------------------------------------------

    @FXML private Label  errorLabel;
    @FXML private Button registerButton;
    @FXML private Button googleBtn;
    @FXML private Button metaBtn;

    // ----------------------------------------------------------------
    // Stato toggle password
    // ----------------------------------------------------------------

    private boolean showPassword        = false;
    private boolean showConfirmPassword = false;

    // ----------------------------------------------------------------
    // Dipendenza applicativa
    // ----------------------------------------------------------------

    private final RegistrationController registrationController = new RegistrationController();

    // ----------------------------------------------------------------
    // Inizializzazione
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        bindHeroImage();
        loadHeroImageAsync();
        setupRoleGroup();
        setupPasswordSync();
        setupTermsCheckBox();
        setupSocialButtons();
        playEntryAnimation();
    }

    // ----------------------------------------------------------------
    // Hero image
    // ----------------------------------------------------------------

    private void bindHeroImage() {
        heroImage.fitWidthProperty().bind(heroPane.widthProperty());
        heroImage.fitHeightProperty().bind(heroPane.heightProperty());
        heroImage.setPreserveRatio(false);
        heroImage.setSmooth(true);
    }

    private void loadHeroImageAsync() {
        heroImage.setImage(new Image(HERO_IMAGE_URL, true));
    }

    // ----------------------------------------------------------------
    // Role group – impedisce che Student venga deselezionato
    // ----------------------------------------------------------------

    private void setupRoleGroup() {
        roleGroup.selectedToggleProperty().addListener((obs, old, nw) -> {
            if (nw == null && old != null) old.setSelected(true);
        });
    }

    // ----------------------------------------------------------------
    // Sincronizzazione PasswordField ↔ TextField (toggle visibilità)
    // ----------------------------------------------------------------

    private void setupPasswordSync() {
        // Quando l'utente digita nel PasswordField, riportiamo il testo nel TextField
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

    // ----------------------------------------------------------------
    // Terms checkbox → abilita/disabilita il pulsante Create Account
    // ----------------------------------------------------------------

    private void setupTermsCheckBox() {
        registerButton.disableProperty().bind(
                termsCheckBox.selectedProperty().not()
        );
    }

    // ----------------------------------------------------------------
    // Social buttons – icone SVG programmatiche (identiche al login)
    // ----------------------------------------------------------------

    private void setupSocialButtons() {
        googleBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        googleBtn.setGraphic(buildSocialRow(buildGoogleIcon(), "Google"));

        metaBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        metaBtn.setGraphic(buildSocialRow(buildMetaIcon(), "Meta"));
    }

    private HBox buildSocialRow(javafx.scene.Node icon, String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #1C2621;");
        HBox row = new HBox(10, icon, lbl);
        row.setAlignment(Pos.CENTER);
        return row;
    }

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

    private SVGPath buildMetaIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z");
        path.setFill(Color.web("#1877F2"));
        path.getTransforms().add(new Scale(0.83, 0.83, 0, 0));
        return path;
    }

    // ----------------------------------------------------------------
    // Animazione d'ingresso (identica al login)
    // ----------------------------------------------------------------

    private void playEntryAnimation() {
        registerCard.setOpacity(0);
        registerCard.setTranslateY(12);

        FadeTransition fade = new FadeTransition(Duration.millis(420), registerCard);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(420), registerCard);
        slide.setFromY(12);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    // ----------------------------------------------------------------
    // Handler FXML
    // ----------------------------------------------------------------

    @FXML
    public void handleTogglePassword() {
        showPassword = !showPassword;
        swapPasswordVisibility(
                passwordField, passwordVisible,
                pwEyeIcon, showPassword
        );
    }

    @FXML
    public void handleToggleConfirmPassword() {
        showConfirmPassword = !showConfirmPassword;
        swapPasswordVisibility(
                confirmPasswordField, confirmPasswordVisible,
                confirmEyeIcon, showConfirmPassword
        );
    }

    private void swapPasswordVisibility(PasswordField pw, TextField plain,
                                        FontIcon icon, boolean reveal) {
        if (reveal) {
            plain.setText(pw.getText());
            pw.setVisible(false);
            pw.setManaged(false);
            plain.setVisible(true);
            plain.setManaged(true);
            plain.requestFocus();
            plain.positionCaret(plain.getText().length());
            icon.setIconLiteral("fas-eye-slash");
        } else {
            pw.setText(plain.getText());
            plain.setVisible(false);
            plain.setManaged(false);
            pw.setVisible(true);
            pw.setManaged(true);
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
        // TODO: aprire dialog o browser con Terms of Service
    }

    @FXML
    public void handlePrivacyLink() {
        // TODO: aprire dialog o browser con Privacy Policy
    }

    @FXML
    public void handleGoToLogin() {
        SceneManager.getInstance().showLogin();
    }

    // ----------------------------------------------------------------
    // Utility UI
    // ----------------------------------------------------------------

    private void showError(String message) {
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
