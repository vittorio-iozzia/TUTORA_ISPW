package it.ispw.tutora.bean;

/**
 * Base comune per i bean che trasportano dati anagrafici di una persona.
 * Elimina la duplicazione di username/email/name/surname tra
 * {@link RegistrationBean} e {@link UserProfileBean}.
 */
public abstract class PersonBean {

    private String username;
    private String email;
    private String name;
    private String surname;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }
}
