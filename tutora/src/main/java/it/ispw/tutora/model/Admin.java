package it.ispw.tutora.model;

import it.ispw.tutora.enums.Role;

import java.time.LocalDateTime;

public class Admin extends User{
    public Admin(String username, String email, String name, String surname, String passwordHash,
                 String description, boolean isActive, LocalDateTime createdAt, LocalDateTime updatedAt){
        super(username, email, name, surname, passwordHash, Role.ADMIN, description, isActive, createdAt, updatedAt);
    }

}
