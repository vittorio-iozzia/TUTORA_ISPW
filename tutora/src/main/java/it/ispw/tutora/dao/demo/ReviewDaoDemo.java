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

public class ReviewDaoDemo implements ReviewDao {

    private final Map<Integer, Review> cache = new HashMap<>();
    private int nextId = 1; // simula AUTO_INCREMENT

    @Override
    public int insertReview(Connection conn, Review rev)
            throws DatabaseException, DuplicateReviewException{
        for (Review r: cache.values()){
            if (r.getBooking().getId()==rev.getBooking().getId())
                throw new DuplicateReviewException(rev.getId());
        }int id = nextId++;
        cache.put(id, rev);
        return id;
    }
    @Override
    public void updateReview(Connection conn, Review rev)
            throws DatabaseException, ReviewNotFoundException {
        if (!cache.containsKey(rev.getId()))
            throw new ReviewNotFoundException(rev.getId());
        cache.put(rev.getId(), rev);
    }
    @Override
    public void deleteReview(Connection conn, int id)
            throws DatabaseException, ReviewNotFoundException{
            if (!cache.containsKey(id))
                throw new ReviewNotFoundException(id);
            cache.remove(id);
    }
    @Override
    public List<Review> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException{
        return cache.values().stream()
                .filter(r -> r.getTutor().getUsername().equals(tutorUsername))
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .toList();
    }
    @Override
    public Review selectReview(Connection conn, int id)
            throws ReviewNotFoundException {
        Review review = cache.get(id);
        if (review == null) throw new ReviewNotFoundException(id);
        return review;
    }
}
