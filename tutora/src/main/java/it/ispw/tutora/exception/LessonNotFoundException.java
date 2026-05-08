package it.ispw.tutora.exception;

public class LessonNotFoundException extends RuntimeException {
    public LessonNotFoundException(String message) {
        super(message);
    }
    public LessonNotFoundException(int id){super("Lesson not found with lesson id:" + id);}
}
