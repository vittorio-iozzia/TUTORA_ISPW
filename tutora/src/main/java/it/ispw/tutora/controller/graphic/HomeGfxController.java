package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.bean.NotificationBean;
import it.ispw.tutora.controller.application.GetNotificationsController;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.SceneManager;
import it.ispw.tutora.view.home.DashboardComponent;
import it.ispw.tutora.view.home.DashboardFactory;
import it.ispw.tutora.view.home.StudentDashboardDecorator;
import it.ispw.tutora.view.home.TutorDashboardDecorator;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Controller del layout shell condiviso (home.fxml).
 *
 * Responsabilità:
 *   - popola la sidebar con i link di navigazione specifici del ruolo
 *   - aggiorna header (titolo sezione, avatar)
 *   - delega la costruzione del contenuto principale a {@link DashboardFactory}
 *   - gestisce il menu a tendina sull'avatar sidebar (Profile / Settings / Logout)
 */
public class HomeGfxController {

    private static final Logger LOGGER =
            Logger.getLogger(HomeGfxController.class.getName());

    @FXML private VBox sidebarNav;
    @FXML private StackPane sidebarAvatarPane;
    @FXML private Label roleBadgeLabel;
    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;
    @FXML private Label avatarLabel;
    @FXML private Label headerTitle;
    @FXML private Button headerProfileBtn;
    @FXML private Button    notifBtn;
    @FXML private Label     notifBadge;
    @FXML private ImageView notifIconView;
    @FXML private VBox contentArea;

    private final List<Button> navButtons = new ArrayList<>();
    private Button activeNavBtn;

    private ContextMenu avatarMenu;
    private final GetNotificationsController notifController = new GetNotificationsController();

    // ----------------------------------------------------------------
    // Inizializzazione
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        String token = SceneManager.getInstance().getSessionToken();
        Session session = SessionManager.getInstance().getSession(token);

        String username = session.getUser().getUsername();
        String initial  = String.valueOf(username.charAt(0)).toUpperCase();

        String roleLabel_ = resolveRoleLabel(session);
        usernameLabel.setText(username);
        roleLabel.setText(roleLabel_);
        roleBadgeLabel.setText(roleLabel_.toUpperCase());
        avatarLabel.setText(initial);

        setupAvatarMenu(session);
        buildNav(session);

