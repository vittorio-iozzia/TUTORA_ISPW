package it.ispw.tutora.dao.db;

import it.ispw.tutora.dao.CategoryDao;
import it.ispw.tutora.dao.factory.ConnectionFactory;
import it.ispw.tutora.exception.CategoryNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Category;
import it.ispw.tutora.model.DocumentRequirement;
import it.ispw.tutora.model.TextRequirement;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementazione JDBC di CategoryDao.
 *
 * Poiché CategoryDao non partecipa a transazioni multi-tabella,
 * ogni metodo apre e chiude autonomamente la propria Connection
 * tramite ConnectionFactory (try-with-resources).
 *
 * findByNameWithRequirements usa un'unica Connection per le tre
 * query consecutive (category, text_requirement, document_requirement),
 * garantendo una lettura consistente senza overhead di connessioni extra.
 */
public class CategoryDaoDb implements CategoryDao {

    @Language("SQL")
    private static final String SQL_FIND_ALL =
            "SELECT name, description " +
            "FROM category " +
            "ORDER BY name";

    @Language("SQL")
    private static final String SQL_FIND_BY_NAME =
            "SELECT name, description " +
            "FROM category " +
            "WHERE name = ?";

    @Language("SQL")
    private static final String SQL_TEXT_REQUIREMENTS =
            "SELECT name, label, description, min_char, max_char, is_required " +
            "FROM text_requirement " +
            "WHERE category_name = ? " +
            "ORDER BY name";

    @Language("SQL")
    private static final String SQL_DOCUMENT_REQUIREMENTS =
            "SELECT name, label, description, is_required " +
            "FROM document_requirement " +
            "WHERE category_name = ? " +
            "ORDER BY name";

    // ----------------------------------------------------------------
    // findAll
    // ----------------------------------------------------------------

    @Override
    public List<Category> findAll() throws DatabaseException {
        List<Category> categories = new ArrayList<>();

        try (Connection conn = ConnectionFactory.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                categories.add(mapCategory(rs));
            }

        } catch (SQLException e) {
            throw new DatabaseException("Errore nel recupero delle categorie.", e);
        }

        return categories;
    }

    // ----------------------------------------------------------------
    // findByNameWithRequirements
    // ----------------------------------------------------------------

    @Override
    public Category findByNameWithRequirements(String name)
            throws DatabaseException, CategoryNotFoundException {

        try (Connection conn = ConnectionFactory.getInstance().getConnection()) {

            Category category = fetchCategory(conn, name);
            fetchTextRequirements(conn, category);
            fetchDocumentRequirements(conn, category);
            return category;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Errore nel recupero della categoria '" + name + "'.", e);
        }
    }

    // ----------------------------------------------------------------
    // Helper privati
    // ----------------------------------------------------------------

    private Category fetchCategory(Connection conn, String name)
            throws SQLException, CategoryNotFoundException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NAME)) {
            ps.setString(1, name);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new CategoryNotFoundException(name);
                }
                return mapCategory(rs);
            }
        }
    }

    private void fetchTextRequirements(Connection conn, Category category)
            throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_TEXT_REQUIREMENTS)) {
            ps.setString(1, category.getName());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    category.addRequirement(new TextRequirement(
                            category.getName(),
                            rs.getString("name"),
                            rs.getString("label"),
                            rs.getString("description"),
                            rs.getBoolean("is_required"),
                            rs.getInt("min_char"),
                            rs.getInt("max_char")
                    ));
                }
            }
        }
    }

    private void fetchDocumentRequirements(Connection conn, Category category)
            throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_DOCUMENT_REQUIREMENTS)) {
            ps.setString(1, category.getName());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    category.addRequirement(new DocumentRequirement(
                            category.getName(),
                            rs.getString("name"),
                            rs.getString("label"),
                            rs.getString("description"),
                            rs.getBoolean("is_required")
                    ));
                }
            }
        }
    }

    private Category mapCategory(ResultSet rs) throws SQLException {
        return new Category(
                rs.getString("name"),
                rs.getString("description")
        );
    }
}
