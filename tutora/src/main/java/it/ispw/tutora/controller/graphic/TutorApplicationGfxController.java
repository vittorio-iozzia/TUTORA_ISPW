package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.bean.ApplicationItemBean;
import it.ispw.tutora.bean.RequirementBean;
import it.ispw.tutora.bean.TutorApplicationBean;
import it.ispw.tutora.controller.application.ApplyToBecomeATutorController;
import it.ispw.tutora.enums.ItemType;
import it.ispw.tutora.exception.*;
import it.ispw.tutora.view.SceneManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

public class TutorApplicationGfxController {

    private static final Logger LOGGER =
            Logger.getLogger(TutorApplicationGfxController.class.getName());

    // ----------------------------------------------------------------
    // Availability config
    // ----------------------------------------------------------------

    private static final String[] DAYS     = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    /** Full DayOfWeek names so the backend can parse with DayOfWeek.valueOf() */
    private static final String[] DAY_KEYS = {
        "MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"};
    private static final String[] SLOTS     = {"Morning\n08:00–12:00", "Afternoon\n12:00–17:00", "Evening\n17:00–21:00"};
    /** Start times – parseable with LocalTime.parse() */
    private static final String[] SLOT_START = {"08:00", "12:00", "17:00"};
    /** End times – parseable with LocalTime.parse() */
    private static final String[] SLOT_END   = {"12:00", "17:00", "21:00"};

    // ----------------------------------------------------------------
    // FXML
    // ----------------------------------------------------------------

    @FXML private VBox      dialogRoot;
    @FXML private Label     titleLabel;
    @FXML private Label     subtitleLabel;
    @FXML private Label     applyingForLabel;
    @FXML private VBox      formContainer;
    @FXML private VBox      successPane;
    @FXML private VBox      footer;
    @FXML private Button    submitBtn;
    @FXML private Button    backBtn;
    @FXML private Label     errorLabel;
    @FXML private CheckBox  agreeCheckBox;
    @FXML private Label     agreeLabel;

    @FXML private StackPane headerIconWrap;
    @FXML private ImageView headerIconView;
    @FXML private StackPane bannerIconWrap;
    @FXML private ImageView bannerIconView;
    @FXML private StackPane successIconWrap;
    @FXML private ImageView successIconView;

    // ----------------------------------------------------------------
    // State
    // ----------------------------------------------------------------

    private String categoryName;
    private List<RequirementBean> requirements = List.of();

    private final Map<String, TextArea> textFields    = new LinkedHashMap<>();
    private final Map<String, File[]>   documentFiles = new LinkedHashMap<>();

    /** Selected availability slots: "MON_MORNING", "WED_EVENING", etc. */
    private final Set<String> selectedSlots = new LinkedHashSet<>();

    private final ApplyToBecomeATutorController appController =
            new ApplyToBecomeATutorController();

    // ----------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------

