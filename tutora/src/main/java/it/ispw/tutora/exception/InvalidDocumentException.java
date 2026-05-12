package it.ispw.tutora.exception;

/**
 * Lanciata quando uno o più documenti allegati a una candidatura
 * non superano la validazione del servizio esterno ({@code CertificateValidationBoundary}).
 *
 * <p>Distinta da {@link ValidationTimeoutException}: qui la validazione
 * è completata ma l'esito è negativo; lì il servizio non ha risposto
 * entro il timeout.</p>
 */
public class InvalidDocumentException extends TutoraException {

    public InvalidDocumentException(String message) {
        super(message);
    }
}
