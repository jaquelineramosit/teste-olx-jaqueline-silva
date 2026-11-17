package br.com.olx.marketplace.domain.ad;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceTest {

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-0.01", "-100"})
    void rejectsZeroAndNegativePrices(String amount) {
        assertThatThrownBy(() -> new Price(new BigDecimal(amount)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessage("price must be greater than zero");
    }

    @Test
    void acceptsSmallestPositiveCent() {
        assertThat(new Price(new BigDecimal("0.01")).amount()).isEqualByComparingTo("0.01");
    }

    @Test
    void normalizesToTwoDecimalPlaces() {
        assertThat(new Price(new BigDecimal("80")).amount()).isEqualTo(new BigDecimal("80.00"));
        assertThat(new Price(new BigDecimal("80.500")).amount()).isEqualTo(new BigDecimal("80.50"));
    }

    @Test
    void rejectsMoreThanTwoDecimalPlaces() {
        assertThatThrownBy(() -> new Price(new BigDecimal("10.999")))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("2 decimal places");
    }

    @Test
    void requiresPrice() {
        assertThatThrownBy(() -> new Price(null)).isInstanceOf(DomainValidationException.class);
    }
}
