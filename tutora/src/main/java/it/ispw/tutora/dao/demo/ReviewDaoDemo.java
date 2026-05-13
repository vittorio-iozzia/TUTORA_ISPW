package it.ispw.tutora.dao.demo;

import it.ispw.tutora.dao.ReviewDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateReviewException;
import it.ispw.tutora.exception.ReviewNotFoundException;
import it.ispw.tutora.model.Review;

import java.sql.Connection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementazione in memoria di ReviewDao.
 * Usata al posto di ReviewDaoDb quando il DB non è disponibile.
 *
 * Le recensioni sono indicizzate per id in una {@link HashMap}.
 * {@code nextId} simula l'AUTO_INCREMENT del DB: parte da 1 e viene
 * incrementato ad ogni {@code insertReview}.
 *
 * In produzione, insert/update/delete su review aggiornano
 * automaticamente {@code tutor.rating} e {@code tutor.rating_count}
 * tramite trigger SQL. In modalità demo questa logica è assente:
 * il rating del tutor non viene aggiornato.
 */
public class ReviewDaoDemo implements ReviewDao {

    private final Map<Integer, Review> cache = new HashMap<>();
    private int nextId = 1; // simula AUTO_INCREMENT

    /**
     * Inserisce una nuova recensione assegnandole un id progressivo.
     * Prima dell'inserimento verifica che non esista già una recensione
     * per la stessa booking (emula il vincolo UNIQUE {@code uq_review_booking}).
     */
    @Override
    public int insertReview(Connection conn, Review rev)
            throws DatabaseException, DuplicateReviewException {
        for (Review r : cache.values()) {
            if (r.getBooking().getId() == rev.getBooking().getId())
                throw new DuplicateReviewException(rev.getId());
        }
        int id = nextId++;
        cache.put(id, rev);
        return id;
    }

    /**
     * Aggiorna rating e commento di una recensione esistente.
     * La modifica avviene sovrascrivendo il valore nella cache con lo stesso id.
     */
    @Override
    public void updateReview(Connection conn, Review rev)
            throws DatabaseException, ReviewNotFoundException {
        if (!cache.containsKey(rev.getId()))
            throw new ReviewNotFoundException(rev.getId());
        cache.put(rev.getId(), rev);
    }

    /**
     * Elimina fisicamente una recensione dalla cache.
     */
    @Override
    public void deleteReview(Connection conn, int id)
            throws DatabaseException, ReviewNotFoundException {
        if (!cache.containsKey(id))
            throw new ReviewNotFoundException(id);
        cache.remove(id);
    }

    /**
     * Restituisce tutte le recensioni di un tutor ordinate per data
     * decrescente (più recenti prima).
     * Restituisce una lista vuota se il tutor non ha recensioni.
     */
    @Override
    public List<Review> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException {
        return cache.values().stream()
                .filter(r -> r.getTutor().getUsername().equals(tutorUsername))
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .toList();
    }

    /**
     * Carica una recensione per id.
     */
    @Override
    public Review selectReview(Connection conn, int id)
            throws DatabaseException, ReviewNotFoundException {
        Review review = cache.get(id);
        if (review == null) throw new ReviewNotFoundException(id);
        return review;
    }
}
