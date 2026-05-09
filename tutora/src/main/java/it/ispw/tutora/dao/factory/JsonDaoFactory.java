package it.ispw.tutora.dao.factory;

import it.ispw.tutora.dao.*;

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
 * Nessun *DaoJson è ancora implementato. Tutti i metodi lanciano
 * UnsupportedOperationException come placeholder.
 * TODO: implementare le classi *DaoJson nel package dao.json.
 */
public class JsonDaoFactory extends DaoFactory {

    // Costruttore package-private: istanziata solo da DaoFactory.loadFactory()
    JsonDaoFactory() {}

    // ----------------------------------------------------------------
    // Metodi factory — tutti placeholder
    // ----------------------------------------------------------------

    @Override
    public UserDao createUserDao() {
        throw new UnsupportedOperationException("UserDaoJson not yet implemented.");
    }

    @Override
    public StudentDao createStudentDao() {
        throw new UnsupportedOperationException("StudentDaoJson not yet implemented.");
    }

    @Override
    public TutorDao createTutorDao() {
        throw new UnsupportedOperationException("TutorDaoJson not yet implemented.");
    }

    @Override
    public CategoryDao createCategoryDao() {
        throw new UnsupportedOperationException("CategoryDaoJson not yet implemented.");
    }

    @Override
    public NotificationDao createNotificationDao() {
        throw new UnsupportedOperationException("NotificationDaoJson not yet implemented.");
    }

    @Override
    public TutorApplicationDao createTutorApplicationDao() {
        throw new UnsupportedOperationException("TutorApplicationDaoJson not yet implemented.");
    }

    @Override
    public ApplicationItemDao createApplicationItemDao() {
        throw new UnsupportedOperationException("ApplicationItemDaoJson not yet implemented.");
    }

    @Override
    public DocumentDao createDocumentDao() {
        throw new UnsupportedOperationException("DocumentDaoJson not yet implemented.");
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
        throw new UnsupportedOperationException("ReviewDaoJson not yet implemented.");
    }
}