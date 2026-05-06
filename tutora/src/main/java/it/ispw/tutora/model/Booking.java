package it.ispw.tutora.model;


import it.ispw.tutora.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Booking {
    private final int id;
    private final Lesson lesson;
    private final Student student;
    private final LocalDateTime bookedAt;
    private final BigDecimal pricePaid;
    private PaymentStatus paymentStatus;
    private final String paymentRef;

    public Booking(int id, Lesson lesson, Student student, LocalDateTime bookedAt, BigDecimal pricePaid,
                   PaymentStatus paymentStatus, String paymentRef) {
        checkPayment(pricePaid);
        this.id=id;
        this.lesson = lesson;
        this.student = student;
        this.bookedAt = bookedAt;
        this.pricePaid = pricePaid;
        this.paymentStatus = paymentStatus;
        this.paymentRef = paymentRef;

    }
    private void checkPayment(BigDecimal newpricePaid){
        if (newpricePaid == null || newpricePaid.compareTo(BigDecimal.ZERO)<=0){
            throw new IllegalArgumentException("Incorrect price.");
        }
    }

    public int getId() {
        return id;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public Student getStudent() {
        return student;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public BigDecimal getPricePaid() {
        return pricePaid;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public String getPaymentRef() {
        return paymentRef;
    }

    public void updatePaymentStatus(PaymentStatus newPaymentStatus){
        if(!isTransitionValid(this.paymentStatus,newPaymentStatus)){
            throw new IllegalArgumentException(
                    "Invalid Transaction: " + this.paymentStatus + " → " + newPaymentStatus);
        }
        this.paymentStatus=newPaymentStatus;
    }
    private boolean isTransitionValid(PaymentStatus from, PaymentStatus to){
        if (from==null || to==null){
            return false;
        }
        return switch (from){
            case PENDING -> to==PaymentStatus.PAID;
            case PAID -> to==PaymentStatus.REFUNDED;
            case REFUNDED -> false;
        };
    }
}
