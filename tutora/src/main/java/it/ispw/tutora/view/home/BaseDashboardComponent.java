package it.ispw.tutora.view.home;

import javafx.scene.layout.VBox;

/**
 * Implementazione concreta di base del pattern Decorator.
 *
 * Non aggiunge alcun controllo all'area di contenuto: si limita a
 * svuotarla per garantire uno stato pulito prima che i decoratori
 * concreti aggiungano i propri elementi.
 * Viene usata come punto di partenza obbligatorio dalla {@link DashboardFactory}.
 */
public class BaseDashboardComponent implements DashboardComponent {

    /**
     * Svuota {@code contentArea} senza aggiungere elementi.
     */
    @Override
    public void decorateContent(VBox contentArea) {
        contentArea.getChildren().clear();
    }
}
