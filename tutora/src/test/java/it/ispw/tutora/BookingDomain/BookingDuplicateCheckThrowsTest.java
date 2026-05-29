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
                tutor, piano, new BigDecimal("40.00"), Status.APPROVED, LocalDateTime.now());

        Lesson lesson = new Lesson.Builder()
                .expertise(expertise)
                .startTime(LocalDateTime.now().plusDays(3))
                .endTime(LocalDateTime.now().plusDays(3).plusHours(1))
                .lessonStatus(LessonStatus.BOOKED)
                .listedPrice(new BigDecimal("40.00"))
                .createdAt(LocalDateTime.now())
                .build();

        bookingDao.insertBooking(null, new Booking.Builder()
                .lesson(lesson).student(student)
                .pricePaid(new BigDecimal("40.00")).paymentStatus(PaymentStatus.PAID).build());
    }

    @Test
    void testCheckThrowsForOverlappingTimeSlot() {
        // La nuova lezione si sovrappone esattamente alla prenotazione esistente (now+3d, now+3d+1h).
        LocalDateTime newStart = LocalDateTime.now().plusDays(3);
        LocalDateTime newEnd   = LocalDateTime.now().plusDays(3).plusHours(1);
        assertThrows(DuplicateBookingException.class,
                () -> bookingDao.checkNoDuplicateBooking(null, "student_test", newStart, newEnd),
                "A lesson that overlaps an active booking should throw DuplicateBookingException.");
    }
}