    @FXML
    private void initialize() {
        setupIconBox(headerIconWrap, headerIconView, "1f393", 22); // 🎓 graduation cap
        setupIconBox(bannerIconWrap, bannerIconView, "1f3f7", 13); // 🏷️ tag
        setupIconBox(successIconWrap, successIconView, "2705", 34); // ✅ check
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

    public void initCategory(String categoryName) {
        this.categoryName = categoryName;

        titleLabel.setText("Apply as " + categoryName + " Tutor");
        subtitleLabel.setText(
                "Fill out the form below to apply as a tutor in the " + categoryName + " category.");
        applyingForLabel.setText("Applying for: " + categoryName + " Category");

        agreeLabel.setText(
                "I agree to the Tutor Terms of Service and understand that my application for the "
                + categoryName + " category will be reviewed by the TUTORA team.");
        agreeCheckBox.selectedProperty().addListener((obs, o, n) -> updateSubmitState());

        try {
            requirements = appController.loadRequirements(categoryName);
            buildForm();
        } catch (Exception e) {
            LOGGER.warning("Cannot load requirements for " + categoryName + ": " + e.getMessage());
            Label fallback = new Label("Unable to load requirements. Please try again later.");
            fallback.setStyle("-fx-text-fill: #9C2121; -fx-font-size: 13px;");
            formContainer.getChildren().add(fallback);
        }
    }

    // ----------------------------------------------------------------
    // Form builder
    // ----------------------------------------------------------------

    private void buildForm() {
        formContainer.getChildren().clear();
        textFields.clear();
        documentFiles.clear();

        if (requirements.isEmpty()) {
            Label empty = new Label("No requirements found for this category.");
            empty.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
            formContainer.getChildren().add(empty);
        } else {
            int step = 1;
            for (RequirementBean req : requirements) {
                formContainer.getChildren().add(buildFieldBox(req, step++));
            }
        }

        formContainer.getChildren().add(buildAvailabilitySection());
        updateSubmitState();
    }

    private VBox buildFieldBox(RequirementBean req, int step) {
        VBox box = new VBox(10);
        box.getStyleClass().add("app-field-box");

        HBox labelRow = new HBox(8);
        labelRow.setAlignment(Pos.CENTER_LEFT);

        Label stepLbl = new Label(String.valueOf(step));
        stepLbl.getStyleClass().add("app-step-badge");

        Label fieldLabel = new Label(req.getLabel());
        fieldLabel.getStyleClass().add("app-field-label");
        labelRow.getChildren().addAll(stepLbl, fieldLabel);

        if (req.isRequired()) {
            Label star = new Label("*");
            star.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 14px;");
            labelRow.getChildren().add(star);
        } else {
            Label opt = new Label("(optional)");
            opt.getStyleClass().add("app-optional-label");
            labelRow.getChildren().add(opt);
        }
        box.getChildren().add(labelRow);

        if (req.getDescription() != null && !req.getDescription().isBlank()) {
            Label desc = new Label(req.getDescription());
            desc.getStyleClass().add("app-field-desc");
            desc.setWrapText(true);
            box.getChildren().add(desc);
        }

        if (req.getItemType() == ItemType.TEXT) {
            box.getChildren().add(buildTextArea(req));
        } else {
            box.getChildren().add(buildFileRow(req));
        }
        return box;
    }

    private TextArea buildTextArea(RequirementBean req) {
        TextArea ta = new TextArea();
        ta.setPromptText("Write here…");
        ta.setPrefRowCount(4);
        ta.setWrapText(true);
        ta.getStyleClass().add("app-text-area");
        if (req.getMaxLength() > 0) {
            ta.setTextFormatter(new TextFormatter<>(change ->
                    change.getControlNewText().length() > req.getMaxLength() ? null : change));
        }
        ta.textProperty().addListener((obs, o, n) -> updateSubmitState());
        textFields.put(req.getName(), ta);
        return ta;
    }

    private HBox buildFileRow(RequirementBean req) {
        File[] holder = new File[1];
        documentFiles.put(req.getName(), holder);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        // 📂 open folder – classic browse icon, yellow/warm on the light outlined button
        ImageView uploadIcon = loadTwemoji("1f4c2", 15);

        Button pickBtn = new Button("Choose file…");
        pickBtn.setGraphic(uploadIcon);
        pickBtn.getStyleClass().add("app-file-btn");

        Label fileNameLbl = new Label("No file selected");
        fileNameLbl.getStyleClass().add("app-filename-label");

        pickBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select " + req.getLabel());
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "Documents", "*.pdf", "*.doc", "*.docx", "*.jpg", "*.png"));
            File chosen = fc.showOpenDialog(submitBtn.getScene().getWindow());
            if (chosen != null) {
                holder[0] = chosen;
                fileNameLbl.setText(chosen.getName());
                fileNameLbl.setStyle("-fx-text-fill: #2A5C45; -fx-font-size: 12px; -fx-font-weight: 600;");
                updateSubmitState();
            }
        });
        row.getChildren().addAll(pickBtn, fileNameLbl);
        return row;
    }

    // ----------------------------------------------------------------
    // Availability calendar
    // ----------------------------------------------------------------

    private VBox buildAvailabilitySection() {
        VBox section = new VBox(12);
        section.getStyleClass().add("app-field-box");

        // Section header row
        HBox labelRow = new HBox(8);
        labelRow.setAlignment(Pos.CENTER_LEFT);

        // 📅 calendar emoji via Twemoji CDN – guaranteed rendering
        ImageView calIcon = loadTwemoji("1f4c5", 22);

        Label title = new Label("Availability");
        title.getStyleClass().add("app-field-label");

        Label star = new Label("*");
        star.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 14px;");

        labelRow.getChildren().addAll(calIcon, title, star);

        Label desc = new Label("Select the days and time slots when you are available to teach.");
        desc.getStyleClass().add("app-field-desc");
        desc.setWrapText(true);

        // Grid
        GridPane grid = buildAvailabilityGrid();

        section.getChildren().addAll(labelRow, desc, grid);
        return section;
    }

    private GridPane buildAvailabilityGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);

        // Column constraints: first col for time labels, rest for days
        ColumnConstraints timeCol = new ColumnConstraints(70);
        grid.getColumnConstraints().add(timeCol);
        for (int d = 0; d < DAYS.length; d++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setMinWidth(52);
            grid.getColumnConstraints().add(cc);
        }

        // Day headers (row 0, cols 1-7)
        for (int d = 0; d < DAYS.length; d++) {
            Label dayLbl = new Label(DAYS[d]);
            dayLbl.getStyleClass().add("app-avail-day-header");
            dayLbl.setMaxWidth(Double.MAX_VALUE);
            dayLbl.setAlignment(Pos.CENTER);
            GridPane.setHgrow(dayLbl, Priority.ALWAYS);
            grid.add(dayLbl, d + 1, 0);
        }

        // Time slot rows
        for (int s = 0; s < SLOTS.length; s++) {
            // Time label
            Label slotLbl = new Label(SLOTS[s]);
            slotLbl.getStyleClass().add("app-avail-time-label");
            slotLbl.setWrapText(true);
            slotLbl.setAlignment(Pos.CENTER_RIGHT);
            grid.add(slotLbl, 0, s + 1);

            // Toggle buttons for each day
            for (int d = 0; d < DAYS.length; d++) {
                // key format: "MONDAY|08:00|12:00" — parseable as DayOfWeek + LocalTime range
                final String key = DAY_KEYS[d] + "|" + SLOT_START[s] + "|" + SLOT_END[s];
                ToggleButton cell = new ToggleButton();
                cell.getStyleClass().add("app-avail-cell");
                cell.setMaxWidth(Double.MAX_VALUE);
                cell.setMaxHeight(Double.MAX_VALUE);
                GridPane.setHgrow(cell, Priority.ALWAYS);
                GridPane.setVgrow(cell, Priority.ALWAYS);

                cell.selectedProperty().addListener((obs, was, on) -> {
                    if (Boolean.TRUE.equals(on)) selectedSlots.add(key);
                    else                         selectedSlots.remove(key);
                    updateSubmitState();
                });

                grid.add(cell, d + 1, s + 1);
            }
        }

        return grid;
    }

    // ----------------------------------------------------------------
    // Submit state
    // ----------------------------------------------------------------

    private void updateSubmitState() {
        submitBtn.setDisable(!isFormComplete());
    }

    private boolean isFormComplete() {
        for (RequirementBean req : requirements) {
            if (!req.isRequired()) continue;
            if (req.getItemType() == ItemType.TEXT) {
                TextArea ta = textFields.get(req.getName());
                if (ta == null || ta.getText().isBlank()) return false;
            } else {
                File[] holder = documentFiles.get(req.getName());
                if (holder == null || holder[0] == null) return false;
            }
        }
        return !selectedSlots.isEmpty() && agreeCheckBox.isSelected();
    }

    // ----------------------------------------------------------------
    // FXML handlers
    // ----------------------------------------------------------------

    @FXML
    private void handleSubmit() {
        if (selectedSlots.isEmpty()) {
            showError("Please select at least one availability slot before submitting.");
            return;
        }
        showError(null);
        submitBtn.setDisable(true);
        submitBtn.setText("Submitting…");

        String token = SceneManager.getInstance().getSessionToken();
        TutorApplicationBean bean = buildBean();

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return appController.submitApplication(bean, token);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(this::showSuccess));
        task.setOnFailed(e -> {
            String msg = resolveErrorMessage(task.getException());
            Platform.runLater(() -> {
                showError(msg);
                submitBtn.setDisable(false);
                submitBtn.setText("Submit application");
            });
        });

        Thread t = new Thread(task, "tutor-application-submit");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleClose() {
        ((Stage) successPane.getScene().getWindow()).close();
    }

    @FXML
    private void handleBack() {
        ((Stage) submitBtn.getScene().getWindow()).close();
    }

    // ----------------------------------------------------------------
    // Bean assembly
    // ----------------------------------------------------------------

    private TutorApplicationBean buildBean() {
        TutorApplicationBean bean = new TutorApplicationBean();
        bean.setCategoryName(categoryName);

        List<ApplicationItemBean> items = new ArrayList<>();
        for (RequirementBean req : requirements) {
            ApplicationItemBean item = new ApplicationItemBean();
            item.setRequirementName(req.getName());
            item.setItemType(req.getItemType());

            if (req.getItemType() == ItemType.TEXT) {
                TextArea ta = textFields.get(req.getName());
                item.setTextContent(ta != null ? ta.getText().trim() : "");
            } else {
                populateDocumentItem(item, req.getName());
            }
            items.add(item);
        }
        bean.setItems(items);

        // Availability as a TEXT item (e.g. "MON_MORNING,WED_AFTERNOON")
        if (!selectedSlots.isEmpty()) {
            ApplicationItemBean avail = new ApplicationItemBean();
            avail.setRequirementName("availability");
            avail.setItemType(ItemType.TEXT);
            avail.setTextContent(String.join(",", selectedSlots));
            items.add(avail);
        }

        return bean;
    }

    private void populateDocumentItem(ApplicationItemBean item, String reqName) {
        File[] holder = documentFiles.get(reqName);
        File f = holder != null ? holder[0] : null;
        if (f == null) return;
        item.setOriginalFilename(f.getName());
        item.setDocumentPath(f.getAbsolutePath());
        item.setSizeBytes(f.length());
        try {
            item.setContent(Files.readAllBytes(f.toPath()));
            item.setMimeType(Files.probeContentType(f.toPath()));
        } catch (IOException ex) {
            LOGGER.warning("Cannot read file: " + ex.getMessage());
        }
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

    private String resolveErrorMessage(Throwable e) {
        if (e instanceof DuplicateApplicationException)
            return "You have already submitted an application for this category.";
        if (e instanceof InvalidDocumentException)
            return "One or more documents are invalid. Please re-upload and try again.";
        if (e instanceof ValidationTimeoutException)
            return "Validation timed out. Please try again later.";
        if (e instanceof AuthenticationException)
            return "Your session has expired. Please log in again.";
        return "An error occurred while submitting. Please try again later.";
    }

    // ----------------------------------------------------------------
    // Icon helper
    // ----------------------------------------------------------------

    /** Loads a Twemoji PNG by Unicode codepoint (e.g. "1f4c5" for 📅). */
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
