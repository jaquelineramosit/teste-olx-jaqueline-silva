package br.com.olx.marketplace.domain.ad;

/**
 * Motivo de reprovação informado pelo serviço de moderação (ex.: {@code price_out_of_range}).
 * Mantido como texto para não acoplar o domínio à lista fechada de códigos do moderador.
 */
public record RejectionReason(String code) {

    public static final int MAX_LENGTH = 100;

    public RejectionReason {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("rejection reason must not be blank");
        }
        code = code.strip();
        if (code.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("rejection reason must have at most " + MAX_LENGTH + " characters");
        }
    }
}
