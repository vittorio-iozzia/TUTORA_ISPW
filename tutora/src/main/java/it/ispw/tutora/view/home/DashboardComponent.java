package it.ispw.tutora.view.home;

import javafx.scene.layout.VBox;

/**
 * Componente radice del pattern Decorator per la dashboard.
 *
 * Ogni implementazione (concreta o decoratore) è responsabile di
 * popolare l'area principale della home con i controlli specifici
 * del ruolo dell'utente autenticato.
 */
public interface DashboardComponent {

    /**
     * Popola {@code contentArea} con i controlli specifici del ruolo.
     */
    void decorateContent(VBox contentArea);
}
