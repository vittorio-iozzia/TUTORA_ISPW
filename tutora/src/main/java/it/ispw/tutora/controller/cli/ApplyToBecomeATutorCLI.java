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
 *  1. Scelta categoria
 *  2. Compilazione requisiti (testo o documento)
 *  3. Invio con validazione certificati
 */
public class ApplyToBecomeATutorCLI {

    private final ApplyToBecomeATutorController ctrl = new ApplyToBecomeATutorController();

    public void show(Scanner sc, String token) {
        printHeader("DIVENTA TUTOR");
        System.out.println();
        info("Compila la candidatura per diventare tutor in una categoria specifica.");
        info("I documenti saranno validati dal sistema prima dell'invio (max 3 min).");
        System.out.println();

        // --- Passo 1: carica categorie ---
        List<CategoryBean> categories;
        try {
            categories = ctrl.loadCategories();
        } catch (DatabaseException e) {
            error("Impossibile caricare le categorie: " + e.getMessage());
            pressEnter(sc);
            return;
        }

        if (categories.isEmpty()) {
            info("Nessuna categoria disponibile al momento.");
            pressEnter(sc);
            return;
        }

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

        int catIdx = readInt(sc, "Categoria", 0, categories.size());
        if (catIdx == 0) return;
        String catName = categories.get(catIdx - 1).getName();

        // --- Passo 2: carica requisiti ---
        List<RequirementBean> requisiti;
        try {
            requisiti = ctrl.loadRequirements(catName);
        } catch (DatabaseException | CategoryNotFoundException e) {
            error("Impossibile caricare i requisiti: " + e.getMessage());
            pressEnter(sc);
            return;
        }

        printHeader("COMPILA CANDIDATURA — " + catName);
        System.out.println();

        List<ApplicationItemBean> items = new ArrayList<>();
        for (RequirementBean req : requisiti) {
            System.out.println("  " + BOLD + req.getLabel() + RESET
                    + (req.isRequired() ? " " + RED + "(obbligatorio)" + RESET : DIM + " (opzionale)" + RESET));
            if (req.getDescription() != null && !req.getDescription().isBlank()) {
                System.out.println("  " + DIM + req.getDescription() + RESET);
            }

            ApplicationItemBean item = new ApplicationItemBean();
            item.setRequirementName(req.getName());
            item.setItemType(req.getItemType());

            if (req.getItemType() == ItemType.TEXT) {
                // Requisito di tipo testo
                String hint = "";
                if (req.getMinChar() > 0 || req.getMaxLength() > 0) {
                    hint = " (min " + req.getMinChar() + ", max " + req.getMaxLength() + " caratteri)";
                }
                String testo;
                if (req.isRequired()) {
                    testo = readLine(sc, "Risposta" + hint);
                } else {
                    testo = readOptionalLine(sc, "Risposta" + hint);
                }
                item.setTextContent(testo);

            } else {
                // Requisito di tipo documento
                System.out.println();
                info("Fornisci il percorso assoluto del documento (PDF, JPG, PNG...)");
                String percorso;
                while (true) {
                    if (req.isRequired()) {
                        percorso = readLine(sc, "Percorso file");
                    } else {
                        percorso = readOptionalLine(sc, "Percorso file");
                        if (percorso.isEmpty()) {
                            // Requisito opzionale saltato
                            item = null;
                            break;
                        }
                    }
                    File file = new File(percorso);
                    if (!file.exists() || !file.isFile()) {
                        error("File non trovato: " + percorso);
                        continue;
                    }
                    try {
                        byte[] content = Files.readAllBytes(file.toPath());
                        item.setDocumentPath(percorso);
                        item.setOriginalFilename(file.getName());
                        item.setSizeBytes(file.length());
                        item.setContent(content);
                        item.setMimeType(guessMimeType(file.getName()));
                        success("File caricato: " + file.getName()
                                + " (" + file.length() / 1024 + " KB)");
                        break;
                    } catch (IOException e) {
                        error("Impossibile leggere il file: " + e.getMessage());
                    }
                }
                if (item == null) {
                    System.out.println();
                    continue; // opzionale saltato
                }
            }

            items.add(item);
            System.out.println();
        }

        // --- Passo 3: conferma e invio ---
        separator();
        System.out.println("  " + BOLD + "Riepilogo candidatura:" + RESET);
        field("Categoria:", catName);
        field("Requisiti compilati:", String.valueOf(items.size()));
        System.out.println();

        if (!confirm(sc, "Inviare la candidatura? (la validazione potrebbe richiedere qualche minuto)")) {
            info("Candidatura annullata.");
            pressEnter(sc);
            return;
        }

        TutorApplicationBean bean = new TutorApplicationBean();
        bean.setCategoryName(catName);
        bean.setItems(items);

        info("Validazione documenti in corso...");
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

        pressEnter(sc);
    }

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------

    private String guessMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf"))  return "application/pdf";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        return "application/octet-stream";
    }
}
