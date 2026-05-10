package it.ispw.tutora.dao.demo;

import it.ispw.tutora.dao.ApplicationItemDao;
import it.ispw.tutora.exception.ApplicationItemNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.ApplicationItem;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementazione in memoria di ApplicationItemDao.
 * Usata al posto di ApplicationItemDaoDb quando il DB non è disponibile.
 *
 * -----------------------------------------------------------------------
 * Nota sulla gestione degli id
 * -----------------------------------------------------------------------
 * Poiché il campo id è final in ApplicationItem, l'id generato da insert()
 * non può essere scritto nell'oggetto dopo la costruzione. Per questo motivo
 * il cache è una LinkedHashMap in cui la chiave è l'id generato e il valore
 * è l'item. findById() cerca per chiave della mappa. update() cerca per
 * chiave naturale (applicationId + requirementName), così funziona anche
 * se l'item passato è stato costruito con id=0.
 *
 * -----------------------------------------------------------------------
 * Nota
 * -----------------------------------------------------------------------
 * Il parametro Connection viene ignorato — non c'è nessun DB.
 * I dati vivono in memoria per tutta la durata dell'esecuzione
 * e spariscono alla chiusura dell'applicazione.
 * Il ciclo di vita è gestito da DemoDaoFactory — non serve Singleton.
 */
public class ApplicationItemDaoDemo implements ApplicationItemDao {

    private final Map<Integer, ApplicationItem> cache = new LinkedHashMap<>();  // LinkedHashMap permette una ricerca istantanea con chiave: costo O(1)
    private int nextId = 1;

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    @Override
    public int insert(Connection conn, ApplicationItem item) throws DatabaseException {
        int id = nextId++;
        cache.put(id, item);
        return id;
    }

    // ----------------------------------------------------------------
    // update
    // ----------------------------------------------------------------

    /**
     * Cerca per chiave naturale (applicationId + requirementName) invece che
     * per id, perché l'item passato potrebbe essere stato costruito con id=0.  --> In ApplicationItem id è final
     */
    @Override
    public void update(Connection conn, ApplicationItem item)
            throws DatabaseException, ApplicationItemNotFoundException {
        for (Map.Entry<Integer, ApplicationItem> entry : cache.entrySet()) {
            ApplicationItem cached = entry.getValue();

            // Cerca per applicationId + requirementName invece che per id
            if (cached.getApplicationId() == item.getApplicationId()
                    && cached.getRequirementName().equals(item.getRequirementName())) {

            // Trovato -  sovrascrive il valore mantenendo la stedda chiave (id)
                cache.put(entry.getKey(), item);
                return;
            }
        }
        // Non trovato - lancia eccezione
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
        ApplicationItem item = cache.get(id);
        if (item == null) throw new ApplicationItemNotFoundException(id);
        return item;
    }

    // ----------------------------------------------------------------
    // findByApplicationId
    // ----------------------------------------------------------------

    @Override
    public List<ApplicationItem> findByApplicationId(Connection conn, int applicationId)
            throws DatabaseException {
        List<ApplicationItem> result = new ArrayList<>();
        for (ApplicationItem item : cache.values()) {
            if (item.getApplicationId() == applicationId) result.add(item);
        }
        return result;
    }
}
