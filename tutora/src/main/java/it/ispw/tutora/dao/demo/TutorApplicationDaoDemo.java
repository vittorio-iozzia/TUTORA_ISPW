package it.ispw.tutora.dao.demo;

import it.ispw.tutora.dao.TutorApplicationDao;
import it.ispw.tutora.enums.ApplicationStatus;
import it.ispw.tutora.exception.ApplicationNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateApplicationException;
import it.ispw.tutora.model.TutorApplication;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Implementazione in memoria di TutorApplicationDao.
 * Usata al posto di TutorApplicationDaoDb quando il DB non è disponibile.
 *
 * -----------------------------------------------------------------------
 * Nota sulla gestione degli id
 * -----------------------------------------------------------------------
 * insert() costruisce un nuovo TutorApplication con l'id generato da nextId,
 * quindi findById() e updateStatus() possono cercare correttamente per id.
 *
 * -----------------------------------------------------------------------
 * Nota
 * -----------------------------------------------------------------------
 * Il parametro Connection viene ignorato — non c'è nessun DB.
 * I dati vivono in memoria per tutta la durata dell'esecuzione
 * e spariscono alla chiusura dell'applicazione.
 * Il ciclo di vita è gestito da DemoDaoFactory — non serve Singleton.
 */
public class TutorApplicationDaoDemo implements TutorApplicationDao {

    private final List<TutorApplication> cache = new ArrayList<>();
    private int nextId = 1;

    // ----------------------------------------------------------------
    // insert
    // ----------------------------------------------------------------

    @Override
    public int insert(Connection conn, TutorApplication application)
            throws DatabaseException, DuplicateApplicationException {
        for (TutorApplication a : cache) {
            if (a.getStudentUsername().equals(application.getStudentUsername())
                    && a.getCategoryName().equals(application.getCategoryName())
                    && Objects.equals(a.getSubcategoryName(), application.getSubcategoryName())
                    && !a.getStatus().isTerminal()) {
                throw new DuplicateApplicationException(
                        application.getStudentUsername(), application.getCategoryName());
            }
        }
        int id = nextId++;
        TutorApplication stored = new TutorApplication(
                id,
                application.getCategoryName(),
                application.getStudentUsername(),
                application.getCreationDate(),
                application.getStatus());
        stored.setSubcategoryName(application.getSubcategoryName());
        cache.add(stored);
        return id;
    }

    // ----------------------------------------------------------------
    // updateStatus
    // ----------------------------------------------------------------

    @Override
    public void updateStatus(Connection conn, TutorApplication application)
            throws DatabaseException, ApplicationNotFoundException {
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).getId() == application.getId()) {
                cache.set(i, application);
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
        for (TutorApplication a : cache) {
            if (a.getId() == id) return a;
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
        for (TutorApplication a : cache) {
            if (a.getStudentUsername().equals(studentUsername)) result.add(a);
        }
        result.sort(Comparator.comparing(TutorApplication::getCreationDate).reversed());
        return result;
    }

    // ----------------------------------------------------------------
    // findByStatus
    // ----------------------------------------------------------------

    @Override
    public List<TutorApplication> findByStatus(Connection conn, ApplicationStatus status)
            throws DatabaseException {
        List<TutorApplication> result = new ArrayList<>();
        for (TutorApplication a : cache) {
            if (a.getStatus() == status) result.add(a);
        }
        result.sort(Comparator.comparing(TutorApplication::getCreationDate).reversed());
        return result;
    }
}
