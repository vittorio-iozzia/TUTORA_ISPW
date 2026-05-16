package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.bean.BookingBean;
import it.ispw.tutora.controller.application.GetStudentLessonsController;
import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Lesson;
import it.ispw.tutora.view.SceneManager;
import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

public class MyLessonsGfxController {

    private static final Logger LOGGER = Logger.getLogger(MyLessonsGfxController.class.getName());

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEE, dd MMM");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    @FXML private HBox  statCard1;
    @FXML private HBox  statCard2;
    @FXML private HBox  statCard3;
    @FXML private HBox  statCard4;
    @FXML private Label statUpcomingValue;
    @FXML private Label statCompletedValue;
    @FXML private Label statHoursValue;
    @FXML private Label statSpentValue;
    @FXML private VBox  upcomingList;
    @FXML private VBox  pastList;

    private final GetStudentLessonsController appController =
            new GetStudentLessonsController();

    @FXML
    public void initialize() {
        try {
            String token = SceneManager.getInstance().getSessionToken();
            List<Booking> bookings = appController.loadBookings(new BookingBean(), token);
            populate(bookings);
        } catch (Exception e) {
            LOGGER.warning("[MyLessons] initialize error: " + e.getMessage());
            showEmptyState();
        }
    }

    // ----------------------------------------------------------------
    // Populate
    // ----------------------------------------------------------------

