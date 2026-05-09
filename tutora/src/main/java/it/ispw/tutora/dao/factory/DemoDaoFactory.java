package it.ispw.tutora.dao.factory;

import it.ispw.tutora.dao.*;
import it.ispw.tutora.dao.demo.*;
import it.ispw.tutora.model.Category;
import it.ispw.tutora.model.DocumentRequirement;
import it.ispw.tutora.model.TextRequirement;

/**
 * Concrete Factory che produce implementazioni in memoria (demo).
 * Utilizzata quando DAO_TYPE=DEMO in app.properties.
 *
 * -----------------------------------------------------------------------
 * Pattern Abstract Factory (GoF – Creazionale)
 * -----------------------------------------------------------------------
 * Produce una famiglia coerente di DAO demo — tutti condividono
 * le stesse Map interne tramite questa factory.
 *
 * Il ciclo di vita è gestito da DaoFactory (Singleton Bill Pugh):
 * questa classe non ha bisogno di essere Singleton autonoma.
 *
 * -----------------------------------------------------------------------
 * Ciclo di vita dei DAO demo
 * -----------------------------------------------------------------------
 * Le istanze dei DAO sono campi di questa factory e vivono per tutta
 * la durata dell'applicazione. I DAO demo non devono essere Singleton
 * autonomi: la loro unicità è garantita da questa factory.
 *
 * -----------------------------------------------------------------------
 * Dati di esempio
 * -----------------------------------------------------------------------
 * Il costruttore popola i DAO con dati hardcodati realistici
 * per permettere il testing della GUI senza database.
 */
public class DemoDaoFactory extends DaoFactory {

    // ----------------------------------------------------------------
    // Istanze condivise dei DAO demo
    // ----------------------------------------------------------------

    private final UserDaoDemo             userDao             = new UserDaoDemo();
    private final TutorDaoDemo             tutorDao            = new TutorDaoDemo();
    private final CategoryDaoDemo          categoryDao         = new CategoryDaoDemo();
    private final TutorApplicationDaoDemo  tutorApplicationDao = new TutorApplicationDaoDemo();
    private final ApplicationItemDaoDemo   applicationItemDao  = new ApplicationItemDaoDemo();
    private final DocumentDaoDemo          documentDao         = new DocumentDaoDemo();
    private final NotificationDaoDemo      notificationDao     = new NotificationDaoDemo();

    // Costruttore package-private: istanziata solo da DaoFactory.loadFactory()
    DemoDaoFactory() {
        populateCategories();
    }

    // ----------------------------------------------------------------
    // Popolamento dati di esempio
    // ----------------------------------------------------------------

    private void populateCategories() {

        // Music
        Category music = new Category("Music",
                "Musical instrument lessons and music theory");
        music.addRequirement(new TextRequirement(
                "Music", "bio", "Biography",
                "Describe your musical background and experience",
                true, 50, 800));
        music.addRequirement(new TextRequirement(
                "Music", "subcategory", "Subcategory",
                "Which instrument do you want to teach?",
                true, 2, 100));
        music.addRequirement(new TextRequirement(
                "Music", "teaching_exp", "Teaching experience",
                "Describe your experience as a music teacher",
                false, 0, 600));
        music.addRequirement(new DocumentRequirement(
                "Music", "music_cert", "Diploma / Certificate",
                "Music school diploma or conservatory certificate",
                true));
        music.addRequirement(new DocumentRequirement(
                "Music", "id_document", "Identity document",
                "Valid national ID or passport",
                true));
        categoryDao.add(music);

        // Photography
        Category photography = new Category("Photography",
                "Photography technique and post-production");
        photography.addRequirement(new TextRequirement(
                "Photography", "bio", "Biography",
                "Describe your photography experience",
                true, 50, 800));
        photography.addRequirement(new DocumentRequirement(
                "Photography", "portfolio", "Portfolio",
                "Upload a sample of your photographic work",
                true));
        categoryDao.add(photography);

        // Sport
        Category sport = new Category("Sport",
                "Athletic training and sports coaching");
        sport.addRequirement(new TextRequirement(
                "Sport", "bio", "Biography",
                "Describe your sports background",
                true, 50, 800));
        sport.addRequirement(new DocumentRequirement(
                "Sport", "certification", "Sports certification",
                "Upload your coaching or sports certification",
                true));
        categoryDao.add(sport);
    }

    // ----------------------------------------------------------------
    // Metodi factory — implementati
    // ----------------------------------------------------------------

    @Override
    public UserDao createUserDao() { return userDao; }

    @Override
    public CategoryDao createCategoryDao() { return categoryDao; }

    @Override
    public TutorApplicationDao createTutorApplicationDao() { return tutorApplicationDao; }

    @Override
    public ApplicationItemDao createApplicationItemDao() { return applicationItemDao; }

    @Override
    public DocumentDao createDocumentDao() { return documentDao; }

    @Override
    public NotificationDao createNotificationDao() { return notificationDao; }

    // ----------------------------------------------------------------
    // Metodi factory — placeholder
    // ----------------------------------------------------------------

    @Override
    public StudentDao createStudentDao() {
        throw new UnsupportedOperationException(
                "StudentDaoDemo not yet implemented.");
    }

    @Override
    public TutorDao createTutorDao() { return tutorDao; }

    @Override
    public LessonDao createLessonDao() {
        throw new UnsupportedOperationException(
                "LessonDaoDemo not yet implemented.");
    }

    @Override
    public BookingDao createBookingDao() {
        throw new UnsupportedOperationException(
                "BookingDaoDemo not yet implemented.");
    }

    @Override
    public TutorExpertiseDao createTutorExpertiseDao() {
        throw new UnsupportedOperationException(
                "TutorExpertiseDaoDemo not yet implemented.");
    }

    @Override
    public ReviewDao createReviewDao() {
        throw new UnsupportedOperationException(
                "ReviewDaoDemo not yet implemented.");
    }
}