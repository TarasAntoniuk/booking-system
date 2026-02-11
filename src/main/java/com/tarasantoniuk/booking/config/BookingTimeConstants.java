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

    /**
     * Interval (in milliseconds) between scheduler runs to cancel expired bookings.
     */
    public static final long SCHEDULER_FIXED_DELAY_MS = 60_000;

    /**
     * Initial delay (in milliseconds) before the first scheduler run after startup.
     */
    public static final long SCHEDULER_INITIAL_DELAY_MS = 10_000;
}
