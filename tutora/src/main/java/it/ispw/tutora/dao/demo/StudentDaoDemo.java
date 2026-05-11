package it.ispw.tutora.dao.demo;

import it.ispw.tutora.dao.StudentDao;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.UserNotFoundException;
import it.ispw.tutora.model.Student;
import it.ispw.tutora.model.User;

import java.math.BigDecimal;
import java.sql.Connection;


public class StudentDaoDemo extends UserDaoDemo implements StudentDao {

    @Override
    public void updateStudentBudget(Connection conn, String username, BigDecimal budget)
            throws DatabaseException, UserNotFoundException {
        Student student = selectStudent(conn, username);
        student.addBudget(budget);
    }
    @Override
    public Student selectStudent(Connection conn, String username)
            throws DatabaseException, UserNotFoundException {
        User user = findByUsername(conn, username);
        return (Student) user;
    }
}
