package it.ispw.tutora.dao;

import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.Tutor;

import java.sql.Connection;
import java.util.List;

/**
 * Contratto DAO per le operazioni specifiche del Tutor.
 * Estende UserDao: copre anche insert, findByUsername,
 * updatePassword e updateProfile sulla tabella user.
 *
 * -----------------------------------------------------------------------
 * Nota sul rating
 * -----------------------------------------------------------------------
 * rating e ratingCount sono aggiornati esclusivamente dai trigger SQL
 * (trg_review_after_insert / update / delete): il layer Java non
 * li scrive mai. selectTutor li rilegge dal DB dopo ogni operazione
 * che potrebbe averli modificati (es. dopo una nuova recensione).
 *
 * -----------------------------------------------------------------------
 * Nota sul pattern Table-Per-Subclass
 * -----------------------------------------------------------------------
 * La tabella tutor è collegata via FK a user.
 * selectTutor esegue il JOIN per ricostruire un oggetto Tutor completo,
 * mentre findByUsername() (ereditato da UserDaoDb) legge solo user
 * ed è sufficiente per il login.
 */
public interface TutorDao extends UserDao {

    Tutor selectTutor(Connection conn, String username)
            throws DatabaseException, UserNotFoundException;

    List<Tutor> selectAllTutors(Connection conn) throws DatabaseException;
}
