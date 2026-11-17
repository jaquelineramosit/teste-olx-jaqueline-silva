package br.com.olx.marketplace.infrastructure.adapter.out.persistence;

import br.com.olx.marketplace.application.port.in.PageResult;
import br.com.olx.marketplace.application.port.out.AdRepository;
import br.com.olx.marketplace.domain.ad.Ad;
import br.com.olx.marketplace.domain.ad.AdId;
import br.com.olx.marketplace.domain.ad.AdTitle;
import br.com.olx.marketplace.domain.ad.Category;
import br.com.olx.marketplace.domain.ad.ImageUrl;
import br.com.olx.marketplace.domain.ad.ModerationStatus;
import br.com.olx.marketplace.domain.ad.Price;
import br.com.olx.marketplace.domain.ad.RejectionReason;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
class AdPersistenceAdapter implements AdRepository {

    /** Ordenação determinística: created_at pode empatar, o id desempata. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final AdJpaRepository jpaRepository;

    AdPersistenceAdapter(AdJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void insert(Ad ad) {
        jpaRepository.save(toEntity(ad));
    }

    @Override
    @Transactional
    public boolean saveModerationOutcome(Ad ad) {
        int updated = jpaRepository.updateModerationOutcomeIfPending(
                ad.id().value(),
                ad.status(),
                ad.rejectionReason().map(RejectionReason::code).orElse(null),
                ad.moderatedAt().orElse(null),
                ad.updatedAt());
        return updated == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ad> findById(AdId id) {
        return jpaRepository.findById(id.value()).map(AdPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Ad> findPublished(Optional<Category> category, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, NEWEST_FIRST);
        Page<AdJpaEntity> result = category
                .map(c -> jpaRepository.findByStatusAndCategory(ModerationStatus.APPROVED, c, pageRequest))
                .orElseGet(() -> jpaRepository.findByStatus(ModerationStatus.APPROVED, pageRequest));
        List<Ad> ads = result.getContent().stream().map(AdPersistenceAdapter::toDomain).toList();
        return new PageResult<>(ads, page, size, result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ad> findPendingCreatedBefore(Instant threshold, int limit) {
        return jpaRepository.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                        ModerationStatus.PENDING_MODERATION, threshold, Limit.of(limit))
                .stream()
                .map(AdPersistenceAdapter::toDomain)
                .toList();
    }

    private static AdJpaEntity toEntity(Ad ad) {
        return new AdJpaEntity(
                ad.id().value(),
                ad.title().value(),
                ad.description(),
                ad.price().amount(),
                ad.category(),
                ad.status(),
                ad.rejectionReason().map(RejectionReason::code).orElse(null),
                ad.createdAt(),
                ad.updatedAt(),
                ad.moderatedAt().orElse(null),
                ad.images().stream().map(ImageUrl::value).toList());
    }

    private static Ad toDomain(AdJpaEntity entity) {
        return Ad.restore(
                new AdId(entity.getId()),
                new AdTitle(entity.getTitle()),
                entity.getDescription(),
                new Price(entity.getPrice()),
                entity.getCategory(),
                entity.getImageUrls().stream().map(ImageUrl::new).toList(),
                entity.getStatus(),
                Optional.ofNullable(entity.getRejectionReason()).map(RejectionReason::new).orElse(null),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getModeratedAt());
    }
}
