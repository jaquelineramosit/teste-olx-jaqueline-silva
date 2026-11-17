package br.com.olx.marketplace.application.port.in;

import br.com.olx.marketplace.domain.ad.Ad;
import br.com.olx.marketplace.domain.ad.AdId;

public interface GetAdByIdUseCase {

    /** @throws AdNotFoundException quando o anúncio não existe */
    Ad getById(AdId id);
}
