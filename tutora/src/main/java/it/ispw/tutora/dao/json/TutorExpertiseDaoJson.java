package it.ispw.tutora.dao.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.ispw.tutora.dao.TutorExpertiseDao;
import it.ispw.tutora.enums.Status;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateTutorExpertiseException;
import it.ispw.tutora.exception.TutorExpertiseNotFoundException;
import it.ispw.tutora.model.SubCategory;
import it.ispw.tutora.model.Tag;
import it.ispw.tutora.model.Tutor;
import it.ispw.tutora.model.TutorExpertise;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementazione JSON di TutorExpertiseDao.
 *
 * Legge e scrive su tutorExpertises.json applicando il pattern
 * read-modify-write ad ogni operazione di scrittura.
 * La Connection viene accettata nelle firme per rispettare l'interfaccia
 * ma non viene usata: non c'è nessun DB.
 *
 * -----------------------------------------------------------------------
 * Struttura del file JSON
 * -----------------------------------------------------------------------
 * Ogni record contiene: tutorUsername, subcategoryName, hourlyPrice,
 * status, createdAt e tags (lista di stringhe).
 * La coppia (tutorUsername, subcategoryName) funge da chiave logica,
 * emulando la PRIMARY KEY composita della tabella DB.
 *
 * -----------------------------------------------------------------------
 * Nota su toTutorExpertise
 * -----------------------------------------------------------------------
 * I tag vengono ripristinati aggiungendoli uno ad uno tramite addTag()
 * dopo la costruzione dell'oggetto TutorExpertise.
 */
public class TutorExpertiseDaoJson implements TutorExpertiseDao {

    private static final String JSON_PATH = "../tutora_data/tutorExpertises.json";
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Inserisce una nuova expertise nel file JSON.
     *
     * @throws DuplicateTutorExpertiseException se la coppia
     *         (tutorUsername, subcategoryName) è già presente
     */
    @Override
    public void insertExpertise(Connection conn, TutorExpertise expertise)
            throws DatabaseException, DuplicateTutorExpertiseException {
        List<TutorExpertiseRecord> list = readAll();
        for (TutorExpertiseRecord r : list) {
            if (r.tutorUsername.equals(expertise.getTutor().getUsername())
                    && r.subcategoryName.equals(expertise.getSubcategory().getName()))
                throw new DuplicateTutorExpertiseException("Expertise already present.");
        }
        list.add(toRecord(expertise));
        writeAll(list);
    }

    /**
     * Aggiorna hourlyPrice e tag dell'expertise nel file JSON.
     *
     * @throws TutorExpertiseNotFoundException se la coppia
     *         (tutorUsername, subcategoryName) non è presente
     */
    @Override
    public void updateExpertise(Connection conn, TutorExpertise expertise)
            throws DatabaseException, TutorExpertiseNotFoundException {
        List<TutorExpertiseRecord> list = readAll();
        for (TutorExpertiseRecord r : list) {
            if (r.tutorUsername.equals(expertise.getTutor().getUsername())
                    && r.subcategoryName.equals(expertise.getSubcategory().getName())) {
                r.hourlyPrice = expertise.getHourlyPrice().toPlainString();
                r.tags = expertise.getExpertiseTags().stream()
                        .map(Tag::getName)
                        .toList();
                writeAll(list);
                return;
            }
        }
        throw new TutorExpertiseNotFoundException("Tutor Expertise not found.");
    }

    /**
     * Restituisce tutte le expertise del tutor.
     * Lista vuota se il tutor non ha expertise registrate.
     */
    @Override
    public List<TutorExpertise> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException {
        List<TutorExpertise> expertises = new ArrayList<>();
        for (TutorExpertiseRecord r : readAll()) {
            if (r.tutorUsername.equals(tutorUsername))
                expertises.add(toTutorExpertise(r));
        }
        return expertises;
    }

    /**
     * Carica una singola expertise per chiave composita.
     *
     * @throws TutorExpertiseNotFoundException se la coppia non è presente
     */
    @Override
    public TutorExpertise selectExpertise(Connection conn,
                                          String tutorUsername,
                                          String subcategoryName)
            throws DatabaseException, TutorExpertiseNotFoundException {
        for (TutorExpertiseRecord r : readAll()) {
            if (r.tutorUsername.equals(tutorUsername)
                    && r.subcategoryName.equals(subcategoryName))
                return toTutorExpertise(r);
        }
        throw new TutorExpertiseNotFoundException("Tutor Expertise not found.");
    }

