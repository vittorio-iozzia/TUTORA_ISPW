package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.bean.BookingBean;
import it.ispw.tutora.bean.LessonStudentBean;
import it.ispw.tutora.controller.application.BookTutorController;
import it.ispw.tutora.model.Lesson;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.view.SceneManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class BookTutorGfxController extends DialogGfxController {

    private static final String APP_FIELD_DESC = "app-field-desc";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label     titleLabel;
    @FXML private Label     subtitleLabel;
    @FXML private Label     bookingBannerLabel;
    @FXML private Button    bookBtn;
    @FXML private Button    backBtn;
    @FXML private CheckBox  confirmCheckBox;
    @FXML private Button    closeBtn;

    private Tutor       tutor;
    private Lesson      selectedLesson;
    private ToggleGroup lessonGroup;

    private final BookTutorController bookTutorController = new BookTutorController();

    @FXML
    private void initialize() {
        setupIconBox(headerIconWrap, headerIconView, "1f4c5", 22);
        setupIconBox(bannerIconWrap, bannerIconView, "1f9d1", 13);
        setupIconBox(successIconWrap, successIconView, "2705", 34);
        applyRoundedClip(dialogRoot);
    }

    public void initTutor(Tutor tutor) {
        this.tutor = tutor;
        titleLabel.setText("Book with " + tutor.getFullName());
        subtitleLabel.setText("Select an available slot for your lesson with " + tutor.getFullName() + ".");
        bookingBannerLabel.setText("Booking with: " + tutor.getFullName());
        confirmCheckBox.selectedProperty().addListener((obs, o, n) -> updateBookState());
        loadLessons();
    }

    private void loadLessons() {
        formContainer.getChildren().clear();
        Label loading = new Label("Loading available slots…");
        loading.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px; -fx-font-style: italic;");
        formContainer.getChildren().add(loading);

        String token = SceneManager.getInstance().getSessionToken();

        Task<List<Lesson>> task = new Task<>() {
            @Override
            protected List<Lesson> call() {
                LessonStudentBean bean = new LessonStudentBean();
                bean.setTutorUsername(tutor.getUsername());
                bookTutorController.searchAvailableLessons(bean, token);
                return bean.getList() != null ? bean.getList() : List.of();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> buildLessonSection(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> {
            Label err = new Label("Could not load lessons. Please try again.");
            err.setStyle("-fx-text-fill: #9C2121; -fx-font-size: 13px;");
            formContainer.getChildren().setAll(err);
        }));

        Thread t = new Thread(task, "book-tutor-load");
        t.setDaemon(true);
        t.start();
    }

    private void buildLessonSection(List<Lesson> lessons) {
        formContainer.getChildren().clear();

        VBox sectionBox = new VBox(12);
        sectionBox.getStyleClass().add("app-field-box");

        HBox labelRow = new HBox(8);
        labelRow.setAlignment(Pos.CENTER_LEFT);
        Label stepBadge = new Label("1");
        stepBadge.getStyleClass().add("app-step-badge");
        Label sectionTitle = new Label("Available Lesson Slots");
        sectionTitle.getStyleClass().add("app-field-label");
        Label star = new Label("*");
        star.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 14px;");
        labelRow.getChildren().addAll(stepBadge, sectionTitle, star);

        Label desc = new Label("Choose the slot that best fits your schedule.");
        desc.getStyleClass().add(APP_FIELD_DESC);
        desc.setWrapText(true);

        sectionBox.getChildren().addAll(labelRow, desc);

        if (lessons.isEmpty()) {
            Label empty = new Label("No available slots at the moment. Check back later.");
            empty.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px; -fx-font-style: italic;");
            sectionBox.getChildren().add(empty);
        } else {
            lessonGroup = new ToggleGroup();
            lessonGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
                if (newT == null && oldT != null) { oldT.setSelected(true); return; }
                if (newT != null) {
                    selectedLesson = (Lesson) newT.getUserData();
                    updateBookState();
                }
            });
            for (Lesson lesson : lessons) {
                sectionBox.getChildren().add(buildLessonCard(lesson));
            }
        }

        formContainer.getChildren().add(sectionBox);
    }

    private ToggleButton buildLessonCard(Lesson lesson) {
        long totalMins = Duration.between(
                lesson.getStartTime().atZone(ZoneId.systemDefault()),
                lesson.getEndTime().atZone(ZoneId.systemDefault())).toMinutes();
        String subject  = lesson.getExpertise().getSubcategory().getName();
        String dateStr  = lesson.getStartTime().format(DATE_FMT);
        String timeStr  = lesson.getStartTime().format(TIME_FMT)
                + " – " + lesson.getEndTime().format(TIME_FMT)
                + "  (" + formatDuration(totalMins) + ")";
        String modeStr  = lesson.isRemote() ? "Online" : "In-Person";
        String priceStr = "€" + lesson.getListedPrice().stripTrailingZeros().toPlainString();

        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().addAll("stat-icon-wrap", "stat-blue");
        iconWrap.setEffect(new DropShadow(10, 0, 3, Color.web("#00000020")));
        iconWrap.getChildren().add(loadTwemoji("1f4c5", 20));

        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label subjectLbl = new Label(subject);
        subjectLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1C2621; -fx-font-family: 'Segoe UI';");

        Label dateLbl = buildMetaRow(dateStr);
        Label timeLbl = buildMetaRow(timeStr);
        Label modeLbl = buildMetaRow(modeStr);
        info.getChildren().addAll(subjectLbl, dateLbl, timeLbl, modeLbl);

        VBox priceBox = new VBox(2);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        Label fromLbl = new Label("from");
        fromLbl.getStyleClass().add("ft-price-from");
        Label priceLbl = new Label(priceStr);
        priceLbl.getStyleClass().add("ft-price-value");
        priceBox.getChildren().addAll(fromLbl, priceLbl);

        HBox content = new HBox(14);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(Double.MAX_VALUE);
        content.getChildren().addAll(iconWrap, info, priceBox);

        ToggleButton card = new ToggleButton();
        card.setGraphic(content);
        card.setToggleGroup(lessonGroup);
        card.setUserData(lesson);
        card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("booking-lesson-card");
        return card;
    }

    private Label buildMetaRow(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("lesson-meta");
        return lbl;
    }

    private void updateBookState() {
        bookBtn.setDisable(selectedLesson == null || !confirmCheckBox.isSelected());
    }

    @FXML
    private void handleBook() {
        if (selectedLesson == null) return;
        showError(null);
        bookBtn.setDisable(true);
        bookBtn.setText("Requesting…");

        String token = SceneManager.getInstance().getSessionToken();
        BookingBean bean = new BookingBean();
        bean.setLessonId(selectedLesson.getId());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                bookTutorController.requestBooking(bean, token);
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (bean.getErrorMessage() != null) {
                showError(bean.getErrorMessage());
                bookBtn.setDisable(false);
                bookBtn.setText("Request Booking");
            } else {
                showSuccess();
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            showError("An unexpected error occurred. Please try again.");
            bookBtn.setDisable(false);
            bookBtn.setText("Request Booking");
        }));

        Thread t = new Thread(task, "book-tutor-request");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleClose() {
        ((Stage) successPane.getScene().getWindow()).close();
    }

    @FXML
    private void handleBack() {
        ((Stage) bookBtn.getScene().getWindow()).close();
    }

    private String formatDuration(long minutes) {
        if (minutes % 60 == 0) return (minutes / 60) + "h";
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }
}
