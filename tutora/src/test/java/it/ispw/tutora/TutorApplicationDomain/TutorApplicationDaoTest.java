// Vittorio Iozzia
package it.ispw.tutora.TutorApplicationDomain;

import it.ispw.tutora.dao.demo.TutorApplicationDaoDemo;
import it.ispw.tutora.enums.ApplicationStatus;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateApplicationException;
import it.ispw.tutora.model.TutorApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TutorApplicationDaoTest {

    private TutorApplicationDaoDemo dao;

    @BeforeEach
    void setUp() throws DatabaseException, DuplicateApplicationException {
        dao = new TutorApplicationDaoDemo();

        dao.insert(null, new TutorApplication(
                0, "Music", "student_test",
                LocalDateTime.of(2025, 6, 1, 10, 0), ApplicationStatus.DRAFT));
    }

    @Test
    void testInsertingDuplicateActiveApplicationThrows() {
        TutorApplication duplicate = new TutorApplication(
                0, "Music", "student_test",
                LocalDateTime.of(2025, 6, 1, 10, 0), ApplicationStatus.DRAFT);

        assertThrows(DuplicateApplicationException.class,
                () -> dao.insert(null, duplicate),
                "A second active application for the same student and category should throw DuplicateApplicationException.");
    }
}
