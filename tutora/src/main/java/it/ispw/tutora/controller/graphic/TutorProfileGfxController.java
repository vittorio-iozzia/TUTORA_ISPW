package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.dao.TutorExpertiseDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.enums.Status;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.TutorExpertise;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.AvatarManager;
import it.ispw.tutora.view.SceneManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Graphic controller per la vista tutor_profile.fxml.
 *
 * Popola hero, stats, about, expertise e contatto dai dati reali
 * del model. Gestisce foto profilo con FileChooser e sincronizza
 * il cambio via {@link AvatarManager}.
 */
public class TutorProfileGfxController {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMMM yyyy");

    // ----------------------------------------------------------------
    // Callback di navigazione
    // ----------------------------------------------------------------

    private static Runnable onBackCallback;

    public static void setOnBackCallback(Runnable r) {
        onBackCallback = r;
    }

    public static Runnable getOnBackCallback() {
        return onBackCallback;
    }

    // ----------------------------------------------------------------
    // FXML fields
    // ----------------------------------------------------------------

    @FXML private StackPane heroAvatarPane;
    @FXML private Label     heroAvatarLabel;
    @FXML private ImageView heroAvatarImage;
    @FXML private StackPane avatarOverlay;

    @FXML private Label heroNameLabel;
    @FXML private Label heroUsernameLabel;
    @FXML private Label heroRoleLabel;
    @FXML private Label heroStatusLabel;
    @FXML private Label heroMemberSinceLabel;

    @FXML private VBox  ratingCard;
    @FXML private VBox  reviewsCard;
    @FXML private VBox  expertisesCard;
    @FXML private VBox  lessonsCard;

    @FXML private Label ratingValueLabel;
    @FXML private Label reviewsValueLabel;
    @FXML private Label expertisesValueLabel;
    @FXML private Label lessonsValueLabel;

    @FXML private Label    aboutLabel;
    @FXML private TextArea aboutTextArea;
    @FXML private HBox     descEditButtons;
    @FXML private Button   editDescBtn;

    @FXML private FlowPane expertisePills;
    @FXML private Label expertisesEmptyLabel;

    @FXML private Label emailLabel;
    @FXML private Label joinedLabel;
    @FXML private Label accountStatusLabel;
    @FXML private Label ratingDetailLabel;

    private String username;
    private Tutor  currentTutor;

    // ----------------------------------------------------------------
    // Inizializzazione
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        String token = SceneManager.getInstance().getSessionToken();
        Session session = SessionManager.getInstance().getSession(token);
        Tutor tutor = (Tutor) session.getUser();
        this.username = tutor.getUsername();
        this.currentTutor = tutor;

        populateHero(tutor);
        populateRatingStat(tutor);
        populateAbout(tutor);
        populateContact(tutor);
        loadExpertisesAndLessons(tutor);

        setupAvatarInteraction();
        applyStoredAvatar();

