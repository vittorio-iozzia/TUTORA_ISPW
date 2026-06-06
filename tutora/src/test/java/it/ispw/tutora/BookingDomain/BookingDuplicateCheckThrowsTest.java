// Alessio Dainelli
package it.ispw.tutora.BookingDomain;

import it.ispw.tutora.dao.demo.BookingDaoDemo;
import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.enums.Status;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateBookingException;
import it.ispw.tutora.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingDuplicateCheckThrowsTest {

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
        SubCategory piano = new SubCategory("Piano", music, "Piano lessons");
        TutorExpertise expertise = new TutorExpertise(
                tutor, piano, new BigDecimal("40.00"), Status.APPROVED, LocalDateTime.of(2025, 6, 1, 10, 0));

        Lesson lesson = new Lesson.Builder()
                .expertise(expertise)
                .startTime(LocalDateTime.of(2025, 6, 1, 10, 0).plusDays(3))
                .endTime(LocalDateTime.of(2025, 6, 1, 10, 0).plusDays(3).plusHours(1))
                .lessonStatus(LessonStatus.BOOKED)
                .listedPrice(new BigDecimal("40.00"))
                .createdAt(LocalDateTime.of(2025, 6, 1, 10, 0))
                .build();

        bookingDao.insertBooking(null, new Booking.Builder()
                .lesson(lesson).student(student)
                .pricePaid(new BigDecimal("40.00")).paymentStatus(PaymentStatus.PAID).build());
    }

    @Test
    void testCheckThrowsForSameTutorAndSubcategory() {
        // Lo student ha gia' una booking attiva con tutor_test per Piano:
        // una seconda richiesta con lo stesso tutor e la stessa materia deve lanciare l'eccezione.
        assertThrows(DuplicateBookingException.class,
                () -> bookingDao.checkNoDuplicateBooking(null, "student_test", "tutor_test", "Piano"),
                "A second booking with the same tutor and subject should throw DuplicateBookingException.");
    }
}
