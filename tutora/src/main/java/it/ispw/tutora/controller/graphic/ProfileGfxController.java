package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.controller.graphic.util.TutorBrowseUtil;
import it.ispw.tutora.model.User;
import it.ispw.tutora.view.AvatarManager;
import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.time.format.DateTimeFormatter;

/**
 * Base condivisa tra {@link TutorProfileGfxController} e {@link StudentProfileGfxController}.
 *
 * Contiene tutto il codice comune: hero, avatar, about, contatto,
 * animazioni, gestione descrizione e callback di navigazione.
 * Le sottoclassi forniscono solo le parti specifiche del ruolo.
 */
public abstract class ProfileGfxController {

    protected static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMMM yyyy");

    private static final String ACTIVE_STYLE   =
            "-fx-background-color: #27AE60; -fx-text-fill: white;";
    private static final String INACTIVE_STYLE =
            "-fx-background-color: #E74C3C; -fx-text-fill: white;";

    private static Runnable onBackCallback;

    public static void setOnBackCallback(Runnable r) { onBackCallback = r; }
    public static Runnable getOnBackCallback()        { return onBackCallback; }

    // ----------------------------------------------------------------
    // FXML fields comuni
    // ----------------------------------------------------------------

    @FXML protected StackPane heroAvatarPane;
    @FXML protected Label heroAvatarLabel;
    @FXML protected ImageView heroAvatarImage;
    @FXML protected StackPane avatarOverlay;

    @FXML protected Label heroNameLabel;
    @FXML protected Label heroUsernameLabel;
    @FXML protected Label heroRoleLabel;
    @FXML protected Label heroStatusLabel;
    @FXML protected Label heroMemberSinceLabel;

    @FXML protected Label aboutLabel;
    @FXML protected TextArea aboutTextArea;
    @FXML protected HBox descEditButtons;
    @FXML protected Button editDescBtn;

    @FXML protected Label emailLabel;
    @FXML protected Label joinedLabel;
    @FXML protected Label accountStatusLabel;

    protected String username;
    protected User   currentUser;

    // ----------------------------------------------------------------
    // Template methods
    // ----------------------------------------------------------------

    protected abstract String getRoleLabel();
    protected abstract String getDefaultDescription();

    // ----------------------------------------------------------------
    // Popolazione campi comuni
    // ----------------------------------------------------------------

    protected void populateHero(User user) {
        String initial = String.valueOf(user.getName().charAt(0)).toUpperCase();
        heroAvatarLabel.setText(initial);
        heroNameLabel.setText(user.getName() + " " + user.getSurname());
        heroUsernameLabel.setText("@" + user.getUsername());
        heroRoleLabel.setText(getRoleLabel());
        heroStatusLabel.setText(user.isActive() ? "Active" : "Inactive");
        heroStatusLabel.setStyle(user.isActive() ? ACTIVE_STYLE : INACTIVE_STYLE);
        heroMemberSinceLabel.setText(
                user.getCreatedAt() != null ? user.getCreatedAt().format(DATE_FMT) : "—");
    }

    protected void populateAbout(User user) {
        String desc = user.getDescription();
        aboutLabel.setText(desc != null && !desc.isBlank() ? desc : getDefaultDescription());
    }

    protected void populateContact(User user) {
        emailLabel.setText(user.getEmail());
        joinedLabel.setText(user.getCreatedAt() != null
                ? user.getCreatedAt().format(DATE_FMT) : "—");
        accountStatusLabel.setText(user.isActive() ? "Verified & Active" : "Inactive");
    }

    // ----------------------------------------------------------------
    // Avatar
    // ----------------------------------------------------------------

    protected void applyStoredAvatar() {
        String url = TutorBrowseUtil.resolveProfileImageUrl(username);
        if (url != null) displayAvatar(url);
    }

    protected void displayAvatar(String url) {
        Image img = new Image(url, 88, 88, false, true);
        heroAvatarImage.setImage(img);
        Circle clip = new Circle(44, 44, 44);
        heroAvatarImage.setClip(clip);
        heroAvatarImage.setVisible(true);
        heroAvatarImage.setManaged(true);
        heroAvatarLabel.setVisible(false);
        heroAvatarLabel.setManaged(false);
    }

    protected void setupAvatarInteraction() {
        heroAvatarPane.setCursor(Cursor.HAND);
        heroAvatarPane.setOnMouseEntered(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(150), avatarOverlay);
            ft.setToValue(1.0);
            ft.play();
        });
        heroAvatarPane.setOnMouseExited(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(150), avatarOverlay);
            ft.setToValue(0.0);
            ft.play();
        });
        heroAvatarPane.setOnMouseClicked(e -> handleChangeAvatar());
    }

    private void handleChangeAvatar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Scegli immagine profilo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Immagini", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp", "*.webp"));
        File file = chooser.showOpenDialog(heroAvatarPane.getScene().getWindow());
        if (file != null) {
            String path = file.getAbsolutePath();
            AvatarManager.setAvatarPath(username, path);
            displayAvatar(new File(path).toURI().toString());
        }
    }

    // ----------------------------------------------------------------
    // Animazioni
    // ----------------------------------------------------------------

    protected void animateStat(Label label, double target, String format) {
        DoubleProperty prop = new SimpleDoubleProperty(0);
        prop.addListener((obs, o, n) ->
                label.setText(String.format(format, n.doubleValue())));
        new Timeline(new KeyFrame(Duration.millis(900),
                new KeyValue(prop, target, Interpolator.EASE_OUT))).play();
    }

    protected void addHoverLift(VBox card) {
        ScaleTransition up   = new ScaleTransition(Duration.millis(140), card);
        up.setToX(1.025); up.setToY(1.025);
        ScaleTransition down = new ScaleTransition(Duration.millis(140), card);
        down.setToX(1.0);  down.setToY(1.0);
        card.setOnMouseEntered(e -> { down.stop(); up.playFromStart(); });
        card.setOnMouseExited (e -> { up.stop();   down.playFromStart(); });
    }

    // ----------------------------------------------------------------
    // Gestione descrizione (edit/save/cancel)
    // ----------------------------------------------------------------

    @FXML
    public void handleEditDescription() {
        aboutTextArea.setText(currentUser.getDescription() != null
                ? currentUser.getDescription() : "");
        aboutLabel.setVisible(false);
        aboutLabel.setManaged(false);
        aboutTextArea.setVisible(true);
        aboutTextArea.setManaged(true);
        descEditButtons.setVisible(true);
        descEditButtons.setManaged(true);
        editDescBtn.setVisible(false);
        editDescBtn.setManaged(false);
    }

    @FXML
    public void handleSaveDescription() {
        String newDesc = aboutTextArea.getText().trim();
        currentUser.setDescription(newDesc.isBlank() ? null : newDesc);
        aboutLabel.setText(newDesc.isBlank() ? getDefaultDescription() : newDesc);
        exitEditMode();
    }

    @FXML
    public void handleCancelDescription() {
        exitEditMode();
    }

    protected void exitEditMode() {
        aboutTextArea.setVisible(false);
        aboutTextArea.setManaged(false);
        descEditButtons.setVisible(false);
        descEditButtons.setManaged(false);
        aboutLabel.setVisible(true);
        aboutLabel.setManaged(true);
        editDescBtn.setVisible(true);
        editDescBtn.setManaged(true);
    }

    @FXML
    public void handleClose() {
        if (onBackCallback != null) onBackCallback.run();
    }
}
