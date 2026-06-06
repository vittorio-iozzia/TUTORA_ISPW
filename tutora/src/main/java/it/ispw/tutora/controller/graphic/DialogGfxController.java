package it.ispw.tutora.controller.graphic;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;

/**
 * Base condivisa tra {@link BookTutorGfxController} e
 * {@link ApplyToBecomeATutorGfxController}.
 *
 * Contiene le FXML fields comuni (dialog root, form, success pane, error label,
 * icon wraps) e i metodi condivisi: clip arrotondata, icon setup, twemoji loader,
 * showError e showSuccess.
 */
public abstract class DialogGfxController {

    @FXML protected VBox dialogRoot;
    @FXML protected VBox formContainer;
    @FXML protected VBox successPane;
    @FXML protected VBox footer;
    @FXML protected Label errorLabel;

    @FXML protected StackPane headerIconWrap;
    @FXML protected ImageView headerIconView;
    @FXML protected StackPane bannerIconWrap;
    @FXML protected ImageView bannerIconView;
    @FXML protected StackPane successIconWrap;
    @FXML protected ImageView successIconView;

    protected void applyRoundedClip(VBox root) {
        if (root == null) return;
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        root.layoutBoundsProperty().addListener((obs, o, n) -> {
            if (n.getWidth() > 0 && root.getClip() == null) {
                clip.setWidth(n.getWidth());
                clip.setHeight(n.getHeight());
                root.widthProperty().addListener((o2, ov, nv)  -> clip.setWidth(nv.doubleValue()));
                root.heightProperty().addListener((o2, ov, nv) -> clip.setHeight(nv.doubleValue()));
                root.setClip(clip);
            }
        });
    }

    protected void setupIconBox(StackPane wrap, ImageView iv, String codepoint, double size) {
        if (wrap == null) return;
        wrap.setEffect(new DropShadow(10, 0, 4, Color.web("#00000026")));
        if (iv != null) {
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            loadTwemojiInto(codepoint, size, iv);
        }
    }

    protected ImageView loadTwemoji(String codepoint, double size) {
        ImageView iv = new ImageView();
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setSmooth(true);
        iv.setPreserveRatio(true);
        loadTwemojiInto(codepoint, size, iv);
        return iv;
    }

    private void loadTwemojiInto(String codepoint, double size, ImageView target) {
        String url = "https://cdnjs.cloudflare.com/ajax/libs/twemoji/14.0.2/72x72/" + codepoint + ".png";
        Image img = new Image(url, size * 2, size * 2, true, true, true);
        img.progressProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() >= 1.0 && !img.isError()) target.setImage(img);
        });
    }

    protected void showError(String msg) {
        if (msg == null || msg.isBlank()) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        } else {
            errorLabel.setText(msg);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    protected void showSuccess() {
        formContainer.setVisible(false);
        formContainer.setManaged(false);
        footer.setVisible(false);
        footer.setManaged(false);
        successPane.setVisible(true);
        successPane.setManaged(true);
    }
}
