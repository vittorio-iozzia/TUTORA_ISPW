package it.ispw.tutora.model;

import it.ispw.tutora.enums.Role;

import java.time.LocalDateTime;

/**
 * Entity che rappresenta un amministratore del sistema.
 * Estende User senza aggiungere campi aggiuntivi, come da
 * tabella admin del DB (solo username come FK di user).
 *
 * -----------------------------------------------------------------------
 * Pattern Builder (GoF – Creazionale)
 * -----------------------------------------------------------------------
 * Coerente con le altre sottoclassi di User (Student, Tutor),
 * Admin usa il Builder per la costruzione dell'oggetto.
 *
 * Utilizzo:
 *   Admin admin = new Admin.Builder()
 *       .username("admin_ale")
 *       .email("admin@tutora.com")
 *       .name("Alessio")
 *       .surname("Dainelli")
 *       .passwordHash("$2a$12$...")
 *       .description("Amministratore di sistema")
 *       .active(true)
 *       .createdAt(LocalDateTime.now())
 *       .build();
 *
 * -----------------------------------------------------------------------
 * Nota architetturale
 * -----------------------------------------------------------------------
 * La tabella admin nel DB non ha campi aggiuntivi rispetto a user.
 * Esiste per consentire estensioni future specifiche del ruolo
 * (es. livello di permessi, area di competenza) senza alterare
 * la tabella user.
 */
public class Admin extends User {

    // Costruttore privato: accessibile solo tramite Builder
    private Admin(Builder builder) {
        super(builder.username,
              builder.email,
              builder.name,
              builder.surname,
              builder.passwordHash,
              Role.ADMIN,
              builder.description,
              builder.active,
              builder.createdAt);
    }

    // ----------------------------------------------------------------
    // Builder – classe interna statica
    // ----------------------------------------------------------------

    public static class Builder {

        private String        username;
        private String        email;
        private String        name;
        private String        surname;
        private String        passwordHash;
        private String        description;
        private boolean       active;
        private LocalDateTime createdAt;

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder surname(String surname) {
            this.surname = surname;
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Admin build() {
            return new Admin(this);
        }
    }
}
