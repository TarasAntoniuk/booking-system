package com.tarasantoniuk.booking.scheduler;

import com.tarasantoniuk.booking.repository.BookingRepository;
import com.tarasantoniuk.event.enums.EventType;
import com.tarasantoniuk.event.service.EventService;
import com.tarasantoniuk.statistic.service.UnitStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.tarasantoniuk.booking.config.BookingTimeConstants.SCHEDULER_FIXED_DELAY_MS;
import static com.tarasantoniuk.booking.config.BookingTimeConstants.SCHEDULER_INITIAL_DELAY_MS;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpirationScheduler {

    private final BookingRepository bookingRepository;
    private final EventService eventService;
    private final UnitStatisticsService unitStatisticsService;


    /**
     * Runs every minute to cancel expired bookings.
     * Uses bulk UPDATE for optimal performance (single SQL query).
     */
    @Scheduled(fixedDelay = SCHEDULER_FIXED_DELAY_MS, initialDelay = SCHEDULER_INITIAL_DELAY_MS)
    @Transactional
    public void cancelExpiredBookings() {
        try {
            LocalDateTime now = LocalDateTime.now();

            // 1. Get IDs of expired bookings (for audit events)
            List<Long> expiredBookingIds = bookingRepository.findExpiredPendingBookingIds(now);

            if (expiredBookingIds.isEmpty()) {
                return;
            }

            log.info("Found {} expired bookings to cancel", expiredBookingIds.size());

            // 2. Bulk cancel - single UPDATE query
            int cancelledCount = bookingRepository.bulkCancelExpiredBookings(now);

            // 3. Create audit events in batch
            eventService.createEventsInBatch(EventType.BOOKING_EXPIRED, expiredBookingIds);

            unitStatisticsService.invalidateAvailableUnitsCache();

            log.info("Successfully cancelled {} expired bookings", cancelledCount);
        } catch (Exception e) {
            log.error("Failed to cancel expired bookings", e);
        }
    }
}