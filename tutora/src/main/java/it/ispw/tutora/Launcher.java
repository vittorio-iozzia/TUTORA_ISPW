package it.ispw.tutora;

import it.ispw.tutora.controller.cli.CLIRunner;

import java.util.Scanner;

/**
 * Entry point per l'avvio dall'IDE e da JAR eseguibile.
 *
 * -----------------------------------------------------------------------
 * Perche' questa classe esiste
 * -----------------------------------------------------------------------
 * In Java 11+ con JavaFX come dipendenza Maven (classpath, non module-path),
 * avviare direttamente una classe che estende javafx.application.Application
 * causa "JavaFX runtime components are missing": la JVM controlla la presenza
 * del modulo javafx.application prima ancora di eseguire main().
 *
 * Questa classe non estende Application, quindi il controllo non viene
 * eseguito all'avvio. JavaFX viene caricato normalmente dal classpath
 * quando TutoraApp.main() chiama Application.launch().
 *
 * -----------------------------------------------------------------------
 * Selezione interfaccia e persistenza da terminale
 * -----------------------------------------------------------------------
 * Argomenti opzionali (qualsiasi ordine):
 *   --ui=GFX|CLI
 *   --dao=DB|DEMO|JSON
 *
 * Se un argomento e' assente viene chiesto interattivamente.
 * La scelta del DAO viene propagata a DaoFactory tramite system property.
 *
 * Esempi:
 *   mvn javafx:run                              -> prompt interattivo
 *   mvn javafx:run -Djavafx.args="--ui=CLI --dao=DEMO"
 *   java -jar tutora.jar --ui=GFX --dao=JSON
 */
@SuppressWarnings("java:S106") // System.out intenzionale: classe boundary di avvio
public class Launcher {

    private static final String ARG_UI  = "--ui=";
    private static final String ARG_DAO = "--dao=";

    public static void main(String[] args) {

        String ui  = parseArg(args, ARG_UI);
        String dao = parseArg(args, ARG_DAO);

        // Scanner unico su System.in: viene riusato dal CLIRunner per evitare
        // il problema del doppio buffer (due Scanner sullo stesso stream).
        Scanner sc = new Scanner(System.in);

        // Se uno o entrambi i parametri mancano, chiedi interattivamente.
        // Il prompt avviene PRIMA di Application.launch() perche' launch() e'
        // bloccante e avvia il thread JavaFX: da quel momento stdin non e' piu' utile.
        if (ui == null)  ui  = askUi(sc);
        if (dao == null) dao = askDao(sc);

        // Propaga la scelta DAO a DaoFactory (legge questa property con priorita'
        // su app.properties, che rimane il fallback per chi lancia dall'IDE).
        System.setProperty("DAO_TYPE", dao.toUpperCase());

        if ("CLI".equalsIgnoreCase(ui)) {
            new CLIRunner(sc).run();
        } else {
            TutoraApp.main(args);
        }
    }

    // ----------------------------------------------------------------
    // Parsing argomenti
    // ----------------------------------------------------------------

    private static String parseArg(String[] args, String prefix) {
        for (String arg : args) {
            if (arg.toLowerCase().startsWith(prefix.toLowerCase())) {
                return arg.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    // ----------------------------------------------------------------
    // ANSI colors
    // ----------------------------------------------------------------

    private static final String CYAN    = "[36m";
    private static final String BOLD    = "[1m";
    private static final String GREEN   = "[32m";
    private static final String RESET   = "[0m";

    // ----------------------------------------------------------------
    // Prompt interattivo
    // ----------------------------------------------------------------

    private static String askUi(Scanner sc) {
        System.out.println();
        System.out.println(CYAN + "  +--------------------------------+" + RESET);
        System.out.println(CYAN + "  |            TUTORA              |" + RESET);
        System.out.println(CYAN + "  +--------------------------------+" + RESET);
        System.out.println();
        System.out.println("  Seleziona interfaccia:");
        System.out.println("    1) Grafica (JavaFX)");
        System.out.println("    2) Testuale (CLI)");
        System.out.println();
        System.out.print("  Scelta [1 o 2]: ");
        System.out.flush();

        String input = sc.nextLine().trim();
        String scelta = "2".equals(input) ? "CLI" : "GFX";
        System.out.println(GREEN + "  >> " + ("CLI".equals(scelta) ? "Testuale (CLI)" : "Grafica (JavaFX)") + " selezionata." + RESET);
        System.out.println();
        return scelta;
    }

    private static String askDao(Scanner sc) {
        System.out.println("  Seleziona modalita di persistenza:");
        System.out.println("    1) Demo  (in-memory, nessun DB)");
        System.out.println("    2) JSON  (file su disco)");
        System.out.println("    3) DB    (MySQL)");
        System.out.println();
        System.out.print("  Scelta [1, 2 o 3]: ");
        System.out.flush();

        String input = sc.nextLine().trim();
        String dao = switch (input) {
            case "2" -> "JSON";
            case "3" -> "DB";
            default  -> "DEMO";
        };
        System.out.println(GREEN + "  >> Persistenza: " + dao + " selezionata." + RESET);
        System.out.println();
        return dao;
    }
}
