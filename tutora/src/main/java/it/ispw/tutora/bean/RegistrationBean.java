package it.ispw.tutora.bean;

/**
 * Bean di trasporto per il form di registrazione.
 *
 * La registrazione è riservata esclusivamente agli studenti.
 * Un utente che vuole diventare tutor deve prima registrarsi
 * come studente, poi candidarsi tramite UC-2 (Apply to Become a Tutor).
 * Gli admin vengono creati direttamente nel DB dallo sviluppatore.
 *
 * La password in chiaro viene svuotata dal Controller applicativo
 * dopo l'hashing con BCrypt.
 */
public class RegistrationBean {

    private String username;
    private String email;
    private String name;
    private String surname;
    private String password;
    private String confirmPassword;
    private boolean success;
    private String errorMessage;


    // ----------------------------------------------------------------
    // Validazione sintattica (no logica di business)
    // ----------------------------------------------------------------

    /**
     * Verifica che password e conferma password coincidano.
     * Controllo sintattico — appartiene al Bean, non al Model.
     */
    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }

    /**
     * Verifica che tutti i campi obbligatori siano compilati.
     */
    public boolean isComplete() {
        return username != null && !username.isBlank()
                && email != null && !email.isBlank()
                && name != null && !name.isBlank()
                && surname != null && !surname.isBlank()
                && password != null && !password.isBlank();
    }

    /**
     * Svuota la password dopo l'hashing.
     * Chiamato dal RegistrationController dopo BCrypt.hashpw().
     */
    public void clearPassword() {
        this.password        = null;
        this.confirmPassword = null;
    }

    // ----------------------------------------------------------------
    // Getter e setter
    // ----------------------------------------------------------------

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void   setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public boolean isSuccess() {
        return success;
    }
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public String getErrorMessage() {
        return errorMessage;
    }
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}