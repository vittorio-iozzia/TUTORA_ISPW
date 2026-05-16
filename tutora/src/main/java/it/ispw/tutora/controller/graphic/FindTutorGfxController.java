package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.controller.application.SearchTutorController;
import it.ispw.tutora.controller.graphic.util.TutorBrowseUtil;
import it.ispw.tutora.dao.TutorExpertiseDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.Status;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Category;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.TutorExpertise;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Controller del frammento find_tutors_content.fxml.
 */
public class FindTutorGfxController {

    private static final Logger LOGGER =
            Logger.getLogger(FindTutorGfxController.class.getName());

    // ----------------------------------------------------------------
    // FXML
    // ----------------------------------------------------------------

    @FXML private TextField searchField;
    @FXML private HBox categoryPills;
    @FXML private FlowPane tutorGrid;
    @FXML private Label resultsLabel;
    @FXML private Label statTutorsLabel;
    @FXML private Label statSubjectsLabel;
    @FXML private HBox ftStatCard1;
    @FXML private HBox ftStatCard2;
    @FXML private HBox ftStatCard3;
    @FXML private HBox ftStatCard4;
    @FXML private Button applyBtn;

    private String selectedCategory = null;

    // ----------------------------------------------------------------
    // State
    // ----------------------------------------------------------------

    private List<Tutor>    allTutors     = List.of();
    private List<Category> allCategories = List.of();

    private Map<String, List<String>>  expertiseNames = new HashMap<>();
    private Map<String, BigDecimal> minPrices = new HashMap<>();

    private final SearchTutorController searchController = new SearchTutorController();

    // ----------------------------------------------------------------
    // Photo pool
    // ----------------------------------------------------------------

    private static final Map<String, String> PHOTO_URLS = Map.of(
        "tutor_vitto",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&h=200&fit=crop&crop=faces"
    );

    private static final List<String> PORTRAIT_POOL = List.of(
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&h=200&fit=crop&crop=faces",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300&h=200&fit=crop&crop=faces",
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&h=200&fit=crop&crop=faces",
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=300&h=200&fit=crop&crop=faces",
        "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=300&h=200&fit=crop&crop=faces",
        "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=300&h=200&fit=crop&crop=faces"
    );

    // ----------------------------------------------------------------
    // Inizializzazione
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        allTutors     = TutorBrowseUtil.loadTutors(LOGGER);
        allCategories = TutorBrowseUtil.loadCategories(searchController, LOGGER);
        loadExpertises();

        updateStatsBar();
        TutorBrowseUtil.addHoverLift(ftStatCard1);
        TutorBrowseUtil.addHoverLift(ftStatCard2);
        TutorBrowseUtil.addHoverLift(ftStatCard3);
        TutorBrowseUtil.addHoverLift(ftStatCard4);
        buildCategoryPills();
        buildTutorGrid(allTutors);

