package it.ispw.tutora.model;

import it.ispw.tutora.enums.LessonStatus;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity che rappresenta uno slot di disponibilità offerto da un tutor
 * per una specifica competenza.
 *
 * Corrisponde alla tabella lesson del DB.
 *
 * -----------------------------------------------------------------------
 * Pattern Builder (GoF – Creazionale)
 * -----------------------------------------------------------------------
 * Il costruttore ha 8 parametri — il Builder rende la costruzione
 * leggibile e a prova di errore.
 *
 * Utilizzo:
 *   Lesson lesson = new Lesson.Builder()
 *       .id(1)
 *       .expertise(expertise)
 *       .startTime(LocalDateTime.of(2026, 6, 1, 10, 0))
 *       .endTime(LocalDateTime.of(2026, 6, 1, 11, 0))
 *       .remote(true)
 *       .listedPrice(new BigDecimal("30.00"))
 *       .lessonStatus(LessonStatus.AVAILABLE)
 *       .createdAt(LocalDateTime.now())
 *       .build();
 *
 * -----------------------------------------------------------------------
 * Pattern Observer – Push Model
 * -----------------------------------------------------------------------
 * Lesson è il "Subject" (Observable).
 * Quando lessonStatus cambia, notifica tutti i listener registrati
 * passando DIRETTAMENTE il nuovo stato nell'evento (push), senza
 * costringere la View a fare un secondo fetch al Model.
 *
 * Utilizzo dal Controller grafico:
 *   lesson.addPropertyChangeListener(PROP_LESSON_STATUS, event -> {
 *       LessonStatus newStatus = (LessonStatus) event.getNewValue();
 *       // aggiorna la UI con il nuovo stato della lezione
 *   });
 *
 * -----------------------------------------------------------------------
 * Logica di dominio
 * -----------------------------------------------------------------------
 * updateLessonStatus() implementa una macchina a stati finiti:
 *   AVAILABLE → BOOKED | CANCELLED
 *   BOOKED    → COMPLETED | CANCELLED
 *   COMPLETED → (nessuna transizione)
 *   CANCELLED → (nessuna transizione)
 *
 * -----------------------------------------------------------------------
 * Nota sul Lesson Overlap
 * -----------------------------------------------------------------------
 * Il DB non impedisce sovrapposizioni temporali per lo stesso tutor.
 * La guardia anti-overlap va implementata nel DAO con questa query
 * prima di ogni INSERT:
 *   SELECT COUNT(*) FROM lesson
 *   WHERE tutor_username = ?
 *     AND status NOT IN ('Cancelled')
 *     AND start_time < ? AND end_time > ?
 * Se COUNT > 0 l'INSERT deve essere rifiutato con un'eccezione.
 */
public class Lesson {

    public static final String PROP_LESSON_STATUS = "lessonStatus";

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final int id;
    private final TutorExpertise expertise;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isRemote;
    private BigDecimal listedPrice;
    private LessonStatus lessonStatus;
    private final LocalDateTime  createdAt;

    private Lesson(Builder builder) {
        checkPrice(builder.listedPrice);
        checkTime(builder.startTime, builder.endTime);
        this.id = builder.id;
        this.expertise = builder.expertise;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.isRemote = builder.isRemote;
        this.listedPrice = builder.listedPrice;
        this.lessonStatus = builder.lessonStatus;
        this.createdAt = builder.createdAt;
    }

    // ----------------------------------------------------------------
    // Builder – classe interna statica
    // ----------------------------------------------------------------

    public static class Builder {

        private int id;
        private TutorExpertise expertise;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean isRemote;
        private BigDecimal listedPrice;
        private LessonStatus lessonStatus;
        private LocalDateTime createdAt;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder expertise(TutorExpertise expertise) {
            this.expertise = expertise;
            return this;
        }

        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder remote(boolean isRemote) {
            this.isRemote = isRemote;
            return this;
        }

        public Builder listedPrice(BigDecimal listedPrice) {
            this.listedPrice = listedPrice;
            return this;
        }

        public Builder lessonStatus(LessonStatus lessonStatus) {
            this.lessonStatus = lessonStatus;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Lesson build() {
            return new Lesson(this);
        }
    }

    // ----------------------------------------------------------------
    // Observer – registrazione e rimozione listener
    // ----------------------------------------------------------------

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

    private void checkPrice(BigDecimal priceToCheck) {
        if (priceToCheck == null || priceToCheck.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid price.");
        }
    }

    private void checkTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Times cannot be null.");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException(
                    "Start time must be strictly before end time."
            );
        }
    }

    // ----------------------------------------------------------------
    // Logica di dominio – transizioni di stato
    // ----------------------------------------------------------------

    /**
     * Aggiorna lo status applicando la macchina a stati finiti
     * e notifica i listener con il push model.
     * Throws IllegalArgumentException if the transition is not valid.
     */
    public void updateLessonStatus(LessonStatus newLessonStatus) {
        if (!isTransitionValid(this.lessonStatus, newLessonStatus)) {
            throw new IllegalArgumentException(
                    "Invalid transition: " + this.lessonStatus + " → " + newLessonStatus
            );
        }
        LessonStatus oldStatus = this.lessonStatus;
        this.lessonStatus = newLessonStatus;
        pcs.firePropertyChange(PROP_LESSON_STATUS, oldStatus, newLessonStatus);
    }

    private boolean isTransitionValid(LessonStatus from, LessonStatus to) {
        if (from == null || to == null) return false;
        return switch (from) {
            case AVAILABLE -> to == LessonStatus.BOOKED
                    || to == LessonStatus.CANCELLED;
            case BOOKED -> to == LessonStatus.COMPLETED
                    || to == LessonStatus.CANCELLED;
            case COMPLETED,
                 CANCELLED -> false;
        };
    }

    // ----------------------------------------------------------------
    // Getter e setter
    // ----------------------------------------------------------------

    public int getId() { return id; }
    public TutorExpertise getExpertise() { return expertise; }
    public LocalDateTime  getStartTime() { return startTime; }
    public LocalDateTime  getEndTime() { return endTime; }
    public boolean isRemote() { return isRemote; }
    public BigDecimal getListedPrice() { return listedPrice; }
    public LessonStatus getLessonStatus() { return lessonStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setLessonTime(LocalDateTime startTime, LocalDateTime endTime) {
        checkTime(startTime, endTime);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void setRemote(boolean remote) { this.isRemote = remote; }

    public void setListedPrice(BigDecimal listedPrice) {
        checkPrice(listedPrice);
        this.listedPrice = listedPrice;
    }
}
