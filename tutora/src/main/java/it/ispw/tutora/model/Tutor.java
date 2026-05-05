package it.ispw.tutora.model;

import it.ispw.tutora.enums.Role;

import java.time.LocalDateTime;
public class Tutor extends User{
    private double rating;
    private int ratingCount;
    public Tutor(String username, String email, String name, String surname, String passwordHash,
                 Role role, String description, boolean isActive, LocalDateTime createdAt, LocalDateTime updatedAt,
                 double rating, int ratingCount){
        super(username, email, name, surname, passwordHash, role, description, isActive, createdAt, updatedAt);
        this.rating=rating;
        this.ratingCount=ratingCount;
    }
    public double getRating(){
        return rating;
    }
    public int getRatingCount(){
        return ratingCount;
    }
    public void setRating(double rating){
        this.rating=rating;
    }
    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }
}
