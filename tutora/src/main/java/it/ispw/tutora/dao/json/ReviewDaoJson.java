package it.ispw.tutora.dao.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.ispw.tutora.dao.ReviewDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateReviewException;
import it.ispw.tutora.exception.ReviewNotFoundException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Review;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.Tutor;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementazione di ReviewDao basata su file JSON.
 * Usata quando DAO_TYPE=JSON in app.properties.
 *
 * -----------------------------------------------------------------------
 * Persistenza
 * -----------------------------------------------------------------------
 * Legge e scrive su ../tutora_data/reviews.json.
 * Ogni operazione di scrittura applica il pattern read-modify-write.
 *
 * -----------------------------------------------------------------------
 * Oggetti parziali
 * -----------------------------------------------------------------------
 * In lettura vengono ricostruiti oggetti Booking, Student e Tutor parziali,
 * contenenti solo i campi presenti nel record JSON (id / username).
 * Identico al comportamento di ReviewDaoDb.mapReview():
 * per oggetti completi il Controller esegue query aggiuntive
 * tramite i rispettivi DAO.
 *
 * -----------------------------------------------------------------------
 * Controllo duplicati
 * -----------------------------------------------------------------------
 * insertReview() verifica che non esista già una recensione per lo stesso
 * bookingId prima di procedere con l'inserimento.
 *
 * -----------------------------------------------------------------------
 * Nota sul parametro Connection
 * -----------------------------------------------------------------------
 * Il parametro Connection viene ignorato: non c'è nessun DB.
 * È presente solo per rispettare il contratto dell'interfaccia ReviewDao,
 * pensata per la gestione delle transazioni JDBC.
 */
public class ReviewDaoJson implements ReviewDao {

    private static final String JSON_PATH = "../tutora_data/reviews.json";
    private final ObjectMapper mapper = new ObjectMapper();

    // ----------------------------------------------------------------
    // insertReview
    // ----------------------------------------------------------------

    @Override
    public int insertReview(Connection conn, Review rev)
            throws DatabaseException, DuplicateReviewException {

        List<ReviewRecord> records = readAll();

        for (ReviewRecord r : records) {
            if (r.bookingId == rev.getBooking().getId()) {
                throw new DuplicateReviewException(rev.getBooking().getId());
            }
        }

        // Simulo in Json un ID auto-incremented
        int id = records.stream().mapToInt(r -> r.id).max().orElse(0) + 1;


        ReviewRecord newRecord = new ReviewRecord();
        newRecord.id = id;
        newRecord.bookingId = rev.getBooking().getId();
        newRecord.studentUsername = rev.getStudent().getUsername();
        newRecord.tutorUsername = rev.getTutor().getUsername();
        newRecord.rating = rev.getRating();
        newRecord.comment = rev.getComment();
        newRecord.createdAt = LocalDateTime.now().toString();

        records.add(newRecord);
        writeAll(records);
        recalculateTutorRating(newRecord.tutorUsername);
        return id;
    }

    // ----------------------------------------------------------------
    // updateReview
    // ----------------------------------------------------------------

    @Override
    public void updateReview(Connection conn, Review rev)
            throws DatabaseException, ReviewNotFoundException {

        List<ReviewRecord> records = readAll();
        for (ReviewRecord r : records) {
            if (r.id == rev.getId()) {
                String tutorUsername = r.tutorUsername;
                r.rating  = rev.getRating();
                r.comment = rev.getComment();
                writeAll(records);
                recalculateTutorRating(tutorUsername);
                return;
            }
        }
        throw new ReviewNotFoundException(rev.getId());
    }

    // ----------------------------------------------------------------
    // deleteReview
    // ----------------------------------------------------------------

    @Override
    public void deleteReview(Connection conn, int id)
            throws DatabaseException, ReviewNotFoundException {

        List<ReviewRecord> records = readAll();
        String tutorUsername = null;
        for (ReviewRecord r : records) {
            if (r.id == id) { tutorUsername = r.tutorUsername; break; }
        }
        if (tutorUsername == null) throw new ReviewNotFoundException(id);
        records.removeIf(r -> r.id == id);
        writeAll(records);
        recalculateTutorRating(tutorUsername);
    }

    // ----------------------------------------------------------------
    // findByTutor
    // ----------------------------------------------------------------

    @Override
    public List<Review> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException {

        List<Review> result = new ArrayList<>();
        for (ReviewRecord r : readAll()) {
            if (r.tutorUsername.equals(tutorUsername)) result.add(toReview(r));
        }
        result.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return result;
    }

    // ----------------------------------------------------------------
    // selectReview
    // ----------------------------------------------------------------

    @Override
    public Review selectReview(Connection conn, int id)
            throws DatabaseException, ReviewNotFoundException {

        for (ReviewRecord r : readAll()) {
            if (r.id == id) return toReview(r);
        }
        throw new ReviewNotFoundException(id);
    }

    // ----------------------------------------------------------------
    // Helper privati
    // ----------------------------------------------------------------

    private void recalculateTutorRating(String tutorUsername) throws DatabaseException {
        List<ReviewRecord> all = readAll();
        List<ReviewRecord> forTutor = all.stream()
                .filter(r -> tutorUsername.equals(r.tutorUsername))
                .toList();
        int count = forTutor.size();
        BigDecimal avg = BigDecimal.ZERO;
        if (count > 0) {
            int sum = forTutor.stream().mapToInt(r -> r.rating).sum();
            avg = new BigDecimal(sum).divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
        }
        new UserDaoJson().updateTutorRating(tutorUsername, avg, count);
    }

    private List<ReviewRecord> readAll() throws DatabaseException {
        try {
            ReviewRecord[] records = mapper.readValue(new File(JSON_PATH), ReviewRecord[].class);
            return new ArrayList<>(Arrays.asList(records));
        } catch (IOException e) {
            throw new DatabaseException("Error reading JSON file: " + JSON_PATH, e);
        }
    }

    private void writeAll(List<ReviewRecord> records) throws DatabaseException {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(JSON_PATH), records);
        } catch (IOException e) {
            throw new DatabaseException("Error writing JSON file: " + JSON_PATH, e);
        }
    }

    private Review toReview(ReviewRecord r) {
        // Oggetti parziali — solo i campi presenti in reviews.json
        Booking partialBooking = new Booking.Builder().id(r.bookingId).build();
        Student partialStudent = new Student.Builder().username(r.studentUsername).build();
        Tutor partialTutor = new Tutor.Builder().username(r.tutorUsername).build();

        return new Review.Builder()
                .id(r.id)
                .booking(partialBooking)
                .student(partialStudent)
                .tutor(partialTutor)
                .rating(r.rating)
                .comment(r.comment)
                .createdAt(LocalDateTime.parse(r.createdAt))
                .build();
    }

    // ----------------------------------------------------------------
    // POJO interno per la deserializzazione Jackson
    // ----------------------------------------------------------------

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static class ReviewRecord {
        int id;
        int bookingId;
        String studentUsername;
        String tutorUsername;
        int rating;
        String comment;
        String createdAt;
    }
}
