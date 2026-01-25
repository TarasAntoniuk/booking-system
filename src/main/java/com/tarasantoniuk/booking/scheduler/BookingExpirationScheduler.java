package com.tarasantoniuk.booking.scheduler;

import com.tarasantoniuk.booking.entity.Booking;
import com.tarasantoniuk.booking.enums.BookingStatus;
import com.tarasantoniuk.booking.repository.BookingRepository;
import com.tarasantoniuk.event.enums.EventType;
import com.tarasantoniuk.event.service.EventService;
import com.tarasantoniuk.statistic.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpirationScheduler {

    private final BookingRepository bookingRepository;
    private final EventService eventService;
    private final CacheService cacheService;


    /**
     * Runs every minute to cancel expired bookings
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000) // 60 seconds delay, 10 seconds initial delay
    @Transactional
    public void cancelExpiredBookings() {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> expiredBookings = bookingRepository
                .findByStatusAndExpiresAtBefore(BookingStatus.PENDING, now);

        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("Found {} expired bookings to cancel", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            eventService.createEvent(EventType.BOOKING_EXPIRED, booking.getId());
            log.debug("Cancelled expired booking ID: {}", booking.getId());
        }

        cacheService.invalidateCache();

        log.info("Successfully cancelled {} expired bookings", expiredBookings.size());
    }
}