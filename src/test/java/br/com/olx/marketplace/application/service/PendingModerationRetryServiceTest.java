package br.com.olx.marketplace.application.service;

import br.com.olx.marketplace.application.port.out.AdRepository;
import br.com.olx.marketplace.application.port.out.ModerationDecision;
import br.com.olx.marketplace.application.port.out.ModerationGateway;
import br.com.olx.marketplace.application.port.out.ModerationUnavailableException;
import br.com.olx.marketplace.domain.ad.Ad;
import br.com.olx.marketplace.domain.ad.ModerationStatus;
import br.com.olx.marketplace.support.AdFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingModerationRetryServiceTest {

    @Mock
    private AdRepository adRepository;
    @Mock
    private ModerationGateway moderationGateway;

    @Test
    void retriesOldPendingAdsAndCountsOnlyResolvedOnes() {
        Clock clock = Clock.fixed(AdFixtures.NOW, ZoneOffset.UTC);
        Ad recovered = AdFixtures.pendingAd();
        Ad stillFailing = AdFixtures.pendingAd();
        when(adRepository.findPendingCreatedBefore(AdFixtures.NOW.minusSeconds(30), 10))
                .thenReturn(List.of(recovered, stillFailing));
        when(moderationGateway.check(recovered)).thenReturn(new ModerationDecision.Approved());
        when(moderationGateway.check(stillFailing)).thenThrow(new ModerationUnavailableException("HTTP 500"));
        when(adRepository.saveModerationOutcome(recovered)).thenReturn(true);

        int resolved = new PendingModerationRetryService(adRepository,
                new AdModerator(moderationGateway, adRepository, clock), clock, Duration.ofSeconds(30), 10)
                .retryPendingModerations();

        assertThat(resolved).isEqualTo(1);
        assertThat(recovered.status()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(stillFailing.status()).isEqualTo(ModerationStatus.PENDING_MODERATION);
    }
}
