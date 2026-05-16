package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.dao.TutorExpertiseDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.Status;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.TutorExpertise;
import it.ispw.tutora.view.AvatarManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller per il profilo pubblico di un tutor visto da uno studente.
 * Il tutor da visualizzare viene impostato via {@link #setTargetTutor(Tutor)}
 * prima che l'FXML venga caricato.
 * Mostra solo informazioni pubbliche: nome, rating, descrizione, expertise.
 * NON mostra email o dati privati.
 */
public class TutorPublicProfileGfxController {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMMM yyyy");

    // ----------------------------------------------------------------
    // Stato statico (impostato da HomeGfxController prima del caricamento)
    // ----------------------------------------------------------------

    private static Tutor targetTutor;
    private static Runnable onBackCallback;

    public static void setTargetTutor(Tutor t) { targetTutor = t; }
    public static void setOnBackCallback(Runnable r) { onBackCallback = r; }

    // ----------------------------------------------------------------
    // FXML fields
    // ----------------------------------------------------------------

    @FXML private StackPane heroAvatarPane;
    @FXML private Label     heroAvatarLabel;
    @FXML private ImageView heroAvatarImage;

    @FXML private Label heroNameLabel;
    @FXML private Label heroUsernameLabel;
    @FXML private Label heroRoleLabel;
    @FXML private Label heroStatusLabel;
    @FXML private Label heroMemberSinceLabel;

    @FXML private VBox  ratingCard;
    @FXML private VBox  reviewsCard;
    @FXML private VBox  expertisesCard;

    @FXML private Label ratingValueLabel;
    @FXML private Label reviewsValueLabel;
    @FXML private Label expertisesValueLabel;

    @FXML private Label     aboutLabel;
    @FXML private FlowPane  expertisePills;
    @FXML private Label     expertisesEmptyLabel;

    @FXML private Label joinedLabel;
    @FXML private Label accountStatusLabel;
    @FXML private Label ratingDetailLabel;

    // ----------------------------------------------------------------
    // Inizializzazione
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        Tutor tutor = targetTutor;
        if (tutor == null) return;

        populateHero(tutor);
        populateRatingStat(tutor);
        populateAbout(tutor);
        populateContact(tutor);
        loadExpertises(tutor);
        applyAvatarIfPresent(tutor);

        addHoverLift(ratingCard);
        addHoverLift(reviewsCard);
        addHoverLift(expertisesCard);
    }

    // ----------------------------------------------------------------
    // Populate
    // ----------------------------------------------------------------

    private void populateHero(Tutor tutor) {
        String initial = tutor.getName() != null && !tutor.getName().isEmpty()
                ? String.valueOf(tutor.getName().charAt(0)).toUpperCase() : "T";
        heroAvatarLabel.setText(initial);
        heroNameLabel.setText(tutor.getName() + " " + tutor.getSurname());
        heroUsernameLabel.setText("@" + tutor.getUsername());
        heroRoleLabel.setText("TUTOR");
        heroStatusLabel.setText(tutor.isActive() ? "Active" : "Inactive");
        heroStatusLabel.setStyle(tutor.isActive()
                ? "-fx-background-color: #27AE60; -fx-text-fill: white;"
                : "-fx-background-color: #E74C3C; -fx-text-fill: white;");
        heroMemberSinceLabel.setText(tutor.getCreatedAt() != null
                ? tutor.getCreatedAt().format(DATE_FMT) : "—");
    }

    private void populateRatingStat(Tutor tutor) {
        if (tutor.getRating() != null && tutor.getRating().doubleValue() > 0) {
            String s = String.format("%.1f ★", tutor.getRating().doubleValue());
            ratingValueLabel.setText(s);
            ratingDetailLabel.setText(s + "  (" + tutor.getRatingCount() + " reviews)");
        } else {
            ratingValueLabel.setText("—");
            ratingDetailLabel.setText("No reviews yet");
        }
        animateStat(reviewsValueLabel, tutor.getRatingCount(), "%.0f");
    }

    private void populateAbout(Tutor tutor) {
        String desc = tutor.getDescription();
        aboutLabel.setText(desc != null && !desc.isBlank()
                ? desc : "This tutor hasn't added a description yet.");
    }

    private void populateContact(Tutor tutor) {
        joinedLabel.setText(tutor.getCreatedAt() != null
                ? tutor.getCreatedAt().format(DATE_FMT) : "—");
        accountStatusLabel.setText(tutor.isActive() ? "Verified & Active" : "Inactive");
    }

    // ----------------------------------------------------------------
    // Expertise (asincrono)
    // ----------------------------------------------------------------

    private void loadExpertises(Tutor tutor) {
        String uname = tutor.getUsername();
        Task<List<TutorExpertise>> task = new Task<>() {
            @Override
            protected List<TutorExpertise> call() throws Exception {
                TutorExpertiseDao dao = DaoFactory.getInstance().createTutorExpertiseDao();
                List<TutorExpertise> all = dao.findByTutor(null, uname);
                return all.stream()
                        .filter(e -> e.getStatus() == Status.APPROVED)
                        .toList();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<TutorExpertise> approved = task.getValue();
            animateStat(expertisesValueLabel, approved.size(), "%.0f");
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
        }));
        task.setOnFailed(ev -> Platform.runLater(() -> expertisesValueLabel.setText("—")));
        Thread t = new Thread(task, "pub-profile-exp");
        t.setDaemon(true);
        t.start();
    }

    // ----------------------------------------------------------------
    // Avatar (se il tutor ha già impostato la sua foto profilo)
    // ----------------------------------------------------------------

    private void applyAvatarIfPresent(Tutor tutor) {
        String path = AvatarManager.getAvatarPath(tutor.getUsername());
        if (path == null || path.isBlank()) return;
        String uri = new File(path).toURI().toString();
        Image img = new Image(uri, 88, 88, false, true);
        heroAvatarImage.setImage(img);
        heroAvatarImage.setClip(new Circle(44, 44, 44));
        heroAvatarImage.setVisible(true);
        heroAvatarImage.setManaged(true);
        heroAvatarLabel.setVisible(false);
        heroAvatarLabel.setManaged(false);
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
        card.setOnMouseEntered(ev -> { down.stop(); up.playFromStart(); });
        card.setOnMouseExited (ev -> { up.stop();   down.playFromStart(); });
    }

    // ----------------------------------------------------------------
    // FXML handler
    // ----------------------------------------------------------------

    @FXML
    public void handleClose() {
        if (onBackCallback != null) onBackCallback.run();
    }
}
