package it.ispw.tutora.bean;

import it.ispw.tutora.enums.Role;

import java.math.BigDecimal;

/**
 * Bean di trasporto per il profilo utente.
 *
 * Copre tutti i ruoli (Student, Tutor, Admin) in un unico bean.
 * I campi specifici del ruolo (budget, rating, ratingCount) sono null
 * quando non applicabili — il boundary li mostra o nasconde in base a role.
 *
 * Usato in due direzioni:
 *   - Controller → View: il Controller popola il bean dopo aver letto
 *     il Model; il boundary lo usa per popolare i campi della schermata.
 *   - View → Controller: il boundary popola solo description e active
 *     (i campi modificabili dall'utente) e passa il bean al Controller
 *     per l'aggiornamento del profilo.
 */
public class UserProfileBean {

    private String username;
    private String email;
    private String name;
    private String surname;
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

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

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
