package it.ispw.tutora.model;

import java.time.LocalDateTime;

public class Admin extends User{
    public Admin(String username, String email, String name, String surname, String passwordHash,
                 Role role, String description, boolean isActive, LocalDateTime createdAt, LocalDateTime updatedAt){
        super(username, email, name, surname, passwordHash, role, description, isActive, createdAt, updatedAt);
    }

}
