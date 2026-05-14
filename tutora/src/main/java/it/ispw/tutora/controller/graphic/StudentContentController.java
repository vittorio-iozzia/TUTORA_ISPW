package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.controller.application.SearchTutorController;
import it.ispw.tutora.dao.TutorDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Category;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.SceneManager;
import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Controller del frammento student_content.fxml.
 *
 * Carica i tutor dal datastore tramite {@link TutorDao}, popola la
 * griglia con ricerca e filtri per categoria (reali da {@link it.ispw.tutora.dao.CategoryDao}),
 * gestisce le animazioni di conteggio e hover sulle stat card.
 */
public class StudentContentController {

    private static final Logger LOGGER =
            Logger.getLogger(StudentContentController.class.getName());

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

    private final SearchTutorController searchController = new SearchTutorController();

    // ----------------------------------------------------------------
    // Unsplash portrait photos — mapped by known username
    // ----------------------------------------------------------------

    private static final Map<String, String> PHOTO_URLS = Map.of(
        "tutor_vitto",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=280&h=200&fit=crop&crop=faces"
    );

    private static final List<String> PORTRAIT_POOL = List.of(
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=280&h=200&fit=crop&crop=faces",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=280&h=200&fit=crop&crop=faces",
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=280&h=200&fit=crop&crop=faces",
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=280&h=200&fit=crop&crop=faces"
    );

    // ----------------------------------------------------------------
    // Demo lesson mock data
    // ----------------------------------------------------------------

    private record LessonMock(String subject, String tutorName, String datetime) {}

    private static final List<LessonMock> MOCK_UPCOMING = List.of(
        new LessonMock("Guitar Lesson", "tutor_vitto", "Tomorrow · 10:00 AM"),
        new LessonMock("Photography Basics","tutor_vitto", "Friday · 2:00 PM"),
        new LessonMock("Tennis Coaching", "tutor_vitto", "Saturday · 9:00 AM")
    );

    private static final List<LessonMock> MOCK_HISTORY = List.of(
        new LessonMock("Piano Lesson", "tutor_vitto", "3 days ago"),
        new LessonMock("Guitar Lesson", "tutor_vitto", "1 week ago")
    );

    // ----------------------------------------------------------------
    // Inizializzazione
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        String token = SceneManager.getInstance().getSessionToken();
        Session session = SessionManager.getInstance().getSession(token);
        welcomeTitle.setText("Welcome back, " + session.getUser().getName() + "!");
        loadHeroImage();

        allTutors     = loadTutors();
        allCategories = loadCategories();

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
    // DAO
    // ----------------------------------------------------------------

    private List<Tutor> loadTutors() {
        try {
            TutorDao dao = DaoFactory.getInstance().createTutorDao();
            return dao.selectAllTutors(DaoFactory.getInstance().getConnection());
        } catch (DatabaseException e) {
            LOGGER.warning("Cannot load tutors: " + e.getMessage());
            return List.of();
        }
    }

    private List<Category> loadCategories() {
        try {
            return searchController.loadCategories();
        } catch (DatabaseException e) {
            LOGGER.warning("Cannot load categories: " + e.getMessage());
            return List.of();
        }
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
            if (on) applyCategory(label);
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
        String q = query.toLowerCase();
        List<Tutor> filtered = allTutors.stream()
                .filter(t -> t.getFullName().toLowerCase().contains(q)
                          || t.getUsername().toLowerCase().contains(q)
                          || (t.getDescription() != null
                              && t.getDescription().toLowerCase().contains(q)))
                .toList();
        buildTutorGrid(filtered);
    }

    // ----------------------------------------------------------------
    // Tutor grid
    // ----------------------------------------------------------------

    private void buildTutorGrid(List<Tutor> tutors) {
        tutorGrid.getChildren().clear();
        if (tutors.isEmpty()) {
            Label empty = new Label("No tutors found.");
            empty.setStyle("-fx-text-fill: #8FAF9A; -fx-font-size: 13px;");
            tutorGrid.getChildren().add(empty);
            return;
        }
        int idx = 0;
        for (Tutor t : tutors) {
            tutorGrid.getChildren().add(buildTutorCard(t, idx++));
        }
    }

    private VBox buildTutorCard(Tutor tutor, int poolIndex) {
        VBox card = new VBox(0);
        card.getStyleClass().add("tutor-card");
        card.setPrefWidth(260);

        // Top half: cover photo with badge overlays
        StackPane photoPane = buildPhotoHalf(tutor, poolIndex);

        // Body section
        VBox body = new VBox(10);
        body.getStyleClass().add("tutor-card-body");

        Label name = new Label(tutor.getFullName());
        name.getStyleClass().add("tutor-name");

        Label desc = new Label(tutor.getDescription() != null
                ? truncate(tutor.getDescription(), 55)
                : tutor.getUsername());
        desc.getStyleClass().add("tutor-subject");
        desc.setWrapText(true);

        HBox ratingRow = new HBox(5);
        ratingRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon star = new FontIcon("fas-star");
        star.getStyleClass().add("star-icon");
        String ratingText = tutor.getRating() != null
                ? String.format("%.1f (%d reviews)", tutor.getRating(), tutor.getRatingCount())
                : "No reviews yet";
        Label ratingLabel = new Label(ratingText);
        ratingLabel.getStyleClass().add("tutor-rating");
        ratingRow.getChildren().addAll(star, ratingLabel);

        Label price = new Label("From €30/h");
        price.getStyleClass().add("tutor-price");

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_LEFT);
        Button profileBtn = new Button("Profile");
        profileBtn.getStyleClass().add("profile-btn");
        HBox.setHgrow(profileBtn, Priority.ALWAYS);
        profileBtn.setMaxWidth(Double.MAX_VALUE);
        Button bookBtn = new Button("Book");
        bookBtn.getStyleClass().add("book-btn");
        HBox.setHgrow(bookBtn, Priority.ALWAYS);
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setOnAction(e -> SceneManager.getInstance().showSearchTutor());
        buttons.getChildren().addAll(profileBtn, bookBtn);

