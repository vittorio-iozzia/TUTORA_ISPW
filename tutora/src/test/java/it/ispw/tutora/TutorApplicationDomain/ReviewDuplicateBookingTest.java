// Vittorio Iozzia
package it.ispw.tutora.TutorApplicationDomain;

import it.ispw.tutora.dao.demo.ReviewDaoDemo;
import it.ispw.tutora.enums.PaymentStatus;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateReviewException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.Review;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.Tutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ReviewDuplicateBookingTest {

    private ReviewDaoDemo reviewDao;
    private Review firstReview;

    @BeforeEach
    void setUp() throws DatabaseException, DuplicateReviewException {
        reviewDao = new ReviewDaoDemo();

        Tutor tutor = new Tutor.Builder()
                .username("tutor_test")
                .name("Test").surname("Tutor")
                .rating(BigDecimal.ZERO).ratingCount(0)
                .build();

        Student student = new Student.Builder()
                .username("student_test")
                .budget(new BigDecimal("100.00"))
                .build();

        Booking booking = new Booking.Builder()
                .id(1).student(student)
                .pricePaid(new BigDecimal("30.00")).paymentStatus(PaymentStatus.PAID)
                .build();

        firstReview = new Review.Builder()
                .id(0).booking(booking).student(student)
                .tutor(tutor).rating(5).createdAt(LocalDateTime.of(2025, 6, 1, 10, 0)).build();

        reviewDao.insertReview(null, firstReview);
    }

    @Test
    void testInsertingSecondReviewForSameBookingThrows() {
        assertThrows(DuplicateReviewException.class,
                () -> reviewDao.insertReview(null, firstReview),
                "Inserting a second review for the same booking should throw DuplicateReviewException.");
    }
}
