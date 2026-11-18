package br.com.olx.marketplace.infrastructure.adapter.out.persistence;

import br.com.olx.marketplace.domain.ad.Category;
import br.com.olx.marketplace.domain.ad.ModerationStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Modelo de persistência. Não sai do adapter: é convertido de/para o aggregate {@code Ad}. */
@Entity
@Table(name = "ads")
class AdJpaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModerationStatus status;

    @Column(name = "rejection_reason", length = 100)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    /** Imagens são Value Objects: coleção sem identidade própria, carregada em lote para evitar N+1. */
    @ElementCollection
    @CollectionTable(name = "ad_images", joinColumns = @JoinColumn(name = "ad_id"))
    @OrderColumn(name = "position")
    @Column(name = "url", nullable = false, length = 2048)
    @BatchSize(size = 100)
    private List<String> imageUrls = new ArrayList<>();

    /** O id é gerado no domínio; sem isso o Spring Data faria um SELECT antes de cada INSERT. */
    @Transient
    private boolean isNew = true;

    protected AdJpaEntity() {
    }

    AdJpaEntity(UUID id, String title, String description, BigDecimal price, Category category,
                ModerationStatus status, String rejectionReason, Instant createdAt, Instant updatedAt,
                Instant moderatedAt, List<String> imageUrls) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.moderatedAt = moderatedAt;
        this.imageUrls = new ArrayList<>(imageUrls);
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

    String getTitle() { return title; }
    String getDescription() { return description; }
    BigDecimal getPrice() { return price; }
    Category getCategory() { return category; }
    ModerationStatus getStatus() { return status; }
    String getRejectionReason() { return rejectionReason; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }
    Instant getModeratedAt() { return moderatedAt; }
    List<String> getImageUrls() { return imageUrls; }
}
