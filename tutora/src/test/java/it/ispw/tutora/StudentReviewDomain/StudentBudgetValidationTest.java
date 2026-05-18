// Vittorio Iozzia
package it.ispw.tutora.StudentReviewDomain;

import it.ispw.tutora.model.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentBudgetValidationTest {

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student.Builder()
                .username("student_test")
                .budget(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void testHasSufficientBudgetReturnsTrueWhenExact() {
        assertTrue(student.hasSufficientBudget(new BigDecimal("100.00")),
                "Budget equal to the required amount should be considered sufficient.");
    }
}
