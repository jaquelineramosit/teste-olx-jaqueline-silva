package br.com.olx.marketplace.domain.ad;

/**
 * Violação de uma invariante do domínio. {@code field} identifica o dado de entrada inválido
 * para que a camada de entrada possa apontá-lo ao cliente.
 */
public class DomainValidationException extends RuntimeException {

    private final String field;

    public DomainValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
