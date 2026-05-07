package it.ispw.tutora.dao;

import it.ispw.tutora.exception.CategoryNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Category;

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
     *
     * @param name nome della categoria (PK)
     * @throws CategoryNotFoundException se la categoria non esiste
     */
    Category findByNameWithRequirements(String name)
            throws DatabaseException, CategoryNotFoundException;
}