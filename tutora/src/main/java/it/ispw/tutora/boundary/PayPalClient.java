package it.ispw.tutora.boundary;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adaptee del pattern Adapter.
 *
 * -----------------------------------------------------------------------
 * Pattern Adapter (GoF – Strutturale)
 * -----------------------------------------------------------------------
 * Rappresenta l'SDK esterno di PayPal con la sua interfaccia "incompatibile":
 * il metodo charge() usa nomi e tipi di ritorno diversi rispetto
 * all'interfaccia PaymentGateway che il Controller si aspetta.
 *
 * Il Controller non conosce mai questa classe — la vede solo attraverso
 * PaymentGateway (Target) tramite PayPalAdapter (Adapter).
 *
 * In produzione il corpo di charge() verrebbe sostituito con una
 * vera chiamata HTTP all'API PayPal senza toccare il Controller.
 *
 * -----------------------------------------------------------------------
 * Simulazione
 * -----------------------------------------------------------------------
 * Simula la chiamata HTTP con ritardo casuale (0-15 min) e probabilità:
 *   - 85% successo → restituisce transactionId
 *   - 10% rifiuto  → restituisce null
 *   - 5%  errore   → lancia RuntimeException
 */
public class PayPalClient {

    private static final Logger LOGGER = Logger.getLogger(
            PayPalClient.class.getName());

    private static final long MAX_SIMULATED_RESPONSE_MS = 900_000L;
    private static final double DECLINED_PROBABILITY = 0.10;
    private static final double ERROR_PROBABILITY    = 0.05;

    private final Random random = new Random();

    /**
     * Chiama l'SDK PayPal per elaborare il pagamento.
     * Interfaccia incompatibile con PaymentGateway:
     *   - si chiama charge() invece di processPayment()
     *   - restituisce null in caso di rifiuto invece di lanciare eccezioni Java
     *   - lancia RuntimeException invece di PaymentException
     *
     * @param amount importo da addebitare
     * @return transactionId se il pagamento è andato a buon fine, null se rifiutato
     * @throws InterruptedException se il thread viene interrotto durante l'attesa
     */
    public String charge(BigDecimal amount) throws InterruptedException {

        long responseTime = (long) (random.nextDouble() * MAX_SIMULATED_RESPONSE_MS);

        LOGGER.log(Level.INFO,
                "Calling PayPal API... (simulated delay: {0}ms)", responseTime);

        // Simula l'attesa della risposta del gateway
        Thread.sleep(responseTime);

        // Errore di sistema (5% dei casi) — lancia RuntimeException (non le nostre eccezioni)
        if (random.nextDouble() < ERROR_PROBABILITY) {
            throw new RuntimeException("PayPal service internal error.");
        }

        // Pagamento rifiutato (10% dei casi) — restituisce null invece di lanciare eccezione
        if (random.nextDouble() < DECLINED_PROBABILITY) {
            LOGGER.info("PayPal response: DECLINED");
            return null;
        }

        // Successo: genera il transactionId univoco
        String transactionId = "PAY-" + UUID.randomUUID().toString().toUpperCase();
        LOGGER.log(Level.INFO, "PayPal response: SUCCESS — {0}", transactionId);
        return transactionId;
    }

    /**
     * Chiama l'SDK PayPal per rimborsare una transazione precedente.
     * Stessa interfaccia incompatibile di charge():
     *   - restituisce null in caso di rifiuto invece di lanciare eccezioni Java
     *   - lancia RuntimeException invece di PaymentException
     *
     * @param transactionId riferimento della transazione originale da rimborsare
     * @param amount        importo da rimborsare
     * @return refundId se il rimborso è andato a buon fine, null se rifiutato
     * @throws InterruptedException se il thread viene interrotto durante l'attesa
     */
    public String refund(String transactionId, BigDecimal amount) throws InterruptedException {

        long responseTime = (long) (random.nextDouble() * MAX_SIMULATED_RESPONSE_MS);

        LOGGER.log(Level.INFO,
                "Calling PayPal Refund API for {0}... (simulated delay: {1}ms)",
                new Object[]{transactionId, responseTime});

        Thread.sleep(responseTime);

        if (random.nextDouble() < ERROR_PROBABILITY) {
            throw new RuntimeException("PayPal refund service internal error.");
        }

        if (random.nextDouble() < DECLINED_PROBABILITY) {
            LOGGER.info("PayPal refund response: DECLINED");
            return null;
        }

        String refundId = "REF-" + UUID.randomUUID().toString().toUpperCase();
        LOGGER.log(Level.INFO, "PayPal refund response: SUCCESS — {0}", refundId);
        return refundId;
    }
}