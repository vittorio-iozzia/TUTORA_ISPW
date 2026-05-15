package it.ispw.tutora.dao.demo;

import it.ispw.tutora.dao.TutorExpertiseDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateTutorExpertiseException;
import it.ispw.tutora.exception.TutorExpertiseNotFoundException;
import it.ispw.tutora.model.TutorExpertise;

import java.sql.Connection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementazione in-memory di TutorExpertiseDao per l'ambiente demo.
 *
 * Sostituisce il DB con una Map<String, TutorExpertise> (cache) che vive
 * per tutta la durata della sessione applicativa.
 * La chiave composita (tutorUsername + "_" + subcategoryName) emula
 * la PRIMARY KEY (tutor_username, subcategory_name) della tabella DB.
 * La Connection viene accettata nelle firme per rispettare l'interfaccia
 * ma non viene usata: in demo non esiste una transazione reale.
 */
public class TutorExpertiseDaoDemo implements TutorExpertiseDao {

    private final Map<String, TutorExpertise> cache = new HashMap<>();

    /** Costruisce la chiave composita che emula la PK del DB. */
    private String makeKey(String tutorUsername, String subcategoryName) {
        return tutorUsername + "_" + subcategoryName;
    }

    /**
     * Inserisce la competenza in cache.
     *
     * @throws DuplicateTutorExpertiseException se la coppia (tutor, subcategory)
     *         è già presente — emula il UNIQUE constraint del DB
     */
    @Override
    public void insertExpertise(Connection conn, TutorExpertise expertise)
            throws DatabaseException, DuplicateTutorExpertiseException {
        if (cache.containsKey(makeKey(expertise.getTutor().getUsername(),
                expertise.getSubcategory().getName())))
            throw new DuplicateTutorExpertiseException("Tutor Expertise already present.");
        cache.put(makeKey(expertise.getTutor().getUsername(),
                expertise.getSubcategory().getName()), expertise);
    }

    /**
     * Aggiorna la competenza in cache (sostituisce il valore esistente).
     *
     * @throws TutorExpertiseNotFoundException se la coppia (tutor, subcategory)
     *         non è presente in cache
     */
    @Override
    public void updateExpertise(Connection conn, TutorExpertise expertise)
            throws DatabaseException, TutorExpertiseNotFoundException {
        if (!cache.containsKey(makeKey(expertise.getTutor().getUsername(),
                expertise.getSubcategory().getName())))
            throw new TutorExpertiseNotFoundException("Tutor Expertise not found.");
        cache.put(makeKey(expertise.getTutor().getUsername(),
                expertise.getSubcategory().getName()), expertise);
    }

    /**
     * Restituisce tutte le competenze del tutor ordinate per nome subcategory.
     * Lista vuota se il tutor non ha competenze registrate.
     */
    @Override
    public List<TutorExpertise> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException {
        return cache.values().stream()
                .filter(tutorExpertise ->
                        tutorExpertise.getTutor().getUsername().equals(tutorUsername))
                .sorted(Comparator.comparing(te -> te.getSubcategory().getName()))
                .toList();
    }

    /**
     * Carica una singola competenza per chiave composita (tutor, subcategory).
     *
     * @throws TutorExpertiseNotFoundException se la coppia non è in cache
     */
    @Override
    public TutorExpertise selectExpertise(Connection conn,
                                          String tutorUsername,
                                          String subcategoryName)
            throws DatabaseException, TutorExpertiseNotFoundException {
        if (!cache.containsKey(makeKey(tutorUsername, subcategoryName)))
            throw new TutorExpertiseNotFoundException("Tutor Expertise not found.");
        return cache.get(makeKey(tutorUsername, subcategoryName));
    }

    /**
     * Aggiorna solo lo status della competenza in cache.
     * La macchina a stati finiti è applicata da TutorExpertise.updateStatus().
     *
     * Non rimpiazza l'intero oggetto (come farebbe updateExpertise) ma agisce
     * esclusivamente sul campo status dell'oggetto già in cache, coerentemente
     * con l'implementazione JSON che scrive solo r.status.
     *
     * Guard: se lo status è già quello richiesto il metodo torna subito.
     * In Demo mode il controller ha già aggiornato l'oggetto in RAM tramite
     * updateStatus() (stesso riferimento restituito da selectExpertise),
     * quindi cached.getStatus() == tutorExpertise.getStatus() → return immediato,
     * senza chiamare updateStatus() una seconda volta sulla FSM.
     *
     * @throws TutorExpertiseNotFoundException se la coppia non è in cache
     */
    @Override
    public void updateExpertiseStatus(Connection conn, TutorExpertise tutorExpertise)
            throws DatabaseException, TutorExpertiseNotFoundException {
        String key = makeKey(tutorExpertise.getTutor().getUsername(),
                tutorExpertise.getSubcategory().getName());
        if (!cache.containsKey(key))
            throw new TutorExpertiseNotFoundException("Tutor Expertise not found.");
        TutorExpertise cached = cache.get(key);
        // Guard: se lo status è già quello richiesto non chiamo updateStatus() di nuovo.
        // In Demo mode il controller ha già aggiornato l'oggetto in RAM (stesso riferimento),
        // quindi cached.getStatus() == tutorExpertise.getStatus() → return immediato.
        if (cached.getStatus() == tutorExpertise.getStatus()) return;
        cached.updateStatus(tutorExpertise.getStatus());
    }
}