    private void populate(List<Booking> bookings) {
        List<Booking> upcoming = bookings.stream()
                .filter(b -> b.getLesson().getLessonStatus() == LessonStatus.BOOKED)
                .sorted((a, b) -> a.getLesson().getStartTime()
                        .compareTo(b.getLesson().getStartTime()))
                .toList();
        List<Booking> past = bookings.stream()
                .filter(b -> b.getLesson().getLessonStatus() == LessonStatus.COMPLETED)
                .sorted((a, b) -> b.getLesson().getStartTime()
                        .compareTo(a.getLesson().getStartTime()))
                .toList();

        long totalMinutes = past.stream()
                .mapToLong(b -> java.time.Duration
                        .between(b.getLesson().getStartTime(), b.getLesson().getEndTime())
                        .toMinutes())
                .sum();
        BigDecimal totalSpent = bookings.stream()
                .map(Booking::getPricePaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        animateStat(statUpcomingValue, upcoming.size(),          "%.0f");
        animateStat(statCompletedValue, past.size(),             "%.0f");
        animateStat(statHoursValue,    totalMinutes / 60.0,     "%.1fh");
        animateStat(statSpentValue,    totalSpent.doubleValue(), "€%.2f");

        addHoverLift(statCard1);
        addHoverLift(statCard2);
        addHoverLift(statCard3);
        addHoverLift(statCard4);

        if (upcoming.isEmpty()) {
            upcomingList.getChildren().add(buildEmptyState("📅",
                    "No upcoming lessons", "Book a session with a tutor to get started"));
        } else {
            for (Booking b : upcoming) upcomingList.getChildren().add(buildCard(b, true));
        }

        if (past.isEmpty()) {
            pastList.getChildren().add(buildEmptyState("✅",
                    "No completed lessons yet", "Your lesson history will appear here"));
        } else {
            for (Booking b : past) pastList.getChildren().add(buildCard(b, false));
        }
    }

    private void showEmptyState() {
        if (statUpcomingValue != null) statUpcomingValue.setText("0");
        if (statCompletedValue != null) statCompletedValue.setText("0");
        if (statHoursValue != null)     statHoursValue.setText("0.0h");
        if (statSpentValue != null)     statSpentValue.setText("€0.00");
        if (upcomingList != null)
            upcomingList.getChildren().add(buildEmptyState("📅",
                    "No upcoming lessons", "Book a session with a tutor to get started"));
        if (pastList != null)
            pastList.getChildren().add(buildEmptyState("✅",
                    "No completed lessons yet", "Your lesson history will appear here"));
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
    }

    // ----------------------------------------------------------------
    // Lesson card
    // ----------------------------------------------------------------

    private HBox buildCard(Booking booking, boolean isUpcoming) {
        Lesson lesson   = booking.getLesson();
        String tutor    = lesson.getExpertise().getTutor().getName()
                        + " " + lesson.getExpertise().getTutor().getSurname();
        String subject  = lesson.getExpertise().getSubcategory().getName();
        LocalDateTime s = lesson.getStartTime();
        LocalDateTime e = lesson.getEndTime();

        // Card shell
        HBox card = new HBox(0);
        card.getStyleClass().add("ml-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);

        // Left colour strip
        Region accent = new Region();
        accent.setStyle(
                (isUpcoming ? "-fx-background-color:#3B82F6;" : "-fx-background-color:#D1D5DB;")
                + "-fx-min-width:5;-fx-max-width:5;-fx-min-height:80;");

        // Inner padded row
        HBox inner = new HBox(18);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.setPadding(new Insets(16, 20, 16, 16));
        HBox.setHgrow(inner, Priority.ALWAYS);

        // Date badge
        VBox dateBadge = new VBox(1);
        dateBadge.setAlignment(Pos.CENTER);
        dateBadge.setStyle(
                (isUpcoming ? "-fx-background-color:#EFF6FF;" : "-fx-background-color:#F3F4F6;")
                + "-fx-background-radius:10;"
                + "-fx-min-width:52;-fx-max-width:52;"
                + "-fx-min-height:52;-fx-max-height:52;");

        Label dayLbl = new Label(String.valueOf(s.getDayOfMonth()));
        dayLbl.setStyle("-fx-font-size:19px;-fx-font-weight:800;"
                + (isUpcoming ? "-fx-text-fill:#1D4ED8;" : "-fx-text-fill:#6B7280;"));

        Label monthLbl = new Label(s.getMonth().toString().substring(0, 3));
        monthLbl.setStyle("-fx-font-size:11px;-fx-font-weight:600;"
                + (isUpcoming ? "-fx-text-fill:#60A5FA;" : "-fx-text-fill:#9CA3AF;"));

        dateBadge.getChildren().addAll(dayLbl, monthLbl);

        // Info column
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label subjectLbl = new Label(subject);
        subjectLbl.getStyleClass().add("ml-subject");

        Label modeLbl = new Label(lesson.isRemote() ? "Online" : "In person");
        modeLbl.getStyleClass().add(lesson.isRemote() ? "ml-badge-online" : "ml-badge-person");
        topRow.getChildren().addAll(subjectLbl, modeLbl);

        Label tutorLbl = new Label("with " + tutor);
        tutorLbl.getStyleClass().add("ml-tutor");

        Label timeLbl = new Label(s.format(DATE_FMT) + "   •   "
                + s.format(TIME_FMT) + " – " + e.format(TIME_FMT));
        timeLbl.getStyleClass().add("ml-time");

        info.getChildren().addAll(topRow, tutorLbl, timeLbl);

        // Right: price + action
        VBox right = new VBox(8);
        right.setAlignment(Pos.CENTER_RIGHT);

        Label price = new Label("€" + booking.getPricePaid().toPlainString());
        price.getStyleClass().add("ml-price");

        if (isUpcoming) {
            Button joinBtn = new Button("Join Lesson");
            joinBtn.getStyleClass().add("ml-join-btn");
            right.getChildren().addAll(price, joinBtn);
        } else {
            Label doneLbl = new Label("✔  Completed");
            doneLbl.getStyleClass().add("ml-done-badge");
            right.getChildren().addAll(price, doneLbl);
        }

        inner.getChildren().addAll(dateBadge, info, right);
        card.getChildren().addAll(accent, inner);
        return card;
    }

    // ----------------------------------------------------------------
    // Empty state
    // ----------------------------------------------------------------

    private VBox buildEmptyState(String emoji, String title, String subtitle) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPadding(new Insets(60, 0, 60, 0));

        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size:40px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size:17px;-fx-font-weight:700;"
                + "-fx-font-family:'Segoe UI Semibold','Segoe UI',sans-serif;"
                + "-fx-text-fill:#374151;");

        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-font-size:13px;-fx-font-family:'Segoe UI',sans-serif;"
                + "-fx-text-fill:#9CA3AF;");
        subLbl.setWrapText(true);
        subLbl.setMaxWidth(300);

        box.getChildren().addAll(icon, titleLbl, subLbl);
        return box;
    }
}
