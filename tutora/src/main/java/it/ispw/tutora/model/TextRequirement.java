package it.ispw.tutora.model;

import it.ispw.tutora.enums.ItemType;

/**
 * Requisito che richiede una risposta testuale da parte dello studente.
 * Corrisponde a una riga della tabella text_requirement.
 *
 * Concrete Product dell'Abstract Factory (RequirementFactory).
 */
public class TextRequirement extends Requirement {

    private final int minChar;
    private final int maxChar;

    public TextRequirement(String categoryName,
                           String name,
                           String label,
                           String description,
                           boolean required,
                           int minChar,
                           int maxChar) {
        super(categoryName, name, label, description, required);
        this.minChar = minChar;
        this.maxChar = maxChar;
    }

    /**
     * Valida il testo inserito dallo studente rispetto ai vincoli del requisito.
     *
     * @param text il testo da validare
     * @return true se il testo rispetta i limiti di caratteri
     */
    public boolean isValidText(String text) {
        if (text == null) return !isRequired();  // La validità del testo dipende da se il requisito è obbligatorio o meno
        int len = text.trim().length();
        return len >= minChar && len <= maxChar;
    }

    @Override
    public ItemType getItemType() {
        return ItemType.TEXT;
    }

    public int getMinChar() { return minChar; }
    public int getMaxChar() { return maxChar; }
}