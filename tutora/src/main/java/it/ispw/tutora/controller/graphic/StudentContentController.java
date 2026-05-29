package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.controller.application.GetStudentLessonsController;
import it.ispw.tutora.controller.application.SearchTutorController;
import it.ispw.tutora.controller.graphic.util.TutorBrowseUtil;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Category;
import it.ispw.tutora.model.Tutor;

import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.SceneManager;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Controller del frammento student_content.fxml.
 *
 * Carica i tutor dal datastore tramite {@link it.ispw.tutora.dao.TutorDao}, popola la
 * griglia con ricerca e filtri per categoria (reali da {@link it.ispw.tutora.dao.CategoryDao}),
 * gestisce le animazioni di conteggio e hover sulle stat card.
 */
public class StudentContentController {

    private static final Logger LOGGER =
            Logger.getLogger(StudentContentController.class.getName());
    private static final String EMPTY_LESSON_STYLE = "-fx-text-fill:#9CA3AF;-fx-font-size:15px;";
    private static final String BOOK_BTN           = "book-btn";

    private final Map<String, Label> tutorRatingLabels = new HashMap<>();

    // ----------------------------------------------------------------
    // FXML – Welcome
    // ----------------------------------------------------------------

    @FXML private Label      welcomeTitle;
    @FXML private StackPane  heroPane;
    @FXML private ImageView  heroBgImageView;

    // ----------------------------------------------------------------
    // FXML – Stats
    // ----------------------------------------------------------------

    @FXML private HBox statCard1;
    @FXML private HBox statCard2;
    @FXML private HBox statCard3;
    @FXML private HBox statCard4;

    @FXML private Label totalLessonsLabel;
    @FXML private Label hoursLearnedLabel;
    @FXML private Label activeTutorsLabel;
    @FXML private Label avgRatingLabel;

    // ----------------------------------------------------------------
    // FXML – Explore tab
    // ----------------------------------------------------------------

    @FXML private TextField searchField;
    @FXML private HBox categoryPills;
    @FXML private FlowPane tutorGrid;

    // ----------------------------------------------------------------
    // FXML – Lesson tabs
    // ----------------------------------------------------------------

    @FXML private VBox upcomingList;
    @FXML private VBox historyList;

    // ----------------------------------------------------------------
    // State
    // ----------------------------------------------------------------

    private List<Tutor>    allTutors     = List.of();
    private List<Category> allCategories = List.of();

    /** username → lista nomi subcategory APPROVED (max 3), usata per card pills e ricerca */
    private Map<String, List<String>> expertiseNames = new HashMap<>();

    private final SearchTutorController      searchController      = new SearchTutorController();
    private final GetStudentLessonsController lessonsController     = new GetStudentLessonsController();

    private static final DateTimeFormatter CARD_FMT =
            DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", java.util.Locale.ENGLISH);

    // ----------------------------------------------------------------
    // Unsplash portrait photos — mapped by known username
    // ----------------------------------------------------------------

    private static final Map<String, String> PHOTO_URLS = Map.of(
        "tutor_vitto",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=280&h=200&fit=crop&crop=faces",
        "tutor_marco",
        "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=280&h=200&fit=crop&crop=faces",
        "tutor_sara",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=280&h=200&fit=crop&crop=faces",
        "tutor_luca",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=280&h=200&fit=crop&crop=faces"
    );


    // ----------------------------------------------------------------
    // Inizializzazione
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        String token = SceneManager.getInstance().getSessionToken();
        Session session = SessionManager.getInstance().getSession(token);
        if (session == null) {
            Platform.runLater(() -> SceneManager.getInstance().showLogin());
            return;
        }
        welcomeTitle.setText("Welcome back, " + session.getUser().getName() + "!");
        loadHeroImage();

        allTutors     = searchController.loadAllTutors();
        allCategories = TutorBrowseUtil.loadCategories(searchController, LOGGER);
        expertiseNames = searchController.loadExpertisesForTutors(allTutors);

        animateStat(totalLessonsLabel,24,"%.0f");
        animateStat(hoursLearnedLabel, 36,"%.0fh");
        animateStat(activeTutorsLabel, allTutors.size(),"%.0f");
        animateStat(avgRatingLabel, 4.8, "%.1f");

        addHoverLift(statCard1);
        addHoverLift(statCard2);
        addHoverLift(statCard3);
        addHoverLift(statCard4);

        buildCategoryPills();
        buildTutorGrid(allTutors);
        buildUpcomingLessons();
        buildLessonHistory();

