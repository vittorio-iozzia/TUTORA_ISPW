package it.ispw.tutora.boundary;

import it.ispw.tutora.bean.ApplicationItemBean;
import it.ispw.tutora.exception.ValidationServiceException;
import it.ispw.tutora.exception.ValidationTimeoutException;

import java.util.List;

/**
 * Interfaccia Target del pattern Adapter.
 *
 * -----------------------------------------------------------------------
 * Pattern Adapter (GoF – Strutturale)
 * -----------------------------------------------------------------------
 * Definisce l'interfaccia che il Controller applicativo si aspetta
 * per la validazione dei documenti. Il Controller dipende solo da
 * questa interfaccia — non conosce né il protocollo HTTP né i dettagli
 * del servizio esterno di validazione.
 *
 * L'implementazione concreta ({@link CertificateValidationBoundary}) adatta
 * la chiamata HTTP dell'API esterna a questa interfaccia semplice.
 */
public interface CertificateValidator {

    /**
     * Valida i documenti allegati all'application.
     *
     * @throws ValidationTimeoutException  se il servizio non risponde entro il timeout
     * @throws ValidationServiceException  se il servizio restituisce un errore interno
     */
    boolean validateDocuments(List<ApplicationItemBean> items)
            throws ValidationTimeoutException, ValidationServiceException;
}