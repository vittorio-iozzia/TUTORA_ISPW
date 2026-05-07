package it.ispw.tutora.model;

import it.ispw.tutora.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity che rappresenta un tutor del sistema.
 * Estende User aggiungendo rating e ratingCount,
 * come da tabella tutor del DB.
 *
 * -----------------------------------------------------------------------
 * Pattern Builder (GoF – Creazionale)
 * -----------------------------------------------------------------------
 * Il costruttore ha molti parametri ereditati da User più quelli
 * specifici di Tutor. Il Builder rende la costruzione leggibile
 * e a prova di errore.
 *
 * Utilizzo:
 *   Tutor tutor = new Tutor.Builder()
 *       .username("tutor_vitto")
 *       .email("vitto@tutora.com")
 *       .name("Vittorio")
 *       .surname("Iozzia")
 *       .passwordHash("$2a$12$...")
 *       .description("Sassofonista professionista")
 *       .active(true)
 *       .createdAt(LocalDateTime.now())
 *       .rating(BigDecimal.ZERO)
 *       .ratingCount(0)
 *       .build();
 *
 * -----------------------------------------------------------------------
 * Nota sul rating
 * -----------------------------------------------------------------------
 * rating e ratingCount sono mantenuti sincronizzati automaticamente
 * dai trigger SQL (trg_review_after_insert / update / delete).
 * Il layer Java NON ricalcola mai il rating manualmente —
 * è sufficiente rileggere i valori dal DB dopo ogni recensione.
 * I due campi vengono aggiornati sempre insieme tramite setRating()
 * per garantire la coerenza.
 */
public class Tutor extends User {

    private BigDecimal rating;
    private int        ratingCount;

    // Costruttore privato: accessibile solo tramite Builder
    private Tutor(Builder builder) {
        super(builder.username,
              builder.email,
              builder.name,
              builder.surname,
              builder.passwordHash,
              Role.TUTOR,
              builder.description,
              builder.active,
              builder.createdAt);
        this.rating      = builder.rating;
        this.ratingCount = builder.ratingCount;
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
        private BigDecimal    rating;
        private int           ratingCount;

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

        public Builder rating(BigDecimal rating) {
            this.rating = rating;
            return this;
        }

        public Builder ratingCount(int ratingCount) {
            this.ratingCount = ratingCount;
            return this;
        }

        public Tutor build() {
            return new Tutor(this);
        }
    }

    // ----------------------------------------------------------------
    // Getter
    // ----------------------------------------------------------------

    public BigDecimal getRating()      { return rating; }
    public int        getRatingCount() { return ratingCount; }

    // ----------------------------------------------------------------
    // Setter atomico
    // ----------------------------------------------------------------

    /**
     * Aggiorna rating e ratingCount dopo una rilettura dal DB.
     * I due parametri sono sempre aggiornati insieme per evitare
     * stati inconsistenti (es. rating aggiornato ma count vecchio).
     *
     * @param rating      nuovo valore medio calcolato dai trigger SQL
     * @param ratingCount nuovo conteggio delle recensioni
     */
    public void setRating(BigDecimal rating, int ratingCount) {
        this.rating      = rating;
        this.ratingCount = ratingCount;
    }
}
