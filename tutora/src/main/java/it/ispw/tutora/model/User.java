package it.ispw.tutora.model;

import it.ispw.tutora.enums.Role;

import java.time.LocalDateTime;


public abstract class User {
    private final String username;
    private final String email;
    private final String name;
    private final String surname;
    private final String passwordHash;
    private final Role role;
    private String description;
    private boolean isActive;
    private final LocalDateTime createdAt;
    public User(String username, String email, String name, String surname, String passwordHash,
                Role role, String description, boolean isActive, LocalDateTime createdAt){
        this.username=username;
        this.email=email;
        this.name=name;
        this.surname=surname;
        this.passwordHash=passwordHash;
        this.role=role;
        this.description=description;
        this.isActive=isActive;
        this.createdAt=createdAt;
    }
    public String getUsername(){
        return username;
    }
    public String getEmail(){
        return email;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }
    public Role getRole() {
        return role;
    }
    public String getDescription(){
        return description;
    }

    public boolean isActive() {
        return isActive;
    }
    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

}
