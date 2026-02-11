package com.tarasantoniuk.booking.event;

import com.tarasantoniuk.event.enums.EventType;
import com.tarasantoniuk.event.service.EventService;
import com.tarasantoniuk.payment.service.PaymentService;
import com.tarasantoniuk.statistic.service.UnitStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Handles cross-cutting concerns triggered by booking state changes:
 * audit event creation, cache invalidation, and payment creation.
 * Decouples BookingService from EventService, UnitStatisticsService, and PaymentService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventListener {

    private final EventService eventService;
    private final UnitStatisticsService unitStatisticsService;
    private final PaymentService paymentService;

    @EventListener
    public void handleBookingEvent(BookingEvent event) {
        log.debug("Handling booking event: type={}, bookingId={}", event.type(), event.bookingId());

        switch (event.type()) {
            case CREATED -> handleCreated(event);
            case CONFIRMED -> handleConfirmed(event);
            case CANCELLED -> handleCancelled(event);
        }

        unitStatisticsService.invalidateAvailableUnitsCache();
    }

    private void handleCreated(BookingEvent event) {
        paymentService.createPaymentForBooking(event.bookingId(), event.totalCost());
        eventService.createEvent(EventType.BOOKING_CREATED, event.bookingId());
    }

    private void handleConfirmed(BookingEvent event) {
        eventService.createEvent(EventType.BOOKING_CONFIRMED, event.bookingId());
    }

    private void handleCancelled(BookingEvent event) {
        eventService.createEvent(EventType.BOOKING_CANCELLED, event.bookingId());
    }
}
