package it.ispw.tutora.dao.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.ispw.tutora.dao.LessonDao;
import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateLessonException;
import it.ispw.tutora.exception.LessonNotFoundException;
import it.ispw.tutora.model.Lesson;
import it.ispw.tutora.model.SubCategory;
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
 * Implementazione JSON di LessonDao.
 *
 * Legge e scrive su lessons.json applicando il pattern read-modify-write
 * ad ogni operazione di scrittura.
 * La Connection viene accettata nelle firme per rispettare l'interfaccia
 * ma non viene usata: non c'è nessun DB.
 *
 * -----------------------------------------------------------------------
 * Nota su toLesson (oggetti parziali)
 * -----------------------------------------------------------------------
 * Il file JSON non conserva tutti i campi della TutorExpertise associata
 * (es. status, tag, createdAt): non sono necessari per la logica delle
 * Lesson. toLesson() costruisce una TutorExpertise parziale con solo
 * tutor, subcategory e hourlyPrice (= listedPrice della lezione).
 * Se serve l'expertise completa, il Controller deve fare un secondo
 * fetch tramite TutorExpertiseDao.
 *
 * -----------------------------------------------------------------------
 * Controllo sovrapposizioni
 * -----------------------------------------------------------------------
 * insertLesson() e updateLesson() verificano che non esista un'altra
 * lezione dello stesso tutor con orario sovrapposto (overlap check).
 * Le lezioni CANCELLED vengono escluse dal controllo.
 *
 * -----------------------------------------------------------------------
 * Nota su insertLesson / updateLesson
 * -----------------------------------------------------------------------
 * L'id viene generato leggendo il massimo id presente nel file e
 * incrementandolo di 1, emulando l'AUTO_INCREMENT del DB.
 * In ambiente multi-thread questo approccio non è thread-safe —
 * accettabile per un'implementazione JSON di sviluppo/test.
 */
public class LessonDaoJson implements LessonDao {

    private static final String JSON_PATH = "../tutora_data/lessons.json";
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String STATUS_CANCELLED = "Cancelled";

    /**
     * Inserisce una nuova lezione nel file JSON dopo aver verificato
     * che non ci siano sovrapposizioni di orario per lo stesso tutor.
     *
     * @throws DuplicateLessonException se esiste già una lezione attiva
     *         dello stesso tutor con orario sovrapposto
     */
    @Override
    public int insertLesson(Connection conn, Lesson newLesson)
            throws DatabaseException, DuplicateLessonException {
        List<LessonRecord> records = readAll();
        for (LessonRecord r : records) {
            if (r.tutorUsername.equals(newLesson.getExpertise().getTutor().getUsername())
                    && !r.status.equals(STATUS_CANCELLED)
                    && LocalDateTime.parse(r.startTime).isBefore(newLesson.getEndTime())
                    && LocalDateTime.parse(r.endTime).isAfter(newLesson.getStartTime())) {
                throw new DuplicateLessonException(newLesson.getId());
            }
        }
        int nextId = records.stream()
                .mapToInt(r -> r.id)
                .max()
                .orElse(0) + 1;
        LessonRecord lessonRecord = toRecord(newLesson);
        lessonRecord.id = nextId;
        records.add(lessonRecord);
        writeAll(records);
        return nextId;
    }

    /**
     * Aggiorna startTime, endTime e isRemote della lezione.
     * Verifica preventivamente l'assenza di sovrapposizioni (escludendo
     * la lezione stessa e quelle CANCELLED).
     *
     * @throws DuplicateLessonException se l'aggiornamento crea un overlap
     * @throws LessonNotFoundException  se nessuna lezione ha quell'id
     */
    @Override
    public void updateLesson(Connection conn, Lesson lesson)
            throws DatabaseException, LessonNotFoundException, DuplicateLessonException {
        List<LessonRecord> records = readAll();
        for (LessonRecord r : records) {
            if (r.id != lesson.getId()
                    && r.tutorUsername.equals(lesson.getExpertise().getTutor().getUsername())
                    && !r.status.equals(STATUS_CANCELLED)
                    && LocalDateTime.parse(r.startTime).isBefore(lesson.getEndTime())
                    && LocalDateTime.parse(r.endTime).isAfter(lesson.getStartTime())) {
                throw new DuplicateLessonException(lesson.getId());
            }
        }
        for (LessonRecord r : records) {
            if (r.id == lesson.getId()) {
                r.startTime = lesson.getStartTime().toString();
                r.endTime = lesson.getEndTime().toString();
                r.isRemote = lesson.isRemote();
                writeAll(records);
                return;
            }
        }
        throw new LessonNotFoundException(lesson.getId());
    }

    /**
     * Aggiorna il LessonStatus della lezione con l'id dato.
     *
     * @throws LessonNotFoundException se nessuna lezione ha quell'id
     */
    @Override
    public void updateStatus(Connection conn, int id, LessonStatus newStatus)
            throws DatabaseException, LessonNotFoundException {
        List<LessonRecord> records = readAll();
        for (LessonRecord r : records) {
            if (r.id == id) {
                r.status = toJsonStatus(newStatus);
                writeAll(records);
                return;
            }
        }
        throw new LessonNotFoundException(id);
    }

