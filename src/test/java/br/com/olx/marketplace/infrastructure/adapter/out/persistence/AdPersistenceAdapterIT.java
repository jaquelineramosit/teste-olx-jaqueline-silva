package br.com.olx.marketplace.infrastructure.adapter.out.persistence;

import br.com.olx.marketplace.application.port.in.PageResult;
import br.com.olx.marketplace.domain.ad.Ad;
import br.com.olx.marketplace.domain.ad.Category;
import br.com.olx.marketplace.domain.ad.ImageUrl;
import br.com.olx.marketplace.domain.ad.ModerationStatus;
import br.com.olx.marketplace.domain.ad.RejectionReason;
import br.com.olx.marketplace.support.AdFixtures;
import br.com.olx.marketplace.support.PostgresTestcontainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static br.com.olx.marketplace.domain.ad.ModerationStatus.APPROVED;
import static br.com.olx.marketplace.domain.ad.ModerationStatus.PENDING_MODERATION;
import static br.com.olx.marketplace.domain.ad.ModerationStatus.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistência contra PostgreSQL real, com o schema criado pelas migrations do Flyway.
 * Sem transação de teste: cada chamada ao adapter commita, como em produção, e as leituras vêm do banco
 * (não do cache de primeiro nível do Hibernate).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestcontainer.class, AdPersistenceAdapter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AdPersistenceAdapterIT {

    private static final Instant T0 = Instant.parse("2026-06-01T12:00:00Z");

    @Autowired
    private AdPersistenceAdapter adapter;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE ads CASCADE");
    }

    @Test
    void migrationsCreateTheExpectedSchema() {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT max(version)::int FROM flyway_schema_history WHERE success", Integer.class);
        assertThat(version).isEqualTo(1);
    }

    @Test
    void insertsAndRestoresAdWithImagesInOrder() {
        Ad ad = AdFixtures.pendingAd();

        adapter.insert(ad);
        Ad restored = adapter.findById(ad.id()).orElseThrow();

        assertThat(restored.title()).isEqualTo(ad.title());
        assertThat(restored.description()).isEqualTo(ad.description());
        assertThat(restored.price().amount()).isEqualByComparingTo(ad.price().amount());
        assertThat(restored.category()).isEqualTo(Category.ESPORTES);
        assertThat(restored.status()).isEqualTo(PENDING_MODERATION);
        assertThat(restored.createdAt()).isEqualTo(ad.createdAt());
        assertThat(restored.images()).extracting(ImageUrl::value).containsExactly("https://img.example.com/bike.jpg");
    }

    @Test
    void persistsRejectionReason() {
        Ad ad = AdFixtures.pendingAd();
        adapter.insert(ad);
        ad.reject(new RejectionReason("price_out_of_range"), T0);

        assertThat(adapter.saveModerationOutcome(ad)).isTrue();

        Ad restored = adapter.findById(ad.id()).orElseThrow();
        assertThat(restored.status()).isEqualTo(REJECTED);
        assertThat(restored.rejectionReason()).map(RejectionReason::code).contains("price_out_of_range");
        assertThat(restored.moderatedAt()).contains(T0);
    }

    @Test
    void doesNotOverwriteAnOutcomeAlreadyStored() {
        Ad ad = AdFixtures.pendingAd();
        adapter.insert(ad);
        Ad concurrentCopy = adapter.findById(ad.id()).orElseThrow();

        ad.approve(T0);
        assertThat(adapter.saveModerationOutcome(ad)).isTrue();
        concurrentCopy.reject(new RejectionReason("prohibited_item"), T0);

        assertThat(adapter.saveModerationOutcome(concurrentCopy)).isFalse();
        assertThat(adapter.findById(ad.id()).orElseThrow().status()).isEqualTo(APPROVED);
    }

    @Test
    void listsOnlyApprovedAdsNewestFirstWithPagination() {
        Ad oldest = insert("Anúncio aprovado 1", Category.ELETRONICOS, APPROVED, T0);
        Ad middle = insert("Anúncio aprovado 2", Category.MOVEIS, APPROVED, T0.plusSeconds(60));
        Ad newest = insert("Anúncio aprovado 3", Category.ELETRONICOS, APPROVED, T0.plusSeconds(120));
        insert("Anúncio reprovado", Category.ELETRONICOS, REJECTED, T0.plusSeconds(180));
        insert("Anúncio pendente!", Category.ELETRONICOS, PENDING_MODERATION, T0.plusSeconds(240));

        PageResult<Ad> first = adapter.findPublished(Optional.empty(), 0, 2);
        PageResult<Ad> second = adapter.findPublished(Optional.empty(), 1, 2);

        assertThat(first.items()).extracting(Ad::id).containsExactly(newest.id(), middle.id());
        assertThat(second.items()).extracting(Ad::id).containsExactly(oldest.id());
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(second.items().getFirst().images()).hasSize(2);
    }

    @Test
    void ordersDeterministicallyWhenCreationDatesTie() {
        Ad a = insert("Mesmo horário A", Category.MOVEIS, APPROVED, T0);
        Ad b = insert("Mesmo horário B", Category.MOVEIS, APPROVED, T0);
        UUID higher = a.id().value().compareTo(b.id().value()) > 0 ? a.id().value() : b.id().value();

        for (int i = 0; i < 3; i++) {
            assertThat(adapter.findPublished(Optional.empty(), 0, 1).items().getFirst().id().value())
                    .isEqualTo(higher);
        }
    }

    @Test
    void filtersByCategory() {
        insert("Notebook Dell Inspiron", Category.ELETRONICOS, APPROVED, T0);
        Ad sofa = insert("Sofá retrátil 3 lugares", Category.MOVEIS, APPROVED, T0);
        insert("Mesa reprovada moveis", Category.MOVEIS, REJECTED, T0);

        PageResult<Ad> result = adapter.findPublished(Optional.of(Category.MOVEIS), 0, 20);

        assertThat(result.items()).extracting(Ad::id).containsExactly(sofa.id());
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void findsOnlyPendingAdsOlderThanThreshold() {
        Ad oldPending = insert("Pendente antigo aaa", Category.MOVEIS, PENDING_MODERATION, T0);
        insert("Pendente recente aaa", Category.MOVEIS, PENDING_MODERATION, T0.plusSeconds(600));
        insert("Aprovado antigo aaaa", Category.MOVEIS, APPROVED, T0);

        assertThat(adapter.findPendingCreatedBefore(T0.plusSeconds(60), 10))
                .extracting(Ad::id).containsExactly(oldPending.id());
    }

    @Test
    void enforcesReferentialIntegrityForImages() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO ad_images (ad_id, position, url) VALUES (?, 0, 'https://x.com/a.jpg')", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesThatOnlyRejectedAdsHaveReason() {
        Ad ad = insert("Aprovado sem motivo", Category.MOVEIS, APPROVED, T0);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE ads SET rejection_reason = 'prohibited_item' WHERE id = ?", ad.id().value()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Ad insert(String title, Category category, ModerationStatus status, Instant createdAt) {
        Ad ad = AdFixtures.storedAd(title, category, status, createdAt);
        adapter.insert(ad);
        return ad;
    }
}
