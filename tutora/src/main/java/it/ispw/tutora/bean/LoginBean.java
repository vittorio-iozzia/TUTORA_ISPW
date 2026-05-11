package it.ispw.tutora.bean;

/**
 * Bean di trasporto per il form di login.
 *
 * Contiene solo i dati strettamente necessari all'autenticazione:
 * username e password in chiaro. La password viene svuotata dal
 * Controller applicativo dopo la verifica, così non circola
 * ulteriormente nell'applicazione.
 *
 * Separata da UserBean per il principio di responsabilità singola:
 * UserBean trasporta i dati del profilo, LoginBean trasporta
 * solo le credenziali di accesso.
 */
public class LoginBean {
    private String username;
    private String password;

    public LoginBean(){}

    public LoginBean(String username, String password){
        this.username = username;
        this.password = password;
    }

    public String getUsername(){ return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword(){ return password;}
    public void setPassword(String password) { this.password = password; }

    /**
     * Svuota la password dopo la verifica.
     * Chiamato dal LoginController dopo matchesPassword().
     */
    public void clearPassword(){ this.password = null; }


}
