package it.ispw.tutora.view.home;

import javafx.scene.layout.VBox;

/**
 * Decoratore astratto della dashboard.
 *
 * Mantiene il riferimento al componente avvolto e delega la chiamata
 * a {@link #decorateContent} prima che le sottoclassi aggiungano i
 * propri controlli, garantendo la catena di decorazione.
 */
public abstract class DashboardDecorator implements DashboardComponent {

    /** Componente avvolto da questo decoratore. */
    protected final DashboardComponent wrapped;


    protected DashboardDecorator(DashboardComponent wrapped) {
        this.wrapped = wrapped;
    }

    /**
     * Delega al componente avvolto, poi le sottoclassi aggiungono
     * i propri elementi sovrascrivendo questo metodo con {@code super}.d
     */
    @Override
    public void decorateContent(VBox contentArea) {
        wrapped.decorateContent(contentArea);
    }
}
