package br.com.olx.marketplace.application.service;

import br.com.olx.marketplace.application.port.out.AdRepository;
import br.com.olx.marketplace.application.port.out.ModerationDecision;
import br.com.olx.marketplace.application.port.out.ModerationGateway;
import br.com.olx.marketplace.domain.ad.Ad;
import br.com.olx.marketplace.domain.ad.ModerationStatus;
import br.com.olx.marketplace.domain.ad.RejectionReason;
import br.com.olx.marketplace.support.AdFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdModeratorTest {

    @Mock
    private ModerationGateway moderationGateway;
    @Mock
    private AdRepository adRepository;

    @Test
    void returnsStoredOutcomeWhenAnotherFlowModeratedFirst() {
        Ad ad = AdFixtures.pendingAd();
        Ad alreadyRejected = AdFixtures.pendingAd();
        alreadyRejected.reject(new RejectionReason("prohibited_item"), AdFixtures.NOW);
        when(moderationGateway.check(ad)).thenReturn(new ModerationDecision.Approved());
        when(adRepository.saveModerationOutcome(ad)).thenReturn(false);
        when(adRepository.findById(ad.id())).thenReturn(Optional.of(alreadyRejected));

        Ad result = new AdModerator(moderationGateway, adRepository, Clock.fixed(AdFixtures.NOW, ZoneOffset.UTC))
                .moderate(ad);

        assertThat(result.status()).isEqualTo(ModerationStatus.REJECTED);
    }
}
