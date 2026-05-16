package it.ispw.tutora.view;

import javafx.application.Platform;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Utility statica per la gestione delle immagini profilo.
 *
 * Mantiene una mappa username → path assoluto del file immagine
 * e notifica tutti i listener registrati ogni volta che un avatar
 * viene aggiornato (es. HomeGfxController sincronizza sidebar e header).
 *
 * Thread-safe: può ricevere setAvatarPath() da qualunque thread;
 * i listener vengono sempre invocati sull'FX Application Thread.
 */
public final class AvatarManager {

    /** username → percorso assoluto del file immagine */
    private static final Map<String, String> paths = new ConcurrentHashMap<>();

    /** Listener da notificare ad ogni cambio avatar */
    private static final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

    private AvatarManager() {}

    // ----------------------------------------------------------------
    // Scrittura / lettura
    // ----------------------------------------------------------------

    /**
     * Imposta (o aggiorna) il percorso dell'immagine profilo per un utente.
     * Notifica tutti i listener sull'FX Thread.
     */
    public static void setAvatarPath(String username, String filePath) {
        paths.put(username, filePath);
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
     * @return {@code true} se l'utente ha un avatar impostato.
     */
    public static boolean hasAvatar(String username) {
        String p = paths.get(username);
        return p != null && !p.isBlank();
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
}
