package it.ispw.tutora.dao.factory;

import it.ispw.tutora.dao.*;
import it.ispw.tutora.dao.json.ApplicationItemDaoJson;
import it.ispw.tutora.dao.json.CategoryDaoJson;
import it.ispw.tutora.dao.json.DocumentDaoJson;
import it.ispw.tutora.dao.json.NotificationDaoJson;
import it.ispw.tutora.dao.json.ReviewDaoJson;
import it.ispw.tutora.dao.json.TutorApplicationDaoJson;
import it.ispw.tutora.dao.json.TutorDaoJson;
import it.ispw.tutora.dao.json.UserDaoJson;

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
 * -----------------------------------------------------------------------
 * Stato attuale
 * -----------------------------------------------------------------------
 * DAO implementati: Category, TutorApplication, Notification,
 * ApplicationItem, Document, Review, User, Tutor.
 * DAO ancora placeholder: Student, Lesson, Booking, TutorExpertise.
 */
public class JsonDaoFactory extends DaoFactory {

    // Costruttore package-private: istanziata solo da DaoFactory.loadFactory()
    JsonDaoFactory() {}

    // ----------------------------------------------------------------
    // Metodi factory — tutti placeholder
    // ----------------------------------------------------------------

    @Override
    public UserDao createUserDao() {
        return new UserDaoJson();
    }

    @Override
    public StudentDao createStudentDao() {
        throw new UnsupportedOperationException("StudentDaoJson not yet implemented.");
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
        throw new UnsupportedOperationException("LessonDaoJson not yet implemented.");
    }

    @Override
    public BookingDao createBookingDao() {
        throw new UnsupportedOperationException("BookingDaoJson not yet implemented.");
    }

    @Override
    public TutorExpertiseDao createTutorExpertiseDao() {
        throw new UnsupportedOperationException("TutorExpertiseDaoJson not yet implemented.");
    }

    @Override
    public ReviewDao createReviewDao() {
        return new ReviewDaoJson();
    }
}