package it.ispw.tutora.dao.demo;

import it.ispw.tutora.dao.ReviewDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateReviewException;
import it.ispw.tutora.exception.ReviewNotFoundException;
import it.ispw.tutora.model.Review;
import it.ispw.tutora.model.Tutor;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        // Ricostruisce la recensione con l'id auto-assegnato, come fa DocumentDaoDemo:
        // id è final → non modificabile dopo la costruzione, va impostato nel Builder.
        int id = nextId++;
        Review stored = new Review.Builder()
                .id(id)
                .booking(rev.getBooking())
                .student(rev.getStudent())
                .tutor(rev.getTutor())
                .rating(rev.getRating())
                .comment(rev.getComment())
                .createdAt(rev.getCreatedAt())
                .build();
        cache.put(id, stored);
        recalcTutorRating(rev.getTutor());
        return id;
    }

    private void recalcTutorRating(Tutor tutor) {
        List<Review> all = cache.values().stream()
                .filter(r -> r.getTutor().getUsername().equals(tutor.getUsername()))
                .toList();
        if (all.isEmpty()) return;
        double avg = all.stream().mapToInt(Review::getRating).average().orElse(0.0);
        tutor.setRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP), all.size());
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
        recalcTutorRating(rev.getTutor());
    }

    /**
     * Elimina fisicamente una recensione dalla cache.
     */
    @Override
    public void deleteReview(Connection conn, int id)
            throws DatabaseException, ReviewNotFoundException {
        Review rev = cache.get(id);
        if (rev == null) throw new ReviewNotFoundException(id);
        cache.remove(id);
        recalcTutorRating(rev.getTutor());
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
