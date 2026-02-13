package com.tarasantoniuk.booking.event;

import com.tarasantoniuk.event.enums.EventType;
import com.tarasantoniuk.event.service.EventService;
import com.tarasantoniuk.payment.service.PaymentService;
import com.tarasantoniuk.statistic.service.UnitStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Handles cross-cutting concerns triggered by booking state changes:
 * audit event creation, cache invalidation, and payment creation.
 * Decouples BookingService from EventService, UnitStatisticsService, and PaymentService.
 *
 * Payment creation runs synchronously (same transaction) to guarantee consistency.
 * Audit logging and cache invalidation run after commit to avoid rollback on non-critical failures.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventListener {

    private final EventService eventService;
    private final UnitStatisticsService unitStatisticsService;
    private final PaymentService paymentService;

    /**
     * Critical operations that must succeed with the booking transaction.
     * Payment creation is transactionally coupled to booking creation.
     */
    @EventListener
    public void handleBookingEvent(BookingEvent event) {
        log.debug("Handling booking event: type={}, bookingId={}", event.type(), event.bookingId());

        if (event.type() == BookingEvent.Type.CREATED) {
            paymentService.createPaymentForBooking(event.bookingId(), event.totalCost());
        }
    }

    /**
     * Non-critical operations that run after the transaction commits.
     * Failures here won't roll back the booking/payment transaction.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingEventAfterCommit(BookingEvent event) {
        log.debug("Post-commit handling: type={}, bookingId={}", event.type(), event.bookingId());

        switch (event.type()) {
            case CREATED -> eventService.createEvent(EventType.BOOKING_CREATED, event.bookingId());
            case CONFIRMED -> eventService.createEvent(EventType.BOOKING_CONFIRMED, event.bookingId());
            case CANCELLED -> eventService.createEvent(EventType.BOOKING_CANCELLED, event.bookingId());
        }

        unitStatisticsService.invalidateAvailableUnitsCache();
    }
}
