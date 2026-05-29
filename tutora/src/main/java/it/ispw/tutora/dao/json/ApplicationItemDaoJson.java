package it.ispw.tutora.dao.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.ispw.tutora.dao.ApplicationItemDao;
import it.ispw.tutora.enums.ItemType;
import it.ispw.tutora.exception.ApplicationItemNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.ApplicationItem;
import it.ispw.tutora.model.Document;
import it.ispw.tutora.model.DocumentItem;
import it.ispw.tutora.model.TextItem;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementazione di ApplicationItemDao basata su file JSON.
 * Usata quando DAO_TYPE=JSON in app.properties.
 *
 * -----------------------------------------------------------------------
 * Persistenza
 * -----------------------------------------------------------------------
 * Legge e scrive su ../tutora_data/application_items.json.
 * Ogni operazione di scrittura applica il pattern read-modify-write.
 *
 * -----------------------------------------------------------------------
 * Polimorfismo TextItem / DocumentItem
 * -----------------------------------------------------------------------
 * Il campo itemType ("TEXT" o "DOCUMENT") discrimina il sottotipo.
 * Per i DocumentItem viene salvato solo documentId (FK verso documents.json):
 * il Document viene ricostruito come oggetto parziale con il solo id,
 * coerente con il comportamento di ApplicationItemDaoDb che usa JOIN.
 * Il contenuto completo del Document si carica separatamente via DocumentDao.
 *
 * -----------------------------------------------------------------------
 * Nota sul parametro Connection
 * -----------------------------------------------------------------------
 * Il parametro Connection viene ignorato: non c'è nessun DB.
 * È presente solo per rispettare il contratto dell'interfaccia.
 *
 * -----------------------------------------------------------------------
 * Nota su update()
 * -----------------------------------------------------------------------
 * La ricerca in update() avviene per (applicationId + requirementName)
 * invece che per id, perché l'item potrebbe essere stato costruito con id=0
 * (il campo id è final in ApplicationItem), come da pattern già usato
 * in ApplicationItemDaoDemo.
 */
public class ApplicationItemDaoJson implements ApplicationItemDao {

    private static final String JSON_PATH = "../tutora_data/application_items.json";
    private final ObjectMapper mapper = new ObjectMapper();

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    @Override
    public int insert(Connection conn, ApplicationItem item) throws DatabaseException {
        List<ItemRecord> records = readAll();

        // Simulo in Json un ID auto-incremented
        int id = records.stream().mapToInt(r -> r.id).max().orElse(0) + 1;


        ItemRecord newRecord = new ItemRecord();
        newRecord.id = id;
        newRecord.applicationId = item.getApplicationId();
        newRecord.requirementName = item.getRequirementName();
        newRecord.itemType = item.getItemType().name();

        if (item instanceof TextItem textItem) {
            newRecord.textContent = textItem.getTextContent();
        } else {
            newRecord.documentId = ((DocumentItem) item).getDocument().getId();
        }

        records.add(newRecord);
        writeAll(records);
        return id;
    }

    // ----------------------------------------------------------------
    // update
    // ----------------------------------------------------------------

    @Override
    public void update(Connection conn, ApplicationItem item)
            throws DatabaseException, ApplicationItemNotFoundException {

        List<ItemRecord> records = readAll();
        for (ItemRecord r : records) {
            if (r.applicationId == item.getApplicationId()
                    && r.requirementName.equals(item.getRequirementName())) {

                if (item instanceof TextItem textItem) {
                    r.textContent = textItem.getTextContent();
                } else {
                    r.documentId = ((DocumentItem) item).getDocument().getId();
                }
                writeAll(records);
                return;
            }
        }
        throw new ApplicationItemNotFoundException(
                "ApplicationItem not found for applicationId: " + item.getApplicationId()
                + ", requirementName: '" + item.getRequirementName() + "'");
    }

    // ----------------------------------------------------------------
    // findById
    // ----------------------------------------------------------------

    @Override
    public ApplicationItem findById(Connection conn, int id)
            throws DatabaseException, ApplicationItemNotFoundException {

        for (ItemRecord r : readAll()) {
            if (r.id == id) return toItem(r);
        }
        throw new ApplicationItemNotFoundException(id);
    }

    // ----------------------------------------------------------------
    // findByApplicationId
    // ----------------------------------------------------------------

    @Override
    public List<ApplicationItem> findByApplicationId(Connection conn, int applicationId)
            throws DatabaseException {

        List<ApplicationItem> result = new ArrayList<>();
        for (ItemRecord r : readAll()) {
            if (r.applicationId == applicationId) result.add(toItem(r));
        }
        return result;
    }

    // ----------------------------------------------------------------
    // Helper privati
    // ----------------------------------------------------------------

    private List<ItemRecord> readAll() throws DatabaseException {
        try {
            ItemRecord[] records = mapper.readValue(new File(JSON_PATH), ItemRecord[].class);
            return new ArrayList<>(Arrays.asList(records));
        } catch (IOException e) {
            throw new DatabaseException("Error reading JSON file: " + JSON_PATH, e);
        }
    }

    private void writeAll(List<ItemRecord> records) throws DatabaseException {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(JSON_PATH), records);
        } catch (IOException e) {
            throw new DatabaseException("Error writing JSON file: " + JSON_PATH, e);
        }
    }

    private ApplicationItem toItem(ItemRecord r) {
        if (ItemType.valueOf(r.itemType) == ItemType.TEXT) {
            return new TextItem(r.id, r.applicationId, r.requirementName, r.textContent);
        }
        // DocumentItem: ricostruisce un Document parziale con il solo id.
        // Il contenuto completo si carica separatamente via DocumentDao.
        Document partialDoc = new Document(r.documentId, null, null, null, 0, null, null);
        return new DocumentItem(r.id, r.applicationId, r.requirementName, partialDoc);
    }

    // ----------------------------------------------------------------
    // POJO interno per la deserializzazione Jackson
    // ----------------------------------------------------------------

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static class ItemRecord {
        int id;
        int applicationId;
        String requirementName;
        String itemType;
        String textContent;
        Integer documentId;
    }
}
