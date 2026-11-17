package br.com.olx.marketplace.application.port.in;

import br.com.olx.marketplace.domain.ad.AdId;

public class AdNotFoundException extends RuntimeException {

    public AdNotFoundException(AdId id) {
        super("Ad " + id + " not found");
    }
}
