package it.ispw.tutora.boundary;

import it.ispw.tutora.bean.ApplicationItemBean;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adaptee del pattern Adapter.
 *
 * -----------------------------------------------------------------------
 * Pattern Adapter (GoF – Strutturale)
 * -----------------------------------------------------------------------
 * Rappresenta il servizio esterno di validazione certificati con la
 * sua interfaccia "incompatibile" — in produzione eseguirebbe una
 * chiamata HTTP POST all'API reale.
 *
 * Il Controller non conosce mai questa classe — la vede solo
 * attraverso {@link CertificateValidator} (Target) tramite
 * {@link CertificateValidationBoundary} (Adapter).
 *
 * -----------------------------------------------------------------------
 * Simulazione
 * -----------------------------------------------------------------------
 * Simula la chiamata HTTP con ritardo casuale e probabilità di successo
 * configurabile. In produzione si sostituisce il corpo di
 * callApi() con una vera chiamata HTTP senza toccare il Controller.
 */
public class CertificateValidationClient {

    private static final Logger LOGGER = Logger.getLogger(
            CertificateValidationClient.class.getName());

    private static final double VALID_PROBABILITY = 0.8;
    private static final int MIN_DELAY_MS = 1_000;
    private static final int MAX_DELAY_MS = 3_000;

    private final Random random = new Random();

    /**
     * Chiama l'API esterna di validazione documenti.
     */
    public boolean callApi(List<ApplicationItemBean> documents)
            throws InterruptedException {

        int delay = MIN_DELAY_MS + random.nextInt(MAX_DELAY_MS - MIN_DELAY_MS);

        LOGGER.log(Level.INFO, "Calling validation API for {0} document(s)... (simulated delay: {1}ms)",
                new Object[]{documents.size(), delay});

        Thread.sleep(delay);

        boolean valid = random.nextDouble() < VALID_PROBABILITY;

        LOGGER.log(Level.INFO, "API response: {0}", valid ? "VALID" : "INVALID");

        return valid;
    }
}