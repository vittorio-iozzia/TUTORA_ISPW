package it.ispw.tutora.dao.demo;

import it.ispw.tutora.dao.LessonDao;
import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateLessonException;
import it.ispw.tutora.exception.LessonNotFoundException;
import it.ispw.tutora.model.Lesson;

import java.sql.Connection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementazione in memoria di LessonDao.
 * Usata al posto di LessonDaoDb quando il DB non è disponibile.
 *
 * -----------------------------------------------------------------------
 * Nota sulla cache
 * -----------------------------------------------------------------------
 * I dati vivono in una HashMap con id come chiave.
 * Il parametro Connection viene ignorato ovunque — non c'è nessun DB.
 * Il ciclo di vita è gestito da DemoDaoFactory — non serve Singleton.
 *
 * -----------------------------------------------------------------------
 * Nota sull'AUTO_INCREMENT
 * -----------------------------------------------------------------------
 * nextId simula il comportamento di AUTO_INCREMENT del DB.
 * Parte da 1 e viene incrementato ad ogni INSERT.
 *
 * -----------------------------------------------------------------------
 * Nota sull'anti-overlap
 * -----------------------------------------------------------------------
 * insertLesson e updateLesson verificano la sovrapposizione temporale
 * scorrendo la cache con un for-each, simulando la SELECT COUNT(*)
 * del LessonDaoDb. Le lezioni CANCELLED vengono escluse dal controllo
 * perché non occupano più slot.
 *
 * -----------------------------------------------------------------------
 * Nota su updateStatus
 * -----------------------------------------------------------------------
 * lessonStatus non è final in Lesson — ha il metodo updateLessonStatus()
 * che implementa la macchina a stati finiti e notifica i listener Observer.
 * Non serve ricostruire l'oggetto con il Builder: si modifica direttamente
 * l'oggetto in cache tramite updateLessonStatus().
 */
public class LessonDaoDemo implements LessonDao {

    // HashMap con id come chiave — simula la tabella lesson
    private final Map<Integer, Lesson> cache = new HashMap<>();

    // Simula AUTO_INCREMENT del DB — parte da 1
    private int nextId = 1;

    // ----------------------------------------------------------------
    // insertLesson
    // ----------------------------------------------------------------

    /**
     * Inserisce una nuova lezione assegnando un id progressivo.
     * Prima dell'INSERT verifica che il tutor non abbia già lezioni
     * attive sovrapposte all'intervallo richiesto.
     *
     * @return id assegnato alla nuova lezione
     * @throws DuplicateLessonException se c'è sovrapposizione temporale
     *         con un'altra lezione attiva dello stesso tutor
     */
    @Override
    public int insertLesson(Connection conn, Lesson newLesson)
            throws DatabaseException, DuplicateLessonException {

        // Controllo anti-overlap: scorre tutte le lezioni del tutor
        // e verifica che il nuovo intervallo non si sovrapponga.
        // Due intervalli si sovrappongono se: start_A < end_B AND end_A > start_B.
        // Le lezioni CANCELLED vengono ignorate: non occupano più slot.
        for (Lesson lesson : cache.values()) {
            if (lesson.getExpertise().getTutor().getUsername()
                    .equals(newLesson.getExpertise().getTutor().getUsername())
                    && lesson.getLessonStatus() != LessonStatus.CANCELLED
                    && lesson.getStartTime().isBefore(newLesson.getEndTime())
                    && lesson.getEndTime().isAfter(newLesson.getStartTime())) {
                throw new DuplicateLessonException(newLesson.getId());
            }
        }
        // Ricostruisce la lezione con l'id auto-assegnato, come fa DocumentDaoDemo:
        // id è final → non modificabile dopo la costruzione, va impostato nel Builder.
        int id = nextId++;
        Lesson stored = new Lesson.Builder()
                .id(id)
                .expertise(newLesson.getExpertise())
                .startTime(newLesson.getStartTime())
                .endTime(newLesson.getEndTime())
                .remote(newLesson.isRemote())
                .listedPrice(newLesson.getListedPrice())
                .lessonStatus(newLesson.getLessonStatus())
                .createdAt(newLesson.getCreatedAt())
                .build();
        cache.put(id, stored);
        return id;
    }

    // ----------------------------------------------------------------
    // updateLesson
    // ----------------------------------------------------------------

