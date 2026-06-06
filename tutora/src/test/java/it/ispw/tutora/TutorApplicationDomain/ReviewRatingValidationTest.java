// Vittorio Iozzia
package it.ispw.tutora.TutorApplicationDomain;

import it.ispw.tutora.model.Review;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ReviewRatingValidationTest {

    @Test
    void testRatingZeroThrows() {
        Review.Builder builder = new Review.Builder().id(0).rating(0).createdAt(LocalDateTime.of(2025, Month.JUNE, 1, 10, 0));
        assertThrows(IllegalArgumentException.class, builder::build,
                "Rating of 0 should throw IllegalArgumentException.");
    }
}
