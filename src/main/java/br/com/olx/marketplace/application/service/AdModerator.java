package br.com.olx.marketplace.application.service;

import br.com.olx.marketplace.application.port.out.AdRepository;
import br.com.olx.marketplace.application.port.out.ModerationDecision;
import br.com.olx.marketplace.application.port.out.ModerationGateway;
import br.com.olx.marketplace.application.port.out.ModerationUnavailableException;
import br.com.olx.marketplace.domain.ad.Ad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;

/**
 * Submete um anúncio pendente à moderação e grava a decisão.
 * Compartilhado pela criação e pelo reprocessamento de pendentes.
 * Não abre transação: a chamada externa acontece entre duas gravações curtas.
 */
public class AdModerator {

    private static final Logger log = LoggerFactory.getLogger(AdModerator.class);

    private final ModerationGateway moderationGateway;
    private final AdRepository adRepository;
    private final Clock clock;

    public AdModerator(ModerationGateway moderationGateway, AdRepository adRepository, Clock clock) {
        this.moderationGateway = moderationGateway;
        this.adRepository = adRepository;
        this.clock = clock;
    }

    public Ad moderate(Ad ad) {
        ModerationDecision decision;
        try {
            decision = moderationGateway.check(ad);
        } catch (ModerationUnavailableException e) {
            log.warn("Moderation unavailable, ad {} stays pending: {}", ad.id(), e.getMessage());
            return ad;
        }

        switch (decision) {
            case ModerationDecision.Approved approved -> ad.approve(clock.instant());
            case ModerationDecision.Rejected rejected -> ad.reject(rejected.reason(), clock.instant());
        }

        if (!adRepository.saveModerationOutcome(ad)) {
            log.info("Ad {} was already moderated by a concurrent flow; keeping stored outcome", ad.id());
            return adRepository.findById(ad.id()).orElse(ad);
        }
        log.info("Ad {} moderated with status {}", ad.id(), ad.status());
        return ad;
    }
}
