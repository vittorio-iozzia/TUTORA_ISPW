package it.ispw.tutora.dao.factory;

import it.ispw.tutora.dao.*;
import it.ispw.tutora.dao.demo.*;
import it.ispw.tutora.enums.ApplicationStatus;
import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.enums.NotificationType;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.enums.Status;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateApplicationException;
import it.ispw.tutora.exception.DuplicateLessonException;
import it.ispw.tutora.exception.DuplicateTutorExpertiseException;
import it.ispw.tutora.exception.DuplicateUserException;
import it.ispw.tutora.model.*;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    // Costanti per i dati di esempio — evitano literal duplicati
    // ----------------------------------------------------------------

    private static final String CAT_MUSIC       = "Music";
    private static final String CAT_PHOTOGRAPHY = "Photography";
    private static final String CAT_SPORT       = "Sport";
    private static final String REQ_BIOGRAPHY   = "Biography";
    private static final String USER_STUDENT    = "student_luigi";
    private static final String USER_TUTOR      = "tutor_vitto";
    private static final String DEMO_HASH_SEED  = "Demo1234"; // dato di esempio in-memory, non credenziale reale

    // References to Category objects so populateExpertises() can link SubCategories
    private Category musicCategory;
    private Category photographyCategory;
    private Category sportCategory;

    // ----------------------------------------------------------------
    // Istanze condivise dei DAO demo
    // ----------------------------------------------------------------

    private final UserDaoDemo userDao = new UserDaoDemo();
    private final StudentDaoDemo studentDao = new StudentDaoDemo();
    private final TutorDaoDemo tutorDao = new TutorDaoDemo();
    private final CategoryDaoDemo categoryDao = new CategoryDaoDemo();
    private final TutorApplicationDaoDemo tutorApplicationDao = new TutorApplicationDaoDemo();
    private final ApplicationItemDaoDemo applicationItemDao = new ApplicationItemDaoDemo();
    private final DocumentDaoDemo documentDao = new DocumentDaoDemo();
    private final NotificationDaoDemo notificationDao = new NotificationDaoDemo();
    private final LessonDaoDemo lessonDao = new LessonDaoDemo();
    private final BookingDaoDemo bookingDao = new BookingDaoDemo();
    private final TutorExpertiseDaoDemo tutorExpertiseDao = new TutorExpertiseDaoDemo();
    private final ReviewDaoDemo reviewDao = new ReviewDaoDemo();

    // Costruttore package-private: istanziata solo da DaoFactory.loadFactory()
    DemoDaoFactory() {
        try {
            populateCategories();
            populateUsers();
            populateExpertises();
            populateApplications();
            populateNotifications();
            populateLessonsAndBookings();
        } catch (DatabaseException | DuplicateUserException | DuplicateApplicationException
                 | DuplicateTutorExpertiseException | DuplicateLessonException e) {
            throw new IllegalStateException("Failed to populate demo data", e);
        }
    }

    // ----------------------------------------------------------------
    // Popolamento dati di esempio
    // ----------------------------------------------------------------

    private void populateCategories() {

        // Music
        musicCategory = new Category(CAT_MUSIC,
                "Musical instrument lessons and music theory");
        Category music = musicCategory;
        music.addRequirement(new TextRequirement(
                CAT_MUSIC, "bio", REQ_BIOGRAPHY,
                "Describe your musical background and experience",
                true, 50, 800));
        music.addRequirement(new TextRequirement(
                CAT_MUSIC, "subcategory", "Subcategory",
                "Which instrument do you want to teach?",
                true, 2, 100));
        music.addRequirement(new TextRequirement(
                CAT_MUSIC, "teaching_exp", "Teaching experience",
                "Describe your experience as a music teacher",
                false, 0, 600));
        music.addRequirement(new DocumentRequirement(
                CAT_MUSIC, "music_cert", "Diploma / Certificate",
                "Music school diploma or conservatory certificate",
                true));
        music.addRequirement(new DocumentRequirement(
                CAT_MUSIC, "id_document", "Identity document",
                "Valid national ID or passport",
                true));
        categoryDao.add(music);

        // Photography
        photographyCategory = new Category(CAT_PHOTOGRAPHY,
                "Photography technique and post-production");
        Category photography = photographyCategory;
        photography.addRequirement(new TextRequirement(
                CAT_PHOTOGRAPHY, "bio", REQ_BIOGRAPHY,
                "Describe your photography experience",
                true, 50, 800));
        photography.addRequirement(new DocumentRequirement(
                CAT_PHOTOGRAPHY, "portfolio", "Portfolio",
                "Upload a sample of your photographic work",
                true));
        categoryDao.add(photography);

        // Sport
        sportCategory = new Category(CAT_SPORT,
                "Athletic training and sports coaching");
        Category sport = sportCategory;
        sport.addRequirement(new TextRequirement(
                CAT_SPORT, "bio", REQ_BIOGRAPHY,
                "Describe your sports background",
                true, 50, 800));
        sport.addRequirement(new DocumentRequirement(
                CAT_SPORT, "certification", "Sports certification",
                "Upload your coaching or sports certification",
                true));
        categoryDao.add(sport);
    }

    private void populateUsers() throws DatabaseException, DuplicateUserException {

        String hash = BCrypt.hashpw(DEMO_HASH_SEED, BCrypt.gensalt(10));

        // Admin: solo in userDao (non ha una tabella studenti né tutor nel DB reale)
        Admin admin = new Admin.Builder()
                .username("admin")
                .email("admin@tutora.it")
                .name("Alessio")
                .surname("Dainelli")
                .passwordHash(hash)
                .description("System administrator")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        userDao.insert(null, admin);

        // Student: in userDao (per il login) + studentDao (per operazioni student-specific)
        Student student = new Student.Builder()
                .username(USER_STUDENT)
                .email("luigi.verdi@tutora.it")
                .name("Luigi")
                .surname("Verdi")
                .passwordHash(hash)
                .description("Music enthusiast looking for a guitar teacher")
                .active(true)
                .createdAt(LocalDateTime.now())
                .budget(new BigDecimal("200.00"))
                .build();
        userDao.insert(null, student);
        studentDao.insert(null, student);

        // Tutor: in userDao (per il login) + tutorDao (per operazioni tutor-specific)
        Tutor tutor = new Tutor.Builder()
                .username("tutor_vitto")
                .email("vitto.iozzia@tutora.it")
                .name("Vittorio")
                .surname("Iozzia")
                .passwordHash(hash)
                .description("Professional saxophonist with 10 years of teaching experience")
                .active(true)
                .createdAt(LocalDateTime.now())
                .rating(BigDecimal.ZERO)
                .ratingCount(0)
                .build();
        userDao.insert(null, tutor);
        tutorDao.insert(null, tutor);
    }

    private void populateExpertises()
            throws DatabaseException, DuplicateTutorExpertiseException {

        Tutor vitto;
        try {
            vitto = tutorDao.selectTutor(null, USER_TUTOR);
        } catch (Exception e) {
            return; // tutor not found, skip
        }

        LocalDateTime now = LocalDateTime.now();

        // Saxophone – Music (APPROVED)
        SubCategory saxophone = new SubCategory(
                "Saxophone", musicCategory, "Saxophone lessons for all levels");
        tutorExpertiseDao.insertExpertise(null, new TutorExpertise(
                vitto, saxophone, new BigDecimal("35.00"), Status.APPROVED, now));

        // Guitar – Music (APPROVED)
        SubCategory guitar = new SubCategory(
                "Guitar", musicCategory, "Classical and electric guitar");
        tutorExpertiseDao.insertExpertise(null, new TutorExpertise(
                vitto, guitar, new BigDecimal("30.00"), Status.APPROVED, now));

        // Blues Piano – Music (APPROVED)
        SubCategory piano = new SubCategory(
                "Piano", musicCategory, "Piano from beginner to advanced");
        tutorExpertiseDao.insertExpertise(null, new TutorExpertise(
                vitto, piano, new BigDecimal("40.00"), Status.APPROVED, now));
    }

    private void populateApplications() throws DatabaseException, DuplicateApplicationException {

        LocalDateTime submittedAt = LocalDateTime.now().minusDays(2);

        TutorApplication application = new TutorApplication(
                1,
                CAT_MUSIC,
                USER_STUDENT,
                submittedAt,
                ApplicationStatus.SUBMITTED
        );
        tutorApplicationDao.insert(null, application);

        TextItem bioItem = new TextItem(
                0, 1, "bio",
                "I have been playing guitar since I was 12 and studied at the conservatory for 5 years.");
        applicationItemDao.insert(null, bioItem);

        TextItem subcategoryItem = new TextItem(
                0, 1, "subcategory",
                "Guitar");
        applicationItemDao.insert(null, subcategoryItem);

        TextItem teachingExpItem = new TextItem(
                0, 1, "teaching_exp",
                "I have taught guitar to beginners for 3 years at a local music school.");
        applicationItemDao.insert(null, teachingExpItem);
    }

    private void populateLessonsAndBookings() throws DatabaseException, DuplicateLessonException {

        Tutor vitto;
        Student luigi;
        try {
            vitto = tutorDao.selectTutor(null, USER_TUTOR);
            luigi = studentDao.selectStudent(null, USER_STUDENT);
        } catch (Exception e) {
            return;
        }

        // Retrieve TutorExpertise objects already seeded in populateExpertises()
        var expertises = tutorExpertiseDao.findByTutor(null, USER_TUTOR);
        if (expertises.size() < 3) return;

        // findByTutor returns sorted by subcategory name: Guitar[0], Piano[1], Saxophone[2]
        var guitarExpertise = expertises.get(0);
        var pianoExpertise  = expertises.get(1);
        var saxExpertise    = expertises.get(2);

        LocalDateTime now = LocalDateTime.now();

        // ── Upcoming lesson 1: Guitar – in 2 days, BOOKED ──────────────
        Lesson guitarLesson = new Lesson.Builder()
                .expertise(guitarExpertise)
                .startTime(now.plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0))
                .endTime(now.plusDays(2).withHour(11).withMinute(0).withSecond(0).withNano(0))
                .remote(true)
                .listedPrice(new BigDecimal("30.00"))
                .lessonStatus(LessonStatus.BOOKED)
                .createdAt(now.minusDays(3))
                .build();
        lessonDao.insertLesson(null, guitarLesson);

        bookingDao.insertBooking(null, new Booking.Builder()
                .lesson(guitarLesson)
                .student(luigi)
                .bookedAt(now.minusDays(3))
                .pricePaid(new BigDecimal("30.00"))
                .paymentStatus(PaymentStatus.PAID)
                .paymentRef("TXN-GUITAR-001")
                .build());

        // ── Upcoming lesson 2: Saxophone – in 5 days, BOOKED ───────────
        Lesson saxLesson = new Lesson.Builder()
                .expertise(saxExpertise)
                .startTime(now.plusDays(5).withHour(15).withMinute(0).withSecond(0).withNano(0))
                .endTime(now.plusDays(5).withHour(16).withMinute(0).withSecond(0).withNano(0))
                .remote(false)
                .listedPrice(new BigDecimal("35.00"))
                .lessonStatus(LessonStatus.BOOKED)
                .createdAt(now.minusDays(1))
                .build();
        lessonDao.insertLesson(null, saxLesson);

        bookingDao.insertBooking(null, new Booking.Builder()
                .lesson(saxLesson)
                .student(luigi)
                .bookedAt(now.minusDays(1))
                .pricePaid(new BigDecimal("35.00"))
                .paymentStatus(PaymentStatus.PAID)
                .paymentRef("TXN-SAX-002")
                .build());

        // ── Past lesson 1: Piano – 10 days ago, COMPLETED ──────────────
        Lesson pianoLesson = new Lesson.Builder()
                .expertise(pianoExpertise)
                .startTime(now.minusDays(10).withHour(14).withMinute(0).withSecond(0).withNano(0))
                .endTime(now.minusDays(10).withHour(15).withMinute(0).withSecond(0).withNano(0))
                .remote(true)
                .listedPrice(new BigDecimal("40.00"))
                .lessonStatus(LessonStatus.COMPLETED)
                .createdAt(now.minusDays(14))
                .build();
        lessonDao.insertLesson(null, pianoLesson);

        bookingDao.insertBooking(null, new Booking.Builder()
                .lesson(pianoLesson)
                .student(luigi)
                .bookedAt(now.minusDays(14))
                .pricePaid(new BigDecimal("40.00"))
                .paymentStatus(PaymentStatus.PAID)
                .paymentRef("TXN-PIANO-003")
                .build());

        // ── Past lesson 2: Guitar – 3 weeks ago, COMPLETED ─────────────
        Lesson guitarLesson2 = new Lesson.Builder()
                .expertise(guitarExpertise)
                .startTime(now.minusDays(21).withHour(11).withMinute(0).withSecond(0).withNano(0))
                .endTime(now.minusDays(21).withHour(12).withMinute(0).withSecond(0).withNano(0))
                .remote(true)
                .listedPrice(new BigDecimal("30.00"))
                .lessonStatus(LessonStatus.COMPLETED)
                .createdAt(now.minusDays(24))
                .build();
        lessonDao.insertLesson(null, guitarLesson2);

        bookingDao.insertBooking(null, new Booking.Builder()
                .lesson(guitarLesson2)
                .student(luigi)
                .bookedAt(now.minusDays(24))
                .pricePaid(new BigDecimal("30.00"))
                .paymentStatus(PaymentStatus.PAID)
                .paymentRef("TXN-GUITAR-004")
                .build());
    }

    private void populateNotifications() throws DatabaseException {

        Notification notification = new Notification.Builder()
                .recipientUsername(USER_STUDENT)
                .senderUsername(null)
                .message("Your application for Music has been received and is under review.")
                .type(NotificationType.APPLICATION_UPDATE)
                .targetId(1)
                .timestamp(LocalDateTime.now().minusDays(2))
                .read(false)
                .build();
        notificationDao.insert(null, notification);
    }

    // ----------------------------------------------------------------
    // Metodi factory
    // ----------------------------------------------------------------

    @Override
    public UserDao createUserDao() { return userDao; }

    @Override
    public StudentDao createStudentDao() { return studentDao; }

    @Override
    public TutorDao createTutorDao() { return tutorDao; }

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

    @Override
    public LessonDao createLessonDao() { return lessonDao; }

    @Override
    public BookingDao createBookingDao() { return bookingDao; }

    @Override
    public TutorExpertiseDao createTutorExpertiseDao() { return tutorExpertiseDao; }

    @Override
    public ReviewDao createReviewDao() { return reviewDao; }
}