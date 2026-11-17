package br.com.olx.marketplace.domain.ad;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdTitleTest {

    @ParameterizedTest
    @ValueSource(ints = {10, 55, 100})
    void acceptsTitlesWithinLimits(int length) {
        assertThat(new AdTitle("a".repeat(length)).value()).hasSize(length);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 9, 101})
    void rejectsTitlesOutsideLimits(int length) {
        assertThatThrownBy(() -> new AdTitle("a".repeat(length)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("between 10 and 100")
                .extracting("field").isEqualTo("title");
    }

    @Test
    void ignoresSurroundingSpacesWhenCounting() {
        assertThatThrownBy(() -> new AdTitle("   curto    ")).isInstanceOf(DomainValidationException.class);
        assertThat(new AdTitle("  Bicicleta aro 29  ").value()).isEqualTo("Bicicleta aro 29");
    }

    @Test
    void countsEmojiAsSingleCharacter() {
        // 99 letras + 1 emoji = 100 caracteres, embora o emoji ocupe 2 unidades UTF-16 (length() = 101)
        String title = "a".repeat(99) + "🚲";
        assertThat(new AdTitle(title).value()).isEqualTo(title);
    }

    @Test
    void requiresTitle() {
        assertThatThrownBy(() -> new AdTitle(null)).isInstanceOf(DomainValidationException.class);
    }
}
