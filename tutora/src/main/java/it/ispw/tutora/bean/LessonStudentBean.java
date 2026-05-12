package it.ispw.tutora.bean;

import java.time.LocalDateTime;

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

    private String tutorUsername;
    private String subCategoryName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public LessonStudentBean() {}

    public String getTutorUsername() { return tutorUsername; }
    public void setTutorUsername(String tutorUsername) { this.tutorUsername = tutorUsername; }

    public String getSubCategoryName() { return subCategoryName; }
    public void setSubCategoryName(String subCategoryName) { this.subCategoryName = subCategoryName; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
