package br.com.olx.marketplace.infrastructure.adapter.in.scheduler;

import br.com.olx.marketplace.application.port.in.RetryPendingModerationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Adapter de entrada temporal: dispara o reprocessamento de anúncios que ficaram pendentes. */
@Component
@ConditionalOnProperty(prefix = "moderation.retry", name = "enabled", havingValue = "true", matchIfMissing = true)
class PendingModerationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PendingModerationScheduler.class);

    private final RetryPendingModerationUseCase retryPendingModerationUseCase;

    PendingModerationScheduler(RetryPendingModerationUseCase retryPendingModerationUseCase) {
        this.retryPendingModerationUseCase = retryPendingModerationUseCase;
    }

    @Scheduled(fixedDelayString = "${moderation.retry.interval}", initialDelayString = "${moderation.retry.interval}")
    void retryPendingModerations() {
        try {
            retryPendingModerationUseCase.retryPendingModerations();
        } catch (RuntimeException e) {
            log.error("Pending moderation retry failed; will run again on next schedule", e);
        }
    }
}
