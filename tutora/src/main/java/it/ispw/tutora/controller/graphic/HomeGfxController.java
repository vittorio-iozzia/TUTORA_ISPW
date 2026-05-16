package it.ispw.tutora.controller.graphic;

import it.ispw.tutora.controller.application.GetNotificationsController;
import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;
import it.ispw.tutora.view.AvatarManager;
import it.ispw.tutora.view.SceneManager;
import it.ispw.tutora.view.home.DashboardComponent;
import it.ispw.tutora.view.home.DashboardFactory;
import it.ispw.tutora.view.home.StudentDashboardDecorator;
import it.ispw.tutora.view.home.TutorDashboardDecorator;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.exception.DatabaseException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
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
 *   - carica il profilo in-place a livello di mainArea (fix nested ScrollPane)
 *   - sincronizza l'immagine avatar con {@link AvatarManager}
 */
public class HomeGfxController {

    private static final Logger LOGGER =
            Logger.getLogger(HomeGfxController.class.getName());

    // ----------------------------------------------------------------
    // FXML fields
    // ----------------------------------------------------------------

    @FXML private VBox      mainArea;          // tutta l'area destra (header + contenuto)
    @FXML private VBox      sidebarNav;
    @FXML private StackPane sidebarAvatarPane;
    @FXML private Label     roleBadgeLabel;
    @FXML private Label     usernameLabel;
    @FXML private Label     roleLabel;
    @FXML private Label     avatarLabel;
    @FXML private ImageView sidebarAvatarImage;
    @FXML private Label     headerTitle;
    @FXML private Button    headerProfileBtn;
    @FXML private Button    notifBtn;
    @FXML private Label     notifBadge;
    @FXML private VBox      contentArea;

    // ----------------------------------------------------------------
    // Stato interno
    // ----------------------------------------------------------------

    /** Singleton dell'istanza corrente — usato per navigazione statica tra controller. */
    private static HomeGfxController instance;

    private final List<Button> navButtons = new ArrayList<>();
    private Button activeNavBtn;
    private Label msgNavBadge;

    /** Snapshot dell'area destra quando mostriamo il profilo a schermo intero. */
    private List<Node> defaultMainAreaChildren;

    private ContextMenu avatarMenu;
    private final GetNotificationsController notifController = new GetNotificationsController();

    /** Username dell'utente corrente — serve per AvatarManager listener. */
    private String currentUsername;

    // ----------------------------------------------------------------
    // Inizializzazione
    // ----------------------------------------------------------------

    @FXML
    public void initialize() {
        String token = SceneManager.getInstance().getSessionToken();
        Session session = SessionManager.getInstance().getSession(token);

        currentUsername = session.getUser().getUsername();
        String initial  = String.valueOf(currentUsername.charAt(0)).toUpperCase();

        String roleLabel_ = resolveRoleLabel(session);
        usernameLabel.setText(currentUsername);
        roleLabel.setText(roleLabel_);
        roleBadgeLabel.setText(roleLabel_.toUpperCase());
        avatarLabel.setText(initial);

        setupAvatarMenu(session);
        buildNav(session);

        DashboardComponent dashboard = DashboardFactory.create(session);
        dashboard.decorateContent(contentArea);
        refreshNotifBadge();

        // Salva i figli originali di mainArea per il restore dopo il profilo
        defaultMainAreaChildren = new ArrayList<>(mainArea.getChildren());

        // Sincronizza avatar all'avvio e ad ogni cambio successivo
        AvatarManager.addListener(() -> updateAvatarDisplay(currentUsername));
        updateAvatarDisplay(currentUsername);

        instance = this;
        startMsgBadgePoller(session);
    }

    // ----------------------------------------------------------------
    // Avatar — hover animation + dropdown menu
    // ----------------------------------------------------------------

    private void setupAvatarMenu(Session session) {
        avatarMenu = buildAvatarMenu(session);

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
            MenuItem profileItem = menuItem("My Profile", "fas-user-circle");
            profileItem.setOnAction(e -> openStudentProfilePage());
            menu.getItems().add(profileItem);
            menu.getItems().add(new SeparatorMenuItem());
        }