    /**
     * Aggiorna una lezione esistente (orario e modalità).
     * Prima dell'UPDATE verifica la sovrapposizione temporale
     * escludendo la lezione stessa dal controllo (stesso id).
     *
     * @throws LessonNotFoundException  se l'id non esiste in cache
     * @throws DuplicateLessonException se il nuovo orario si sovrappone
     *         a un'altra lezione attiva dello stesso tutor
     */
    @Override
    public void updateLesson(Connection conn, Lesson lesson)
            throws DatabaseException, LessonNotFoundException, DuplicateLessonException {

        if (!cache.containsKey(lesson.getId()))
            throw new LessonNotFoundException(lesson.getId());

        // Controllo anti-overlap: esclude la lezione stessa con id != lesson.getId()
        // per evitare che si auto-sovrapponga con il proprio vecchio intervallo
        for (Lesson existing : cache.values()) {
            if (existing.getId() != lesson.getId()
                    && existing.getExpertise().getTutor().getUsername()
                    .equals(lesson.getExpertise().getTutor().getUsername())
                    && existing.getLessonStatus() != LessonStatus.CANCELLED
                    && existing.getStartTime().isBefore(lesson.getEndTime())
                    && existing.getEndTime().isAfter(lesson.getStartTime())) {
                throw new DuplicateLessonException(lesson.getId());
            }
        }

        // Sovrascrive la lezione nella cache con i nuovi valori
        cache.put(lesson.getId(), lesson);
    }

    // ----------------------------------------------------------------
    // updateStatus
    // ----------------------------------------------------------------

    /**
     * Aggiorna lo status di una lezione.
     * lessonStatus non è final in Lesson: si usa updateLessonStatus()
     * che implementa la macchina a stati finiti e notifica i listener.
     * Non serve ricostruire l'oggetto — la modifica avviene direttamente
     * sull'oggetto in cache.
     *
     * @throws LessonNotFoundException se l'id non esiste in cache
     */
    @Override
    public void updateStatus(Connection conn, int id, LessonStatus newStatus)
            throws DatabaseException, LessonNotFoundException {

        if (!cache.containsKey(id)) throw new LessonNotFoundException(id);
        Lesson lesson = cache.get(id);
        // Guard: se lo status è già quello richiesto, non chiamare updateLessonStatus()
        // di nuovo. In Demo mode respondToRequest() aggiorna già l'oggetto in memoria
        // prima che lessonDao.updateStatus() venga chiamato → senza questo guard la FSM
        // riceverebbe due volte la stessa transizione e lancerebbe IllegalArgumentException.
        if (lesson.getLessonStatus() == newStatus) return;
        // Modifica diretta sull'oggetto in cache — la HashMap vede il cambiamento
        // perché contiene il riferimento all'oggetto, non una copia
        lesson.updateLessonStatus(newStatus);
    }

    // ----------------------------------------------------------------
    // selectLesson
    // ----------------------------------------------------------------

    /**
     * Carica una lezione per id.
     *
     * @throws LessonNotFoundException se l'id non esiste in cache
     */
    @Override
    public Lesson selectLesson(Connection conn, int id)
            throws DatabaseException, LessonNotFoundException {

        if (!cache.containsKey(id)) throw new LessonNotFoundException(id);
        return cache.get(id);
    }

    // ----------------------------------------------------------------
    // findByTutor
    // ----------------------------------------------------------------

    /**
     * Carica tutte le lezioni di un tutor ordinate per data crescente.
     * Restituisce una lista vuota se il tutor non ha lezioni —
     * l'assenza di lezioni non è un errore ma un caso legittimo.
     */
    @Override
    public List<Lesson> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException {

        return cache.values().stream()
                .filter(l -> l.getExpertise().getTutor().getUsername().equals(tutorUsername))
                .sorted(Comparator.comparing(Lesson::getStartTime))
                .toList();
    }

    // ----------------------------------------------------------------
    // findByTutorAndStatus
    // ----------------------------------------------------------------

    /**
     * Carica le lezioni di un tutor filtrate per status, ordinate per
     * data crescente. Restituisce una lista vuota se non ce ne sono.
     */
    @Override
    public List<Lesson> findByTutorAndStatus(Connection conn, String tutorUsername,
                                             LessonStatus status)
            throws DatabaseException {

        return cache.values().stream()
                .filter(l -> l.getExpertise().getTutor().getUsername().equals(tutorUsername)
                        && l.getLessonStatus().equals(status))
                .sorted(Comparator.comparing(Lesson::getStartTime))
                .toList();
    }
}
