package it.ispw.tutora.dao;

import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateLessonException;
import it.ispw.tutora.exception.LessonNotFoundException;
import it.ispw.tutora.model.Lesson;

import java.sql.Connection;
import java.util.List;


public interface LessonDao {

    void updateLesson(Connection conn, Lesson lesson)
            throws DatabaseException, LessonNotFoundException, DuplicateLessonException;

    int insertLesson(Connection conn, Lesson newlesson) throws DatabaseException, DuplicateLessonException;

    Lesson selectLesson(Connection conn, int id) throws DatabaseException, LessonNotFoundException;

    void updateStatus(Connection conn, int id, LessonStatus newStatus)
            throws DatabaseException, LessonNotFoundException;

    List<Lesson> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException;

    List<Lesson> findByTutorAndStatus(Connection conn, String tutorUsername,
                                      LessonStatus status)
            throws DatabaseException;

}
