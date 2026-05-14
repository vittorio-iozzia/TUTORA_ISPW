package it.ispw.tutora.exception;

/**
 * Eccezione lanciata quando il gateway di pagamento non risponde
 * entro il timeout di 10 minuti.
 */
public class PaymentTimeoutException extends TutoraException {
    public PaymentTimeoutException(String message) {
        super(message);
    }
}