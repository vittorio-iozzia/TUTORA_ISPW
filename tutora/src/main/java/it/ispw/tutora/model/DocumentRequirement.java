package it.ispw.tutora.model;

import it.ispw.tutora.enums.ItemType;

/**
 * Requisito che richiede il caricamento di un documento (PDF, immagine, ecc.).
 * Corrisponde a una riga della tabella document_requirement.
 *
 * Concrete Product dell'Abstract Factory (RequirementFactory).
 */
public class DocumentRequirement extends Requirement {

    // Dimensione massima accettata in byte (default: 10 MB)
    private static final long DEFAULT_MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private final long maxSizeBytes;

    public DocumentRequirement(String categoryName,
                               String name,
                               String label,
                               String description,
                               boolean required) {
        this(categoryName, name, label, description,
                required, DEFAULT_MAX_SIZE_BYTES);  // chiama il costruttore 2
    }

    public DocumentRequirement(String categoryName,
                               String name,
                               String label,
                               String description,
                               boolean required,
                               long maxSizeBytes) {
        super(categoryName, name, label, description, required);
        this.maxSizeBytes = maxSizeBytes;
    }

    /**
     * Verifica che il documento rispetti il limite di dimensione.
     *
     * @param sizeBytes dimensione del file in byte
     * @return true se la dimensione è nei limiti
     */
    public boolean isValidSize(long sizeBytes) {
        return sizeBytes > 0 && sizeBytes <= maxSizeBytes;
    }

    @Override
    public ItemType getItemType() {
        return ItemType.DOCUMENT;
    }

    public long getMaxSizeBytes() { return maxSizeBytes; }
}