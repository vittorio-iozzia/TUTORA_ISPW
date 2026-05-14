package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.SceneManager;
import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;

/**
 * Graphic controller for the student_profile.fxml full-page view.
 *
 * Populates all sections (hero, stats, about, contact) from the
 * current session's Student data, and runs count/scale animations
 * on the stat cards mirroring the dashboard style.
 */
public class StudentProfileGfxController {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMMM yyyy");

    // Hero
    @FXML private Label heroAvatarLabel;
    @FXML private Label     heroNameLabel;
    @FXML private Label     heroUsernameLabel;
    @FXML private Label     heroRoleLabel;
    @FXML private Label     heroStatusLabel;
    @FXML private Label     heroMemberSinceLabel;

    // Stat cards (VBox wrappers for hover animation)
    @FXML private VBox budgetCard;
    @FXML private VBox lessonsCard;
    @FXML private VBox tutorsCard;
    @FXML private VBox ratingCard;

    // Stat values
    @FXML private Label budgetValueLabel;
    @FXML private Label lessonsValueLabel;
    @FXML private Label tutorsValueLabel;
    @FXML private Label ratingValueLabel;

    // About
    @FXML private Label aboutLabel;

    // Interests
    @FXML private HBox  interestsPills;
    @FXML private Label interestsEmptyLabel;

    // Contact
    @FXML private Label emailLabel;
    @FXML private Label joinedLabel;
    @FXML private Label accountStatusLabel;

    // ----------------------------------------------------------------
    // Inizializzazione
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        String token = SceneManager.getInstance().getSessionToken();
        Session session = SessionManager.getInstance().getSession(token);
        Student student = (Student) session.getUser();

        populateHero(student);
        populateStats(student);
        populateAbout(student);
        populateInterests(student);
        populateContact(student);

        addHoverLift(budgetCard);
        addHoverLift(lessonsCard);
        addHoverLift(tutorsCard);
        addHoverLift(ratingCard);
    }

    // ----------------------------------------------------------------
    // Hero
    // ----------------------------------------------------------------

    private void populateHero(Student student) {
        String initial = String.valueOf(student.getName().charAt(0)).toUpperCase();
        heroAvatarLabel.setText(initial);
        heroNameLabel.setText(student.getName() + " " + student.getSurname());
        heroUsernameLabel.setText("@" + student.getUsername());
        heroRoleLabel.setText("STUDENT");
        heroStatusLabel.setText(student.isActive() ? "Active" : "Inactive");
        heroStatusLabel.setStyle(student.isActive()
                ? "-fx-text-fill: #27AE60;"
                : "-fx-text-fill: #E74C3C;");
        heroMemberSinceLabel.setText(
                student.getCreatedAt() != null
                        ? student.getCreatedAt().format(DATE_FMT)
                        : "—");
    }

    // ----------------------------------------------------------------
    // Stats (count animations)
    // ----------------------------------------------------------------

    private void populateStats(Student student) {
        double budget = student.getBudget() != null
                ? student.getBudget().doubleValue() : 0.0;
        animateStat(budgetValueLabel,  budget, "€%.2f");
        animateStat(lessonsValueLabel, 5,      "%.0f");   // demo representative
        animateStat(tutorsValueLabel,  2,      "%.0f");   // demo representative
        ratingValueLabel.setText("—");                    // not yet implemented
    }

    private void animateStat(Label label, double target, String format) {
        DoubleProperty prop = new SimpleDoubleProperty(0);
        prop.addListener((obs, o, n) ->
                label.setText(String.format(format, n.doubleValue())));
        new Timeline(new KeyFrame(Duration.millis(900),
                new KeyValue(prop, target, Interpolator.EASE_OUT))).play();
    }

    // ----------------------------------------------------------------
    // About
    // ----------------------------------------------------------------

    private void populateAbout(Student student) {
        String desc = student.getDescription();
        aboutLabel.setText(desc != null && !desc.isBlank()
                ? desc
                : "No description added yet. Tell the world about yourself and what you want to learn!");
    }

    // ----------------------------------------------------------------
    // Interests (from Student.getInterests())
    // ----------------------------------------------------------------

    private void populateInterests(Student student) {
        if (student.getInterests().isEmpty()) {
            interestsPills.setManaged(false);
            interestsPills.setVisible(false);
            interestsEmptyLabel.setManaged(true);
            interestsEmptyLabel.setVisible(true);
        } else {
            for (var cat : student.getInterests()) {
                Label pill = new Label(cat.getName());
                pill.getStyleClass().add("interest-pill");
                interestsPills.getChildren().add(pill);
            }
        }
    }

    // ----------------------------------------------------------------
    // Contact
    // ----------------------------------------------------------------

    private void populateContact(Student student) {
        emailLabel.setText(student.getEmail());
        joinedLabel.setText(student.getCreatedAt() != null
                ? student.getCreatedAt().format(DATE_FMT)
                : "—");
        accountStatusLabel.setText(student.isActive() ? "Verified & Active" : "Inactive");
    }

    // ----------------------------------------------------------------
    // Hover lift animation (same pattern as dashboard stat cards)
    // ----------------------------------------------------------------

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

    @FXML
    public void handleClose() {
        Stage stage = (Stage) heroNameLabel.getScene().getWindow();
        stage.close();
    }
}
