package br.com.olx.marketplace.domain.ad;

import java.net.URI;
import java.net.URISyntaxException;

/** Imagem do anúncio, representada por uma URL absoluta http ou https. */
public record ImageUrl(String value) {

    public static final int MAX_LENGTH = 2048;

    public ImageUrl {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("images", "image url is required");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH || !isAbsoluteHttpUrl(value)) {
            throw new DomainValidationException("images", "image must be an absolute http(s) url");
        }
    }

    private static boolean isAbsoluteHttpUrl(String candidate) {
        try {
            URI uri = new URI(candidate);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
