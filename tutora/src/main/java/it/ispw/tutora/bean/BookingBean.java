package it.ispw.tutora.bean;

/**
 * Bean di trasporto per la prenotazione di una lezione da parte dello student (UC-Booking).
 *
 * Trasporta dal boundary al Controller solo i dati che lo student
 * deve fornire esplicitamente:
 *   - lessonId identifica la lezione da prenotare.
 *   - paymentRef è il riferimento alla transazione di pagamento esterno
 *     (opzionale: può essere null se il pagamento avviene in un secondo momento).
 *
 * I campi restanti del model Booking (student, bookedAt, pricePaid, paymentStatus)
 * vengono derivati dal Controller tramite sessione, orologio di sistema
 * e il prezzo già registrato sulla lezione.
 */
public class BookingBean {

    private int id;
    private int lessonId;
    private String paymentRef;
    private String errorMessage;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLessonId() { return lessonId; }
    public void setLessonId(int lessonId) { this.lessonId = lessonId; }

    public String getPaymentRef() { return paymentRef; }
    public void setPaymentRef(String paymentRef) { this.paymentRef = paymentRef; }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
