package it.ispw.tutora.exception;

public class LessonNotFoundException extends TutoraException {
    public LessonNotFoundException(int id){super("Lesson not found with lesson id:" + id);}
}
