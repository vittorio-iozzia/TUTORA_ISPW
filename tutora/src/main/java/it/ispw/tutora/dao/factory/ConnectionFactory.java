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
 * Rispetto all'approccio con connessione statica singola, qui ogni
 * chiamata a getConnection() apre una connessione nuova. Questo evita:
 *   - Connessioni stale (MySQL chiude idle connections dopo ~8h)
 *   - Conflitti tra operazioni concorrenti sulla stessa Connection
 *   - autoCommit bloccato a true che impedisce le transazioni
 *
 * Il chiamante (DAO o Controller applicativo) è responsabile di
 * chiudere la connessione nel proprio try-with-resources.
 */
public class ConnectionFactory {

    private static final Logger LOGGER = Logger.getLogger(ConnectionFactory.class.getName());

    private static final String PROPERTIES_FILE = "/db.properties";
    private static final String KEY_URL          = "CONNECTION_URL";
    private static final String KEY_USER         = "LOGIN_USER";
    private static final String KEY_PASS         = "LOGIN_PASS";

    private final String url;
    private final String user;
    private final String pass;

    // ----------------------------------------------------------------
    // Costruttore privato
    // ----------------------------------------------------------------

    private ConnectionFactory() {
        Properties props = new Properties();

        try (InputStream in = ConnectionFactory.class.getResourceAsStream(PROPERTIES_FILE)) {

            if (in == null) {
                throw new ExceptionInInitializerError(
                        "File '" + PROPERTIES_FILE + "' non trovato nel classpath. " +
                                "Copia db.properties.template in db.properties e configuralo."
                );
            }
            props.load(in);

        } catch (IOException e) {
            throw new ExceptionInInitializerError(
                    "Impossibile leggere " + PROPERTIES_FILE + ": " + e.getMessage()
            );
        }

        this.url  = props.getProperty(KEY_URL);
        this.user = props.getProperty(KEY_USER);
        this.pass = props.getProperty(KEY_PASS);

        if (url == null || user == null || pass == null) {
            throw new ExceptionInInitializerError(
                    "db.properties incompleto: verificare le chiavi " +
                            KEY_URL + ", " + KEY_USER + ", " + KEY_PASS
            );
        }

        LOGGER.info("ConnectionFactory inizializzata correttamente.");
    }

    // ----------------------------------------------------------------
    // Bill Pugh Holder
    // ----------------------------------------------------------------

    private static class Holder {
        private static final ConnectionFactory INSTANCE = new ConnectionFactory();
    }

    public static ConnectionFactory getInstance() {
        return Holder.INSTANCE;
    }

    // ----------------------------------------------------------------
    // API pubblica
    // ----------------------------------------------------------------

    /**
     * Apre e restituisce una nuova connessione JDBC.
     *
     * autoCommit è TRUE di default: va bene per operazioni singole.
     * Per le transazioni multi-step il Controller applicativo chiama
     * conn.setAutoCommit(false) prima di iniziare la sequenza.
     *
     * @return una nuova Connection verso TUTORA_db
     * @throws DatabaseException se la connessione non può essere aperta
     */
    public Connection getConnection() throws DatabaseException {
        try {
            return DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            LOGGER.severe("Errore apertura connessione: " + e.getMessage());
            throw new DatabaseException(
                    "Impossibile connettersi al database: " + e.getMessage(), e
            );
        }
    }
}