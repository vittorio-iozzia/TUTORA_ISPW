package it.ispw.tutora.model;

import it.ispw.tutora.enums.ApplicationStatus;

import java.beans.PropertyChangeListener; // Interfaccia che deve implementare chi vuole ricevere notifiche
import java.beans.PropertyChangeSupport; // Infrastruttura del Subject (Observable)
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Entity principale di UC-2: rappresenta la candidatura di uno studente
 * per diventare tutor in una determinata categoria.
 *
 * -----------------------------------------------------------------------
 * Pattern Observer – Push Model
 * -----------------------------------------------------------------------
 * TutorApplication è il "Subject" (Observable).
 * Quando lo status cambia, notifica tutti i listener registrati
 * passando DIRETTAMENTE il nuovo stato nell'evento (push), senza
 * costringere la View a fare un secondo fetch al Model.
 *
 * Utilizzo dal Controller grafico:
 *   application.addPropertyChangeListener(PROP_STATUS, event -> {
 *       ApplicationStatus newStatus = (ApplicationStatus) event.getNewValue();
 *       // aggiorna la UI direttamente con newStatus
 *   });
 */
public class TutorApplication {

    // Nomi delle proprietà osservabili — usati come chiave negli eventi push
    public static final String PROP_STATUS = "status";
    public static final String PROP_ITEMS  = "items";

    // Infrastruttura Observer di Java standard (java.beans)
    // nessuna dipendenza esterna necessaria
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final int id;
    private final String categoryName;
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
    // Observer – registrazione e rimozione listener
    // ----------------------------------------------------------------

    /**
     * La View si registra qui per ricevere
     * aggiornamenti push quando lo status cambia.
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
    // Logica di dominio
    // ----------------------------------------------------------------

    /**
     * Cambia lo status e notifica i listener con il push model.
     * Solleva IllegalStateException se la transizione non è valida.
     *
     * Transizioni valide:
     *   DRAFT     → SUBMITTED
     *   SUBMITTED → ACCEPTED | REJECTED
     */
    public void updateStatus(ApplicationStatus newStatus) {
        if (!isTransitionValid(this.status, newStatus)) {
            throw new IllegalStateException(
                    "Transizione non valida: " + this.status + " → " + newStatus
            );
        }
        ApplicationStatus oldStatus = this.status;
        this.status = newStatus;
        // PUSH: il listener riceve direttamente il nuovo stato nell'evento
        pcs.firePropertyChange(PROP_STATUS, oldStatus, newStatus);
    }

    /**
     * Aggiunge un item e notifica i listener.
     */
    public void addItem(ApplicationItem item) {
        List<ApplicationItem> oldItems = List.copyOf(items);
        items.add(item);
        pcs.firePropertyChange(PROP_ITEMS, oldItems,
                Collections.unmodifiableList(items));
    }

    /**
     * Verifica che tutti i requisiti obbligatori siano compilati.
     * Chiamato dal Controller applicativo prima del submit.
     */
    public boolean isReadyToSubmit(List<Requirement> requirements) {
        for (Requirement req : requirements) {
            if (!req.isRequired()) continue;
            boolean found = items.stream()
                    .anyMatch(item ->
                            item.getRequirementName().equals(req.getName())  // Il nome del documento deve coincidere con il nome el requisito
                                    && item.isFilled());  // Il documento deve essere compilato
            if (!found) return false;
        }
        return true;
    }

    // ----------------------------------------------------------------
    // Getter e setter
    // ----------------------------------------------------------------

    public int getId() { return id; }
    public String getCategoryName() { return categoryName; }
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

    private boolean isTransitionValid(ApplicationStatus from,
                                      ApplicationStatus to) {
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
