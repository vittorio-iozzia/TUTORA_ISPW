package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.dao.LessonDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Lesson;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.SceneManager;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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

public class TutorContentController {

    private static final Logger LOGGER =
            Logger.getLogger(TutorContentController.class.getName());
    private static final String LESSON_META_ICON = "lesson-meta-icon";
    private static final String LESSON_META = "lesson-meta";

    private static final DateTimeFormatter CARD_FMT =
            DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", java.util.Locale.ENGLISH);

    private String tutorUsername;

    // Hero
    @FXML private StackPane heroPane;
    @FXML private ImageView heroBgImageView;

    // Stats
    @FXML private HBox statCard1;
    @FXML private HBox statCard2;
    @FXML private HBox statCard3;
    @FXML private HBox statCard4;
    @FXML private Label welcomeTitle;
    @FXML private Label totalStudentsLabel;
    @FXML private Label lessonsMonthLabel;
    @FXML private Label earningsLabel;
    @FXML private Label avgRatingLabel;

    // Tabs
    @FXML private VBox calendarContainer;
    @FXML private VBox upcomingLessonsList;

    // Right column
    @FXML private Label pendingBadge;
    @FXML private VBox  bookingRequestsList;
    @FXML private VBox  thisWeekList;

    // Calendar state
    private YearMonth currentMonth    = YearMonth.now();
    private LocalDate selectedCalendarDate = null;
    private Set<LocalDate> lessonDates = new HashSet<>();

    // ----------------------------------------------------------------
    // Demo data
    // ----------------------------------------------------------------

    private record BookingMock(String student, String subject, String date, String time, boolean pending) {}

    private static final List<BookingMock> MOCK_BOOKINGS = List.of(
        new BookingMock("Alex Thompson","Calculus II",   "Jan 15","3:00 PM", true),
        new BookingMock("Emma Wilson",  "Linear Algebra","Jan 16","10:00 AM",true),
        new BookingMock("Michael Brown","Statistics",    "Jan 17","2:00 PM", false)
    );

    // ----------------------------------------------------------------
    // Init
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        String token   = SceneManager.getInstance().getSessionToken();
        Session session = SessionManager.getInstance().getSession(token);
        this.tutorUsername = session.getUser().getUsername();
        welcomeTitle.setText("Welcome, " + session.getUser().getName() + "!");

        loadHeroImage();

        animateStat(totalStudentsLabel, 48, "%.0f");
        animateStat(lessonsMonthLabel, 32, "%.0f");
        animateStat(earningsLabel, 1440, "€%.0f");
        animateStat(avgRatingLabel, 4.9, "%.1f");

        addHoverLift(statCard1);
        addHoverLift(statCard2);
        addHoverLift(statCard3);
        addHoverLift(statCard4);

        loadLessonDates();
        buildUpcomingLessons();
        buildBookingRequests();
        buildThisWeek();

