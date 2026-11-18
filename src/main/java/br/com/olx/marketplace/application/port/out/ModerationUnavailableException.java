package br.com.olx.marketplace.application.port.out;

/** Falha técnica ao consultar a moderação. Não representa decisão de negócio. */
public class ModerationUnavailableException extends RuntimeException {

    public ModerationUnavailableException(String message) {
        super(message);
    }

    public ModerationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
