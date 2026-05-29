package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.bean.ReviewBean;
import it.ispw.tutora.controller.application.LeaveAReviewController;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.view.SceneManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ReviewGfxController {

    private static final Logger LOGGER   = Logger.getLogger(ReviewGfxController.class.getName());
    private static final String STAR_OFF = "STAR_OFF";

    @FXML private VBox     dialogRoot;
    @FXML private Label    subtitleLabel;
    @FXML private HBox     starsBox;
    @FXML private TextArea commentArea;
    @FXML private Button   submitBtn;
    @FXML private Button   cancelBtn;
    @FXML private Label    errorLabel;
    @FXML private VBox     formBody;
    @FXML private VBox     successPane;
    @FXML private Label    successSubLabel;

    private Booking  booking;
    private int      selectedRating = 0;
    private Runnable onReviewSubmitted;
    private final List<Label> starLabels = new ArrayList<>();

    private final LeaveAReviewController reviewController = new LeaveAReviewController();

    @FXML
    public void initialize() {
        // Clip to rounded corners so children don't overflow the border-radius
        Rectangle clip = new Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        clip.widthProperty().bind(dialogRoot.widthProperty());
        clip.heightProperty().bind(dialogRoot.heightProperty());
        dialogRoot.setClip(clip);
    }

    public void initBooking(Booking booking, Runnable onSubmitted) {
        this.booking           = booking;
        this.onReviewSubmitted = onSubmitted;

        Tutor  tutor   = booking.getLesson().getExpertise().getTutor();
        String subject = booking.getLesson().getExpertise().getSubcategory().getName();
        subtitleLabel.setText("Rating for " + tutor.getFullName() + " · " + subject);

        buildStars();
    }

    private void buildStars() {
        for (int i = 1; i <= 5; i++) {
            final int value = i;
            Label star = new Label("★");
            star.getStyleClass().addAll("review-star", STAR_OFF);
            star.setOnMouseEntered(e -> highlightStars(value));
            star.setOnMouseExited (e -> highlightStars(selectedRating));
            star.setOnMouseClicked(e -> selectRating(value));
            starsBox.getChildren().add(star);
            starLabels.add(star);
        }
    }

    private void highlightStars(int upTo) {
        for (int i = 0; i < starLabels.size(); i++) {
            Label s = starLabels.get(i);
            s.getStyleClass().removeAll("review-star-on", STAR_OFF);
            s.getStyleClass().add(i < upTo ? "review-star-on" : STAR_OFF);
        }
    }

    private void selectRating(int rating) {
        selectedRating = rating;
        highlightStars(rating);
    }

    @FXML
    private void handleSubmit() {
        if (selectedRating == 0) {
            showError("Please select a star rating before submitting.");
            return;
        }

        submitBtn.setDisable(true);
        cancelBtn.setDisable(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        String token = SceneManager.getInstance().getSessionToken();
        Tutor  tutor = booking.getLesson().getExpertise().getTutor();

        ReviewBean bean = new ReviewBean();
        bean.setBookingId(booking.getId());
        bean.setTutorUsername(tutor.getUsername());
        bean.setRating(selectedRating);
        bean.setComment(commentArea.getText().trim());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                reviewController.submitReview(bean, token);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            if (bean.getErrorMessage() != null) {
                submitBtn.setDisable(false);
                cancelBtn.setDisable(false);
                showError(bean.getErrorMessage());
            } else {
                formBody.setVisible(false);
                formBody.setManaged(false);
                successPane.setVisible(true);
                successPane.setManaged(true);
                submitBtn.setVisible(false);
                submitBtn.setManaged(false);
                cancelBtn.setText("Close");
                cancelBtn.getStyleClass().setAll("review-close-btn");
                cancelBtn.setVisible(true);
                cancelBtn.setManaged(true);
                cancelBtn.setDisable(false);
                successSubLabel.setText(
                        "Thank you! Your review for " + tutor.getFullName() + " has been saved.");
                if (onReviewSubmitted != null) onReviewSubmitted.run();
            }
        });

        task.setOnFailed(e -> {
            submitBtn.setDisable(false);
            cancelBtn.setDisable(false);
            showError("Failed to submit review. Please try again.");
            LOGGER.warning("Review submission failed: " + task.getException().getMessage());
        });

        Thread t = new Thread(task, "submit-review");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleClose() {
        cancelBtn.getScene().getWindow().hide();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
