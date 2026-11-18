package br.com.olx.marketplace.infrastructure.adapter.out.persistence;

import br.com.olx.marketplace.domain.ad.Category;
import br.com.olx.marketplace.domain.ad.ModerationStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface AdJpaRepository extends JpaRepository<AdJpaEntity, UUID> {

    Page<AdJpaEntity> findByStatus(ModerationStatus status, Pageable pageable);

    Page<AdJpaEntity> findByStatusAndCategory(ModerationStatus status, Category category, Pageable pageable);

    List<AdJpaEntity> findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(ModerationStatus status, Instant threshold,
                                                                       Limit limit);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AdJpaEntity a
               set a.status = :status,
                   a.rejectionReason = :rejectionReason,
                   a.moderatedAt = :moderatedAt,
                   a.updatedAt = :updatedAt
             where a.id = :id
               and a.status = br.com.olx.marketplace.domain.ad.ModerationStatus.PENDING_MODERATION
            """)
    int updateModerationOutcomeIfPending(@Param("id") UUID id,
                                         @Param("status") ModerationStatus status,
                                         @Param("rejectionReason") String rejectionReason,
                                         @Param("moderatedAt") Instant moderatedAt,
                                         @Param("updatedAt") Instant updatedAt);
}
