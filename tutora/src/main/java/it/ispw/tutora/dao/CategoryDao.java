package it.ispw.tutora.dao;

import it.ispw.tutora.exception.CategoryNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Category;

import java.sql.Connection;
import java.util.List;

/**
 * Contratto DAO per la tabella category e i suoi requisiti.
 *
 * L'implementazione concreta (CategoryDaoDb, CategoryDaoDemo,
 * CategoryDaoJson) è l'unica classe che conosce i dettagli
 * di persistenza. Il Controller applicativo dipende solo
 * da questa interfaccia, mai dall'implementazione.
 */
public interface CategoryDao {

    /**
     * Restituisce tutte le categorie disponibili, senza requisiti.
     * Usata per popolare la lista di selezione nello step 1 del form.
     */
    List<Category> findAll() throws DatabaseException;

    /**
     * Restituisce una categoria con la lista completa dei suoi requisiti
     * (TextRequirement + DocumentRequirement), pronti per costruire il form.
     */
    Category findByNameWithRequirements(String name)
            throws DatabaseException, CategoryNotFoundException;

    /**
     * Assicura che la subcategory esista; usa INSERT IGNORE per essere idempotente.
     * Chiamato in modalità DB prima di insertExpertise() per evitare violazioni FK.
     * Le implementazioni non-DB sono no-op perché non hanno vincoli FK.
     *
     * @param conn             connessione nella transazione corrente (mai null in modalità DB)
     * @param categoryName     categoria padre
     * @param subcategoryName  nome della subcategory da garantire
     */
    void ensureSubcategoryExists(Connection conn, String categoryName, String subcategoryName);

    /**
     * Cerca nella tabella subcategory il nome esatto corrispondente a rawName,
     * usando confronto case-insensitive con fallback su LIKE parziale.
     * Usato per risolvere il testo libero dell'utente in un valore FK valido.
     *
     * Le implementazioni non-DB (Demo, Json) restituiscono rawName invariato
     * perché non hanno vincoli FK da soddisfare.
     *
     * @param conn          connessione nella transazione corrente (mai null in modalità DB)
     * @param categoryName  categoria padre della subcategory
     * @param rawName       testo inserito dall'utente
     * @return              nome esatto trovato nel DB, oppure rawName se nessun match
     */
    String findSubcategoryName(Connection conn, String categoryName, String rawName);
}