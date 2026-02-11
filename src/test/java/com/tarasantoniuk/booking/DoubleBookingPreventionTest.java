package com.tarasantoniuk.booking;

import com.tarasantoniuk.booking.dto.BookingResponseDto;
import com.tarasantoniuk.booking.dto.CreateBookingRequestDto;
import com.tarasantoniuk.booking.enums.BookingStatus;
import com.tarasantoniuk.booking.exception.UnitNotAvailableException;
import com.tarasantoniuk.booking.repository.BookingRepository;
import com.tarasantoniuk.booking.service.BookingService;
import com.tarasantoniuk.common.AbstractIntegrationTest;
import com.tarasantoniuk.event.repository.EventRepository;
import com.tarasantoniuk.payment.repository.PaymentRepository;
import com.tarasantoniuk.unit.entity.Unit;
import com.tarasantoniuk.unit.enums.AccommodationType;
import com.tarasantoniuk.unit.repository.UnitRepository;
import com.tarasantoniuk.user.entity.User;
import com.tarasantoniuk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test to verify double booking prevention
 * <p>
 * This test verifies the bug fix where the system was allowing
 * two bookings for the same unit and dates when the first booking
 * was still in PENDING status.
 */
@DisplayName("Double Booking Prevention Integration Tests")
class DoubleBookingPreventionTest extends AbstractIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Unit testUnit;

    @BeforeEach
    void setUp() {
        // Clean up (delete in correct order due to foreign keys)
        eventRepository.deleteAll();
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        unitRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser = userRepository.save(testUser);

        // Create test unit
        testUnit = new Unit();
        testUnit.setNumberOfRooms(2);
        testUnit.setAccommodationType(AccommodationType.FLAT);
        testUnit.setFloor(3);
        testUnit.setBaseCost(BigDecimal.valueOf(100));
        testUnit.setDescription("Test unit");
        testUnit.setOwner(testUser);
        testUnit = unitRepository.save(testUnit);
    }

    @Test
    @DisplayName("Should prevent double booking when first booking is PENDING")
    void shouldPreventDoubleBookingWhenFirstIsPending() {
        // Given - Same dates for both bookings
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        CreateBookingRequestDto firstRequest = new CreateBookingRequestDto();
        firstRequest.setUnitId(testUnit.getId());
        firstRequest.setUserId(testUser.getId());
        firstRequest.setStartDate(startDate);
        firstRequest.setEndDate(endDate);

        CreateBookingRequestDto secondRequest = new CreateBookingRequestDto();
        secondRequest.setUnitId(testUnit.getId());
        secondRequest.setUserId(testUser.getId());
        secondRequest.setStartDate(startDate);
        secondRequest.setEndDate(endDate);

        // When - Create first booking (PENDING status)
        BookingResponseDto firstBooking = bookingService.createBooking(firstRequest);
        assertThat(firstBooking).isNotNull();
        assertThat(firstBooking.getStatus()).isEqualTo(BookingStatus.PENDING);

        // Then - Attempting to create second booking should fail
        assertThatThrownBy(() -> bookingService.createBooking(secondRequest))
                .isInstanceOf(UnitNotAvailableException.class)
                .hasMessageContaining("Unit is not available");

        // Verify only one booking exists
        var allBookings = bookingRepository.findAll();
        assertThat(allBookings).hasSize(1);
    }

    @Test
    @DisplayName("Should prevent double booking for exact same date (same day check-in and check-out)")
    void shouldPreventDoubleBookingForSameDate() {
        // Given - Bug scenario: same date for start and end
        LocalDate sameDate = LocalDate.of(2026, 1, 22);

        CreateBookingRequestDto firstRequest = new CreateBookingRequestDto();
        firstRequest.setUnitId(testUnit.getId());
        firstRequest.setUserId(testUser.getId());
        firstRequest.setStartDate(sameDate);
        firstRequest.setEndDate(sameDate);

        CreateBookingRequestDto secondRequest = new CreateBookingRequestDto();
        secondRequest.setUnitId(testUnit.getId());
        secondRequest.setUserId(testUser.getId());
        secondRequest.setStartDate(sameDate);
        secondRequest.setEndDate(sameDate);

        // When - Create first booking
        BookingResponseDto firstBooking = bookingService.createBooking(firstRequest);
        assertThat(firstBooking).isNotNull();
        assertThat(firstBooking.getStatus()).isEqualTo(BookingStatus.PENDING);

        // Then - Second booking should fail
        assertThatThrownBy(() -> bookingService.createBooking(secondRequest))
                .isInstanceOf(UnitNotAvailableException.class)
                .hasMessageContaining("Unit is not available");

        // Verify only one booking exists
        var allBookings = bookingRepository.findAll();
        assertThat(allBookings).hasSize(1);
        assertThat(allBookings.get(0).getStartDate()).isEqualTo(sameDate);
        assertThat(allBookings.get(0).getEndDate()).isEqualTo(sameDate);
    }

    @Test
    @DisplayName("Should prevent double booking with overlapping dates")
    void shouldPreventDoubleBookingWithOverlappingDates() {
        // Given
        LocalDate date1Start = LocalDate.now().plusDays(1);
        LocalDate date1End = LocalDate.now().plusDays(5);
        LocalDate date2Start = LocalDate.now().plusDays(3);  // Overlaps with first booking
        LocalDate date2End = LocalDate.now().plusDays(7);

        CreateBookingRequestDto firstRequest = new CreateBookingRequestDto();
        firstRequest.setUnitId(testUnit.getId());
        firstRequest.setUserId(testUser.getId());
        firstRequest.setStartDate(date1Start);
        firstRequest.setEndDate(date1End);

        CreateBookingRequestDto secondRequest = new CreateBookingRequestDto();
        secondRequest.setUnitId(testUnit.getId());
        secondRequest.setUserId(testUser.getId());
        secondRequest.setStartDate(date2Start);
        secondRequest.setEndDate(date2End);

        // When - Create first booking
        BookingResponseDto firstBooking = bookingService.createBooking(firstRequest);
        assertThat(firstBooking).isNotNull();

        // Then - Overlapping booking should fail
        assertThatThrownBy(() -> bookingService.createBooking(secondRequest))
                .isInstanceOf(UnitNotAvailableException.class)
                .hasMessageContaining("Unit is not available");
    }

    @Test
    @DisplayName("Should allow booking after cancelled booking for same dates")
    void shouldAllowBookingAfterCancelledBooking() {
        // Given
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        CreateBookingRequestDto firstRequest = new CreateBookingRequestDto();
        firstRequest.setUnitId(testUnit.getId());
        firstRequest.setUserId(testUser.getId());
        firstRequest.setStartDate(startDate);
        firstRequest.setEndDate(endDate);

        CreateBookingRequestDto secondRequest = new CreateBookingRequestDto();
        secondRequest.setUnitId(testUnit.getId());
        secondRequest.setUserId(testUser.getId());
        secondRequest.setStartDate(startDate);
        secondRequest.setEndDate(endDate);

        // When - Create and cancel first booking
        BookingResponseDto firstBooking = bookingService.createBooking(firstRequest);
        bookingService.cancelBooking(firstBooking.getId(), testUser.getId());

        // Then - Second booking should succeed (first is cancelled)
        BookingResponseDto secondBooking = bookingService.createBooking(secondRequest);
        assertThat(secondBooking).isNotNull();
        assertThat(secondBooking.getId()).isNotEqualTo(firstBooking.getId());

        // Verify two bookings exist (one cancelled, one pending)
        var allBookings = bookingRepository.findAll();
        assertThat(allBookings).hasSize(2);
        assertThat(allBookings).anyMatch(b -> b.getStatus() == BookingStatus.CANCELLED);
        assertThat(allBookings).anyMatch(b -> b.getStatus() == BookingStatus.PENDING);
    }

    @Test
    @DisplayName("Should allow non-overlapping bookings for same unit")
    void shouldAllowNonOverlappingBookings() {
        // Given - Two non-overlapping date ranges
        LocalDate date1Start = LocalDate.now().plusDays(1);
        LocalDate date1End = LocalDate.now().plusDays(3);
        LocalDate date2Start = LocalDate.now().plusDays(5);  // After first booking
        LocalDate date2End = LocalDate.now().plusDays(7);

        CreateBookingRequestDto firstRequest = new CreateBookingRequestDto();
        firstRequest.setUnitId(testUnit.getId());
        firstRequest.setUserId(testUser.getId());
        firstRequest.setStartDate(date1Start);
        firstRequest.setEndDate(date1End);

        CreateBookingRequestDto secondRequest = new CreateBookingRequestDto();
        secondRequest.setUnitId(testUnit.getId());
        secondRequest.setUserId(testUser.getId());
        secondRequest.setStartDate(date2Start);
        secondRequest.setEndDate(date2End);

        // When - Create both bookings
        BookingResponseDto firstBooking = bookingService.createBooking(firstRequest);
        BookingResponseDto secondBooking = bookingService.createBooking(secondRequest);

        // Then - Both should succeed
        assertThat(firstBooking).isNotNull();
        assertThat(secondBooking).isNotNull();
        assertThat(firstBooking.getId()).isNotEqualTo(secondBooking.getId());

        // Verify two bookings exist
        var allBookings = bookingRepository.findAll();
        assertThat(allBookings).hasSize(2);
    }
}
