package it.ispw.tutora.controller.application;

import it.ispw.tutora.bean.BookingBean;
import it.ispw.tutora.bean.BookingTutorBean;
import it.ispw.tutora.bean.LessonStudentBean;
import it.ispw.tutora.boundary.PayPalBoundary;
import it.ispw.tutora.boundary.PaymentGateway;
import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.dao.LessonDao;
import it.ispw.tutora.dao.NotificationDao;
import it.ispw.tutora.dao.StudentDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.enums.NotificationType;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.exception.*;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Lesson;
import it.ispw.tutora.model.Notification;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.session.SessionManager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.logging.Logger;

public class BookTutorController {

    private static final Logger LOGGER = Logger.getLogger(BookTutorController.class.getName());

    private final StudentDao student;
    private final BookingDao booking;
    private final LessonDao lesson;
    private final NotificationDao notification;
    private final PaymentGateway paymentGateway;

    public BookTutorController() {
        DaoFactory daoFactory = DaoFactory.getInstance();
        this.student = daoFactory.createStudentDao();
        this.booking = daoFactory.createBookingDao();
        this.lesson = daoFactory.createLessonDao();
        this.notification = daoFactory.createNotificationDao();
        this.paymentGateway = new PayPalBoundary();
    }

    // ----------------------------------------------------------------
    // searchAvailableLessons
    // ----------------------------------------------------------------

