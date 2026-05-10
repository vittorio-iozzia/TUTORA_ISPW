package it.ispw.tutora.bean;

/**
 * Bean di trasporto per il cambio password.
 *
 * La vecchia password viene verificata dal Controller prima di
 * applicare il cambio, così un utente non può modificare la password
 * di qualcun altro anche se accede alla schermata per errore.
 * Tutte le password vengono svuotate dopo l'uso tramite clearPasswords().
 */
public class ChangePasswordBean {

    private String username;
    private String oldPassword;
    private String newPassword;
    private String confirmNewPassword;

    public ChangePasswordBean() {}

    // ----------------------------------------------------------------
    // Validazione sintattica
    // ----------------------------------------------------------------

    /**
     * Verifica che nuova password e conferma coincidano.
     */
    public boolean passwordsMatch() {
        return newPassword != null && newPassword.equals(confirmNewPassword);
    }

    /**
     * Svuota tutte le password dopo l'elaborazione.
     * Chiamato dal Controller dopo BCrypt.hashpw().
     */
    public void clearPasswords() {
        this.oldPassword = null;
        this.newPassword = null;
        this.confirmNewPassword = null;
    }

    // ----------------------------------------------------------------
    // Getter e setter
    // ----------------------------------------------------------------

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmNewPassword() { return confirmNewPassword; }
    public void setConfirmNewPassword(String confirmNewPassword) { this.confirmNewPassword = confirmNewPassword; }
}
