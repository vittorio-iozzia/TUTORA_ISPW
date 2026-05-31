package it.ispw.tutora.dao.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.ispw.tutora.dao.TutorApplicationDao;
import it.ispw.tutora.enums.ApplicationStatus;
import it.ispw.tutora.exception.ApplicationNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateApplicationException;
import it.ispw.tutora.model.TutorApplication;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Implementazione di TutorApplicationDao basata su file JSON.
 * Usata quando DAO_TYPE=JSON in app.properties.
 *
 * -----------------------------------------------------------------------
 * Persistenza
 * -----------------------------------------------------------------------
 * Legge e scrive su ../tutora_data/tutor_applications.json.
 * Ogni operazione di scrittura (insert, updateStatus) applica il pattern
 * read-modify-write: legge l'intero file, modifica la lista in memoria,
 * riscrive il file aggiornato.
 *
 * -----------------------------------------------------------------------
 * Nota sul parametro Connection
 * -----------------------------------------------------------------------
 * Il parametro Connection viene ignorato: non c'è nessun DB.
 * È presente solo per rispettare il contratto dell'interfaccia TutorApplicationDao,
 * pensata per la gestione delle transazioni JDBC.
 *
 * -----------------------------------------------------------------------
 * LocalDateTime
 * -----------------------------------------------------------------------
 * Le date vengono serializzate come stringhe ISO-8601
 * (es. "2026-05-10T09:00:00") e ricostruite con LocalDateTime.parse().
 */
public class TutorApplicationDaoJson implements TutorApplicationDao {

    private static final String JSON_PATH = "../tutora_data/tutor_applications.json";
    private final ObjectMapper mapper = new ObjectMapper();

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    @Override
    public int insert(Connection conn, TutorApplication application)
            throws DatabaseException, DuplicateApplicationException {

        List<AppRecord> records = readAll();

        String newSub = normalizeSubcategory(application.getSubcategoryName());
        for (AppRecord r : records) {
            if (r.studentUsername.equals(application.getStudentUsername())
                    && r.categoryName.equals(application.getCategoryName())
                    && Objects.equals(normalizeSubcategory(r.subcategoryName), newSub)
                    && !ApplicationStatus.valueOf(r.status).isTerminal()) {
                throw new DuplicateApplicationException(
                        application.getStudentUsername(), application.getCategoryName());
            }
        }

        // Simulo in Json un ID auto-incremented
        int id = records.stream().mapToInt(r -> r.id).max().orElse(0) + 1;

        AppRecord newRecord = new AppRecord();
        newRecord.id = id;
        newRecord.categoryName = application.getCategoryName();
        newRecord.subcategoryName = application.getSubcategoryName();
        newRecord.studentUsername = application.getStudentUsername();
        newRecord.creationDate = application.getCreationDate().toString();
        newRecord.status = application.getStatus().name();
        newRecord.adminNotes = application.getAdminNotes();
        newRecord.evaluatedAt = application.getEvaluatedAt() != null
                ? application.getEvaluatedAt().toString() : null;

        records.add(newRecord);
        writeAll(records);
        return id;
    }

    // ----------------------------------------------------------------
    // updateStatus
    // ----------------------------------------------------------------

    @Override
    public void updateStatus(Connection conn, TutorApplication application)
            throws DatabaseException, ApplicationNotFoundException {

        List<AppRecord> records = readAll();
        for (AppRecord r : records) {
            if (r.id == application.getId()) {
                r.status = application.getStatus().name();
                r.adminNotes = application.getAdminNotes();
                r.evaluatedAt = application.getEvaluatedAt() != null
                        ? application.getEvaluatedAt().toString()
                        : null;
                writeAll(records);
                return;
            }
        }
        throw new ApplicationNotFoundException(application.getId());
    }

    // ----------------------------------------------------------------
    // findById
    // ----------------------------------------------------------------

    @Override
    public TutorApplication findById(Connection conn, int id)
            throws DatabaseException, ApplicationNotFoundException {

        for (AppRecord r : readAll()) {
            if (r.id == id) return toApplication(r);
        }
        throw new ApplicationNotFoundException(id);
    }

    // ----------------------------------------------------------------
    // findByStudent
    // ----------------------------------------------------------------

    @Override
    public List<TutorApplication> findByStudent(Connection conn, String studentUsername)
            throws DatabaseException {

        List<TutorApplication> result = new ArrayList<>();
        for (AppRecord r : readAll()) {
            if (r.studentUsername.equals(studentUsername)) result.add(toApplication(r));
        }
        return result;
    }

    // ----------------------------------------------------------------
    // findByStatus
    // ----------------------------------------------------------------

    @Override
    public List<TutorApplication> findByStatus(Connection conn, ApplicationStatus status)
            throws DatabaseException {

        List<TutorApplication> result = new ArrayList<>();
        for (AppRecord r : readAll()) {
            if (ApplicationStatus.valueOf(r.status) == status) result.add(toApplication(r));
        }
        return result;
    }

    // ----------------------------------------------------------------
    // Helper privati
    // ----------------------------------------------------------------

    private List<AppRecord> readAll() throws DatabaseException {
        try {
            AppRecord[] records = mapper.readValue(new File(JSON_PATH), AppRecord[].class);
            return new ArrayList<>(Arrays.asList(records));
        } catch (IOException e) {
            throw new DatabaseException("Error reading JSON file: " + JSON_PATH, e);
        }
    }

    private void writeAll(List<AppRecord> records) throws DatabaseException {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(JSON_PATH), records);
        } catch (IOException e) {
            throw new DatabaseException("Error writing JSON file: " + JSON_PATH, e);
        }
    }

    private TutorApplication toApplication(AppRecord r) {
        TutorApplication app = new TutorApplication(
                r.id,
                r.categoryName,
                r.studentUsername,
                LocalDateTime.parse(r.creationDate),
                ApplicationStatus.valueOf(r.status));
        app.setAdminNotes(r.adminNotes);
        if (r.evaluatedAt != null) app.setEvaluatedAt(LocalDateTime.parse(r.evaluatedAt));
        return app;
    }

    // ----------------------------------------------------------------
    // POJO interno per la deserializzazione Jackson
    // ----------------------------------------------------------------

    private static String normalizeSubcategory(String s) {
        if (s == null) return null;
        return s.trim().toLowerCase().replaceAll("\\s+", "");
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static class AppRecord {
        int id;
        String categoryName;
        String subcategoryName;
        String studentUsername;
        String creationDate;
        String status;
        String adminNotes;
        String evaluatedAt;
    }
}
