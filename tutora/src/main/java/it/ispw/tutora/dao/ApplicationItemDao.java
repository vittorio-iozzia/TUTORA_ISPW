package it.ispw.tutora.dao;

import it.ispw.tutora.exception.ApplicationItemNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.ApplicationItem;

import java.sql.Connection;
import java.util.List;

/**
 * Contratto DAO per la tabella application_item.
 *
 * -----------------------------------------------------------------------
 * Nota sulle transazioni
 * -----------------------------------------------------------------------
 * Tutti i metodi ricevono la Connection come parametro: è il Controller
 * applicativo a gestire commit e rollback, non il DAO.
 * Gli item vengono sempre inseriti/aggiornati nell'ambito della stessa
 * transazione che coinvolge TutorApplication.
 *
 * -----------------------------------------------------------------------
 * Nota sul polimorfismo
 * -----------------------------------------------------------------------
 * In lettura l'implementazione concreta deve ricostruire il sottotipo
 * corretto (TextItem o DocumentItem) in base al valore della colonna
 * item_type. Il metodo di fabbrica ApplicationItemFactory incapsula
 * questa logica di disambiguazione.
 */
public interface ApplicationItemDao {

    /**
     * Persiste un nuovo item (TextItem o DocumentItem) per una application.
     * La colonna item_type è determinata da {@code item.getItemType()}.
     */
    int insert(Connection conn, ApplicationItem item)
            throws DatabaseException;

    /**
     * Aggiorna il contenuto di un item già esistente.
     * Per un TextItem sovrascrive text_content;
     * per un DocumentItem sovrascrive document_id.
     */
    void update(Connection conn, ApplicationItem item)
            throws DatabaseException, ApplicationItemNotFoundException;

    /**
     * Carica tutti gli item associati a una application, ordinati per
     * requirement_name. Usata per il lazy-loading degli item dopo che
     * TutorApplication è stata recuperata senza di essi.
     */
    List<ApplicationItem> findByApplicationId(Connection conn, int applicationId)
            throws DatabaseException;

    /**
     * Carica un singolo item per id.
     */
    ApplicationItem findById(Connection conn, int id)
            throws DatabaseException, ApplicationItemNotFoundException;
}
