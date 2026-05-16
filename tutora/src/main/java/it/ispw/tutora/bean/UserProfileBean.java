package it.ispw.tutora.bean;

import it.ispw.tutora.enums.Role;

import java.math.BigDecimal;

/**
 * Bean di trasporto per il profilo utente.
 *
 * Copre tutti i ruoli (Student, Tutor, Admin) in un unico bean.
 * I campi specifici del ruolo (budget, rating, ratingCount) sono null
 * quando non applicabili — il boundary li mostra o nasconde in base a role.
 */
public class UserProfileBean extends PersonBean {

    private String description;
    private boolean active;
    private Role role;

    // Campi specifici del ruolo — null se non applicabili
    private BigDecimal budget;       // STUDENT
    private BigDecimal rating;       // TUTOR
    private int ratingCount;         // TUTOR

    // ----------------------------------------------------------------
    // Getter e setter
    // ----------------------------------------------------------------

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
}
