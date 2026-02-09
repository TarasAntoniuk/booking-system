package com.tarasantoniuk.booking.config;

/**
 * Time-related constants for booking operations.
 * Defines timing rules and windows for booking lifecycle.
 */
public final class BookingTimeConstants {

    private BookingTimeConstants() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }

    /**
     * Time window (in minutes) for completing booking payment.
     * After this period, unpaid bookings automatically expire.
     */
    public static final int BOOKING_EXPIRATION_MINUTES = 15;
}
