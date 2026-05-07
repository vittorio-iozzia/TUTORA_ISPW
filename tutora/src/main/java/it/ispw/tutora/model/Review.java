package it.ispw.tutora.model;

import java.time.LocalDateTime;

/**
 * Entity che rappresenta una recensione lasciata da uno studente
 * al termine di una lezione completata.
 *
 * Corrisponde alla tabella review del DB.
 *
 * -----------------------------------------------------------------------
 * Pattern Builder (GoF – Creazionale)
 * -----------------------------------------------------------------------
 * Sostituisce i due costruttori sovrapposti (con e senza commento)
 * con un Builder che rende opzionale il campo comment in modo
 * esplicito e leggibile.
 *
 * Utilizzo:
 *   Review review = new Review.Builder()
 *       .id(1)
 *       .booking(booking)
 *       .student(student)
 *       .tutor(tutor)
 *       .rating(5)
 *       .comment("Ottima lezione!")  // opzionale
 *       .createdAt(LocalDateTime.now())
 *       .build();
 *
 * -----------------------------------------------------------------------
 * Logica di dominio
 * -----------------------------------------------------------------------
 * checkRating() è richiamato sia nel costruttore che nel setRating()
 * per centralizzare la validazione del voto in un unico punto.
 * Il rating deve essere compreso tra 1 e 5, come da vincolo
 * CHECK nel DB (chk_review_rating).
 */
public class Review {

    private final int id;
    private final Booking booking;
    private final Student student;
    private final Tutor tutor;
    private int rating;
    private String comment;
    private final LocalDateTime createdAt;

    // Costruttore privato: accessibile solo tramite Builder
    private Review(Builder builder) {
        checkRating(builder.rating);
        this.id = builder.id;
        this.booking = builder.booking;
        this.student = builder.student;
        this.tutor = builder.tutor;
        this.rating = builder.rating;
        this.comment = builder.comment;
        this.createdAt = builder.createdAt;
    }

    // ----------------------------------------------------------------
    // Builder – classe interna statica
    // ----------------------------------------------------------------

    public static class Builder {

        private int id;
        private Booking booking;
        private Student student;
        private Tutor tutor;
        private int rating;
        private String comment;
        private LocalDateTime createdAt;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder booking(Booking booking) {
            this.booking = booking;
            return this;
        }

        public Builder student(Student student) {
            this.student = student;
            return this;
        }

        public Builder tutor(Tutor tutor) {
            this.tutor = tutor;
            return this;
        }

        public Builder rating(int rating) {
            this.rating = rating;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Review build() {
            return new Review(this);
        }
    }

    // ----------------------------------------------------------------
    // Logica di dominio
    // ----------------------------------------------------------------

    private void checkRating(int ratingToCheck) {
        if (ratingToCheck < 1 || ratingToCheck > 5) {
            throw new IllegalArgumentException(
                "Il voto deve essere compreso tra 1 e 5."
            );
        }
    }

    // ----------------------------------------------------------------
    // Getter e setter
    // ----------------------------------------------------------------

    public int getId() { return id; }
    public Booking getBooking() { return booking; }
    public Student getStudent() { return student; }
    public Tutor getTutor() { return tutor; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setRating(int rating) {
        checkRating(rating);
        this.rating = rating;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
