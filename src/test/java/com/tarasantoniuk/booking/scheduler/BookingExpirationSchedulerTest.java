package com.tarasantoniuk.booking.scheduler;

import com.tarasantoniuk.booking.entity.Booking;
import com.tarasantoniuk.booking.enums.BookingStatus;
import com.tarasantoniuk.booking.repository.BookingRepository;
import com.tarasantoniuk.event.enums.EventType;
import com.tarasantoniuk.event.service.EventService;
import com.tarasantoniuk.statistic.service.CacheService;
import com.tarasantoniuk.unit.entity.Unit;
import com.tarasantoniuk.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private CacheService cacheService;

    @InjectMocks
    private BookingExpirationScheduler bookingExpirationScheduler;

    private User testUser;
    private Unit testUnit;
    private List<Booking> expiredBookings;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testUnit = new Unit();
        testUnit.setId(1L);

        expiredBookings = new ArrayList<>();
    }

    @Test
    @DisplayName("Should cancel expired bookings successfully")
    void shouldCancelExpiredBookingsSuccessfully() {
        // Given
        Booking booking1 = createExpiredBooking(1L, LocalDateTime.now().minusMinutes(10));
        Booking booking2 = createExpiredBooking(2L, LocalDateTime.now().minusMinutes(5));
        expiredBookings.add(booking1);
        expiredBookings.add(booking2);

        when(bookingRepository.findByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(expiredBookings);

        // When
        bookingExpirationScheduler.cancelExpiredBookings();

        // Then
        verify(bookingRepository).findByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class));
        verify(bookingRepository, times(2)).save(any(Booking.class));

        assertThat(booking1.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking2.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        verify(eventService).createEvent(EventType.BOOKING_EXPIRED, 1L);
        verify(eventService).createEvent(EventType.BOOKING_EXPIRED, 2L);
    }

    @Test
    @DisplayName("Should do nothing when no expired bookings found")
    void shouldDoNothingWhenNoExpiredBookings() {
        // Given
        when(bookingRepository.findByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        // When
        bookingExpirationScheduler.cancelExpiredBookings();

        // Then
        verify(bookingRepository).findByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(eventService, never()).createEvent(any(), any());
    }

    @Test
    @DisplayName("Should cancel only one expired booking")
    void shouldCancelOnlyOneExpiredBooking() {
        // Given
        Booking booking = createExpiredBooking(1L, LocalDateTime.now().minusMinutes(1));
        expiredBookings.add(booking);

        when(bookingRepository.findByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(expiredBookings);

        // When
        bookingExpirationScheduler.cancelExpiredBookings();

        // Then
        verify(bookingRepository).save(booking);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(eventService).createEvent(EventType.BOOKING_EXPIRED, 1L);
    }

    @Test
    @DisplayName("Should cancel multiple expired bookings in batch")
    void shouldCancelMultipleExpiredBookingsInBatch() {
        // Given
        for (int i = 1; i <= 5; i++) {
            expiredBookings.add(createExpiredBooking((long) i, LocalDateTime.now().minusMinutes(i)));
        }

        when(bookingRepository.findByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(expiredBookings);

        // When
        bookingExpirationScheduler.cancelExpiredBookings();

        // Then
        verify(bookingRepository, times(5)).save(any(Booking.class));
        verify(eventService, times(5)).createEvent(eq(EventType.BOOKING_EXPIRED), anyLong());

        expiredBookings.forEach(booking ->
                assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED)
        );
    }

    @Test
    @DisplayName("Should use current time when checking for expired bookings")
    void shouldUseCurrentTimeWhenCheckingForExpiredBookings() {
        // Given
        when(bookingRepository.findByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        // When
        bookingExpirationScheduler.cancelExpiredBookings();

        // Then
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(bookingRepository).findByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), timeCaptor.capture());

        LocalDateTime capturedTime = timeCaptor.getValue();
        assertThat(capturedTime).isBetween(
                LocalDateTime.now().minusSeconds(5),
                LocalDateTime.now().plusSeconds(5)
        );
    }

    @Test
    @DisplayName("Should save each booking individually")
    void shouldSaveEachBookingIndividually() {
        // Given
        Booking booking1 = createExpiredBooking(1L, LocalDateTime.now().minusMinutes(10));
        Booking booking2 = createExpiredBooking(2L, LocalDateTime.now().minusMinutes(5));
        expiredBookings.add(booking1);
        expiredBookings.add(booking2);

        when(bookingRepository.findByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(expiredBookings);

        // When
        bookingExpirationScheduler.cancelExpiredBookings();

        // Then
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository, times(2)).save(bookingCaptor.capture());

        List<Booking> savedBookings = bookingCaptor.getAllValues();
        assertThat(savedBookings).containsExactlyInAnyOrder(booking1, booking2);
    }

    @Test
    @DisplayName("Should create event for each cancelled booking")
    void shouldCreateEventForEachCancelledBooking() {
        // Given
        Booking booking1 = createExpiredBooking(1L, LocalDateTime.now().minusMinutes(10));
        Booking booking2 = createExpiredBooking(2L, LocalDateTime.now().minusMinutes(5));
        Booking booking3 = createExpiredBooking(3L, LocalDateTime.now().minusMinutes(1));
        expiredBookings.add(booking1);
        expiredBookings.add(booking2);
        expiredBookings.add(booking3);

        when(bookingRepository.findByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(expiredBookings);

        // When
        bookingExpirationScheduler.cancelExpiredBookings();

        // Then
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        verify(eventService, times(3)).createEvent(eq(EventType.BOOKING_EXPIRED), idCaptor.capture());

        List<Long> capturedIds = idCaptor.getAllValues();
        assertThat(capturedIds).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    private Booking createExpiredBooking(Long id, LocalDateTime expiresAt) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(expiresAt);
        booking.setUser(testUser);
        booking.setUnit(testUnit);
        booking.setStartDate(LocalDate.now().plusDays(1));
        booking.setEndDate(LocalDate.now().plusDays(3));
        return booking;
    }
}
