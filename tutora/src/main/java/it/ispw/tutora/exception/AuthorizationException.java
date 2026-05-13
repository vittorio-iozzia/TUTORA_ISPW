package it.ispw.tutora.exception;

/**
 * Lanciata quando un utente autenticato tenta di eseguire un'operazione
 * per cui non ha i permessi necessari (es. un Student che chiama
 * un metodo riservato agli Admin).
 *
 * <p>Distinta da {@link AuthenticationException} (identità non verificata)
 * perché qui l'identità è nota ma il ruolo è insufficiente.</p>
 */
public class AuthorizationException extends TutoraException {

    public AuthorizationException(String message) {
        super(message);
    }
}
