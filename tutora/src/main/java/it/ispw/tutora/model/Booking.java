package it.ispw.tutora.model;

import it.ispw.tutora.enums.PaymentStatus;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity che rappresenta la prenotazione di una lezione da parte
 * di uno studente.
 *
 * Corrisponde alla tabella booking del DB.
 *
 * -----------------------------------------------------------------------
 * Pattern Builder (GoF – Creazionale)
 * -----------------------------------------------------------------------
 * Il costruttore ha 7 parametri — il Builder rende la costruzione
 * leggibile e a prova di errore.
 *
 * Utilizzo:
 *   Booking booking = new Booking.Builder()
 *       .id(1)
 *       .lesson(lesson)
 *       .student(student)
 *       .bookedAt(LocalDateTime.now())
 *       .pricePaid(new BigDecimal("30.00"))
 *       .paymentStatus(PaymentStatus.PENDING)
 *       .paymentRef("TXN-12345")   // opzionale
 *       .build();
 *
 * -----------------------------------------------------------------------
 * Pattern Observer – Push Model
 * -----------------------------------------------------------------------
 * Booking è il "Subject" (Observable).
 * Quando paymentStatus cambia, notifica tutti i listener registrati
 * passando DIRETTAMENTE il nuovo stato nell'evento (push), senza
 * costringere la View a fare un secondo fetch al Model.
 *
 * Utilizzo dal Controller grafico:
 *   booking.addPropertyChangeListener(PROP_PAYMENT_STATUS, event -> {
 *       PaymentStatus newStatus = (PaymentStatus) event.getNewValue();
 *       // aggiorna la UI con il nuovo stato del pagamento
 *   });
 *
 * -----------------------------------------------------------------------
 * Logica di dominio
 * -----------------------------------------------------------------------
 * updatePaymentStatus() implementa una macchina a stati finiti:
 *   PENDING  → PAID
 *   PAID     → REFUNDED
 *   REFUNDED → (nessuna transizione)
 *
 * pricePaid è uno snapshot fissato al momento della prenotazione —
 * non cambia mai, indipendente da future modifiche al prezzo del tutor.
 *
 * -----------------------------------------------------------------------
 * Nota sulla persistenza
 * -----------------------------------------------------------------------
 * La FK fk_booking_student usa ON DELETE RESTRICT nel DB:
 * i dati finanziari non vengono mai cancellati a cascata,
 * nemmeno alla disattivazione dell'utente (soft delete).
 */
public class Booking {

    // Nome della proprietà osservata — usato come chiave negli eventi push
    public static final String PROP_PAYMENT_STATUS = "paymentStatus";

    // Infrastruttura Observer di Java standard (java.beans)
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final int           id;
    private final Lesson        lesson;
    private final Student       student;
    private final LocalDateTime bookedAt;
    private final BigDecimal    pricePaid;
    private       PaymentStatus paymentStatus;
    private final String        paymentRef;

    // Costruttore privato: accessibile solo tramite Builder
    private Booking(Builder builder) {
        checkPayment(builder.pricePaid);
        this.id            = builder.id;
        this.lesson        = builder.lesson;
        this.student       = builder.student;
        this.bookedAt      = builder.bookedAt;
        this.pricePaid     = builder.pricePaid;
        this.paymentStatus = builder.paymentStatus;
        this.paymentRef    = builder.paymentRef;
    }

    // ----------------------------------------------------------------
    // Builder – classe interna statica
    // ----------------------------------------------------------------

    public static class Builder {

        private int           id;
        private Lesson        lesson;
        private Student       student;
        private LocalDateTime bookedAt;
        private BigDecimal    pricePaid;
        private PaymentStatus paymentStatus;
        private String        paymentRef;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder lesson(Lesson lesson) {
            this.lesson = lesson;
            return this;
        }

        public Builder student(Student student) {
            this.student = student;
            return this;
        }

        public Builder bookedAt(LocalDateTime bookedAt) {
            this.bookedAt = bookedAt;
            return this;
        }

        public Builder pricePaid(BigDecimal pricePaid) {
            this.pricePaid = pricePaid;
            return this;
        }

        public Builder paymentStatus(PaymentStatus paymentStatus) {
            this.paymentStatus = paymentStatus;
            return this;
        }

        public Builder paymentRef(String paymentRef) {
            this.paymentRef = paymentRef;
            return this;
        }

        public Booking build() {
            return new Booking(this);
        }
    }

    // ----------------------------------------------------------------
    // Observer – registrazione e rimozione listener
    // ----------------------------------------------------------------

    /**
     * Il Controller grafico si registra qui per ricevere
     * aggiornamenti push quando paymentStatus cambia.
     */
    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName,
                                             PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    // ----------------------------------------------------------------
    // Logica di dominio – validazione
    // ----------------------------------------------------------------

    private void checkPayment(BigDecimal pricePaidToCheck) {
        if (pricePaidToCheck == null
                || pricePaidToCheck.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid price.");
        }
    }

    // ----------------------------------------------------------------
    // Logica di dominio – transizioni di stato
    // ----------------------------------------------------------------

    /**
     * Aggiorna lo stato del pagamento applicando la macchina a stati finiti
     * e notifica i listener con il push model.
     * Throws IllegalArgumentException if the transition is not valid.
     */
    public void updatePaymentStatus(PaymentStatus newPaymentStatus) {
        if (!isTransitionValid(this.paymentStatus, newPaymentStatus)) {
            throw new IllegalArgumentException(
                    "Invalid transition: " + this.paymentStatus + " → " + newPaymentStatus
            );
        }
        PaymentStatus oldStatus = this.paymentStatus;
        this.paymentStatus = newPaymentStatus;
        // PUSH: il listener riceve direttamente il nuovo stato nell'evento
        pcs.firePropertyChange(PROP_PAYMENT_STATUS, oldStatus, newPaymentStatus);
    }

    private boolean isTransitionValid(PaymentStatus from, PaymentStatus to) {
        if (from == null || to == null) return false;
        return switch (from) {
            case PENDING  -> to == PaymentStatus.PAID;
            case PAID     -> to == PaymentStatus.REFUNDED;
            case REFUNDED -> false;
        };
    }

    // ----------------------------------------------------------------
    // Getter
    // ----------------------------------------------------------------

    public int           getId()            { return id; }
    public Lesson        getLesson()        { return lesson; }
    public Student       getStudent()       { return student; }
    public LocalDateTime getBookedAt()      { return bookedAt; }
    public BigDecimal    getPricePaid()     { return pricePaid; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public String        getPaymentRef()    { return paymentRef; }
}
