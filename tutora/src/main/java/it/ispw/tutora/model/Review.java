package it.ispw.tutora.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Review {
    private final int id;
    private final Booking booking;
    private final Student student;
    private final Tutor tutor;
    private int rating;
    private String comment;
    private final LocalDateTime createdAt;

    public Review(int id, Booking booking, Student student, Tutor tutor, int rating,
                  String comment, LocalDateTime createdAt) {
        checkRating(rating);
        this.id = id;
        this.booking = booking;
        this.student = student;
        this.tutor = tutor;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }
    public Review(int id, Booking booking, Student student, Tutor tutor, int rating,
                  LocalDateTime createdAt){
        this(id,booking,student,tutor,rating,null,createdAt);
    }

    public int getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public Student getStudent() {
        return student;
    }

    public Tutor getTutor() {
        return tutor;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setRating(int rating) {
        checkRating(rating);
        this.rating = rating;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
    private void checkRating(int newrating){
        if (newrating>5 || newrating<1){
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
    }
}
