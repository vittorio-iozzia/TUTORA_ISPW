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
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

public class TutorApplicationGfxController extends DialogGfxController {

    private static final Logger LOGGER =
            Logger.getLogger(TutorApplicationGfxController.class.getName());

    private static final String HOURLY_PRICE_KEY = "hourly_price";

    // ----------------------------------------------------------------
    // FXML
    // ----------------------------------------------------------------

    @FXML private Label     titleLabel;
    @FXML private Label     subtitleLabel;
    @FXML private Label     applyingForLabel;
    @FXML private Button    submitBtn;
    @FXML private Button    backBtn;
    @FXML private CheckBox  agreeCheckBox;
    @FXML private Label     agreeLabel;

    // ----------------------------------------------------------------
    // State
    // ----------------------------------------------------------------

    private String categoryName;
    private List<RequirementBean> requirements = List.of();

    private final Map<String, TextArea> textFields    = new LinkedHashMap<>();
    private final Map<String, File[]>   documentFiles = new LinkedHashMap<>();

    private TextField hourlyRateField;

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

        int step = 1;
        if (requirements.isEmpty()) {
            Label empty = new Label("No requirements found for this category.");
            empty.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
            formContainer.getChildren().add(empty);
        } else {
            for (RequirementBean req : requirements) {
                formContainer.getChildren().add(buildFieldBox(req, step++));
            }
        }

        formContainer.getChildren().add(buildHourlyRateSection(step));
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
    // Hourly rate field
    // ----------------------------------------------------------------

    private VBox buildHourlyRateSection(int step) {
        VBox section = new VBox(10);
        section.getStyleClass().add("app-field-box");

        HBox labelRow = new HBox(8);
        labelRow.setAlignment(Pos.CENTER_LEFT);

        Label stepLbl = new Label(String.valueOf(step));
        stepLbl.getStyleClass().add("app-step-badge");

        Label title = new Label("Hourly Rate (€)");
        title.getStyleClass().add("app-field-label");

        Label star = new Label("*");
        star.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 14px;");

        labelRow.getChildren().addAll(stepLbl, title, star);

        Label desc = new Label("Set your hourly rate for this subject. You can update it later from your profile.");
        desc.getStyleClass().add("app-field-desc");
        desc.setWrapText(true);

        hourlyRateField = new TextField();
        hourlyRateField.setPromptText("e.g. 35.00");
        hourlyRateField.getStyleClass().add("lesson-field");
        hourlyRateField.textProperty().addListener((obs, o, n) -> updateSubmitState());

        section.getChildren().addAll(labelRow, desc, hourlyRateField);
        return section;
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
        if (hourlyRateField == null || hourlyRateField.getText().isBlank()) return false;
        try {
            BigDecimal price = new BigDecimal(hourlyRateField.getText().trim().replace(",", "."));
            if (price.compareTo(BigDecimal.ZERO) <= 0) return false;
        } catch (NumberFormatException e) {
            return false;
        }
        return agreeCheckBox.isSelected();
    }

    // ----------------------------------------------------------------
    // FXML handlers
    // ----------------------------------------------------------------

    @FXML
    private void handleSubmit() {
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
        ApplicationItemBean priceItem = new ApplicationItemBean();
        priceItem.setRequirementName(HOURLY_PRICE_KEY);
        priceItem.setItemType(ItemType.TEXT);
        priceItem.setTextContent(hourlyRateField.getText().trim().replace(",", "."));
        items.add(priceItem);

        bean.setItems(items);
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

    private String resolveErrorMessage(Throwable e) {
        if (e instanceof DuplicateApplicationException)
            return "You have already submitted a pending application for this subject in this category.";
        if (e instanceof InvalidDocumentException)
            return "One or more documents are invalid. Please re-upload and try again.";
        if (e instanceof ValidationTimeoutException)
            return "Validation timed out. Please try again later.";
        if (e instanceof AuthenticationException)
            return "Your session has expired. Please log in again.";
        return "An error occurred while submitting. Please try again later.";
    }
}
