package com.tarasantoniuk.booking.service;

import com.tarasantoniuk.booking.dto.BookingResponseDto;
import com.tarasantoniuk.booking.dto.CreateBookingRequestDto;
import com.tarasantoniuk.booking.entity.Booking;
import com.tarasantoniuk.booking.enums.BookingStatus;
import com.tarasantoniuk.booking.exception.UnitNotAvailableException;
import com.tarasantoniuk.booking.repository.BookingRepository;
import com.tarasantoniuk.event.enums.EventType;
import com.tarasantoniuk.event.service.EventService;
import com.tarasantoniuk.payment.service.PaymentService;
import com.tarasantoniuk.statistic.service.CacheService;
import com.tarasantoniuk.unit.entity.Unit;
import com.tarasantoniuk.unit.enums.AccommodationType;
import com.tarasantoniuk.unit.repository.UnitRepository;
import com.tarasantoniuk.user.entity.User;
import com.tarasantoniuk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Unit Tests")
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private EventService eventService;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Unit testUnit;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testUnit = new Unit();
        testUnit.setId(1L);
        testUnit.setNumberOfRooms(2);
        testUnit.setAccommodationType(AccommodationType.FLAT);
        testUnit.setFloor(3);
        testUnit.setBaseCost(BigDecimal.valueOf(100));

        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setUnit(testUnit);
        testBooking.setUser(testUser);
        testBooking.setStartDate(LocalDate.now().plusDays(1));
        testBooking.setEndDate(LocalDate.now().plusDays(3));
        testBooking.setStatus(BookingStatus.PENDING);
        testBooking.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create booking successfully")
    void shouldCreateBookingSuccessfully() {
        // Given
        CreateBookingRequestDto request = new CreateBookingRequestDto();
        request.setUnitId(1L);
        request.setUserId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));

        when(unitRepository.findById(1L)).thenReturn(Optional.of(testUnit));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookingRepository.findConflictingBookings(eq(1L), any(), any())).thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        // When
        BookingResponseDto response = bookingService.createBooking(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);

        verify(unitRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(bookingRepository).save(any(Booking.class));
        verify(paymentService).createPayment(eq(testBooking), any(BigDecimal.class));
        verify(eventService).createEvent(EventType.BOOKING_CREATED, 1L);
    }

    @Test
    @DisplayName("Should throw exception when unit not found")
    void shouldThrowExceptionWhenUnitNotFound() {
        // Given
        CreateBookingRequestDto request = new CreateBookingRequestDto();
        request.setUnitId(999L);
        request.setUserId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));

        when(unitRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit not found");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        CreateBookingRequestDto request = new CreateBookingRequestDto();
        request.setUnitId(1L);
        request.setUserId(999L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));

        when(unitRepository.findById(1L)).thenReturn(Optional.of(testUnit));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw exception when unit not available")
    void shouldThrowExceptionWhenUnitNotAvailable() {
        // Given
        CreateBookingRequestDto request = new CreateBookingRequestDto();
        request.setUnitId(1L);
        request.setUserId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));

        Booking conflictingBooking = new Booking();
        conflictingBooking.setId(2L);

        when(unitRepository.findById(1L)).thenReturn(Optional.of(testUnit));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookingRepository.findConflictingBookings(eq(1L), any(), any()))
                .thenReturn(List.of(conflictingBooking));

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(UnitNotAvailableException.class)
                .hasMessageContaining("Unit is not available");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should get booking by id successfully")
    void shouldGetBookingByIdSuccessfully() {
        // Given
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // When
        BookingResponseDto response = bookingService.getBookingById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);
        verify(bookingRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when booking not found")
    void shouldThrowExceptionWhenBookingNotFound() {
        // Given
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.getBookingById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Booking not found");
    }

    @Test
    @DisplayName("Should get user bookings successfully")
    void shouldGetUserBookingsSuccessfully() {
        // Given
        Booking booking2 = new Booking();
        booking2.setId(2L);
        booking2.setUnit(testUnit);
        booking2.setUser(testUser);
        booking2.setStartDate(LocalDate.now().plusDays(5));
        booking2.setEndDate(LocalDate.now().plusDays(7));
        booking2.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findByUserId(1L)).thenReturn(List.of(testBooking, booking2));

        // When
        List<BookingResponseDto> bookings = bookingService.getUserBookings(1L);

        // Then
        assertThat(bookings).hasSize(2);
        assertThat(bookings.get(0).getId()).isEqualTo(1L);
        assertThat(bookings.get(1).getId()).isEqualTo(2L);
        verify(bookingRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("Should cancel booking successfully")
    void shouldCancelBookingSuccessfully() {
        // Given
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        // When
        bookingService.cancelBooking(1L, 1L);

        // Then
        verify(bookingRepository).findById(1L);
        verify(bookingRepository).save(testBooking);
        verify(eventService).createEvent(EventType.BOOKING_CANCELLED, 1L);
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should throw exception when cancelling other user's booking")
    void shouldThrowExceptionWhenCancellingOtherUsersBooking() {
        // Given
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // When & Then
        assertThatThrownBy(() -> bookingService.cancelBooking(1L, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("You can only cancel your own bookings");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw exception when cancelling already cancelled booking")
    void shouldThrowExceptionWhenCancellingAlreadyCancelledBooking() {
        // Given
        testBooking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // When & Then
        assertThatThrownBy(() -> bookingService.cancelBooking(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Booking is already cancelled");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("BUG: Should prevent double booking when first booking is PENDING")
    void shouldPreventDoubleBookingWhenFirstBookingIsPending() {
        // Given - First booking with PENDING status
        Booking pendingBooking = new Booking();
        pendingBooking.setId(1L);
        pendingBooking.setUnit(testUnit);
        pendingBooking.setUser(testUser);
        pendingBooking.setStartDate(LocalDate.of(2026, 1, 22));
        pendingBooking.setEndDate(LocalDate.of(2026, 1, 22));
        pendingBooking.setStatus(BookingStatus.PENDING);

        CreateBookingRequestDto request = new CreateBookingRequestDto();
        request.setUnitId(1L);
        request.setUserId(1L);
        request.setStartDate(LocalDate.of(2026, 1, 22));
        request.setEndDate(LocalDate.of(2026, 1, 22));

        when(unitRepository.findById(1L)).thenReturn(Optional.of(testUnit));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // BUG: findConflictingBookings only checks CONFIRMED bookings, not PENDING
        // This allows the second booking to be created even though there's a PENDING booking
        when(bookingRepository.findConflictingBookings(eq(1L), any(), any()))
                .thenReturn(List.of(pendingBooking)); // Should find the pending booking

        // When & Then
        // This SHOULD throw UnitNotAvailableException but currently doesn't
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(UnitNotAvailableException.class)
                .hasMessageContaining("Unit is not available");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("BUG: Should prevent double booking for same dates (scenario from issue)")
    void shouldPreventDoubleBookingForSameDates() {
        // Given - Simulating the exact scenario from the bug report
        LocalDate sameDate = LocalDate.of(2026, 1, 22);

        CreateBookingRequestDto firstRequest = new CreateBookingRequestDto();
        firstRequest.setUnitId(1L);
        firstRequest.setUserId(1L);
        firstRequest.setStartDate(sameDate);
        firstRequest.setEndDate(sameDate);

        CreateBookingRequestDto secondRequest = new CreateBookingRequestDto();
        secondRequest.setUnitId(1L);
        secondRequest.setUserId(1L);
        secondRequest.setStartDate(sameDate);
        secondRequest.setEndDate(sameDate);

        // Setup for first booking
        Booking firstBooking = new Booking();
        firstBooking.setId(1L);
        firstBooking.setUnit(testUnit);
        firstBooking.setUser(testUser);
        firstBooking.setStartDate(sameDate);
        firstBooking.setEndDate(sameDate);
        firstBooking.setStatus(BookingStatus.PENDING);

        when(unitRepository.findById(1L)).thenReturn(Optional.of(testUnit));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // First booking creation - no conflicts
        when(bookingRepository.findConflictingBookings(eq(1L), any(), any()))
                .thenReturn(List.of()) // No conflicts initially
                .thenReturn(List.of(firstBooking)); // Should find first booking on second attempt

        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(firstBooking);

        // When - Create first booking (should succeed)
        BookingResponseDto firstResult = bookingService.createBooking(firstRequest);
        assertThat(firstResult).isNotNull();

        // Then - Try to create second booking for same dates (should fail)
        assertThatThrownBy(() -> bookingService.createBooking(secondRequest))
                .isInstanceOf(UnitNotAvailableException.class)
                .hasMessageContaining("Unit is not available");
    }
}
