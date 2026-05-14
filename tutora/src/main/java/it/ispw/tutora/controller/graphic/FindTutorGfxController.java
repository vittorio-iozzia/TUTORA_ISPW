package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.controller.application.SearchTutorController;
import it.ispw.tutora.dao.TutorDao;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Controller del frammento find_tutors_content.fxml.
 *
 * Arricchisce ogni tutor card con chip di expertise e prezzo minimo,
 * popola la barra statistiche e anima le card con una FadeTransition +
 * TranslateTransition al hover (nessun effetto bordo indesiderato).
 */
public class FindTutorGfxController {

    private static final Logger LOGGER =
            Logger.getLogger(FindTutorGfxController.class.getName());

    // ----------------------------------------------------------------
    // FXML
    // ----------------------------------------------------------------

    @FXML private TextField searchField;
    @FXML private HBox      categoryPills;
    @FXML private FlowPane  tutorGrid;
    @FXML private Label     resultsLabel;
    @FXML private Label     statTutorsLabel;
    @FXML private Label     statSubjectsLabel;
    @FXML private HBox      ftStatCard1;
    @FXML private HBox      ftStatCard2;
    @FXML private HBox      ftStatCard3;
    @FXML private HBox      ftStatCard4;
    @FXML private Button    applyBtn;

    /** Currently selected category, null when "All" is active. */
    private String selectedCategory = null;

    // ----------------------------------------------------------------
    // State
    // ----------------------------------------------------------------

    private List<Tutor>    allTutors     = List.of();
    private List<Category> allCategories = List.of();

    /** Subcategory names (APPROVED) per tutor username, max 3. */
    private Map<String, List<String>>   expertiseNames = new HashMap<>();
    /** Minimum hourly price (APPROVED expertise) per tutor username. */
    private Map<String, BigDecimal>     minPrices      = new HashMap<>();

    private final SearchTutorController searchController = new SearchTutorController();

    // ----------------------------------------------------------------
    // Photo pool
    // ----------------------------------------------------------------

    private static final Map<String, String> PHOTO_URLS = Map.of(
        "tutor_vitto",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=104&h=104&fit=crop&crop=face"
    );

    private static final List<String> PORTRAIT_POOL = List.of(
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=104&h=104&fit=crop&crop=face",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=104&h=104&fit=crop&crop=face",
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=104&h=104&fit=crop&crop=face",
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=104&h=104&fit=crop&crop=face",
        "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=104&h=104&fit=crop&crop=face",
        "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=104&h=104&fit=crop&crop=face"
    );

    // ----------------------------------------------------------------
    // Inizializzazione
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        allTutors     = loadTutors();
        allCategories = loadCategories();
        loadExpertises();

        updateStatsBar();
        addStatCardHover(ftStatCard1);
        addStatCardHover(ftStatCard2);
        addStatCardHover(ftStatCard3);
        addStatCardHover(ftStatCard4);
        buildCategoryPills();
        buildTutorGrid(allTutors);

