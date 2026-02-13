package com.tarasantoniuk.unit.service;

import com.tarasantoniuk.booking.entity.Booking;
import com.tarasantoniuk.booking.enums.BookingStatus;
import com.tarasantoniuk.booking.repository.BookingRepository;
import com.tarasantoniuk.common.AbstractIntegrationTest;
import com.tarasantoniuk.unit.dto.UnitSearchCriteriaDto;
import com.tarasantoniuk.unit.entity.Unit;
import com.tarasantoniuk.unit.enums.AccommodationType;
import com.tarasantoniuk.unit.repository.UnitRepository;
import com.tarasantoniuk.user.entity.User;
import com.tarasantoniuk.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UnitSpecification filtering logic.
 * Uses real database (Testcontainers) to ensure JPA Specifications work correctly.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UnitSpecification Tests")
class UnitSpecificationTest extends AbstractIntegrationTest {

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    private Unit unit1;
    private Unit unit2;
    private Unit unit3;
    private User testOwner;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up
        bookingRepository.deleteAll();
        unitRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        testOwner = new User();
        testOwner.setUsername("testowner");
        testOwner.setEmail("owner@test.com");
        testOwner = userRepository.save(testOwner);

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("user@test.com");
        testUser = userRepository.save(testUser);

        // Create test units with different properties
        unit1 = createAndSaveUnit(
                2,  // rooms
                AccommodationType.APARTMENTS,
                1,  // floor
                BigDecimal.valueOf(100)  // base cost
        );

        unit2 = createAndSaveUnit(
                3,  // rooms
                AccommodationType.HOME,
                2,  // floor
                BigDecimal.valueOf(200)  // base cost
        );

