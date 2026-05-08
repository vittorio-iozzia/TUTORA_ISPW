package it.ispw.tutora.model;

import it.ispw.tutora.enums.Role;

/**
 * Entity che rappresenta un amministratore del sistema.
 * Estende User senza aggiungere campi aggiuntivi, come da
 * tabella admin del DB (solo username come FK di user).
 *
 * -----------------------------------------------------------------------
 * Pattern Builder (GoF – Creazionale)
 * -----------------------------------------------------------------------
 * Estende User.Builder per ereditare i campi comuni senza duplicarli.
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
        super(builder, Role.ADMIN);
    }

    // ----------------------------------------------------------------
    // Builder – estende User.Builder
    // ----------------------------------------------------------------

    public static class Builder extends User.Builder<Builder> {
        @Override
        public Admin build() {
            return new Admin(this);
        }
    }
}