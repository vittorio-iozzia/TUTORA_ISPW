package it.ispw.tutora.bean;

import it.ispw.tutora.model.Booking;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Bean di trasporto per la pagina "My Lessons" dello student.
 *
 * Viene popolato da {@link it.ispw.tutora.controller.application.GetStudentLessonsController}
 * e consumato da {@code MyLessonsGfxController} soltanto per la visualizzazione.
 *
 * Separazione BCE: tutta la logica di filtro (LessonStatus) e le aggregazioni
 * (ore totali, spesa totale) sono calcolate nel Controller applicativo;
 * il boundary riceve dati già pronti e si occupa solo della resa grafica.
 */
public class MyLessonsBean {

    private List<Booking> upcoming = Collections.emptyList();
    private List<Booking> past     = Collections.emptyList();
    private long          totalMinutes = 0L;
    private BigDecimal    totalSpent   = BigDecimal.ZERO;

    public List<Booking> getUpcoming()      { return upcoming; }
    public List<Booking> getPast()          { return past; }
    public long          getTotalMinutes()  { return totalMinutes; }
    public BigDecimal    getTotalSpent()    { return totalSpent; }

    public void setUpcoming(List<Booking> upcoming)         { this.upcoming     = upcoming; }
    public void setPast(List<Booking> past)                 { this.past         = past; }
    public void setTotalMinutes(long totalMinutes)          { this.totalMinutes = totalMinutes; }
    public void setTotalSpent(BigDecimal totalSpent)        { this.totalSpent   = totalSpent; }
}
