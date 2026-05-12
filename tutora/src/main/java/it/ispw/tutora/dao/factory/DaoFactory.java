package it.ispw.tutora.dao.factory;

import it.ispw.tutora.dao.*;
import it.ispw.tutora.exception.DatabaseException;

import java.io.IOException;
import java.sql.Connection;
import java.io.InputStream;
import java.util.Properties;

/**
 * Abstract Factory per i DAO.
 *
 * -----------------------------------------------------------------------
 * Pattern Abstract Factory (GoF – Creazionale)
 * -----------------------------------------------------------------------
 * Disaccoppia il codice applicativo (Controller) dalla tecnologia di
 * persistenza concreta. I Controller ottengono i DAO chiamando i metodi
 * factory su questa classe, senza mai istanziare direttamente né le
 * classi *DaoDb, né *DaoDemo, né *DaoJson.
 *
 * -----------------------------------------------------------------------
 * Pattern Singleton – Bill Pugh (Initialization-on-demand Holder)
 * -----------------------------------------------------------------------
 * L'unico Singleton è qui in DaoFactory — le Concrete Factory
 * (DbDaoFactory, DemoDaoFactory, JsonDaoFactory) non hanno bisogno
 * di essere Singleton autonome. Il ciclo di vita è gestito qui.
 * app.properties viene letto una sola volta alla prima chiamata
 * di getInstance().
 *
 * -----------------------------------------------------------------------
 * Configurazione
 * -----------------------------------------------------------------------
 * Il tipo di factory è scelto alla lettura del file /app.properties
 * (classpath), tramite la chiave DAO_TYPE:
 *
 *   DAO_TYPE=DB    → DbDaoFactory   (MySQL via JDBC)
 *   DAO_TYPE=DEMO  → DemoDaoFactory (in-memory, nessun DB)
 *   DAO_TYPE=JSON  → JsonDaoFactory (file JSON su disco)
 *
 * Se la chiave è assente o il file non esiste, si usa DB come default.
 */
public abstract class DaoFactory {

    private static final String PROPERTIES_FILE = "/app.properties";
    private static final String KEY_DAO_TYPE = "DAO_TYPE";

    // ----------------------------------------------------------------
    // Singleton – Bill Pugh Holder
    // ----------------------------------------------------------------

    private static class Holder {

        private static final DaoFactory INSTANCE = loadFactory();

        private static DaoFactory loadFactory() {
            String type = readDaoType();
            return switch (type.toUpperCase()) {
                case "DEMO" -> new DemoDaoFactory();
                case "JSON" -> new JsonDaoFactory();
                default     -> new DbDaoFactory();
            };
        }

        private static String readDaoType() {
            try (InputStream in = DaoFactory.class
                    .getResourceAsStream(PROPERTIES_FILE)) {
                if (in == null) return "";
                Properties props = new Properties();
                props.load(in);
                return props.getProperty(KEY_DAO_TYPE, "");
            } catch (IOException e) {
                return "";
            }
        }
    }

    /**
     * Restituisce l'unica istanza della Concrete Factory configurata.
     * Thread-safe senza synchronized — garantito dal class-loading JVM.
     */
    public static DaoFactory getInstance() {
        return Holder.INSTANCE;
    }

    // ----------------------------------------------------------------
    // Connessione
    // ----------------------------------------------------------------

    /**
     * Restituisce una connessione DB se DAO_TYPE=DB, null altrimenti.
     * Permette ai Controller di usare sempre lo stesso pattern
     * try-with-resources senza sapere quale tipo di persistenza è attivo.
     *
     * Utilizzo nei Controller:
     *   try (Connection conn = DaoFactory.getInstance().getConnection()) {
     *       userDao.findByUsername(conn, username);
     *   }
     */
    public Connection getConnection() throws DatabaseException {
        return null; // default: Demo e JSON non hanno connessione
    }

    // ----------------------------------------------------------------
    // Metodi factory — da implementare nelle sottoclassi concrete
    // ----------------------------------------------------------------

    public abstract UserDao createUserDao();
    public abstract StudentDao createStudentDao();
    public abstract TutorDao createTutorDao();
    public abstract CategoryDao createCategoryDao();
    public abstract NotificationDao createNotificationDao();

    public abstract TutorApplicationDao createTutorApplicationDao();
    public abstract ApplicationItemDao createApplicationItemDao();
    public abstract DocumentDao createDocumentDao();

    public abstract LessonDao createLessonDao();
    public abstract BookingDao createBookingDao();
    public abstract TutorExpertiseDao createTutorExpertiseDao();
    public abstract ReviewDao createReviewDao();
}