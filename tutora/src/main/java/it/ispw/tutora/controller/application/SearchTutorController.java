package it.ispw.tutora.controller.application;

import it.ispw.tutora.bean.CategoryBean;
import it.ispw.tutora.dao.CategoryDao;
import it.ispw.tutora.dao.TutorDao;
import it.ispw.tutora.dao.TutorExpertiseDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.Status;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Category;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.TutorExpertise;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


public class SearchTutorController {

    private static final Logger LOGGER =
            Logger.getLogger(SearchTutorController.class.getName());

    /**
     * Restituisce le categorie valide come entity — usato da CLI e TutorBrowseUtil.
     */
    public List<Category> loadCategories() throws DatabaseException {
        CategoryDao dao = DaoFactory.getInstance().createCategoryDao();
        return dao.findAll();
    }

    /**
     * Restituisce le categorie valide come bean — usato dalla GUI (Apply to Become a Tutor).
     */
    public List<CategoryBean> loadCategoriesAsBean() throws DatabaseException {
        List<CategoryBean> beans = new ArrayList<>();
        for (Category c : loadCategories()) {
            CategoryBean bean = new CategoryBean();
            bean.setName(c.getName());
            bean.setDescription(c.getDescription());
            beans.add(bean);
        }
        return beans;
    }

    /**
     * Restituisce tutti i tutor registrati nel sistema.
     * Usato dai controller grafici che mostrano la lista tutor (FindTutor,
     * StudentContent, AdminContent) senza chiamare TutorDao direttamente.
     */
    public List<Tutor> loadAllTutors() {
        DaoFactory factory = DaoFactory.getInstance();
        TutorDao dao = factory.createTutorDao();
        try (Connection conn = factory.getConnection()) {
            return dao.selectAllTutors(conn);
        } catch (DatabaseException | SQLException e) {
            LOGGER.warning("Cannot load tutors: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Carica le expertise approvate per ciascun tutor nella lista.
     * Restituisce una mappa username → lista nomi subcategory (max 3, APPROVED).
     * Usato dai controller grafici per mostrare i tag nelle card tutor
     * senza chiamare TutorExpertiseDao direttamente.
     */
    public Map<String, List<String>> loadExpertisesForTutors(List<Tutor> tutors) {
        DaoFactory factory = DaoFactory.getInstance();
        TutorExpertiseDao dao = factory.createTutorExpertiseDao();
        Map<String, List<String>> result = new HashMap<>();
        try (Connection conn = factory.getConnection()) {
            for (Tutor t : tutors) {
                List<TutorExpertise> list = dao.findByTutor(conn, t.getUsername());
                List<String> approved = list.stream()
                        .filter(e -> e.getStatus() == Status.APPROVED)
                        .map(e -> e.getSubcategory().getName())
                        .distinct()
                        .limit(3)
                        .toList();
                result.put(t.getUsername(), approved);
            }
        } catch (SQLException | DatabaseException e) {
            LOGGER.warning("Cannot load expertises: " + e.getMessage());
        }
        return result;
    }

    /**
     * Filtra i tutor per query testuale (case-insensitive).
     * La query viene confrontata con nome completo, username, descrizione
     * e nomi delle expertise approvate pre-caricati dalla view.
     */
    public List<Tutor> filterByQuery(List<Tutor> tutors,
                                     Map<String, List<String>> expertiseNames,
                                     String query) {
        if (query == null || query.isBlank()) return tutors;
        String q = query.toLowerCase();
        return tutors.stream()
                .filter(t -> t.getFullName().toLowerCase().contains(q)
                          || t.getUsername().toLowerCase().contains(q)
                          || (t.getDescription() != null
                              && t.getDescription().toLowerCase().contains(q))
                          || expertiseNames.getOrDefault(t.getUsername(), List.of())
                                .stream().anyMatch(s -> s.toLowerCase().contains(q)))
                .toList();
    }

    /**
     * Overload senza expertise names: filtra per nome completo, username e descrizione.
     * Usato dall'admin dove la mappa expertise non è pre-caricata.
     *
     * @see #filterByQuery(List, Map, String)
     */
    public List<Tutor> filterByQuery(List<Tutor> tutors, String query) {
        return filterByQuery(tutors, Map.of(), query);
    }

    public List<Tutor> filterByCategory(List<Tutor> all, String categoryName) {
        DaoFactory factory = DaoFactory.getInstance();
        TutorExpertiseDao expertiseDao = factory.createTutorExpertiseDao();
        List<Tutor> result = new ArrayList<>();
        try (Connection conn = factory.getConnection()) {
            for (Tutor tutor : all) {
                List<TutorExpertise> expertises =
                        expertiseDao.findByTutor(conn, tutor.getUsername());
                boolean matches = expertises.stream().anyMatch(e ->
                        e.getSubcategory().getParentCategory() != null
                        && categoryName.equalsIgnoreCase(
                                e.getSubcategory().getParentCategory().getName()));
                if (matches) result.add(tutor);
            }
        } catch (SQLException | DatabaseException e) {
            LOGGER.warning("Error filtering tutors by category: " + e.getMessage());
        }
        return result;
    }
}
