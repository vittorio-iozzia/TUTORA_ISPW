package it.ispw.tutora.model;

import it.ispw.tutora.enums.ItemType;

/**
 * Risposta testuale a un TextRequirement.
 * Concrete Product del Factory Method (ApplicationItemFactory).
 */
public class TextItem extends ApplicationItem {

    private String textContent;

    public TextItem(int id,
                    int applicationId,
                    String requirementName,
                    String textContent) {
        super(id, applicationId, requirementName);
        this.textContent = textContent;
    }

    @Override
    public ItemType getItemType() {
        return ItemType.TEXT;
    }

    @Override
    public boolean isFilled() {
        return textContent != null && !textContent.isBlank(); // Restituisce True se la stringa è vuota
    }

    public String getTextContent() { return textContent; }
    public void   setTextContent(String text) { this.textContent = text; }
}