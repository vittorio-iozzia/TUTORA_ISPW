package it.ispw.tutora.controller.application;

import it.ispw.tutora.dao.CategoryDao;
import it.ispw.tutora.dao.TutorExpertiseDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Category;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.TutorExpertise;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Application controller for the "Search Tutor" use case.
 *
 * Loads available categories from {@link CategoryDao} and filters a
 * tutor list by category by inspecting each tutor's expertises via
 * {@link TutorExpertiseDao}, keeping the graphic controller free from
 * DAO dependencies.
 */
public class SearchTutorController {

    private static final Logger LOGGER =
            Logger.getLogger(SearchTutorController.class.getName());

    /**
     * Returns all available categories.
     * Used to populate the category pills in the Explore Tutors tab.
     */
    public List<Category> loadCategories() throws DatabaseException {
        CategoryDao dao = DaoFactory.getInstance().createCategoryDao();
        return dao.findAll();
    }

    /**
     * Filters the given list keeping only tutors who have at least one
     * expertise whose parent category name matches {@code categoryName}
     * (case-insensitive).
     */
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
