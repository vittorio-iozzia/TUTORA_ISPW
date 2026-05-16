package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.bean.ApplicationItemBean;
import it.ispw.tutora.bean.CategoryBean;
import it.ispw.tutora.bean.RequirementBean;
import it.ispw.tutora.bean.TutorApplicationBean;
import it.ispw.tutora.controller.application.ApplyToBecomeATutorController;
import it.ispw.tutora.enums.ItemType;
import it.ispw.tutora.exception.*;
import it.ispw.tutora.view.SceneManager;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Controller grafico per UC-2: Apply to Become a Tutor.
 *
 * -----------------------------------------------------------------------
 * Flusso in due fasi
 * -----------------------------------------------------------------------
 * Fase 1 – Selezione categoria:
 *   Mostra la ListView delle categorie disponibili.
 *   Al click su "Next" carica i requisiti della categoria selezionata.
 *
 * Fase 2 – Compilazione form:
 *   Costruisce dinamicamente i campi in base ai requisiti (TEXT o DOCUMENT).
 *   Al click su "Submit" raccoglie i dati, valida client-side e delega
 *   la sottomissione ad ApplyToBecomeATutorController su un thread separato
 *   per non bloccare il thread JavaFX durante la validazione certificati.
 *
 * -----------------------------------------------------------------------
 * Thread management
 * -----------------------------------------------------------------------
 * submitApplication() può richiedere fino a 3 minuti (timer certificati).
 * Viene eseguito su un Task JavaFX: il thread FX rimane responsivo e
 * il pulsante Submit viene disabilitato per tutta la durata dell'operazione.
 */
public class ApplyToBecomeATutorGfxController {

    private static final Logger LOGGER = Logger.getLogger(
            ApplyToBecomeATutorGfxController.class.getName());
    private static final String FIELD_PREFIX = "Field \"";
    private static final String CHARACTERS   = " characters";

    // ----------------------------------------------------------------
    // FXML – Fase 1: selezione categoria
    // ----------------------------------------------------------------

    @FXML private VBox categorySection;
    @FXML private ListView<String> categoryListView;
    @FXML private Label categoryDescriptionLabel;
    @FXML private Label categoryErrorLabel;
    @FXML private Button nextButton;

    // ----------------------------------------------------------------
    // FXML – Fase 2: form requisiti
    // ----------------------------------------------------------------

    @FXML private VBox formSection;
    @FXML private Label selectedCategoryLabel;
    @FXML private VBox formContent;
    @FXML private Label messageLabel;
    @FXML private Button submitButton;

    // ----------------------------------------------------------------
    // Stato interno
    // ----------------------------------------------------------------

    private final ApplyToBecomeATutorController controller =
            new ApplyToBecomeATutorController();

    private List<RequirementBean> requirements;

    // requirementName → TextArea (TEXT) o Label placeholder (DOCUMENT)
    private final Map<String, TextArea> textFieldMap = new HashMap<>();

    // requirementName → contenuto file (DOCUMENT)
    private final Map<String, byte[]> fileContentMap = new HashMap<>();
    private final Map<String, String> fileNameMap = new HashMap<>();
    private final Map<String, String> fileMimeMap = new HashMap<>();
    private final Map<String, Long> fileSizeMap = new HashMap<>();
    // ----------------------------------------------------------------
    // Inizializzazione
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        categoryErrorLabel.setVisible(false);
        messageLabel.setVisible(false);
        formSection.setVisible(false);
        formSection.setManaged(false);

