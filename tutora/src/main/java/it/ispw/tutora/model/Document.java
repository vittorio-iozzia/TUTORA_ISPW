package it.ispw.tutora.model;

import java.time.LocalDateTime;

/**
 * Entity che rappresenta un file caricato dallo studente
 * durante la compilazione di un'application (tabella document).
 *
 * stored_filename è il nome UUID generato in Java prima dell'INSERT
 * per garantire unicità assoluta nel DB.
 */
public class Document {

    private final int id;
    private final String originalFilename;
    private final String storedFilename;
    private final String mimeType;
    private final long sizeBytes;
    private final byte[] content;
    private final LocalDateTime uploadedAt;

    public Document(int id,
                    String originalFilename,
                    String storedFilename,
                    String mimeType,
                    long sizeBytes,
                    byte[] content,
                    LocalDateTime uploadedAt) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.content = content;
        this.uploadedAt = uploadedAt;
    }

    public int getId() { return id; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStoredFilename() { return storedFilename; }
    public String getMimeType() { return mimeType; }
    public long getSizeBytes() { return sizeBytes; }
    public byte[] getContent() { return content; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}