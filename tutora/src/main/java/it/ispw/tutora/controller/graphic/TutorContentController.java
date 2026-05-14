package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.SceneManager;
import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class TutorContentController {

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
    @FXML private FlowPane timeSlotGrid;
    @FXML private VBox     upcomingLessonsList;

    // Right column
    @FXML private Label pendingBadge;
    @FXML private VBox  bookingRequestsList;
    @FXML private VBox  thisWeekList;

    // ----------------------------------------------------------------
    // Demo data
    // ----------------------------------------------------------------

    private record BookingMock(String student, String subject, String date, String time, boolean pending) {}
    private record LessonMock(String subject, String student, String date, String time) {}

    private static final List<String>  TIME_SLOTS     = List.of(
        "9:00 AM","10:00 AM","11:00 AM","12:00 PM",
        "1:00 PM","2:00 PM","3:00 PM","4:00 PM","5:00 PM"
    );
    private static final boolean[] SLOT_AVAILABLE = {true,true,false,false,true,true,true,false,true};

    private static final List<BookingMock> MOCK_BOOKINGS = List.of(
        new BookingMock("Alex Thompson","Calculus II",   "Jan 15","3:00 PM", true),
        new BookingMock("Emma Wilson",  "Linear Algebra","Jan 16","10:00 AM",true),
        new BookingMock("Michael Brown","Statistics",    "Jan 17","2:00 PM", false)
    );

    private static final List<LessonMock> MOCK_UPCOMING = List.of(
        new LessonMock("Calculus I",             "Sarah Davis","Today",   "4:00 PM"),
        new LessonMock("Differential Equations", "John Smith", "Tomorrow","11:00 AM")
    );

    // ----------------------------------------------------------------
    // Init
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        String token   = SceneManager.getInstance().getSessionToken();
        Session session = SessionManager.getInstance().getSession(token);
        welcomeTitle.setText("Welcome, " + session.getUser().getName() + "!");

        animateStat(totalStudentsLabel, 48,   "%.0f");
        animateStat(lessonsMonthLabel,  32,   "%.0f");
        animateStat(earningsLabel,    1440,   "€%.0f");
        animateStat(avgRatingLabel,    4.9,   "%.1f");

        addHoverLift(statCard1);
        addHoverLift(statCard2);
        addHoverLift(statCard3);
        addHoverLift(statCard4);

        buildTimeSlots();
        buildUpcomingLessons();
        buildBookingRequests();
        buildThisWeek();
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
    // Time slots
    // ----------------------------------------------------------------

    private void buildTimeSlots() {
        for (int i = 0; i < TIME_SLOTS.size(); i++) {
            String time      = TIME_SLOTS.get(i);
            boolean available = SLOT_AVAILABLE[i];
            ToggleButton btn  = new ToggleButton(time);
            btn.setSelected(available);
            btn.getStyleClass().add("time-slot-btn");
            if (!available) btn.getStyleClass().add("time-slot-unavailable");
            btn.selectedProperty().addListener((obs, was, on) -> {
                if (on) btn.getStyleClass().remove("time-slot-unavailable");
                else    { if (!btn.getStyleClass().contains("time-slot-unavailable")) btn.getStyleClass().add("time-slot-unavailable"); }
            });
            timeSlotGrid.getChildren().add(btn);
        }
    }

    // ----------------------------------------------------------------
    // Upcoming lessons
    // ----------------------------------------------------------------

    private void buildUpcomingLessons() {
        for (LessonMock lesson : MOCK_UPCOMING) {
            upcomingLessonsList.getChildren().add(buildUpcomingCard(lesson));
        }
    }

    private HBox buildUpcomingCard(LessonMock lesson) {
        HBox card = new HBox(16);
        card.getStyleClass().add("lesson-card");
        card.setAlignment(Pos.CENTER_LEFT);

        StackPane dot = new StackPane();
        dot.getStyleClass().add("lesson-dot-upcoming");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label subject = new Label(lesson.subject());
        subject.getStyleClass().add("lesson-title");

        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);
        FontIcon userIcon = new FontIcon("fas-user");
        userIcon.getStyleClass().add("lesson-meta-icon");
        Label studentLbl = new Label(lesson.student());
        studentLbl.getStyleClass().add("lesson-meta");
        FontIcon clockIcon = new FontIcon("fas-clock");
        clockIcon.getStyleClass().add("lesson-meta-icon");
        Label time = new Label(lesson.date() + " · " + lesson.time());
        time.getStyleClass().add("lesson-meta");
        meta.getChildren().addAll(userIcon, studentLbl, clockIcon, time);
        info.getChildren().addAll(subject, meta);

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
        calIcon.getStyleClass().add("lesson-meta-icon");
        Label dateLbl = new Label(booking.date());
        dateLbl.getStyleClass().add("lesson-meta");
        FontIcon clkIcon = new FontIcon("fas-clock");
        clkIcon.getStyleClass().add("lesson-meta-icon");
        Label timeLbl = new Label(booking.time());
        timeLbl.getStyleClass().add("lesson-meta");
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
