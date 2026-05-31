package it.ispw.tutora.model;

import it.ispw.tutora.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entity che rappresenta la competenza di un tutor in una specifica
 * sottocategoria (es. Vittorio insegna Sassofono Blues in presenza).
 *
 * Corrisponde alla tabella tutor_expertise del DB.
 *
 * -----------------------------------------------------------------------
 * Composizione invece di ereditarietà
 * -----------------------------------------------------------------------
 * TutorExpertise NON estende Tutor — la relazione è "ha un" e non
 * "è un". Un'expertise appartiene a un tutor, non è un tipo di tutor.
 *
 * -----------------------------------------------------------------------
 * Logica di dominio
 * -----------------------------------------------------------------------
 * updateStatus() implementa una macchina a stati finiti:
 *   PENDING  → APPROVED | REJECTED
 *   APPROVED → REJECTED  (l'admin può revocare una competenza)
 *   REJECTED → (nessuna transizione possibile)
 *
 * checkPrice() è richiamato sia nel costruttore che nel setter
 * per centralizzare la validazione del prezzo in un unico punto.
 */
public class TutorExpertise {

    private final Tutor tutor;
    private final SubCategory subcategory;
    private BigDecimal hourlyPrice;
    private Status status;
    private final LocalDateTime createdAt;
    private final List<Tag> expertiseTags;

    public TutorExpertise(Tutor tutor,
                          SubCategory subcategory,
                          BigDecimal hourlyPrice,
                          Status status,
                          LocalDateTime createdAt) {
        checkPrice(hourlyPrice);
        this.tutor = tutor;
        this.subcategory = subcategory;
        this.hourlyPrice = hourlyPrice;
        this.status = status;
        this.createdAt = createdAt;
        this.expertiseTags = new ArrayList<>();
    }

    // ----------------------------------------------------------------
    // Gestione tag
    // ----------------------------------------------------------------

    public void addTag(Tag tag) {
        expertiseTags.add(tag);
    }

    /**
     * Restituisce una vista non modificabile dei tag.
     * Il chiamante non può alterare la lista interna.
     */
    public List<Tag> getExpertiseTags() {
        return Collections.unmodifiableList(expertiseTags);
    }

    // ----------------------------------------------------------------
    // Logica di dominio – validazione prezzo
    // ----------------------------------------------------------------

    private void checkPrice(BigDecimal priceToCheck) {
        if (priceToCheck == null || priceToCheck.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid price.");
        }
    }

    // ----------------------------------------------------------------
    // Logica di dominio – transizioni di stato
    // ----------------------------------------------------------------

    /**
     * Aggiorna lo status applicando la macchina a stati finiti.
     * Throws IllegalArgumentException if the transition is not valid.
     */
    public void updateStatus(Status newStatus) {
        if (!isTransitionValid(this.status, newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid transition: " + this.status + " → " + newStatus
            );
        }
        this.status = newStatus;
    }

    private boolean isTransitionValid(Status from, Status to) {
        if (from == null || to == null) return false;
        return switch (from) {
            case PENDING  -> to == Status.APPROVED || to == Status.REJECTED;
            case APPROVED -> to == Status.REJECTED;
            case REJECTED -> false;
        };
    }

    // ----------------------------------------------------------------
    // Getter e setter
    // ----------------------------------------------------------------

    public Tutor getTutor() { return tutor; }
    public SubCategory getSubcategory() { return subcategory; }
    public Status getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public BigDecimal getHourlyPrice() { return hourlyPrice; }

    public void setHourlyPrice(BigDecimal newHourlyPrice) {
        checkPrice(newHourlyPrice);
        this.hourlyPrice = newHourlyPrice;
    }
}
