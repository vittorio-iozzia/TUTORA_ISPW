package it.ispw.tutora.model;

import it.ispw.tutora.enums.Role;

import java.time.LocalDateTime;


public abstract class User {
    private final String username;
    private String email;
    private final String name;
    private final String surname;
    private String passwordHash;
    private Role role;
    private String description;
    private boolean isActive;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public User(String username, String email, String name, String surname, String passwordHash,
                Role role, String description, boolean isActive, LocalDateTime createdAt, LocalDateTime updatedAt){
        this.username=username;
        this.email=email;
        this.name=name;
        this.surname=surname;
        this.passwordHash=passwordHash;
        this.role=role;
        this.description=description;
        this.isActive=isActive;
        this.createdAt=createdAt;
        this.updatedAt=updatedAt;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
