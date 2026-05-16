package it.ispw.tutora.controller.cli;

import java.io.Console;
import java.util.Scanner;

/**
 * Utility statiche per la CLI di TUTORA.
 *
 * Gestisce:
 *  - Costanti ANSI (colori, stili)
 *  - Output formattato (banner, header, separatori, messaggi)
 *  - Input da tastiera (testo, password, numeri interi)
 */
public final class CLIUtils {

    // ----------------------------------------------------------------
    // Costanti ANSI
    // ----------------------------------------------------------------

    public static final String RESET   = "[0m";
    public static final String BOLD    = "[1m";
    public static final String DIM     = "[2m";
    public static final String RED     = "[31m";
    public static final String GREEN   = "[32m";
    public static final String YELLOW  = "[33m";
    public static final String BLUE    = "[34m";
    public static final String MAGENTA = "[35m";
    public static final String CYAN    = "[36m";
    public static final String WHITE   = "[37m";

    private static final int LINE_WIDTH = 60;

    private CLIUtils() {}

    // ----------------------------------------------------------------
    // Banner e header
    // ----------------------------------------------------------------

    public static void printBanner() {
        System.out.println();
        System.out.println(BLUE + BOLD +
            "  ████████╗██╗   ██╗████████╗ ██████╗ ██████╗  █████╗ " + RESET);
        System.out.println(BLUE + BOLD +
            "     ██╔══╝██║   ██║╚══██╔══╝██╔═══██╗██╔══██╗██╔══██╗" + RESET);
        System.out.println(BLUE + BOLD +
            "     ██║   ██║   ██║   ██║   ██║   ██║██████╔╝███████║" + RESET);
        System.out.println(BLUE + BOLD +
            "     ██║   ██║   ██║   ██║   ██║   ██║██╔══██╗██╔══██║" + RESET);
        System.out.println(BLUE + BOLD +
            "     ██║   ╚██████╔╝   ██║   ╚██████╔╝██║  ██║██║  ██║" + RESET);
        System.out.println(BLUE + BOLD +
            "     ╚═╝    ╚═════╝    ╚═╝    ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝" + RESET);
        System.out.println(DIM + "           Tutoring Platform — Command Line Interface" + RESET);
        System.out.println();
    }

    public static void printHeader(String title) {
        System.out.println();
        separator('═');
        int pad = (LINE_WIDTH - title.length()) / 2;
        System.out.println(BOLD + CYAN + " ".repeat(Math.max(0, pad)) + title + RESET);
        separator('═');
    }

    public static void separator(char ch) {
        System.out.println(DIM + String.valueOf(ch).repeat(LINE_WIDTH) + RESET);
    }

    public static void separator() {
        separator('─');
    }

    // ----------------------------------------------------------------
    // Messaggi formattati
    // ----------------------------------------------------------------

    public static void success(String msg) {
        System.out.println(GREEN + BOLD + "  ✓  " + RESET + GREEN + msg + RESET);
    }

    public static void error(String msg) {
        System.out.println(RED + BOLD + "  ✗  " + RESET + RED + msg + RESET);
    }

    public static void info(String msg) {
        System.out.println(CYAN + "  ℹ  " + RESET + msg);
    }

    public static void warn(String msg) {
        System.out.println(YELLOW + "  ⚠  " + RESET + YELLOW + msg + RESET);
    }

    public static void menuItem(int n, String label) {
        System.out.printf("  %s[%d]%s %s%n", YELLOW + BOLD, n, RESET, label);
    }

    public static void field(String label, String value) {
        System.out.printf("  %-20s %s%s%s%n",
                DIM + label + RESET, BOLD, value != null ? value : "—", RESET);
    }

    // ----------------------------------------------------------------
    // Input — testo
    // ----------------------------------------------------------------

    /**
     * Legge una riga non vuota. Ripete il prompt finché l'input è blank.
     */
    public static String readLine(Scanner sc, String prompt) {
        String line;
        do {
            System.out.print(CYAN + "  > " + RESET + prompt + ": ");
            line = sc.nextLine().trim();
            if (line.isEmpty()) error("Il campo non può essere vuoto.");
        } while (line.isEmpty());
        return line;
    }

    /**
     * Legge una riga opzionale (può essere vuota).
     */
    public static String readOptionalLine(Scanner sc, String prompt) {
        System.out.print(CYAN + "  > " + RESET + prompt + " (invio per saltare): ");
        return sc.nextLine().trim();
    }

    // ----------------------------------------------------------------
    // Input — password
    // ----------------------------------------------------------------

    /**
     * Legge una password usando Console (oscurata) se disponibile,
     * altrimenti usa lo Scanner visibile.
     */
    public static String readPassword(Scanner sc, String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] pwd = console.readPassword(CYAN + "  > " + RESET + prompt + ": ");
            return pwd != null ? new String(pwd) : "";
        }
        // Fallback: visibile (ambienti IDE)
        System.out.print(CYAN + "  > " + RESET + prompt + ": ");
        return sc.nextLine().trim();
    }

    // ----------------------------------------------------------------
    // Input — numero intero
    // ----------------------------------------------------------------

    /**
     * Legge un intero nell'intervallo [min, max]. Ripete fino a input valido.
     */
    public static int readInt(Scanner sc, String prompt, int min, int max) {
        while (true) {
            System.out.print(CYAN + "  > " + RESET + prompt
                    + " [" + min + "-" + max + "]: ");
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

    /**
     * Legge un intero >= min senza limite superiore.
     */
    public static int readInt(Scanner sc, String prompt, int min) {
        return readInt(sc, prompt, min, Integer.MAX_VALUE);
    }

    // ----------------------------------------------------------------
    // Pausa
    // ----------------------------------------------------------------

    public static void pressEnter(Scanner sc) {
        System.out.print(DIM + "\n  Premi INVIO per continuare..." + RESET);
        sc.nextLine();
    }

    // ----------------------------------------------------------------
    // Conferma sì/no
    // ----------------------------------------------------------------

    public static boolean confirm(Scanner sc, String prompt) {
        System.out.print(CYAN + "  > " + RESET + prompt + " [s/N]: ");
        String r = sc.nextLine().trim().toLowerCase();
        return r.equals("s") || r.equals("si") || r.equals("sì") || r.equals("y") || r.equals("yes");
    }
}
