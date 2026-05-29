package it.ispw.tutora.controller.application;

import it.ispw.tutora.bean.LessonTutorBean;
import it.ispw.tutora.dao.LessonDao;
import java.math.BigDecimal;
import it.ispw.tutora.dao.TutorExpertiseDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.enums.Status;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateLessonException;
import it.ispw.tutora.model.Lesson;
import it.ispw.tutora.model.TutorExpertise;
import it.ispw.tutora.model.session.SessionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class CreateLessonController {

    private final LessonDao lessonDao;
    private final TutorExpertiseDao expertiseDao;

    public CreateLessonController() {
        DaoFactory factory = DaoFactory.getInstance();
        this.lessonDao    = factory.createLessonDao();
        this.expertiseDao = factory.createTutorExpertiseDao();
    }

    /**
     * Valida i dati di una lezione prima della creazione.
     * Restituisce un messaggio di errore se i dati non sono validi, null se tutto è corretto.
     *
     * Regole di dominio:
     *   - Il prezzo deve essere positivo (> 0)
     *   - L'orario di fine deve essere successivo all'orario di inizio
     *
     * Spostato da CreateLessonGfxController per rispettare BCE:
     * queste sono regole di business, non di presentazione.
     */
    public String validateLessonInput(LessonTutorBean bean) {
        if (bean.getListedPrice() == null
                || bean.getListedPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return "Invalid price. Enter a positive number (e.g. 35.00).";
        }
        if (bean.getStartTime() == null || bean.getEndTime() == null
                || !bean.getEndTime().isAfter(bean.getStartTime())) {
            return "End time must be after start time.";
        }
        return null;
    }

    public List<TutorExpertise> loadApprovedExpertises(String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) return Collections.emptyList();
        String username = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            return expertiseDao.findByTutor(conn, username).stream()
                    .filter(e -> e.getStatus() == Status.APPROVED)
                    .toList();
        } catch (DatabaseException | SQLException e) {
            return Collections.emptyList();
        }
    }

    public void createLesson(LessonTutorBean bean, String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) {
            bean.setErrorMessage("Sessione non valida.");
            return;
        }
        String username = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            TutorExpertise expertise = expertiseDao.findByTutor(conn, username).stream()
                    .filter(e -> e.getStatus() == Status.APPROVED
                            && e.getSubcategory().getName().equals(bean.getSubcategoryName()))
                    .findFirst()
                    .orElse(null);

            if (expertise == null) {
                bean.setErrorMessage("Materia non trovata o non approvata.");
                return;
            }

            Lesson lesson = new Lesson.Builder()
                    .expertise(expertise)
                    .startTime(bean.getStartTime())
                    .endTime(bean.getEndTime())
                    .remote(bean.isRemote())
                    .listedPrice(bean.getListedPrice())
                    .lessonStatus(LessonStatus.AVAILABLE)
                    .createdAt(LocalDateTime.now())
                    .build();

            lessonDao.insertLesson(conn, lesson);

        } catch (DuplicateLessonException e) {
            bean.setErrorMessage("You already have a lesson scheduled in this time slot.");
        } catch (DatabaseException | SQLException e) {
            bean.setErrorMessage("Errore durante la creazione della lezione.");
        } catch (IllegalArgumentException e) {
            bean.setErrorMessage(e.getMessage());
        }
    }
}
