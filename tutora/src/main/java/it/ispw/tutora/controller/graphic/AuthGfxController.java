package it.ispw.tutora.controller.graphic;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

/**
 * Base condivisa tra {@link LoginGfxController} e {@link RegistrationGfxController}.
 *
 * Contiene hero image, role group, social icons, entry animation e feedback UI.
 */
public abstract class AuthGfxController {

    protected static final String HERO_IMAGE_URL =
            "https://images.unsplash.com/photo-1766117651759-640cbc09369c" +
            "?crop=entropy&cs=srgb&fm=jpg&q=85&w=1200";

    @FXML protected StackPane  heroPane;
    @FXML protected ImageView  heroImage;
    @FXML protected ToggleGroup roleGroup;
    @FXML protected Label      errorLabel;
    @FXML protected Button     googleBtn;
    @FXML protected Button     metaBtn;

    protected void bindHeroImage() {
        heroImage.fitWidthProperty().bind(heroPane.widthProperty());
        heroImage.fitHeightProperty().bind(heroPane.heightProperty());
        heroImage.setPreserveRatio(false);
        heroImage.setSmooth(true);
    }

    protected void loadHeroImageAsync() {
        heroImage.setImage(new Image(HERO_IMAGE_URL, true));
    }

    protected void setupRoleGroup() {
        roleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) oldToggle.setSelected(true);
        });
    }

    protected Group buildGoogleIcon() {
        SVGPath p1 = new SVGPath();
        p1.setContent("M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z");
        p1.setFill(Color.web("#4285F4"));
        SVGPath p2 = new SVGPath();
        p2.setContent("M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z");
        p2.setFill(Color.web("#34A853"));
        SVGPath p3 = new SVGPath();
        p3.setContent("M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z");
        p3.setFill(Color.web("#FBBC05"));
        SVGPath p4 = new SVGPath();
        p4.setContent("M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z");
        p4.setFill(Color.web("#EA4335"));
        Group g = new Group(p1, p2, p3, p4);
        g.getTransforms().add(new Scale(0.83, 0.83, 0, 0));
        return g;
    }

    protected SVGPath buildMetaIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z");
        path.setFill(Color.web("#1877F2"));
        path.getTransforms().add(new Scale(0.83, 0.83, 0, 0));
        return path;
    }

    protected HBox buildSocialRow(Node icon, String text, int spacing) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #1C2621;");
        HBox row = new HBox(spacing, icon, lbl);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    protected void playEntryAnimation(VBox card) {
        card.setOpacity(0);
        card.setTranslateY(12);
        FadeTransition fade = new FadeTransition(Duration.millis(420), card);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(420), card);
        slide.setFromY(12);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    protected void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), errorLabel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    protected void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
