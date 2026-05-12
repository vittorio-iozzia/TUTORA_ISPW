package it.ispw.tutora.dao.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.ispw.tutora.dao.DocumentDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DocumentNotFoundException;
import it.ispw.tutora.model.Document;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Implementazione di DocumentDao basata su file JSON.
 * Usata quando DAO_TYPE=JSON in app.properties.
 *
 * -----------------------------------------------------------------------
 * Persistenza
 * -----------------------------------------------------------------------
 * Legge e scrive su ../tutora_data/documents.json.
 * Ogni operazione di scrittura applica il pattern read-modify-write.
 *
 * -----------------------------------------------------------------------
 * Contenuto binario
 * -----------------------------------------------------------------------
 * Il campo content (byte[]) viene serializzato come stringa Base64
 * e ricostruito con Base64.getDecoder().decode() in lettura.
 * Nessuna dipendenza aggiuntiva oltre a java.util.Base64 (Java 8+).
 *
 * -----------------------------------------------------------------------
 * Nota sul parametro Connection
 * -----------------------------------------------------------------------
 * Il parametro Connection viene ignorato: non c'è nessun DB.
 * È presente solo per rispettare il contratto dell'interfaccia DocumentDao,
 * pensata per la gestione delle transazioni JDBC.
 */
public class DocumentDaoJson implements DocumentDao {

    private static final String JSON_PATH = "../tutora_data/documents.json";
    private final ObjectMapper mapper = new ObjectMapper();

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    @Override
    public int insert(Connection conn, Document document) throws DatabaseException {
        List<DocRecord> records = readAll();

        // Simulo in Json un ID auto-incremented
        int id = records.stream()                     // Crea uno stream di AppRecord
                .mapToInt(r -> r.id)        // Trasforma ogni AppRecord nel suo id (IntStream)
                .max()                                // Trova il valore massimo
                .orElse(0)                      // Se la lista è vuota restituisce zero
                + 1;                                  // Aggiunge uno per otterene il prossimo id


        DocRecord newRecord = new DocRecord();
        newRecord.id = id;
        newRecord.originalFilename = document.getOriginalFilename();
        newRecord.storedFilename = document.getStoredFilename();
        newRecord.mimeType = document.getMimeType();
        newRecord.sizeBytes = document.getSizeBytes();
        newRecord.contentBase64 = document.getContent() != null
                ? Base64.getEncoder().encodeToString(document.getContent())
                : null;
        newRecord.uploadedAt = LocalDateTime.now().toString();

        records.add(newRecord);
        writeAll(records);
        return id;
    }

    // ----------------------------------------------------------------
    // findById
    // ----------------------------------------------------------------

    @Override
    public Document findById(Connection conn, int id)
            throws DatabaseException, DocumentNotFoundException {

        for (DocRecord r : readAll()) {
            if (r.id == id) return toDocument(r);
        }
        throw new DocumentNotFoundException(id);
    }

    // ----------------------------------------------------------------
    // delete
    // ----------------------------------------------------------------

    @Override
    public void delete(Connection conn, int id)
            throws DatabaseException, DocumentNotFoundException {

        List<DocRecord> records = readAll();
        boolean removed = records.removeIf(r -> r.id == id);
        if (!removed) throw new DocumentNotFoundException(id);
        writeAll(records);
    }

    // ----------------------------------------------------------------
    // Helper privati
    // ----------------------------------------------------------------

    private List<DocRecord> readAll() throws DatabaseException {
        try {
            DocRecord[] records = mapper.readValue(new File(JSON_PATH), DocRecord[].class);
            return new ArrayList<>(Arrays.asList(records));
        } catch (IOException e) {
            throw new DatabaseException("Error reading JSON file: " + JSON_PATH, e);
        }
    }

    private void writeAll(List<DocRecord> records) throws DatabaseException {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(JSON_PATH), records);
        } catch (IOException e) {
            throw new DatabaseException("Error writing JSON file: " + JSON_PATH, e);
        }
    }

    private Document toDocument(DocRecord r) {
        byte[] content = r.contentBase64 != null
                ? Base64.getDecoder().decode(r.contentBase64)
                : null;
        return new Document(
                r.id,
                r.originalFilename,
                r.storedFilename,
                r.mimeType,
                r.sizeBytes,
                content,
                LocalDateTime.parse(r.uploadedAt));
    }

    // ----------------------------------------------------------------
    // POJO interno per la deserializzazione Jackson
    // ----------------------------------------------------------------

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static class DocRecord {
        int id;
        String originalFilename;
        String storedFilename;
        String mimeType;
        long sizeBytes;
        String contentBase64;
        String uploadedAt;
    }
}
