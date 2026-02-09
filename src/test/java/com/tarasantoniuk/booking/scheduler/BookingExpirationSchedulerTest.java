package com.tarasantoniuk.booking.scheduler;

import com.tarasantoniuk.booking.repository.BookingRepository;
import com.tarasantoniuk.event.enums.EventType;
import com.tarasantoniuk.event.service.EventService;
import com.tarasantoniuk.statistic.service.UnitStatisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingExpirationScheduler Unit Tests")
class BookingExpirationSchedulerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventService eventService;

    @Mock
    private UnitStatisticsService unitStatisticsService;

    @InjectMocks
    private BookingExpirationScheduler bookingExpirationScheduler;

    @Test
    @DisplayName("Should cancel expired bookings using bulk UPDATE")
    void shouldCancelExpiredBookingsSuccessfully() {
        // Given
        List<Long> expiredIds = List.of(1L, 2L);
        when(bookingRepository.findExpiredPendingBookingIds(any(LocalDateTime.class)))
                .thenReturn(expiredIds);
        when(bookingRepository.bulkCancelExpiredBookings(any(LocalDateTime.class)))
                .thenReturn(2);

        // When
        bookingExpirationScheduler.cancelExpiredBookings();

        // Then
        verify(bookingRepository).findExpiredPendingBookingIds(any(LocalDateTime.class));
        verify(bookingRepository).bulkCancelExpiredBookings(any(LocalDateTime.class));
        verify(eventService).createEventsInBatch(eq(EventType.BOOKING_EXPIRED), eq(expiredIds));
        verify(unitStatisticsService).invalidateAvailableUnitsCache();
    }

    @Test
    @DisplayName("Should do nothing when no expired bookings found")
    void shouldDoNothingWhenNoExpiredBookings() {
        // Given
        when(bookingRepository.findExpiredPendingBookingIds(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        bookingExpirationScheduler.cancelExpiredBookings();

        // Then
        verify(bookingRepository).findExpiredPendingBookingIds(any(LocalDateTime.class));
        verify(bookingRepository, never()).bulkCancelExpiredBookings(any());
        verify(eventService, never()).createEventsInBatch(any(), any());
        verify(unitStatisticsService, never()).invalidateAvailableUnitsCache();
    }

    @Test
    @DisplayName("Should cancel single expired booking using bulk UPDATE")
    void shouldCancelOnlyOneExpiredBooking() {
        // Given
        List<Long> expiredIds = List.of(1L);
        when(bookingRepository.findExpiredPendingBookingIds(any(LocalDateTime.class)))
                .thenReturn(expiredIds);
        when(bookingRepository.bulkCancelExpiredBookings(any(LocalDateTime.class)))
                .thenReturn(1);

        // When
        bookingExpirationScheduler.cancelExpiredBookings();

        // Then
        verify(bookingRepository).bulkCancelExpiredBookings(any(LocalDateTime.class));
        verify(eventService).createEventsInBatch(EventType.BOOKING_EXPIRED, expiredIds);
    }

    @Test
    @DisplayName("Should cancel multiple expired bookings using single bulk UPDATE")
    void shouldCancelMultipleExpiredBookingsInBatch() {
        // Given
        List<Long> expiredIds = List.of(1L, 2L, 3L, 4L, 5L);
        when(bookingRepository.findExpiredPendingBookingIds(any(LocalDateTime.class)))
                .thenReturn(expiredIds);
        when(bookingRepository.bulkCancelExpiredBookings(any(LocalDateTime.class)))
                .thenReturn(5);

        // When
        bookingExpirationScheduler.cancelExpiredBookings();

        // Then
        verify(bookingRepository, times(1)).bulkCancelExpiredBookings(any(LocalDateTime.class));
        verify(eventService, times(1)).createEventsInBatch(eq(EventType.BOOKING_EXPIRED), eq(expiredIds));
    }

    @Test
    @DisplayName("Should use current time when checking for expired bookings")
    void shouldUseCurrentTimeWhenCheckingForExpiredBookings() {
        // Given
        when(bookingRepository.findExpiredPendingBookingIds(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        bookingExpirationScheduler.cancelExpiredBookings();

        // Then
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(bookingRepository).findExpiredPendingBookingIds(timeCaptor.capture());

        LocalDateTime capturedTime = timeCaptor.getValue();
        assertThat(capturedTime).isBetween(
                LocalDateTime.now().minusSeconds(5),
                LocalDateTime.now().plusSeconds(5)
        );
    }

    @Test
    @DisplayName("Should use same timestamp for finding IDs and bulk cancel")
    void shouldUseSameTimestampForFindAndCancel() {
        // Given
        List<Long> expiredIds = List.of(1L, 2L);
        when(bookingRepository.findExpiredPendingBookingIds(any(LocalDateTime.class)))
                .thenReturn(expiredIds);
        when(bookingRepository.bulkCancelExpiredBookings(any(LocalDateTime.class)))
                .thenReturn(2);

        // When
        bookingExpirationScheduler.cancelExpiredBookings();

        // Then
        ArgumentCaptor<LocalDateTime> findTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> cancelTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(bookingRepository).findExpiredPendingBookingIds(findTimeCaptor.capture());
        verify(bookingRepository).bulkCancelExpiredBookings(cancelTimeCaptor.capture());

        assertThat(findTimeCaptor.getValue()).isEqualTo(cancelTimeCaptor.getValue());
    }

    @Test
    @DisplayName("Should create events in batch for all cancelled bookings")
    void shouldCreateEventsInBatchForAllCancelledBookings() {
        // Given
        List<Long> expiredIds = List.of(1L, 2L, 3L);
        when(bookingRepository.findExpiredPendingBookingIds(any(LocalDateTime.class)))
                .thenReturn(expiredIds);
        when(bookingRepository.bulkCancelExpiredBookings(any(LocalDateTime.class)))
                .thenReturn(3);

        // When
        bookingExpirationScheduler.cancelExpiredBookings();

        // Then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> idCaptor = ArgumentCaptor.forClass(List.class);
        verify(eventService, times(1)).createEventsInBatch(eq(EventType.BOOKING_EXPIRED), idCaptor.capture());

        List<Long> capturedIds = idCaptor.getValue();
        assertThat(capturedIds).containsExactlyInAnyOrder(1L, 2L, 3L);
    }
}
