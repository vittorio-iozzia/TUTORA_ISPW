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
import it.ispw.tutora.exception.DuplicateReviewException;
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

    /** In modalità demo i dati sono in-memory: nessuna persistenza su disco. */
    @Override
    public boolean isDemo() { return true; }

    // ----------------------------------------------------------------
    // Costanti per i dati di esempio — evitano literal duplicati
    // ----------------------------------------------------------------

    private static final String CAT_MUSIC       = "Music";
    private static final String CAT_PHOTOGRAPHY = "Photography";
    private static final String CAT_SPORT       = "Sport";
    private static final String REQ_BIOGRAPHY   = "Biography";
    private static final String USER_STUDENT    = "student_luigi";
    private static final String USER_TUTOR      = "tutor_vitto";
    private static final String USER_TUTOR_MARCO = "tutor_marco";
    private static final String USER_TUTOR_SARA  = "tutor_sara";
    private static final String USER_TUTOR_LUCA  = "tutor_luca";
    private static final String DEMO_HASH_SEED  = "Demo1234"; // dato di esempio in-memory, non credenziale reale
    private static final String PRICE_40        = "40.00";
    private static final String PRICE_35        = "35.00";
    private static final String PRICE_30        = "30.00";
    private static final String PRICE_25        = "25.00";
    private static final String PRICE_45        = "45.00";
    private static final String PRICE_50        = "50.00";
    private static final String REQ_SUBCATEGORY   = "subcategory";
    private static final String LABEL_SUBCATEGORY = "Subcategory";
    private static final String SUBCAT_GUITAR     = "Guitar";

    // References to Category objects so populateExpertises() can link SubCategories
    private Category musicCategory;
    private Category photographyCategory;
    private Category sportCategory;

    // References kept for cross-populate use (e.g. seeding reviews)
    private Tutor   tutorVitto;
    private Student studentLuigi;

    // ----------------------------------------------------------------
    // Istanze condivise dei DAO demo
    // ----------------------------------------------------------------

    private final TutorDaoDemo tutorDao = new TutorDaoDemo();
    // userDao wraps the base impl; promoteToTutor() also registers the new Tutor
    // into tutorDao.cache so selectAllTutors() finds promoted students immediately.
    private final UserDaoDemo userDao = new UserDaoDemo() {
        @Override
        public it.ispw.tutora.model.Tutor promoteToTutor(java.sql.Connection conn, String studentUsername)
                throws it.ispw.tutora.exception.DatabaseException,
                       it.ispw.tutora.exception.UserNotFoundException {
            it.ispw.tutora.model.Tutor t = super.promoteToTutor(conn, studentUsername);
            try { tutorDao.insert(conn, t); }
            catch (it.ispw.tutora.exception.DuplicateUserException ignored) { /* already there */ }
            return t;
        }
    };
    private final StudentDaoDemo studentDao = new StudentDaoDemo(userDao);
    private final CategoryDaoDemo categoryDao = new CategoryDaoDemo();
    private final TutorApplicationDaoDemo tutorApplicationDao = new TutorApplicationDaoDemo();
    private final ApplicationItemDaoDemo applicationItemDao = new ApplicationItemDaoDemo();
    private final DocumentDaoDemo documentDao = new DocumentDaoDemo();
    private final NotificationDaoDemo notificationDao = new NotificationDaoDemo();
    private final LessonDaoDemo lessonDao = new LessonDaoDemo();
    private final BookingDaoDemo bookingDao = new BookingDaoDemo();
    private final TutorExpertiseDaoDemo tutorExpertiseDao = new TutorExpertiseDaoDemo();
    private final ReviewDaoDemo reviewDao = new ReviewDaoDemo();
    private final MessageDaoDemo messageDao = new MessageDaoDemo();

    // Costruttore package-private: istanziata solo da DaoFactory.loadFactory()
    DemoDaoFactory() {
        try {
            populateCategories();
            populateUsers();
            populateExpertises();
            populateApplications();
            int[] availableIds = populateLessonsAndBookings();
            populateNotifications(availableIds);
            populateMessages();
            populateReviews();
        } catch (DatabaseException | DuplicateUserException | DuplicateApplicationException
                 | DuplicateTutorExpertiseException | DuplicateLessonException
                 | DuplicateReviewException e) {
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
                CAT_MUSIC, REQ_SUBCATEGORY, LABEL_SUBCATEGORY,
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
        photography.addRequirement(new TextRequirement(
                CAT_PHOTOGRAPHY, REQ_SUBCATEGORY, LABEL_SUBCATEGORY,
                "What specific area do you want to teach? (e.g., Portrait, Landscape, Videomaking)",
                true, 2, 100));
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
        sport.addRequirement(new TextRequirement(
                CAT_SPORT, REQ_SUBCATEGORY, LABEL_SUBCATEGORY,
                "What sport do you want to teach? (e.g., Tennis, Swimming, Football)",
                true, 2, 100));
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

        // Student: studentDao.insert() scrive in entrambe le cache (userDao + studentDao)
        studentLuigi = new Student.Builder()
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
        studentDao.insert(null, studentLuigi);

        // Tutor: in userDao (per il login) + tutorDao (per operazioni tutor-specific)
        tutorVitto = new Tutor.Builder()
                .username(USER_TUTOR)
                .email("vitto.iozzia@tutora.it")
                .name("Vittorio")
                .surname("Iozzia")
                .passwordHash(hash)
                .description("Professional saxophonist with 10 years of teaching experience")
                .active(true)
                .createdAt(LocalDateTime.now())
                .rating(new BigDecimal("4.8"))
                .ratingCount(24)
                .build();
        userDao.insert(null, tutorVitto);
        tutorDao.insert(null, tutorVitto);

        Tutor marco = new Tutor.Builder()
                .username(USER_TUTOR_MARCO)
                .email("marco.rossi@tutora.it")
                .name("Marco")
                .surname("Rossi")
                .passwordHash(hash)
                .description("Classical and electric guitar teacher with conservatory degree")
                .active(true)
                .createdAt(LocalDateTime.now().minusDays(30))
                .rating(new BigDecimal("4.6"))
                .ratingCount(18)
                .build();
        userDao.insert(null, marco);
        tutorDao.insert(null, marco);

        Tutor sara = new Tutor.Builder()
                .username(USER_TUTOR_SARA)
                .email("sara.bianchi@tutora.it")
                .name("Sara")
                .surname("Bianchi")
                .passwordHash(hash)
                .description("Portrait and landscape photographer, 8 years of professional experience")
                .active(true)
                .createdAt(LocalDateTime.now().minusDays(60))
                .rating(new BigDecimal("4.9"))
                .ratingCount(31)
                .build();
        userDao.insert(null, sara);
        tutorDao.insert(null, sara);

        Tutor luca = new Tutor.Builder()
                .username(USER_TUTOR_LUCA)
                .email("luca.ferrari@tutora.it")
                .name("Luca")
                .surname("Ferrari")
                .passwordHash(hash)
                .description("Certified tennis coach, former regional champion")
                .active(true)
                .createdAt(LocalDateTime.now().minusDays(15))
                .rating(new BigDecimal("4.7"))
                .ratingCount(12)
                .build();
        userDao.insert(null, luca);
        tutorDao.insert(null, luca);
    }

    private void populateExpertises()
            throws DatabaseException, DuplicateTutorExpertiseException {

        LocalDateTime now = LocalDateTime.now();

        // ── tutor_vitto: Music ──────────────────────────────────────────
        Tutor vitto;
        try { vitto = tutorDao.selectTutor(null, USER_TUTOR); }
        catch (Exception e) { return; }

        SubCategory saxophone = new SubCategory(
                "Saxophone", musicCategory, "Saxophone lessons for all levels");
        tutorExpertiseDao.insertExpertise(null, new TutorExpertise(
                vitto, saxophone, new BigDecimal(PRICE_35), Status.APPROVED, now));

        SubCategory guitarVitto = new SubCategory(
                SUBCAT_GUITAR, musicCategory, "Classical and electric guitar");
        tutorExpertiseDao.insertExpertise(null, new TutorExpertise(
                vitto, guitarVitto, new BigDecimal(PRICE_30), Status.APPROVED, now));

        SubCategory piano = new SubCategory(
                "Piano", musicCategory, "Piano from beginner to advanced");
        tutorExpertiseDao.insertExpertise(null, new TutorExpertise(
                vitto, piano, new BigDecimal(PRICE_40), Status.APPROVED, now));

        // ── tutor_marco: Guitar ─────────────────────────────────────────
        Tutor marco;
        try { marco = tutorDao.selectTutor(null, USER_TUTOR_MARCO); }
        catch (Exception e) { return; }

        SubCategory guitarMarco = new SubCategory(
                SUBCAT_GUITAR, musicCategory, "Classical and rock guitar");
        tutorExpertiseDao.insertExpertise(null, new TutorExpertise(
                marco, guitarMarco, new BigDecimal(PRICE_25), Status.APPROVED, now.minusDays(30)));

        SubCategory ukulele = new SubCategory(
                "Ukulele", musicCategory, "Ukulele for beginners");
        tutorExpertiseDao.insertExpertise(null, new TutorExpertise(
                marco, ukulele, new BigDecimal(PRICE_25), Status.APPROVED, now.minusDays(30)));

        // ── tutor_sara: Photography ─────────────────────────────────────
        Tutor sara;
        try { sara = tutorDao.selectTutor(null, USER_TUTOR_SARA); }
        catch (Exception e) { return; }

        SubCategory portrait = new SubCategory(
                "Portrait", photographyCategory, "Portrait photography techniques");
        tutorExpertiseDao.insertExpertise(null, new TutorExpertise(
                sara, portrait, new BigDecimal(PRICE_45), Status.APPROVED, now.minusDays(60)));

        SubCategory landscape = new SubCategory(
                "Landscape", photographyCategory, "Landscape and nature photography");
        tutorExpertiseDao.insertExpertise(null, new TutorExpertise(
                sara, landscape, new BigDecimal(PRICE_40), Status.APPROVED, now.minusDays(60)));

        // ── tutor_luca: Sport ───────────────────────────────────────────
        Tutor luca;
        try { luca = tutorDao.selectTutor(null, USER_TUTOR_LUCA); }
        catch (Exception e) { return; }

        SubCategory tennis = new SubCategory(
                "Tennis", sportCategory, "Tennis coaching for all levels");
        tutorExpertiseDao.insertExpertise(null, new TutorExpertise(
                luca, tennis, new BigDecimal(PRICE_50), Status.APPROVED, now.minusDays(15)));

        SubCategory padel = new SubCategory(
                "Padel", sportCategory, "Padel technique and tactics");
        tutorExpertiseDao.insertExpertise(null, new TutorExpertise(
                luca, padel, new BigDecimal(PRICE_40), Status.APPROVED, now.minusDays(15)));
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
        application.setSubcategoryName(SUBCAT_GUITAR);
        tutorApplicationDao.insert(null, application);

        TextItem bioItem = new TextItem(
                0, 1, "bio",
                "I have been playing guitar since I was 12 and studied at the conservatory for 5 years.");
        applicationItemDao.insert(null, bioItem);

        TextItem subcategoryItem = new TextItem(
                0, 1, REQ_SUBCATEGORY,
                SUBCAT_GUITAR);
        applicationItemDao.insert(null, subcategoryItem);

        TextItem teachingExpItem = new TextItem(
                0, 1, "teaching_exp",
                "I have taught guitar to beginners for 3 years at a local music school.");
        applicationItemDao.insert(null, teachingExpItem);
    }

    private int[] populateLessonsAndBookings() throws DatabaseException, DuplicateLessonException {

        Student luigi;
        try {
            luigi = studentDao.selectStudent(null, USER_STUDENT);
        } catch (Exception e) {
            return new int[0];
        }

        // Retrieve TutorExpertise objects already seeded in populateExpertises()
        var expertises = tutorExpertiseDao.findByTutor(null, USER_TUTOR);
        if (expertises.size() < 3) return new int[0];

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
                .listedPrice(new BigDecimal(PRICE_30))
                .lessonStatus(LessonStatus.BOOKED)
                .createdAt(now.minusDays(3))
                .build();
        int guitarLessonId = lessonDao.insertLesson(null, guitarLesson);

        bookingDao.insertBooking(null, new Booking.Builder()
                .lesson(guitarLesson)
                .student(luigi)
                .bookedAt(now.minusDays(3))
                .pricePaid(new BigDecimal(PRICE_30))
                .paymentStatus(PaymentStatus.PAID)
                .paymentRef("TXN-GUITAR-001")
                .build());

        // ── Upcoming lesson 2: Saxophone – in 5 days, BOOKED ───────────
        Lesson saxLesson = new Lesson.Builder()
                .expertise(saxExpertise)
                .startTime(now.plusDays(5).withHour(15).withMinute(0).withSecond(0).withNano(0))
                .endTime(now.plusDays(5).withHour(16).withMinute(0).withSecond(0).withNano(0))
                .remote(false)
                .listedPrice(new BigDecimal(PRICE_35))
                .lessonStatus(LessonStatus.BOOKED)
                .createdAt(now.minusDays(1))
                .build();
        int saxLessonId = lessonDao.insertLesson(null, saxLesson);

        bookingDao.insertBooking(null, new Booking.Builder()
                .lesson(saxLesson)
                .student(luigi)
                .bookedAt(now.minusDays(1))
                .pricePaid(new BigDecimal(PRICE_35))
                .paymentStatus(PaymentStatus.PAID)
                .paymentRef("TXN-SAX-002")
                .build());

        // ── Past lesson 1: Piano – 10 days ago, COMPLETED ──────────────
        Lesson pianoLesson = new Lesson.Builder()
                .expertise(pianoExpertise)
                .startTime(now.minusDays(10).withHour(14).withMinute(0).withSecond(0).withNano(0))
                .endTime(now.minusDays(10).withHour(15).withMinute(0).withSecond(0).withNano(0))
                .remote(true)
                .listedPrice(new BigDecimal(PRICE_40))
                .lessonStatus(LessonStatus.COMPLETED)
                .createdAt(now.minusDays(14))
                .build();
        lessonDao.insertLesson(null, pianoLesson);

        bookingDao.insertBooking(null, new Booking.Builder()
                .lesson(pianoLesson)
                .student(luigi)
                .bookedAt(now.minusDays(14))
                .pricePaid(new BigDecimal(PRICE_40))
                .paymentStatus(PaymentStatus.PAID)
                .paymentRef("TXN-PIANO-003")
                .build());

        // ── Past lesson 2: Guitar – 3 weeks ago, COMPLETED ─────────────
        Lesson guitarLesson2 = new Lesson.Builder()
                .expertise(guitarExpertise)
                .startTime(now.minusDays(21).withHour(11).withMinute(0).withSecond(0).withNano(0))
                .endTime(now.minusDays(21).withHour(12).withMinute(0).withSecond(0).withNano(0))
                .remote(true)
                .listedPrice(new BigDecimal(PRICE_30))
                .lessonStatus(LessonStatus.COMPLETED)
                .createdAt(now.minusDays(24))
                .build();
        lessonDao.insertLesson(null, guitarLesson2);

        bookingDao.insertBooking(null, new Booking.Builder()
                .lesson(guitarLesson2)
                .student(luigi)
                .bookedAt(now.minusDays(24))
                .pricePaid(new BigDecimal(PRICE_30))
                .paymentStatus(PaymentStatus.PAID)
                .paymentRef("TXN-GUITAR-004")
                .build());

        // ── Available lesson (for booking dialog demo) ──────────────────
        // Solo Piano: student_luigi ha già booking attive (Paid) per Saxophone
        // e Jazz Guitar con tutor_vitto — il checkNoDuplicateBooking blocca
        // quelle sottocategorie, quindi mostrarle come disponibili sarebbe fuorviante.
        Lesson availPiano = new Lesson.Builder()
                .expertise(pianoExpertise)
                .startTime(now.plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0))
                .endTime(now.plusDays(7).withHour(11).withMinute(0).withSecond(0).withNano(0))
                .remote(true)
                .listedPrice(new BigDecimal(PRICE_40))
                .lessonStatus(LessonStatus.AVAILABLE)
                .createdAt(now)
                .build();
        lessonDao.insertLesson(null, availPiano);

        return new int[]{ guitarLessonId, saxLessonId };
    }

    private void populateNotifications(int[] availableLessonIds) throws DatabaseException {

        Notification appNotif = new Notification.Builder()
                .recipientUsername(USER_STUDENT)
                .senderUsername(null)
                .message("Your application for Music has been received and is under review.")
                .type(NotificationType.APPLICATION_UPDATE)
                .targetId(1)
                .timestamp(LocalDateTime.now().minusDays(2))
                .read(false)
                .build();
        notificationDao.insert(null, appNotif);

        Notification adminAppNotif = new Notification.Builder()
                .recipientUsername("admin")
                .senderUsername(USER_STUDENT)
                .message(USER_STUDENT + " has submitted a new tutor application for Music · Guitar."
                        + " Please review and evaluate.")
                .type(NotificationType.APPLICATION_UPDATE)
                .targetId(1)
                .timestamp(LocalDateTime.now().minusDays(2))
                .read(false)
                .build();
        notificationDao.insert(null, adminAppNotif);

        if (availableLessonIds.length == 0) return;

        // Le notifiche puntano alle lezioni già BOOKED (non alle AVAILABLE):
        // rappresentano le richieste storiche di luigi, già accettate e pagate.
        // Così checkNoPendingRequest non blocca le lezioni AVAILABLE.
        int guitarLessonId = availableLessonIds[0];
        int saxLessonId    = availableLessonIds[1];

        Notification tutorNotif = new Notification.Builder()
                .recipientUsername(USER_TUTOR)
                .senderUsername(USER_STUDENT)
                .message(USER_STUDENT + " has requested a lesson on "
                        + "Saxophone. Please review and respond.")
                .type(NotificationType.LESSON_BOOKED)
                .targetId(saxLessonId)
                .timestamp(LocalDateTime.now().minusHours(1))
                .read(true)
                .build();
        notificationDao.insert(null, tutorNotif);

        Notification tutorNotif2 = new Notification.Builder()
                .recipientUsername(USER_TUTOR)
                .senderUsername(USER_STUDENT)
                .message(USER_STUDENT + " has requested a lesson on "
                        + "Guitar. Please review and respond.")
                .type(NotificationType.LESSON_BOOKED)
                .targetId(guitarLessonId)
                .timestamp(LocalDateTime.now().minusMinutes(20))
                .read(true)
                .build();
        notificationDao.insert(null, tutorNotif2);
    }

    private void populateMessages() throws DatabaseException {
        LocalDateTime base = LocalDateTime.now().minusHours(2);

        messageDao.insert(null, new Message.Builder()
                .senderUsername(USER_STUDENT).recipientUsername(USER_TUTOR)
                .content("Hey! I can't wait for our Guitar lesson next week.")
                .sentAt(base).read(true).build());

        messageDao.insert(null, new Message.Builder()
                .senderUsername(USER_TUTOR).recipientUsername(USER_STUDENT)
                .content("Hey Luigi! We'll start with the C major scale and basic chords. Get your guitar ready!")
                .sentAt(base.plusMinutes(6)).read(true).build());

        messageDao.insert(null, new Message.Builder()
                .senderUsername(USER_STUDENT).recipientUsername(USER_TUTOR)
                .content("Perfect! Do I need to bring anything specific?")
                .sentAt(base.plusMinutes(14)).read(true).build());

        messageDao.insert(null, new Message.Builder()
                .senderUsername(USER_TUTOR).recipientUsername(USER_STUDENT)
                .content("Just your guitar and a notebook. I'll bring the sheet music. See you Tuesday!")
                .sentAt(base.plusMinutes(22)).read(true).build());


        messageDao.insert(null, new Message.Builder()
                .senderUsername(USER_STUDENT).recipientUsername(USER_TUTOR)
                .content("Great, see you Tuesday then. Thanks a lot!")
                .sentAt(base.plusMinutes(30)).read(false).build());
    }

    private void populateReviews() throws DatabaseException, DuplicateReviewException {

        LocalDateTime base = LocalDateTime.now().minusMonths(6);
        int bookingId = 1000;
        for (int i = 0; i < 20; i++) {
            Booking stub = new Booking.Builder()
                    .id(bookingId++)
                    .pricePaid(new BigDecimal(PRICE_40))
                    .paymentStatus(PaymentStatus.PAID)
                    .build();
            reviewDao.insertReview(null, new Review.Builder()
                    .booking(stub).student(studentLuigi).tutor(tutorVitto)
                    .rating(5).createdAt(base.plusDays(bookingId)).build());
        }
        for (int i = 0; i < 4; i++) {
            Booking stub = new Booking.Builder()
                    .id(bookingId++)
                    .pricePaid(new BigDecimal(PRICE_40))
                    .paymentStatus(PaymentStatus.PAID)
                    .build();
            reviewDao.insertReview(null, new Review.Builder()
                    .booking(stub).student(studentLuigi).tutor(tutorVitto)
                    .rating(4).createdAt(base.plusDays(bookingId)).build());
        }
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

    @Override
    public MessageDao createMessageDao() { return messageDao; }
}