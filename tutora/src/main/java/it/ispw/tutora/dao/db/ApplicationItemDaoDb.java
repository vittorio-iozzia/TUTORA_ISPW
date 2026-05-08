package it.ispw.tutora.dao.db;

import it.ispw.tutora.dao.ApplicationItemDao;
import it.ispw.tutora.enums.ItemType;
import it.ispw.tutora.exception.ApplicationItemNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.ApplicationItem;
import it.ispw.tutora.model.Document;
import it.ispw.tutora.model.DocumentItem;
import it.ispw.tutora.model.TextItem;
import org.intellij.lang.annotations.Language;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementazione JDBC di ApplicationItemDao.
 *
 * -----------------------------------------------------------------------
 * Nota sulle transazioni
 * -----------------------------------------------------------------------
 * Tutti i metodi ricevono la Connection come parametro: commit e rollback
 * sono gestiti dal Controller applicativo, non dal DAO.
 *
 * -----------------------------------------------------------------------
 * Nota sul polimorfismo
 * -----------------------------------------------------------------------
 * In lettura, mapItem() ricostruisce il sottotipo corretto (TextItem o
 * DocumentItem) in base al valore della colonna item_type.
 * Per i DocumentItem le query usano un LEFT JOIN sulla tabella document,
 * così il Document viene popolato in un'unica round-trip al DB.
 *
 * -----------------------------------------------------------------------
 * Nota sulla conversione del tipo
 * -----------------------------------------------------------------------
 * Il DB memorizza item_type come ENUM('Text','Document').
 * I metodi toDbType / fromDbType gestiscono la conversione bidirezionale
 * rispetto all'enum Java ItemType (TEXT, DOCUMENT).
 */
public class ApplicationItemDaoDb implements ApplicationItemDao {

    @Language("SQL")
    private static final String SQL_INSERT =
            "INSERT INTO application_item (application_id, requirement_name, item_type, text_content, document_id) " +
            "VALUES (?, ?, ?, ?, ?)";

    @Language("SQL")
    private static final String SQL_UPDATE_TEXT =
            "UPDATE application_item SET text_content = ? " +
            "WHERE id = ?";

    @Language("SQL")
    private static final String SQL_UPDATE_DOCUMENT =
            "UPDATE application_item SET document_id = ? " +
            "WHERE id = ?";

    @Language("SQL")
    private static final String SQL_FIND_BY_ID =
            "SELECT ai.id, ai.application_id, ai.requirement_name, ai.item_type, " +
            "       ai.text_content, ai.document_id, " +
            "       d.original_filename, d.stored_filename, d.mime_type, " +
            "       d.size_bytes, d.content, d.uploaded_at " +
            "FROM application_item ai " +
            "LEFT JOIN document d ON ai.document_id = d.id " +
            "WHERE ai.id = ?";

    @Language("SQL")
    private static final String SQL_FIND_BY_APPLICATION_ID =
            "SELECT ai.id, ai.application_id, ai.requirement_name, ai.item_type, " +
            "       ai.text_content, ai.document_id, " +
            "       d.original_filename, d.stored_filename, d.mime_type, " +
            "       d.size_bytes, d.content, d.uploaded_at " +
            "FROM application_item ai " +
            "LEFT JOIN document d ON ai.document_id = d.id " +
            "WHERE ai.application_id = ? " +
            "ORDER BY ai.requirement_name";

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    @Override
    public int insert(Connection conn, ApplicationItem item) throws DatabaseException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, item.getApplicationId());
            ps.setString(2, item.getRequirementName());
            ps.setString(3, toDbType(item.getItemType()));

            if (item instanceof TextItem textItem) {
                ps.setString(4, textItem.getTextContent());
                ps.setNull(5, Types.INTEGER);
            } else {
                DocumentItem docItem = (DocumentItem) item;
                ps.setNull(4, Types.VARCHAR);
                ps.setInt(5, docItem.getDocument().getId());
            }

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("No generated ID for the new application item.");
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error inserting application item.", e);
        }
    }

    // ----------------------------------------------------------------
    // update
    // ----------------------------------------------------------------

    @Override
    public void update(Connection conn, ApplicationItem item)
            throws DatabaseException, ApplicationItemNotFoundException {

        String sql = (item.getItemType() == ItemType.TEXT)
                ? SQL_UPDATE_TEXT
                : SQL_UPDATE_DOCUMENT;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            if (item instanceof TextItem textItem) {
                ps.setString(1, textItem.getTextContent());
            } else {
                ps.setInt(1, ((DocumentItem) item).getDocument().getId());
            }
            ps.setInt(2, item.getId());

            if (ps.executeUpdate() == 0) throw new ApplicationItemNotFoundException(item.getId());

        } catch (ApplicationItemNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating application item with id=" + item.getId(), e);
        }
    }

    // ----------------------------------------------------------------
    // findById
    // ----------------------------------------------------------------

    @Override
    public ApplicationItem findById(Connection conn, int id)
            throws DatabaseException, ApplicationItemNotFoundException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ApplicationItemNotFoundException(id);
                return mapItem(rs);
            }
        } catch (ApplicationItemNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving application item with id=" + id, e);
        }
    }

    // ----------------------------------------------------------------
    // findByApplicationId
    // ----------------------------------------------------------------

    @Override
    public List<ApplicationItem> findByApplicationId(Connection conn, int applicationId)
            throws DatabaseException {

        List<ApplicationItem> items = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_APPLICATION_ID)) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(mapItem(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving items for application id=" + applicationId, e);
        }

        return items;
    }

    // ----------------------------------------------------------------
    // Helper privati
    // ----------------------------------------------------------------

    private ApplicationItem mapItem(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int applicationId = rs.getInt("application_id");
        String requirement = rs.getString("requirement_name");
        ItemType type = fromDbType(rs.getString("item_type"));

        if (type == ItemType.TEXT) {
            return new TextItem(id, applicationId, requirement, rs.getString("text_content"));
        }

        Document doc = new Document(
                rs.getInt("document_id"),
                rs.getString("original_filename"),
                rs.getString("stored_filename"),
                rs.getString("mime_type"),
                rs.getLong("size_bytes"),
                rs.getBytes("content"),
                rs.getObject("uploaded_at", LocalDateTime.class)
        );
        return new DocumentItem(id, applicationId, requirement, doc);
    }

    private static String toDbType(ItemType type) {
        return switch (type) {
            case TEXT     -> "Text";
            case DOCUMENT -> "Document";
        };
    }

    private static ItemType fromDbType(String s) {
        return switch (s) {
            case "Text"     -> ItemType.TEXT;
            case "Document" -> ItemType.DOCUMENT;
            default -> throw new IllegalArgumentException("Unknown DB item_type: " + s);
        };
    }
}
