package it.ispw.tutora.bean;

import it.ispw.tutora.enums.ApplicationStatus;

/**
 * Bean di trasporto per la revisione di una candidatura da parte dell'admin (UC-2).
 *
 * Trasporta la decisione dell'admin dal boundary al Controller:
 *   - applicationId identifica la candidatura da aggiornare.
 *   - status è ACCEPTED o REJECTED.
 *   - adminNotes è il feedback opzionale mostrato allo studente.
 *
 * Separato da TutorApplicationBean per SRP: TutorApplicationBean
 * trasporta i dati di lettura di un'application, ApplicationReviewBean
 * trasporta esclusivamente la decisione di scrittura dell'admin.
 */
public class ApplicationReviewBean {

    private int applicationId;
    private ApplicationStatus status;
    private String adminNotes;

    // ----------------------------------------------------------------
    // Getter e setter
    // ----------------------------------------------------------------

    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}
