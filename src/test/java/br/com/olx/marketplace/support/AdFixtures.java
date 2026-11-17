package br.com.olx.marketplace.support;

import br.com.olx.marketplace.domain.ad.Ad;
import br.com.olx.marketplace.domain.ad.AdId;
import br.com.olx.marketplace.domain.ad.AdTitle;
import br.com.olx.marketplace.domain.ad.Category;
import br.com.olx.marketplace.domain.ad.ImageUrl;
import br.com.olx.marketplace.domain.ad.ModerationStatus;
import br.com.olx.marketplace.domain.ad.Price;
import br.com.olx.marketplace.domain.ad.RejectionReason;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class AdFixtures {

    public static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");

    private AdFixtures() {
    }

    public static Ad pendingAd() {
        return pendingAd(Category.ESPORTES);
    }

    public static Ad pendingAd(Category category) {
        return Ad.create(new AdTitle("Bicicleta aro 29 Caloi"), "Pouco uso, revisada.",
                new Price(new BigDecimal("1500.00")), category,
                List.of(new ImageUrl("https://img.example.com/bike.jpg")), NOW);
    }

    /** Anúncio já persistido, com data e status controlados (útil para ordenação e filtros). */
    public static Ad storedAd(String title, Category category, ModerationStatus status, Instant createdAt) {
        RejectionReason reason = status == ModerationStatus.REJECTED ? new RejectionReason("prohibited_item") : null;
        Instant moderatedAt = status == ModerationStatus.PENDING_MODERATION ? null : createdAt;
        return Ad.restore(AdId.newId(), new AdTitle(title), "Descrição do anúncio", new Price(new BigDecimal("99.90")),
                category, List.of(new ImageUrl("https://img.example.com/1.jpg"),
                        new ImageUrl("https://img.example.com/2.jpg")),
                status, reason, createdAt, createdAt, moderatedAt);
    }
}
