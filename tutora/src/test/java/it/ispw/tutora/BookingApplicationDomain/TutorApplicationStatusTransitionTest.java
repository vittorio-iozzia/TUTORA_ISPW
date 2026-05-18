// Alessio Dainelli
package it.ispw.tutora.BookingApplicationDomain;

import it.ispw.tutora.enums.ApplicationStatus;
import it.ispw.tutora.model.TutorApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TutorApplicationStatusTransitionTest {

    private TutorApplication application;

    @BeforeEach
    void setUp() {
        application = new TutorApplication(
                1, "Music", "student_test",
                LocalDateTime.now(), ApplicationStatus.DRAFT);
    }

    @Test
    void testDraftToAcceptedThrows() {
        assertThrows(IllegalStateException.class,
                () -> application.updateStatus(ApplicationStatus.ACCEPTED),
                "Skipping SUBMITTED and going directly to ACCEPTED should throw IllegalStateException.");
    }
}
