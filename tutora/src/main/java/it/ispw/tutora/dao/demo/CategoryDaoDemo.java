package it.ispw.tutora.dao.demo;

import it.ispw.tutora.dao.CategoryDao;
import it.ispw.tutora.exception.CategoryNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Category;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementazione in memoria di CategoryDao.
 * Usata al posto di CategoryDaoDb quando il DB non è disponibile.
 *
 * -----------------------------------------------------------------------
 * Nota
 * -----------------------------------------------------------------------
 * I dati vivono in memoria per tutta la durata dell'esecuzione
 * e spariscono alla chiusura dell'applicazione.
 * Il ciclo di vita è gestito da DemoDaoFactory — non serve Singleton.
 */
public class CategoryDaoDemo implements CategoryDao {

    private final List<Category> cache = new ArrayList<>();

    // ----------------------------------------------------------------
    // Metodo di popolamento (chiamato da DemoDaoFactory)
    // ----------------------------------------------------------------

    public void add(Category category) {
        cache.add(category);
    }

    // ----------------------------------------------------------------
    // findAll
    // ----------------------------------------------------------------

    @Override
    public List<Category> findAll() throws DatabaseException {
        return new ArrayList<>(cache);
    }

    // ----------------------------------------------------------------
    // findByNameWithRequirements
    // ----------------------------------------------------------------

    @Override
    public Category findByNameWithRequirements(String name)
            throws DatabaseException, CategoryNotFoundException {
        for (Category c : cache) {
            if (c.getName().equals(name)) return c;
        }
        throw new CategoryNotFoundException(name);
    }

    @Override
    public void ensureSubcategoryExists(Connection conn, String categoryName, String subcategoryName) {
        // no-op: implementazione in memoria, nessun FK da soddisfare
    }

    @Override
    public String findSubcategoryName(Connection conn, String categoryName, String rawName) {
        return rawName;
    }
}