        loadCategories();
    }

    private void loadCategories() {
        try {
            List<CategoryBean> categories = controller.loadCategories();
            List<String> names = categories.stream()
                    .map(CategoryBean::getName)
                    .toList();
            categoryListView.setItems(FXCollections.observableArrayList(names));

            // Mostra descrizione categoria al cambio selezione
            categoryListView.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldVal, newName) ->
                        categories.stream()
                                .filter(c -> c.getName().equals(newName))
                                .findFirst()
                                .ifPresent(c -> categoryDescriptionLabel.setText(c.getDescription())));

        } catch (DatabaseException e) {
            LOGGER.warning("Cannot load categories: " + e.getMessage());
            categoryErrorLabel.setText("Cannot load categories. Please try again.");
            categoryErrorLabel.setVisible(true);
        }
    }

    // ----------------------------------------------------------------
    // Fase 1 → Fase 2
    // ----------------------------------------------------------------

    @FXML
    public void handleNext() {
        String selectedCategory = categoryListView.getSelectionModel().getSelectedItem();
        if (selectedCategory == null) {
            categoryErrorLabel.setText("Please select a category.");
            categoryErrorLabel.setVisible(true);
            return;
        }

        try {
            requirements = controller.loadRequirements(selectedCategory);
        } catch (CategoryNotFoundException e) {
            categoryErrorLabel.setText("Category not found. Please reload.");
            categoryErrorLabel.setVisible(true);
            return;
        } catch (DatabaseException e) {
            categoryErrorLabel.setText("Cannot load requirements. Please try again.");
            categoryErrorLabel.setVisible(true);
            return;
        }

        selectedCategoryLabel.setText(selectedCategory);
        buildForm(requirements);

        categorySection.setVisible(false);
        categorySection.setManaged(false);
        formSection.setVisible(true);
        formSection.setManaged(true);
    }

    @FXML
    public void handleBack() {
        textFieldMap.clear();
        fileContentMap.clear();
        fileNameMap.clear();
        fileMimeMap.clear();
        fileSizeMap.clear();
        formContent.getChildren().clear();

        formSection.setVisible(false);
        formSection.setManaged(false);
        categorySection.setVisible(true);
        categorySection.setManaged(true);
        messageLabel.setVisible(false);
    }

    // ----------------------------------------------------------------
    // Costruzione dinamica del form
    // ----------------------------------------------------------------

    private void buildForm(List<RequirementBean> reqs) {
        formContent.getChildren().clear();
        textFieldMap.clear();
        fileContentMap.clear();
        fileNameMap.clear();
        fileMimeMap.clear();
        fileSizeMap.clear();

        for (RequirementBean req : reqs) {
            VBox fieldBox = buildFieldBox(req);
            formContent.getChildren().add(fieldBox);
        }
    }

    private VBox buildFieldBox(RequirementBean req) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(0, 0, 8, 0));

        // Label titolo (obbligatorio segnato con *)
        String labelText = req.isRequired()
                ? req.getLabel() + " *"
                : req.getLabel();
        Label title = new Label(labelText);
        title.setStyle("-fx-font-weight: bold;");
        box.getChildren().add(title);

        // Descrizione opzionale
        if (req.getDescription() != null && !req.getDescription().isBlank()) {
            Label desc = new Label(req.getDescription());
            desc.setStyle("-fx-font-size: 11; -fx-text-fill: #888888;");
            desc.setWrapText(true);
            box.getChildren().add(desc);
        }

        if (req.getItemType() == ItemType.TEXT) {
            box.getChildren().add(buildTextArea(req));
        } else {
            box.getChildren().add(buildFileRow(req.getName()));
        }

        return box;
    }

    private TextArea buildTextArea(RequirementBean req) {
        TextArea ta = new TextArea();
        ta.setWrapText(true);
        ta.setPrefRowCount(4);

        String prompt = buildTextPrompt(req);
        ta.setPromptText(prompt);

        textFieldMap.put(req.getName(), ta);
        return ta;
    }

    private String buildTextPrompt(RequirementBean req) {
        if (req.getMinChar() > 0 && req.getMaxLength() > 0) {
            return "Between " + req.getMinChar() + " and " + req.getMaxLength() + CHARACTERS;
        } else if (req.getMaxLength() > 0) {
            return "Max " + req.getMaxLength() + CHARACTERS;
        } else if (req.getMinChar() > 0) {
            return "At least " + req.getMinChar() + CHARACTERS;
        }
        return "";
    }

    private HBox buildFileRow(String reqName) {
        HBox row = new HBox(10);
        row.setStyle("-fx-alignment: CENTER_LEFT;");

        Button chooseBtn = new Button("Choose file…");
        Label fileLabel = new Label("No file selected");
        fileLabel.setStyle("-fx-text-fill: #666666;");

        chooseBtn.setOnAction(e -> handleFileChooser(reqName, fileLabel, chooseBtn));
        row.getChildren().addAll(chooseBtn, fileLabel);
        return row;
    }

    // ----------------------------------------------------------------
    // File chooser
    // ----------------------------------------------------------------

    private void handleFileChooser(String reqName, Label fileLabel, Button chooseBtn) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select document for: " + reqName);
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.jpg", "*.jpeg", "*.png"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );

        File file = fc.showOpenDialog(chooseBtn.getScene().getWindow());
        if (file == null) return;

        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String mime = Files.probeContentType(file.toPath());

            fileContentMap.put(reqName, bytes);
            fileNameMap.put(reqName, file.getName());
            fileSizeMap.put(reqName, file.length());
            fileMimeMap.put(reqName, mime != null ? mime : "application/octet-stream");

            fileLabel.setText(file.getName());
            fileLabel.setStyle("-fx-text-fill: #2e7d32;");
        } catch (IOException e) {
            fileLabel.setText("Error reading file.");
            fileLabel.setStyle("-fx-text-fill: red;");
        }
    }

    // ----------------------------------------------------------------
    // Fase 2 – Submit
    // ----------------------------------------------------------------

    @FXML
    public void handleSubmit() {
        TutorApplicationBean appBean = collectBean();
        if (appBean == null) return; // validazione client-side fallita

        String token = SceneManager.getInstance().getSessionToken();
        submitButton.setDisable(true);
        showMessage("Submitting application — this may take a few minutes…", false);

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call()
                    throws AuthenticationException,
                    ValidationTimeoutException,
                    ValidationServiceException,
                    InvalidDocumentException,
                    DuplicateApplicationException,
                    DatabaseException {
                return controller.submitApplication(appBean, token);
            }
        };

        task.setOnSucceeded(e -> {
            int id = task.getValue();
            showMessage("Application submitted successfully! (ID: " + id + ")", false);
            submitButton.setDisable(false);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            submitButton.setDisable(false);
            handleSubmitError(ex);
        });

        Thread taskThread = new Thread(task);
        taskThread.setDaemon(true);
        taskThread.start();
    }

    /**
     * Raccoglie i dati dal form e li mette nel Bean.
     * Restituisce null se la validazione client-side fallisce.
     */
    private TutorApplicationBean collectBean() {
        List<ApplicationItemBean> items = new ArrayList<>();
        for (RequirementBean req : requirements) {
            ApplicationItemBean item = collectItem(req);
            if (item == null) return null;
            if (isItemNonEmpty(item)) items.add(item);
        }
        TutorApplicationBean bean = new TutorApplicationBean();
        bean.setCategoryName(selectedCategoryLabel.getText());
        bean.setItems(items);
        return bean;
    }

    private ApplicationItemBean collectItem(RequirementBean req) {
        return req.getItemType() == ItemType.TEXT
                ? collectTextItem(req)
                : collectDocumentItem(req);
    }

    private boolean isItemNonEmpty(ApplicationItemBean item) {
        return item.getItemType() == ItemType.TEXT
                ? !item.getTextContent().isEmpty()
                : item.getContent() != null;
    }

    private ApplicationItemBean collectTextItem(RequirementBean req) {
        TextArea ta = textFieldMap.get(req.getName());
        String text = ta != null ? ta.getText().trim() : "";

        if (req.isRequired() && text.isEmpty()) {
            showMessage(FIELD_PREFIX + req.getLabel() + "\" is required.", true);
            return null;
        }
        if (req.getMinChar() > 0 && !text.isEmpty() && text.length() < req.getMinChar()) {
            showMessage(FIELD_PREFIX + req.getLabel() + "\" needs at least "
                    + req.getMinChar() + CHARACTERS + ".", true);
            return null;
        }
        if (req.getMaxLength() > 0 && text.length() > req.getMaxLength()) {
            showMessage(FIELD_PREFIX + req.getLabel() + "\" must not exceed "
                    + req.getMaxLength() + CHARACTERS + ".", true);
            return null;
        }

        ApplicationItemBean item = new ApplicationItemBean();
        item.setRequirementName(req.getName());
        item.setItemType(ItemType.TEXT);
        item.setTextContent(text);
        return item;
    }

    private ApplicationItemBean collectDocumentItem(RequirementBean req) {
        byte[] content = fileContentMap.get(req.getName());

        if (req.isRequired() && content == null) {
            showMessage("Document \"" + req.getLabel() + "\" is required.", true);
            return null;
        }

        ApplicationItemBean item = new ApplicationItemBean();
        item.setRequirementName(req.getName());
        item.setItemType(ItemType.DOCUMENT);
        item.setContent(content);
        item.setOriginalFilename(fileNameMap.get(req.getName()));
        item.setMimeType(fileMimeMap.get(req.getName()));
        Long size = fileSizeMap.get(req.getName());
        item.setSizeBytes(size != null ? size : 0L);
        return item;
    }

    // ----------------------------------------------------------------
    // Gestione errori dal Task
    // ----------------------------------------------------------------

    private void handleSubmitError(Throwable ex) {
        if (ex instanceof ValidationTimeoutException) {
            showMessage("Certificate validation timed out. Please try again.", true);
        } else if (ex instanceof ValidationServiceException) {
            showMessage("Validation service error. Please try again later.", true);
        } else if (ex instanceof InvalidDocumentException) {
            showMessage("One or more documents are invalid. Please resubmit.", true);
        } else if (ex instanceof DuplicateApplicationException) {
            showMessage("You already have an active application for this category.", true);
        } else if (ex instanceof AuthenticationException) {
            showMessage("Session expired. Please log in again.", true);
            SceneManager.getInstance().showLogin();
        } else {
            LOGGER.severe("Unexpected submit error: " + ex.getMessage());
            showMessage("An unexpected error occurred. Please try again.", true);
        }
    }

    // ----------------------------------------------------------------
    // Utility UI
    // ----------------------------------------------------------------

    private void showMessage(String text, boolean isError) {
        messageLabel.setText(text);
        messageLabel.setStyle(isError
                ? "-fx-text-fill: #c62828;"
                : "-fx-text-fill: #2e7d32;");
        messageLabel.setVisible(true);
    }
}
