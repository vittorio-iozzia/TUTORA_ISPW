package it.ispw.tutora.boundary;

import it.ispw.tutora.exception.PaymentException;
import it.ispw.tutora.exception.PaymentTimeoutException;

import java.math.BigDecimal;
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
 *   Target  → PaymentGateway      (interfaccia attesa dal Controller)
 *   Adapter → PayPalAdapter       (questa classe)
 *   Adaptee → PayPalClient        (SDK PayPal con interfaccia incompatibile)
 *
 * Adatta l'interfaccia incompatibile di PayPalClient all'interfaccia
 * PaymentGateway che il Controller si aspetta.
 * Il Controller chiama processPayment() senza sapere nulla di
 * PayPalClient, timeout o formato della risposta.
 *
 * -----------------------------------------------------------------------
 * Incompatibilità adattate
 * -----------------------------------------------------------------------
 *   charge() → processPayment()
 *   null     → PaymentException ("Payment declined")
 *   RuntimeException → PaymentException ("Service unavailable")
 *   TimeoutException → PaymentTimeoutException
 *
 * -----------------------------------------------------------------------
 * Timer
 * -----------------------------------------------------------------------
 * Il pagamento viene eseguito in un thread separato con timeout
 * di 10 minuti. Se il timer scade viene lanciata PaymentTimeoutException.
 */
public class PayPalBoundary implements PaymentGateway {

    private static final Logger LOGGER = Logger.getLogger(
            PayPalBoundary.class.getName());

    private static final long TIMEOUT_MINUTES = 10L;

    // Adaptee — composizione come da pattern Adapter classico
    private final PayPalClient adaptee;

    public PayPalBoundary() {
        this.adaptee = new PayPalClient();
    }

    // ----------------------------------------------------------------
    // Target — implementazione dell'interfaccia attesa dal Controller
    // ----------------------------------------------------------------

    /**
     * Adatta la chiamata dell'Adaptee all'interfaccia Target.
     * Gestisce il timeout di 10 minuti e traduce la risposta
     * dell'Adaptee nelle eccezioni Java appropriate.
     */
    @Override
    @SuppressWarnings("java:S2095") // ExecutorService.close() introdotto in Java 19
    public String processPayment(BigDecimal amount)
            throws PaymentException, PaymentTimeoutException {

        // Validazione importo prima di chiamare l'Adaptee
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Invalid payment amount.");
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Traduce la chiamata Java nell'interfaccia dell'Adaptee
            Future<String> future = executor.submit(() -> adaptee.charge(amount));
            String transactionId = future.get(TIMEOUT_MINUTES, TimeUnit.MINUTES);

            // Adatta null → PaymentException (rifiuto PayPal)
            if (transactionId == null) {
                throw new PaymentException(
                        "Payment declined. Please check your payment method.");
            }
            return transactionId;

        } catch (TimeoutException e) {
            throw new PaymentTimeoutException(
                    "Payment service did not respond within "
                            + TIMEOUT_MINUTES + " minutes. Please try again.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentTimeoutException("Payment was interrupted.");

        } catch (ExecutionException e) {
            // Adatta RuntimeException dell'Adaptee → PaymentException
            LOGGER.severe("PayPal service error: " + e.getCause());
            throw new PaymentException(
                    "Payment service unavailable. Please try again later.", e.getCause());

        } finally {
            executor.shutdownNow();
        }
    }
}