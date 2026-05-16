package it.ispw.tutora.controller.application;

import it.ispw.tutora.bean.SocialLoginBean;
import it.ispw.tutora.boundary.GoogleAuthBoundary;
import it.ispw.tutora.boundary.MetaAuthBoundary;
import it.ispw.tutora.boundary.OAuthCallbackServer;
import it.ispw.tutora.dao.StudentDao;
import it.ispw.tutora.dao.UserDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateUserException;
import it.ispw.tutora.exception.OAuthException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.User;
import it.ispw.tutora.model.session.SessionManager;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Controller applicativo per il login tramite provider OAuth2 (Google, Meta).
 *
 * -----------------------------------------------------------------------
 * Flusso per ogni provider
 * -----------------------------------------------------------------------
 * 1. Legge le credenziali OAuth da app.properties
 * 2. Costruisce l'URL di autorizzazione tramite la Boundary specifica
 * 3. Apre il browser di sistema con quell'URL
 * 4. Avvia OAuthCallbackServer per catturare il codice di ritorno
 * 5. La Boundary scambia il codice per un access token e recupera il profilo
 * 6. Cerca l'utente per email nel DAO — se non esiste lo registra come Student
 * 7. Crea la sessione tramite SessionManager e restituisce il token
 *
 * -----------------------------------------------------------------------
 * Registrazione automatica
 * -----------------------------------------------------------------------
 * Gli utenti OAuth che accedono per la prima volta vengono registrati
 * automaticamente come Student con budget 0. Possono cambiare ruolo
 * in seguito tramite il flusso di registrazione standard.
 *
 * -----------------------------------------------------------------------
 * Configurazione
 * -----------------------------------------------------------------------
 * Le chiavi OAuth devono essere impostate in app.properties:
 *   GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET
 *   META_APP_ID, META_APP_SECRET
 * Se mancanti, viene lanciata OAuthException con messaggio esplicativo.
 */
public class SocialLoginController {

    private static final Logger LOGGER = Logger.getLogger(SocialLoginController.class.getName());

    private static final String PROPERTIES_FILE = "/app.properties";
    private static final int    CALLBACK_PORT   = 8888;
    private static final String REDIRECT_URI    = "http://localhost:" + CALLBACK_PORT + "/callback";

    private final Properties props;

    public SocialLoginController() {
        this.props = loadProperties();
    }

    // ----------------------------------------------------------------
    // Entry points – chiamati dal GfxController
    // ----------------------------------------------------------------

    /**
     * Esegue il login tramite Google OAuth2.
     *
     * @return session token da passare a SceneManager
     * @throws OAuthException    se il flusso OAuth fallisce o non è configurato
     * @throws DatabaseException se il DAO non riesce a leggere/scrivere l'utente
     */
    public String loginWithGoogle() throws OAuthException, DatabaseException {
        String clientId     = props.getProperty("GOOGLE_CLIENT_ID", "").trim();
        String clientSecret = props.getProperty("GOOGLE_CLIENT_SECRET", "").trim();

        if (clientId.isBlank()) {
            throw new OAuthException(
                    "Google OAuth non configurato. " +
                    "Aggiungi GOOGLE_CLIENT_ID e GOOGLE_CLIENT_SECRET in app.properties.");
        }

        GoogleAuthBoundary boundary = new GoogleAuthBoundary(clientId, clientSecret, REDIRECT_URI);
        String state    = UUID.randomUUID().toString();
        String authUrl  = boundary.getAuthorizationUrl(state);

        return performFlow(authUrl, boundary::fetchUserProfile);
    }

    /**
     * Esegue il login tramite Meta (Facebook) OAuth2.
     */
    public String loginWithMeta() throws OAuthException, DatabaseException {
        String appId     = props.getProperty("META_APP_ID", "").trim();
        String appSecret = props.getProperty("META_APP_SECRET", "").trim();

        if (appId.isBlank()) {
            throw new OAuthException(
                    "Meta OAuth non configurato. " +
                    "Aggiungi META_APP_ID e META_APP_SECRET in app.properties.");
        }

        MetaAuthBoundary boundary = new MetaAuthBoundary(appId, appSecret, REDIRECT_URI);
        String state   = UUID.randomUUID().toString();
        String authUrl = boundary.getAuthorizationUrl(state);

        return performFlow(authUrl, boundary::fetchUserProfile);
    }

