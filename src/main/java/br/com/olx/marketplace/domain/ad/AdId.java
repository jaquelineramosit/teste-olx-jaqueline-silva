package br.com.olx.marketplace.domain.ad;

import java.util.Objects;
import java.util.UUID;

public record AdId(UUID value) {

    public AdId {
        Objects.requireNonNull(value, "ad id must not be null");
    }

    public static AdId newId() {
        return new AdId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
