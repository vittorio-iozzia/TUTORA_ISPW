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
@SuppressWarnings("java:S106") // System.out e' intenzionale: classe boundary della CLI
public class CLIRunner {

    private final Scanner     sc;
    private final LoginCLI    loginCLI   = new LoginCLI();
    private final StudentCLI  studentCLI = new StudentCLI();
    private final TutorCLI    tutorCLI   = new TutorCLI();
    private final AdminCLI    adminCLI   = new AdminCLI();

    /**
     * Accetta lo Scanner gia' aperto su System.in.
     * Usare sempre lo stesso Scanner evita il problema dei buffer multipli.
     */
    public CLIRunner(Scanner sc) {
        this.sc = sc;
    }

    /** Avvia il loop principale. Blocca finche' l'utente non esce. */
    public void run() {
        // Svuota eventuali byte rimasti in stdin (newline extra iniettato da Maven
        // o dal prompt interattivo del Launcher) per evitare il doppio INVIO.
        try {
            while (System.in.available() > 0) System.in.read();
        } catch (Exception ignored) { /* non critico */ }
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