    // ----------------------------------------------------------------
    // Flusso comune
    // ----------------------------------------------------------------

    private String performFlow(String authUrl, ProfileFetcher fetcher)
            throws OAuthException, DatabaseException {

        openBrowser(authUrl);

        OAuthCallbackServer callbackServer = new OAuthCallbackServer(CALLBACK_PORT);
        String code = callbackServer.waitForCode();

        SocialLoginBean profile = fetcher.fetch(code);

        return resolveSession(profile);
    }

    private String resolveSession(SocialLoginBean profile)
            throws DatabaseException, OAuthException {

        DaoFactory factory = DaoFactory.getInstance();
        UserDao    userDao = factory.createUserDao();

        try (Connection conn = factory.getConnection()) {

            User user = findOrRegister(factory, userDao, conn, profile);

            if (!user.isActive()) {
                throw new OAuthException("Account disabilitato. Contatta l'amministratore.");
            }

            return SessionManager.getInstance().createSession(user);

        } catch (SQLException e) {
            throw new DatabaseException("Errore durante il social login.", e);
        }
    }

    private User findOrRegister(DaoFactory factory, UserDao userDao,
                                Connection conn, SocialLoginBean profile)
            throws DatabaseException, OAuthException {
        try {
            return userDao.findByEmail(conn, profile.getEmail());
        } catch (UserNotFoundException e) {
            return registerOAuthStudent(factory, conn, profile);
        }
    }

    private User registerOAuthStudent(DaoFactory factory, Connection conn,
                                      SocialLoginBean profile)
            throws DatabaseException, OAuthException {

        Student student = new Student.Builder()
                .username(generateUsername(profile))
                .email(profile.getEmail())
                .name(profile.getName())
                .surname(profile.getSurname().isBlank() ? "–" : profile.getSurname())
                .passwordHash(null)
                .active(true)
                .createdAt(LocalDateTime.now())
                .budget(BigDecimal.ZERO)
                .build();

        StudentDao studentDao = factory.createStudentDao();
        try {
            studentDao.insert(conn, student);
        } catch (DuplicateUserException ex) {
            throw new OAuthException(
                    "Impossibile registrare l'utente OAuth: " + ex.getMessage(), ex);
        }

        LOGGER.info("Nuovo utente OAuth registrato: " + student.getUsername()
                    + " (" + profile.getProvider() + ")");
        return student;
    }

    // ----------------------------------------------------------------
    // Utility
    // ----------------------------------------------------------------

    private void openBrowser(String url) throws OAuthException {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            } else {
                throw new OAuthException(
                        "Impossibile aprire il browser automaticamente. " +
                        "Visita manualmente: " + url);
            }
        } catch (IOException e) {
            throw new OAuthException("Impossibile aprire il browser.", e);
        }
    }

    private String generateUsername(SocialLoginBean profile) {
        String base = (profile.getName() + profile.getSurname())
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
        if (base.isBlank()) base = "user";
        return base + "_" + UUID.randomUUID().toString().substring(0, 6);
    }

    private Properties loadProperties() {
        Properties p = new Properties();
        try (InputStream in = getClass().getResourceAsStream(PROPERTIES_FILE)) {
            if (in != null) p.load(in);
        } catch (IOException e) {
            LOGGER.warning("Impossibile caricare app.properties per la config OAuth.");
        }
        return p;
    }

    // ----------------------------------------------------------------
    // Interfaccia funzionale interna
    // ----------------------------------------------------------------

    @FunctionalInterface
    private interface ProfileFetcher {
        SocialLoginBean fetch(String code) throws OAuthException;
    }
}
