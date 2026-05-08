package it.ispw.tutora.dao.db;

import it.ispw.tutora.dao.DocumentDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DocumentNotFoundException;
import it.ispw.tutora.model.Document;
import org.intellij.lang.annotations.Language;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * Implementazione JDBC di DocumentDao.
 *
 * -----------------------------------------------------------------------
 * Gestione delle transazioni
 * -----------------------------------------------------------------------
 * Tutti i metodi ricevono la Connection come parametro: commit e rollback
 * sono gestiti dal Controller applicativo, non dal DAO.
 * Un Document viene sempre inserito nella stessa transazione che crea
 * il DocumentItem che lo referenzia.
 *
 * -----------------------------------------------------------------------
 * Vincolo FK RESTRICT
 * -----------------------------------------------------------------------
 * La colonna document_id in application_item è definita con
 * ON DELETE RESTRICT: un Document non può essere eliminato finché
 * almeno un ApplicationItem lo referenzia. Il Controller deve quindi
 * aggiornare o eliminare l'item prima di invocare delete().
 */
public class DocumentDaoDb implements DocumentDao {

    @Language("SQL")
    private static final String SQL_INSERT =
            "INSERT INTO document (original_filename, stored_filename, mime_type, size_bytes, content) " +
            "VALUES (?, ?, ?, ?, ?)";

    @Language("SQL")
    private static final String SQL_FIND_BY_ID =
            "SELECT id, original_filename, stored_filename, mime_type, size_bytes, content, uploaded_at " +
            "FROM document " +
            "WHERE id = ?";

    @Language("SQL")
    private static final String SQL_DELETE =
            "DELETE FROM document " +
            "WHERE id = ?";

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    @Override
    public int insert(Connection conn, Document document) throws DatabaseException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, document.getOriginalFilename());
            ps.setString(2, document.getStoredFilename());
            ps.setString(3, document.getMimeType());
            ps.setLong(4, document.getSizeBytes());
            ps.setBytes(5, document.getContent());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DatabaseException("No generated ID for the new document.");
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error inserting document.", e);
        }
    }

    // ----------------------------------------------------------------
    // findById
    // ----------------------------------------------------------------

    @Override
    public Document findById(Connection conn, int id)
            throws DatabaseException, DocumentNotFoundException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new DocumentNotFoundException(id);
                return mapDocument(rs);
            }
        } catch (DocumentNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving document with id=" + id, e);
        }
    }

    // ----------------------------------------------------------------
    // delete
    // ----------------------------------------------------------------

    @Override
    public void delete(Connection conn, int id)
            throws DatabaseException, DocumentNotFoundException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) throw new DocumentNotFoundException(id);
        } catch (DocumentNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting document with id=" + id, e);
        }
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private Document mapDocument(ResultSet rs) throws SQLException {
        return new Document(
                rs.getInt("id"),
                rs.getString("original_filename"),
                rs.getString("stored_filename"),
                rs.getString("mime_type"),
                rs.getLong("size_bytes"),
                rs.getBytes("content"),
                rs.getObject("uploaded_at", LocalDateTime.class)
        );
    }
}