        searchField.textProperty().addListener((obs, old, val) -> filterTutors(val));
    }

    // ----------------------------------------------------------------
    // DAO
    // ----------------------------------------------------------------

    private List<Tutor> loadTutors() {
        try {
            TutorDao dao = DaoFactory.getInstance().createTutorDao();
            return dao.selectAllTutors(DaoFactory.getInstance().getConnection());
        } catch (DatabaseException e) {
            LOGGER.warning("Cannot load tutors: " + e.getMessage());
            return List.of();
        }
    }

    private List<Category> loadCategories() {
        try {
            return searchController.loadCategories();
        } catch (DatabaseException e) {
            LOGGER.warning("Cannot load categories: " + e.getMessage());
            return List.of();
        }
    }

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
            if (on) {
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

            // Blur + dim the parent window while the modal is open
            javafx.scene.Parent parentRoot = applyBtn.getScene().getRoot();
            GaussianBlur blur = new GaussianBlur(10);
            ColorAdjust dim  = new ColorAdjust();
            dim.setBrightness(-0.35);
            dim.setInput(blur);
            parentRoot.setEffect(dim);

            Stage stage = new Stage();
            stage.initOwner(applyBtn.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Apply – " + selectedCategory + " Tutor · TUTORA");
            stage.setScene(new Scene(root));
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
            VBox empty = buildEmptyState();
            tutorGrid.getChildren().add(empty);
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
            fade.setDelay(Duration.millis(idx * 55L));
            fade.play();
            tutorGrid.getChildren().add(card);
            idx++;
        }
        updateResultsLabel(tutors.size());
    }

    private void updateResultsLabel(int count) {
        resultsLabel.setText(count == 0 ? "No tutors match your search"
                : count + (count == 1 ? " tutor found" : " tutors found"));
    }

    // ----------------------------------------------------------------
    // Tutor card
    // ----------------------------------------------------------------

    private VBox buildTutorCard(Tutor tutor, int poolIndex) {
        VBox card = new VBox(12);
        card.getStyleClass().add("tutor-card");
        card.setPrefWidth(295);

        // ── Header: photo + name + desc ──
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane photo = buildPhotoPane(tutor, poolIndex);

        VBox nameBox = new VBox(3);
        Label name = new Label(tutor.getFullName());
        name.getStyleClass().add("tutor-name");
        String descText = tutor.getDescription() != null
                ? truncate(tutor.getDescription(), 48)
                : "@" + tutor.getUsername();
        Label desc = new Label(descText);
        desc.getStyleClass().add("tutor-subject");
        desc.setWrapText(true);
        nameBox.getChildren().addAll(name, desc);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        header.getChildren().addAll(photo, nameBox);

        // ── Rating ──
        HBox ratingRow = buildRatingRow(tutor);

        // ── Expertise chips ──
        List<String> chips = expertiseNames.getOrDefault(tutor.getUsername(), List.of());
        FlowPane chipsPane = buildChipsPane(chips);

        // ── Divider ──
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #F3F4F6;");

        // ── Footer: price + book ──
        HBox footer = buildFooter(tutor);

        card.getChildren().addAll(header, ratingRow);
        if (!chips.isEmpty()) card.getChildren().add(chipsPane);
        card.getChildren().addAll(sep, footer);

        addHoverLift(card);
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

        Button bookBtn = new Button("Book");
        bookBtn.getStyleClass().add("book-btn");

        footer.getChildren().addAll(spacer, bookBtn);
        return footer;
    }

    // ----------------------------------------------------------------
    // Empty state
    // ----------------------------------------------------------------

    private VBox buildEmptyState() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(48));

        // magnifying glass via Twemoji CDN – no font rendering issues
        StackPane iconCircle = new StackPane();
        iconCircle.setStyle(
            "-fx-background-color: #EEF2FF;" +
            "-fx-background-radius: 44;" +
            "-fx-min-width: 88; -fx-max-width: 88;" +
            "-fx-min-height: 88; -fx-max-height: 88;");
        iconCircle.getChildren().add(loadTwemoji("1f50d", 44));

        Label title = new Label("No tutors found");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #6B7280;");
        Label sub = new Label("Try a different search term or category");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #9CA3AF;");

        box.getChildren().addAll(iconCircle, title, sub);
        return box;
    }

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

    // ----------------------------------------------------------------
    // Hover lift (TranslateTransition – no border scaling glitch)
    // ----------------------------------------------------------------

    private void addStatCardHover(HBox card) {
        TranslateTransition up   = new TranslateTransition(Duration.millis(160), card);
        up.setToY(-5);
        TranslateTransition down = new TranslateTransition(Duration.millis(160), card);
        down.setToY(0);
        card.setOnMouseEntered(e -> { down.stop(); up.playFromStart(); });
        card.setOnMouseExited (e -> { up.stop();   down.playFromStart(); });
        card.setStyle("-fx-cursor: hand;");
    }

    private void addHoverLift(VBox card) {
        TranslateTransition up   = new TranslateTransition(Duration.millis(160), card);
        up.setToY(-5);
        TranslateTransition down = new TranslateTransition(Duration.millis(160), card);
        down.setToY(0);
        card.setOnMouseEntered(e -> { down.stop(); up.playFromStart(); });
        card.setOnMouseExited (e -> { up.stop();   down.playFromStart(); });
        card.setStyle("-fx-cursor: hand;");
    }

    // ----------------------------------------------------------------
    // Photo pane
    // ----------------------------------------------------------------

    private StackPane buildPhotoPane(Tutor tutor, int poolIndex) {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("tutor-avatar");

        String photoUrl = PHOTO_URLS.getOrDefault(
                tutor.getUsername(),
                PORTRAIT_POOL.get(poolIndex % PORTRAIT_POOL.size()));

        ImageView imgView = new ImageView();
        imgView.setFitWidth(52);
        imgView.setFitHeight(52);
        imgView.setPreserveRatio(false);
        imgView.setSmooth(true);
        imgView.setClip(buildCircleClip());

        Label initial = new Label(String.valueOf(tutor.getName().charAt(0)).toUpperCase());
        initial.getStyleClass().add("tutor-avatar-letter");
        pane.getChildren().add(initial);

        Image img = new Image(photoUrl, 104, 104, true, true, true);
        img.progressProperty().addListener((obs, oldV, newV) -> {
            if (newV.doubleValue() >= 1.0 && !img.isError()) {
                imgView.setImage(img);
                pane.getChildren().setAll(imgView);
            }
        });

        return pane;
    }

    private Circle buildCircleClip() {
        Circle clip = new Circle(26);
        clip.setCenterX(26);
        clip.setCenterY(26);
        return clip;
    }

    private String truncate(String text, int max) {
        return text.length() > max ? text.substring(0, max) + "…" : text;
    }
}