    /**
     * Carica una lezione per id.
     *
     * @throws LessonNotFoundException se nessuna lezione ha quell'id
     */
    @Override
    public Lesson selectLesson(Connection conn, int id)
            throws DatabaseException, LessonNotFoundException {
        for (LessonRecord r : readAll()) {
            if (r.id == id) return toLesson(r);
        }
        throw new LessonNotFoundException(id);
    }

    /**
     * Restituisce tutte le lezioni del tutor.
     * Lista vuota se il tutor non ha lezioni registrate.
     */
    @Override
    public List<Lesson> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException {
        List<Lesson> list = new ArrayList<>();
        for (LessonRecord r : readAll()) {
            if (r.tutorUsername.equals(tutorUsername))
                list.add(toLesson(r));
        }
        return list;
    }

    /**
     * Restituisce le lezioni del tutor filtrate per status.
     * Lista vuota se non esistono lezioni con quel status.
     */
    @Override
    public List<Lesson> findByTutorAndStatus(Connection conn,
                                             String tutorUsername,
                                             LessonStatus status)
            throws DatabaseException {
        List<Lesson> list = new ArrayList<>();
        for (LessonRecord r : readAll()) {
            if (r.tutorUsername.equals(tutorUsername)
                    && r.status.equals(toJsonStatus(status)))
                list.add(toLesson(r));
        }
        return list;
    }

    // ----------------------------------------------------------------
    // Mapping record ↔ model
    // ----------------------------------------------------------------

    /**
     * Ricostruisce un oggetto Lesson da un record JSON.
     * Costruisce una TutorExpertise parziale (tutor + subcategory +
     * hourlyPrice) sufficiente per la logica delle lezioni.
     * Si usa listedPrice come valore di hourlyPrice dell'expertise parziale.
     */
    private Lesson toLesson(LessonRecord r) {
        Tutor partialTutor = new Tutor.Builder()
                .username(r.tutorUsername)
                .build();
        SubCategory partialSubCategory = new SubCategory(r.subcategoryName, null, null);
        TutorExpertise partialExpertise = new TutorExpertise(
                partialTutor, partialSubCategory,
                new BigDecimal(r.listedPrice),
                null, null);
        return new Lesson.Builder()
                .id(r.id)
                .expertise(partialExpertise)
                .startTime(LocalDateTime.parse(r.startTime))
                .endTime(LocalDateTime.parse(r.endTime))
                .remote(r.isRemote)
                .listedPrice(new BigDecimal(r.listedPrice))
                .lessonStatus(fromJsonStatus(r.status))
                .createdAt(LocalDateTime.parse(r.createdAt))
                .build();
    }

    /** Converte un oggetto Lesson nel corrispondente record JSON. */
    private LessonRecord toRecord(Lesson lesson) {
        LessonRecord lessonRecord = new LessonRecord();
        lessonRecord.id = lesson.getId();
        lessonRecord.tutorUsername = lesson.getExpertise().getTutor().getUsername();
        lessonRecord.subcategoryName = lesson.getExpertise().getSubcategory().getName();
        lessonRecord.startTime = lesson.getStartTime().toString();
        lessonRecord.endTime = lesson.getEndTime().toString();
        lessonRecord.isRemote = lesson.isRemote();
        lessonRecord.listedPrice = lesson.getListedPrice().toPlainString();
        lessonRecord.status = toJsonStatus(lesson.getLessonStatus());
        lessonRecord.createdAt = lesson.getCreatedAt() != null
                ? lesson.getCreatedAt().toString()
                : LocalDateTime.now().toString();
        return lessonRecord;
    }

    // ----------------------------------------------------------------
    // POJO interno per la serializzazione Jackson
    // ----------------------------------------------------------------
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private static class LessonRecord {
        int id;
        String tutorUsername;
        String subcategoryName;
        String startTime;
        String endTime;
        boolean isRemote;
        String listedPrice;
        String status;
        String createdAt;
    }

    // ----------------------------------------------------------------
    // Conversione LessonStatus ↔ stringa JSON
    // ----------------------------------------------------------------

    private static String toJsonStatus(LessonStatus status) {
        return switch (status) {
            case AVAILABLE -> "Available";
            case BOOKED    -> "Booked";
            case COMPLETED -> "Completed";
            case CANCELLED -> STATUS_CANCELLED;
        };
    }

    private static LessonStatus fromJsonStatus(String s) {
        return switch (s) {
            case "Available" -> LessonStatus.AVAILABLE;
            case "Booked"    -> LessonStatus.BOOKED;
            case "Completed" -> LessonStatus.COMPLETED;
            case STATUS_CANCELLED -> LessonStatus.CANCELLED;
            default -> throw new IllegalArgumentException("Unknown lesson status: " + s);
        };
    }

    // ----------------------------------------------------------------
    // I/O JSON
    // ----------------------------------------------------------------

    private List<LessonRecord> readAll() throws DatabaseException {
        try {
            LessonRecord[] records = mapper.readValue(
                    new File(JSON_PATH), LessonRecord[].class);
            return new ArrayList<>(Arrays.asList(records));
        } catch (IOException e) {
            throw new DatabaseException("Error reading JSON file: " + JSON_PATH, e);
        }
    }

    private void writeAll(List<LessonRecord> records) throws DatabaseException {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(JSON_PATH), records);
        } catch (IOException e) {
            throw new DatabaseException("Error writing JSON file: " + JSON_PATH, e);
        }
    }
}
