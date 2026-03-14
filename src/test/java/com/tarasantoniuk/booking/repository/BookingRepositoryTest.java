package com.tarasantoniuk.booking.repository;

import com.tarasantoniuk.booking.entity.Booking;
import com.tarasantoniuk.booking.enums.BookingStatus;
import com.tarasantoniuk.common.AbstractIntegrationTest;
import com.tarasantoniuk.unit.entity.Unit;
import com.tarasantoniuk.unit.enums.AccommodationType;
import com.tarasantoniuk.unit.repository.UnitRepository;
import com.tarasantoniuk.user.entity.User;
import com.tarasantoniuk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository layer tests for BookingRepository using Testcontainers PostgreSQL.
 * Tests custom queries and JPA methods critical for booking logic.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("BookingRepository Tests")
class BookingRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Unit testUnit;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("user@test.com");
        testUser = userRepository.save(testUser);

        // Create test unit owner
        User owner = new User();
        owner.setUsername("owner");
        owner.setEmail("owner@test.com");
        owner = userRepository.save(owner);

        // Create test unit
        testUnit = new Unit();
        testUnit.setOwner(owner);
        testUnit.setNumberOfRooms(2);
        testUnit.setAccommodationType(AccommodationType.FLAT);
        testUnit.setFloor(1);
        testUnit.setBaseCost(new BigDecimal("100.00"));
        testUnit.setDescription("Test unit");
        testUnit = unitRepository.save(testUnit);
    }

    @Test
    @DisplayName("Should find bookings by status")
    void shouldFindBookingsByStatus() {
        // Given
        Booking pending1 = createBooking(BookingStatus.PENDING, LocalDate.now(), LocalDate.now().plusDays(2));
        Booking pending2 = createBooking(BookingStatus.PENDING, LocalDate.now().plusDays(5), LocalDate.now().plusDays(7));
        Booking confirmed = createBooking(BookingStatus.CONFIRMED, LocalDate.now().plusDays(10), LocalDate.now().plusDays(12));

        bookingRepository.save(pending1);
        bookingRepository.save(pending2);
        bookingRepository.save(confirmed);

        // When
        List<Booking> pendingBookings = bookingRepository.findByStatus(BookingStatus.PENDING);

        // Then
        assertThat(pendingBookings).hasSize(2);
        assertThat(pendingBookings).allMatch(b -> b.getStatus() == BookingStatus.PENDING);
    }

    @Test
    @DisplayName("Should find bookings by user id")
    void shouldFindBookingsByUserId() {
        // Given
        User anotherUser = new User();
        anotherUser.setUsername("another");
        anotherUser.setEmail("another@test.com");
        anotherUser = userRepository.save(anotherUser);

        Booking booking1 = createBooking(BookingStatus.PENDING, LocalDate.now(), LocalDate.now().plusDays(2));
        booking1.setUser(testUser);
        bookingRepository.save(booking1);

        Booking booking2 = createBooking(BookingStatus.CONFIRMED, LocalDate.now().plusDays(5), LocalDate.now().plusDays(7));
        booking2.setUser(testUser);
        bookingRepository.save(booking2);

        Booking otherBooking = createBooking(BookingStatus.PENDING, LocalDate.now(), LocalDate.now().plusDays(2));
        otherBooking.setUser(anotherUser);
        bookingRepository.save(otherBooking);

        // When
        List<Booking> userBookings = bookingRepository.findByUserId(testUser.getId());

        // Then
        assertThat(userBookings).hasSize(2);
        assertThat(userBookings).allMatch(b -> b.getUser().getId().equals(testUser.getId()));
    }

    @Test
    @DisplayName("Should find expired bookings")
    void shouldFindExpiredBookings() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        Booking expired1 = createBooking(BookingStatus.PENDING, LocalDate.now(), LocalDate.now().plusDays(2));
        expired1.setExpiresAt(now.minusMinutes(10)); // Expired 10 minutes ago
        bookingRepository.save(expired1);

        Booking expired2 = createBooking(BookingStatus.PENDING, LocalDate.now().plusDays(5), LocalDate.now().plusDays(7));
        expired2.setExpiresAt(now.minusMinutes(5)); // Expired 5 minutes ago
        bookingRepository.save(expired2);

        Booking notExpired = createBooking(BookingStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(12));
        notExpired.setExpiresAt(now.plusMinutes(10)); // Expires in 10 minutes
        bookingRepository.save(notExpired);

        // When
        List<Booking> expiredBookings = bookingRepository.findByStatusAndExpiresAtBefore(
                BookingStatus.PENDING,
                now
        );

        // Then
        assertThat(expiredBookings).hasSize(2);
        assertThat(expiredBookings).allMatch(b -> b.getExpiresAt().isBefore(now));
    }

    @Test
    @DisplayName("Should find conflicting bookings - exact overlap")
    void shouldFindConflictingBookingsExactOverlap() {
        // Given
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        Booking existing = createBooking(BookingStatus.PENDING, startDate, endDate);
        bookingRepository.save(existing);

        // When - try to book same dates
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                testUnit.getId(),
                startDate,
                endDate
        );

        // Then
        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).getId()).isEqualTo(existing.getId());
    }

    @Test
    @DisplayName("Should find conflicting bookings - partial overlap start")
    void shouldFindConflictingBookingsPartialOverlapStart() {
        // Given
        Booking existing = createBooking(
                BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(10)
        );
        bookingRepository.save(existing);

        // When - new booking overlaps at the start
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                testUnit.getId(),
                LocalDate.now().plusDays(3),  // Starts before existing
                LocalDate.now().plusDays(7)   // Ends during existing
        );

        // Then
        assertThat(conflicts).hasSize(1);
    }

    @Test
    @DisplayName("Should find conflicting bookings - partial overlap end")
    void shouldFindConflictingBookingsPartialOverlapEnd() {
        // Given
        Booking existing = createBooking(
                BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(10)
        );
        bookingRepository.save(existing);

        // When - new booking overlaps at the end
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                testUnit.getId(),
                LocalDate.now().plusDays(8),  // Starts during existing
                LocalDate.now().plusDays(12)  // Ends after existing
        );

        // Then
        assertThat(conflicts).hasSize(1);
    }

    @Test
    @DisplayName("Should find conflicting bookings - complete enclosure")
    void shouldFindConflictingBookingsCompleteEnclosure() {
        // Given
        Booking existing = createBooking(
                BookingStatus.PENDING,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(10)
        );
        bookingRepository.save(existing);

        // When - new booking completely contains existing
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                testUnit.getId(),
                LocalDate.now().plusDays(3),
                LocalDate.now().plusDays(12)
        );

        // Then
        assertThat(conflicts).hasSize(1);
    }

    @Test
    @DisplayName("Should NOT find conflicts for different units")
    void shouldNotFindConflictsForDifferentUnits() {
        // Given
        Booking existing = createBooking(
                BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(10)
        );
        bookingRepository.save(existing);

        // Create another unit
        Unit anotherUnit = new Unit();
        anotherUnit.setOwner(testUnit.getOwner());
        anotherUnit.setNumberOfRooms(3);
        anotherUnit.setAccommodationType(AccommodationType.APARTMENT);
        anotherUnit.setFloor(2);
        anotherUnit.setBaseCost(new BigDecimal("150.00"));
        anotherUnit.setDescription("Another unit");
        anotherUnit = unitRepository.save(anotherUnit);

        // When - check conflicts for different unit
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                anotherUnit.getId(),
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(10)
        );

        // Then
        assertThat(conflicts).isEmpty();
    }

    @Test
    @DisplayName("Should NOT find conflicts for CANCELLED bookings")
    void shouldNotFindConflictsForCancelledBookings() {
        // Given
        Booking cancelled = createBooking(
                BookingStatus.CANCELLED,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(10)
        );
        bookingRepository.save(cancelled);

        // When - check conflicts for same dates
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                testUnit.getId(),
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(10)
        );

        // Then
        assertThat(conflicts).isEmpty();
    }

    @Test
    @DisplayName("Should find conflicts for both PENDING and CONFIRMED")
    void shouldFindConflictsForBothPendingAndConfirmed() {
        // Given
        Booking pending = createBooking(
                BookingStatus.PENDING,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3)
        );
        bookingRepository.save(pending);

        Booking confirmed = createBooking(
                BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7)
        );
        bookingRepository.save(confirmed);

        // When - check conflicts overlapping with pending
        List<Booking> pendingConflicts = bookingRepository.findConflictingBookings(
                testUnit.getId(),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3)
        );

        // And - check conflicts overlapping with confirmed
        List<Booking> confirmedConflicts = bookingRepository.findConflictingBookings(
                testUnit.getId(),
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7)
        );

        // Then
        assertThat(pendingConflicts).hasSize(1);
        assertThat(confirmedConflicts).hasSize(1);
    }

    private Booking createBooking(BookingStatus status, LocalDate startDate, LocalDate endDate) {
        Booking booking = new Booking();
        booking.setUser(testUser);
        booking.setUnit(testUnit);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setStatus(status);
        if (status == BookingStatus.PENDING) {
            booking.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        }
        return booking;
    }
}
