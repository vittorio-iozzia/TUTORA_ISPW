package it.ispw.tutora.model;

import it.ispw.tutora.enums.LessonStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Lesson {
    private final int id;
    private final TutorExpertise expertise;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isRemote;
    private BigDecimal listedPrice;
    private LessonStatus lessonStatus;
    private final LocalDateTime createdAt;

    public Lesson(int id, TutorExpertise expertise, LocalDateTime startTime, LocalDateTime endTime,
                  boolean isRemote, BigDecimal listedPrice, LessonStatus lessonStatus, LocalDateTime createdAt){
        checkPrice(listedPrice);
        checkTime(startTime,endTime);
        this.id=id;
        this.expertise=expertise;
        this.startTime =startTime;
        this.endTime =endTime;
        this.isRemote = isRemote;
        this.listedPrice = listedPrice;
        this.lessonStatus = lessonStatus;
        this.createdAt = createdAt;
    }
    private void checkPrice(BigDecimal priceToCheck){
        if (priceToCheck==null || priceToCheck.compareTo(BigDecimal.ZERO)<=0){
            throw new IllegalArgumentException("Incorrect price.");
        }
    }

    public int getId() {
        return id;
    }

    public TutorExpertise getExpertise(){
        return expertise;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public boolean isRemote() {
        return isRemote;
    }
    public BigDecimal getListedPrice() {
        return listedPrice;
    }
    public LessonStatus getLessonStatus() {
        return lessonStatus;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setLessonTime(LocalDateTime startTime, LocalDateTime endTime) {
        checkTime(startTime,endTime);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void setRemote(boolean remote) {
        this.isRemote = remote;
    }

    public void setListedPrice(BigDecimal listedPrice) {
        checkPrice(listedPrice);
        this.listedPrice = listedPrice;
    }
    private void checkTime(LocalDateTime start, LocalDateTime end){
        if (start == null || end == null) {
            throw new IllegalArgumentException("Time cannot be null.");
        }
        if (!end.isAfter(start)){
            throw new IllegalArgumentException("Start time must be strictly before end time.");
        }
    }
    public void updateLessonStatus(LessonStatus newLessonStatus){
        if (!isTransitionValid(this.lessonStatus,newLessonStatus)){
            throw new IllegalArgumentException(
                    "Invalid Transaction: " + this.lessonStatus + " → " + newLessonStatus);
        }
        this.lessonStatus =newLessonStatus;
    }
    private boolean isTransitionValid(LessonStatus from, LessonStatus to){
        if (from == null || to == null) {
            return false;
        }
        return switch (from){
            case AVAILABLE -> to==LessonStatus.BOOKED || to==LessonStatus.CANCELLED;
            case BOOKED -> to==LessonStatus.COMPLETED || to==LessonStatus.CANCELLED;
            case CANCELLED, COMPLETED -> false;
        };
    }
}
