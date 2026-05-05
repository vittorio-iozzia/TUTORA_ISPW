package it.ispw.tutora;

import it.ispw.tutora.dao.factory.ConnectionFactory;
import it.ispw.tutora.exception.DatabaseException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestConnection {

    public static void main(String[] args) {
        try {
            Connection conn = ConnectionFactory.getInstance().getConnection();
            System.out.println("Connessione riuscita!");

            // Verifica che le tabelle esistano
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW TABLES");

            System.out.println("Tabelle trovate nel database:");
            while (rs.next()) {
                System.out.println("  - " + rs.getString(1));
            }

            rs.close();
            stmt.close();
            conn.close();
            System.out.println("Connessione chiusa correttamente.");

        } catch (DatabaseException e) {
            System.err.println("Errore di connessione: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Errore generico: " + e.getMessage());
        }
    }
}