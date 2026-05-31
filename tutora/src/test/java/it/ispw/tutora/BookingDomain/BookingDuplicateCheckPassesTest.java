// Alessio Dainelli
package it.ispw.tutora.BookingDomain;

import it.ispw.tutora.dao.demo.BookingDaoDemo;
import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.enums.Status;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BookingDuplicateCheckPassesTest {

    private BookingDaoDemo bookingDao;

    @BeforeEach
    void setUp() throws DatabaseException {
        bookingDao = new BookingDaoDemo();

        Tutor tutor = new Tutor.Builder()
                .username("tutor_test")
                .name("Test").surname("Tutor")
                .rating(BigDecimal.ZERO).ratingCount(0)
                .build();

        Student student = new Student.Builder()
                .username("student_test")
                .budget(new BigDecimal("200.00"))
                .build();

        Category music = new Category("Music", "Music lessons");
        SubCategory guitar = new SubCategory("Guitar", music, "Guitar lessons");
        TutorExpertise expertise = new TutorExpertise(
                tutor, guitar, new BigDecimal("30.00"), Status.APPROVED, LocalDateTime.now());

        Lesson lesson = new Lesson.Builder()
                .expertise(expertise)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(1))
                .lessonStatus(LessonStatus.BOOKED)
                .listedPrice(new BigDecimal("30.00"))
                .createdAt(LocalDateTime.now())
                .build();

        bookingDao.insertBooking(null, new Booking.Builder()
                .lesson(lesson).student(student)
                .pricePaid(new BigDecimal("30.00")).paymentStatus(PaymentStatus.PAID).build());
    }

    @Test
    void testCheckPassesForDifferentTutor() {
        // Lo student ha una booking attiva con tutor_test per Guitar:
        // una richiesta con un tutor diverso deve passare senza eccezioni.
        assertDoesNotThrow(
                () -> bookingDao.checkNoDuplicateBooking(null, "student_test", "other_tutor", "Guitar"),
                "A booking with a different tutor should not be flagged as duplicate.");
    }

    @Test
    void testCheckPassesForDifferentSubcategory() {
        // Lo student ha una booking attiva con tutor_test per Guitar:
        // una richiesta per una materia diversa con lo stesso tutor deve passare.
        assertDoesNotThrow(
                () -> bookingDao.checkNoDuplicateBooking(null, "student_test", "tutor_test", "Piano"),
                "A booking with the same tutor but a different subject should not be flagged as duplicate.");
    }
}
