package br.com.olx.marketplace.domain.ad;

import java.math.BigDecimal;

/** Preço em reais: maior que zero e com no máximo duas casas decimais. */
public record Price(BigDecimal amount) {

    private static final int MAX_SCALE = 2;

    public Price {
        if (amount == null) {
            throw new DomainValidationException("price", "price is required");
        }
        if (amount.signum() <= 0) {
            throw new DomainValidationException("price", "price must be greater than zero");
        }
        BigDecimal normalized = amount.stripTrailingZeros();
        if (normalized.scale() > MAX_SCALE) {
            throw new DomainValidationException("price", "price must have at most 2 decimal places");
        }
        amount = amount.setScale(MAX_SCALE);
    }
}
