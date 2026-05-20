package it.ispw.tutora.controller.cli;

import it.ispw.tutora.bean.LoginBean;
import it.ispw.tutora.bean.RegistrationBean;
import it.ispw.tutora.controller.application.LoginController;
import it.ispw.tutora.controller.application.RegistrationController;
import it.ispw.tutora.exception.AuthenticationException;
import it.ispw.tutora.exception.DatabaseException;

import java.util.Scanner;

import static it.ispw.tutora.controller.cli.CLIUtils.*;

/**
 * Schermata di autenticazione CLI.
 * Gestisce login e registrazione studente.
 */
@SuppressWarnings("java:S106") // System.out e' intenzionale: classe boundary della CLI
public class LoginCLI {

    private final LoginController        loginCtrl = new LoginController();
    private final RegistrationController regCtrl   = new RegistrationController();

    /**
     * Mostra il menu principale (Login / Registrati / Esci).
     * Ritorna il token di sessione se l'autenticazione ha successo,
     * oppure null se l'utente sceglie di uscire.
     */
    public String show(Scanner sc) {
        while (true) {
            printBanner();
            System.out.println("  " + BOLD + "Benvenuto in TUTORA" + RESET);
            System.out.println();
            menuItem(1, "Accedi");
            menuItem(2, "Registrati come studente");
            menuItem(0, "Esci");
            System.out.println();

            int scelta = readInt(sc, "Scelta", 0, 2);
            switch (scelta) {
                case 1 -> {
                    String token = doLogin(sc);
                    if (token != null) return token;
                }
                case 2 -> doRegister(sc);
                case 0 -> { return null; }
                default -> error("Scelta non valida.");
            }
        }
    }

    // ----------------------------------------------------------------
    // Login
    // ----------------------------------------------------------------

    private String doLogin(Scanner sc) {
        printHeader("ACCEDI");
        String username = readLine(sc, "Username");
        String password = readPassword(sc, "Password");

        LoginBean bean = new LoginBean();
        bean.setUsername(username);
        bean.setPassword(password);

        try {
            String token = loginCtrl.login(bean);
            success("Login effettuato con successo!");
            return token;
        } catch (AuthenticationException e) {
            error(e.getMessage());
            pressEnter(sc);
            return null;
        } catch (DatabaseException e) {
            error("Errore di sistema. Riprova piu' tardi.");
            pressEnter(sc);
            return null;
        }
    }

    // ----------------------------------------------------------------
    // Registrazione
    // ----------------------------------------------------------------

    private void doRegister(Scanner sc) {
        printHeader("REGISTRAZIONE STUDENTE");
        info("Tutti i campi sono obbligatori.");
        System.out.println();

        String username  = readLine(sc, "Username");
        String email     = readLine(sc, "Email");
        String nome      = readLine(sc, "Nome");
        String cognome   = readLine(sc, "Cognome");
        String password  = readPassword(sc, "Password (min 8 caratteri)");
        String conferma  = readPassword(sc, "Conferma password");

        RegistrationBean bean = new RegistrationBean();
        bean.setUsername(username);
        bean.setEmail(email);
        bean.setName(nome);
        bean.setSurname(cognome);
        bean.setPassword(password);
        bean.setConfirmPassword(conferma);

        regCtrl.register(bean);

        if (bean.isSuccess()) {
            success("Registrazione completata! Ora puoi effettuare il login.");
        } else {
            error(bean.getErrorMessage());
        }
        pressEnter(sc);
    }
}
