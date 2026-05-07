package it.ispw.tutora.model;

import it.ispw.tutora.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entity che rappresenta uno studente del sistema.
 * Estende User aggiungendo budget, preferenze tutor e interessi
 * per categoria, come da tabelle student, student_preference
 * e student_interest del DB.
 *
 * -----------------------------------------------------------------------
 * Pattern Builder (GoF – Creazionale)
 * -----------------------------------------------------------------------
 * Utilizzo:
 *   Student student = new Student.Builder()
 *       .username("student_luigi")
 *       .email("luigi.verdi@tutora.com")
 *       .name("Luigi")
 *       .surname("Verdi")
 *       .passwordHash("$2a$12$...")
 *       .description("Studente di musica")
 *       .active(true)
 *       .createdAt(LocalDateTime.now())
 *       .budget(new BigDecimal("150.00"))
 *       .build();
 *
 * -----------------------------------------------------------------------
 * Relazioni M:N
 * -----------------------------------------------------------------------
 * student_preference e student_interest sono tabelle di join pure
 * nel DB — non diventano classi nel Model ma liste all'interno
 * di Student, popolate dal DAO tramite JOIN.
 *
 * -----------------------------------------------------------------------
 * Logica di dominio
 * -----------------------------------------------------------------------
 * deductBudget() e addBudget() sono gli unici modi per modificare
 * il budget — setBudget() è stato rimosso perché bypassava le guardie
 * di validazione, permettendo valori negativi o inconsistenti.
 */
public class Student extends User {

    private BigDecimal       budget;
    private final List<Tutor>    preferredTutors;
    private final List<Category> interests;

    // Costruttore privato: accessibile solo tramite Builder
    private Student(Builder builder) {
        super(builder.username,
                builder.email,
                builder.name,
                builder.surname,
                builder.passwordHash,
                Role.STUDENT,
                builder.description,
                builder.active,
                builder.createdAt);
        this.budget          = builder.budget;
        this.preferredTutors = new ArrayList<>();
        this.interests       = new ArrayList<>();
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
        private BigDecimal    budget;

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

        public Builder budget(BigDecimal budget) {
            this.budget = budget;
            return this;
        }

        public Student build() {
            return new Student(this);
        }
    }

    // ----------------------------------------------------------------
    // Preferenze e interessi
    // ----------------------------------------------------------------

    public void addPreference(Tutor tutor) {
        if (preferredTutors.contains(tutor)){
            throw new IllegalArgumentException("Tutor already present.");
        }
        preferredTutors.add(tutor);
    }

    public void addInterest(Category category) {
        if(interests.contains(category)){
            throw new IllegalArgumentException("Category already present.");
        }
        interests.add(category);
    }

    /**
     * Restituisce una vista non modificabile dei tutor preferiti.
     */
    public List<Tutor> getPreferredTutors() {
        return Collections.unmodifiableList(preferredTutors);
    }

    /**
     * Restituisce una vista non modificabile degli interessi per categoria.
     */
    public List<Category> getInterests() {
        return Collections.unmodifiableList(interests);
    }

    // ----------------------------------------------------------------
    // Logica di dominio – budget
    // ----------------------------------------------------------------

    /**
     * Verifica se lo studente ha fondi sufficienti per una spesa.
     */
    public boolean hasSufficientBudget(BigDecimal amount) {
        return budget.compareTo(amount) >= 0;
    }

    /**
     * Scala il budget dopo un pagamento.
     * Solleva IllegalArgumentException se l'importo è negativo
     * o supera il budget disponibile.
     */
    public void deductBudget(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Negative amount.");
        }
        if (!hasSufficientBudget(amount)) {
            throw new IllegalArgumentException("Insufficient budget.");
        }
        this.budget = budget.subtract(amount);
    }

    /**
     * Aggiunge credito al budget dello studente.
     * Usato quando l'admin ricarica il budget o viene effettuato
     * un rimborso.
     */
    public void addBudget(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        this.budget = budget.add(amount);
    }

    // ----------------------------------------------------------------
    // Getter
    // ----------------------------------------------------------------

    public BigDecimal getBudget() { return budget; }
}
