package it.ispw.tutora.bean;

/**
 * Bean di trasporto per una categoria (UC-2).
 *
 * Usato dal Controller per passare alla View la lista delle categorie
 * disponibili nella schermata di selezione categoria.
 * I requisiti della categoria vengono trasportati separatamente
 * tramite RequirementBean, popolati solo quando l'utente
 * seleziona una categoria specifica.
 */
public class CategoryBean {

    private String name;
    private String description;

    public CategoryBean() {}

    // ----------------------------------------------------------------
    // Getter e setter
    // ----------------------------------------------------------------

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
