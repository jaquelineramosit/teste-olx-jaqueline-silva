package br.com.olx.marketplace.application.service;

import br.com.olx.marketplace.application.port.in.AdNotFoundException;
import br.com.olx.marketplace.application.port.in.ListAdsUseCase.ListAdsQuery;
import br.com.olx.marketplace.application.port.in.PageResult;
import br.com.olx.marketplace.application.port.out.AdRepository;
import br.com.olx.marketplace.domain.ad.Ad;
import br.com.olx.marketplace.domain.ad.AdId;
import br.com.olx.marketplace.domain.ad.Category;
import br.com.olx.marketplace.domain.ad.DomainValidationException;
import br.com.olx.marketplace.support.AdFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryServicesTest {

    @Mock
    private AdRepository adRepository;

    @Test
    void getsExistingAd() {
        Ad ad = AdFixtures.pendingAd();
        when(adRepository.findById(ad.id())).thenReturn(Optional.of(ad));

        assertThat(new GetAdByIdService(adRepository).getById(ad.id())).isSameAs(ad);
    }

    @Test
    void failsWithNotFoundForUnknownAd() {
        AdId unknown = new AdId(UUID.randomUUID());
        when(adRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new GetAdByIdService(adRepository).getById(unknown))
                .isInstanceOf(AdNotFoundException.class);
    }

    @Test
    void listsPublishedAdsWithPagination() {
        PageResult<Ad> page = new PageResult<>(List.of(AdFixtures.pendingAd()), 1, 10, 11);
        when(adRepository.findPublished(Optional.empty(), 1, 10)).thenReturn(page);

        PageResult<Ad> result = new ListAdsService(adRepository).listPublished(new ListAdsQuery(Optional.empty(), 1, 10));

        assertThat(result.items()).hasSize(1);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void filtersByCategoryCode() {
        PageResult<Ad> empty = new PageResult<>(List.of(), 0, 20, 0);
        when(adRepository.findPublished(Optional.of(Category.VEICULOS), 0, 20)).thenReturn(empty);

        PageResult<Ad> result = new ListAdsService(adRepository)
                .listPublished(new ListAdsQuery(Optional.of("veiculos"), 0, 20));

        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    @Test
    void rejectsUnknownCategoryFilter() {
        assertThatThrownBy(() -> new ListAdsService(adRepository)
                .listPublished(new ListAdsQuery(Optional.of("brinquedos"), 0, 20)))
                .isInstanceOf(DomainValidationException.class);
        verifyNoInteractions(adRepository);
    }
}
