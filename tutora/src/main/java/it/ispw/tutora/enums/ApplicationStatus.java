package it.ispw.tutora.enums;

/**
 * Stati del ciclo di vita di una TutorApplication.
 * Specchia il campo ENUM('Draft','Submitted','Accepted','Rejected')
 * della tabella tutor_application.
 *
 * Transizioni valide:
 *   DRAFT     → SUBMITTED
 *   SUBMITTED → ACCEPTED | REJECTED
 */
public enum ApplicationStatus {
    DRAFT, // Application creata, ma ancora non inviata
    SUBMITTED, // Application inviata. Risulta "congelata" e non può più essere modificata.
    ACCEPTED, // Valutazione dell'admin
    REJECTED; // Valutazione dell'admin

    /**
     * Verifica se lo stato è terminale (nessuna ulteriore transizione possibile).
     */
    public boolean isTerminal() {
        return this == ACCEPTED || this == REJECTED;
    }

    /**
     * Verifica se l'application è ancora modificabile dallo studente.
     */
    public boolean isEditable() {
        return this == DRAFT;
    }
}