        if (session.isTutor()) {
            MenuItem profileItem = menuItem("My Profile", "fas-user-circle");
            profileItem.setOnAction(e -> openTutorProfilePage());
            menu.getItems().add(profileItem);
            menu.getItems().add(new SeparatorMenuItem());
        }

        MenuItem settingsItem = menuItem("Settings", "fas-cog");
        settingsItem.setOnAction(e -> {}); // noop — future use
        menu.getItems().add(settingsItem);

        menu.getItems().add(new SeparatorMenuItem());

        MenuItem logoutItem = menuItem("Log out", "fas-sign-out-alt");
        logoutItem.setOnAction(e -> SceneManager.getInstance().showLogin());
        // Stesso stile del pulsante logout in sidebar: size 16 + classe CSS rossa
        if (logoutItem.getGraphic() instanceof FontIcon icon) {
            icon.setIconSize(16);
            icon.getStyleClass().add("logout-icon");
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
    // Sincronizzazione avatar (sidebar + header)
    // ----------------------------------------------------------------

    /**
     * Aggiorna sidebar avatar e header avatar in base all'immagine
     * registrata in {@link AvatarManager} per l'utente corrente.
     */
    private void updateAvatarDisplay(String username) {
        if (AvatarManager.hasAvatar(username)) {
            String path = AvatarManager.getAvatarPath(username);
            String uri  = new File(path).toURI().toString();

            // Sidebar: mostra ImageView con clip circolare, nasconde la lettera
            Image img = new Image(uri, 40, 40, false, true);
            sidebarAvatarImage.setImage(img);
            Circle sidebarClip = new Circle(20, 20, 20);
            sidebarAvatarImage.setClip(sidebarClip);
            sidebarAvatarImage.setVisible(true);
            sidebarAvatarImage.setManaged(true);
            avatarLabel.setVisible(false);
            avatarLabel.setManaged(false);

            // Header: sostituisce il FontIcon con un ImageView circolare
            Image headerImg = new Image(uri, 40, 40, false, true);
            ImageView headerIv = new ImageView(headerImg);
            headerIv.setFitWidth(40);
            headerIv.setFitHeight(40);
            headerIv.setPreserveRatio(false);
            Circle headerClip = new Circle(20, 20, 20);
            headerIv.setClip(headerClip);
            headerProfileBtn.setGraphic(headerIv);

        } else {
            // Nessun avatar: ripristina lettera e FontIcon
            sidebarAvatarImage.setVisible(false);
            sidebarAvatarImage.setManaged(false);
            avatarLabel.setVisible(true);
            avatarLabel.setManaged(true);

            FontIcon icon = new FontIcon("fas-user-circle");
            icon.setIconSize(40);
            icon.getStyleClass().add("header-profile-icon");
            headerProfileBtn.setGraphic(icon);
        }
    }

    // ----------------------------------------------------------------
    // Profilo utente — navigazione interna (swap mainArea)
    // ----------------------------------------------------------------

    /**
     * Carica l'FXML del profilo direttamente in mainArea, sostituendo
     * header + contentArea. Questo garantisce che la ScrollPane interna
     * del profilo riceva un'altezza vincolata e funzioni correttamente.
     */
    private void swapMainArea(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            VBox.setVgrow(content, Priority.ALWAYS);
            mainArea.getChildren().setAll(content);
        } catch (Exception e) {
            LOGGER.severe("Cannot load main area fragment: " + fxmlPath + " — " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Ripristina l'area destra allo stato originale (header + contentArea).
     * Chiamato dal callback "Back" dei controller di profilo.
     */
    public void restoreMainArea() {
        if (defaultMainAreaChildren != null) {
            mainArea.getChildren().setAll(defaultMainAreaChildren);
        }
        headerTitle.setText("Dashboard");
        if (!navButtons.isEmpty()) setActive(navButtons.get(0));
    }

    /**
     * Permette ad altri controller (es. FindTutorGfxController) di aprire
     * il profilo pubblico di un tutor nella main area senza conoscere HomeGfxController.
     */
    public static void navigateToTutorPublicProfile(it.ispw.tutora.model.Tutor tutor) {
        if (instance == null) return;
        TutorPublicProfileGfxController.setTargetTutor(tutor);
        TutorPublicProfileGfxController.setOnBackCallback(instance::restoreMainArea);
        instance.swapMainArea("/fxml/tutor_public_profile.fxml");
    }

    private void openStudentProfilePage() {
        StudentProfileGfxController.setOnBackCallback(this::restoreMainArea);
        swapMainArea("/fxml/student_profile.fxml");
    }

    private void openTutorProfilePage() {
        TutorProfileGfxController.setOnBackCallback(this::restoreMainArea);
        swapMainArea("/fxml/tutor_profile.fxml");
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
        btn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        btn.setGraphicTextGap(10);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("nav-item");

        if ("Messages".equals(entry.label())) {
            msgNavBadge = new Label();
            msgNavBadge.getStyleClass().add("nav-msg-badge");
            msgNavBadge.setVisible(false);
            msgNavBadge.setManaged(false);
            StackPane iconWrapper = new StackPane(icon, msgNavBadge);
            iconWrapper.setAlignment(Pos.CENTER);
            StackPane.setAlignment(msgNavBadge, Pos.TOP_RIGHT);
            msgNavBadge.setTranslateX(10);
            msgNavBadge.setTranslateY(-8);
            btn.setGraphic(iconWrapper);
        } else {
            btn.setGraphic(icon);
        }

        btn.setOnAction(e -> {
            // Se il profilo è aperto, ripristina prima la struttura normale
            if (mainArea.getChildren().size() == 1
                    && !(mainArea.getChildren().get(0) instanceof javafx.scene.layout.HBox)) {
                mainArea.getChildren().setAll(defaultMainAreaChildren);
            }
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
                new NavEntry("Dashboard",    "fas-home",        "Dashboard",    () -> swapContent("/fxml/tutor_content.fxml")),
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

            // Callback che aggiorna "Upcoming Lessons" dopo il pagamento
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

            Parent sceneRoot = (Parent) notifBtn.getScene().getRoot();
            sceneRoot.setEffect(new GaussianBlur(8));
            dialog.setOnHiding(e -> {
                notifCtrl.markVisibleAsRead();
                sceneRoot.setEffect(null);
                refreshNotifBadge();
            });

            dialog.show();
        } catch (IOException e) {
            LOGGER.warning("Cannot open notifications: " + e.getMessage());
        }
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
            boolean isChat = "/fxml/messages_content.fxml".equals(fxmlPath);
            // Chat fills edge-to-edge; other pages keep standard padding
            contentArea.setPadding(isChat ? new Insets(0) : new Insets(28, 36, 28, 36));
            // Force the wrapping ScrollPane to fill height in chat mode so the
            // sidebar stretches all the way to the bottom of the window
            if (contentArea.getParent() instanceof ScrollPane sp) {
                sp.setFitToHeight(isChat);
            }
        } catch (Exception e) {
            LOGGER.severe("Cannot load content fragment: " + fxmlPath + " — " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startMsgBadgePoller(Session session) {
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(5),
                e -> refreshMsgBadge(session)));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();
        refreshMsgBadge(session); // initial check
    }

    private void refreshMsgBadge(Session session) {
        if (msgNavBadge == null) return;
        String username = session.getUser().getUsername();
        Task<Integer> task = new Task<>() {
            @Override protected Integer call() {
                try {
                    return DaoFactory.getInstance().createMessageDao()
                            .countTotalUnread(DaoFactory.getInstance().getConnection(), username);
                } catch (DatabaseException ex) {
                    return 0;
                }
            }
        };
        task.setOnSucceeded(ev -> Platform.runLater(() -> {
            int count = task.getValue();
            if (count > 0) {
                msgNavBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                msgNavBadge.setVisible(true);
                msgNavBadge.setManaged(true);
            } else {
                msgNavBadge.setVisible(false);
                msgNavBadge.setManaged(false);
            }
        }));
        Thread t = new Thread(task, "msg-badge-poll");
        t.setDaemon(true);
        t.start();
    }

    private void noop() {}

    private record NavEntry(String label, String iconLiteral, String headerTitle, Runnable action) {}
}