        body.getChildren().addAll(name, desc, ratingRow, price, buttons);
        card.getChildren().addAll(photoPane, body);
        return card;
    }

    // ----------------------------------------------------------------
    // Photo half (card cover)
    // ----------------------------------------------------------------

    private StackPane buildPhotoHalf(Tutor tutor, int poolIndex) {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("tutor-card-photo-wrap");
        pane.setPrefHeight(160);
        pane.setMinHeight(160);
        pane.setMaxHeight(160);

        // Fallback letter shown while image loads
        Label initial = new Label(String.valueOf(tutor.getName().charAt(0)).toUpperCase());
        initial.getStyleClass().add("tutor-card-photo-initial");
        pane.getChildren().add(initial);

        // Cover image
        ImageView imgView = new ImageView();
        imgView.setFitWidth(260);
        imgView.setFitHeight(160);
        imgView.setPreserveRatio(false);
        imgView.setSmooth(true);
        pane.getChildren().add(imgView);

        // Clip taller than the pane so bottom arcs fall below visible area —
        // only the top-left and top-right corners appear rounded
        Rectangle photoClip = new Rectangle(260, 400);
        photoClip.setArcWidth(28);
        photoClip.setArcHeight(28);
        pane.setClip(photoClip);

        // Featured badge — top-left
        Label featured = new Label("⭐ Featured");
        featured.getStyleClass().add("tutor-card-featured-badge");
        StackPane.setAlignment(featured, Pos.TOP_LEFT);
        StackPane.setMargin(featured, new Insets(10, 0, 0, 10));

        // Mode badge — top-right (alternate per pool index)
        boolean isRemote = (poolIndex % 2 == 0);
        Label mode = new Label(isRemote ? "Online" : "In-Person");
        mode.getStyleClass().add(isRemote ? "tutor-card-online-badge" : "tutor-card-inperson-badge");
        StackPane.setAlignment(mode, Pos.TOP_RIGHT);
        StackPane.setMargin(mode, new Insets(10, 10, 0, 0));

        pane.getChildren().addAll(featured, mode);

        // Async load
        String photoUrl = PHOTO_URLS.getOrDefault(
                tutor.getUsername(),
                PORTRAIT_POOL.get(poolIndex % PORTRAIT_POOL.size()));
        Image img = new Image(photoUrl, 280, 200, false, true, true);
        img.progressProperty().addListener((obs, oldV, newV) -> {
            if (newV.doubleValue() >= 1.0 && !img.isError()) {
                imgView.setImage(img);
            }
        });

        return pane;
    }

    private String truncate(String text, int max) {
        return text.length() > max ? text.substring(0, max) + "…" : text;
    }

    // ----------------------------------------------------------------
    // Lesson cards
    // ----------------------------------------------------------------

    private void buildUpcomingLessons() {
        for (LessonMock lesson : MOCK_UPCOMING) {
            upcomingList.getChildren().add(buildLessonCard(lesson, true));
        }
    }

    private void buildLessonHistory() {
        for (LessonMock lesson : MOCK_HISTORY) {
            historyList.getChildren().add(buildLessonCard(lesson, false));
        }
    }

    private HBox buildLessonCard(LessonMock lesson, boolean upcoming) {
        HBox card = new HBox(16);
        card.getStyleClass().add("lesson-card");
        card.setAlignment(Pos.CENTER_LEFT);

        StackPane dot = new StackPane();
        dot.getStyleClass().add(upcoming ? "lesson-dot-upcoming" : "lesson-dot-completed");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label subject = new Label(lesson.subject());
        subject.getStyleClass().add("lesson-title");

        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);
        FontIcon userIcon = new FontIcon("fas-user");
        userIcon.getStyleClass().add("lesson-meta-icon");
        Label tutorLbl = new Label(lesson.tutorName());
        tutorLbl.getStyleClass().add("lesson-meta");
        FontIcon clockIcon = new FontIcon("fas-clock");
        clockIcon.getStyleClass().add("lesson-meta-icon");
        Label time = new Label(lesson.datetime());
        time.getStyleClass().add("lesson-meta");
        meta.getChildren().addAll(userIcon, tutorLbl, clockIcon, time);
        info.getChildren().addAll(subject, meta);

        if (upcoming) {
            Button joinBtn = new Button("Join");
            joinBtn.getStyleClass().add("join-btn");
            card.getChildren().addAll(dot, info, joinBtn);
        } else {
            Label badge = new Label("Completed");
            badge.getStyleClass().add("badge-completed");
            card.getChildren().addAll(dot, info, badge);
        }

        HBox.setMargin(card, new Insets(0));
        return card;
    }
}
