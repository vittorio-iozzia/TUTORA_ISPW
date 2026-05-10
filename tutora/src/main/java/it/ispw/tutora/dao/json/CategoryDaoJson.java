package it.ispw.tutora.dao.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.ispw.tutora.dao.CategoryDao;
import it.ispw.tutora.exception.CategoryNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Category;
import it.ispw.tutora.model.DocumentRequirement;
import it.ispw.tutora.model.TextRequirement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementazione di CategoryDao basata su file JSON.
 * Usata quando DAO_TYPE=JSON in app.properties.
 *
 * -----------------------------------------------------------------------
 * File sorgente
 * -----------------------------------------------------------------------
 * Legge da ../tutora_data/categories.json rispetto alla working directory
 * del modulo Maven (TUTORA_ISPW/tutora/). Un file per entità garantisce
 * separazione pulita e caricamento indipendente per entità.
 *
 * -----------------------------------------------------------------------
 * Struttura JSON attesa
 * -----------------------------------------------------------------------
 * Array di oggetti categoria con i campi:
 *   name, description,
 *   textRequirements[]     { name, label, description, required, minChar, maxChar }
 *   documentRequirements[] { name, label, description, required }
 *
 * I due array separati evitano la gestione del polimorfismo in fase di
 * deserializzazione, mantenendo il JSON leggibile e il codice semplice.
 */
public class CategoryDaoJson implements CategoryDao {

    private static final String JSON_PATH = "../tutora_data/categories.json";

    private final ObjectMapper mapper = new ObjectMapper(); // Classe di Jackson che permette la conversione degli oggetti java/json

    // ----------------------------------------------------------------
    // findAll
    // ----------------------------------------------------------------

    @Override
    public List<Category> findAll() throws DatabaseException {
        return readAll();
    }

    // ----------------------------------------------------------------
    // findByNameWithRequirements
    // ----------------------------------------------------------------

    @Override
    public Category findByNameWithRequirements(String name)
            throws DatabaseException, CategoryNotFoundException {

        for (Category c : readAll()) {
            if (c.getName().equals(name)) return c;
        }
        throw new CategoryNotFoundException(name);
    }

    // ----------------------------------------------------------------
    // Helper privati
    // ----------------------------------------------------------------

    private List<Category> readAll() throws DatabaseException {
        CategoryRecord[] records;
        try {
            records = mapper.readValue(new File(JSON_PATH), CategoryRecord[].class);
        } catch (IOException e) {
            throw new DatabaseException(
                    "Error reading JSON file: " + JSON_PATH, e);
        }

        List<Category> result = new ArrayList<>();
        for (CategoryRecord r : records) {
            result.add(toCategory(r));
        }
        return result;
    }

    private Category toCategory(CategoryRecord r) {
        Category category = new Category(r.name, r.description);

        if (r.textRequirements != null) {
            for (TextReqRecord tr : r.textRequirements) {
                category.addRequirement(new TextRequirement(
                        r.name, tr.name, tr.label, tr.description,
                        tr.required, tr.minChar, tr.maxChar));
            }
        }
        if (r.documentRequirements != null) {
            for (DocReqRecord dr : r.documentRequirements) {
                category.addRequirement(new DocumentRequirement(
                        r.name, dr.name, dr.label, dr.description, dr.required));
            }
        }
        return category;
    }

    // ----------------------------------------------------------------
    // POJO interni per la deserializzazione Jackson
    // Campi package-private: visibili solo a ObjectMapper via reflection.
    // ----------------------------------------------------------------

    private static class CategoryRecord {
        public String name;
        public String description;
        public List<TextReqRecord> textRequirements;
        public List<DocReqRecord> documentRequirements;
    }

    private static class TextReqRecord {
        public String name;
        public String label;
        public String description;
        public boolean required;
        public int minChar;
        public int maxChar;
    }

    private static class DocReqRecord {
        public String name;
        public String label;
        public String description;
        public boolean required;
    }
}
