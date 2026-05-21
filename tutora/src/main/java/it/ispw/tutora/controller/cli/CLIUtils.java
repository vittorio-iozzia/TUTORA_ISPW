package it.ispw.tutora.controller.cli;

import java.io.Console;
import java.util.Scanner;

/**
 * Utility statiche per la CLI di TUTORA.
 *
 * Gestisce:
 *  - Costanti ANSI (colori, stili) - ESC = \u001B
 *  - Output formattato (banner, header, separatori, messaggi)
 *  - Input da tastiera (testo, password, numeri interi)
 */
@SuppressWarnings("java:S106") // System.out e' intenzionale: CLIUtils e' il layer di output della CLI
public final class CLIUtils {

    // ----------------------------------------------------------------
    // Costanti ANSI  (ESC = \u001B, Unicode U+001B)
    // ----------------------------------------------------------------

    public static final String RESET   = "\u001B[0m";
    public static final String BOLD    = "\u001B[1m";
    public static final String DIM     = "\u001B[2m";
    public static final String RED     = "\u001B[31m";
    public static final String GREEN   = "\u001B[32m";
    public static final String YELLOW  = "\u001B[33m";
    public static final String BLUE    = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN    = "\u001B[36m";
    public static final String WHITE   = "\u001B[37m";

    private static final int LINE_WIDTH = 60;

    private CLIUtils() {}

    // ----------------------------------------------------------------
    // Banner e header
    // ----------------------------------------------------------------

    public static void printBanner() {
        System.out.println();
        System.out.println(BLUE + BOLD + "  ████████╗██╗   ██╗████████╗ ██████╗ ██████╗  █████╗ " + RESET);
        System.out.println(BLUE + BOLD + "     ██╔══╝██║   ██║╚══██╔══╝██╔═══██╗██╔══██╗██╔══██╗" + RESET);
        System.out.println(BLUE + BOLD + "     ██║   ██║   ██║   ██║   ██║   ██║██████╔╝███████║" + RESET);
        System.out.println(BLUE + BOLD + "     ██║   ██║   ██║   ██║   ██║   ██║██╔══██╗██╔══██║" + RESET);
        System.out.println(BLUE + BOLD + "     ██║   ╚██████╔╝   ██║   ╚██████╔╝██║  ██║██║  ██║" + RESET);
        System.out.println(BLUE + BOLD + "     ╚═╝    ╚═════╝    ╚═╝    ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝" + RESET);
        System.out.println(DIM +  "           Tutoring Platform — Command Line Interface" + RESET);
        System.out.println();
    }

    public static void printHeader(String title) {
        System.out.println();
        separator('=');
        int pad = (LINE_WIDTH - title.length()) / 2;
        System.out.println(BOLD + CYAN + " ".repeat(Math.max(0, pad)) + title + RESET);
        separator('=');
    }

    public static void separator(char ch) {
        System.out.println(DIM + String.valueOf(ch).repeat(LINE_WIDTH) + RESET);
    }

    public static void separator() {
        separator('-');
    }

    // ----------------------------------------------------------------
    // Messaggi formattati
    // ----------------------------------------------------------------

    public static void success(String msg) {
        System.out.println(GREEN + BOLD + "  [OK] " + RESET + GREEN + msg + RESET);
    }

    public static void error(String msg) {
        System.out.println(RED + BOLD + "  [ERR] " + RESET + RED + msg + RESET);
    }

    public static void info(String msg) {
        System.out.println(CYAN + "  [i] " + RESET + msg);
    }

    public static void warn(String msg) {
        System.out.println(YELLOW + "  [!] " + RESET + YELLOW + msg + RESET);
    }

    public static void menuItem(int n, String label) {
        System.out.printf("  %s[%d]%s %s%n", YELLOW + BOLD, n, RESET, label);
    }

    public static void field(String label, String value) {
        System.out.printf("  %-22s %s%s%s%n",
                DIM + label + RESET, BOLD, value != null ? value : "-", RESET);
    }

    // ----------------------------------------------------------------
    // Input - testo
    // ----------------------------------------------------------------

    /** Legge una riga non vuota. Ripete il prompt finche' l'input e' blank. */
    public static String readLine(Scanner sc, String prompt) {
        String line;
        do {
            System.out.print(CYAN + "  > " + RESET + prompt + ": ");
            System.out.flush();
            line = sc.nextLine().trim();
            if (line.isEmpty()) error("Il campo non puo' essere vuoto.");
        } while (line.isEmpty());
        return line;
    }

    /** Legge una riga opzionale (puo' essere vuota). */
    public static String readOptionalLine(Scanner sc, String prompt) {
        System.out.print(CYAN + "  > " + RESET + prompt + " (invio per saltare): ");
        System.out.flush();
        return sc.nextLine().trim();
    }

    // ----------------------------------------------------------------
    // Input - password
    // ----------------------------------------------------------------

    /**
     * Legge una password usando Console (oscurata) se disponibile,
     * altrimenti usa lo Scanner (visibile - tipico negli IDE).
     */
    public static String readPassword(Scanner sc, String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] pwd = console.readPassword(CYAN + "  > " + RESET + prompt + ": ");
            return pwd != null ? new String(pwd) : "";
        }
        System.out.print(CYAN + "  > " + RESET + prompt + ": ");
        System.out.flush();
        return sc.nextLine().trim();
    }

    // ----------------------------------------------------------------
    // Input - numero intero
    // ----------------------------------------------------------------

    /** Legge un intero nell'intervallo [min, max]. Ripete fino a input valido. */
    public static int readInt(Scanner sc, String prompt, int min, int max) {
        while (true) {
            System.out.print(CYAN + "  > " + RESET + prompt
                    + " [" + min + "-" + max + "]: ");
            System.out.flush();
            String raw = sc.nextLine().trim();
            try {
                int v = Integer.parseInt(raw);
                if (v >= min && v <= max) return v;
                error("Inserisci un numero tra " + min + " e " + max + ".");
            } catch (NumberFormatException e) {
                error("Input non valido. Inserisci un numero.");
            }
        }
    }

    // ----------------------------------------------------------------
    // Pausa e conferma
    // ----------------------------------------------------------------

    public static void pressEnter(Scanner sc) {
        System.out.print(DIM + "\n  Premi INVIO per continuare..." + RESET);
        System.out.flush();
        sc.nextLine();
    }

    public static boolean confirm(Scanner sc, String prompt) {
        System.out.print(CYAN + "  > " + RESET + prompt + " [s/N]: ");
        System.out.flush();
        String r = sc.nextLine().trim().toLowerCase();
        return r.equals("s") || r.equals("si") || r.equals("y") || r.equals("yes");
    }
}
