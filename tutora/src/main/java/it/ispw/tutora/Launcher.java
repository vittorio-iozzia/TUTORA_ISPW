 package it.ispw.tutora;

/**
 * Entry point per l'avvio dall'IDE e da JAR eseguibile.
 *
 * -----------------------------------------------------------------------
 * Perché questa classe esiste
 * -----------------------------------------------------------------------
 * In Java 11+ con JavaFX come dipendenza Maven (classpath, non module-path),
 * avviare direttamente una classe che estende javafx.application.Application
 * causa "JavaFX runtime components are missing": la JVM controlla la presenza
 * del modulo javafx.application prima ancora di eseguire main().
 *
 * Questa classe non estende Application, quindi il controllo non viene
 * eseguito all'avvio. JavaFX viene caricato normalmente dal classpath
 * quando TutoraApp.main() chiama Application.launch().
 */
public class Launcher {

    public static void main(String[] args) {
        TutoraApp.main(args);
    }
}
