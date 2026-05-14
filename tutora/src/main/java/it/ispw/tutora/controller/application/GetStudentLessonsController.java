package it.ispw.tutora.controller.application;

import it.ispw.tutora.bean.BookingBean;
import it.ispw.tutora.dao.BookingDao;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.model.Booking;
import it.ispw.tutora.model.session.SessionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class GetStudentLessonsController {

    private final BookingDao bookingDao;

    public GetStudentLessonsController() {
        this.bookingDao = DaoFactory.getInstance().createBookingDao();
    }

    /**
     * Carica le prenotazioni dello student autenticato e popola la bean.
     * Se c'è un errore viene mostrata una lista vuota.
     */
    public List<Booking> loadBookings(BookingBean bean, String token) {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token) || !sm.getSession(token).isStudent()) {
            bean.setErrorMessage("Unauthorized.");
            return Collections.emptyList();
        }

        String username = sm.getCurrentUser(token).getUsername();
        try (Connection conn = DaoFactory.getInstance().getConnection()) {
            return bookingDao.findByStudent(conn, username);
        } catch (DatabaseException | SQLException e) {
            bean.setErrorMessage("Could not load lessons.");
            return Collections.emptyList();
        }
    }
}
