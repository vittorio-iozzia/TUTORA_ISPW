package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.dao.ReviewDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.exception.DuplicateReviewException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Review;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.SceneManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.time.LocalDateTime;
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

        String  token   = SceneManager.getInstance().getSessionToken();
        Session session = SessionManager.getInstance().getSession(token);
        Student student = (Student) session.getUser();
        Tutor   tutor   = booking.getLesson().getExpertise().getTutor();

        Review review = new Review.Builder()
                .id(0)
                .booking(booking)
                .student(student)
                .tutor(tutor)
                .rating(selectedRating)
                .comment(commentArea.getText().trim())
                .createdAt(LocalDateTime.now())
                .build();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ReviewDao dao = DaoFactory.getInstance().createReviewDao();
                dao.insertReview(DaoFactory.getInstance().getConnection(), review);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            formBody.setVisible(false);
            formBody.setManaged(false);
            successPane.setVisible(true);
            successPane.setManaged(true);
            submitBtn.setVisible(false);
            submitBtn.setManaged(false);
            cancelBtn.setText("Close");
            cancelBtn.setDisable(false);
            successSubLabel.setText(
                    "Thank you! Your review for " + tutor.getFullName() + " has been saved.");
            if (onReviewSubmitted != null) onReviewSubmitted.run();
        });

        task.setOnFailed(e -> {
            submitBtn.setDisable(false);
            cancelBtn.setDisable(false);
            Throwable ex = task.getException();
            if (ex instanceof DuplicateReviewException) {
                showError("You have already submitted a review for this lesson.");
            } else {
                showError("Failed to submit review. Please try again.");
                LOGGER.warning("Review submission failed: " + ex.getMessage());
            }
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
