package it.ispw.tutora.model;

import it.ispw.tutora.enums.Role;

import java.time.LocalDateTime;


public class User {
    private String username;
    private String email;
    private String name;
    private String surname;
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

    public String getPasswordHash() {
        return passwordHash;
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
    public void setName(String name) {
        this.name = name;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