        if (SessionManager.getInstance().consumeNewlyPromotedTutor(tutorUsername)) {
            Platform.runLater(this::showWelcomePopup);
        }
    }

    // ----------------------------------------------------------------
    // Hero image
    // ----------------------------------------------------------------

    private void loadHeroImage() {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        clip.widthProperty().bind(heroPane.widthProperty());
        clip.heightProperty().bind(heroPane.heightProperty());
        heroPane.setClip(clip);

        heroBgImageView.fitWidthProperty().bind(heroPane.widthProperty());
        heroBgImageView.fitHeightProperty().bind(heroPane.heightProperty());
        heroBgImageView.setPreserveRatio(false);
        heroBgImageView.setSmooth(true);

        Image img = new Image(
            "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=1200&h=300&fit=crop&crop=center",
            1200, 300, false, true, true
        );
        img.progressProperty().addListener((obs, oldV, newV) -> {
            if (newV.doubleValue() >= 1.0 && !img.isError()) {
                heroBgImageView.setImage(img);
            }
        });
    }

    // ----------------------------------------------------------------
    // Calendar
    // ----------------------------------------------------------------

    private void loadLessonDates() {
        Task<Set<LocalDate>> task = new Task<>() {
            @Override
            protected Set<LocalDate> call() throws Exception {
                LessonDao dao = DaoFactory.getInstance().createLessonDao();
                return dao.findByTutor(DaoFactory.getInstance().getConnection(), tutorUsername)
                        .stream()
                        .map(l -> l.getStartTime().toLocalDate())
                        .collect(Collectors.toSet());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            lessonDates = task.getValue();
            buildCalendar();
        }));
        task.setOnFailed(e -> {
            LOGGER.warning("Cannot load lesson dates: " + task.getException().getMessage());
            buildCalendar();
        });
        Thread t = new Thread(task, "load-lesson-dates");
        t.setDaemon(true);
        t.start();
    }

    private void buildCalendar() {
        calendarContainer.getChildren().clear();

        // Header: prev arrow — month/year label — next arrow
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Button prevBtn = new Button("‹");
        prevBtn.getStyleClass().add("cal-nav-btn");
        prevBtn.setOnAction(e -> { currentMonth = currentMonth.minusMonths(1); buildCalendar(); });

        Label monthLabel = new Label(
                currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)));
        monthLabel.getStyleClass().add("cal-month-label");
        HBox.setHgrow(monthLabel, Priority.ALWAYS);
        monthLabel.setMaxWidth(Double.MAX_VALUE);

        Button nextBtn = new Button("›");
        nextBtn.getStyleClass().add("cal-nav-btn");
        nextBtn.setOnAction(e -> { currentMonth = currentMonth.plusMonths(1); buildCalendar(); });

        header.getChildren().addAll(prevBtn, monthLabel, nextBtn);

        // Day-of-week header row
        String[] DOW = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        GridPane dowRow = new GridPane();
        dowRow.setHgap(6);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            cc.setFillWidth(true);
            cc.setHgrow(Priority.ALWAYS);
            dowRow.getColumnConstraints().add(cc);
            Label d = new Label(DOW[i]);
            d.getStyleClass().add("cal-day-header");
            d.setMaxWidth(Double.MAX_VALUE);
            d.setAlignment(Pos.CENTER);
            dowRow.add(d, i, 0);
        }

        // Days grid
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            cc.setFillWidth(true);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        LocalDate today     = LocalDate.now();
        LocalDate firstDay  = currentMonth.atDay(1);
        LocalDate lastDay   = currentMonth.atEndOfMonth();
        int startCol = firstDay.getDayOfWeek().getValue() - 1; // Mon=0, Sun=6

        int col = startCol;
        int row = 0;
        LocalDate day = firstDay;

        while (!day.isAfter(lastDay)) {
            final LocalDate d = day;
            Button dayBtn = new Button(String.valueOf(day.getDayOfMonth()));
            dayBtn.setMaxWidth(Double.MAX_VALUE);
            GridPane.setFillWidth(dayBtn, true);

            boolean isPast     = d.isBefore(today);
            boolean isToday    = d.equals(today);
            boolean isSelected = d.equals(selectedCalendarDate);
            boolean hasLesson  = lessonDates.contains(d);

            if (isSelected) {
                dayBtn.getStyleClass().add("cal-day-selected");
            } else if (!isPast && hasLesson) {
                dayBtn.getStyleClass().add("cal-day-has-lesson");
            } else if (isToday) {
                dayBtn.getStyleClass().add("cal-day-today");
            } else if (isPast) {
                dayBtn.getStyleClass().add("cal-day-past");
                dayBtn.setDisable(true);
            } else {
                dayBtn.getStyleClass().add("cal-day");
            }

            if (!isPast) {
                dayBtn.setOnAction(e -> {
                    selectedCalendarDate = d;
                    buildCalendar();
                    openCreateLessonDialog(d);
                });
            }

            grid.add(dayBtn, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
            day = day.plusDays(1);
        }

        VBox calCard = new VBox(12);
        calCard.getStyleClass().add("cal-card");
        calCard.getChildren().addAll(header, dowRow, grid);
        calendarContainer.getChildren().add(calCard);
    }

    private void openCreateLessonDialog(LocalDate date) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/create_lesson.fxml"));
            Parent root = loader.load();
            CreateLessonGfxController ctrl = loader.getController();
            ctrl.initDate(date);
            ctrl.setOnLessonCreated(() -> {
                selectedCalendarDate = null;
                loadLessonDates();
            });

            Parent parentRoot = heroPane.getScene().getRoot();
            GaussianBlur blur = new GaussianBlur(10);
            ColorAdjust dim   = new ColorAdjust();
            dim.setBrightness(-0.35);
            dim.setInput(blur);
            parentRoot.setEffect(dim);

            Stage stage = new Stage();
            stage.initOwner(heroPane.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.setMinWidth(500);
            stage.setMinHeight(440);
            stage.setOnHiding(e -> parentRoot.setEffect(null));
            stage.show();
        } catch (IOException e) {
            LOGGER.warning("Cannot open create lesson dialog: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Welcome popup
    // ----------------------------------------------------------------

    private void showWelcomePopup() {
        javafx.scene.Scene scene = welcomeTitle.getScene();
        if (!(scene.getRoot() instanceof Pane root)) return;

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.52);");
        overlay.setManaged(false);
        overlay.setLayoutX(0);
        overlay.setLayoutY(0);
        overlay.resize(scene.getWidth(), scene.getHeight());
        scene.widthProperty().addListener((obs, o, n) ->
                overlay.resize(n.doubleValue(), overlay.getHeight()));
        scene.heightProperty().addListener((obs, o, n) ->
                overlay.resize(overlay.getWidth(), n.doubleValue()));

        VBox card = new VBox(16);
        card.setMaxWidth(440);
        card.setMaxHeight(480);
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16px;" +
            "-fx-padding: 28 28 22 28;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 40, 0, 0, 12);"
        );
        card.setOnMouseClicked(javafx.event.Event::consume);
        overlay.setOnMouseClicked(e -> dismissOverlay(root, overlay, card));

        StackPane iconWrap = new StackPane();
        iconWrap.setStyle(
            "-fx-background-color: #2A5C45;" +
            "-fx-background-radius: 28px;" +
            "-fx-min-width: 56px; -fx-min-height: 56px;" +
            "-fx-max-width: 56px; -fx-max-height: 56px;" +
            "-fx-effect: dropshadow(gaussian, rgba(42,92,69,0.45), 14, 0, 0, 6);"
        );
        iconWrap.getChildren().add(loadEmoji("1f3c6", 28));

        Label title = new Label("You're now a Tutor!");
        title.setStyle(
            "-fx-font-family: 'Manrope','Segoe UI',sans-serif;" +
            "-fx-font-size: 24px; -fx-font-weight: bold;" +
            "-fx-text-fill: #1C2621; -fx-text-alignment: center;"
        );

        Label subtitle = new Label("Your application was approved — here's what you can do:");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #5C6661; -fx-text-alignment: center;");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(360);

        VBox features = new VBox(8);
        features.getChildren().addAll(
            featureRow("1f4c5", "Set your availability",   "Choose days and time slots to teach."),
            featureRow("1f4da", "Manage your bookings",    "Accept or decline lesson requests."),
            featureRow("1f4c8", "Track your earnings",     "Monitor revenue and upcoming payouts."),
            featureRow("2b50",  "Build your reputation",   "Collect ratings and grow your students.")
        );

        Button startBtn = new Button("Get Started →");
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setStyle(
            "-fx-background-color: #2A5C45; -fx-background-radius: 8px;" +
            "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 600;" +
            "-fx-padding: 11px 20px; -fx-cursor: hand;"
        );
        startBtn.setOnMouseEntered(e ->
            startBtn.setStyle(startBtn.getStyle().replace("#2A5C45", "#214A37")));
        startBtn.setOnMouseExited(e ->
            startBtn.setStyle(startBtn.getStyle().replace("#214A37", "#2A5C45")));
        startBtn.setOnAction(e -> dismissOverlay(root, overlay, card));

        card.getChildren().addAll(iconWrap, title, subtitle, features, startBtn);
        overlay.getChildren().add(card);
        root.getChildren().add(overlay);

        card.setScaleX(0.0);
        card.setScaleY(0.0);
        card.setOpacity(0);
        overlay.setOpacity(0);

        Timeline popupEntryAnim = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(card.scaleXProperty(),     0.0),
                new KeyValue(card.scaleYProperty(),     0.0),
                new KeyValue(card.opacityProperty(),    0.0),
                new KeyValue(overlay.opacityProperty(), 0.0)
            ),
            new KeyFrame(Duration.millis(160),
                new KeyValue(card.opacityProperty(),    1.0,  Interpolator.EASE_IN),
                new KeyValue(overlay.opacityProperty(), 1.0,  Interpolator.EASE_IN)
            ),
            new KeyFrame(Duration.millis(300),
                new KeyValue(card.scaleXProperty(), 1.10, Interpolator.EASE_OUT),
                new KeyValue(card.scaleYProperty(), 1.10, Interpolator.EASE_OUT)
            ),
            new KeyFrame(Duration.millis(400),
                new KeyValue(card.scaleXProperty(), 0.96, Interpolator.EASE_BOTH),
                new KeyValue(card.scaleYProperty(), 0.96, Interpolator.EASE_BOTH)
            ),
            new KeyFrame(Duration.millis(460),
                new KeyValue(card.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                new KeyValue(card.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)
            )
        );
        popupEntryAnim.play();
    }

    private HBox featureRow(String emojiCodepoint, String heading, String detail) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane dot = new StackPane();
        dot.setStyle(
            "-fx-background-color: #F3F4F1;" +
            "-fx-background-radius: 10px;" +
            "-fx-min-width: 42px; -fx-min-height: 42px;" +
            "-fx-max-width: 42px; -fx-max-height: 42px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 0, 3);"
        );
        dot.getChildren().add(loadEmoji(emojiCodepoint, 22));

        VBox text = new VBox(2);
        Label h = new Label(heading);
        h.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #1C2621;");
        Label d = new Label(detail);
        d.setStyle("-fx-font-size: 12px; -fx-text-fill: #5C6661;");
        text.getChildren().addAll(h, d);

        row.getChildren().addAll(dot, text);
        return row;
    }

    private javafx.scene.image.ImageView loadEmoji(String codepoint, double size) {
        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView();
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setSmooth(true);
        iv.setPreserveRatio(true);
        String url = "https://cdnjs.cloudflare.com/ajax/libs/twemoji/14.0.2/72x72/" + codepoint + ".png";
        javafx.scene.image.Image img = new javafx.scene.image.Image(url, size * 2, size * 2, true, true, true);
        img.progressProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() >= 1.0 && !img.isError()) iv.setImage(img);
        });
        return iv;
    }

    private void dismissOverlay(Pane root, StackPane overlay, VBox card) {
        Timeline popupExitAnim = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(card.scaleXProperty(),     1.0),
                new KeyValue(card.scaleYProperty(),     1.0),
                new KeyValue(overlay.opacityProperty(), 1.0)
            ),
            new KeyFrame(Duration.millis(200),
                new KeyValue(card.scaleXProperty(),     0.90, Interpolator.EASE_IN),
                new KeyValue(card.scaleYProperty(),     0.90, Interpolator.EASE_IN),
                new KeyValue(overlay.opacityProperty(), 0.0,  Interpolator.EASE_IN)
            )
        );
        popupExitAnim.setOnFinished(e -> root.getChildren().remove(overlay));
        popupExitAnim.play();
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
    // Upcoming lessons — real data from BookingDao (PAID only)
    // ----------------------------------------------------------------

    private void buildUpcomingLessons() {
        try {
            BookingDao dao = DaoFactory.getInstance().createBookingDao();
            LocalDateTime now = LocalDateTime.now();
            List<Lesson> upcoming = dao.findByTutor(
                            DaoFactory.getInstance().getConnection(), tutorUsername).stream()
                    .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                    .filter(b -> b.getLesson().getStartTime().isAfter(now))
                    .sorted(Comparator.comparing(b -> b.getLesson().getStartTime()))
                    .map(Booking::getLesson)
                    .toList();
            if (upcoming.isEmpty()) {
                Label empty = new Label("No upcoming lessons.");
                empty.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:13px;");
                upcomingLessonsList.getChildren().add(empty);
            } else {
                for (Lesson l : upcoming) upcomingLessonsList.getChildren().add(buildUpcomingCard(l));
            }
        } catch (DatabaseException e) {
            LOGGER.warning("Cannot load upcoming lessons: " + e.getMessage());
        }
    }

    public void refreshUpcomingLessons() {
        upcomingLessonsList.getChildren().clear();

        Task<List<Lesson>> task = new Task<>() {
            @Override
            protected List<Lesson> call() throws Exception {
                BookingDao dao = DaoFactory.getInstance().createBookingDao();
                LocalDateTime now = LocalDateTime.now();
                return dao.findByTutor(
                                DaoFactory.getInstance().getConnection(), tutorUsername).stream()
                        .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                        .filter(b -> b.getLesson().getStartTime().isAfter(now))
                        .sorted(Comparator.comparing(b -> b.getLesson().getStartTime()))
                        .map(Booking::getLesson)
                        .toList();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<Lesson> upcoming = task.getValue();
            upcomingLessonsList.getChildren().clear();
            if (upcoming.isEmpty()) {
                Label empty = new Label("No upcoming lessons.");
                empty.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:13px;");
                upcomingLessonsList.getChildren().add(empty);
            } else {
                for (Lesson l : upcoming) upcomingLessonsList.getChildren().add(buildUpcomingCard(l));
            }
        }));

        task.setOnFailed(e ->
                LOGGER.warning("Cannot refresh upcoming lessons: " + task.getException().getMessage()));

        Thread t = new Thread(task, "tutor-upcoming-refresh");
        t.setDaemon(true);
        t.start();
    }

    private HBox buildUpcomingCard(Lesson lesson) {
        String subject  = lesson.getExpertise().getSubcategory().getName();
        String mode     = lesson.isRemote() ? "Remote" : "In-Person";
        String datetime = lesson.getStartTime().format(CARD_FMT);

        HBox card = new HBox(16);
        card.getStyleClass().add("lesson-card");
        card.setAlignment(Pos.CENTER_LEFT);

        StackPane dot = new StackPane();
        dot.getStyleClass().add("lesson-dot-upcoming");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label subjectLbl = new Label(subject);
        subjectLbl.getStyleClass().add("lesson-title");

        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);
        FontIcon modeIcon = new FontIcon(lesson.isRemote() ? "fas-video" : "fas-map-marker-alt");
        modeIcon.getStyleClass().add(LESSON_META_ICON);
        Label modeLbl = new Label(mode);
        modeLbl.getStyleClass().add(LESSON_META);
        FontIcon clockIcon = new FontIcon("fas-clock");
        clockIcon.getStyleClass().add(LESSON_META_ICON);
        Label time = new Label(datetime);
        time.getStyleClass().add(LESSON_META);
        meta.getChildren().addAll(modeIcon, modeLbl, clockIcon, time);
        info.getChildren().addAll(subjectLbl, meta);

        Button startBtn = new Button("Start");
        startBtn.getStyleClass().add("book-btn");
        card.getChildren().addAll(dot, info, startBtn);
        return card;
    }

    // ----------------------------------------------------------------
    // Booking requests
    // ----------------------------------------------------------------

    private void buildBookingRequests() {
        long pendingCount = MOCK_BOOKINGS.stream().filter(BookingMock::pending).count();
        pendingBadge.setText(pendingCount + " new");
        for (BookingMock booking : MOCK_BOOKINGS) {
            bookingRequestsList.getChildren().add(buildBookingCard(booking));
        }
    }

    private VBox buildBookingCard(BookingMock booking) {
        VBox card = new VBox(8);
        card.getStyleClass().add("booking-request-card");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label avatar = new Label(String.valueOf(booking.student().charAt(0)).toUpperCase());
        avatar.getStyleClass().add("booking-avatar");

        VBox nameBox = new VBox(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        Label name = new Label(booking.student());
        name.getStyleClass().add("booking-student-name");
        Label subject = new Label(booking.subject());
        subject.getStyleClass().add("booking-subject");
        nameBox.getChildren().addAll(name, subject);

        Label statusLabel = new Label(booking.pending() ? "pending" : "confirmed");
        statusLabel.getStyleClass().add(booking.pending() ? "badge-pending-status" : "badge-confirmed-status");
        topRow.getChildren().addAll(avatar, nameBox, statusLabel);

        HBox metaRow = new HBox(12);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon calIcon = new FontIcon("fas-calendar-alt");
        calIcon.getStyleClass().add(LESSON_META_ICON);
        Label dateLbl = new Label(booking.date());
        dateLbl.getStyleClass().add(LESSON_META);
        FontIcon clkIcon = new FontIcon("fas-clock");
        clkIcon.getStyleClass().add(LESSON_META_ICON);
        Label timeLbl = new Label(booking.time());
        timeLbl.getStyleClass().add(LESSON_META);
        metaRow.getChildren().addAll(calIcon, dateLbl, clkIcon, timeLbl);

        card.getChildren().addAll(topRow, metaRow);

        if (booking.pending()) {
            HBox actions = new HBox(8);
            Button acceptBtn = new Button("Accept");
            acceptBtn.getStyleClass().add("accept-btn");
            acceptBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(acceptBtn, Priority.ALWAYS);
            Button declineBtn = new Button("Decline");
            declineBtn.getStyleClass().add("decline-btn");
            declineBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(declineBtn, Priority.ALWAYS);
            actions.getChildren().addAll(acceptBtn, declineBtn);
            card.getChildren().add(actions);
        }
        return card;
    }

    // ----------------------------------------------------------------
    // This week
    // ----------------------------------------------------------------

    private void buildThisWeek() {
        addWeekRow("Lessons Completed", "8",    false);
        addWeekRow("Hours Taught",      "12h",  false);
        addWeekRow("Earnings",          "€540", true);
        addWeekRow("New Students",      "3",    false);
    }

    private void addWeekRow(String label, String value, boolean green) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("week-row-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label val = new Label(value);
        val.getStyleClass().add(green ? "week-row-value-green" : "week-row-value");
        row.getChildren().addAll(lbl, spacer, val);
        thisWeekList.getChildren().add(row);
    }
}
