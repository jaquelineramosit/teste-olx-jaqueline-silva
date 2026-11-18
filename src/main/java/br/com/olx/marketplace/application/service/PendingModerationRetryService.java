package br.com.olx.marketplace.application.service;

import br.com.olx.marketplace.application.port.in.RetryPendingModerationUseCase;
import br.com.olx.marketplace.application.port.out.AdRepository;
import br.com.olx.marketplace.domain.ad.Ad;
import br.com.olx.marketplace.domain.ad.ModerationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

public class PendingModerationRetryService implements RetryPendingModerationUseCase {

    private static final Logger log = LoggerFactory.getLogger(PendingModerationRetryService.class);

    private final AdRepository adRepository;
    private final AdModerator adModerator;
    private final Clock clock;
    private final Duration minPendingAge;
    private final int batchSize;

    /**
     * @param minPendingAge idade mínima do anúncio pendente, para não competir com a moderação
     *                      síncrona que ainda pode estar em andamento na requisição de criação
     * @param batchSize     limite de anúncios por execução, para respeitar o rate limit da moderação
     */
    public PendingModerationRetryService(AdRepository adRepository, AdModerator adModerator, Clock clock,
                                         Duration minPendingAge, int batchSize) {
        this.adRepository = adRepository;
        this.adModerator = adModerator;
        this.clock = clock;
        this.minPendingAge = minPendingAge;
        this.batchSize = batchSize;
    }

    @Override
    public int retryPendingModerations() {
        List<Ad> pending = adRepository.findPendingCreatedBefore(clock.instant().minus(minPendingAge), batchSize);
        int resolved = 0;
        for (Ad ad : pending) {
            if (adModerator.moderate(ad).status() != ModerationStatus.PENDING_MODERATION) {
                resolved++;
            }
        }
        if (!pending.isEmpty()) {
            log.info("Pending moderation retry: {} found, {} resolved", pending.size(), resolved);
        }
        return resolved;
    }
}
