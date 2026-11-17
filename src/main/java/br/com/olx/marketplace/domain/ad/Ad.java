package br.com.olx.marketplace.domain.ad;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate Root do anúncio. Garante as invariantes de criação e as transições de status.
 */
public final class Ad {

    public static final int MAX_DESCRIPTION_LENGTH = 5000;
    public static final int MAX_IMAGES = 10;

    private final AdId id;
    private final AdTitle title;
    private final String description;
    private final Price price;
    private final Category category;
    private final List<ImageUrl> images;
    private final Instant createdAt;
    private ModerationStatus status;
    private RejectionReason rejectionReason;
    private Instant updatedAt;
    private Instant moderatedAt;

    private Ad(AdId id, AdTitle title, String description, Price price, Category category, List<ImageUrl> images,
               ModerationStatus status, RejectionReason rejectionReason,
               Instant createdAt, Instant updatedAt, Instant moderatedAt) {
        this.id = Objects.requireNonNull(id);
        this.title = Objects.requireNonNull(title);
        this.description = description;
        this.price = Objects.requireNonNull(price);
        this.category = Objects.requireNonNull(category);
        this.images = List.copyOf(images);
        this.status = Objects.requireNonNull(status);
        this.rejectionReason = rejectionReason;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.moderatedAt = moderatedAt;
    }

    /** Cria um novo anúncio, sempre aguardando moderação. */
    public static Ad create(AdTitle title, String description, Price price, Category category,
                            List<ImageUrl> images, Instant now) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(price, "price");
        if (category == null) {
            throw new DomainValidationException("category", "category is required");
        }
        return new Ad(AdId.newId(), title, validDescription(description), price, category, validImages(images),
                ModerationStatus.PENDING_MODERATION, null, now, now, null);
    }

    /** Reconstitui um anúncio já persistido, sem reaplicar regras de criação. */
    public static Ad restore(AdId id, AdTitle title, String description, Price price, Category category,
                             List<ImageUrl> images, ModerationStatus status, RejectionReason rejectionReason,
                             Instant createdAt, Instant updatedAt, Instant moderatedAt) {
        return new Ad(id, title, description, price, category, images, status, rejectionReason,
                createdAt, updatedAt, moderatedAt);
    }

    public void approve(Instant now) {
        transitionTo(ModerationStatus.APPROVED, now);
    }

    public void reject(RejectionReason reason, Instant now) {
        Objects.requireNonNull(reason, "a rejection must have a reason");
        transitionTo(ModerationStatus.REJECTED, now);
        this.rejectionReason = reason;
    }

    private void transitionTo(ModerationStatus target, Instant now) {
        if (status != ModerationStatus.PENDING_MODERATION) {
            throw new InvalidStatusTransitionException(id, status, target);
        }
        this.status = target;
        this.moderatedAt = now;
        this.updatedAt = now;
    }

    private static String validDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new DomainValidationException("description", "description is required");
        }
        String stripped = description.strip();
        if (stripped.codePointCount(0, stripped.length()) > MAX_DESCRIPTION_LENGTH) {
            throw new DomainValidationException("description",
                    "description must have at most %d characters".formatted(MAX_DESCRIPTION_LENGTH));
        }
        return stripped;
    }

    private static List<ImageUrl> validImages(List<ImageUrl> images) {
        if (images == null || images.isEmpty()) {
            throw new DomainValidationException("images", "at least one image is required");
        }
        if (images.size() > MAX_IMAGES) {
            throw new DomainValidationException("images", "at most %d images are allowed".formatted(MAX_IMAGES));
        }
        return images;
    }

    public AdId id() { return id; }
    public AdTitle title() { return title; }
    public String description() { return description; }
    public Price price() { return price; }
    public Category category() { return category; }
    public List<ImageUrl> images() { return images; }
    public ModerationStatus status() { return status; }
    public Optional<RejectionReason> rejectionReason() { return Optional.ofNullable(rejectionReason); }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public Optional<Instant> moderatedAt() { return Optional.ofNullable(moderatedAt); }
}
