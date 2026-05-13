package it.ispw.tutora.exception;

/**
 * Eccezione lanciata quando il gateway di pagamento rifiuta la transazione
 * o si verifica un errore generico durante il pagamento.
 */
public class PaymentException extends TutoraException {
    public PaymentException(String message) {
        super(message);
    }
    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}