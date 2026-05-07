package it.ispw.tutora.dao;

import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DocumentNotFoundException;
import it.ispw.tutora.model.Document;

import java.sql.Connection;

/**
 * Contratto DAO per la tabella document.
 *
 * -----------------------------------------------------------------------
 * Nota sulle transazioni
 * -----------------------------------------------------------------------
 * Tutti i metodi ricevono la Connection come parametro: è il Controller
 * applicativo a gestire commit e rollback, non il DAO.
 * L'inserimento di un Document avviene sempre nella stessa transazione
 * in cui viene creato il DocumentItem che lo referenzia.
 *
 * -----------------------------------------------------------------------
 * Nota sul vincolo FK RESTRICT
 * -----------------------------------------------------------------------
 * La colonna document_id in application_item è definita con
 * ON DELETE RESTRICT: un Document non può essere eliminato finché
 * almeno un ApplicationItem lo referenzia. Il Controller deve quindi
 * prima aggiornare o eliminare l'item, poi invocare delete().
 */
public interface DocumentDao {

    /**
     * Persiste un nuovo documento (metadati + contenuto binario).
     * Il campo stored_filename deve essere generato (UUID) dal chiamante
     * prima dell'invocazione, così da garantire unicità nel DB.
     *
     * @return id AUTO_INCREMENT assegnato dal DB
     */
    int insert(Connection conn, Document document)
            throws DatabaseException;

    /**
     * Carica un documento per id, incluso il contenuto binario.
     * Usata per il download o la visualizzazione da parte dell'admin.
     *
     * @throws DocumentNotFoundException se l'id non corrisponde
     *         ad alcuna riga in document
     */
    Document findById(Connection conn, int id)
            throws DatabaseException, DocumentNotFoundException;

    /**
     * Elimina un documento. Può essere invocata solo dopo che nessun
     * ApplicationItem referenzia più il documento (vincolo RESTRICT),
     * altrimenti il DB solleverà un'eccezione di integrità referenziale
     * che verrà propagata come DatabaseException.
     *
     * @throws DocumentNotFoundException se l'id non corrisponde
     *         ad alcuna riga in document
     */
    void delete(Connection conn, int id)
            throws DatabaseException, DocumentNotFoundException;
}
