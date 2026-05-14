package it.ispw.tutora.bean;

import it.ispw.tutora.enums.Status;

/**
 * Bean di trasporto per la revisione di una expertise da parte dell'admin.
 *
 * Trasporta la decisione dell'admin dal boundary al Controller:
 *   - tutorUsername e subcategoryName identificano univocamente l'expertise
 *     da revisionare (chiave composita, come nel DB e nel DAO).
 *   - status è APPROVED o REJECTED.
 *   - adminNotes è il feedback opzionale mostrato al tutor.
 *
 * Separato da TutorExpertiseBean per SRP: TutorExpertiseBean trasporta
 * i dati di creazione/modifica del tutor, TutorExpertiseReviewBean
 * trasporta esclusivamente la decisione di revisione dell'admin.
 *
 * Segue lo stesso pattern di ApplicationReviewBean (UC-2).
 */
public class TutorExpertiseReviewBean {

    private String tutorUsername;
    private String subcategoryName;
    private Status status;
    private String adminNotes;


    public String getTutorUsername() { return tutorUsername; }
    public void setTutorUsername(String tutorUsername) { this.tutorUsername = tutorUsername; }

    public String getSubcategoryName() { return subcategoryName; }
    public void setSubcategoryName(String subcategoryName) { this.subcategoryName = subcategoryName; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}
