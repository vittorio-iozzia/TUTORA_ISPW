package it.ispw.tutora.dao;

import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateReviewException;
import it.ispw.tutora.exception.ReviewNotFoundException;
import it.ispw.tutora.model.Review;

import java.sql.Connection;
import java.util.List;

/**
 * Contratto DAO per la tabella review.
 *
 * -----------------------------------------------------------------------
 * Nota sulle transazioni
 * -----------------------------------------------------------------------
 * I metodi di scrittura (insertReview, updateReview, deleteReview)
 * ricevono la Connection come parametro: è il Controller applicativo
 * a gestire commit e rollback, non il DAO.
 * insertReview viene tipicamente invocato nella stessa transazione
 * in cui viene aggiornato il rating del tutor (gestito automaticamente
 * dai trigger SQL trg_review_after_insert / update / delete).
 *
 * -----------------------------------------------------------------------
 * Nota su findByTutor
 * -----------------------------------------------------------------------
 * findByTutor restituisce una lista vuota se il tutor non ha recensioni:
 * non lancia ReviewNotFoundException perché l'assenza di recensioni
 * non è una condizione di errore ma un caso legittimo
 * (tutor nuovo o senza valutazioni).
 *
 * -----------------------------------------------------------------------
 * Nota sui trigger rating
 * -----------------------------------------------------------------------
 * insert, update e delete su review aggiornano automaticamente
 * tutor.rating e tutor.rating_count tramite trigger SQL.
 * Il layer Java NON deve aggiornare il rating manualmente.
 */
public interface ReviewDao {

    /**
     * Persiste una nuova recensione.
     * Prima dell'INSERT verifica che non esista già una recensione
     * per la stessa booking (vincolo UNIQUE uq_review_booking).
     */
    int insertReview(Connection conn, Review rev)
            throws DatabaseException, DuplicateReviewException;

    /**
     * Aggiorna rating e comment di una recensione esistente.
     */
    void updateReview(Connection conn, Review rev)
            throws DatabaseException, ReviewNotFoundException;

    /**
     * Elimina fisicamente una recensione.
     * Il trigger trg_review_after_delete aggiorna automaticamente
     * rating e rating_count del tutor.
     */
    void deleteReview(Connection conn, int id)
            throws DatabaseException, ReviewNotFoundException;

    /**
     * Carica tutte le recensioni di un tutor ordinate per data
     * decrescente (più recenti prima).
     * Restituisce una lista vuota se il tutor non ha recensioni.
     */
    List<Review> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException;

    /**
     * Carica una recensione per id.
     */
    Review selectReview(Connection conn, int id)
            throws DatabaseException, ReviewNotFoundException;
}