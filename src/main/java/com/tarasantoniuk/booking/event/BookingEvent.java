package com.tarasantoniuk.booking.event;

import java.math.BigDecimal;

/**
 * Domain event published when a booking state changes.
 * Listened to by BookingEventListener for cross-cutting concerns
 * (audit events, cache invalidation, payment creation).
 */
public record BookingEvent(
        Type type,
        Long bookingId,
        BigDecimal totalCost
) {
    public enum Type {
        CREATED,
        CONFIRMED,
        CANCELLED
    }

    public static BookingEvent created(Long bookingId, BigDecimal totalCost) {
        return new BookingEvent(Type.CREATED, bookingId, totalCost);
    }

    public static BookingEvent confirmed(Long bookingId) {
        return new BookingEvent(Type.CONFIRMED, bookingId, null);
    }

    public static BookingEvent cancelled(Long bookingId) {
        return new BookingEvent(Type.CANCELLED, bookingId, null);
    }
}
