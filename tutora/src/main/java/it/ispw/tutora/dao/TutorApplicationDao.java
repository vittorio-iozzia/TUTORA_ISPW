package it.ispw.tutora.dao;

import it.ispw.tutora.enums.ApplicationStatus;
import it.ispw.tutora.exception.ApplicationNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateApplicationException;
import it.ispw.tutora.model.TutorApplication;

import java.sql.Connection;
import java.util.List;

/**
 * Contratto DAO per la tabella tutor_application.
 *
 * -----------------------------------------------------------------------
 * Nota sulle transazioni
 * -----------------------------------------------------------------------
 * I metodi che partecipano a transazioni multi-tabella ricevono
 * la Connection come parametro: è il Controller applicativo a gestire
 * commit e rollback, non il DAO.
 *
 * Esempio di transazione nel Controller:
 *   conn.setAutoCommit(false);
 *   try {
 *       tutorApplicationDao.insert(conn, application);
 *       notificationDao.insert(conn, notification);
 *       conn.commit();
 *   } catch (Exception e) {
 *       conn.rollback();
 *   }
 */
public interface TutorApplicationDao {

    /**
     * Persiste una nuova application in stato DRAFT.
     * Imposta active_key = studentUsername come da contratto SQL.
     */
    int insert(Connection conn, TutorApplication application)
            throws DatabaseException, DuplicateApplicationException;

    /**
     * Aggiorna lo status e, se terminale, imposta
     * active_key = id + "_closed".
     * In caso di ACCEPTED / REJECTED salva anche
     * adminNotes e evaluatedAt.
     */
    void updateStatus(Connection conn, TutorApplication application)
            throws DatabaseException, ApplicationNotFoundException;

    /**
     * Carica un'application per id, senza gli item (lazy).
     */
    TutorApplication findById(Connection conn, int id)
            throws DatabaseException, ApplicationNotFoundException;

    /**
     * Carica tutte le application di uno studente,
     * ordinate per data decrescente.
     */
    List<TutorApplication> findByStudent(Connection conn, String studentUsername)
            throws DatabaseException;

    /**
     * Carica le application in un determinato stato.
     * Usata dalla dashboard admin per vedere le SUBMITTED.
     */
    List<TutorApplication> findByStatus(Connection conn, ApplicationStatus status)
            throws DatabaseException;
}