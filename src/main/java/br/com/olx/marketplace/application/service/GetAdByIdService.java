package br.com.olx.marketplace.application.service;

import br.com.olx.marketplace.application.port.in.AdNotFoundException;
import br.com.olx.marketplace.application.port.in.GetAdByIdUseCase;
import br.com.olx.marketplace.application.port.out.AdRepository;
import br.com.olx.marketplace.domain.ad.Ad;
import br.com.olx.marketplace.domain.ad.AdId;

public class GetAdByIdService implements GetAdByIdUseCase {

    private final AdRepository adRepository;

    public GetAdByIdService(AdRepository adRepository) {
        this.adRepository = adRepository;
    }

    @Override
    public Ad getById(AdId id) {
        return adRepository.findById(id).orElseThrow(() -> new AdNotFoundException(id));
    }
}
