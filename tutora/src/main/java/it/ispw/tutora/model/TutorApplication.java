package it.ispw.tutora.model;

import it.ispw.tutora.enums.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entity principale di UC-2: rappresenta la candidatura di uno studente
 * per diventare tutor in una determinata categoria.
 */
public class TutorApplication {

    private final int id;
    private final String categoryName;
    private String subcategoryName;
    private final String studentUsername;
    private final LocalDateTime creationDate;
    private ApplicationStatus status;
    private String adminNotes;
    private LocalDateTime evaluatedAt;
    private final List<ApplicationItem> items;

    public TutorApplication(int id,
                            String categoryName,
                            String studentUsername,
                            LocalDateTime creationDate,
                            ApplicationStatus status) {
        this.id = id;
        this.categoryName = categoryName;
        this.studentUsername = studentUsername;
        this.creationDate = creationDate;
        this.status = status;
        this.items = new ArrayList<>();
    }

    // ----------------------------------------------------------------
    // Logica di dominio
    // ----------------------------------------------------------------

    /**
     * Cambia lo status applicando la macchina a stati finiti.
     * Solleva IllegalStateException se la transizione non è valida.
     *
     * Transizioni valide:
     *   DRAFT     → SUBMITTED
     *   SUBMITTED → ACCEPTED | REJECTED
     */
    public void updateStatus(ApplicationStatus newStatus) {
        if (!isTransitionValid(this.status, newStatus)) {
            throw new IllegalStateException(
                    "Invalid transaction: " + this.status + " → " + newStatus
            );
        }
        this.status = newStatus;
    }

    /**
     * Aggiunge un item alla candidatura.
     */
    public void addItem(ApplicationItem item) {
        items.add(item);
    }

    /**
     * Verifica che tutti i requisiti obbligatori siano compilati.
     * Chiamato dal Controller applicativo prima del submit.
     */
    public boolean isReadyToSubmit(List<Requirement> requirements) {
        for (Requirement req : requirements) {
            if (req.isOptional()) continue;
            boolean found = items.stream()
                    .anyMatch(item ->
                            item.getRequirementName().equals(req.getName())
                                    && item.isFilled());
            if (!found) return false;
        }
        return true;
    }

    // ----------------------------------------------------------------
    // Getter e setter
    // ----------------------------------------------------------------

    public int getId() { return id; }
    public String getCategoryName() { return categoryName; }
    public String getSubcategoryName() { return subcategoryName; }
    public void setSubcategoryName(String subcategoryName) { this.subcategoryName = subcategoryName; }
    public String getStudentUsername() { return studentUsername; }
    public LocalDateTime getCreationDate() { return creationDate; }
    public ApplicationStatus getStatus() { return status; }
    public String getAdminNotes() { return adminNotes; }
    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }

    public List<ApplicationItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    // ----------------------------------------------------------------
    // Utility privata
    // ----------------------------------------------------------------

    private boolean isTransitionValid(ApplicationStatus from, ApplicationStatus to) {
        return switch (from) {
            case DRAFT -> to == ApplicationStatus.SUBMITTED;
            case SUBMITTED -> to == ApplicationStatus.ACCEPTED
                    || to == ApplicationStatus.REJECTED;
            default -> false;
        };
    }

    @Override
    public String toString() {
        return "TutorApplication{id=" + id
                + ", category='" + categoryName + "'"
                + ", student='" + studentUsername + "'"
                + ", status=" + status + "}";
    }
}
