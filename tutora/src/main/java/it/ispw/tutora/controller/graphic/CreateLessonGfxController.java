package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.bean.LessonTutorBean;
import it.ispw.tutora.controller.application.CreateLessonController;
import it.ispw.tutora.model.TutorExpertise;
import it.ispw.tutora.view.SceneManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class CreateLessonGfxController {

    private static final Logger LOGGER =
            Logger.getLogger(CreateLessonGfxController.class.getName());
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy", Locale.ENGLISH);

    @FXML private VBox dialogRoot;
    @FXML private Label dateLabel;
    @FXML private ComboBox<String> subjectCombo;
    @FXML private Label subjectHintLabel;
    @FXML private ComboBox<String> startTimeCombo;
    @FXML private ComboBox<String> endTimeCombo;
    @FXML private TextField priceField;
    @FXML private ToggleButton remoteBtn;
    @FXML private ToggleButton inPersonBtn;
    @FXML private Label errorLabel;
    @FXML private Button submitBtn;

    private LocalDate selectedDate;
    private Runnable onLessonCreated;
    private final CreateLessonController createLessonController = new CreateLessonController();

    @FXML
    public void initialize() {
        applyRoundedClip();

        ToggleGroup modeGroup = new ToggleGroup();
        remoteBtn.setToggleGroup(modeGroup);
        inPersonBtn.setToggleGroup(modeGroup);
        remoteBtn.setSelected(true);
        // Prevent deselecting both
        modeGroup.selectedToggleProperty().addListener((obs, old, newT) -> {
            if (newT == null && old != null) old.setSelected(true);
        });

        populateTimeCombos();
        loadExpertises();
    }

    private void applyRoundedClip() {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        dialogRoot.layoutBoundsProperty().addListener((obs, o, n) -> {
            if (n.getWidth() > 0 && dialogRoot.getClip() == null) {
                clip.setWidth(n.getWidth());
                clip.setHeight(n.getHeight());
                dialogRoot.widthProperty().addListener((o2, ov, nv) -> clip.setWidth(nv.doubleValue()));
                dialogRoot.heightProperty().addListener((o2, ov, nv) -> clip.setHeight(nv.doubleValue()));
                dialogRoot.setClip(clip);
            }
        });
    }

    public void initDate(LocalDate date) {
        this.selectedDate = date;
        dateLabel.setText(date.format(DATE_FMT));
    }

    public void setOnLessonCreated(Runnable callback) {
        this.onLessonCreated = callback;
    }

    private void populateTimeCombos() {
        for (int h = 7; h <= 21; h++) {
            String slot = String.format("%02d:00", h);
            startTimeCombo.getItems().add(slot);
            endTimeCombo.getItems().add(slot);
        }
        startTimeCombo.getSelectionModel().select("09:00");
        endTimeCombo.getSelectionModel().select("10:00");
    }

    private void loadExpertises() {
        String token = SceneManager.getInstance().getSessionToken();
        Task<List<TutorExpertise>> task = new Task<>() {
            @Override
            protected List<TutorExpertise> call() {
                return createLessonController.loadApprovedExpertises(token);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<TutorExpertise> expertises = task.getValue();
            for (TutorExpertise ex : expertises) {
                subjectCombo.getItems().add(ex.getSubcategory().getName());
            }
            if (!expertises.isEmpty()) {
                subjectCombo.getSelectionModel().selectFirst();
                // Always show the hint so tutors know how to add new subjects
                subjectHintLabel.setVisible(true);
                subjectHintLabel.setManaged(true);
            } else {
                showError("You have no approved subjects yet. Apply for a new expertise and wait for admin approval before creating lessons.");
                submitBtn.setDisable(true);
                subjectHintLabel.setVisible(true);
                subjectHintLabel.setManaged(true);
            }
        }));
        task.setOnFailed(e ->
                LOGGER.warning("Cannot load expertises: " + task.getException().getMessage()));
        Thread t = new Thread(task, "load-expertises");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleSubmit() {
        showError(null);

        String subject  = subjectCombo.getValue();
        String startStr = startTimeCombo.getValue();
        String endStr   = endTimeCombo.getValue();
        String priceStr = priceField.getText().trim().replace(",", ".");

        if (subject == null || subject.isBlank())  { showError("Please select a subject."); return; }
        if (selectedDate == null)                   { showError("Invalid date."); return; }
        if (startStr == null || endStr == null)     { showError("Please select a start and end time."); return; }
        if (priceStr.isBlank())                     { showError("Please enter a price."); return; }

        BigDecimal price;
        try {
            price = new BigDecimal(priceStr);
            if (price.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showError("Invalid price. Enter a positive number (e.g. 35.00).");
            return;
        }

        LocalDateTime start = LocalDateTime.of(selectedDate, LocalTime.parse(startStr));
        LocalDateTime end   = LocalDateTime.of(selectedDate, LocalTime.parse(endStr));
        if (!end.isAfter(start)) {
            showError("End time must be after start time.");
            return;
        }

        boolean isRemote = remoteBtn.isSelected();
        String token = SceneManager.getInstance().getSessionToken();

        LessonTutorBean bean = new LessonTutorBean();
        bean.setSubcategoryName(subject);
        bean.setStartTime(start);
        bean.setEndTime(end);
        bean.setRemote(isRemote);
        bean.setListedPrice(price);

        submitBtn.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                createLessonController.createLesson(bean, token);
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (bean.getErrorMessage() != null) {
                showError(bean.getErrorMessage());
                submitBtn.setDisable(false);
            } else {
                if (onLessonCreated != null) onLessonCreated.run();
                closeDialog();
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            showError("Unexpected error. Please try again.");
            submitBtn.setDisable(false);
        }));

        Thread t = new Thread(task, "create-lesson");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleClose() {
        closeDialog();
    }

    private void closeDialog() {
        ((Stage) submitBtn.getScene().getWindow()).close();
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
}
