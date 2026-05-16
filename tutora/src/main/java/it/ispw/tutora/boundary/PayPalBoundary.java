package it.ispw.tutora.boundary;

import it.ispw.tutora.exception.PaymentException;
import it.ispw.tutora.exception.PaymentTimeoutException;

import java.math.BigDecimal;
import java.util.concurrent.Callable;
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

    @Override
    @SuppressWarnings("java:S2095") // ExecutorService.close() introdotto in Java 19
    public String processPayment(BigDecimal amount)
            throws PaymentException, PaymentTimeoutException {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Invalid payment amount.");
        }

        return executeWithTimeout(
                () -> adaptee.charge(amount),
                "Payment declined. Please check your payment method.",
                "Payment service did not respond within " + TIMEOUT_MINUTES + " minutes. Please try again.",
                "Payment was interrupted.",
                "PayPal service error: ",
                "Payment service unavailable. Please try again later.");
    }

    @Override
    @SuppressWarnings("java:S2095") // ExecutorService.close() introdotto in Java 19
    public String refund(String paymentRef, BigDecimal amount)
            throws PaymentException, PaymentTimeoutException {

        if (paymentRef == null || paymentRef.isBlank()) {
            throw new PaymentException("Invalid payment reference.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Invalid refund amount.");
        }

        return executeWithTimeout(
                () -> adaptee.refund(paymentRef, amount),
                "Refund declined by PayPal.",
                "Refund service did not respond within " + TIMEOUT_MINUTES + " minutes.",
                "Refund was interrupted.",
                "PayPal refund error: ",
                "Refund service unavailable. Please contact support.");
    }

    @SuppressWarnings("java:S2095") // ExecutorService.close() introdotto in Java 19
    private String executeWithTimeout(Callable<String> callable,
                                      String nullMsg,
                                      String timeoutMsg,
                                      String interruptedMsg,
                                      String execLogPrefix,
                                      String execMsg)
            throws PaymentException, PaymentTimeoutException {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> future = executor.submit(callable);
            String result = future.get(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (result == null) throw new PaymentException(nullMsg);
            return result;

        } catch (TimeoutException e) {
            throw new PaymentTimeoutException(timeoutMsg);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentTimeoutException(interruptedMsg);

        } catch (ExecutionException e) {
            LOGGER.severe(execLogPrefix + e.getCause());
            throw new PaymentException(execMsg, e.getCause());

        } finally {
            executor.shutdownNow();
        }
    }
}