package br.com.olx.marketplace.application.service;

import br.com.olx.marketplace.application.port.in.CreateAdUseCase.CreateAdCommand;
import br.com.olx.marketplace.application.port.out.AdRepository;
import br.com.olx.marketplace.application.port.out.ModerationDecision;
import br.com.olx.marketplace.application.port.out.ModerationGateway;
import br.com.olx.marketplace.application.port.out.ModerationUnavailableException;
import br.com.olx.marketplace.domain.ad.Ad;
import br.com.olx.marketplace.domain.ad.Category;
import br.com.olx.marketplace.domain.ad.DomainValidationException;
import br.com.olx.marketplace.domain.ad.ModerationStatus;
import br.com.olx.marketplace.domain.ad.RejectionReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static br.com.olx.marketplace.support.AdFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAdServiceTest {

    @Mock
    private AdRepository adRepository;
    @Mock
    private ModerationGateway moderationGateway;

    private CreateAdService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new CreateAdService(adRepository, new AdModerator(moderationGateway, adRepository, clock), clock);
    }

    @Test
    void persistsPendingAdBeforeModerationAndThenStoresApproval() {
        when(moderationGateway.check(any())).thenReturn(new ModerationDecision.Approved());
        when(adRepository.saveModerationOutcome(any())).thenReturn(true);

        Ad ad = service.create(validCommand());

        assertThat(ad.status()).isEqualTo(ModerationStatus.APPROVED);
        InOrder order = inOrder(adRepository, moderationGateway);
        ArgumentCaptor<Ad> inserted = ArgumentCaptor.forClass(Ad.class);
        order.verify(adRepository).insert(inserted.capture());
        order.verify(moderationGateway).check(ad);
        order.verify(adRepository).saveModerationOutcome(ad);
        assertThat(inserted.getValue().id()).isEqualTo(ad.id());
        assertThat(ad.title().value()).isEqualTo("Bicicleta aro 29 Caloi");
        assertThat(ad.category()).isEqualTo(Category.ESPORTES);
        assertThat(ad.price().amount()).isEqualByComparingTo("1500");
    }

    @Test
    void storesRejectionWithReason() {
        when(moderationGateway.check(any()))
                .thenReturn(new ModerationDecision.Rejected(new RejectionReason("price_out_of_range")));
        when(adRepository.saveModerationOutcome(any())).thenReturn(true);

        Ad ad = service.create(validCommand());

        assertThat(ad.status()).isEqualTo(ModerationStatus.REJECTED);
        assertThat(ad.rejectionReason()).map(RejectionReason::code).contains("price_out_of_range");
        verify(adRepository).saveModerationOutcome(ad);
    }

    @Test
    void keepsAdPendingWhenModerationIsUnavailable() {
        when(moderationGateway.check(any()))
                .thenThrow(new ModerationUnavailableException("moderation failed after 3 attempt(s): HTTP 500"));

        Ad ad = service.create(validCommand());

        assertThat(ad.status()).isEqualTo(ModerationStatus.PENDING_MODERATION);
        assertThat(ad.rejectionReason()).isEmpty();
        verify(adRepository).insert(ad);
        verify(adRepository, never()).saveModerationOutcome(any());
    }

    @Test
    void doesNotPersistNorModerateInvalidAd() {
        CreateAdCommand withoutImages = new CreateAdCommand("Bicicleta aro 29 Caloi", "Pouco uso",
                new BigDecimal("1500"), "esportes", List.of());

        assertThatThrownBy(() -> service.create(withoutImages)).isInstanceOf(DomainValidationException.class);
        verifyNoInteractions(adRepository, moderationGateway);
    }

    @Test
    void rejectsUnknownCategoryBeforeCallingModeration() {
        CreateAdCommand unknownCategory = new CreateAdCommand("Bicicleta aro 29 Caloi", "Pouco uso",
                new BigDecimal("1500"), "brinquedos", List.of("https://img.example.com/a.jpg"));

        assertThatThrownBy(() -> service.create(unknownCategory))
                .isInstanceOf(DomainValidationException.class)
                .extracting("field").isEqualTo("category");
        verifyNoInteractions(adRepository, moderationGateway);
    }

    private static CreateAdCommand validCommand() {
        return new CreateAdCommand("Bicicleta aro 29 Caloi", "Pouco uso, revisada.", new BigDecimal("1500"),
                "esportes", List.of("https://img.example.com/bike.jpg"));
    }
}
