package it.ispw.tutora.model;

import it.ispw.tutora.enums.Role;

import java.time.LocalDateTime;

public class Student extends User {
    private double budget; // Attributo specifico della tabella student

    // Il costruttore di Student deve "passare" i dati al costruttore di User
    public Student(String username, String email, String name, String surname, String passwordHash,
                   Role role, String description, boolean isActive, LocalDateTime createdAt, LocalDateTime updatedAt,
                   double budget){
        super(username, email, name, surname, passwordHash, role, description, isActive, createdAt, updatedAt);
        this.budget = budget;
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }


}