        DashboardComponent dashboard = DashboardFactory.create(session);
        dashboard.decorateContent(contentArea);
        refreshNotifBadge();
        loadNotifBellEmoji();
    }

    // ----------------------------------------------------------------
    // Avatar — hover animation + dropdown menu
    // ----------------------------------------------------------------

    private void setupAvatarMenu(Session session) {
        avatarMenu = buildAvatarMenu(session);

        // Dropdown aperto dal pulsante profilo in alto a destra
        headerProfileBtn.setOnAction(e -> {
            if (avatarMenu.isShowing()) {
                avatarMenu.hide();
            } else {
                avatarMenu.show(headerProfileBtn,
                        javafx.geometry.Side.BOTTOM, 0, 4);
            }
        });
    }

    private ContextMenu buildAvatarMenu(Session session) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("avatar-menu");

        if (session.isStudent()) {
            MenuItem profileItem = menuItem("My Profile",  "fas-user-circle");
            profileItem.setOnAction(e -> openProfilePage());
            menu.getItems().add(profileItem);
            menu.getItems().add(new SeparatorMenuItem());
        }

        MenuItem settingsItem = menuItem("Settings", "fas-cog");
        settingsItem.setOnAction(e -> {}); // noop — future use
        menu.getItems().add(settingsItem);

        menu.getItems().add(new SeparatorMenuItem());

        MenuItem logoutItem = menuItem("Log out", "fas-sign-out-alt");
        logoutItem.setOnAction(e -> SceneManager.getInstance().showLogin());
        // Colour the logout icon red directly — CSS .graphic selectors are unreliable on MenuItem
        if (logoutItem.getGraphic() instanceof FontIcon icon) {
            icon.setStyle("-fx-icon-color: #9C2121;");
        }
        menu.getItems().add(logoutItem);

        return menu;
    }

    private MenuItem menuItem(String label, String iconLiteral) {
        MenuItem item = new MenuItem(label);
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(13);
        item.setGraphic(icon);
        return item;
    }

    // ----------------------------------------------------------------
    // Profile page (full-size window)
    // ----------------------------------------------------------------

    private void openProfilePage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/student_profile.fxml"));
            Parent root = loader.load();

            Stage profileStage = new Stage();
            profileStage.initOwner(sidebarAvatarPane.getScene().getWindow());
            profileStage.initModality(Modality.WINDOW_MODAL);
            profileStage.setTitle("My Profile – TUTORA");
            profileStage.setScene(new Scene(root, 1100, 700));
            profileStage.setMinWidth(900);
            profileStage.setMinHeight(620);
            profileStage.show();
        } catch (IOException e) {
            LOGGER.warning("Cannot open profile page: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Sidebar nav
    // ----------------------------------------------------------------

    private void buildNav(Session session) {
        List<NavEntry> items = resolveNavItems(session);
        for (NavEntry entry : items) {
            Button btn = buildNavButton(entry);
            navButtons.add(btn);
            sidebarNav.getChildren().add(btn);
        }
        if (!navButtons.isEmpty()) setActive(navButtons.get(0));
    }

    private Button buildNavButton(NavEntry entry) {
        FontIcon icon = new FontIcon(entry.iconLiteral());
        icon.setIconSize(15);
        icon.getStyleClass().add("nav-icon");

        Button btn = new Button(entry.label());
        btn.setGraphic(icon);
        btn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        btn.setGraphicTextGap(10);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("nav-item");
        btn.setOnAction(e -> {
            setActive(btn);
            headerTitle.setText(entry.headerTitle());
            entry.action().run();
        });
        return btn;
    }

    private void setActive(Button btn) {
        if (activeNavBtn != null) activeNavBtn.getStyleClass().remove("nav-active");
        activeNavBtn = btn;
        btn.getStyleClass().add("nav-active");
    }

    private List<NavEntry> resolveNavItems(Session session) {
        if (session.isStudent()) {
            return List.of(
                new NavEntry("Dashboard",  "fas-home",        "Dashboard",   () -> swapContent("/fxml/student_content.fxml")),
                new NavEntry("Find Tutors","fas-search",      "Find Tutors", () -> swapContent("/fxml/find_tutors_content.fxml")),
                new NavEntry("My Lessons", "fas-calendar-alt","My Lessons",  () -> swapContent("/fxml/my_lessons_content.fxml")),
                new NavEntry("Messages",   "fas-comments",    "Messages",    () -> swapContent("/fxml/messages_content.fxml")),
                new NavEntry("Payments",   "fas-credit-card", "Payments",    this::noop),
                new NavEntry("Favorites",  "fas-heart",       "Favorites",   this::noop),
                new NavEntry("Settings",   "fas-cog",         "Settings",    this::noop)
            );
        }
        if (session.isTutor()) {
            return List.of(
                new NavEntry("Dashboard",    "fas-home",        "Dashboard",    this::noop),
                new NavEntry("My Lessons",   "fas-calendar-alt","My Lessons",   () -> SceneManager.getInstance().showTutorLessons()),
                new NavEntry("My Expertise", "fas-star",        "My Expertise", () -> SceneManager.getInstance().showTutorExpertise()),
                new NavEntry("Messages",     "fas-comments",    "Messages",     () -> swapContent("/fxml/messages_content.fxml")),
                new NavEntry("Settings",     "fas-cog",         "Settings",     this::noop)
            );
        }
        // Admin
        return List.of(
            new NavEntry("Dashboard", "fas-home", "Dashboard", this::noop),
            new NavEntry("Settings",  "fas-cog",  "Settings",  this::noop)
        );
    }

    // ----------------------------------------------------------------
    // FXML handlers
    // ----------------------------------------------------------------

    @FXML
    public void handleLogout() {
        SceneManager.getInstance().showLogin();
    }

    @FXML
    public void handleNotifications() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/notifications.fxml"));
            Parent root = loader.load();
            NotificationGfxController notifCtrl = loader.getController();

            // Registra il callback che aggiorna "Upcoming Lessons" dopo il pagamento
            Session currentSession = SessionManager.getInstance().getSession(
                    SceneManager.getInstance().getSessionToken());
            if (currentSession.isStudent()) {
                NotificationGfxController.setPaymentConfirmedCallback(
                        StudentDashboardDecorator::refreshUpcoming);
            } else if (currentSession.isTutor()) {
                NotificationGfxController.setPaymentConfirmedCallback(
                        TutorDashboardDecorator::refreshUpcoming);
            } else {
                NotificationGfxController.setPaymentConfirmedCallback(null);
            }

            Stage dialog = new Stage();
            dialog.initOwner(notifBtn.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initStyle(StageStyle.DECORATED);
            dialog.setTitle("Notifications");
            dialog.setResizable(false);
            javafx.scene.Scene notifScene = new javafx.scene.Scene(root);
            dialog.setScene(notifScene);
            dialog.setMinWidth(460);
            dialog.setMinHeight(400);

            // Sfuma lo sfondo mentre il dialog è aperto (come BookTutor)
            Parent sceneRoot = (Parent) notifBtn.getScene().getRoot();
            sceneRoot.setEffect(new GaussianBlur(8));
            dialog.setOnHiding(e -> {
                // Marca come lette tutte le notifiche viste tranne le
                // richieste di booking pendenti del tutor
                notifCtrl.markVisibleAsRead();
                sceneRoot.setEffect(null);
                refreshNotifBadge();
            });

            dialog.show();
        } catch (IOException e) {
            LOGGER.warning("Cannot open notifications: " + e.getMessage());
        }
    }

    private void loadNotifBellEmoji() {
        if (notifIconView == null) return;
        notifIconView.setFitWidth(20);
        notifIconView.setFitHeight(20);
        notifIconView.setSmooth(true);
        notifIconView.setPreserveRatio(true);
        String url = "https://cdnjs.cloudflare.com/ajax/libs/twemoji/14.0.2/72x72/1f514.png";
        Image img = new Image(url, 40, 40, true, true, true);
        img.progressProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() >= 1.0 && !img.isError()) notifIconView.setImage(img);
        });
    }

    private void refreshNotifBadge() {
        String tk = SceneManager.getInstance().getSessionToken();
        Task<Integer> task = new Task<>() {
            @Override protected Integer call() { return notifController.getUnreadCount(tk); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            int count = task.getValue();
            if (count > 0) {
                notifBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                notifBadge.setVisible(true);
                notifBadge.setManaged(true);
            } else {
                notifBadge.setVisible(false);
                notifBadge.setManaged(false);
            }
        }));
        Thread t = new Thread(task, "notif-badge");
        t.setDaemon(true);
        t.start();
    }

    // ----------------------------------------------------------------
    // Utility
    // ----------------------------------------------------------------

    private String resolveRoleLabel(Session session) {
        if (session.isAdmin()) return "Admin";
        if (session.isTutor()) return "Tutor";
        return "Student";
    }

    private void swapContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            VBox.setVgrow(content, Priority.ALWAYS);
            contentArea.getChildren().setAll(content);
        } catch (Exception e) {
            LOGGER.severe("Cannot load content fragment: " + fxmlPath + " — " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void noop() {}

    private record NavEntry(String label, String iconLiteral, String headerTitle, Runnable action) {}
}
