package it.ispw.tutora.model;

import it.ispw.tutora.enums.ItemType;

/**
 * Risposta documentale a un DocumentRequirement.
 * Concrete Product del Factory Method (ApplicationItemFactory).
 */
public class DocumentItem extends ApplicationItem {

    private Document document;

    public DocumentItem(int id,
                        int applicationId,
                        String requirementName,
                        Document document) {
        super(id, applicationId, requirementName);
        this.document = document;
    }

    @Override
    public ItemType getItemType() {
        return ItemType.DOCUMENT;
    }

    @Override
    public boolean isFilled() {
        return document != null;
    }

    public Document getDocument() { return document; }
    public void setDocument(Document doc) { this.document = doc; }

}
