package br.com.olx.marketplace.infrastructure.adapter.in.web.dto;

import br.com.olx.marketplace.domain.ad.Ad;
import br.com.olx.marketplace.domain.ad.ImageUrl;
import br.com.olx.marketplace.domain.ad.RejectionReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record AdResponse(
        UUID id,
        String title,
        String description,
        BigDecimal price,
        String category,
        List<String> images,
        @Schema(allowableValues = {"pending_moderation", "approved", "rejected"})
        String status,
        @Schema(description = "Preenchido somente quando status = rejected", example = "price_out_of_range",
                nullable = true)
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt,
        @Schema(nullable = true)
        Instant moderatedAt) {

    public static AdResponse from(Ad ad) {
        return new AdResponse(
                ad.id().value(),
                ad.title().value(),
                ad.description(),
                ad.price().amount(),
                ad.category().code(),
                ad.images().stream().map(ImageUrl::value).toList(),
                ad.status().name().toLowerCase(Locale.ROOT),
                ad.rejectionReason().map(RejectionReason::code).orElse(null),
                ad.createdAt(),
                ad.updatedAt(),
                ad.moderatedAt().orElse(null));
    }
}
