package it.ispw.tutora.controller.cli;

import it.ispw.tutora.bean.ApplicationItemBean;
import it.ispw.tutora.bean.CategoryBean;
import it.ispw.tutora.bean.RequirementBean;
import it.ispw.tutora.bean.TutorApplicationBean;
import it.ispw.tutora.controller.application.ApplyToBecomeATutorController;
import it.ispw.tutora.enums.ItemType;
import it.ispw.tutora.exception.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static it.ispw.tutora.controller.cli.CLIUtils.*;

/**
 * Schermata "Diventa tutor" per lo studente.
 * Guida l'utente nella compilazione e invio della candidatura:
 *  1. Scelta categoria  → pickCategory()
 *  2. Compilazione requisiti → collectItems()
 *  3. Conferma e invio → submitApplication()
 */
@SuppressWarnings("java:S106") // System.out è intenzionale: classe boundary della CLI
public class ApplyToBecomeATutorCLI {

    private final ApplyToBecomeATutorController ctrl = new ApplyToBecomeATutorController();

    // ----------------------------------------------------------------
    // Entry point — orchestrazione dei 3 passi
    // ----------------------------------------------------------------

    public void show(Scanner sc, String token) {
        printHeader("DIVENTA TUTOR");
        System.out.println();
        info("Compila la candidatura per diventare tutor in una categoria specifica.");
        info("I documenti saranno validati dal sistema prima dell'invio (max 3 min).");
        System.out.println();

        String catName = pickCategory(sc);
        if (catName == null) return;

        List<ApplicationItemBean> items = collectItems(sc, catName);
        if (items == null) return;

        submitApplication(sc, token, catName, items);
        pressEnter(sc);
    }

    // ----------------------------------------------------------------
    // Passo 1 — selezione categoria
    // ----------------------------------------------------------------

    /** Mostra le categorie e restituisce il nome scelto, o null se l'utente annulla. */
    private String pickCategory(Scanner sc) {
        List<CategoryBean> categories;
        try {
            categories = ctrl.loadCategories();
        } catch (DatabaseException e) {
            error("Impossibile caricare le categorie: " + e.getMessage());
            pressEnter(sc);
            return null;
        }
        if (categories.isEmpty()) {
            info("Nessuna categoria disponibile al momento.");
            pressEnter(sc);
            return null;
        }
        printCategoryMenu(categories);
        int idx = readInt(sc, "Categoria", 0, categories.size());
        return idx == 0 ? null : categories.get(idx - 1).getName();
    }

    private void printCategoryMenu(List<CategoryBean> categories) {
        System.out.println("  " + BOLD + "Seleziona la categoria:" + RESET);
        System.out.println();
        for (int i = 0; i < categories.size(); i++) {
            CategoryBean cat = categories.get(i);
            System.out.printf("  %s[%d]%s %-25s  %s%s%s%n",
                    YELLOW + BOLD, i + 1, RESET,
                    BOLD + cat.getName() + RESET,
                    DIM, cat.getDescription() != null ? cat.getDescription() : "", RESET);
        }
        menuItem(0, "Annulla");
        System.out.println();
    }

    // ----------------------------------------------------------------
    // Passo 2 — raccolta requisiti
    // ----------------------------------------------------------------

    /** Raccoglie la risposta a ogni requisito. Restituisce null in caso di errore. */
    private List<ApplicationItemBean> collectItems(Scanner sc, String catName) {
        List<RequirementBean> requisiti;
        try {
            requisiti = ctrl.loadRequirements(catName);
        } catch (DatabaseException | CategoryNotFoundException e) {
            error("Impossibile caricare i requisiti: " + e.getMessage());
            pressEnter(sc);
            return null;
        }
        printHeader("COMPILA CANDIDATURA — " + catName);
        System.out.println();
        List<ApplicationItemBean> items = new ArrayList<>();
        for (RequirementBean req : requisiti) {
            printRequirementHeader(req);
            ApplicationItemBean item = buildItem(sc, req);
            if (item != null) items.add(item);
            System.out.println();
        }
        return items;
    }

    private void printRequirementHeader(RequirementBean req) {
        System.out.println("  " + BOLD + req.getLabel() + RESET
                + (req.isRequired() ? " " + RED + "(obbligatorio)" + RESET
                                    : DIM + " (opzionale)" + RESET));
        if (req.getDescription() != null && !req.getDescription().isBlank()) {
            System.out.println("  " + DIM + req.getDescription() + RESET);
        }
    }

    /** Costruisce l'item per un requisito (TEXT o DOCUMENT). Null = opzionale saltato. */
    private ApplicationItemBean buildItem(Scanner sc, RequirementBean req) {
        if (req.getItemType() == ItemType.TEXT) {
            ApplicationItemBean item = new ApplicationItemBean();
            item.setRequirementName(req.getName());
            item.setItemType(req.getItemType());
            item.setTextContent(readTextAnswer(sc, req));
            return item;
        }
        return readDocumentItem(sc, req);
    }

    private String readTextAnswer(Scanner sc, RequirementBean req) {
        String hint = (req.getMinChar() > 0 || req.getMaxLength() > 0)
                ? " (min " + req.getMinChar() + ", max " + req.getMaxLength() + " caratteri)"
                : "";
        return req.isRequired()
                ? readLine(sc, "Risposta" + hint)
                : readOptionalLine(sc, "Risposta" + hint);
    }

    // ----------------------------------------------------------------
    // Gestione documento
    // ----------------------------------------------------------------

