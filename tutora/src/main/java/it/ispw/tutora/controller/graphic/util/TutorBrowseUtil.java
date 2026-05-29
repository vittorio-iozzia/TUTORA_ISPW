package it.ispw.tutora.controller.graphic.util;

import it.ispw.tutora.controller.application.SearchTutorController;
import it.ispw.tutora.controller.graphic.BookTutorGfxController;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Category;
import it.ispw.tutora.model.Tutor;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import it.ispw.tutora.view.AvatarManager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

// ── Photo URL registry ────────────────────────────────────────────────────────
// Single source of truth for per-username Unsplash portraits.
// All controllers (cards, chat, profile) derive their URLs from here so that
// the same person always maps to the same face.

/**
 * Utility condivisa tra {@link it.ispw.tutora.controller.graphic.FindTutorGfxController}
 * e {@link it.ispw.tutora.controller.graphic.StudentContentController}.
 */
public final class TutorBrowseUtil {

    /** Per-username Unsplash portrait, sized for profile hero (88 px diameter). */
    public static final Map<String, String> PROFILE_PHOTO_URLS = Map.of(
        "tutor_vitto", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=88&h=88&fit=crop&crop=faces",
        "tutor_marco", "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=88&h=88&fit=crop&crop=faces",
        "tutor_sara",  "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=88&h=88&fit=crop&crop=faces",
        "tutor_luca",  "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=88&h=88&fit=crop&crop=faces"
    );

    /**
     * Returns the best image URL for a profile avatar:
     * 1. AvatarManager file path (user-uploaded) → converted to file:// URI
     * 2. Per-username Unsplash portrait from PROFILE_PHOTO_URLS
     * 3. null → caller should fall back to the initial letter
     */
    public static String resolveProfileImageUrl(String username) {
        if (AvatarManager.hasAvatar(username))
            return new File(AvatarManager.getAvatarPath(username)).toURI().toString();
        return PROFILE_PHOTO_URLS.get(username);
    }

    private TutorBrowseUtil() {}

    public static List<Category> loadCategories(SearchTutorController ctrl, Logger logger) {
        try {
            return ctrl.loadCategories();
        } catch (DatabaseException e) {
            logger.warning("Cannot load categories: " + e.getMessage());
            return List.of();
        }
    }

    public static String truncate(String text, int max) {
        return text.length() > max ? text.substring(0, max) + "…" : text;
    }

    /**
     * Costruisce la metà superiore (cover photo) di una tutor card.
     *
     * @param width     larghezza della card in pixel (es. 280 o 260)
     * @param photoUrls map username → URL foto dedicata
     * @param pool      lista URL foto di riserva ordinate per poolIndex
     */
    public static StackPane buildPhotoHalf(Tutor tutor, int poolIndex, double width,
                                           Map<String, String> photoUrls) {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("tutor-card-photo-wrap");
        pane.setPrefHeight(160);
        pane.setMinHeight(160);
        pane.setMaxHeight(160);

        Label initial = new Label(String.valueOf(tutor.getName().charAt(0)).toUpperCase());
        initial.getStyleClass().add("tutor-card-photo-initial");
        pane.getChildren().add(initial);

        ImageView imgView = new ImageView();
        imgView.setFitWidth(width);
        imgView.setFitHeight(160);
        imgView.setPreserveRatio(false);
        imgView.setSmooth(true);
        pane.getChildren().add(imgView);

        Rectangle photoClip = new Rectangle(width, 400);
        photoClip.setArcWidth(28);
        photoClip.setArcHeight(28);
        pane.setClip(photoClip);

        Label featured = new Label("⭐ Featured");
        featured.getStyleClass().add("tutor-card-featured-badge");
        StackPane.setAlignment(featured, Pos.TOP_LEFT);
        StackPane.setMargin(featured, new Insets(10, 0, 0, 10));

        boolean isRemote = poolIndex % 2 == 0;
        Label mode = new Label(isRemote ? "Online" : "In-Person");
        mode.getStyleClass().add(isRemote ? "tutor-card-online-badge" : "tutor-card-inperson-badge");
        StackPane.setAlignment(mode, Pos.TOP_RIGHT);
        StackPane.setMargin(mode, new Insets(10, 10, 0, 0));

        pane.getChildren().addAll(featured, mode);

        // Risoluzione URL foto (stessa logica della pagina profilo → massima coerenza visiva):
        // 1. Avatar caricato dall'utente (AvatarManager) — priorità assoluta
        // 2. Mappa specifica della card (risoluzione ottimale, passata dal controller)
        // 3. PROFILE_PHOTO_URLS — stessa sorgente usata dalla pagina profilo
        // 4. null → imgView resta vuoto, il Label con la lettera iniziale rimane visibile
        String photoUrl;
        if (AvatarManager.hasAvatar(tutor.getUsername())) {
            photoUrl = new File(AvatarManager.getAvatarPath(tutor.getUsername())).toURI().toString();
        } else {
            photoUrl = photoUrls.get(tutor.getUsername());
            if (photoUrl == null) {
                // Fallback: stessa sorgente della pagina profilo → card e profilo sempre coerenti
                photoUrl = resolveProfileImageUrl(tutor.getUsername());
            }
        }
        if (photoUrl != null) {
            // Caricamento sincrono (nessun backgroundLoading) per mostrare l'immagine subito,
            // coerente con ProfileGfxController.displayAvatar() che usa la stessa modalità.
            // Evita il race-condition in cui il listener asincrono non veniva mai invocato
            // per immagini già in cache, lasciando visibile la lettera iniziale verde.
            Image img = new Image(photoUrl, width + 20, 200, false, true);
            if (!img.isError()) imgView.setImage(img);
        }

        return pane;
    }

    public static ImageView loadTwemoji(String codepoint, double size) {
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

    /** Applies a TranslateTransition hover lift to any Region (HBox, VBox, etc.). */
    public static void addHoverLift(Region node) {
        javafx.animation.TranslateTransition up =
                new javafx.animation.TranslateTransition(javafx.util.Duration.millis(160), node);
        up.setToY(-5);
        javafx.animation.TranslateTransition down =
                new javafx.animation.TranslateTransition(javafx.util.Duration.millis(160), node);
        down.setToY(0);
        node.setOnMouseEntered(e -> { down.stop(); up.playFromStart(); });
        node.setOnMouseExited (e -> { up.stop();   down.playFromStart(); });
        node.setStyle("-fx-cursor: hand;");
    }

    /**
     * Apre il dialogo di booking per un tutor in una Stage modale trasparente.
     * Applica blur+dim alla finestra padre mentre il dialogo è aperto.
     *
     * @param anchor qualunque nodo della scena corrente (usato per ottenere root e window)
     */
    public static void openBookingDialog(Tutor tutor, Node anchor, Logger logger) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    TutorBrowseUtil.class.getResource("/fxml/book_tutor.fxml"));
            Parent root = loader.load();
            BookTutorGfxController ctrl = loader.getController();
            ctrl.initTutor(tutor);

            Parent parentRoot = anchor.getScene().getRoot();
            GaussianBlur blur = new GaussianBlur(10);
            ColorAdjust dim = new ColorAdjust();
            dim.setBrightness(-0.35);
            dim.setInput(blur);
            parentRoot.setEffect(dim);

            Stage stage = new Stage();
            stage.initOwner(anchor.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.setMinWidth(560);
            stage.setMinHeight(500);
            stage.setOnHiding(e -> parentRoot.setEffect(null));
            stage.show();
        } catch (IOException e) {
            logger.warning("Cannot open booking dialog: " + e.getMessage());
        }
    }
}
