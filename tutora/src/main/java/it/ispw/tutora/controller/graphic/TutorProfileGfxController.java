package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.controller.application.GetTutorDashboardController;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.TutorExpertise;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.SceneManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Graphic controller per tutor_profile.fxml.
 * Contiene solo la logica specifica del tutor; tutto il codice comune
 * è ereditato da {@link ProfileGfxController}.
 */
public class TutorProfileGfxController extends ProfileGfxController {

    @FXML private VBox  ratingCard;
    @FXML private VBox  reviewsCard;
    @FXML private VBox  expertisesCard;
    @FXML private VBox  lessonsCard;

    @FXML private Label ratingValueLabel;
    @FXML private Label reviewsValueLabel;
    @FXML private Label expertisesValueLabel;
    @FXML private Label lessonsValueLabel;

    @FXML private FlowPane expertisePills;
    @FXML private Label expertisesEmptyLabel;
    @FXML private Label ratingDetailLabel;

    private final GetTutorDashboardController tutorDashController = new GetTutorDashboardController();

    @Override
    protected String getRoleLabel() { return "TUTOR"; }

    @Override
    protected String getDefaultDescription() {
        return "No description added yet. Tell students about yourself, your teaching style, and your areas of expertise!";
    }

    @FXML
    public void initialize() {
        String token = SceneManager.getInstance().getSessionToken();
        Session session = SessionManager.getInstance().getSession(token);
        Tutor tutor = (Tutor) session.getUser();
        this.username    = tutor.getUsername();
        this.currentUser = tutor;

        populateHero(tutor);
        populateRatingStat(tutor);
        populateAbout(tutor);
        populateContact(tutor);
        loadExpertisesAndLessons();

        setupAvatarInteraction();
        applyStoredAvatar();

        addHoverLift(ratingCard);
        addHoverLift(reviewsCard);
        addHoverLift(expertisesCard);
        addHoverLift(lessonsCard);
    }

    private void populateRatingStat(Tutor tutor) {
        if (tutor.getRating() != null && tutor.getRating().doubleValue() > 0) {
            String ratingStr = String.format("%.1f ★", tutor.getRating().doubleValue());
            ratingValueLabel.setText(ratingStr);
            ratingDetailLabel.setText(ratingStr + "  (" + tutor.getRatingCount() + " reviews)");
        } else {
            ratingValueLabel.setText("—");
            ratingDetailLabel.setText("No reviews yet");
        }
        animateStat(reviewsValueLabel, tutor.getRatingCount(), "%.0f");
    }

    private void loadExpertisesAndLessons() {
        String token = SceneManager.getInstance().getSessionToken();

        Task<long[]> task = new Task<>() {
            @Override
            protected long[] call() {
                List<TutorExpertise> approved = tutorDashController.getApprovedExpertises(token);
                long paidLessons = tutorDashController.getPaidBookingsCount(token);
                Platform.runLater(() -> populateExpertisePills(approved));
                return new long[]{ approved.size(), paidLessons };
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            long[] res = task.getValue();
            animateStat(expertisesValueLabel, res[0], "%.0f");
            animateStat(lessonsValueLabel,    res[1], "%.0f");
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            expertisesValueLabel.setText("—");
            lessonsValueLabel.setText("—");
        }));

        Thread t = new Thread(task, "tutor-profile-stats");
        t.setDaemon(true);
        t.start();
    }

    private void populateExpertisePills(List<TutorExpertise> approved) {
        if (approved.isEmpty()) {
            expertisePills.setManaged(false);
            expertisePills.setVisible(false);
            expertisesEmptyLabel.setManaged(true);
            expertisesEmptyLabel.setVisible(true);
        } else {
            for (TutorExpertise exp : approved) {
                String name = exp.getSubcategory() != null
                        ? exp.getSubcategory().getName() : "—";
                Label pill = new Label(name);
                pill.getStyleClass().add("interest-pill");
                expertisePills.getChildren().add(pill);
            }
        }
    }
}
