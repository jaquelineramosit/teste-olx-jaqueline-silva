package br.com.olx.marketplace.application.service;

import br.com.olx.marketplace.application.port.in.CreateAdUseCase;
import br.com.olx.marketplace.application.port.out.AdRepository;
import br.com.olx.marketplace.domain.ad.Ad;
import br.com.olx.marketplace.domain.ad.AdTitle;
import br.com.olx.marketplace.domain.ad.Category;
import br.com.olx.marketplace.domain.ad.DomainValidationException;
import br.com.olx.marketplace.domain.ad.ImageUrl;
import br.com.olx.marketplace.domain.ad.Price;

import java.time.Clock;
import java.util.List;

public class CreateAdService implements CreateAdUseCase {

    private final AdRepository adRepository;
    private final AdModerator adModerator;
    private final Clock clock;

    public CreateAdService(AdRepository adRepository, AdModerator adModerator, Clock clock) {
        this.adRepository = adRepository;
        this.adModerator = adModerator;
        this.clock = clock;
    }

    @Override
    public Ad create(CreateAdCommand command) {
        Ad ad = Ad.create(
                new AdTitle(command.title()),
                command.description(),
                new Price(command.price()),
                category(command.category()),
                images(command.imageUrls()),
                clock.instant());

        adRepository.insert(ad);
        return adModerator.moderate(ad);
    }

    private static Category category(String code) {
        if (code == null) {
            throw new DomainValidationException("category", "category is required");
        }
        return Category.fromCode(code);
    }

    private static List<ImageUrl> images(List<String> urls) {
        return urls == null ? List.of() : urls.stream().map(ImageUrl::new).toList();
    }
}
