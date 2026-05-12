package it.ispw.tutora.bean;

import it.ispw.tutora.enums.ItemType;

/**
 * Bean di trasporto per una singola risposta a un requisito (UC-2).
 *
 * Rappresenta un campo del form di candidatura:
 *   - Se itemType == TEXT: textContent contiene la risposta testuale.
 *   - Se itemType == DOCUMENT: documentPath contiene il percorso del file
 *     selezionato dall'utente; il boundary lo converte in byte[] prima
 *     di passarlo al Controller.
 *
 * Il campo requirementName funge da chiave naturale — identifica a quale
 * requisito della Category questo item risponde.
 */
public class ApplicationItemBean {

    private int applicationId;
    private String requirementName;
    private ItemType itemType;
    private String textContent;

    // Campi per item di tipo DOCUMENT — popolati dal boundary prima di passare al Controller
    private String documentPath;
    private String originalFilename;
    private String mimeType;
    private long sizeBytes;
    private byte[] content;

    public ApplicationItemBean() {}

    // ----------------------------------------------------------------
    // Getter e setter
    // ----------------------------------------------------------------

    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }

    public String getRequirementName() { return requirementName; }
    public void setRequirementName(String requirementName) { this.requirementName = requirementName; }

    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }

    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }

    public String getDocumentPath() { return documentPath; }
    public void setDocumentPath(String documentPath) { this.documentPath = documentPath; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }
}