        searchField.textProperty().addListener((obs, old, val) -> filterTutors(val));
    }

    // ----------------------------------------------------------------
    // DAO
    // ----------------------------------------------------------------

    private void loadExpertises() {
        DaoFactory factory = DaoFactory.getInstance();
        TutorExpertiseDao dao = factory.createTutorExpertiseDao();
        try (Connection conn = factory.getConnection()) {
            for (Tutor t : allTutors) {
                List<TutorExpertise> list = dao.findByTutor(conn, t.getUsername());
                List<String> approved = list.stream()
                        .filter(e -> e.getStatus() == Status.APPROVED)
                        .map(e -> e.getSubcategory().getName())
                        .distinct()
                        .limit(3)
                        .toList();
                expertiseNames.put(t.getUsername(), approved);

                list.stream()
                        .filter(e -> e.getStatus() == Status.APPROVED
                                && e.getHourlyPrice() != null)
                        .map(TutorExpertise::getHourlyPrice)
                        .min(BigDecimal::compareTo)
                        .ifPresent(min -> minPrices.put(t.getUsername(), min));
            }
        } catch (SQLException | DatabaseException e) {
            LOGGER.warning("Cannot load expertises: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Stats bar
    // ----------------------------------------------------------------

    private void updateStatsBar() {
        statTutorsLabel.setText(allTutors.size() + "+");

        long subjectCount = expertiseNames.values().stream()
                .flatMap(List::stream)
                .distinct()
                .count();
        statSubjectsLabel.setText(subjectCount > 0 ? subjectCount + "+" : String.valueOf(allCategories.size()));
    }

    // ----------------------------------------------------------------
    // Category pills
    // ----------------------------------------------------------------

    private void buildCategoryPills() {
        ToggleGroup group = new ToggleGroup();
        addPill(group, "All", true);
        for (Category cat : allCategories) {
            addPill(group, cat.getName(), false);
        }
        group.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) oldT.setSelected(true);
        });
    }

    private void addPill(ToggleGroup group, String label, boolean selected) {
        ToggleButton pill = new ToggleButton(label);
        pill.setToggleGroup(group);
        pill.getStyleClass().add("category-pill");
        pill.setSelected(selected);
        pill.selectedProperty().addListener((obs, was, on) -> {
            if (Boolean.TRUE.equals(on)) {
                applyCategory(label);
                selectedCategory = "All".equals(label) ? null : label;
                updateApplyButton();
            }
        });
        categoryPills.getChildren().add(pill);
    }

    private void updateApplyButton() {
        if (selectedCategory == null) {
            applyBtn.setText("Seleziona una categoria");
            applyBtn.setDisable(true);
        } else {
            applyBtn.setText("Apply to become a " + selectedCategory + " Tutor");
            applyBtn.setDisable(false);
        }
    }

    @FXML
    private void handleApply() {
        if (selectedCategory == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/tutor_application.fxml"));
            Parent root = loader.load();
            TutorApplicationGfxController ctrl = loader.getController();
            ctrl.initCategory(selectedCategory);

            javafx.scene.Parent parentRoot = applyBtn.getScene().getRoot();
            GaussianBlur blur = new GaussianBlur(10);
            ColorAdjust dim  = new ColorAdjust();
            dim.setBrightness(-0.35);
            dim.setInput(blur);
            parentRoot.setEffect(dim);

            Stage stage = new Stage();
            stage.initOwner(applyBtn.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.setMinWidth(560);
            stage.setMinHeight(500);
            stage.setOnHiding(e -> parentRoot.setEffect(null));
            stage.show();
        } catch (java.io.IOException e) {
            LOGGER.warning("Cannot open application window: " + e.getMessage());
        }
    }

    private void applyCategory(String category) {
        List<Tutor> result = category.equals("All")
                ? allTutors
                : searchController.filterByCategory(allTutors, category);
        buildTutorGrid(result);
    }

    private void filterTutors(String query) {
        String q = query.toLowerCase();
        List<Tutor> filtered = allTutors.stream()
                .filter(t -> t.getFullName().toLowerCase().contains(q)
                          || t.getUsername().toLowerCase().contains(q)
                          || (t.getDescription() != null
                              && t.getDescription().toLowerCase().contains(q))
                          || expertiseNames.getOrDefault(t.getUsername(), List.of())
                                .stream().anyMatch(s -> s.toLowerCase().contains(q)))
                .toList();
        buildTutorGrid(filtered);
    }

    // ----------------------------------------------------------------
    // Tutor grid
    // ----------------------------------------------------------------

    private void buildTutorGrid(List<Tutor> tutors) {
        tutorGrid.getChildren().clear();

        if (tutors.isEmpty()) {
            tutorGrid.getChildren().add(buildEmptyState());
            updateResultsLabel(0);
            return;
        }

        int idx = 0;
        for (Tutor t : tutors) {
            VBox card = buildTutorCard(t, idx);
            card.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(320), card);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setDelay(Duration.millis((double) idx * 55));
            fade.play();
            tutorGrid.getChildren().add(card);
            idx++;
        }
        updateResultsLabel(tutors.size());
    }

    private void updateResultsLabel(int count) {
        if (count == 0) {
            resultsLabel.setText("No tutors match your search");
        } else {
            String suffix = count == 1 ? " tutor found" : " tutors found";
            resultsLabel.setText(count + suffix);
        }
    }

    // ----------------------------------------------------------------
    // Tutor card
    // ----------------------------------------------------------------

    private VBox buildTutorCard(Tutor tutor, int poolIndex) {
        VBox card = new VBox(0);
        card.getStyleClass().add("tutor-card");
        card.setPrefWidth(280);

        StackPane photo = TutorBrowseUtil.buildPhotoHalf(tutor, poolIndex, 280, PHOTO_URLS, PORTRAIT_POOL);

        VBox body = new VBox(10);
        body.getStyleClass().add("tutor-card-body");

        Label name = new Label(tutor.getFullName());
        name.getStyleClass().add("tutor-name");

        String descText = tutor.getDescription() != null
                ? TutorBrowseUtil.truncate(tutor.getDescription(), 55)
                : "@" + tutor.getUsername();
        Label desc = new Label(descText);
        desc.getStyleClass().add("tutor-subject");
        desc.setWrapText(true);

        HBox ratingRow = buildRatingRow(tutor);

        List<String> chips = expertiseNames.getOrDefault(tutor.getUsername(), List.of());

        HBox footer = buildFooter(tutor);

        body.getChildren().addAll(name, desc, ratingRow);
        if (!chips.isEmpty()) body.getChildren().add(buildChipsPane(chips));
        body.getChildren().add(footer);

        card.getChildren().addAll(photo, body);
        TutorBrowseUtil.addHoverLift(card);
        return card;
    }

    private HBox buildRatingRow(Tutor tutor) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        FontIcon star = new FontIcon("fas-star");
        star.getStyleClass().add("star-icon");
        boolean hasRating = tutor.getRating() != null && tutor.getRatingCount() > 0;
        String text = hasRating
                ? String.format("%.1f", tutor.getRating())
                  + "  ·  " + tutor.getRatingCount() + " reviews"
                : "No reviews yet";
        Label lbl = new Label(text);
        lbl.getStyleClass().add("tutor-rating");
        row.getChildren().addAll(star, lbl);
        return row;
    }

    private FlowPane buildChipsPane(List<String> chips) {
        FlowPane pane = new FlowPane(6, 6);
        for (String chip : chips) {
            Label tag = new Label(chip);
            tag.getStyleClass().add("expertise-chip");
            pane.getChildren().add(tag);
        }
        return pane;
    }

    private HBox buildFooter(Tutor tutor) {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);

        BigDecimal price = minPrices.get(tutor.getUsername());
        if (price != null) {
            VBox priceBox = new VBox(0);
            priceBox.setAlignment(Pos.CENTER_LEFT);
            Label fromLbl = new Label("from");
            fromLbl.getStyleClass().add("ft-price-from");
            Label priceLbl = new Label("€" + price.stripTrailingZeros().toPlainString() + "/h");
            priceLbl.getStyleClass().add("ft-price-value");
            priceBox.getChildren().addAll(fromLbl, priceLbl);
            footer.getChildren().add(priceBox);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button profileBtn = new Button("Profile");
        profileBtn.getStyleClass().add("profile-btn");
        profileBtn.setOnAction(e -> HomeGfxController.navigateToTutorPublicProfile(tutor));

        Button bookBtn = new Button("Book");
        bookBtn.getStyleClass().add("book-btn");
        bookBtn.setOnAction(e -> TutorBrowseUtil.openBookingDialog(tutor, applyBtn, LOGGER));

        footer.getChildren().addAll(spacer, profileBtn, bookBtn);
        return footer;
    }

    // ----------------------------------------------------------------
    // Empty state
    // ----------------------------------------------------------------

    private VBox buildEmptyState() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(48));

        StackPane iconCircle = new StackPane();
        iconCircle.setStyle(
            "-fx-background-color: #EEF2FF;" +
            "-fx-background-radius: 44;" +
            "-fx-min-width: 88; -fx-max-width: 88;" +
            "-fx-min-height: 88; -fx-max-height: 88;");
        iconCircle.getChildren().add(TutorBrowseUtil.loadTwemoji("1f50d", 44));

        Label title = new Label("No tutors found");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #6B7280;");
        Label sub = new Label("Try a different search term or category");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #9CA3AF;");

        box.getChildren().addAll(iconCircle, title, sub);
        return box;
    }
}
