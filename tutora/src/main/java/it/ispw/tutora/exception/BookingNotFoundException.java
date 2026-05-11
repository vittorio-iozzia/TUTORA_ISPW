package it.ispw.tutora.exception;

public class BookingNotFoundException extends TutoraException {
    public BookingNotFoundException(int id){super("Booking not found with booking id:" + id);}
}
