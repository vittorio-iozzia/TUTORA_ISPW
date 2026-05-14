package it.ispw.tutora.view.home;

import it.ispw.tutora.model.session.Session;

/**
 * Factory per la creazione del {@link DashboardComponent} corretto
 * in base al ruolo dell'utente autenticato.
 *
 * Costruisce sempre a partire da un {@link BaseDashboardComponent} e
 * avvolge il componente base nel decoratore appropriato.
 * Il costruttore è privato perché la classe espone solo metodi statici.
 */
public class DashboardFactory {

    private DashboardFactory() {}

    /**
     * Restituisce il decoratore di dashboard corrispondente al ruolo
     * dell'utente nella {@code session} fornita.
     */
    public static DashboardComponent create(Session session) {
        DashboardComponent base = new BaseDashboardComponent();
        if (session.isAdmin()) {
            return new AdminDashboardDecorator(base);
        } else if (session.isTutor()) {
            return new TutorDashboardDecorator(base);
        } else {
            return new StudentDashboardDecorator(base);
        }
    }
}
