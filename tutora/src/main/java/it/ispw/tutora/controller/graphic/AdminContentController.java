package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.bean.ApplicationReviewBean;
import it.ispw.tutora.controller.application.ApplyToBecomeATutorController;
import it.ispw.tutora.dao.TutorApplicationDao;
import it.ispw.tutora.dao.TutorDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.ApplicationStatus;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.TutorApplication;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.SceneManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.logging.Logger;

public class AdminContentController {

    private static final Logger LOGGER = Logger.getLogger(AdminContentController.class.getName());

    private final ApplyToBecomeATutorController appController = new ApplyToBecomeATutorController();

    // Stats
    @FXML private Label welcomeTitle;
    @FXML private HBox  statCard1;
    @FXML private HBox  statCard2;
    @FXML private HBox  statCard3;
    @FXML private HBox  statCard4;
    @FXML private Label totalUsersLabel;
    @FXML private Label activeTutorsLabel;
    @FXML private Label totalLessonsLabel;
    @FXML private Label revenueLabel;

    // Approvals tab
    @FXML private VBox  applicationsList;
    @FXML private Label pendingCountLabel;

    // Users tab
    @FXML private TextField userSearchField;
    @FXML private VBox      usersList;

    // State
    private List<TutorApplication> pendingApplications = List.of();
    private List<Tutor>            allTutors           = List.of();

    // ----------------------------------------------------------------
    // Init
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        String token    = SceneManager.getInstance().getSessionToken();
        Session session = SessionManager.getInstance().getSession(token);
        welcomeTitle.setText("Welcome, " + session.getUser().getName() + "!");

        pendingApplications = loadPendingApplications();
        allTutors           = loadTutors();

        animateStat(totalUsersLabel,   2847,             "%.0f");
        animateStat(activeTutorsLabel, allTutors.size(), "%.0f");
        animateStat(totalLessonsLabel, 8429,             "%.0f");
        animateStat(revenueLabel,      124580,           "€%.0f");

        addHoverLift(statCard1);
        addHoverLift(statCard2);
        addHoverLift(statCard3);
        addHoverLift(statCard4);

        buildApplicationsList();
        buildUsersList(allTutors);

