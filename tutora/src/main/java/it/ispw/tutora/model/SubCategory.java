package it.ispw.tutora.model;

/**
 * Entity che rappresenta una sottocategoria di tutoraggio
 * (es. Sassofono, Chitarra Jazz appartenenti a Music).
 *
 * Corrisponde alla tabella subcategory del DB.
 *
 * -----------------------------------------------------------------------
 * Composizione invece di ereditarietà
 * -----------------------------------------------------------------------
 * SubCategory NON estende Category — la relazione è "ha una" e non
 * "è una". Una sottocategoria appartiene a una categoria padre,
 * non è un tipo di categoria.
 *
 * Classe immutabile: tutti i campi sono final, nessun setter.
 * Una sottocategoria non cambia mai dopo la creazione.
 */
public class SubCategory {

    private final String   name;
    private final Category parentCategory;
    private final String   description;

    public SubCategory(String name,
                       Category parentCategory,
                       String description) {
        this.name           = name;
        this.parentCategory = parentCategory;
        this.description    = description;
    }

    public String   getName()           { return name; }
    public Category getParentCategory() { return parentCategory; }
    public String   getDescription()    { return description; }

    @Override
    public String toString() {
        String categoryName = parentCategory != null
                ? parentCategory.getName()
                : "N/A";
        return name + " (" + categoryName + ")";
    }
}
