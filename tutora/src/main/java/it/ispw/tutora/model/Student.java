package it.ispw.tutora.model;

import it.ispw.tutora.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Student extends User {
    private BigDecimal budget; // Attributo specifico della tabella student

    // Il costruttore di Student deve "passare" i dati al costruttore di User
    public Student(String username, String email, String name, String surname, String passwordHash,
                   String description, boolean isActive, LocalDateTime createdAt,
                   BigDecimal budget){
        super(username, email, name, surname, passwordHash, Role.STUDENT, description, isActive, createdAt);
        this.budget = budget;
    }
    public boolean hasSufficientBudget(BigDecimal amount){
        return budget.compareTo(amount) >= 0;
    }
    public void deductBudget(BigDecimal amount){
        if (amount.compareTo(BigDecimal.ZERO)<0){
            throw new IllegalArgumentException("Negative amount.");
        }
        if (!hasSufficientBudget(amount)){
            throw new IllegalArgumentException("Insufficient budget.");
        }
        this.budget=budget.subtract(amount);
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }


}
