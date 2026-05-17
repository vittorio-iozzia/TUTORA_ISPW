package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.dao.StudentDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.SceneManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.logging.Logger;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Graphic controller per student_profile.fxml.
 * Contiene solo la logica specifica dello studente; tutto il codice comune
 * è ereditato da {@link ProfileGfxController}.
 */
public class StudentProfileGfxController extends ProfileGfxController {

    private static final Logger LOGGER = Logger.getLogger(StudentProfileGfxController.class.getName());

    @FXML private VBox  budgetCard;
    @FXML private VBox  lessonsCard;
    @FXML private VBox  tutorsCard;
    @FXML private VBox  ratingCard;

    @FXML private Label budgetValueLabel;
    @FXML private Label lessonsValueLabel;
    @FXML private Label tutorsValueLabel;
    @FXML private Label ratingValueLabel;

    @FXML private FlowPane interestsPills;
    @FXML private Label    interestsEmptyLabel;

    @FXML private VBox  preferredTutorsBox;
    @FXML private Label preferredTutorsEmptyLabel;

    @FXML private Label     budgetDetailLabel;
    @FXML private HBox      budgetEditBox;
    @FXML private TextField budgetField;
    @FXML private Button    editBudgetBtn;

    @Override
    protected String getRoleLabel() { return "STUDENT"; }

    @Override
    protected String getDefaultDescription() {
        return "No description added yet. Tell the world about yourself and what you want to learn!";
    }

    @FXML
    public void initialize() {
        String token = SceneManager.getInstance().getSessionToken();
        Session session = SessionManager.getInstance().getSession(token);
        Student student = (Student) session.getUser();
        this.username    = student.getUsername();
        this.currentUser = student;

        populateHero(student);
        populateAbout(student);
        populateInterests(student);
        populatePreferredTutors(student);
        populateContact(student);
        populateBudgetStat(student);
        loadBookingStats(student);

        setupAvatarInteraction();
        applyStoredAvatar();

        addHoverLift(budgetCard);
        addHoverLift(lessonsCard);
        addHoverLift(tutorsCard);
        addHoverLift(ratingCard);
    }

    private void populateInterests(Student student) {
        if (student.getInterests().isEmpty()) {
            interestsPills.setManaged(false);
            interestsPills.setVisible(false);
            interestsEmptyLabel.setManaged(true);
            interestsEmptyLabel.setVisible(true);
        } else {
            for (var cat : student.getInterests()) {
                Label pill = new Label(cat.getName());
                pill.getStyleClass().add("interest-pill");
                interestsPills.getChildren().add(pill);
            }
        }
    }

    private void populatePreferredTutors(Student student) {
        List<Tutor> preferred = student.getPreferredTutors();
        if (preferred.isEmpty()) {
            preferredTutorsBox.setManaged(false);
            preferredTutorsBox.setVisible(false);
            preferredTutorsEmptyLabel.setManaged(true);
            preferredTutorsEmptyLabel.setVisible(true);
        } else {
            for (Tutor t : preferred) {
                Label row = new Label("⭐ " + t.getName() + " " + t.getSurname()
                        + " (@" + t.getUsername() + ")");
                row.getStyleClass().add("profile-field-value");
                preferredTutorsBox.getChildren().add(row);
            }
        }
    }

    private void populateBudgetStat(Student student) {
        double budget = student.getBudget() != null
                ? student.getBudget().doubleValue() : 0.0;
        animateStat(budgetValueLabel, budget, "€%.2f");
        budgetDetailLabel.setText(student.getBudget() != null
                ? "€" + student.getBudget().toPlainString()
                : "—");
        ratingValueLabel.setText("—");
    }

    // ----------------------------------------------------------------
    // Budget editing
    // ----------------------------------------------------------------

    @FXML
    private void handleEditBudget() {
        Student student = (Student) currentUser;
        budgetField.setText(student.getBudget() != null
                ? student.getBudget().toPlainString() : "0.00");
        budgetDetailLabel.setVisible(false);
        budgetDetailLabel.setManaged(false);
        editBudgetBtn.setVisible(false);
        editBudgetBtn.setManaged(false);
        budgetEditBox.setVisible(true);
        budgetEditBox.setManaged(true);
        budgetField.requestFocus();
        budgetField.selectAll();
    }

    @FXML
    private void handleCancelBudget() {
        budgetEditBox.setVisible(false);
        budgetEditBox.setManaged(false);
        budgetDetailLabel.setVisible(true);
        budgetDetailLabel.setManaged(true);
        editBudgetBtn.setVisible(true);
        editBudgetBtn.setManaged(true);
    }

    @FXML
    private void handleSaveBudget() {
        String raw = budgetField.getText().trim();
        BigDecimal newBudget;
        try {
            newBudget = new BigDecimal(raw);
            if (newBudget.compareTo(BigDecimal.ZERO) < 0) {
                budgetField.setStyle("-fx-border-color: #EF4444;");
                return;
            }
        } catch (NumberFormatException ex) {
            budgetField.setStyle("-fx-border-color: #EF4444;");
            return;
        }
        budgetField.setStyle(null);

        Student student = (Student) currentUser;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                StudentDao dao = DaoFactory.getInstance().createStudentDao();
                dao.updateStudentBudget(
                        DaoFactory.getInstance().getConnection(),
                        student.getUsername(), newBudget);
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            // Sync the in-memory session object
            BigDecimal current = student.getBudget() != null ? student.getBudget() : BigDecimal.ZERO;
            BigDecimal delta   = newBudget.subtract(current);
            if (delta.compareTo(BigDecimal.ZERO) > 0) student.addBudget(delta);
            else if (delta.compareTo(BigDecimal.ZERO) < 0) student.deductBudget(delta.abs());

            budgetDetailLabel.setText("€" + newBudget.toPlainString());
            animateStat(budgetValueLabel, newBudget.doubleValue(), "€%.2f");
            handleCancelBudget();
        }));

        task.setOnFailed(e -> Platform.runLater(() ->
                LOGGER.warning("Budget update failed: " + task.getException().getMessage())));

        new Thread(task, "budget-update").start();
    }

    private void loadBookingStats(Student student) {
        String uname = student.getUsername();
        Task<long[]> task = new Task<>() {
            @Override
            protected long[] call() throws Exception {
                BookingDao dao = DaoFactory.getInstance().createBookingDao();
                List<Booking> bookings = dao.findByStudent(
                        DaoFactory.getInstance().getConnection(), uname);

                long paidCount = bookings.stream()
                        .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                        .count();

                Set<String> tutorSet = bookings.stream()
                        .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                        .map(b -> {
                            try {
                                return b.getLesson().getExpertise().getTutor().getUsername();
                            } catch (Exception ex) {
                                return null;
                            }
                        })
                        .filter(t -> t != null)
                        .collect(Collectors.toSet());

                return new long[]{ paidCount, tutorSet.size() };
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            long[] res = task.getValue();
            animateStat(lessonsValueLabel, res[0], "%.0f");
            animateStat(tutorsValueLabel,  res[1], "%.0f");
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            lessonsValueLabel.setText("—");
            tutorsValueLabel.setText("—");
        }));

        Thread t = new Thread(task, "profile-stats");
        t.setDaemon(true);
        t.start();
    }
}
