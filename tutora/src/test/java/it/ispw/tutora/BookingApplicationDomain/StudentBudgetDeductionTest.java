// Vittorio Iozzia
package it.ispw.tutora.StudentReviewDomain;

import it.ispw.tutora.model.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudentBudgetDeductionTest {

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student.Builder()
                .username("student_test")
                .budget(new BigDecimal("150.00"))
                .build();
    }

    @Test
    void testDeductBudgetReducesBalanceCorrectly() {
        student.deductBudget(new BigDecimal("40.00"));
        assertEquals(new BigDecimal("110.00"), student.getBudget(),
                "Balance after deducting 40.00 from 150.00 should be 110.00.");
    }
}
