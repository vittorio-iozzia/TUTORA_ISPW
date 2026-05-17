package it.ispw.tutora.bean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bean di trasporto per la creazione o modifica di una lezione da parte del tutor.
 *
 * Trasporta dal boundary al Controller i dati che il tutor deve fornire:
 *   - subcategoryName identifica l'expertise (subcategory) per cui si crea la lezione.
 *     Il Controller abbina questo campo con il tutor in sessione per recuperare
 *     la TutorExpertise corrispondente.
 *   - startTime / endTime definiscono l'intervallo temporale della lezione.
 *   - isRemote indica se la lezione si svolge in presenza o da remoto.
 *   - listedPrice è il prezzo orario proposto per questa lezione.
 *
 * Il tutor (username) non è nella bean: viene ricavato dal Controller
 * direttamente dalla sessione utente.
 */
public class LessonTutorBean {

    private String subcategoryName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isRemote;
    private BigDecimal listedPrice;


    public String getSubcategoryName() { return subcategoryName; }
    public void setSubcategoryName(String subcategoryName) { this.subcategoryName = subcategoryName; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public boolean isRemote() { return isRemote; }
    public void setRemote(boolean remote) { isRemote = remote; }

    public BigDecimal getListedPrice() { return listedPrice; }
    public void setListedPrice(BigDecimal listedPrice) { this.listedPrice = listedPrice; }

    private String errorMessage;
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
