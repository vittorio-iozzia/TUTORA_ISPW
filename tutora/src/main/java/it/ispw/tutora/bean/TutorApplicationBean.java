package it.ispw.tutora.bean;

import it.ispw.tutora.enums.ApplicationStatus;

import java.time.LocalDateTime;

/**
 * Bean di trasporto per una candidatura tutor (UC-2).
 *
 * Usato in due direzioni:
 *   - View → Controller: il boundary popola categoryName e studentUsername
 *     quando lo studente avvia una nuova candidatura.
 *   - Controller → View: il Controller popola tutti i campi per mostrare
 *     lo stato di una candidatura esistente (lista candidature, dettaglio).
 */
public class TutorApplicationBean {

    private int applicationId;
    private String categoryName;
    private String studentUsername;
    private ApplicationStatus status;
    private String adminNotes;
    private LocalDateTime creationDate;
    private LocalDateTime evaluatedAt;

    public TutorApplicationBean() {}

    // ----------------------------------------------------------------
    // Getter e setter
    // ----------------------------------------------------------------

    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getStudentUsername() { return studentUsername; }
    public void setStudentUsername(String studentUsername) { this.studentUsername = studentUsername; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; }

    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
}
