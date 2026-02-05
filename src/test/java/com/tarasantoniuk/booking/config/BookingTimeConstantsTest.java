package com.tarasantoniuk.booking.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BookingTimeConstants Tests")
class BookingTimeConstantsTest {

    @Test
    @DisplayName("Should have correct BOOKING_EXPIRATION_MINUTES value")
    void shouldHaveCorrectBookingExpirationMinutes() {
        // Then
        assertThat(BookingTimeConstants.BOOKING_EXPIRATION_MINUTES)
                .isEqualTo(15)
                .as("Booking expiration should be 15 minutes");
    }

    @Test
    @DisplayName("Should verify BOOKING_EXPIRATION_MINUTES is positive")
    void shouldVerifyBookingExpirationMinutesIsPositive() {
        // Then
        assertThat(BookingTimeConstants.BOOKING_EXPIRATION_MINUTES)
                .isPositive()
                .as("Expiration time must be positive");
    }

    @Test
    @DisplayName("Should throw exception when trying to instantiate utility class")
    void shouldThrowExceptionWhenInstantiating() throws Exception {
        // Given
        Constructor<BookingTimeConstants> constructor = BookingTimeConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // When & Then
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .getCause()
                .hasMessageContaining("Utility class - cannot be instantiated");
    }
}
