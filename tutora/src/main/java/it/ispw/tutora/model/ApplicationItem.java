package it.ispw.tutora.model;

import it.ispw.tutora.enums.ItemType;

/**
 * Classe astratta che rappresenta la risposta di uno studente
 * a un singolo Requirement all'interno di una TutorApplication.
 *
 * Le sottoclassi concrete sono:
 *   - TextItem      (risposta testuale)
 *   - DocumentItem  (documento allegato)
 *
 * Factory Method: ApplicationItemFactory decide quale
 * sottoclasse istanziare in base all'ItemType del Requirement.
 */

public abstract class ApplicationItem {

    private final int id;
    private final int applicationId;
    private final String requirementName;

    protected ApplicationItem(int id,
                              int applicationId,
                              String requirementName) {
        this.id = id;
        this.applicationId = applicationId;
        this.requirementName = requirementName;
    }

    /**
     * Restituisce il tipo di questo item.
     * Usato dal DAO per costruire la query corretta.
     */
    public abstract ItemType getItemType();

    /**
     * Verifica se il contenuto dell'item è stato compilato.
     * Usato da TutorApplication.isReadyToSubmit().
     */
    public abstract boolean isFilled();

    public int getId() { return id; }
    public int getApplicationId() { return applicationId; }
    public String getRequirementName() { return requirementName; }
}