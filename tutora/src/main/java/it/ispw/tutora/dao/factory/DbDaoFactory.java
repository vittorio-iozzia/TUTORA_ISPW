package it.ispw.tutora.dao.factory;

import it.ispw.tutora.dao.*;
import it.ispw.tutora.dao.db.*;

/**
 * Concrete Factory che produce implementazioni DAO MySQL via JDBC.
 * Utilizzata quando DAO_TYPE=DB in app.properties.
 *
 * -----------------------------------------------------------------------
 * Pattern Abstract Factory (GoF – Creazionale)
 * -----------------------------------------------------------------------
 * Ogni metodo create*() istanzia l'implementazione concreta
 * corrispondente nel package dao.db.
 * Il Controller applicativo non conosce mai questa classe —
 * interagisce solo con DaoFactory e le interfacce DAO.
 *
 * Il ciclo di vita è gestito da DaoFactory (Singleton Bill Pugh):
 * questa classe non ha bisogno di essere Singleton autonoma.
 *
 * -----------------------------------------------------------------------
 * Nota su createUserDao()
 * -----------------------------------------------------------------------
 * Restituisce StudentDaoDb perché è la sottoclasse più completa
 * di UserDaoDb e implementa entrambe le interfacce User e Student.
 * Usare createStudentDao() quando servono i metodi Student-specifici.
 *
 * -----------------------------------------------------------------------
 * Nota sui DAO non ancora implementati
 * -----------------------------------------------------------------------
 * I metodi che lanciano UnsupportedOperationException sono placeholder:
 * verranno completati quando le classi *DaoDb corrispondenti
 * saranno disponibili.
 */
public class DbDaoFactory extends DaoFactory {

    // Costruttore package-private: istanziata solo da DaoFactory.loadFactory()
    DbDaoFactory() {}

    // ----------------------------------------------------------------
    // Metodi factory — implementati
    // ----------------------------------------------------------------

    @Override
    public UserDao createUserDao() {
        return new StudentDaoDb();
    }

    @Override
    public StudentDao createStudentDao() {
        return new StudentDaoDb();
    }

    @Override
    public TutorApplicationDao createTutorApplicationDao() {
        return new TutorApplicationDaoDb();
    }

    @Override
    public ApplicationItemDao createApplicationItemDao() {
        return new ApplicationItemDaoDb();
    }

    @Override
    public CategoryDao createCategoryDao() {
        return new CategoryDaoDb();
    }

    @Override
    public DocumentDao createDocumentDao() {
        return new DocumentDaoDb();
    }

    // ----------------------------------------------------------------
    // Metodi factory — placeholder
    // ----------------------------------------------------------------

    @Override
    public TutorDao createTutorDao() {
        return new TutorDaoDb();
    }

    @Override
    public NotificationDao createNotificationDao() {
        return new NotificationDaoDb();
    }

    @Override
    public LessonDao createLessonDao() {
        throw new UnsupportedOperationException("LessonDaoDb not yet implemented.");
    }

    @Override
    public BookingDao createBookingDao() {
        throw new UnsupportedOperationException("BookingDaoDb not yet implemented.");
    }

    @Override
    public TutorExpertiseDao createTutorExpertiseDao() {
        throw new UnsupportedOperationException("TutorExpertiseDaoDb not yet implemented.");
    }

    @Override
    public ReviewDao createReviewDao() {
        throw new UnsupportedOperationException("ReviewDaoDb not yet implemented.");
    }
}