    /**
     * Carica le lezioni disponibili per un tutor e le mette nella bean.
     * Non richiede transazione - e' una sola SELECT.
     */
    public void searchAvailableLessons(LessonStudentBean bean, String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token)) {
            bean.setErrorMessage("Invalid or expired session.");
            return;
        }
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            bean.setList(lesson.findByTutorAndStatus(
                    conn, bean.getTutorUsername(), LessonStatus.AVAILABLE));
        } catch (DatabaseException | SQLException e) {
            bean.setErrorMessage("System Error. Try later.");
        }
    }

    // ----------------------------------------------------------------
    // requestBooking
    // ----------------------------------------------------------------

    /**
     * Lo student richiede la prenotazione di una lezione.
     * Verifica il budget prima di notificare il tutor.
     * Non richiede transazione - e' una sola INSERT sulla notifica.
     */
    public void requestBooking(BookingBean bean, String token) {
        SessionManager sm = SessionManager.getInstance();
        // Verifica sessione prima di aprire la connessione
        if (!sm.isSessionValid(token) || !sm.getSession(token).isStudent()) {
            bean.setErrorMessage("Unauthorized.");
            return;
        }
        String username = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            Lesson less = lesson.selectLesson(conn, bean.getLessonId());
            // Ricarica lo student dal DB: il budget nella sessione e' snapshot al login, potrebbe essere stale
            Student stu = student.selectStudent(conn, username);
            // Pre-verifica del budget: se insufficiente si interrompe prima di notificare il tutor
            if (!stu.hasSufficientBudget(less.getListedPrice())) {
                bean.setErrorMessage("Insufficient budget.");
                return;
            }
            Notification notify = new Notification.Builder()
                    .recipientUsername(less.getExpertise().getTutor().getUsername())
                    .senderUsername(stu.getUsername())
                    .message(stu.getUsername() + " has just booked a lesson with you from "
                            + less.getStartTime() + " to " + less.getEndTime()
                            + " Please check your dashboard for more details.")
                    .type(NotificationType.LESSON_BOOKED)
                    .targetId(less.getId())
                    .timestamp(LocalDateTime.now())
                    .build();
            notification.insert(conn, notify);
        } catch (LessonNotFoundException e) {
            bean.setErrorMessage("Lesson not found.");
        } catch (UserNotFoundException e) {
            bean.setErrorMessage("Account not found.");
        } catch (DatabaseException | SQLException e) {
            bean.setErrorMessage("System Error. Try later.");
        }
    }

    // ----------------------------------------------------------------
    // respondToRequest
    // ----------------------------------------------------------------

    /**
     * Il tutor accetta o rifiuta la richiesta di prenotazione.
     * Se accetta: aggiorna lo status della lezione e notifica lo student.
     * Se rifiuta: notifica lo student e termina.
     * Le operazioni di accettazione sono atomiche: se una fallisce
     * safeRollback annulla tutto senza inghiottire l'eccezione originale.
     */
    public void respondToRequest(BookingTutorBean bean, String token) {
        SessionManager sm = SessionManager.getInstance();
        // Verifica che il chiamante sia un tutor autenticato
        if (!sm.isSessionValid(token) || !sm.getSession(token).isTutor()) {
            bean.setErrorMessage("Unauthorized.");
            return;
        }
        String tutorUsername = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            // null-check: in modalita' Demo/Json getConnection() restituisce null
            if (conn != null) conn.setAutoCommit(false);
            try {
                Notification.Builder builder = new Notification.Builder();
                if (!bean.isAccepted()) {
                    // Rifiuto: notifica lo student e committa
                    Notification n = builder
                            .recipientUsername(bean.getStudentUsername())
                            .senderUsername(tutorUsername)
                            .message("Your reservation for this lesson: "
                                    + bean.getLessonId() + " has been rejected.")
                            .type(NotificationType.LESSON_REJECTED)
                            .targetId(bean.getLessonId())
                            .timestamp(LocalDateTime.now())
                            .build();
                    notification.insert(conn, n);
                    if (conn != null) conn.commit();
                    return;
                }
                // Accettazione: aggiorna status lezione poi notifica lo student
                Lesson less = lesson.selectLesson(conn, bean.getLessonId());
                // updateLessonStatus applica la FSM del model: lancia IllegalArgumentException
                // se la lezione non e' in stato AVAILABLE (es. gia' cancellata o prenotata)
                less.updateLessonStatus(LessonStatus.BOOKED);
                lesson.updateStatus(conn, bean.getLessonId(), LessonStatus.BOOKED);
                Notification notification1 = builder
                        .recipientUsername(bean.getStudentUsername())
                        .senderUsername(tutorUsername)
                        .message("Your reservation for this lesson: "
                                + bean.getLessonId() + " has been accepted.")
                        .type(NotificationType.LESSON_ACCEPTED)
                        .targetId(bean.getLessonId())
                        .timestamp(LocalDateTime.now())
                        .build();
                notification.insert(conn, notification1);
                if (conn != null) conn.commit();
            } catch (LessonNotFoundException e) {
                safeRollback(conn);
                bean.setErrorMessage("Lesson not found.");
            } catch (IllegalArgumentException e) {
                // FSM violation: la lezione non e' piu' in stato AVAILABLE
                safeRollback(conn);
                bean.setErrorMessage("Invalid argument.");
            } catch (DatabaseException | SQLException e) {
                // safeRollback evita che una SQLException dal rollback inghiotta l'eccezione originale
                safeRollback(conn);
                bean.setErrorMessage("System Error. Try later.");
            }
        } catch (DatabaseException | SQLException e) {
            bean.setErrorMessage("System Error. Try later.");
        }
    }

    // ----------------------------------------------------------------
    // payment
    // ----------------------------------------------------------------

    /**
     * Lo student effettua il pagamento dopo che il tutor ha accettato.
     *
     * Il metodo e' strutturato in tre fasi distinte per evitare di tenere
     * aperta la connessione DB durante la chiamata al gateway esterno
     * (che puo' impiegare fino a 10 minuti):
     *
     *   Fase 1 - Lettura dati (connessione read, subito chiusa)
     *   Fase 2 - Chiamata gateway PayPal (fuori da qualsiasi connessione DB)
     *   Fase 3 - Transazione atomica DB (scritture con dati ricaricati freschi)
     */
    public void payment(BookingBean bean, String token) {
        SessionManager sm = SessionManager.getInstance();
        // Verifica sessione prima di aprire la connessione
        if (!sm.isSessionValid(token) || !sm.getSession(token).isStudent()) {
            bean.setErrorMessage("Unauthorized.");
            return;
        }
        String username = sm.getCurrentUser(token).getUsername();

        // Fase 1: leggi il prezzo della lezione e verifica il budget
        // La connessione viene chiusa prima di chiamare il gateway esterno
        BigDecimal price;
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            Lesson less = lesson.selectLesson(conn, bean.getLessonId());
            Student stu = student.selectStudent(conn, username);
            if (!stu.hasSufficientBudget(less.getListedPrice())) {
                bean.setErrorMessage("Insufficient budget.");
                return;
            }
            price = less.getListedPrice();
        } catch (LessonNotFoundException e) {
            bean.setErrorMessage("Lesson not found.");
            return;
        } catch (UserNotFoundException e) {
            bean.setErrorMessage("Account not found.");
            return;
        } catch (DatabaseException | SQLException e) {
            bean.setErrorMessage("System Error. Try later.");
            return;
        }

        // Fase 2: chiama il gateway PayPal fuori da qualsiasi connessione DB
        // Tenere aperta la connessione durante questa attesa esaurirebbe il pool
        // e manterrebbe lock inutilmente per fino a 10 minuti
        String paymentRef;
        try {
            paymentRef = paymentGateway.processPayment(price);
            bean.setPaymentRef(paymentRef);
        } catch (PaymentException e) {
            // Propaga il messaggio del gateway (es. "Payment declined"): e' informazione utile all'utente
            bean.setErrorMessage(e.getMessage());
            return;
        } catch (PaymentTimeoutException e) {
            bean.setErrorMessage(e.getMessage());
            return;
        }

        // Fase 3: transazione DB atomica con dati ricaricati freschi
        // Student e lesson vengono ricaricati perche' i dati della Fase 1 potrebbero
        // essere cambiati nel frattempo (es. budget scalato da un'altra sessione)
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            // null-check: in modalita' Demo/Json getConnection() restituisce null
            if (conn != null) conn.setAutoCommit(false);
            try {
                Student stu = student.selectStudent(conn, username);
                Lesson less = lesson.selectLesson(conn, bean.getLessonId());
                // Ri-verifica budget: potrebbe essere cambiato tra la Fase 1 e ora
                if (!stu.hasSufficientBudget(less.getListedPrice())) {
                    bean.setErrorMessage("Insufficient budget.");
                    return;
                }
                Booking booking1 = new Booking.Builder()
                        .lesson(less)
                        .student(stu)
                        .bookedAt(LocalDateTime.now())
                        .pricePaid(less.getListedPrice())
                        .paymentStatus(PaymentStatus.PENDING)
                        .paymentRef(paymentRef)
                        .build();
                bean.setId(booking.insertBooking(conn, booking1));
                stu.deductBudget(less.getListedPrice());
                // Persiste il budget aggiornato nel DB: deductBudget modifica solo il model in-memory
                student.updateStudentBudget(conn, stu.getUsername(), stu.getBudget());
                // updatePaymentStatus applica la FSM del model: lancia IllegalArgumentException
                // se la transizione PENDING -> PAID non e' valida
                booking1.updatePaymentStatus(PaymentStatus.PAID);
                booking.updateStatus(conn, bean.getId(), PaymentStatus.PAID);
                Notification notification1 = new Notification.Builder()
                        .recipientUsername(less.getExpertise().getTutor().getUsername())
                        .senderUsername(username)
                        .message("The booking has been paid.")
                        .type(NotificationType.PAYMENT_CONFIRMED)
                        .targetId(bean.getId())
                        .timestamp(LocalDateTime.now())
                        .build();
                notification.insert(conn, notification1);
                if (conn != null) conn.commit();
            } catch (LessonNotFoundException e) {
                safeRollback(conn);
                bean.setErrorMessage("Lesson not found.");
            } catch (UserNotFoundException e) {
                safeRollback(conn);
                bean.setErrorMessage("Account not found.");
            } catch (BookingNotFoundException e) {
                safeRollback(conn);
                bean.setErrorMessage("Booking not found.");
            } catch (IllegalArgumentException e) {
                // FSM violation: stato della booking o del budget non valido
                safeRollback(conn);
                bean.setErrorMessage("Invalid argument.");
            } catch (DatabaseException | SQLException e) {
                // safeRollback evita che una SQLException dal rollback inghiotta l'eccezione originale
                safeRollback(conn);
                bean.setErrorMessage("System Error. Try later.");
            }
        } catch (DatabaseException | SQLException e) {
            bean.setErrorMessage("System Error. Try later.");
        }
    }

    // ----------------------------------------------------------------
    // Helper privati
    // ----------------------------------------------------------------

    /**
     * Esegue il rollback loggando eventuali errori senza propagarli:
     * se rollback() lanciasse SQLException inghiottirebbe l'eccezione originale del chiamante.
     * Il null-check gestisce la modalita' Demo/Json dove conn e' null.
     */
    private void safeRollback(Connection conn) {
        if (conn == null) return;
        try {
            conn.rollback();
        } catch (SQLException e) {
            LOGGER.warning("Rollback failed: " + e.getMessage());
        }
    }
}