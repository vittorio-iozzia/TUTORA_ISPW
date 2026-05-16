package it.ispw.tutora.exception;

public class OAuthException extends Exception {
    public OAuthException(String message) { super(message); }
    public OAuthException(String message, Throwable cause) { super(message, cause); }
}
