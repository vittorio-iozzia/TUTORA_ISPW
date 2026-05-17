package it.ispw.tutora.controller.cli;

import it.ispw.tutora.model.session.Session;
import it.ispw.tutora.model.session.SessionManager;

import java.util.Scanner;

import static it.ispw.tutora.controller.cli.CLIUtils.*;

/**
 * Orchestratore principale della CLI di TUTORA.
 *
 * Ciclo di vita:
 *  1. Mostra il banner e la schermata di login/registrazione
 *  2. Dopo il login, instrada verso il menu del ruolo appropriato
 *     (StudentCLI, TutorCLI, AdminCLI)
 *  3. Al logout ripresenta la schermata di autenticazione
 *  4. Se l'utente sceglie "Esci" il programma termina
 */
@SuppressWarnings("java:S106") // System.out è intenzionale: classe boundary della CLI
public class CLIRunner {

    private final Scanner     sc       = new Scanner(System.in);
    private final LoginCLI    loginCLI = new LoginCLI();
    private final StudentCLI  studentCLI = new StudentCLI();
    private final TutorCLI    tutorCLI   = new TutorCLI();
    private final AdminCLI    adminCLI   = new AdminCLI();

    /** Avvia il loop principale. Blocca finché l'utente non esce. */
    public void run() {
        while (true) {
            String token = loginCLI.show(sc);

            // L'utente ha scelto "Esci" dalla schermata di login
            if (token == null) {
                System.out.println();
                info("Grazie per aver usato TUTORA. A presto!");
                System.out.println();
                return;
            }

            // Instrada in base al ruolo
            Session session = SessionManager.getInstance().getSession(token);
            if (session == null) {
                error("Sessione non valida dopo il login. Riprova.");
                continue;
            }

            if (session.isStudent()) {
                studentCLI.run(sc, token);
            } else if (session.isTutor()) {
                tutorCLI.run(sc, token);
            } else if (session.isAdmin()) {
                adminCLI.run(sc, token);
            } else {
                error("Ruolo non riconosciuto: " + session.getUser().getClass().getSimpleName());
            }

            // Dopo il logout, il loop ricomincia dalla schermata di login
        }
    }
}
