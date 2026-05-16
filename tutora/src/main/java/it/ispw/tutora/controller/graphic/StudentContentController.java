package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.controller.application.SearchTutorController;
import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.dao.TutorDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Category;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.SceneManager;

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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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

    private String username;

    private static final DateTimeFormatter CARD_FMT =
            DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", java.util.Locale.ENGLISH);

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
    // Inizializzazione
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        String token = SceneManager.getInstance().getSessionToken();
        Session session = SessionManager.getInstance().getSession(token);
        this.username = session.getUser().getUsername();
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
        bookBtn.setOnAction(e -> openBookingDialog(tutor));
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

    private void openBookingDialog(Tutor tutor) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/book_tutor.fxml"));
            Parent root = loader.load();
            BookTutorGfxController ctrl = loader.getController();
            ctrl.initTutor(tutor);

            javafx.scene.Parent parentRoot = tutorGrid.getScene().getRoot();
            javafx.scene.effect.GaussianBlur blur = new javafx.scene.effect.GaussianBlur(10);
            javafx.scene.effect.ColorAdjust dim = new javafx.scene.effect.ColorAdjust();
            dim.setBrightness(-0.35);
            dim.setInput(blur);
            parentRoot.setEffect(dim);

            Stage stage = new Stage();
            stage.initOwner(tutorGrid.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.setMinWidth(560);
            stage.setMinHeight(500);
            stage.setOnHiding(ev -> parentRoot.setEffect(null));
            stage.show();
        } catch (java.io.IOException e) {
            LOGGER.warning("Cannot open booking dialog: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Lesson cards — dati reali da BookingDao
    // ----------------------------------------------------------------

    private void buildUpcomingLessons() {
        try {
            BookingDao dao = DaoFactory.getInstance().createBookingDao();
            List<Booking> bookings = dao.findByStudent(
                    DaoFactory.getInstance().getConnection(), username);
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            List<Booking> upcoming = bookings.stream()
                    .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                    .filter(b -> b.getLesson().getStartTime().isAfter(now))
                    .sorted(java.util.Comparator.comparing(b -> b.getLesson().getStartTime()))
                    .toList();
            if (upcoming.isEmpty()) {
                Label empty = new Label("No upcoming lessons.");
                empty.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:15px;");
                upcomingList.getChildren().add(empty);
            } else {
                for (Booking b : upcoming) upcomingList.getChildren().add(buildLessonCard(b, true));
            }
        } catch (DatabaseException e) {
            LOGGER.warning("Cannot load upcoming lessons: " + e.getMessage());
        }
    }

    /**
     * Ricarica la lista delle lezioni imminenti in background.
     * Chiamato da HomeGfxController dopo che un pagamento è stato confermato.
     */
    public void refreshUpcomingLessons() {
        upcomingList.getChildren().clear();

        Task<List<Booking>> task = new Task<>() {
            @Override
            protected List<Booking> call() throws Exception {
                BookingDao dao = DaoFactory.getInstance().createBookingDao();
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                return dao.findByStudent(DaoFactory.getInstance().getConnection(), username)
                        .stream()
                        .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                        .filter(b -> b.getLesson().getStartTime().isAfter(now))
                        .sorted(java.util.Comparator.comparing(b -> b.getLesson().getStartTime()))
                        .toList();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<Booking> upcoming = task.getValue();
            upcomingList.getChildren().clear();
            if (upcoming.isEmpty()) {
                Label empty = new Label("No upcoming lessons.");
                empty.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:15px;");
                upcomingList.getChildren().add(empty);
            } else {
                for (Booking b : upcoming) upcomingList.getChildren().add(buildLessonCard(b, true));
            }
        }));

        task.setOnFailed(e ->
                LOGGER.warning("Cannot refresh upcoming lessons: " + task.getException().getMessage()));

        Thread t = new Thread(task, "student-upcoming-refresh");
        t.setDaemon(true);
        t.start();
    }

    private void buildLessonHistory() {
        try {
            BookingDao dao = DaoFactory.getInstance().createBookingDao();
            List<Booking> bookings = dao.findByStudent(
                    DaoFactory.getInstance().getConnection(), username);
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            List<Booking> history = bookings.stream()
                    .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                    .filter(b -> !b.getLesson().getStartTime().isAfter(now))
                    .sorted(java.util.Comparator.comparing(
                            (Booking b) -> b.getLesson().getStartTime()).reversed())
                    .toList();
            if (history.isEmpty()) {
                Label empty = new Label("No past lessons.");
                empty.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:15px;");
                historyList.getChildren().add(empty);
            } else {
                for (Booking b : history) historyList.getChildren().add(buildLessonCard(b, false));
            }
        } catch (DatabaseException e) {
            LOGGER.warning("Cannot load lesson history: " + e.getMessage());
        }
    }

    private HBox buildLessonCard(Booking booking, boolean upcoming) {
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
            joinBtn.getStyleClass().add("book-btn");
            card.getChildren().add(joinBtn);
        }

        return card;
    }
}
