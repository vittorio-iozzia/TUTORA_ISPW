package it.ispw.tutora.bean;

import it.ispw.tutora.enums.LessonStatus;
import it.ispw.tutora.model.Lesson;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bean di trasporto per la ricerca di lezioni disponibili da parte dello student.
 *
 * Trasporta i criteri di filtro dal boundary al Controller:
 *   - tutorUsername filtra le lezioni per tutor specifico (opzionale).
 *   - subCategoryName filtra per materia (opzionale).
 *   - startTime / endTime definiscono la finestra temporale di ricerca.
 *
 * Tutti i campi sono opzionali: il Controller ignora i campi null
 * e applica solo i filtri valorizzati dallo student.
 */
public class LessonStudentBean {

    private int id;
    private String tutorUsername;
    private String subCategoryName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<Lesson> list;
    private String errorMessage;


    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getTutorUsername() {
        return tutorUsername;
    }

    public void setTutorUsername(String tutorUsername) {
        this.tutorUsername = tutorUsername;
    }

    public String getSubCategoryName() {
        return subCategoryName;
    }

    public void setSubCategoryName(String subCategoryName) {
        this.subCategoryName = subCategoryName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }


    public List<Lesson> getList() {
        return list;
    }

    public void setList(List<Lesson> list) {
        this.list = list;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
