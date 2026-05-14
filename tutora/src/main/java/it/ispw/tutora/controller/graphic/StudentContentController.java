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

    @FXML private Label welcomeTitle;

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
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=104&h=104&fit=crop&crop=face"
    );

    private static final List<String> PORTRAIT_POOL = List.of(
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=104&h=104&fit=crop&crop=face",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=104&h=104&fit=crop&crop=face",
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=104&h=104&fit=crop&crop=face",
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=104&h=104&fit=crop&crop=face"
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
        VBox card = new VBox(16);
        card.getStyleClass().add("tutor-card");
        card.setPrefWidth(300);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane photoPane = buildPhotoPane(tutor, poolIndex);

        VBox nameBox = new VBox(3);
        Label name = new Label(tutor.getFullName());
        name.getStyleClass().add("tutor-name");
        Label desc = new Label(tutor.getDescription() != null
                ? truncate(tutor.getDescription(), 40)
                : tutor.getUsername());
        desc.getStyleClass().add("tutor-subject");
        desc.setWrapText(true);
        nameBox.getChildren().addAll(name, desc);
        header.getChildren().addAll(photoPane, nameBox);

        HBox ratingRow = new HBox(6);
        ratingRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon star = new FontIcon("fas-star");
        star.getStyleClass().add("star-icon");
        String ratingText = tutor.getRating() != null
                ? String.format("%.1f (%d reviews)", tutor.getRating(), tutor.getRatingCount())
                : "No reviews yet";
        Label ratingLabel = new Label(ratingText);
        ratingLabel.getStyleClass().add("tutor-rating");
        ratingRow.getChildren().addAll(star, ratingLabel);

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        Label contact = new Label("Contact");
        contact.getStyleClass().add("tutor-rate");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button bookBtn = new Button("Book");
        bookBtn.getStyleClass().add("book-btn");
        bookBtn.setOnAction(e -> SceneManager.getInstance().showSearchTutor());
        footer.getChildren().addAll(contact, spacer, bookBtn);

        card.getChildren().addAll(header, ratingRow, footer);
        return card;
    }

    // ----------------------------------------------------------------
    // Photo pane
    // ----------------------------------------------------------------

    private StackPane buildPhotoPane(Tutor tutor, int poolIndex) {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("tutor-avatar");

        String photoUrl = PHOTO_URLS.getOrDefault(
                tutor.getUsername(),
                PORTRAIT_POOL.get(poolIndex % PORTRAIT_POOL.size()));

        ImageView imgView = new ImageView();
        imgView.setFitWidth(52);
        imgView.setFitHeight(52);
        imgView.setPreserveRatio(false);
        imgView.setSmooth(true);
        imgView.setClip(buildCircleClip());

        Label initial = new Label(String.valueOf(tutor.getName().charAt(0)).toUpperCase());
        initial.getStyleClass().add("tutor-avatar-letter");
        pane.getChildren().add(initial);

        Image img = new Image(photoUrl, 104, 104, true, true, true);
        img.progressProperty().addListener((obs, oldV, newV) -> {
            if (newV.doubleValue() >= 1.0 && !img.isError()) {
                imgView.setImage(img);
                pane.getChildren().setAll(imgView);
            }
        });

        return pane;
    }

    private javafx.scene.shape.Circle buildCircleClip() {
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(26);
        clip.setCenterX(26);
        clip.setCenterY(26);
        return clip;
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
