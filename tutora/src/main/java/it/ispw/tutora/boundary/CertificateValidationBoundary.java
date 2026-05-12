package it.ispw.tutora.boundary;

import it.ispw.tutora.bean.ApplicationItemBean;
import it.ispw.tutora.enums.ItemType;
import it.ispw.tutora.exception.ValidationServiceException;
import it.ispw.tutora.exception.ValidationTimeoutException;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Adapter del pattern Adapter (GoF – Strutturale).
 *
 * -----------------------------------------------------------------------
 * Pattern Adapter
 * -----------------------------------------------------------------------
 * Ruoli:
 *   Target  → {@link CertificateValidator} (interfaccia attesa dal Controller)
 *   Adapter → {@link CertificateValidationBoundary} (questa classe)
 *   Adaptee → {@link CertificateValidationClient} (servizio HTTP esterno)
 *
 * Adatta l'interfaccia HTTP del servizio esterno di validazione
 * all'interfaccia {@link CertificateValidator} che il Controller si aspetta.
 * Il Controller chiama validateDocuments() senza sapere nulla di
 * HTTP, timeout o formato della risposta.
 *
 * -----------------------------------------------------------------------
 * Timer
 * -----------------------------------------------------------------------
 * La validazione viene eseguita in un thread separato con timeout
 * di TIMEOUT_MINUTES minuti, come da activity diagram UC-2.
 * Se il timer scade viene lanciata ValidationTimeoutException
 * e il caso d'uso termina.
 */
public class CertificateValidationBoundary implements CertificateValidator {

    private static final Logger LOGGER = Logger.getLogger(
            CertificateValidationBoundary.class.getName());

    private static final long TIMEOUT_MINUTES = 3;

    // Adaptee — composizione come da pattern Adapter classico
    private final CertificateValidationClient apiClient;

    public CertificateValidationBoundary() {
        this.apiClient = new CertificateValidationClient();
    }

    // ----------------------------------------------------------------
    // Target — implementazione dell'interfaccia attesa dal Controller
    // ----------------------------------------------------------------

    /**
     * Adatta la chiamata HTTP dell'Adaptee all'interfaccia Target.
     * Gestisce il timeout di 3 minuti come da activity diagram.
     */
    @Override
    @SuppressWarnings("java:S2095") // ExecutorService.close() introdotto in Java 19; su Java 17 shutdownNow() nel finally è sufficiente
    public boolean validateDocuments(List<ApplicationItemBean> items)
            throws ValidationTimeoutException, ValidationServiceException {

        List<ApplicationItemBean> documents = items.stream()
                .filter(item -> item.getItemType() == ItemType.DOCUMENT)
                .toList();

        if (documents.isEmpty()) {
            LOGGER.info("No documents to validate.");
            return true;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // traduce la chiamata Java nell'interfaccia dell'Adaptee
            Future<Boolean> future = executor.submit(() -> apiClient.callApi(documents));
            return future.get(TIMEOUT_MINUTES, TimeUnit.MINUTES);

        } catch (TimeoutException e) {
            // shutdownNow() nel finally interrompe già il task in esecuzione
            throw new ValidationTimeoutException(
                    "Certificate validation timed out after "
                            + TIMEOUT_MINUTES + " minutes.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ValidationTimeoutException(
                    "Certificate validation was interrupted.");

        } catch (ExecutionException e) {
            LOGGER.severe("Validation service error: " + e.getCause());
            throw new ValidationServiceException(
                    "Certificate validation service error.", e.getCause());

        } finally {
            executor.shutdownNow();
        }
    }
}