        searchField.textProperty().addListener((obs, old, val) -> filterTutors(val));
    }

    // ----------------------------------------------------------------
    // Hero image
    // ----------------------------------------------------------------

    private void loadHeroImage() {
        // Clip the hero pane to rounded corners
        Rectangle heroClip = new Rectangle();
        heroClip.setArcWidth(24);
        heroClip.setArcHeight(24);
        heroClip.widthProperty().bind(heroPane.widthProperty());
        heroClip.heightProperty().bind(heroPane.heightProperty());
        heroPane.setClip(heroClip);

        // Background image fills the pane
        heroBgImageView.fitWidthProperty().bind(heroPane.widthProperty());
        heroBgImageView.fitHeightProperty().bind(heroPane.heightProperty());
        heroBgImageView.setPreserveRatio(false);
        heroBgImageView.setSmooth(true);

        Image img = new Image(
            "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=1200&h=300&fit=crop",
            1200, 300, false, true, true
        );
        img.progressProperty().addListener((obs, oldV, newV) -> {
            if (newV.doubleValue() >= 1.0 && !img.isError()) {
                heroBgImageView.setImage(img);
            }
        });
    }

    // ----------------------------------------------------------------
    // Stat hover animation (lift + scale)
    // ----------------------------------------------------------------

    private void addHoverLift(HBox card) {
        ScaleTransition up   = new ScaleTransition(Duration.millis(140), card);
        up.setToX(1.035); up.setToY(1.035);
        ScaleTransition down = new ScaleTransition(Duration.millis(140), card);
        down.setToX(1.0);  down.setToY(1.0);
        card.setOnMouseEntered(e -> { down.stop(); up.playFromStart(); });
        card.setOnMouseExited (e -> { up.stop(); down.playFromStart(); });
        card.setStyle("-fx-cursor: default;");
    }

    // ----------------------------------------------------------------
    // Stat count animation
    // ----------------------------------------------------------------

    private void animateStat(Label label, double target, String format) {
        DoubleProperty prop = new SimpleDoubleProperty(0);
        prop.addListener((obs, o, n) ->
                label.setText(String.format(format, n.doubleValue())));
        new Timeline(new KeyFrame(Duration.millis(900),
                new KeyValue(prop, target, Interpolator.EASE_OUT))).play();
    }

    // ----------------------------------------------------------------
    // Category pills (real categories from DAO)
    // ----------------------------------------------------------------

    private void buildCategoryPills() {
        ToggleGroup group = new ToggleGroup();
        addPill(group, "All", true);
        for (Category cat : allCategories) {
            addPill(group, cat.getName(), false);
        }
        group.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) oldT.setSelected(true);
        });
    }

    private void addPill(ToggleGroup group, String label, boolean selected) {
        ToggleButton pill = new ToggleButton(label);
        pill.setToggleGroup(group);
        pill.getStyleClass().add("category-pill");
        pill.setSelected(selected);
        pill.selectedProperty().addListener((obs, was, on) -> {
            if (Boolean.TRUE.equals(on)) applyCategory(label);
        });
        categoryPills.getChildren().add(pill);
    }

    private void applyCategory(String category) {
        if (category.equals("All")) {
            buildTutorGrid(allTutors);
        } else {
            buildTutorGrid(searchController.filterByCategory(allTutors, category));
        }
    }

    private void filterTutors(String query) {
        buildTutorGrid(searchController.filterByQuery(allTutors, expertiseNames, query));
    }

    // ----------------------------------------------------------------
    // Tutor grid
    // ----------------------------------------------------------------

    private void buildTutorGrid(List<Tutor> tutors) {
        tutorGrid.getChildren().clear();
        if (tutors.isEmpty()) {
            tutorGrid.getChildren().add(buildEmptyState());
            return;
        }
        int idx = 0;
        for (Tutor t : tutors) {
            tutorGrid.getChildren().add(buildTutorCard(t, idx++));
        }
    }

    private VBox buildEmptyState() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(48));

        StackPane iconCircle = new StackPane();
        iconCircle.setStyle(
            "-fx-background-color: #EEF2FF;" +
            "-fx-background-radius: 44;" +
            "-fx-min-width: 88; -fx-max-width: 88;" +
            "-fx-min-height: 88; -fx-max-height: 88;");
        iconCircle.getChildren().add(TutorBrowseUtil.loadTwemoji("1f50d", 44));

        Label title = new Label("No tutors found");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #6B7280;");
        Label sub = new Label("Try a different search term or category");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #9CA3AF;");

        box.getChildren().addAll(iconCircle, title, sub);
        return box;
    }

    private VBox buildTutorCard(Tutor tutor, int poolIndex) {
        VBox card = new VBox(0);
        card.getStyleClass().add("tutor-card");
        card.setPrefWidth(260);

        // Top half: cover photo with badge overlays
        StackPane photoPane = TutorBrowseUtil.buildPhotoHalf(tutor, poolIndex, 260, PHOTO_URLS);

        // Body section
        VBox body = new VBox(10);
        body.getStyleClass().add("tutor-card-body");

        Label name = new Label(tutor.getFullName());
        name.getStyleClass().add("tutor-name");

        // Descrizione: bio truncata, oppure nomi expertise come fallback
        List<String> tags = expertiseNames.getOrDefault(tutor.getUsername(), List.of());
        String descText;
        if (tutor.getDescription() != null && !tutor.getDescription().isBlank()) {
            descText = TutorBrowseUtil.truncate(tutor.getDescription(), 60);
        } else if (!tags.isEmpty()) {
            descText = String.join(" · ", tags);
        } else {
            descText = "Tutor";
        }
        Label desc = new Label(descText);
        desc.getStyleClass().add("tutor-subject");
        desc.setWrapText(true);

        // Expertise tag pills
        HBox pillBox = new HBox(6);
        pillBox.setAlignment(Pos.CENTER_LEFT);
        pillBox.setPadding(new Insets(2, 0, 0, 0));
        for (String tag : tags) {
            Label pill = new Label(tag);
            pill.getStyleClass().add("interest-pill");
            pillBox.getChildren().add(pill);
        }

        HBox ratingRow = new HBox(5);
        ratingRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon star = new FontIcon("fas-star");
        star.getStyleClass().add("star-icon");
        String ratingText = tutor.getRating() != null
                ? String.format("%.1f (%d reviews)", tutor.getRating(), tutor.getRatingCount())
                : "No reviews yet";
        Label ratingLabel = new Label(ratingText);
        ratingLabel.getStyleClass().add("tutor-rating");
        tutorRatingLabels.put(tutor.getUsername(), ratingLabel);
        ratingRow.getChildren().addAll(star, ratingLabel);

        Label price = new Label("From €30/h");
        price.getStyleClass().add("tutor-price");

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_LEFT);
        Button profileBtn = new Button("Profile");
        profileBtn.getStyleClass().add("profile-btn");
        HBox.setHgrow(profileBtn, Priority.ALWAYS);
        profileBtn.setMaxWidth(Double.MAX_VALUE);
        profileBtn.setOnAction(e -> HomeGfxController.navigateToTutorPublicProfile(tutor));
        Button bookBtn = new Button("Book");
        bookBtn.getStyleClass().add(BOOK_BTN);
        HBox.setHgrow(bookBtn, Priority.ALWAYS);
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setOnAction(e -> TutorBrowseUtil.openBookingDialog(tutor, tutorGrid, LOGGER));
        buttons.getChildren().addAll(profileBtn, bookBtn);

        body.getChildren().addAll(name, desc, pillBox, ratingRow, price, buttons);
        card.getChildren().addAll(photoPane, body);
        return card;
    }

    // ----------------------------------------------------------------
    // Lesson cards — dati reali da BookingDao
    // ----------------------------------------------------------------

    private void buildUpcomingLessons() {
        String token = SceneManager.getInstance().getSessionToken();
        List<Booking> upcoming = lessonsController.loadUpcomingLessons(token);
        if (upcoming.isEmpty()) {
            Label empty = new Label("No upcoming lessons.");
            empty.setStyle(EMPTY_LESSON_STYLE);
            upcomingList.getChildren().add(empty);
        } else {
            for (Booking b : upcoming) upcomingList.getChildren().add(buildLessonCard(b, true));
        }
    }

    /**
     * Ricarica la lista delle lezioni imminenti in background.
     * Chiamato da HomeGfxController dopo che un pagamento è stato confermato.
     */
    public void refreshUpcomingLessons() {
        upcomingList.getChildren().clear();
        String token = SceneManager.getInstance().getSessionToken();

        Task<List<Booking>> task = new Task<>() {
            @Override
            protected List<Booking> call() {
                return lessonsController.loadUpcomingLessons(token);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<Booking> upcoming = task.getValue();
            upcomingList.getChildren().clear();
            if (upcoming.isEmpty()) {
                Label empty = new Label("No upcoming lessons.");
                empty.setStyle(EMPTY_LESSON_STYLE);
                upcomingList.getChildren().add(empty);
            } else {
                for (Booking b : upcoming) upcomingList.getChildren().add(buildLessonCard(b, true));
            }
        }));

        task.setOnFailed(e ->
                LOGGER.warning("Cannot refresh upcoming lessons: " + task.getException().getMessage()));

        Thread thread = new Thread(task, "student-upcoming-refresh");
        thread.setDaemon(true);
        thread.start();
    }

    private void buildLessonHistory() {
        String token = SceneManager.getInstance().getSessionToken();
        List<Booking> history    = lessonsController.loadLessonHistory(token);
        Set<Integer>  reviewedIds = lessonsController.getReviewedBookingIds(history, token);

        if (history.isEmpty()) {
            Label empty = new Label("No past lessons.");
            empty.setStyle(EMPTY_LESSON_STYLE);
            historyList.getChildren().add(empty);
        } else {
            for (Booking b : history) {
                historyList.getChildren().add(
                        buildLessonCard(b, false, reviewedIds.contains(b.getId())));
            }
        }
    }

    private HBox buildLessonCard(Booking booking, boolean upcoming) {
        return buildLessonCard(booking, upcoming, false);
    }

    private HBox buildLessonCard(Booking booking, boolean upcoming, boolean isReviewed) {
        String subject  = booking.getLesson().getExpertise().getSubcategory().getName();
        String tutorName = booking.getLesson().getExpertise().getTutor().getFullName();
        if (tutorName == null || tutorName.isBlank())
            tutorName = booking.getLesson().getExpertise().getTutor().getUsername();
        String datetime = booking.getLesson().getStartTime().format(CARD_FMT);

        HBox card = new HBox(16);
        card.getStyleClass().addAll("lesson-card", upcoming ? "lesson-card-upcoming" : "lesson-card-completed");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(6);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Title row: subject + status badge
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label subjectLbl = new Label(subject);
        subjectLbl.getStyleClass().add("lesson-title");
        HBox.setHgrow(subjectLbl, Priority.ALWAYS);
        Label statusBadge = new Label(upcoming ? "Upcoming" : "Completed");
        statusBadge.getStyleClass().add(upcoming ? "badge-upcoming" : "badge-completed");
        titleRow.getChildren().addAll(subjectLbl, statusBadge);

        // Tutor row
        HBox tutorRow = new HBox(6);
        tutorRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon userIcon = new FontIcon("fas-user");
        userIcon.getStyleClass().add("lesson-meta-icon");
        Label tutorLbl = new Label(tutorName);
        tutorLbl.getStyleClass().add("lesson-meta");
        tutorRow.getChildren().addAll(userIcon, tutorLbl);

        // Time row
        HBox timeRow = new HBox(6);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon clockIcon = new FontIcon("fas-clock");
        clockIcon.getStyleClass().add("lesson-meta-icon");
        Label time = new Label(datetime);
        time.getStyleClass().add("lesson-meta");
        timeRow.getChildren().addAll(clockIcon, time);

        info.getChildren().addAll(titleRow, tutorRow, timeRow);
        card.getChildren().add(info);

        if (upcoming) {
            Button joinBtn = new Button("Join");
            joinBtn.getStyleClass().add(BOOK_BTN);
            card.getChildren().add(joinBtn);
        } else {
            Button reviewBtn = new Button(isReviewed ? "Reviewed ✓" : "Review");
            reviewBtn.getStyleClass().add(isReviewed ? "lesson-reviewed-btn" : BOOK_BTN);
            reviewBtn.setDisable(isReviewed);
            if (!isReviewed) {
                reviewBtn.setOnAction(e -> openReviewDialog(booking, reviewBtn));
            }
            card.getChildren().add(reviewBtn);
        }

        return card;
    }

    private void openReviewDialog(Booking booking, Button reviewBtn) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/review_dialog.fxml"));
            Parent root = loader.load();
            ReviewGfxController ctrl = loader.getController();
            ctrl.initBooking(booking, () -> {
                reviewBtn.setText("Reviewed ✓");
                reviewBtn.getStyleClass().remove(BOOK_BTN);
                reviewBtn.getStyleClass().add("lesson-reviewed-btn");
                reviewBtn.setDisable(true);
                reviewBtn.setOnAction(null);

                Tutor t = booking.getLesson().getExpertise().getTutor();
                Label lbl = tutorRatingLabels.get(t.getUsername());
                if (lbl != null) {
                    String updated = t.getRating() != null
                            ? String.format("%.1f (%d reviews)", t.getRating(), t.getRatingCount())
                            : "No reviews yet";
                    lbl.setText(updated);
                }
            });

            Parent parentRoot = reviewBtn.getScene().getRoot();
            GaussianBlur blur = new GaussianBlur(10);
            ColorAdjust dim  = new ColorAdjust();
            dim.setBrightness(-0.35);
            dim.setInput(blur);
            parentRoot.setEffect(dim);

            Stage stage = new Stage();
            stage.initOwner(reviewBtn.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.setMinWidth(460);
            stage.setWidth(500);
            stage.setHeight(500);
            stage.setOnHiding(ev -> parentRoot.setEffect(null));
            stage.show();
        } catch (IOException e) {
            LOGGER.warning("Cannot open review dialog: " + e.getMessage());
        }
    }
}
