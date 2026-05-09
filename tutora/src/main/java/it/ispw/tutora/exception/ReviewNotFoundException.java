package it.ispw.tutora.exception;

public class ReviewNotFoundException extends TutoraException {
    public ReviewNotFoundException(int id){super("Review not found with review id:" + id);}
    public ReviewNotFoundException(String mes){super(mes);}
}
