package it.ispw.tutora.exception;

public class DuplicateLessonException extends TutoraException {
    public DuplicateLessonException(int id) {
        super("Lesson: " + id + "already present ");
    }
    public DuplicateLessonException(String mes) {
        super(mes);
    }
}
