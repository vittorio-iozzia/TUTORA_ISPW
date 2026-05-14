package it.ispw.tutora.boundary;

import it.ispw.tutora.exception.PaymentException;
import it.ispw.tutora.exception.PaymentTimeoutException;

import java.math.BigDecimal;

/**
 * Interfaccia Target del pattern Adapter.
 *
 * -----------------------------------------------------------------------
 * Pattern Adapter (GoF – Strutturale)
 * -----------------------------------------------------------------------
 * Definisce l'interfaccia che il Controller si aspetta per il pagamento.
 * Il Controller dipende solo da questa interfaccia — non conosce né
 * PayPalClient né i dettagli del gateway esterno.
 *
 * L'implementazione concreta (PayPalAdapter) adatta la chiamata
 * dell'SDK esterno a questa interfaccia semplice.
 */
public interface PaymentGateway {

    /**
     * Elabora un pagamento per l'importo specificato.
     *
     * @param amount importo da addebitare — deve essere > 0
     * @return paymentRef riferimento univoco della transazione
     * @throws PaymentException        se il pagamento viene rifiutato
     *                                 o il sistema non è disponibile
     * @throws PaymentTimeoutException se il gateway non risponde
     *                                 entro il timeout di 10 minuti
     */
    String processPayment(BigDecimal amount)
            throws PaymentException, PaymentTimeoutException;

    /**
     * Rimborsa una transazione precedentemente completata.
     *
     * @param paymentRef riferimento della transazione originale da rimborsare
     * @param amount     importo da rimborsare — deve essere > 0
     * @return refundRef riferimento univoco del rimborso
     * @throws PaymentException        se il rimborso viene rifiutato
     *                                 o il sistema non è disponibile
     * @throws PaymentTimeoutException se il gateway non risponde
     *                                 entro il timeout di 10 minuti
     */
    String refund(String paymentRef, BigDecimal amount)
            throws PaymentException, PaymentTimeoutException;
}