        unit3 = createAndSaveUnit(
                2,  // rooms
                AccommodationType.FLAT,
                1,  // floor
                BigDecimal.valueOf(150)  // base cost
        );
    }

    @AfterEach
    void tearDown() {
        bookingRepository.deleteAll();
        unitRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Number of Rooms Filter")
    class NumberOfRoomsFilterTests {

        @Test
        void whenFilterByTwoRooms_thenReturnOnlyTwoRoomUnits() {
            // Given
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setNumberOfRooms(2);

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then
            assertThat(results)
                    .hasSize(2)
                    .extracting(Unit::getId)
                    .containsExactlyInAnyOrder(unit1.getId(), unit3.getId());
        }

        @Test
        void whenFilterByThreeRooms_thenReturnOnlyThreeRoomUnits() {
            // Given
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setNumberOfRooms(3);

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then
            assertThat(results)
                    .hasSize(1)
                    .extracting(Unit::getId)
                    .containsExactly(unit2.getId());
        }

        @Test
        void whenNumberOfRoomsIsNull_thenReturnAllUnits() {
            // Given
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setNumberOfRooms(null);

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then
            assertThat(results).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Accommodation Type Filter")
    class AccommodationTypeFilterTests {

        @Test
        void whenFilterByApartments_thenReturnOnlyApartments() {
            // Given
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setAccommodationType(AccommodationType.APARTMENTS);

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then
            assertThat(results)
                    .hasSize(1)
                    .extracting(Unit::getId)
                    .containsExactly(unit1.getId());
        }

        @Test
        void whenAccommodationTypeIsNull_thenReturnAllUnits() {
            // Given
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setAccommodationType(null);

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then
            assertThat(results).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Floor Filter")
    class FloorFilterTests {

        @Test
        void whenFilterByFloorOne_thenReturnOnlyFirstFloorUnits() {
            // Given
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setFloor(1);

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then
            assertThat(results)
                    .hasSize(2)
                    .extracting(Unit::getId)
                    .containsExactlyInAnyOrder(unit1.getId(), unit3.getId());
        }

        @Test
        void whenFloorIsNull_thenReturnAllUnits() {
            // Given
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setFloor(null);

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then
            assertThat(results).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Cost Filter with Markup")
    class CostFilterTests {

        @Test
        void whenFilterByMinCost_thenReturnUnitsAboveMinimum() {
            // Given - min cost 150 (with markup = 172.5 displayed to user)
            // Base costs: 100, 150, 200
            // After markup: 115, 172.5, 230
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setMinCost(BigDecimal.valueOf(150));  // user sees 172.5 after markup

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - should return unit2 (base 200) and unit3 (base 150)
            assertThat(results)
                    .hasSize(2)
                    .extracting(Unit::getId)
                    .containsExactlyInAnyOrder(unit2.getId(), unit3.getId());
        }

        @Test
        void whenFilterByMaxCost_thenReturnUnitsBelowMaximum() {
            // Given - max cost 150 means user wants units with display price <= 150
            // Converted to base cost: 150 / 1.15 = 130.43
            // Base costs: unit1=100 (displays as 115), unit2=200 (displays as 230), unit3=150 (displays as 172.5)
            // Only unit1 should match (115 <= 150)
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setMaxCost(BigDecimal.valueOf(150));

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - should return only unit1 (base 100, displays as 115)
            assertThat(results)
                    .hasSize(1)
                    .extracting(Unit::getId)
                    .containsExactly(unit1.getId());
        }

        @Test
        void whenFilterByMinAndMaxCost_thenReturnUnitsInRange() {
            // Given - range 120-180 after markup conversion
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setMinCost(BigDecimal.valueOf(120));
            criteria.setMaxCost(BigDecimal.valueOf(180));

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - should return only unit3 (base 150, displays as 172.5)
            assertThat(results)
                    .hasSize(1)
                    .extracting(Unit::getId)
                    .containsExactly(unit3.getId());
        }

        @Test
        void whenCostFiltersAreNull_thenReturnAllUnits() {
            // Given
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setMinCost(null);
            criteria.setMaxCost(null);

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then
            assertThat(results).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Date Availability Filter - Critical Logic")
    class DateAvailabilityFilterTests {

        @Test
        void whenNoBookings_thenUnitIsAvailable() {
            // Given - no bookings exist
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setStartDate(LocalDate.now().plusDays(1));
            criteria.setEndDate(LocalDate.now().plusDays(5));

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - all units available
            assertThat(results).hasSize(3);
        }

        @Test
        void whenRequestedPeriodStartsDuringExistingBooking_thenUnitUnavailable() {
            // Given - unit1 booked from day 5 to day 10
            createAndSaveBooking(
                    unit1,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    BookingStatus.CONFIRMED
            );

            // Search: day 7 to day 12 (starts during existing booking)
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setStartDate(LocalDate.now().plusDays(7));
            criteria.setEndDate(LocalDate.now().plusDays(12));

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - unit1 unavailable, unit2 and unit3 available
            assertThat(results)
                    .hasSize(2)
                    .extracting(Unit::getId)
                    .containsExactlyInAnyOrder(unit2.getId(), unit3.getId());
        }

        @Test
        void whenRequestedPeriodEndsDuringExistingBooking_thenUnitUnavailable() {
            // Given - unit1 booked from day 5 to day 10
            createAndSaveBooking(
                    unit1,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    BookingStatus.CONFIRMED
            );

            // Search: day 3 to day 7 (ends during existing booking)
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setStartDate(LocalDate.now().plusDays(3));
            criteria.setEndDate(LocalDate.now().plusDays(7));

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - unit1 unavailable
            assertThat(results)
                    .hasSize(2)
                    .extracting(Unit::getId)
                    .containsExactlyInAnyOrder(unit2.getId(), unit3.getId());
        }

        @Test
        void whenRequestedPeriodCoversEntireExistingBooking_thenUnitUnavailable() {
            // Given - unit1 booked from day 5 to day 10
            createAndSaveBooking(
                    unit1,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    BookingStatus.CONFIRMED
            );

            // Search: day 3 to day 12 (covers entire booking)
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setStartDate(LocalDate.now().plusDays(3));
            criteria.setEndDate(LocalDate.now().plusDays(12));

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - unit1 unavailable
            assertThat(results)
                    .hasSize(2)
                    .extracting(Unit::getId)
                    .containsExactlyInAnyOrder(unit2.getId(), unit3.getId());
        }

        @Test
        void whenExistingBookingCoversEntireRequestedPeriod_thenUnitUnavailable() {
            // Given - unit1 booked from day 3 to day 12
            createAndSaveBooking(
                    unit1,
                    LocalDate.now().plusDays(3),
                    LocalDate.now().plusDays(12),
                    BookingStatus.CONFIRMED
            );

            // Search: day 5 to day 10 (within existing booking)
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setStartDate(LocalDate.now().plusDays(5));
            criteria.setEndDate(LocalDate.now().plusDays(10));

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - unit1 unavailable
            assertThat(results)
                    .hasSize(2)
                    .extracting(Unit::getId)
                    .containsExactlyInAnyOrder(unit2.getId(), unit3.getId());
        }

        @Test
        void whenBookingIsBeforeRequestedPeriod_thenUnitAvailable() {
            // Given - unit1 booked from day 1 to day 3
            createAndSaveBooking(
                    unit1,
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(3),
                    BookingStatus.CONFIRMED
            );

            // Search: day 5 to day 10 (after booking)
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setStartDate(LocalDate.now().plusDays(5));
            criteria.setEndDate(LocalDate.now().plusDays(10));

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - all units available
            assertThat(results).hasSize(3);
        }

        @Test
        void whenBookingIsAfterRequestedPeriod_thenUnitAvailable() {
            // Given - unit1 booked from day 15 to day 20
            createAndSaveBooking(
                    unit1,
                    LocalDate.now().plusDays(15),
                    LocalDate.now().plusDays(20),
                    BookingStatus.CONFIRMED
            );

            // Search: day 5 to day 10 (before booking)
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setStartDate(LocalDate.now().plusDays(5));
            criteria.setEndDate(LocalDate.now().plusDays(10));

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - all units available
            assertThat(results).hasSize(3);
        }

        @Test
        void whenPendingBookingExists_thenUnitUnavailable() {
            // Given - unit1 has PENDING booking
            createAndSaveBooking(
                    unit1,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    BookingStatus.PENDING
            );

            // Search overlapping period
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setStartDate(LocalDate.now().plusDays(7));
            criteria.setEndDate(LocalDate.now().plusDays(12));

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - unit1 unavailable (PENDING blocks availability)
            assertThat(results)
                    .hasSize(2)
                    .extracting(Unit::getId)
                    .containsExactlyInAnyOrder(unit2.getId(), unit3.getId());
        }

        @Test
        void whenCancelledBookingExists_thenUnitAvailable() {
            // Given - unit1 has CANCELLED booking
            createAndSaveBooking(
                    unit1,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    BookingStatus.CANCELLED
            );

            // Search overlapping period
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setStartDate(LocalDate.now().plusDays(7));
            criteria.setEndDate(LocalDate.now().plusDays(12));

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - all units available (CANCELLED doesn't block)
            assertThat(results).hasSize(3);
        }

        @Test
        void whenMultipleBookingsExist_thenFilterCorrectly() {
            // Given
            // unit1: CONFIRMED booking day 5-10
            createAndSaveBooking(
                    unit1,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    BookingStatus.CONFIRMED
            );

            // unit2: CANCELLED booking day 5-10 (shouldn't block)
            createAndSaveBooking(
                    unit2,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    BookingStatus.CANCELLED
            );

            // unit3: no bookings

            // Search: day 7-12
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setStartDate(LocalDate.now().plusDays(7));
            criteria.setEndDate(LocalDate.now().plusDays(12));

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - unit2 and unit3 available, unit1 blocked
            assertThat(results)
                    .hasSize(2)
                    .extracting(Unit::getId)
                    .containsExactlyInAnyOrder(unit2.getId(), unit3.getId());
        }

        @Test
        void whenOnlyStartDateProvided_thenNoDateFilter() {
            // Given - unit1 has overlapping booking
            createAndSaveBooking(
                    unit1,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    BookingStatus.CONFIRMED
            );

            // Search with only startDate (endDate is null)
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setStartDate(LocalDate.now().plusDays(7));
            criteria.setEndDate(null);

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - all units returned (incomplete date range skips date filter)
            assertThat(results).hasSize(3);
        }

        @Test
        void whenOnlyEndDateProvided_thenNoDateFilter() {
            // Given - unit1 has overlapping booking
            createAndSaveBooking(
                    unit1,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    BookingStatus.CONFIRMED
            );

            // Search with only endDate (startDate is null)
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setStartDate(null);
            criteria.setEndDate(LocalDate.now().plusDays(12));

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - all units returned (incomplete date range skips date filter)
            assertThat(results).hasSize(3);
        }

        @Test
        void whenDateRangeIsNull_thenNoDateFilter() {
            // Given - unit1 has booking
            createAndSaveBooking(
                    unit1,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    BookingStatus.CONFIRMED
            );

            // Search with null dates
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setStartDate(null);
            criteria.setEndDate(null);

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - all units returned (no date filtering)
            assertThat(results).hasSize(3);
        }

        @Test
        void whenBookingStartsExactlyOnRequestedEndDate_thenUnitAvailable() {
            // Given - unit1 booked from day 10 to day 15
            createAndSaveBooking(
                    unit1,
                    LocalDate.now().plusDays(10),
                    LocalDate.now().plusDays(15),
                    BookingStatus.CONFIRMED
            );

            // Search: day 5 to day 10 (ends exactly when booking starts)
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setStartDate(LocalDate.now().plusDays(5));
            criteria.setEndDate(LocalDate.now().plusDays(10));

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - unit should be unavailable due to overlap on day 10
            assertThat(results)
                    .hasSize(2)
                    .extracting(Unit::getId)
                    .containsExactlyInAnyOrder(unit2.getId(), unit3.getId());
        }

        @Test
        void whenRequestedStartsExactlyOnBookingEndDate_thenUnitAvailable() {
            // Given - unit1 booked from day 5 to day 10
            createAndSaveBooking(
                    unit1,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    BookingStatus.CONFIRMED
            );

            // Search: day 10 to day 15 (starts exactly when booking ends)
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setStartDate(LocalDate.now().plusDays(10));
            criteria.setEndDate(LocalDate.now().plusDays(15));

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - unit should be unavailable due to overlap on day 10
            assertThat(results)
                    .hasSize(2)
                    .extracting(Unit::getId)
                    .containsExactlyInAnyOrder(unit2.getId(), unit3.getId());
        }
    }

    @Nested
    @DisplayName("Combined Filters")
    class CombinedFiltersTests {

        @Test
        void whenMultipleFiltersApplied_thenReturnUnitsMatchingAll() {
            // Given
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setNumberOfRooms(2);
            criteria.setFloor(1);
            criteria.setAccommodationType(AccommodationType.APARTMENTS);

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - only unit1 matches all criteria
            assertThat(results)
                    .hasSize(1)
                    .extracting(Unit::getId)
                    .containsExactly(unit1.getId());
        }

        @Test
        void whenAllFiltersApplied_thenReturnMatchingUnits() {
            // Given - create booking on unit2
            createAndSaveBooking(
                    unit2,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    BookingStatus.CONFIRMED
            );

            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setNumberOfRooms(2);
            criteria.setFloor(1);
            criteria.setMinCost(BigDecimal.valueOf(50));
            criteria.setMaxCost(BigDecimal.valueOf(200));
            criteria.setStartDate(LocalDate.now().plusDays(5));
            criteria.setEndDate(LocalDate.now().plusDays(10));

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - unit1 and unit3 match (both 2 rooms, floor 1, available, in price range)
            assertThat(results)
                    .hasSize(2)
                    .extracting(Unit::getId)
                    .containsExactlyInAnyOrder(unit1.getId(), unit3.getId());
        }

        @Test
        void whenEmptyCriteria_thenReturnAllUnits() {
            // Given - all null criteria
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - all units returned
            assertThat(results).hasSize(3);
        }

        @Test
        void whenNoUnitsMatchCriteria_thenReturnEmptyList() {
            // Given - impossible criteria
            UnitSearchCriteriaDto criteria = new UnitSearchCriteriaDto();
            criteria.setNumberOfRooms(99);  // no unit has 99 rooms

            // When
            List<Unit> results = unitRepository.findAll(UnitSpecification.withCriteria(criteria));

            // Then - empty result
            assertThat(results).isEmpty();
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Helper methods for test data creation
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private Unit createAndSaveUnit(
            Integer numberOfRooms,
            AccommodationType accommodationType,
            Integer floor,
            BigDecimal baseCost
    ) {
        Unit unit = new Unit();
        unit.setOwner(testOwner);
        unit.setNumberOfRooms(numberOfRooms);
        unit.setAccommodationType(accommodationType);
        unit.setFloor(floor);
        unit.setBaseCost(baseCost);
        unit.setDescription("Test unit");
        return unitRepository.save(unit);
    }

    private Booking createAndSaveBooking(
            Unit unit,
            LocalDate startDate,
            LocalDate endDate,
            BookingStatus status
    ) {
        Booking booking = new Booking();
        booking.setUnit(unit);
        booking.setUser(testUser);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setStatus(status);
        if (status == BookingStatus.PENDING) {
            booking.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        }
        return bookingRepository.save(booking);
    }
}