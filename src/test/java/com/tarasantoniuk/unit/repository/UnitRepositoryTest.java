package com.tarasantoniuk.unit.repository;

import com.tarasantoniuk.booking.entity.Booking;
import com.tarasantoniuk.booking.enums.BookingStatus;
import com.tarasantoniuk.booking.repository.BookingRepository;
import com.tarasantoniuk.common.AbstractIntegrationTest;
import com.tarasantoniuk.unit.entity.Unit;
import com.tarasantoniuk.unit.enums.AccommodationType;
import com.tarasantoniuk.user.entity.User;
import com.tarasantoniuk.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UnitRepository Tests")
class UnitRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private User testOwner;
    private User testUser;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        unitRepository.deleteAll();
        userRepository.deleteAll();

        testOwner = new User();
        testOwner.setUsername("testowner");
        testOwner.setEmail("owner@test.com");
        testOwner = userRepository.save(testOwner);

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("user@test.com");
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should save and find unit by id")
    void shouldSaveAndFindUnitById() {
        // Given
        Unit unit = createUnit(2, AccommodationType.FLAT, 100.0);

        // When
        Unit saved = unitRepository.save(unit);
        Unit found = unitRepository.findById(saved.getId()).orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getNumberOfRooms()).isEqualTo(2);
        assertThat(found.getAccommodationType()).isEqualTo(AccommodationType.FLAT);
    }

    @Test
    @DisplayName("Should find all units with pagination")
    void shouldFindAllUnitsWithPagination() {
        // Given
        unitRepository.save(createUnit(1, AccommodationType.FLAT, 80.0));
        unitRepository.save(createUnit(2, AccommodationType.FLAT, 100.0));
        unitRepository.save(createUnit(3, AccommodationType.APARTMENT, 150.0));

        // When
        Page<Unit> page = unitRepository.findAll(PageRequest.of(0, 2));

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find units by owner id")
    void shouldFindUnitsByOwnerId() {
        // Given
        unitRepository.save(createUnit(2, AccommodationType.FLAT, 100.0));
        unitRepository.save(createUnit(3, AccommodationType.FLAT, 120.0));

        // When
        List<Unit> units = unitRepository.findAll();

        // Then
        assertThat(units).hasSize(2);
        assertThat(units).allMatch(u -> u.getOwner().getId().equals(testOwner.getId()));
    }

    @Test
    @DisplayName("Should calculate total cost correctly")
    void shouldCalculateTotalCostCorrectly() {
        // Given
        Unit unit = createUnit(2, AccommodationType.FLAT, 100.0);

        // When
        Unit saved = unitRepository.save(unit);

        // Then
        BigDecimal expectedTotal = new BigDecimal("115.00"); // 100 * 1.15
        assertThat(saved.getTotalCost()).isEqualByComparingTo(expectedTotal);
    }

    @Test
    @DisplayName("Should persist all unit properties")
    void shouldPersistAllUnitProperties() {
        // Given
        Unit unit = createUnit(3, AccommodationType.APARTMENT, 200.0);
        unit.setFloor(5);
        unit.setDescription("Luxury apartment with great view");

        // When
        Unit saved = unitRepository.save(unit);
        unitRepository.flush();
        Unit found = unitRepository.findById(saved.getId()).orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getNumberOfRooms()).isEqualTo(3);
        assertThat(found.getAccommodationType()).isEqualTo(AccommodationType.APARTMENT);
        assertThat(found.getFloor()).isEqualTo(5);
        assertThat(found.getBaseCost()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(found.getDescription()).isEqualTo("Luxury apartment with great view");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Nested
    @DisplayName("countAvailableUnits")
    class CountAvailableUnitsTests {

        @Test
        @DisplayName("Should count unit with future booking as available")
        void shouldCountUnitWithFutureBookingAsAvailable() {
            // Given
            Unit unit = unitRepository.save(createUnit(2, AccommodationType.FLAT, 100.0));
            createBooking(unit, 7, 3, BookingStatus.CONFIRMED);

            // When
            Long count = unitRepository.countAvailableUnits();

            // Then
            assertThat(count).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should not count unit with active booking as available")
        void shouldNotCountUnitWithActiveBookingAsAvailable() {
            // Given
            Unit unit = unitRepository.save(createUnit(2, AccommodationType.FLAT, 100.0));
            createBooking(unit, -2, 4, BookingStatus.CONFIRMED);

            // When
            Long count = unitRepository.countAvailableUnits();

            // Then
            assertThat(count).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should count unit with past booking as available")
        void shouldCountUnitWithPastBookingAsAvailable() {
            // Given
            Unit unit = unitRepository.save(createUnit(2, AccommodationType.FLAT, 100.0));
            createBooking(unit, -5, 3, BookingStatus.CONFIRMED);

            // When
            Long count = unitRepository.countAvailableUnits();

            // Then
            assertThat(count).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should count unit with no bookings as available")
        void shouldCountUnitWithNoBookingsAsAvailable() {
            // Given
            unitRepository.save(createUnit(2, AccommodationType.FLAT, 100.0));

            // When
            Long count = unitRepository.countAvailableUnits();

            // Then
            assertThat(count).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should not count unit with active PENDING booking as available")
        void shouldNotCountUnitWithActivePendingBookingAsAvailable() {
            // Given
            Unit unit = unitRepository.save(createUnit(2, AccommodationType.FLAT, 100.0));
            createBooking(unit, -1, 3, BookingStatus.PENDING);

            // When
            Long count = unitRepository.countAvailableUnits();

            // Then
            assertThat(count).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should count unit with cancelled active-dates booking as available")
        void shouldCountUnitWithCancelledBookingAsAvailable() {
            // Given
            Unit unit = unitRepository.save(createUnit(2, AccommodationType.FLAT, 100.0));
            createBooking(unit, -1, 3, BookingStatus.CANCELLED);

            // When
            Long count = unitRepository.countAvailableUnits();

            // Then
            assertThat(count).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should correctly count mixed scenario: multiple units with different booking states")
        void shouldCorrectlyCountMixedScenario() {
            // Given
            // Unit 1: no bookings -> available
            unitRepository.save(createUnit(1, AccommodationType.FLAT, 80.0));

            // Unit 2: active booking -> unavailable
            Unit unit2 = unitRepository.save(createUnit(2, AccommodationType.APARTMENT, 120.0));
            createBooking(unit2, -2, 5, BookingStatus.CONFIRMED);

            // Unit 3: future booking only -> available
            Unit unit3 = unitRepository.save(createUnit(3, AccommodationType.HOME, 200.0));
            createBooking(unit3, 7, 4, BookingStatus.CONFIRMED);

            // When
            Long count = unitRepository.countAvailableUnits();

            // Then
            assertThat(count).isEqualTo(2L);
        }
    }

    private Unit createUnit(int rooms, AccommodationType type, double baseCost) {
        Unit unit = new Unit();
        unit.setOwner(testOwner);
        unit.setNumberOfRooms(rooms);
        unit.setAccommodationType(type);
        unit.setFloor(1);
        unit.setBaseCost(new BigDecimal(baseCost));
        unit.setDescription("Test unit");
        return unit;
    }

    private Booking createBooking(Unit unit, int daysFromNow, int duration, BookingStatus status) {
        Booking booking = new Booking();
        booking.setUnit(unit);
        booking.setUser(testUser);
        booking.setStartDate(LocalDate.now().plusDays(daysFromNow));
        booking.setEndDate(LocalDate.now().plusDays(daysFromNow + duration));
        booking.setStatus(status);
        if (status == BookingStatus.PENDING) {
            booking.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        }
        return bookingRepository.save(booking);
    }
}
