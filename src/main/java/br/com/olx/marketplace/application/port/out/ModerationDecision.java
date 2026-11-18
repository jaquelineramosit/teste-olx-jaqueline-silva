package br.com.olx.marketplace.application.port.out;

import br.com.olx.marketplace.domain.ad.RejectionReason;

import java.util.Objects;

/** Decisão de negócio devolvida pelo serviço de moderação. */
public sealed interface ModerationDecision {

    record Approved() implements ModerationDecision {
    }

    record Rejected(RejectionReason reason) implements ModerationDecision {
        public Rejected {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
