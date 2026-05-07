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
 * Singleton that provides JDBC connections to the MySQL database.
 *
 * -----------------------------------------------------------------------
 * Pattern Singleton – Bill Pugh (Initialization-on-demand Holder)
 * -----------------------------------------------------------------------
 * Unlike the single static Connection approach, each call to
 * getConnection() opens a fresh connection. This avoids:
 *   - Stale connections (MySQL closes idle connections after ~8h)
 *   - Conflicts between concurrent operations on the same Connection
 *   - autoCommit locked to true, which prevents transactions
 *
 * The caller (DAO or application Controller) is responsible for
 * closing the connection in its own try-with-resources block.
 */
public class ConnectionFactory {

    private static final Logger LOGGER = Logger.getLogger(ConnectionFactory.class.getName());

    private static final String PROPERTIES_FILE = "/db.properties";
    private static final String KEY_URL = "CONNECTION_URL";
    private static final String KEY_USER = "LOGIN_USER";
    private static final String KEY_PASS = "LOGIN_PASS";

    private final String url;
    private final String user;
    private final String pass;

    // ----------------------------------------------------------------
    // Private constructor
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
                    "Unable to read " + PROPERTIES_FILE + ": " + e.getMessage()
            );
        }

        this.url  = props.getProperty(KEY_URL);
        this.user = props.getProperty(KEY_USER);
        this.pass = props.getProperty(KEY_PASS);

        if (url == null || user == null || pass == null) {
            throw new ExceptionInInitializerError(
                    "db.properties is incomplete: verify keys " +
                            KEY_URL + ", " + KEY_USER + ", " + KEY_PASS
            );
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
     * Returns the unique ConnectionFactory instance.
     * Thread-safe without synchronization — guaranteed by JVM class loading.
     */
    public static ConnectionFactory getInstance() {
        return Holder.INSTANCE;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Opens and returns a new JDBC connection.
     *
     * autoCommit defaults to TRUE — suitable for single operations.
     * For multi-step transactions the application Controller calls
     * conn.setAutoCommit(false) before starting the sequence.
     *
     * @return a new Connection to TUTORA_db
     * @throws DatabaseException if the connection cannot be opened
     */
    public Connection getConnection() throws DatabaseException {
        try {
            return DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            LOGGER.severe("Error opening connection: " + e.getMessage());
            throw new DatabaseException(
                    "Unable to connect to the database: " + e.getMessage(), e
            );
        }
    }
}