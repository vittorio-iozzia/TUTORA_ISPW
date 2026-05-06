package it.ispw.tutora.model;
import it.ispw.tutora.enums.Status;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TutorExpertise {
    private final Tutor tutor;
    private final SubCategory subcategory;
    private BigDecimal hourlyprice;
    private Status status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final List<Tag> expertisetag;
    public TutorExpertise(Tutor tutor, SubCategory subcategory, BigDecimal hourlyprice,
                          Status status, LocalDateTime createdAt, LocalDateTime updatedAt){
        checkprice(hourlyprice);
        this.tutor=tutor;
        this.subcategory=subcategory;
        this.hourlyprice=hourlyprice;
        this.status=status;
        this.createdAt=createdAt;
        this.updatedAt=updatedAt;
        this.expertisetag=new ArrayList<>();
    }
    public void addTag(Tag tag){
       expertisetag.add(tag);
    }

    private void checkprice(BigDecimal pricetocheck){
        if (pricetocheck==null || pricetocheck.compareTo(BigDecimal.ZERO)<=0){
            throw new IllegalArgumentException("Incorrect price.");
        }
    }
    public Tutor getTutor() {
        return tutor;
    }

    public SubCategory getSubcategory() {
        return subcategory;
    }

    public BigDecimal getHourlyprice() {
        return hourlyprice;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setHourlyprice(BigDecimal newhourlyprice) {
        checkprice(newhourlyprice);
        this.hourlyprice = newhourlyprice;

    }
    public void setStatus(Status status) {
        this.status = status;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    public List<Tag> getExpertisetag() {
        return expertisetag;
    }
}
