package it.ispw.tutora.dao.demo;

import it.ispw.tutora.dao.DocumentDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DocumentNotFoundException;
import it.ispw.tutora.model.Document;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementazione in memoria di DocumentDao.
 * Usata al posto di DocumentDaoDb quando il DB non è disponibile.
 *
 * -----------------------------------------------------------------------
 * Nota sulla gestione degli id
 * -----------------------------------------------------------------------
 * insert() costruisce un nuovo Document con l'id generato da nextId,
 * quindi findById() e delete() possono cercare correttamente per id.
 *
 * -----------------------------------------------------------------------
 * Nota
 * -----------------------------------------------------------------------
 * Il parametro Connection viene ignorato — non c'è nessun DB.
 * I dati vivono in memoria per tutta la durata dell'esecuzione
 * e spariscono alla chiusura dell'applicazione.
 * Il ciclo di vita è gestito da DemoDaoFactory — non serve Singleton.
 */
public class DocumentDaoDemo implements DocumentDao {

    private final List<Document> cache = new ArrayList<>();
    private int nextId = 1;

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    @Override
    public int insert(Connection conn, Document document) throws DatabaseException {
        int id = nextId++;
        cache.add(new Document(
                id,
                document.getOriginalFilename(),
                document.getStoredFilename(),
                document.getMimeType(),
                document.getSizeBytes(),
                document.getContent(),
                LocalDateTime.now()
        ));
        return id;
    }

    // ----------------------------------------------------------------
    // findById
    // ----------------------------------------------------------------

    @Override
    public Document findById(Connection conn, int id)
            throws DatabaseException, DocumentNotFoundException {
        for (Document d : cache) {
            if (d.getId() == id) return d;
        }
        throw new DocumentNotFoundException(id);
    }

    // ----------------------------------------------------------------
    // delete
    // ----------------------------------------------------------------

    @Override
    public void delete(Connection conn, int id)
            throws DatabaseException, DocumentNotFoundException {
        boolean removed = cache.removeIf(d -> d.getId() == id);
        if (!removed) throw new DocumentNotFoundException(id);
    }
}
