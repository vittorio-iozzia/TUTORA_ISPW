package it.ispw.tutora.model;

import it.ispw.tutora.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDateTime;
public class Tutor extends User{
    private BigDecimal rating;
    private int ratingCount;
    public Tutor(String username, String email, String name, String surname, String passwordHash,
                 String description, boolean isActive, LocalDateTime createdAt,
                 BigDecimal rating, int ratingCount){
        super(username, email, name, surname, passwordHash, Role.TUTOR, description, isActive, createdAt);
        this.rating=rating;
        this.ratingCount=ratingCount;
    }
    public BigDecimal getRating(){
        return rating;
    }
    public int getRatingCount(){
        return ratingCount;
    }
    public void setRating(BigDecimal rating, int ratingCount){
        this.rating=rating;
        this.ratingCount=ratingCount;
    }

}
