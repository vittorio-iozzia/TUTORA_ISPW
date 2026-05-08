package it.ispw.tutora.model;

import it.ispw.tutora.enums.Role;

import java.math.BigDecimal;
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
 * Estende User.Builder per ereditare i campi comuni senza duplicarli.
 *
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

    private BigDecimal budget;
    private final List<Tutor> preferredTutors;
    private final List<Category> interests;

    // Costruttore privato: accessibile solo tramite Builder
    private Student(Builder builder) {
        super(builder, Role.STUDENT);
        this.budget = builder.budget;
        this.preferredTutors = new ArrayList<>();
        this.interests = new ArrayList<>();
    }

    // ----------------------------------------------------------------
    // Builder – estende User.Builder
    // ----------------------------------------------------------------

    public static class Builder extends User.Builder<Builder> {

        private BigDecimal budget;

        public Builder budget(BigDecimal budget) {
            this.budget = budget;
            return this;
        }
        @Override
        public Student build() {
            return new Student(this);
        }
    }

    // ----------------------------------------------------------------
    // Preferenze e interessi
    // ----------------------------------------------------------------

    public void addPreference(Tutor tutor) {
        if (preferredTutors.stream()
                .anyMatch(t -> t.getUsername().equals(tutor.getUsername()))) {
            throw new IllegalArgumentException(
                    "Tutor already in preferences."
            );
        }
        preferredTutors.add(tutor);
    }

    public void addInterest(Category category) {
        if (interests.stream()
                .anyMatch(c -> c.getName().equals(category.getName()))) {
            throw new IllegalArgumentException(
                    "Category already in interests."
            );
        }
        interests.add(category);
    }

    public List<Tutor> getPreferredTutors() {
        return Collections.unmodifiableList(preferredTutors);
    }

    public List<Category> getInterests() {
        return Collections.unmodifiableList(interests);
    }

    // ----------------------------------------------------------------
    // Logica di dominio – budget
    // ----------------------------------------------------------------

    public boolean hasSufficientBudget(BigDecimal amount) {
        return budget.compareTo(amount) >= 0;
    }

    public void deductBudget(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Negative amount.");
        }
        if (!hasSufficientBudget(amount)) {
            throw new IllegalArgumentException("Insufficient budget.");
        }
        this.budget = budget.subtract(amount);
    }

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