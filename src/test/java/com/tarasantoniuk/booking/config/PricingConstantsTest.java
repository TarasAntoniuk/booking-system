package com.tarasantoniuk.booking.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PricingConstants Tests")
class PricingConstantsTest {

    @Test
    @DisplayName("Should have correct MARKUP_RATE value")
    void shouldHaveCorrectMarkupRate() {
        // Then
        assertThat(PricingConstants.MARKUP_RATE)
                .isNotNull()
                .isEqualTo(BigDecimal.valueOf(0.15))
                .isEqualByComparingTo("0.15");
    }

    @Test
    @DisplayName("Should have correct MARKUP_MULTIPLIER value")
    void shouldHaveCorrectMarkupMultiplier() {
        // Then
        assertThat(PricingConstants.MARKUP_MULTIPLIER)
                .isNotNull()
                .isEqualTo(BigDecimal.valueOf(1.15))
                .isEqualByComparingTo("1.15");
    }

    @Test
    @DisplayName("Should verify MARKUP_MULTIPLIER is calculated correctly")
    void shouldVerifyMarkupMultiplierCalculation() {
        // Given
        BigDecimal expected = BigDecimal.ONE.add(PricingConstants.MARKUP_RATE);

        // Then
        assertThat(PricingConstants.MARKUP_MULTIPLIER)
                .isEqualByComparingTo(expected)
                .as("MARKUP_MULTIPLIER should equal 1.0 + MARKUP_RATE");
    }

    @Test
    @DisplayName("Should throw exception when trying to instantiate utility class")
    void shouldThrowExceptionWhenInstantiating() throws Exception {
        // Given
        Constructor<PricingConstants> constructor = PricingConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When & Then
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .getCause()
                .hasMessageContaining("Utility class - cannot be instantiated");
    }
}
