package it.ispw.tutora.dao.factory;

import it.ispw.tutora.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Singleton che fornisce connessioni JDBC al database MySQL.
 *
 * -----------------------------------------------------------------------
 * Pattern Singleton – Bill Pugh (Initialization-on-demand Holder)
 * -----------------------------------------------------------------------
 * A differenza dell'approccio con connessione statica singola, ogni
 * chiamata a getConnection() apre una connessione nuova. Questo evita:
 *   - Connessioni stale (MySQL chiude le connessioni idle dopo ~8h)
 *   - Conflitti tra operazioni concorrenti sulla stessa Connection
 *   - autoCommit bloccato a true, che impedisce le transazioni
 *
 * Il chiamante (DAO o Controller applicativo) è responsabile di
 * chiudere la connessione nel proprio blocco try-with-resources.
 */
public class ConnectionFactory {

    private static final Logger LOGGER = Logger.getLogger(
            ConnectionFactory.class.getName());

    private static final String PROPERTIES_FILE = "/db.properties";
    private static final String KEY_URL = "CONNECTION_URL";
    private static final String KEY_USER = "LOGIN_USER";
    private static final String KEY_PASS = "LOGIN_PASS";

    private final String url;
    private final String user;
    private final String pass;

    // ----------------------------------------------------------------
    // Costruttore privato
    // ----------------------------------------------------------------

    private ConnectionFactory() {
        Properties props = new Properties();

        try (InputStream in = ConnectionFactory.class
                .getResourceAsStream(PROPERTIES_FILE)) {

            if (in == null) {
                throw new ExceptionInInitializerError(
                        "File '" + PROPERTIES_FILE + "' not found in classpath. " +
                                "Copy db.properties.template to db.properties and configure it."
                );
            }
            props.load(in);

        } catch (IOException e) {
            throw new ExceptionInInitializerError(
                    "Unable to read " + PROPERTIES_FILE + ": " + e.getMessage());
        }

        this.url  = props.getProperty(KEY_URL);
        this.user = props.getProperty(KEY_USER);
        this.pass = props.getProperty(KEY_PASS);

        if (url == null || user == null || pass == null) {
            throw new ExceptionInInitializerError(
                    "db.properties is incomplete: verify keys " +
                            KEY_URL + ", " + KEY_USER + ", " + KEY_PASS);
        }

        LOGGER.info("ConnectionFactory initialized successfully.");
    }

    // ----------------------------------------------------------------
    // Bill Pugh Holder
    // ----------------------------------------------------------------

    private static class Holder {
        private static final ConnectionFactory INSTANCE = new ConnectionFactory();
    }

    /**
     * Restituisce l'unica istanza di ConnectionFactory.
     * Thread-safe senza synchronized — garantito dal class-loading della JVM.
     */
    public static ConnectionFactory getInstance() {
        return Holder.INSTANCE;
    }

    // ----------------------------------------------------------------
    // API pubblica
    // ----------------------------------------------------------------

    /**
     * Apre e restituisce una nuova connessione JDBC.
     *
     * autoCommit è TRUE di default — adatto per operazioni singole.
     * Per le transazioni multi-step il Controller applicativo chiama
     * conn.setAutoCommit(false) prima di iniziare la sequenza.
     */
    public Connection getConnection() throws DatabaseException {
        try {
            return DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            LOGGER.severe("Error opening connection: " + e.getMessage());
            throw new DatabaseException(
                    "Unable to connect to the database: " + e.getMessage(), e);
        }
    }
}