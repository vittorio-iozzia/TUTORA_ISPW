package it.ispw.tutora.bean;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bean di trasporto per la creazione o modifica di una expertise da parte del tutor.
 *
 * Trasporta dal boundary al Controller i dati che il tutor deve fornire:
 *   - subCategoryName identifica la subcategory dell'expertise.
 *   - hourlyPrice è il prezzo orario richiesto dal tutor.
 *   - expertiseTags è la lista dei nomi dei tag associati (es. "Sax", "Blues").
 *
 * Il tutor (username) non è nella bean: viene ricavato dal Controller
 * direttamente dalla sessione utente.
 *
 * I tag sono trasportati come List<String> (nomi) e non come List<Tag>:
 * la bean è un DTO tra View e Controller e non deve dipendere dal model layer.
 * Il Controller costruisce gli oggetti Tag dal nome prima di passarli al model.
 */
public class TutorExpertiseBean {

    private String subCategoryName;
    private BigDecimal hourlyPrice;
    // I tag vengono trasportati come nomi (stringhe); il Controller crea i Tag model.
    private List<String> expertiseTags;


    public String getSubCategoryName() { return subCategoryName; }
    public void setSubCategoryName(String subCategoryName) { this.subCategoryName = subCategoryName; }

    public BigDecimal getHourlyPrice() { return hourlyPrice; }
    public void setHourlyPrice(BigDecimal hourlyPrice) { this.hourlyPrice = hourlyPrice; }

    public List<String> getExpertiseTags() { return expertiseTags; }
    public void setExpertiseTags(List<String> expertiseTags) { this.expertiseTags = expertiseTags; }
}
