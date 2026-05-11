package it.ispw.tutora.dao.json;

import it.ispw.tutora.dao.TutorDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateUserException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.User;

import java.sql.Connection;

/**
 * Implementazione di TutorDao basata su file JSON.
 * Usata quando DAO_TYPE=JSON in app.properties.
 *
 * -----------------------------------------------------------------------
 * Pattern di ereditarietà
 * -----------------------------------------------------------------------
 * Estende UserDaoJson per ereditare tutte le operazioni UserDao
 * (insert, findByUsername, updatePassword, updateProfile) che operano
 * sul file condiviso users.json. Specchia il pattern DB in cui
 * TutorDaoDb estende UserDaoDb.
 *
 * -----------------------------------------------------------------------
 * Responsabilità aggiuntive rispetto a UserDaoJson
 * -----------------------------------------------------------------------
 * insert (override) — valida che l'oggetto passato sia un Tutor prima
 *   di delegare a super.insert(). Il record scritto in users.json
 *   include già rating e ratingCount grazie alla logica di toRecord()
 *   in UserDaoJson.
 *
 * selectTutor — carica un Tutor completo chiamando findByUsername()
 *   e facendo il cast al sottotipo. Equivalente a TutorDaoDb.selectTutor()
 *   che esegue il JOIN user + tutor per ricostruire l'oggetto completo.
 *   Nel JSON non serve un JOIN perché tutti i campi sono già nello stesso record.
 *
 * -----------------------------------------------------------------------
 * Nota sul parametro Connection
 * -----------------------------------------------------------------------
 * Il parametro Connection viene ignorato: non c'è nessun DB.
 * È presente solo per rispettare il contratto dell'interfaccia TutorDao,
 * pensata per la gestione delle transazioni JDBC.
 */
public class TutorDaoJson extends UserDaoJson implements TutorDao {

    // ----------------------------------------------------------------
    // insert (override)
    // ----------------------------------------------------------------

    @Override
    public void insert(Connection conn, User user)
            throws DatabaseException, DuplicateUserException {

        if (!(user instanceof Tutor)) {
            throw new IllegalArgumentException("Expected Tutor instance.");
        }
        super.insert(conn, user);
    }

    // ----------------------------------------------------------------
    // selectTutor
    // ----------------------------------------------------------------

    @Override
    public Tutor selectTutor(Connection conn, String username)
            throws DatabaseException, UserNotFoundException {

        User user = findByUsername(conn, username);
        if (!(user instanceof Tutor tutor)) {
            throw new UserNotFoundException(username);
        }
        return tutor;
    }
}
