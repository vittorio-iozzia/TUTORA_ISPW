package it.ispw.tutora.model;

import it.ispw.tutora.enums.Role;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;

/**
 * Entity base condivisa da tutti i ruoli del sistema.
 *
 * -----------------------------------------------------------------------
 * Pattern Table-Per-Subclass
 * -----------------------------------------------------------------------
 * Ogni ruolo ha la propria tabella nel DB (student, tutor, admin)
 * collegata via FK a questa tabella base (user).
 * Le sottoclassi concrete sono Student, Tutor e Admin.
 *
 * passwordHash non ha getter pubblico: l'hash viene usato solo
 * internamente dal metodo matchesPassword() per la verifica
 * delle credenziali durante il login. Non circola mai fuori
 * dalla classe. Il cambio password passa sempre dal DAO.
 */
public abstract class User {

    private final String        username;
    private final String        email;
    private final String        name;
    private final String        surname;
    private final String        passwordHash;
    private final Role          role;
    private       String        description;
    private       boolean       isActive;
    private final LocalDateTime createdAt;

    protected User(String username,
                   String email,
                   String name,
                   String surname,
                   String passwordHash,
                   Role role,
                   String description,
                   boolean isActive,
                   LocalDateTime createdAt) {
        this.username     = username;
        this.email        = email;
        this.name         = name;
        this.surname      = surname;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.description  = description;
        this.isActive     = isActive;
        this.createdAt    = createdAt;
    }

    // ----------------------------------------------------------------
    // Autenticazione
    // ----------------------------------------------------------------

    /**
     * Verifica se la password in chiaro corrisponde all'hash memorizzato.
     * L'hash non viene mai esposto all'esterno — la verifica avviene
     * interamente all'interno della classe tramite BCrypt.
     *
     * Usato dal Controller applicativo durante il login:
     *   if (user.matchesPassword(bean.getPassword())) { ... }
     *
     * @param rawPassword password in chiaro inserita dall'utente
     * @return true se la password è corretta
     */
    public boolean matchesPassword(String rawPassword) {
        return BCrypt.checkpw(rawPassword, this.passwordHash);
    }

    // ----------------------------------------------------------------
    // Getter
    // ----------------------------------------------------------------

    public String        getUsername()    { return username; }
    public String        getEmail()       { return email; }
    public String        getName()        { return name; }
    public String        getSurname()     { return surname; }
    public Role          getRole()        { return role; }
    public String        getDescription() { return description; }
    public boolean       isActive()       { return isActive; }
    public LocalDateTime getCreatedAt()   { return createdAt; }

    /**
     * Restituisce nome e cognome concatenati.
     * Evita la duplicazione della logica di formattazione
     * nei Controller e nella View.
     */
    public String getFullName() {
        return name + " " + surname;
    }

    // ----------------------------------------------------------------
    // Setter (solo campi mutabili dopo la creazione)
    // ----------------------------------------------------------------

    public void setDescription(String description) { this.description = description; }
    public void setActive(boolean isActive)        { this.isActive = isActive; }

    @Override
    public String toString() {
        return getClass().getSimpleName()
               + "{username='" + username + "', role=" + role + "}";
    }
}
