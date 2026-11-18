package br.com.olx.marketplace.domain.ad;

/**
 * Ciclo de vida do anúncio.
 * <pre>
 * PENDING_MODERATION ──approve()──▶ APPROVED   (publicado)
 *          └────────reject(reason)──▶ REJECTED (não publicado, com motivo)
 * </pre>
 * APPROVED e REJECTED são estados finais.
 */
public enum ModerationStatus {
    PENDING_MODERATION,
    APPROVED,
    REJECTED;

    public boolean isPublished() {
        return this == APPROVED;
    }
}
