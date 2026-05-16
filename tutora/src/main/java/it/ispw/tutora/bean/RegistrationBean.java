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
public class RegistrationBean extends PersonBean {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private String password;
    private String confirmPassword;
    private boolean success;
    private String errorMessage;

    // ----------------------------------------------------------------
    // Validazione sintattica (no logica di business)
    // ----------------------------------------------------------------

    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }

    public boolean isPasswordValid() {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    public boolean isComplete() {
        return getUsername() != null && !getUsername().isBlank()
                && getEmail() != null && !getEmail().isBlank()
                && getName() != null && !getName().isBlank()
                && getSurname() != null && !getSurname().isBlank()
                && password != null && !password.isBlank();
    }

    public void clearPassword() {
        this.password = null;
        this.confirmPassword = null;
    }

    // ----------------------------------------------------------------
    // Getter e setter
    // ----------------------------------------------------------------

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
