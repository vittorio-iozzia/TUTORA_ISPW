package it.ispw.tutora.model;

import it.ispw.tutora.enums.ItemType;

/**
 * Classe astratta che rappresenta un requisito di categoria
 * per l'application "Apply to Become a Tutor" (UC-2).
 *
 * Le sottoclassi concrete sono:
 *   - TextRequirement      (risposta testuale libera)
 *   - DocumentRequirement  (upload di un file)
 *
 * Questo è il "Product" dell'Abstract Factory pattern:
 * RequirementFactory produce oggetti di tipo Requirement.
 */
public abstract class Requirement {

    private final String  categoryName;
    private final String  name;
    private final String  label;
    private final String  description;
    private final boolean required;

    protected Requirement(String categoryName,
                          String name,
                          String label,
                          String description,
                          boolean required) {
        this.categoryName = categoryName;
        this.name         = name;
        this.label        = label;
        this.description  = description;
        this.required     = required;
    }

    /**
     * Restituisce il tipo di item che questo requisito richiede.
     * Usato da ApplicationItemFactory per creare l'ApplicationItem corretto.
     */
    public abstract ItemType getItemType();

    public String  getCategoryName() { return categoryName; }
    public String  getName()         { return name; }
    public String  getLabel()        { return label; }
    public String  getDescription()  { return description; }
    public boolean isRequired()      { return required; }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{name='" + name + "', required=" + required + "}";
    }
}