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
 *
 * -----------------------------------------------------------------------
 * Pattern Builder (GoF – Creazionale)
 * -----------------------------------------------------------------------
 * User espone un Builder astratto generico che le sottoclassi
 * estendono per ereditare i campi comuni senza duplicarli.
 * Ogni sottoclasse definisce il proprio Builder concreto:
 *
 *   public static class Builder extends User.Builder<Builder> { ... }
 *
 * Questo elimina la ripetizione di username, email, name, surname,
 * passwordHash, description, active, createdAt in ogni sottoclasse.
 */
public abstract class User {

    private final String username;
    private final String email;
    private final String name;
    private final String surname;
    private final String passwordHash;
    private final Role role;
    private String description;
    private boolean isActive;
    private final LocalDateTime createdAt;

    // ----------------------------------------------------------------
    // Costruttore protetto — chiamato dai Builder delle sottoclassi
    // ----------------------------------------------------------------

    protected User(Builder<?> builder, Role role) {
        this.username     = builder.username;
        this.email        = builder.email;
        this.name         = builder.name;
        this.surname      = builder.surname;
        this.passwordHash = builder.passwordHash;
        this.role         = role;
        this.description  = builder.description;
        this.isActive     = builder.active;
        this.createdAt    = builder.createdAt;
    }

    // ----------------------------------------------------------------
    // Builder astratto generico
    // ----------------------------------------------------------------

    /**
     * Builder astratto parametrico.
     * Il parametro T consente il method chaining nelle sottoclassi:
     *   new Student.Builder().username("x").email("y").budget(...).build()
     * senza dover fare cast.
     *
     * @param <T> tipo del Builder concreto della sottoclasse
     */
    @SuppressWarnings("unchecked")
    public abstract static class Builder<T extends Builder<T>> {

        private String username;
        private String email;
        private String name;
        private String surname;
        private String passwordHash;
        private String description;
        private boolean active;
        private LocalDateTime createdAt;

        public T username(String username) {
            this.username = username;
            return (T) this;
        }

        public T email(String email) {
            this.email = email;
            return (T) this;
        }

        public T name(String name) {
            this.name = name;
            return (T) this;
        }

        public T surname(String surname) {
            this.surname = surname;
            return (T) this;
        }

        public T passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return (T) this;
        }

        public T description(String description) {
            this.description = description;
            return (T) this;
        }

        public T active(boolean active) {
            this.active = active;
            return (T) this;
        }

        public T createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return (T) this;
        }
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