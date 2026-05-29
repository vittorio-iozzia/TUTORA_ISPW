package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.bean.TutorApplicationBean;
import it.ispw.tutora.controller.application.ApplyToBecomeATutorController;
import it.ispw.tutora.controller.application.SearchTutorController;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.SceneManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.StringConverter;
import java.util.List;
import java.util.logging.Logger;

public class AdminContentController {

    private static final Logger LOGGER = Logger.getLogger(AdminContentController.class.getName());
    private static final String EMPTY_LABEL_STYLE = "-fx-text-fill: #8FAF9A; -fx-font-size: 13px;";
    private static final String TOOLTIP_STYLE     = "-fx-font-size:12px;";

    private final ApplyToBecomeATutorController appController = new ApplyToBecomeATutorController();
    private final SearchTutorController searchController = new SearchTutorController();

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
    @FXML private VBox usersList;

    // Analytics tab
    @FXML private AreaChart<String, Number> revenueChart;
    @FXML private BarChart<String, Number>  bookingsChart;
    @FXML private PieChart                  categoryChart;

    // State
    private List<TutorApplicationBean> pendingApplications = List.of();
    private List<Tutor> allTutors = List.of();

    // ----------------------------------------------------------------
    // Init
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        String token    = SceneManager.getInstance().getSessionToken();
        Session session = SessionManager.getInstance().getSession(token);
        welcomeTitle.setText("Welcome, " + session.getUser().getName() + "!");

        try {
            pendingApplications = appController.loadPendingApplications(token);
        } catch (Exception e) {
            LOGGER.warning("Cannot load applications: " + e.getMessage());
        }
        allTutors = searchController.loadAllTutors();

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
        buildRevenueChart();
        buildBookingsChart();
        buildCategoryChart();

        userSearchField.textProperty().addListener((obs, old, val) -> filterUsers(val));

