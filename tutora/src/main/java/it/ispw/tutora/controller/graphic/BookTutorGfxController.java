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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class BookTutorGfxController {

    private static final Logger LOGGER = Logger.getLogger(BookTutorGfxController.class.getName());
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    @FXML private VBox      dialogRoot;
    @FXML private Label     titleLabel;
    @FXML private Label     subtitleLabel;
    @FXML private Label     bookingBannerLabel;
    @FXML private VBox      formContainer;
    @FXML private VBox      successPane;
    @FXML private VBox      footer;
    @FXML private Button    bookBtn;
    @FXML private Button    backBtn;
    @FXML private Label     errorLabel;
    @FXML private CheckBox  confirmCheckBox;
    @FXML private Button    closeBtn;

    @FXML private StackPane headerIconWrap;
    @FXML private ImageView headerIconView;
    @FXML private StackPane bannerIconWrap;
    @FXML private ImageView bannerIconView;
    @FXML private StackPane successIconWrap;
    @FXML private ImageView successIconView;

    private Tutor       tutor;
    private Lesson      selectedLesson;
    private double      selectedDurationHours = 1.0;

    private VBox        durationSection;
    private HBox        durationBtnsRow;
    private Label       priceValueLabel;
    private ToggleGroup lessonGroup;
    private ToggleGroup durationGroup;

    private final BookTutorController bookTutorController = new BookTutorController();

    // ----------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------

    @FXML
    private void initialize() {
        setupIconBox(headerIconWrap, headerIconView, "1f4c5", 22);
        setupIconBox(bannerIconWrap, bannerIconView, "1f9d1", 13);
        setupIconBox(successIconWrap, successIconView, "2705", 34);
        applyRoundedClip(dialogRoot);
    }

    private void applyRoundedClip(VBox root) {
        if (root == null) return;
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        root.layoutBoundsProperty().addListener((obs, o, n) -> {
            if (n.getWidth() > 0 && root.getClip() == null) {
                clip.setWidth(n.getWidth());
                clip.setHeight(n.getHeight());
                root.widthProperty().addListener((o2, ov, nv)  -> clip.setWidth(nv.doubleValue()));
                root.heightProperty().addListener((o2, ov, nv) -> clip.setHeight(nv.doubleValue()));
                root.setClip(clip);
            }
        });
    }

    private void setupIconBox(StackPane wrap, ImageView iv, String codepoint, double size) {
        if (wrap == null) return;
        wrap.setEffect(new DropShadow(10, 0, 4, Color.web("#00000026")));
        if (iv != null) {
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            String url = "https://cdnjs.cloudflare.com/ajax/libs/twemoji/14.0.2/72x72/" + codepoint + ".png";
            Image img = new Image(url, size * 2, size * 2, true, true, true);
            img.progressProperty().addListener((obs, o, n) -> {
                if (n.doubleValue() >= 1.0 && !img.isError()) iv.setImage(img);
            });
        }
    }

    // ----------------------------------------------------------------
    // Entry point
    // ----------------------------------------------------------------

    public void initTutor(Tutor tutor) {
        this.tutor = tutor;
        titleLabel.setText("Book with " + tutor.getFullName());
        subtitleLabel.setText("Select an available slot for your lesson with " + tutor.getFullName() + ".");
        bookingBannerLabel.setText("Booking with: " + tutor.getFullName());
        confirmCheckBox.selectedProperty().addListener((obs, o, n) -> updateBookState());
        loadLessons();
    }

    // ----------------------------------------------------------------
    // Load lessons (background thread)
    // ----------------------------------------------------------------

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

    // ----------------------------------------------------------------
    // Lesson section builder
    // ----------------------------------------------------------------

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
        desc.getStyleClass().add("app-field-desc");
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
                    selectedDurationHours = 1.0;
                    refreshDurationSection();
                    updateBookState();
                }
            });
            for (Lesson lesson : lessons) {
                sectionBox.getChildren().add(buildLessonCard(lesson));
            }
        }

        formContainer.getChildren().add(sectionBox);

        // Duration section (hidden until lesson is selected)
        durationSection = buildDurationSection();
        durationSection.setVisible(false);
        durationSection.setManaged(false);
        formContainer.getChildren().add(durationSection);
    }

    private ToggleButton buildLessonCard(Lesson lesson) {
        long totalMins = Duration.between(lesson.getStartTime(), lesson.getEndTime()).toMinutes();
        String subject  = lesson.getExpertise().getSubcategory().getName();
        String dateStr  = lesson.getStartTime().format(DATE_FMT);
        String timeStr  = lesson.getStartTime().format(TIME_FMT)
                + " – " + lesson.getEndTime().format(TIME_FMT)
                + "  (" + formatDuration(totalMins) + ")";
        String modeStr  = lesson.isRemote() ? "Online" : "In-Person";
        String priceStr = "€" + lesson.getListedPrice().stripTrailingZeros().toPlainString();

        // Icon
        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().addAll("stat-icon-wrap", "stat-blue");
        iconWrap.setEffect(new DropShadow(10, 0, 3, Color.web("#00000020")));
        iconWrap.getChildren().add(loadTwemoji("1f4c5", 20)); // 📅 calendar

        // Info
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label subjectLbl = new Label(subject);
        subjectLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1C2621; -fx-font-family: 'Segoe UI';");

        Label dateLbl = buildMetaRow("fas-calendar", dateStr);
        Label timeLbl = buildMetaRow("fas-clock", timeStr);
        Label modeLbl = buildMetaRow(lesson.isRemote() ? "fas-wifi" : "fas-map-marker-alt", modeStr);
        info.getChildren().addAll(subjectLbl, dateLbl, timeLbl, modeLbl);

        // Price
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

    private Label buildMetaRow(String iconLiteral, String text) {
        // Returns a Label with icon prefix rendered as text (FontIcon not embeddable in Label)
        // Use HBox approach but return as standalone element via wrapper trick:
        // Actually just return plain label; we style via CSS
        Label lbl = new Label(text);
        lbl.getStyleClass().add("lesson-meta");
        return lbl;
    }

    // ----------------------------------------------------------------
    // Duration section
    // ----------------------------------------------------------------

    private VBox buildDurationSection() {
        VBox section = new VBox(12);
        section.getStyleClass().add("app-field-box");

        HBox labelRow = new HBox(8);
        labelRow.setAlignment(Pos.CENTER_LEFT);
        Label stepBadge = new Label("2");
        stepBadge.getStyleClass().add("app-step-badge");
        Label sectionTitle = new Label("Duration");
        sectionTitle.getStyleClass().add("app-field-label");
        Label star = new Label("*");
        star.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 14px;");
        labelRow.getChildren().addAll(stepBadge, sectionTitle, star);

        Label desc = new Label("Choose how long your lesson will be. Options depend on the available slot.");
        desc.getStyleClass().add("app-field-desc");
        desc.setWrapText(true);

        durationBtnsRow = buildDurationBtnsRow();
        VBox priceSummary = buildPriceSummary();

        section.getChildren().addAll(labelRow, desc, durationBtnsRow, priceSummary);
        return section;
    }

    private HBox buildDurationBtnsRow() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        durationGroup = new ToggleGroup();

        double[] durations = {1.0, 1.5, 2.0};
        String[] labels    = {"1 hour", "1h 30m", "2 hours"};

        for (int i = 0; i < durations.length; i++) {
            final double dur = durations[i];
            ToggleButton btn = new ToggleButton(labels[i]);
            btn.getStyleClass().add("time-slot-btn");
            btn.setToggleGroup(durationGroup);
            btn.setUserData(dur);
            btn.selectedProperty().addListener((obs, o, n) -> {
                if (n) { selectedDurationHours = dur; updatePriceDisplay(); }
            });
            box.getChildren().add(btn);
        }

        durationGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) oldT.setSelected(true);
        });

        return box;
    }

    private VBox buildPriceSummary() {
        VBox box = new VBox(8);
        box.getStyleClass().add("booking-price-box");

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label("Estimated cost");
        label.getStyleClass().add("booking-price-label");
        HBox.setHgrow(label, Priority.ALWAYS);

        priceValueLabel = new Label("€—");
        priceValueLabel.getStyleClass().add("booking-price-value");
        row.getChildren().addAll(label, priceValueLabel);

        Label note = new Label("Final price confirmed by the tutor upon acceptance.");
        note.getStyleClass().add("app-field-desc");
        note.setWrapText(true);

        box.getChildren().addAll(row, note);
        return box;
    }

    // ----------------------------------------------------------------
    // State refresh
    // ----------------------------------------------------------------

    private void refreshDurationSection() {
        if (selectedLesson == null) {
            durationSection.setVisible(false);
            durationSection.setManaged(false);
            return;
        }

        long totalMins = Duration.between(
                selectedLesson.getStartTime(), selectedLesson.getEndTime()).toMinutes();

        double[] durations = {1.0, 1.5, 2.0};
        int firstEnabled = -1;
        for (int i = 0; i < durationBtnsRow.getChildren().size(); i++) {
            ToggleButton btn = (ToggleButton) durationBtnsRow.getChildren().get(i);
            boolean fits = (long)(durations[i] * 60) <= totalMins;
            btn.setDisable(!fits);
            btn.getStyleClass().remove("time-slot-unavailable");
            if (!fits) btn.getStyleClass().add("time-slot-unavailable");
            else if (firstEnabled < 0) firstEnabled = i;
        }

        // Auto-select first available duration
        if (firstEnabled >= 0) {
            ToggleButton first = (ToggleButton) durationBtnsRow.getChildren().get(firstEnabled);
            first.setSelected(true);
            selectedDurationHours = durations[firstEnabled];
        }

        durationSection.setVisible(true);
        durationSection.setManaged(true);
        updatePriceDisplay();
    }

    private void updatePriceDisplay() {
        if (selectedLesson == null || priceValueLabel == null) return;
        long totalMins = Duration.between(
                selectedLesson.getStartTime(), selectedLesson.getEndTime()).toMinutes();
        if (totalMins == 0) return;
        BigDecimal hourlyRate = selectedLesson.getListedPrice()
                .divide(BigDecimal.valueOf(totalMins).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP),
                        2, RoundingMode.HALF_UP);
        BigDecimal cost = hourlyRate
                .multiply(BigDecimal.valueOf(selectedDurationHours))
                .setScale(2, RoundingMode.HALF_UP);
        priceValueLabel.setText("€" + cost.toPlainString());
    }

    private void updateBookState() {
        bookBtn.setDisable(selectedLesson == null || !confirmCheckBox.isSelected());
    }

    // ----------------------------------------------------------------
    // FXML handlers
    // ----------------------------------------------------------------

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

    // ----------------------------------------------------------------
    // UI helpers
    // ----------------------------------------------------------------

    private void showSuccess() {
        formContainer.setVisible(false);
        formContainer.setManaged(false);
        footer.setVisible(false);
        footer.setManaged(false);
        successPane.setVisible(true);
        successPane.setManaged(true);
    }

    private void showError(String msg) {
        if (msg == null || msg.isBlank()) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        } else {
            errorLabel.setText(msg);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private String formatDuration(long minutes) {
        if (minutes % 60 == 0) return (minutes / 60) + "h";
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    private ImageView loadTwemoji(String codepoint, double size) {
        ImageView iv = new ImageView();
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setSmooth(true);
        iv.setPreserveRatio(true);
        String url = "https://cdnjs.cloudflare.com/ajax/libs/twemoji/14.0.2/72x72/" + codepoint + ".png";
        Image img = new Image(url, size * 2, size * 2, true, true, true);
        img.progressProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() >= 1.0 && !img.isError()) iv.setImage(img);
        });
        return iv;
    }
}
