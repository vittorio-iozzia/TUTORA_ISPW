// Vittorio Iozzia
package it.ispw.tutora.TutorApplicationDomain;

import it.ispw.tutora.enums.ApplicationStatus;
import it.ispw.tutora.model.TextRequirement;
import it.ispw.tutora.model.TutorApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class TutorApplicationReadyToSubmitTest {

    private TutorApplication application;
    private TextRequirement mandatoryReq;

    @BeforeEach
    void setUp() {
        application = new TutorApplication(
                1, "Music", "student_test",
                LocalDateTime.of(2025, Month.JUNE, 1, 10, 0), ApplicationStatus.DRAFT);

        mandatoryReq = new TextRequirement(
                "Music", "motivation_letter", "Motivation Letter",
                "Why do you want to become a tutor?", true, 50, 1000);
    }

    @Test
    void testNotReadyWhenMandatoryRequirementIsMissing() {
        assertFalse(application.isReadyToSubmit(List.of(mandatoryReq)),
                "Application should not be ready when a mandatory requirement has no filled item.");
    }
}