    /** Ciclo di lettura percorso file. Restituisce null se opzionale e saltato. */
    private ApplicationItemBean readDocumentItem(Scanner sc, RequirementBean req) {
        String userHome = System.getProperty("user.home");
        String cwd      = new File("").getAbsolutePath();
        printDocumentHint(userHome);
        ApplicationItemBean result = null;
        String percorso = readDocumentPath(sc, req, userHome);
        while (percorso != null && result == null) {
            File file = resolveFile(percorso, userHome, cwd);
            if (file == null) {
                printFileNotFound(percorso, userHome, cwd);
            } else {
                result = tryLoadFile(req, file);
            }
            if (result == null) {
                percorso = readDocumentPath(sc, req, userHome);
            }
        }
        return result;
    }

    private void printDocumentHint(String userHome) {
        System.out.println();
        info("Inserisci il percorso COMPLETO del file (PDF, JPG, PNG, DOCX...)");
        System.out.println("    La tua home e': " + userHome);
        System.out.println("    Esempio Windows: " + userHome + "\\Desktop\\diploma.pdf");
        System.out.println("    Esempio Mac/Lin: " + userHome + "/Desktop/diploma.pdf");
        System.out.println("    (puoi anche trascinare il file nel terminale se supportato)");
        System.out.println();
        System.out.flush();
    }

    /** Legge il percorso da tastiera, normalizza e gestisce il caso opzionale. Null = saltato. */
    private String readDocumentPath(Scanner sc, RequirementBean req, String userHome) {
        String percorso;
        if (req.isRequired()) {
            percorso = readLine(sc, "Percorso file");
        } else {
            percorso = readOptionalLine(sc, "Percorso file");
            if (percorso.isEmpty()) return null;
        }
        percorso = percorso.trim().replace("\"", "").replace("'", "").trim();
        return percorso.startsWith("~") ? userHome + percorso.substring(1) : percorso;
    }

    private void printFileNotFound(String percorso, String userHome, String cwd) {
        error("File non trovato. Percorsi tentati:");
        printCandidate(1, percorso);
        printCandidate(2, new File(userHome, percorso).getPath());
        printCandidate(3, new File(cwd, percorso).getPath());
        warn("Copia il percorso COMPLETO dal file manager, incl. lettera unita' (es. C:\\...)");
    }

    /** Carica il file nel bean. Restituisce null se la lettura fallisce (riprova). */
    private ApplicationItemBean tryLoadFile(RequirementBean req, File file) {
        try {
            byte[] content = Files.readAllBytes(file.toPath());
            ApplicationItemBean item = new ApplicationItemBean();
            item.setRequirementName(req.getName());
            item.setItemType(req.getItemType());
            item.setDocumentPath(file.getAbsolutePath());
            item.setOriginalFilename(file.getName());
            item.setSizeBytes(file.length());
            item.setContent(content);
            item.setMimeType(guessMimeType(file.getName()));
            success("File caricato: " + file.getName() + " (" + file.length() / 1024 + " KB)");
            return item;
        } catch (IOException e) {
            error("Impossibile leggere il file: " + e.getMessage());
            return null;
        }
    }

    // ----------------------------------------------------------------
    // Passo 3 — conferma e invio
    // ----------------------------------------------------------------

    private void submitApplication(Scanner sc, String token,
                                   String catName, List<ApplicationItemBean> items) {
        separator();
        System.out.println("  " + BOLD + "Riepilogo candidatura:" + RESET);
        field("Categoria:", catName);
        field("Requisiti compilati:", String.valueOf(items.size()));
        System.out.println();
        if (!confirm(sc, "Inviare la candidatura? (la validazione potrebbe richiedere qualche minuto)")) {
            info("Candidatura annullata.");
            return;
        }
        TutorApplicationBean bean = new TutorApplicationBean();
        bean.setCategoryName(catName);
        bean.setItems(items);
        info("Validazione documenti in corso...");
        doSubmit(bean, token);
    }

    private void doSubmit(TutorApplicationBean bean, String token) {
        try {
            int id = ctrl.submitApplication(bean, token);
            success("Candidatura inviata con successo! ID: " + id);
            info("L'admin esaminerà la tua candidatura e riceverai una notifica.");
        } catch (InvalidDocumentException e) {
            error("Documenti non validi: " + e.getMessage());
        } catch (DuplicateApplicationException e) {
            error("Hai già una candidatura attiva per questa categoria.");
        } catch (ValidationTimeoutException e) {
            error("Timeout validazione. Riprova più tardi.");
        } catch (ValidationServiceException e) {
            error("Servizio di validazione non disponibile: " + e.getMessage());
        } catch (AuthenticationException e) {
            error("Sessione non valida. Effettua nuovamente il login.");
        } catch (DatabaseException e) {
            error("Errore di sistema: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Utility di risoluzione percorso
    // ----------------------------------------------------------------

    private File resolveFile(String percorso, String userHome, String cwd) {
        File[] candidates = {
            new File(percorso),
            new File(userHome, percorso),
            new File(cwd, percorso)
        };
        for (File f : candidates) {
            try {
                File canonical = f.getCanonicalFile();
                if (canonical.exists() && canonical.isFile()) return canonical;
            } catch (IOException e) {
                if (f.exists() && f.isFile()) return f.getAbsoluteFile();
            }
        }
        return null;
    }

    private void printCandidate(int n, String raw) {
        try {
            File f = new File(raw).getAbsoluteFile();
            String note = f.exists()
                    ? (f.isDirectory() ? " [ESISTE MA E' DIRECTORY]" : " [ESISTE]")
                    : " [NON TROVATO]";
            System.out.println("    " + n + ") " + f.getPath() + note);
        } catch (Exception e) {
            System.out.println("    " + n + ") " + raw + " [percorso non valido]");
        }
    }

    private String guessMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf"))  return "application/pdf";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        return "application/octet-stream";
    }
}
