package it.ispw.tutora.bean;

/**
 * Bean di trasporto per la creazione o modifica di una recensione da parte dello student.
 *
 * Trasporta dal boundary al Controller i dati che lo student deve fornire:
 *   - bookingId identifica la booking su cui si lascia la recensione.
 *     Il Controller ricava il tutor direttamente dalla booking → lezione → expertise,
 *     senza che lo student debba fornire il tutorUsername.
 *   - rating è il voto (1–5), validato dal model Review.checkRating().
 *   - comment è il testo opzionale della recensione.
 *
 * Separato da BookingBean per SRP: BookingBean trasporta la prenotazione,
 * ReviewBean trasporta esclusivamente i dati di valutazione.
 */
public class ReviewBean {

    private int bookingId;
    private int rating;
    private String comment;



    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