    /**
     * Aggiorna solo il campo status dell'expertise nel file JSON.
     *
     * @throws TutorExpertiseNotFoundException se la coppia non è presente
     */
    @Override
    public void updateExpertiseStatus(Connection conn, TutorExpertise tutorExpertise)
            throws DatabaseException, TutorExpertiseNotFoundException {
        List<TutorExpertiseRecord> list = readAll();
        for (TutorExpertiseRecord r : list) {
            if (r.tutorUsername.equals(tutorExpertise.getTutor().getUsername())
                    && r.subcategoryName.equals(tutorExpertise.getSubcategory().getName())) {
                r.status = toJsonStatus(tutorExpertise.getStatus());
                writeAll(list);
                return;
            }
        }
        throw new TutorExpertiseNotFoundException("Tutor Expertise not found.");
    }

    // ----------------------------------------------------------------
    // Mapping record ↔ model
    // ----------------------------------------------------------------

    /**
     * Ricostruisce un oggetto TutorExpertise da un record JSON,
     * ripristinando anche la lista dei tag.
     * FIX: r.tags non veniva letto — l'oggetto tornava senza tag.
     */
    private TutorExpertise toTutorExpertise(TutorExpertiseRecord r) {
        TutorExpertise tutorExpertise = new TutorExpertise(
                new Tutor.Builder().username(r.tutorUsername).build(),
                new SubCategory(r.subcategoryName, null, null),
                new BigDecimal(r.hourlyPrice),
                fromJsonStatus(r.status),
                LocalDateTime.parse(r.createdAt));
        if (r.tags != null) {
            for (String tagName : r.tags)
                tutorExpertise.addTag(new Tag(tagName));
        }
        return tutorExpertise;
    }

    /** Converte un oggetto TutorExpertise nel corrispondente record JSON. */
    private TutorExpertiseRecord toRecord(TutorExpertise t) {
        TutorExpertiseRecord r = new TutorExpertiseRecord();
        r.tutorUsername = t.getTutor().getUsername();
        r.subcategoryName = t.getSubcategory().getName();
        r.hourlyPrice = t.getHourlyPrice().toPlainString();
        r.status = toJsonStatus(t.getStatus());
        r.createdAt = t.getCreatedAt().toString();
        r.tags = t.getExpertiseTags().stream()
                .map(Tag::getName)
                .toList();
        return r;
    }

    // ----------------------------------------------------------------
    // POJO interno per la serializzazione Jackson
    // ----------------------------------------------------------------
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static class TutorExpertiseRecord {
        String tutorUsername;
        String subcategoryName;
        String hourlyPrice;
        String status;
        String createdAt;
        List<String> tags;
    }

    // ----------------------------------------------------------------
    // Conversione Status ↔ stringa JSON
    // ----------------------------------------------------------------

    private static String toJsonStatus(Status status) {
        return switch (status) {
            case PENDING  -> "Pending";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
        };
    }

    private static Status fromJsonStatus(String s) {
        return switch (s) {
            case "Pending"  -> Status.PENDING;
            case "Approved" -> Status.APPROVED;
            case "Rejected" -> Status.REJECTED;
            default -> throw new IllegalArgumentException("Unknown status: " + s);
        };
    }

    // ----------------------------------------------------------------
    // I/O JSON
    // ----------------------------------------------------------------

    private List<TutorExpertiseRecord> readAll() throws DatabaseException {
        try {
            TutorExpertiseRecord[] records = mapper.readValue(
                    new File(JSON_PATH), TutorExpertiseRecord[].class);
            return new ArrayList<>(Arrays.asList(records));
        } catch (IOException e) {
            throw new DatabaseException("Error reading JSON file: " + JSON_PATH, e);
        }
    }

    private void writeAll(List<TutorExpertiseRecord> records) throws DatabaseException {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(JSON_PATH), records);
        } catch (IOException e) {
            throw new DatabaseException("Error writing JSON file: " + JSON_PATH, e);
        }
    }
}
