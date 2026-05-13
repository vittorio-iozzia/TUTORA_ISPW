package it.ispw.tutora.controller.application;

import it.ispw.tutora.bean.BookingBean;
import it.ispw.tutora.bean.BookingTutorBean;
import it.ispw.tutora.bean.LessonStudentBean;
import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.dao.LessonDao;
import it.ispw.tutora.dao.NotificationDao;
import it.ispw.tutora.dao.StudentDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.enums.NotificationType;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.exception.BookingNotFoundException;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.LessonNotFoundException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Lesson;
import it.ispw.tutora.model.Notification;
import it.ispw.tutora.model.Student;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class BookTutorController {

    private final StudentDao student;
    private final BookingDao booking;
    private final LessonDao lesson;
    private final NotificationDao notification;

    public BookTutorController() {
        DaoFactory daoFactory = DaoFactory.getInstance();
        this.student = daoFactory.createStudentDao();
        this.booking = daoFactory.createBookingDao();
        this.lesson = daoFactory.createLessonDao();
        this.notification = daoFactory.createNotificationDao();
    }

    // ----------------------------------------------------------------
    // searchAvailableLessons
    // ----------------------------------------------------------------

    /**
     * Carica le lezioni disponibili per un tutor e le mette nella bean.
     * Non richiede transazione — è una sola SELECT.
     */
    public void searchAvailableLessons(LessonStudentBean bean) {
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
     * Non richiede transazione — è una sola INSERT sulla notifica.
     */
    public void requestBooking(BookingBean bean) {
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            Lesson less = lesson.selectLesson(conn, bean.getLessonId());
            // Session non ancora presente — da aggiungere dopo il pull
            Student stu = (Student) Session.getInstance().getCurrentUser();
            // Pre-verifica del budget: se insufficiente si interrompe prima di notificare il tutor
            if (!stu.hasSufficientBudget(less.getListedPrice())) {
                bean.setErrorMessage("Insufficient budget.");
                return;
            }
            Notification notify = new Notification.Builder()
                    .recipientUsername(less.getExpertise().getTutor().getUsername())
                    .message(stu.getUsername() + " has just booked a lesson with you from "
                            + less.getStartTime() + " to " + less.getEndTime()
                            + " Please check your dashboard for more details.")
                    .type(NotificationType.LESSON_BOOKED)
                    .timestamp(LocalDateTime.now())
                    .build();
            notification.insert(conn, notify);
        } catch (DatabaseException | SQLException | LessonNotFoundException e) {
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
     * Usa il flag booleano per garantire il rollback in caso di errore
     * mantenendo il try-with-resources per la chiusura automatica della connessione.
     */
    public void respondToRequest(BookingTutorBean bean) {
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            // flag: se rimane false il finally esegue il rollback
            boolean success = false;
            try {
                Notification.Builder builder = new Notification.Builder();
                if (!bean.isAccepted()) {
                    // Rifiuto: notifica lo student e committa
                    Notification n = builder
                            .recipientUsername(bean.getStudentUsername())
                            .message("Your reservation for this lesson: "
                                    + bean.getLessonId() + " has been rejected.")
                            .type(NotificationType.LESSON_REJECTED)
                            .timestamp(LocalDateTime.now())
                            .build();
                    notification.insert(conn, n);
                    conn.commit();
                    success = true;
                    return;
                }
                // Accettazione: aggiorna status lezione poi notifica lo student
                Lesson less = lesson.selectLesson(conn, bean.getLessonId());
                // updateLessonStatus applica la FSM del model: lancia IllegalArgumentException
                // se la lezione non è in stato AVAILABLE (es. già cancellata o prenotata)
                less.updateLessonStatus(LessonStatus.BOOKED);
                lesson.updateStatus(conn, bean.getLessonId(), LessonStatus.BOOKED);
                Notification notification1 = builder
                        .recipientUsername(bean.getStudentUsername())
                        .message("Your reservation for this lesson: "
                                + bean.getLessonId() + " has been accepted.")
                        .type(NotificationType.LESSON_ACCEPTED)
                        .timestamp(LocalDateTime.now())
                        .build();
                notification.insert(conn, notification1);
                conn.commit();
                success = true;
            } finally {
                // rollback automatico se success è rimasto false
                if (!success) conn.rollback();
            }
        } catch (DatabaseException | SQLException | LessonNotFoundException | IllegalArgumentException e) {
            bean.setErrorMessage("System Error. Try later.");
        }
    }

    // ----------------------------------------------------------------
    // payment
    // ----------------------------------------------------------------

    /**
     * Lo student effettua il pagamento dopo che il tutor ha accettato.
     * Operazioni atomiche nella stessa transazione:
     *   1. Inserisce la booking con status PENDING
     *   2. Scala il budget dello student
     *   3. Aggiorna il payment status a PAID
     *   4. Notifica il tutor del pagamento avvenuto
     * Usa il flag booleano per garantire il rollback in caso di errore
     * mantenendo il try-with-resources per la chiusura automatica della connessione.
     */
    public void payment(BookingBean bean) {
        // Session non ancora presente — da aggiungere dopo il pull
        Student stu = (Student) Session.getInstance().getCurrentUser();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            // flag: se rimane false il finally esegue il rollback
            boolean success = false;
            try {
                Lesson less = lesson.selectLesson(conn, bean.getLessonId());
                // Il budget va verificato PRIMA di inserire la booking: un insertBooking
                // con budget insufficiente lascerebbe una riga PENDING orfana nel DB
                if (!stu.hasSufficientBudget(less.getListedPrice())) {
                    bean.setErrorMessage("Insufficient budget.");
                    success = true; // non è un errore tecnico — nessun rollback necessario
                    return;
                }
                Booking booking1 = new Booking.Builder()
                        .lesson(less)
                        .student(stu)
                        .bookedAt(LocalDateTime.now())
                        .pricePaid(less.getListedPrice())
                        .paymentStatus(PaymentStatus.PENDING)
                        .paymentRef(bean.getPaymentRef())
                        .build();
                bean.setId(booking.insertBooking(conn, booking1));
                stu.deductBudget(less.getListedPrice());
                // Persiste il budget aggiornato nel DB: deductBudget modifica solo il model in-memory
                student.updateStudentBudget(conn, stu.getUsername(), stu.getBudget());
                // updatePaymentStatus applica la FSM del model: lancia IllegalArgumentException
                // se la transizione PENDING → PAID non è valida
                booking1.updatePaymentStatus(PaymentStatus.PAID);
                booking.updateStatus(conn, bean.getId(), PaymentStatus.PAID);
                Notification notification1 = new Notification.Builder()
                        .recipientUsername(less.getExpertise().getTutor().getUsername())
                        .message("The booking has been paid.")
                        .type(NotificationType.PAYMENT_CONFIRMED)
                        .timestamp(LocalDateTime.now())
                        .build();
                notification.insert(conn, notification1);
                conn.commit();
                success = true;
            } finally {
                // rollback automatico se success è rimasto false
                if (!success) conn.rollback();
            }
        } catch (DatabaseException | SQLException | BookingNotFoundException
                 | LessonNotFoundException | UserNotFoundException | IllegalArgumentException e) {
            bean.setErrorMessage("System Error. Try later.");
        }
    }
}
