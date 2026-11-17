package br.com.olx.marketplace.application.service;

import br.com.olx.marketplace.application.port.in.ListAdsUseCase;
import br.com.olx.marketplace.application.port.in.PageResult;
import br.com.olx.marketplace.application.port.out.AdRepository;
import br.com.olx.marketplace.domain.ad.Ad;
import br.com.olx.marketplace.domain.ad.Category;

public class ListAdsService implements ListAdsUseCase {

    private final AdRepository adRepository;

    public ListAdsService(AdRepository adRepository) {
        this.adRepository = adRepository;
    }

    @Override
    public PageResult<Ad> listPublished(ListAdsQuery query) {
        return adRepository.findPublished(query.category().map(Category::fromCode), query.page(), query.size());
    }
}
