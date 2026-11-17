package br.com.olx.marketplace.domain.ad;

import br.com.olx.marketplace.support.AdFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static br.com.olx.marketplace.support.AdFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdTest {

    private static final Instant LATER = NOW.plusSeconds(2);

    @Test
    void newAdStartsPendingModeration() {
        Ad ad = AdFixtures.pendingAd();

        assertThat(ad.status()).isEqualTo(ModerationStatus.PENDING_MODERATION);
        assertThat(ad.status().isPublished()).isFalse();
        assertThat(ad.rejectionReason()).isEmpty();
        assertThat(ad.moderatedAt()).isEmpty();
        assertThat(ad.createdAt()).isEqualTo(NOW);
        assertThat(ad.images()).extracting(ImageUrl::value).containsExactly("https://img.example.com/bike.jpg");
    }

    @Test
    void requiresAtLeastOneImage() {
        assertThatThrownBy(() -> createWithImages(List.of()))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("at least one image is required")
                .extracting("field").isEqualTo("images");
    }

    @Test
    void limitsNumberOfImages() {
        List<ImageUrl> images = Collections.nCopies(Ad.MAX_IMAGES + 1, new ImageUrl("https://img.example.com/a.jpg"));
        assertThatThrownBy(() -> createWithImages(images)).isInstanceOf(DomainValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void requiresDescription(String description) {
        assertThatThrownBy(() -> Ad.create(new AdTitle("Bicicleta aro 29"), description,
                new Price(BigDecimal.TEN), Category.ESPORTES, List.of(new ImageUrl("https://x.com/a.jpg")), NOW))
                .isInstanceOf(DomainValidationException.class)
                .extracting("field").isEqualTo("description");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://x.com/a.jpg", "imagem.jpg", "https://", "not a url"})
    void rejectsImagesThatAreNotAbsoluteHttpUrls(String url) {
        assertThatThrownBy(() -> new ImageUrl(url)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void approvesPendingAd() {
        Ad ad = AdFixtures.pendingAd();

        ad.approve(LATER);

        assertThat(ad.status()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(ad.status().isPublished()).isTrue();
        assertThat(ad.moderatedAt()).contains(LATER);
        assertThat(ad.updatedAt()).isEqualTo(LATER);
        assertThat(ad.rejectionReason()).isEmpty();
    }

    @Test
    void rejectsPendingAdKeepingTheReason() {
        Ad ad = AdFixtures.pendingAd();

        ad.reject(new RejectionReason("contact_info_not_allowed"), LATER);

        assertThat(ad.status()).isEqualTo(ModerationStatus.REJECTED);
        assertThat(ad.status().isPublished()).isFalse();
        assertThat(ad.rejectionReason()).map(RejectionReason::code).contains("contact_info_not_allowed");
        assertThat(ad.moderatedAt()).contains(LATER);
    }

    @Test
    void rejectionRequiresReason() {
        Ad ad = AdFixtures.pendingAd();

        assertThatThrownBy(() -> ad.reject(null, LATER)).isInstanceOf(NullPointerException.class);
        assertThat(ad.status()).isEqualTo(ModerationStatus.PENDING_MODERATION);
    }

    @Test
    void approvedAdCannotBeModeratedAgain() {
        Ad ad = AdFixtures.pendingAd();
        ad.approve(LATER);

        assertThatThrownBy(() -> ad.reject(new RejectionReason("prohibited_item"), LATER))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> ad.approve(LATER)).isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(ad.status()).isEqualTo(ModerationStatus.APPROVED);
    }

    @Test
    void rejectedAdCannotBeApproved() {
        Ad ad = AdFixtures.pendingAd();
        ad.reject(new RejectionReason("prohibited_item"), LATER);

        assertThatThrownBy(() -> ad.approve(LATER)).isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(ad.status()).isEqualTo(ModerationStatus.REJECTED);
        assertThat(ad.rejectionReason()).map(RejectionReason::code).contains("prohibited_item");
    }

    @Test
    void categoryIsResolvedFromContractCode() {
        assertThat(Category.fromCode("eletronicos")).isEqualTo(Category.ELETRONICOS);
        assertThatThrownBy(() -> Category.fromCode("ELETRONICOS"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("eletronicos, moveis, veiculos, imoveis, esportes, colecionaveis");
    }

    private static Ad createWithImages(List<ImageUrl> images) {
        return Ad.create(new AdTitle("Bicicleta aro 29"), "Descrição", new Price(BigDecimal.TEN), Category.ESPORTES,
                images, NOW);
    }
}