        // Quando l'admin valuta un'application dal pannello notifiche, aggiorna la lista qui.
        NotificationGfxController.setApplicationEvaluatedCallback(this::reloadApplicationsList);
    }

    private void reloadApplicationsList() {
        String reloadToken = SceneManager.getInstance().getSessionToken();
        Task<List<TutorApplicationBean>> task = new Task<>() {
            @Override
            protected List<TutorApplicationBean> call() {
                try {
                    return appController.loadPendingApplications(reloadToken);
                } catch (Exception e) {
                    LOGGER.warning("Cannot reload applications: " + e.getMessage());
                    return List.of();
                }
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            pendingApplications = task.getValue();
            applicationsList.getChildren().clear();
            buildApplicationsList();
        }));
        Thread t = new Thread(task, "admin-reload-apps");
        t.setDaemon(true);
        t.start();
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
            empty.setStyle(EMPTY_LABEL_STYLE);
            applicationsList.getChildren().add(empty);
            return;
        }
        for (TutorApplicationBean app : pendingApplications) {
            applicationsList.getChildren().add(buildApplicationCard(app));
        }
    }

    private HBox buildApplicationCard(TutorApplicationBean app) {
        HBox card = new HBox(16);
        card.getStyleClass().add("application-card");
        card.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label(String.valueOf(app.getStudentUsername().charAt(0)).toUpperCase());
        avatar.getStyleClass().add("admin-app-avatar");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nameLabel = new Label(app.getStudentUsername());
        nameLabel.getStyleClass().add("admin-app-name");
        String catText = app.getCategoryName()
                + (app.getSubcategoryName() != null && !app.getSubcategoryName().isBlank()
                   ? " · " + app.getSubcategoryName() : "");
        Label catLabel = new Label(catText);
        catLabel.getStyleClass().add("admin-app-category");
        info.getChildren().addAll(nameLabel, catLabel);

        Button viewBtn = new Button("View Application");
        viewBtn.getStyleClass().add("admin-approve-btn");

        viewBtn.setOnAction(e -> openApplicationReviewDialog(app, card));

        card.getChildren().addAll(avatar, info, viewBtn);
        return card;
    }

    private void openApplicationReviewDialog(TutorApplicationBean app, HBox card) {
        String token = SceneManager.getInstance().getSessionToken();

        Task<TutorApplicationBean> task = new Task<>() {
            @Override
            protected TutorApplicationBean call() throws Exception {
                return appController.loadApplicationDetail(app.getApplicationId(), token);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/application_review.fxml"));
                Parent root = loader.load();
                ApplicationReviewGfxController ctrl = loader.getController();
                ctrl.initApplication(task.getValue());
                ctrl.setOnEvaluated(() -> {
                    HomeGfxController.refreshBadgeStatic();
                    reloadApplicationsList();
                });

                Parent parentRoot = card.getScene().getRoot();
                GaussianBlur blur = new GaussianBlur(10);
                ColorAdjust dim  = new ColorAdjust();
                dim.setBrightness(-0.35);
                dim.setInput(blur);
                parentRoot.setEffect(dim);

                Stage stage = new Stage();
                stage.initOwner(card.getScene().getWindow());
                stage.initModality(Modality.WINDOW_MODAL);
                stage.initStyle(StageStyle.TRANSPARENT);
                Scene scene = new Scene(root);
                scene.setFill(Color.TRANSPARENT);
                stage.setScene(scene);
                stage.setMinWidth(560);
                stage.setOnHiding(ev -> parentRoot.setEffect(null));
                stage.show();
            } catch (Exception ex) {
                LOGGER.warning("Cannot open application review: " + ex.getMessage());
            }
        }));

        task.setOnFailed(e -> LOGGER.warning("Cannot load application detail: " +
                (task.getException() != null ? task.getException().getMessage() : "error")));

        Thread t = new Thread(task, "admin-load-app");
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
            empty.setStyle(EMPTY_LABEL_STYLE);
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
        avatar.getStyleClass().add("admin-app-avatar");

        VBox nameBox = new VBox(4);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        Label name     = new Label(tutor.getFullName());
        name.getStyleClass().add("admin-app-name");
        Label username = new Label("@" + tutor.getUsername());
        username.getStyleClass().add("admin-app-category");
        nameBox.getChildren().addAll(name, username);

        Label roleBadge = new Label("Tutor");
        roleBadge.getStyleClass().add("role-pill-tutor");

        row.getChildren().addAll(avatar, nameBox, roleBadge);
        return row;
    }

    private void filterUsers(String query) {
        buildUsersList(searchController.filterByQuery(allTutors, query));
    }

    // ----------------------------------------------------------------
    // Analytics charts
    // ----------------------------------------------------------------

    private void buildRevenueChart() {
        NumberAxis yAxis = (NumberAxis) revenueChart.getYAxis();
        yAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override public String toString(Number n) {
                return "€" + (int) (n.doubleValue() / 1000) + "k";
            }
            @Override public Number fromString(String s) { return 0; }
        });

        String[]  months  = {"Dec", "Jan", "Feb", "Mar", "Apr", "May"};
        double[]  revenue = {9_800, 11_200, 10_500, 13_400, 15_600, 17_890};

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 0; i < months.length; i++) {
            XYChart.Data<String, Number> d = new XYChart.Data<>(months[i], revenue[i]);
            series.getData().add(d);
            d.nodeProperty().addListener((obs, old, node) -> {
                if (node == null) return;
                String label = "€" + String.format("%.0f", d.getYValue().doubleValue());
                Tooltip tip = new Tooltip(label);
                tip.setStyle(TOOLTIP_STYLE);
                Tooltip.install(node, tip);
                node.setOnMouseEntered(e -> node.setStyle("-fx-opacity:0.75;-fx-cursor:hand;"));
                node.setOnMouseExited (e -> node.setStyle("-fx-opacity:1.0;"));
            });
        }
        revenueChart.getData().add(series);
    }

    private void buildBookingsChart() {
        String[] months   = {"Dec", "Jan", "Feb", "Mar", "Apr", "May"};
        int[]    bookings = {42, 58, 51, 74, 89, 103};

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 0; i < months.length; i++) {
            XYChart.Data<String, Number> d = new XYChart.Data<>(months[i], bookings[i]);
            series.getData().add(d);
            d.nodeProperty().addListener((obs, old, node) -> {
                if (node == null) return;
                Tooltip tip = new Tooltip(d.getYValue() + " bookings");
                tip.setStyle(TOOLTIP_STYLE);
                Tooltip.install(node, tip);
                node.setOnMouseEntered(e -> node.setStyle("-fx-bar-fill: #1B5E35;-fx-cursor:hand;"));
                node.setOnMouseExited (e -> node.setStyle("-fx-bar-fill: #2E7D50;"));
            });
        }
        bookingsChart.getData().add(series);
    }

    private void buildCategoryChart() {
        String[] categories = {"Music", "Math", "Languages", "Science", "Sport", "Other"};
        double[] values     = {28, 22, 18, 14, 10, 8};

        for (int i = 0; i < categories.length; i++) {
            PieChart.Data slice = new PieChart.Data(categories[i], values[i]);
            categoryChart.getData().add(slice);
            slice.nodeProperty().addListener((obs, old, node) -> {
                if (node == null) return;
                String label = slice.getName() + " — " + (int) slice.getPieValue() + " tutors";
                Tooltip tip = new Tooltip(label);
                tip.setStyle(TOOLTIP_STYLE);
                Tooltip.install(node, tip);
                node.setOnMouseEntered(e -> node.setStyle("-fx-opacity:0.8;-fx-cursor:hand;"));
                node.setOnMouseExited (e -> node.setStyle("-fx-opacity:1.0;"));
            });
        }
    }
}
