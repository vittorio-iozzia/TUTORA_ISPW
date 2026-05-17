package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.bean.ApplicationItemBean;
import it.ispw.tutora.bean.ApplicationReviewBean;
import it.ispw.tutora.bean.TutorApplicationBean;
import it.ispw.tutora.controller.application.ApplyToBecomeATutorController;
import it.ispw.tutora.enums.ApplicationStatus;
import it.ispw.tutora.enums.ItemType;
import it.ispw.tutora.view.SceneManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.logging.Logger;

public class ApplicationReviewGfxController {

    private static final Logger LOGGER =
            Logger.getLogger(ApplicationReviewGfxController.class.getName());
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    @FXML private VBox      dialogRoot;
    @FXML private Label     headerSubtitle;
    @FXML private Label     applicantLabel;
    @FXML private Label     dateLabel;
    @FXML private VBox      itemsContainer;
    @FXML private TextArea  adminNotesField;
    @FXML private Label     errorLabel;
    @FXML private Button    approveBtn;
    @FXML private Button    rejectBtn;

    private TutorApplicationBean applicationBean;
    private String token;
    private Runnable onEvaluated;
    private final ApplyToBecomeATutorController appController =
            new ApplyToBecomeATutorController();

    @FXML
    public void initialize() {
        applyRoundedClip();
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

    public void initApplication(TutorApplicationBean bean) {
        this.applicationBean = bean;
        this.token = SceneManager.getInstance().getSessionToken();

        String category = bean.getCategoryName();
        String sub = bean.getSubcategoryName();
        headerSubtitle.setText(sub != null && !sub.isBlank()
                ? category + " — " + sub
                : category);

        applicantLabel.setText("Applicant: " + bean.getStudentUsername());
        dateLabel.setText(bean.getCreationDate() != null
                ? bean.getCreationDate().format(DATE_FMT)
                : "");

        buildItems(bean);
    }

    public void setOnEvaluated(Runnable callback) {
        this.onEvaluated = callback;
    }

    private void buildItems(TutorApplicationBean bean) {
        itemsContainer.getChildren().clear();
        if (bean.getItems() == null || bean.getItems().isEmpty()) {
            Label empty = new Label("No form items available.");
            empty.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px;");
            itemsContainer.getChildren().add(empty);
            return;
        }
        for (ApplicationItemBean item : bean.getItems()) {
            String key = item.getRequirementName();
            if ("hourly_price".equals(key)) {
                itemsContainer.getChildren().add(buildTextItem("Hourly Rate (€)", item.getTextContent()));
            } else if ("availability".equals(key)) {
                // skip legacy availability items
            } else if (item.getItemType() == ItemType.TEXT) {
                itemsContainer.getChildren().add(buildTextItem(formatLabel(key), item.getTextContent()));
            } else {
                itemsContainer.getChildren().add(buildDocItem(formatLabel(key), item.getOriginalFilename(), item.getSizeBytes()));
            }
        }
    }

    private VBox buildTextItem(String label, String content) {
        VBox box = new VBox(6);
        box.getStyleClass().add("app-field-box");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("app-field-label");
        Label val = new Label(content != null ? content : "—");
        val.setWrapText(true);
        val.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");
        box.getChildren().addAll(lbl, val);
        return box;
    }

    private VBox buildDocItem(String label, String filename, long sizeBytes) {
        VBox box = new VBox(6);
        box.getStyleClass().add("app-field-box");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("app-field-label");
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("📄");
        Label name = new Label(filename != null ? filename : "No file");
        name.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");
        String sizeStr = sizeBytes > 0
                ? " (" + (sizeBytes / 1024) + " KB)"
                : "";
        Label size = new Label(sizeStr);
        size.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
        row.getChildren().addAll(icon, name, size);
        box.getChildren().addAll(lbl, row);
        return box;
    }

    private String formatLabel(String key) {
        if (key == null) return "—";
        return key.replace("_", " ")
                .substring(0, 1).toUpperCase()
                + key.replace("_", " ").substring(1);
    }

    @FXML
    private void handleApprove() {
        doEvaluate(ApplicationStatus.ACCEPTED);
    }

    @FXML
    private void handleReject() {
        doEvaluate(ApplicationStatus.REJECTED);
    }

    @FXML
    private void handleClose() {
        ((Stage) dialogRoot.getScene().getWindow()).close();
    }

    private void doEvaluate(ApplicationStatus status) {
        showError(null);
        approveBtn.setDisable(true);
        rejectBtn.setDisable(true);

        ApplicationReviewBean bean = new ApplicationReviewBean();
        bean.setApplicationId(applicationBean.getApplicationId());
        bean.setStatus(status);
        bean.setAdminNotes(adminNotesField.getText().trim());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                appController.evaluateApplication(bean, token);
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (onEvaluated != null) onEvaluated.run();
            ((Stage) dialogRoot.getScene().getWindow()).close();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            showError("Error: " + (task.getException() != null
                    ? task.getException().getMessage() : "Unknown error."));
            approveBtn.setDisable(false);
            rejectBtn.setDisable(false);
        }));

        Thread t = new Thread(task, status == ApplicationStatus.ACCEPTED ? "app-approve" : "app-reject");
        t.setDaemon(true);
        t.start();
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
