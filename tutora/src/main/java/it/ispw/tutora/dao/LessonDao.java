package it.ispw.tutora.dao;

import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateLessonException;
import it.ispw.tutora.exception.LessonNotFoundException;
import it.ispw.tutora.model.Lesson;

import java.sql.Connection;
import java.util.List;

/**
 * Contratto DAO per la tabella lesson.
 *
 * -----------------------------------------------------------------------
 * Nota sulle transazioni
 * -----------------------------------------------------------------------
 * Tutti i metodi di scrittura ricevono la Connection come parametro:
 * è il Controller applicativo a gestire commit e rollback, non il DAO.
 *
 * -----------------------------------------------------------------------
 * Nota sul Lesson Overlap
 * -----------------------------------------------------------------------
 * insertLesson() e updateLesson() devono verificare l'assenza di
 * sovrapposizioni temporali per lo stesso tutor prima di procedere.
 * Se viene rilevata una sovrapposizione va lanciata DuplicateLessonException.
 * Query di controllo:
 *   SELECT COUNT(*) FROM lesson
 *   WHERE tutor_username = ?
 *     AND status NOT IN ('Cancelled')
 *     AND start_time < ? AND end_time > ?
 */
public interface LessonDao {

    /**
     * Persiste un nuovo slot lezione.
     * Verifica l'assenza di sovrapposizioni temporali prima dell'INSERT.
     */
    int insertLesson(Connection conn, Lesson newLesson)
            throws DatabaseException, DuplicateLessonException;

    /**
     * Aggiorna i campi modificabili di una lezione (orari, prezzo, modalità).
     * Verifica l'assenza di sovrapposizioni temporali prima dell'UPDATE.
     */
    void updateLesson(Connection conn, Lesson lesson)
            throws DatabaseException, LessonNotFoundException, DuplicateLessonException;

    /**
     * Aggiorna solo lo status della lezione applicando la macchina a stati.
     * La validazione delle transizioni è già garantita dal Model (Lesson.updateLessonStatus).
     */
    void updateStatus(Connection conn, int id, LessonStatus newStatus)
            throws DatabaseException, LessonNotFoundException;

    /**
     * Carica una lezione per id.
     */
    Lesson selectLesson(Connection conn, int id)
            throws DatabaseException, LessonNotFoundException;

    /**
     * Carica tutte le lezioni di un tutor, ordinate per start_time crescente.
     * Restituisce una lista vuota se il tutor non ha lezioni.
     */
    List<Lesson> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException;

    /**
     * Carica le lezioni di un tutor filtrate per status,
     * ordinate per start_time crescente.
     * Restituisce una lista vuota se non ci sono lezioni con quel status.
     */
    List<Lesson> findByTutorAndStatus(Connection conn, String tutorUsername, LessonStatus status)
            throws DatabaseException;
}