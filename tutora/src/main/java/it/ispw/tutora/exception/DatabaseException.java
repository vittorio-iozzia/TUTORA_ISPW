package it.ispw.tutora.exception;

/**
 * Lanciata quando un'operazione JDBC fallisce (connessione, query, ecc.).
 * Avvolge la SQLException originale così il layer superiore non dipende
 * da java.sql e rimane disaccoppiato dall'implementazione del DAO.
 */
public class DatabaseException extends TutoraException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}