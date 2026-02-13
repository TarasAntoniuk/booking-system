package com.tarasantoniuk.booking.service;

import com.tarasantoniuk.booking.dto.BookingResponseDto;
import com.tarasantoniuk.booking.dto.CreateBookingRequestDto;
import com.tarasantoniuk.booking.entity.Booking;
import com.tarasantoniuk.booking.enums.BookingStatus;
import com.tarasantoniuk.booking.event.BookingEvent;
import com.tarasantoniuk.booking.exception.UnitNotAvailableException;
import com.tarasantoniuk.booking.repository.BookingRepository;
import com.tarasantoniuk.common.TestFixtures;
import com.tarasantoniuk.common.exception.ResourceNotFoundException;
import com.tarasantoniuk.unit.entity.Unit;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Unit testUnit;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testUser = TestFixtures.createTestUser();
        testUnit = TestFixtures.createTestUnit();
        testBooking = TestFixtures.createTestBooking(testUnit, testUser);
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

        when(unitRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testUnit));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookingRepository.findConflictingBookings(eq(1L), any(), any())).thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        // When
        BookingResponseDto response = bookingService.createBooking(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);

        verify(unitRepository).findByIdWithLock(1L);
        verify(userRepository).findById(1L);
        verify(bookingRepository).save(any(Booking.class));
        verify(eventPublisher).publishEvent(any(BookingEvent.class));
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

        when(unitRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(ResourceNotFoundException.class)
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

        when(unitRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testUnit));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(ResourceNotFoundException.class)
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

        when(unitRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testUnit));
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
    @DisplayName("Should throw exception when end date is before start date")
    void shouldThrowExceptionWhenEndDateBeforeStartDate() {
        // Given
        CreateBookingRequestDto request = new CreateBookingRequestDto();
        request.setUnitId(1L);
        request.setUserId(1L);
        request.setStartDate(LocalDate.now().plusDays(5));
        request.setEndDate(LocalDate.now().plusDays(2));

        when(unitRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testUnit));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date must be on or after start date");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should allow booking when end date equals start date")
    void shouldAllowBookingWhenEndDateEqualsStartDate() {
        // Given
        LocalDate sameDate = LocalDate.now().plusDays(5);
        CreateBookingRequestDto request = new CreateBookingRequestDto();
        request.setUnitId(1L);
        request.setUserId(1L);
        request.setStartDate(sameDate);
        request.setEndDate(sameDate);

        when(unitRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testUnit));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookingRepository.findConflictingBookings(eq(1L), any(), any())).thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        // When
        BookingResponseDto response = bookingService.createBooking(request);

        // Then
        assertThat(response).isNotNull();
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should get booking by id successfully")
    void shouldGetBookingByIdSuccessfully() {
        // Given
        when(bookingRepository.findByIdWithUnit(1L)).thenReturn(Optional.of(testBooking));

        // When
        BookingResponseDto response = bookingService.getBookingById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);
        verify(bookingRepository).findByIdWithUnit(1L);
    }

    @Test
    @DisplayName("Should throw exception when booking not found")
    void shouldThrowExceptionWhenBookingNotFound() {
        // Given
        when(bookingRepository.findByIdWithUnit(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.getBookingById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Booking not found");
    }

    @Test
    @DisplayName("Should get user bookings successfully with pagination")
    void shouldGetUserBookingsSuccessfully() {
        // Given
        Booking booking2 = new Booking();
        booking2.setId(2L);
        booking2.setUnit(testUnit);
        booking2.setUser(testUser);
        booking2.setStartDate(LocalDate.now().plusDays(5));
        booking2.setEndDate(LocalDate.now().plusDays(7));
        booking2.setStatus(BookingStatus.CONFIRMED);

        Pageable pageable = PageRequest.of(0, 20);
        Page<Booking> bookingPage = new PageImpl<>(List.of(testBooking, booking2), pageable, 2);
        when(bookingRepository.findByUserIdWithUnit(1L, pageable)).thenReturn(bookingPage);

        // When
        Page<BookingResponseDto> bookings = bookingService.getUserBookings(1L, pageable);

        // Then
        assertThat(bookings.getContent()).hasSize(2);
        assertThat(bookings.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(bookings.getContent().get(1).getId()).isEqualTo(2L);
        assertThat(bookings.getTotalElements()).isEqualTo(2);
        verify(bookingRepository).findByUserIdWithUnit(1L, pageable);
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
        verify(eventPublisher).publishEvent(any(BookingEvent.class));
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
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Booking is already cancelled");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw exception when booking not found during cancellation")
    void shouldThrowExceptionWhenBookingNotFoundDuringCancellation() {
        // Given
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.cancelBooking(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Booking not found with id: 999");

        verify(bookingRepository).findById(999L);
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

        when(unitRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testUnit));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // findConflictingBookings checks both CONFIRMED and PENDING bookings
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

        when(unitRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testUnit));
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