        addHoverLift(ratingCard);
        addHoverLift(reviewsCard);
        addHoverLift(expertisesCard);
        addHoverLift(lessonsCard);
    }

    // ----------------------------------------------------------------
    // Hero
    // ----------------------------------------------------------------

    private void populateHero(Tutor tutor) {
        String initial = String.valueOf(tutor.getName().charAt(0)).toUpperCase();
        heroAvatarLabel.setText(initial);
        heroNameLabel.setText(tutor.getName() + " " + tutor.getSurname());
        heroUsernameLabel.setText("@" + tutor.getUsername());
        heroRoleLabel.setText("TUTOR");
        heroStatusLabel.setText(tutor.isActive() ? "Active" : "Inactive");
        heroStatusLabel.setStyle(tutor.isActive()
                ? "-fx-background-color: #27AE60; -fx-text-fill: white;"
                : "-fx-background-color: #E74C3C; -fx-text-fill: white;");
        heroMemberSinceLabel.setText(
                tutor.getCreatedAt() != null
                        ? tutor.getCreatedAt().format(DATE_FMT)
                        : "—");
    }

    // ----------------------------------------------------------------
    // Rating (sincrono dal model)
    // ----------------------------------------------------------------

    private void populateRatingStat(Tutor tutor) {
        if (tutor.getRating() != null && tutor.getRating().doubleValue() > 0) {
            String ratingStr = String.format("%.1f ★", tutor.getRating().doubleValue());
            ratingValueLabel.setText(ratingStr);
            ratingDetailLabel.setText(ratingStr + "  (" + tutor.getRatingCount() + " reviews)");
        } else {
            ratingValueLabel.setText("—");
            ratingDetailLabel.setText("No reviews yet");
        }
        animateStat(reviewsValueLabel, tutor.getRatingCount(), "%.0f");
    }

    // ----------------------------------------------------------------
    // About
    // ----------------------------------------------------------------

    private void populateAbout(Tutor tutor) {
        String desc = tutor.getDescription();
        aboutLabel.setText(desc != null && !desc.isBlank()
                ? desc
                : "No description added yet. Tell students about yourself, your teaching style, and your areas of expertise!");
    }

    // ----------------------------------------------------------------
    // Contatto
    // ----------------------------------------------------------------

    private void populateContact(Tutor tutor) {
        emailLabel.setText(tutor.getEmail());
        joinedLabel.setText(tutor.getCreatedAt() != null
                ? tutor.getCreatedAt().format(DATE_FMT)
                : "—");
        accountStatusLabel.setText(tutor.isActive() ? "Verified & Active" : "Inactive");
    }

    // ----------------------------------------------------------------
    // Expertise + Lessons insegnate (asincrono, da DAO)
    // ----------------------------------------------------------------

    private void loadExpertisesAndLessons(Tutor tutor) {
        String uname = tutor.getUsername();

        Task<long[]> task = new Task<>() {
            @Override
            protected long[] call() throws Exception {
                // Expertise approvate
                TutorExpertiseDao expDao = DaoFactory.getInstance().createTutorExpertiseDao();
                List<TutorExpertise> expertises = expDao.findByTutor(null, uname);
                List<TutorExpertise> approved = expertises.stream()
                        .filter(e -> e.getStatus() == Status.APPROVED)
                        .toList();

                // Lezioni insegnate (booking PAID)
                BookingDao bookingDao = DaoFactory.getInstance().createBookingDao();
                List<Booking> bookings = bookingDao.findByTutor(
                        DaoFactory.getInstance().getConnection(), uname);
                long paidLessons = bookings.stream()
                        .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                        .count();

                // Comunica la lista approvate al FX thread tramite side channel
                Platform.runLater(() -> populateExpertisePills(approved));

                return new long[]{ approved.size(), paidLessons };
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            long[] res = task.getValue();
            animateStat(expertisesValueLabel, res[0], "%.0f");
            animateStat(lessonsValueLabel,    res[1], "%.0f");
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            expertisesValueLabel.setText("—");
            lessonsValueLabel.setText("—");
        }));

        Thread t = new Thread(task, "tutor-profile-stats");
        t.setDaemon(true);
        t.start();
    }

    /** Popola le pill delle expertise approvate nella sezione About. */
    private void populateExpertisePills(List<TutorExpertise> approved) {
        if (approved.isEmpty()) {
            expertisePills.setManaged(false);
            expertisePills.setVisible(false);
            expertisesEmptyLabel.setManaged(true);
            expertisesEmptyLabel.setVisible(true);
        } else {
            for (TutorExpertise exp : approved) {
                String name = exp.getSubcategory() != null
                        ? exp.getSubcategory().getName() : "—";
                Label pill = new Label(name);
                pill.getStyleClass().add("interest-pill");
                expertisePills.getChildren().add(pill);
            }
        }
    }

    // ----------------------------------------------------------------
    // Avatar
    // ----------------------------------------------------------------

    private void applyStoredAvatar() {
        if (AvatarManager.hasAvatar(username)) {
            displayAvatar(AvatarManager.getAvatarPath(username));
        }
    }

    private void displayAvatar(String filePath) {
        String uri = new File(filePath).toURI().toString();
        Image img = new Image(uri, 88, 88, false, true);
        heroAvatarImage.setImage(img);
        Circle clip = new Circle(44, 44, 44);
        heroAvatarImage.setClip(clip);
        heroAvatarImage.setVisible(true);
        heroAvatarImage.setManaged(true);
        heroAvatarLabel.setVisible(false);
        heroAvatarLabel.setManaged(false);
    }

    private void setupAvatarInteraction() {
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
            displayAvatar(path);
        }
    }

    // ----------------------------------------------------------------
    // Animazioni
    // ----------------------------------------------------------------

    private void animateStat(Label label, double target, String format) {
        DoubleProperty prop = new SimpleDoubleProperty(0);
        prop.addListener((obs, o, n) ->
                label.setText(String.format(format, n.doubleValue())));
        new Timeline(new KeyFrame(Duration.millis(900),
                new KeyValue(prop, target, Interpolator.EASE_OUT))).play();
    }

    private void addHoverLift(VBox card) {
        ScaleTransition up   = new ScaleTransition(Duration.millis(140), card);
        up.setToX(1.025); up.setToY(1.025);
        ScaleTransition down = new ScaleTransition(Duration.millis(140), card);
        down.setToX(1.0);  down.setToY(1.0);
        card.setOnMouseEntered(e -> { down.stop(); up.playFromStart(); });
        card.setOnMouseExited (e -> { up.stop();   down.playFromStart(); });
    }

    // ----------------------------------------------------------------
    // FXML handlers
    // ----------------------------------------------------------------

    /** Entra in modalità modifica descrizione. */
    @FXML
    public void handleEditDescription() {
        aboutTextArea.setText(currentTutor.getDescription() != null
                ? currentTutor.getDescription() : "");
        aboutLabel.setVisible(false);
        aboutLabel.setManaged(false);
        aboutTextArea.setVisible(true);
        aboutTextArea.setManaged(true);
        descEditButtons.setVisible(true);
        descEditButtons.setManaged(true);
        editDescBtn.setVisible(false);
        editDescBtn.setManaged(false);
    }

    /** Salva la descrizione modificata. */
    @FXML
    public void handleSaveDescription() {
        String newDesc = aboutTextArea.getText().trim();
        currentTutor.setDescription(newDesc.isBlank() ? null : newDesc);
        aboutLabel.setText(newDesc.isBlank()
                ? "No description added yet. Tell students about yourself, your teaching style, and your areas of expertise!"
                : newDesc);
        exitEditMode();
    }

    /** Annulla la modifica senza salvare. */
    @FXML
    public void handleCancelDescription() {
        exitEditMode();
    }

    private void exitEditMode() {
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
        if (onBackCallback != null) {
            onBackCallback.run();
        }
    }
}
