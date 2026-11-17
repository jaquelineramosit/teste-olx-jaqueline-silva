package br.com.olx.marketplace.domain.ad;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(AdId adId, ModerationStatus from, ModerationStatus to) {
        super("Ad %s cannot move from %s to %s".formatted(adId, from, to));
    }
}
