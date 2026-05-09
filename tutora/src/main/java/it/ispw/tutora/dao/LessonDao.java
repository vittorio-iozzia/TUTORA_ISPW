package it.ispw.tutora.dao;

import java.sql.Connection;
import java.time.LocalDateTime;

public interface LessonDao {

    void updateLesson(Connection conn, int id, LocalDateTime start, LocalDateTime end);
}
