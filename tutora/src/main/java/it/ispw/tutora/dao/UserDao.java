package it.ispw.tutora.dao;

import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateUserException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.User;

import java.sql.Connection;

/**
 * Contratto DAO per la tabella user e le sue tabelle figlio
 * (student, tutor, admin) — schema Table-Per-Subclass.
 *
 * -----------------------------------------------------------------------
 * Nota sulle transazioni
 * -----------------------------------------------------------------------
 * Tutti i metodi ricevono la Connection come parametro: è il Controller
 * applicativo a gestire commit e rollback, non il DAO.
 * L'inserimento di un User richiede almeno due INSERT atomiche
 * (tabella user + tabella specifica del ruolo), quindi partecipa
 * sempre a una transazione esplicita.
 *
 * -----------------------------------------------------------------------
 * Nota sul polimorfismo
 * -----------------------------------------------------------------------
 * findByUsername restituisce il sottotipo concreto corretto
 * (Student, Tutor o Admin) basandosi sulla colonna role della
 * tabella user, eseguendo il JOIN con la tabella del ruolo
 * corrispondente. Il chiamante può fare il cast se ha necessità
 * di accedere ai campi specifici del ruolo.
 *
 * -----------------------------------------------------------------------
 * Nota sulla password
 * -----------------------------------------------------------------------
 * Il campo passwordHash non ha getter in User: circola solo nel DAO,
 * che lo legge dal ResultSet e lo passa al Builder. All'esterno la
 * verifica avviene tramite User.matchesPassword(rawPassword).
 * updatePassword riceve già l'hash calcolato dal chiamante (BCrypt).
 */
public interface UserDao {

    /**
     * Persiste un nuovo utente: INSERT in user + INSERT nella
     * tabella specifica del ruolo (student, tutor o admin).
     * Le due operazioni devono essere atomiche (stessa transazione).
     *
     * @throws DuplicateUserException se username o email sono già presenti
     */
    void insert(Connection conn, User user)
            throws DatabaseException, DuplicateUserException;

    /**
     * Carica un utente per username, ricostruendo il sottotipo corretto.
     * Usata principalmente durante il login e per il caricamento del profilo.
     *
     * @throws UserNotFoundException se lo username non corrisponde
     *         ad alcuna riga in user
     */
    User findByUsername(Connection conn, String username)
            throws DatabaseException, UserNotFoundException;

    /**
     * Aggiorna il passwordHash nella tabella user.
     * Il chiamante è responsabile di calcolare l'hash (BCrypt)
     * prima di invocare questo metodo.
     *
     * @param newPasswordHash hash BCrypt della nuova password
     * @throws UserNotFoundException se lo username non corrisponde
     *         ad alcuna riga in user
     */
    void updatePassword(Connection conn, String username, String newPasswordHash)
            throws DatabaseException, UserNotFoundException;

    /**
     * Aggiorna i campi mutabili del profilo (description, isActive).
     * Non aggiorna passwordHash — usare updatePassword per quello.
     *
     * @throws UserNotFoundException se lo username non corrisponde
     *         ad alcuna riga in user
     */
    void updateProfile(Connection conn, User user)
            throws DatabaseException, UserNotFoundException;
}
