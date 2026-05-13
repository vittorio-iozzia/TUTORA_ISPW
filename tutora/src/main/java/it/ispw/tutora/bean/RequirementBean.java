package it.ispw.tutora.bean;

import it.ispw.tutora.enums.ItemType;

/**
 * Bean di trasporto per un singolo requisito di categoria (UC-2).
 *
 * Usato dal Controller per passare alla View i requisiti della categoria
 * selezionata, così il boundary sa quali campi renderizzare nel form.
 *
 * itemType discrimina il tipo di campo:
 *   - TEXT → campo di testo; minLength e maxLength sono valorizzati.
 *   - DOCUMENT → file upload; minLength e maxLength sono 0.
 *
 * Il campo required guida la View nel segnalare i campi obbligatori
 * e il Controller nel validare la completezza prima del submit.
 */
public class RequirementBean {

    private String categoryName;
    private String name;
    private String label;
    private String description;
    private boolean required;
    private ItemType itemType;
    private int minChar;
    private int maxChar;

    // ----------------------------------------------------------------
    // Getter e setter
    // ----------------------------------------------------------------

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }

    public int getMinChar() { return minChar; }
    public void setMinChar(int minChar) { this.minChar = minChar; }

    public int getMaxLength() { return maxChar; }
    public void setMaxChar(int maxChar) { this.maxChar = maxChar; }
}
