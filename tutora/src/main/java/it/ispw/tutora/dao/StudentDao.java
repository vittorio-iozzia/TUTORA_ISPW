package it.ispw.tutora.dao;

import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.Student;

import java.math.BigDecimal;
import java.sql.Connection;

public interface StudentDao extends UserDao {
    void updateStudentBudget(Connection conn, String username, BigDecimal budget) throws DatabaseException, UserNotFoundException;
    Student selectStudent(Connection conn, String username) throws DatabaseException, UserNotFoundException;
}