package it.ispw.tutora.dao.factory;

import it.ispw.tutora.dao.*;
import it.ispw.tutora.dao.demo.MessageDaoDemo;
import it.ispw.tutora.dao.json.*;

/**
 * Concrete Factory che produce implementazioni DAO basate su file JSON.
 * Utilizzata quando DAO_TYPE=JSON in app.properties.
 *
 * -----------------------------------------------------------------------
 * Pattern Abstract Factory (GoF – Creazionale)
 * -----------------------------------------------------------------------
 * Pensata per scenari di testing o ambienti senza MySQL (es. CI).
 * Ogni metodo create*() restituirà l'implementazione *DaoJson
 * corrispondente quando disponibile.
 *
 * Il ciclo di vita è gestito da DaoFactory (Singleton Bill Pugh):
 * questa classe non ha bisogno di essere Singleton autonoma.
 *
 */
public class JsonDaoFactory extends DaoFactory {

    // Costruttore package-private: istanziata solo da DaoFactory.loadFactory()
    JsonDaoFactory() {}

    // ----------------------------------------------------------------
    // Metodi factory
    // ----------------------------------------------------------------

    @Override
    public UserDao createUserDao() {
        return new UserDaoJson();
    }

    @Override
    public StudentDao createStudentDao() {
        return new StudentDaoJson();
    }

    @Override
    public TutorDao createTutorDao() {
        return new TutorDaoJson();
    }

    @Override
    public CategoryDao createCategoryDao() {
        return new CategoryDaoJson();
    }

    @Override
    public NotificationDao createNotificationDao() {
        return new NotificationDaoJson();
    }

    @Override
    public TutorApplicationDao createTutorApplicationDao() {
        return new TutorApplicationDaoJson();
    }

    @Override
    public ApplicationItemDao createApplicationItemDao() {
        return new ApplicationItemDaoJson();
    }

    @Override
    public DocumentDao createDocumentDao() {
        return new DocumentDaoJson();
    }

    @Override
    public LessonDao createLessonDao() {
        return new LessonDaoJson();
    }

    @Override
    public BookingDao createBookingDao() {
        return new BookingDaoJson();
    }

    @Override
    public TutorExpertiseDao createTutorExpertiseDao() {
        return new TutorExpertiseDaoJson();
    }

    @Override
    public ReviewDao createReviewDao() { return new ReviewDaoJson(); }

    @Override
    public MessageDao createMessageDao() { return new MessageDaoDemo(); }

    @Override
    public boolean isNewlyPromotedTutor(String username) {
        try {
            return new UserDaoJson().isNewlyPromoted(username);
        } catch (it.ispw.tutora.exception.DatabaseException e) {
            return false;
        }
    }

    @Override
    public void clearNewlyPromotedTutor(String username) {
        try {
            new UserDaoJson().clearNewlyPromoted(username);
        } catch (it.ispw.tutora.exception.DatabaseException ignored) {
            // best-effort
        }
    }
}