package it.ispw.tutora;

import it.ispw.tutora.controller.cli.CLIRunner;
import it.ispw.tutora.dao.factory.DaoFactory;

import java.util.Scanner;

/**
 * Entry point per la modalità CLI di TUTORA.
 *
 * Avvio da IDE:
 *   Lancia direttamente questa classe come "Run Configuration"
 *   (non estende Application, quindi non ha il problema JavaFX).
 *
 * Avvio da terminale (dopo il build Maven):
 *   java -cp target/tutora-*.jar it.ispw.tutora.CLIApp
 *
 * Modalità DAO configurabile tramite DaoFactory (Demo / JSON / DB):
 *   Di default usa la stessa factory già configurata in TutoraApp.
 *   Per forzare la modalità Demo (senza DB) impostare la property:
 *     -Ddao.mode=demo
 */
public class CLIApp {

    public static void main(String[] args) {
        // Inizializza la DaoFactory con la stessa logica di TutoraApp
        // (Demo / JSON / DB in base alla configurazione corrente)
        DaoFactory.getInstance();

        new CLIRunner(new Scanner(System.in)).run();
    }
}
