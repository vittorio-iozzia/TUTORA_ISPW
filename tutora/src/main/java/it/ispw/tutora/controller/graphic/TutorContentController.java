package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Lesson;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.SceneManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.logging.Logger;
import javafx.animation.*;
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

public class TutorContentController {

    private static final Logger LOGGER =
            Logger.getLogger(TutorContentController.class.getName());

    private static final DateTimeFormatter CARD_FMT =
            DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", java.util.Locale.ENGLISH);

    private String tutorUsername;

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

    // ----------------------------------------------------------------
    // Init
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        String token   = SceneManager.getInstance().getSessionToken();
        Session session = SessionManager.getInstance().getSession(token);
        this.tutorUsername = session.getUser().getUsername();
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
    // Upcoming lessons — dati reali da BookingDao (solo booking PAID)
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

    /**
     * Ricarica la lista delle lezioni imminenti in background.
     * Chiamato da HomeGfxController dopo che un pagamento è stato confermato.
     */
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

        task.setOnSucceeded(e -> javafx.application.Platform.runLater(() -> {
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
        modeIcon.getStyleClass().add("lesson-meta-icon");
        Label modeLbl = new Label(mode);
        modeLbl.getStyleClass().add("lesson-meta");
        FontIcon clockIcon = new FontIcon("fas-clock");
        clockIcon.getStyleClass().add("lesson-meta-icon");
        Label time = new Label(datetime);
        time.getStyleClass().add("lesson-meta");
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