        userSearchField.textProperty().addListener((obs, old, val) -> filterUsers(val));
    }

    // ----------------------------------------------------------------
    // DAO
    // ----------------------------------------------------------------

    private List<TutorApplication> loadPendingApplications() {
        try {
            TutorApplicationDao dao = DaoFactory.getInstance().createTutorApplicationDao();
            return dao.findByStatus(DaoFactory.getInstance().getConnection(), ApplicationStatus.SUBMITTED);
        } catch (DatabaseException e) {
            LOGGER.warning("Cannot load applications: " + e.getMessage());
            return List.of();
        }
    }

    private List<Tutor> loadTutors() {
        try {
            TutorDao dao = DaoFactory.getInstance().createTutorDao();
            return dao.selectAllTutors(DaoFactory.getInstance().getConnection());
        } catch (DatabaseException e) {
            LOGGER.warning("Cannot load tutors: " + e.getMessage());
            return List.of();
        }
    }

    // ----------------------------------------------------------------
    // Animations
    // ----------------------------------------------------------------

    private void animateStat(Label label, double target, String format) {
        DoubleProperty prop = new SimpleDoubleProperty(0);
        prop.addListener((obs, o, n) -> label.setText(String.format(format, n.doubleValue())));
        new Timeline(new KeyFrame(Duration.millis(900),
                new KeyValue(prop, target, Interpolator.EASE_OUT))).play();
    }

    private void addHoverLift(HBox card) {
        ScaleTransition up   = new ScaleTransition(Duration.millis(140), card);
        up.setToX(1.035); up.setToY(1.035);
        ScaleTransition down = new ScaleTransition(Duration.millis(140), card);
        down.setToX(1.0);  down.setToY(1.0);
        card.setOnMouseEntered(e -> { down.stop(); up.playFromStart(); });
        card.setOnMouseExited (e -> { up.stop();   down.playFromStart(); });
        card.setStyle("-fx-cursor: default;");
    }

    // ----------------------------------------------------------------
    // Tutor Approvals tab
    // ----------------------------------------------------------------

    private void buildApplicationsList() {
        pendingCountLabel.setText(pendingApplications.size() + " pending");
        if (pendingApplications.isEmpty()) {
            Label empty = new Label("No pending applications.");
            empty.setStyle("-fx-text-fill: #8FAF9A; -fx-font-size: 13px;");
            applicationsList.getChildren().add(empty);
            return;
        }
        for (TutorApplication app : pendingApplications) {
            applicationsList.getChildren().add(buildApplicationCard(app));
        }
    }

    private HBox buildApplicationCard(TutorApplication app) {
        HBox card = new HBox(14);
        card.getStyleClass().add("application-card");
        card.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label(String.valueOf(app.getStudentUsername().charAt(0)).toUpperCase());
        avatar.getStyleClass().add("booking-avatar");

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nameLabel = new Label(app.getStudentUsername());
        nameLabel.getStyleClass().add("booking-student-name");
        Label catLabel  = new Label(app.getCategoryName());
        catLabel.getStyleClass().add("booking-subject");
        info.getChildren().addAll(nameLabel, catLabel);

        Button approveBtn = new Button("Approve");
        approveBtn.getStyleClass().add("accept-btn");
        Button rejectBtn  = new Button("Reject");
        rejectBtn.getStyleClass().add("decline-btn");

        approveBtn.setOnAction(e -> {
            approveBtn.setDisable(true);
            rejectBtn.setDisable(true);
            doEvaluate(app, ApplicationStatus.ACCEPTED, card, approveBtn, rejectBtn);
        });
        rejectBtn.setOnAction(e -> {
            approveBtn.setDisable(true);
            rejectBtn.setDisable(true);
            doEvaluate(app, ApplicationStatus.REJECTED, card, approveBtn, rejectBtn);
        });

        card.getChildren().addAll(avatar, info, approveBtn, rejectBtn);
        return card;
    }

    private void doEvaluate(TutorApplication app, ApplicationStatus status,
                            HBox card, Button approveBtn, Button rejectBtn) {
        String token = SceneManager.getInstance().getSessionToken();

        ApplicationReviewBean bean = new ApplicationReviewBean();
        bean.setApplicationId(app.getId());
        bean.setStatus(status);
        bean.setAdminNotes("");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                appController.evaluateApplication(bean, token);
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            applicationsList.getChildren().remove(card);
            int remaining = applicationsList.getChildren().size();
            pendingCountLabel.setText(remaining + " pending");
            if (remaining == 0) {
                Label empty = new Label("No pending applications.");
                empty.setStyle("-fx-text-fill: #8FAF9A; -fx-font-size: 13px;");
                applicationsList.getChildren().add(empty);
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            approveBtn.setDisable(false);
            rejectBtn.setDisable(false);
            LOGGER.warning("Evaluation failed: " + task.getException().getMessage());
        }));

        Thread t = new Thread(task, "admin-evaluate");
        t.setDaemon(true);
        t.start();
    }

    // ----------------------------------------------------------------
    // User Management tab
    // ----------------------------------------------------------------

    private void buildUsersList(List<Tutor> tutors) {
        usersList.getChildren().clear();
        if (tutors.isEmpty()) {
            Label empty = new Label("No tutors found.");
            empty.setStyle("-fx-text-fill: #8FAF9A; -fx-font-size: 13px;");
            usersList.getChildren().add(empty);
            return;
        }
        for (Tutor t : tutors) {
            usersList.getChildren().add(buildUserRow(t));
        }
    }

    private HBox buildUserRow(Tutor tutor) {
        HBox row = new HBox(14);
        row.getStyleClass().add("user-row");
        row.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label(String.valueOf(tutor.getName().charAt(0)).toUpperCase());
        avatar.getStyleClass().add("booking-avatar");

        VBox nameBox = new VBox(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        Label name     = new Label(tutor.getFullName());
        name.getStyleClass().add("booking-student-name");
        Label username = new Label("@" + tutor.getUsername());
        username.getStyleClass().add("lesson-meta");
        nameBox.getChildren().addAll(name, username);

        Label roleBadge = new Label("Tutor");
        roleBadge.getStyleClass().add("role-pill-tutor");

        row.getChildren().addAll(avatar, nameBox, roleBadge);
        return row;
    }

    private void filterUsers(String query) {
        String q = query.toLowerCase();
        List<Tutor> filtered = allTutors.stream()
                .filter(t -> t.getFullName().toLowerCase().contains(q)
                          || t.getUsername().toLowerCase().contains(q))
                .toList();
        buildUsersList(filtered);
    }
}
