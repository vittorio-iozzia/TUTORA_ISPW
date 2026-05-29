package it.ispw.tutora.view;

import javafx.application.Platform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Utility statica per la gestione delle immagini profilo.
 *
 * Mantiene una mappa username → path assoluto del file immagine
 * e notifica tutti i listener registrati ogni volta che un avatar
 * viene aggiornato (es. HomeGfxController sincronizza sidebar e header).
 *
 * Persistenza: il mapping viene salvato su disco in
 * {@value AVATARS_FILE} ad ogni {@link #setAvatarPath} e ricaricato
 * automaticamente al primo accesso. Questo garantisce che un avatar
 * impostato in una sessione precedente sia visibile anche nelle
 * sessioni successive e nella schermata "Find Tutors" per altri utenti.
 *
 * Thread-safe: può ricevere setAvatarPath() da qualunque thread;
 * i listener vengono sempre invocati sull'FX Application Thread.
 */
public final class AvatarManager {

    /** File di persistenza condiviso con i DAO JSON (stessa working directory). */
    private static final String AVATARS_FILE = "../tutora_data/avatars.properties";

    /** username → percorso assoluto del file immagine */
    private static final Map<String, String> paths = new ConcurrentHashMap<>();

    /** Listener da notificare ad ogni cambio avatar */
    private static final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

    /**
     * Flag di persistenza su disco.
     * {@code true} (default) = DB/JSON mode: i path vengono salvati e ricaricati.
     * {@code false} = Demo mode: tutto in-memory, nessuna lettura/scrittura su disco.
     *
     * Impostato dall'entry point {@link it.ispw.tutora.TutoraApp} prima che
     * la prima schermata venga mostrata, tramite {@link #setDiskPersistenceEnabled}.
     */
    private static volatile boolean diskEnabled = true;

    static {
        // Carica i path salvati nelle sessioni precedenti al primo accesso alla classe.
        // In demo mode verrà sovrascitto subito da TutoraApp tramite setDiskPersistenceEnabled(false).
        loadFromDisk();
    }

    private AvatarManager() {}

    // ----------------------------------------------------------------
    // Scrittura / lettura
    // ----------------------------------------------------------------

    /**
     * Imposta (o aggiorna) il percorso dell'immagine profilo per un utente,
     * persiste su disco e notifica tutti i listener sull'FX Thread.
     */
    public static void setAvatarPath(String username, String filePath) {
        paths.put(username, filePath);
        saveToDisk();
        if (Platform.isFxApplicationThread()) {
            listeners.forEach(Runnable::run);
        } else {
            Platform.runLater(() -> listeners.forEach(Runnable::run));
        }
    }

    /**
     * Restituisce il percorso del file immagine, o {@code null} se non impostato.
     */
    public static String getAvatarPath(String username) {
        return paths.get(username);
    }

    /**
     * @return {@code true} se l'utente ha un avatar impostato e il file esiste ancora.
     */
    public static boolean hasAvatar(String username) {
        String p = paths.get(username);
        if (p == null || p.isBlank()) return false;
        // Verifica che il file sia ancora presente sul filesystem
        return new File(p).exists();
    }

    // ----------------------------------------------------------------
    // Listener
    // ----------------------------------------------------------------

    public static void addListener(Runnable r) {
        listeners.add(r);
    }

    public static void removeListener(Runnable r) {
        listeners.remove(r);
    }

    // ----------------------------------------------------------------
    // Controllo persistenza (demo vs. db/json mode)
    // ----------------------------------------------------------------

    /**
     * Abilita o disabilita la persistenza su disco degli avatar.
     *
     * <p>Deve essere chiamato dall'entry point dell'applicazione
     * ({@link it.ispw.tutora.TutoraApp}) prima che venga mostrata
     * la prima schermata, passando {@code false} in demo mode.</p>
     *
     * <p>Quando la persistenza viene <em>disabilitata</em>, la mappa
     * in-memory viene svuotata (rimuove eventuali dati caricati dallo
     * static initializer da sessioni precedenti) e i listener vengono
     * notificati per aggiornare le viste già presenti.</p>
     *
     * @param enabled {@code true} → usa disco (DB/JSON); {@code false} → solo in-memory (Demo)
     */
    public static void setDiskPersistenceEnabled(boolean enabled) {
        diskEnabled = enabled;
        if (!enabled) {
            paths.clear();   // rimuove qualsiasi avatar caricato da sessioni precedenti
            if (Platform.isFxApplicationThread()) {
                listeners.forEach(Runnable::run);
            } else {
                Platform.runLater(() -> listeners.forEach(Runnable::run));
            }
        }
    }

    // ----------------------------------------------------------------
    // Persistenza su disco
    // ----------------------------------------------------------------

    /**
     * Carica il mapping username→path dal file di persistenza.
     * Chiamato automaticamente al primo accesso alla classe.
     * Ignora silenziosamente eventuali errori di I/O
     * (la persistenza avatar è best-effort, non critica).
     */
    private static void loadFromDisk() {
        if (!diskEnabled) return;  // demo mode: solo in-memory, non leggere da disco
        File file = new File(AVATARS_FILE);
        if (!file.exists()) return;
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
            for (String key : props.stringPropertyNames()) {
                String val = props.getProperty(key);
                if (val != null && !val.isBlank()) {
                    paths.put(key, val);
                }
            }
        } catch (IOException ignored) {
            // Best-effort: se il file non è leggibile si parte da mappa vuota
        }
    }

    /**
     * Salva il mapping corrente su disco.
     * Chiamato automaticamente da {@link #setAvatarPath}.
     * Ignora silenziosamente eventuali errori di I/O.
     */
    private static void saveToDisk() {
        if (!diskEnabled) return;  // demo mode: solo in-memory, non scrivere su disco
        File file = new File(AVATARS_FILE);
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        Properties props = new Properties();
        paths.forEach(props::setProperty);
        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "TUTORA avatar paths — do not edit manually");
        } catch (IOException ignored) {
            // Best-effort: se il salvataggio fallisce l'app continua normalmente
        }
    }
}
