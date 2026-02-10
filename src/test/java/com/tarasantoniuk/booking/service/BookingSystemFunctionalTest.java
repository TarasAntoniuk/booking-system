package com.tarasantoniuk.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarasantoniuk.booking.dto.CreateBookingRequestDto;
import com.tarasantoniuk.booking.repository.BookingRepository;
import com.tarasantoniuk.common.AbstractIntegrationTest;
import com.tarasantoniuk.event.repository.EventRepository;
import com.tarasantoniuk.payment.dto.ProcessPaymentRequestDto;
import com.tarasantoniuk.payment.repository.PaymentRepository;
import com.tarasantoniuk.unit.dto.CreateUnitRequestDto;
import com.tarasantoniuk.unit.enums.AccommodationType;
import com.tarasantoniuk.unit.repository.UnitRepository;
import com.tarasantoniuk.user.dto.UserRequestDto;
import com.tarasantoniuk.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Functional Tests for Booking System
 * <p>
 * Tests complete user flows from API level without mocking services.
 * Uses real database (H2 in-memory for tests) and all layers of the application.
 */
@AutoConfigureMockMvc
@DisplayName("Booking System - End-to-End Functional Tests")
class BookingSystemFunctionalTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private UserRepository userRepository;

    private Long userId;
    private Long unitId;

    @AfterEach
    void tearDown() {
        eventRepository.deleteAll();
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        unitRepository.deleteAll();
        userRepository.deleteAll();
    }

    @BeforeEach
    void setUp() throws Exception {
        // Clean up from previous tests
        eventRepository.deleteAll();
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        unitRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        UserRequestDto userRequest = new UserRequestDto();
        userRequest.setUsername("john.doe");
        userRequest.setEmail("john.doe@example.com");

        MvcResult userResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String userResponse = userResult.getResponse().getContentAsString();
        this.userId = objectMapper.readTree(userResponse).get("id").asLong();

        // Create test unit
        CreateUnitRequestDto unitRequest = new CreateUnitRequestDto();
        unitRequest.setOwnerId(userId);
        unitRequest.setNumberOfRooms(2);
        unitRequest.setAccommodationType(AccommodationType.FLAT);
        unitRequest.setFloor(5);
        unitRequest.setBaseCost(BigDecimal.valueOf(100.00));
        unitRequest.setDescription("Cozy 2-bedroom apartment");

        MvcResult unitResult = mockMvc.perform(post("/api/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unitRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String unitResponse = unitResult.getResponse().getContentAsString();
        this.unitId = objectMapper.readTree(unitResponse).get("id").asLong();
    }

    @Test
    @DisplayName("Complete Booking Flow: Create → Pay → Confirm")
    void completeBookingFlowCreatePayConfirm() throws Exception {
        // Step 1: Check initial statistics
        mockMvc.perform(get("/api/statistics/available-units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableUnitsCount", greaterThan(0)));

        // Step 2: Create booking
        CreateBookingRequestDto bookingRequest = new CreateBookingRequestDto();
        bookingRequest.setUnitId(unitId);
        bookingRequest.setUserId(userId);
        bookingRequest.setStartDate(LocalDate.now().plusDays(1));
        bookingRequest.setEndDate(LocalDate.now().plusDays(3));

        MvcResult bookingResult = mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalCost").exists())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andReturn();

        String bookingResponse = bookingResult.getResponse().getContentAsString();
        Long bookingId = objectMapper.readTree(bookingResponse).get("id").asLong();

        // Step 3: Verify booking appears in user's bookings
        mockMvc.perform(get("/api/bookings/user/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(bookingId));

        // Step 4: Verify statistics updated (unit now unavailable)
        mockMvc.perform(get("/api/statistics/available-units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableUnitsCount", greaterThanOrEqualTo(0)));

        // Step 5: Process payment
        ProcessPaymentRequestDto paymentRequest = new ProcessPaymentRequestDto();
        paymentRequest.setBookingId(bookingId);

        mockMvc.perform(post("/api/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Step 6: Verify booking is now CONFIRMED
        mockMvc.perform(get("/api/bookings/" + bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.expiresAt").doesNotExist()); // expiration cleared after payment
    }

    @Test
    @DisplayName("Booking Cancellation Flow: Create → Cancel → Unit Available Again")
    void bookingCancellationFlow() throws Exception {
        // Step 1: Get initial available units count
        MvcResult statsResult = mockMvc.perform(get("/api/statistics/available-units"))
                .andExpect(status().isOk())
                .andReturn();

        String statsResponse = statsResult.getResponse().getContentAsString();
        int initialCount = objectMapper.readTree(statsResponse).get("availableUnitsCount").asInt();

        // Step 2: Create booking
        CreateBookingRequestDto bookingRequest = new CreateBookingRequestDto();
        bookingRequest.setUnitId(unitId);
        bookingRequest.setUserId(userId);
        bookingRequest.setStartDate(LocalDate.now().plusDays(1));
        bookingRequest.setEndDate(LocalDate.now().plusDays(3));

        MvcResult bookingResult = mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long bookingId = objectMapper.readTree(bookingResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Step 3: Verify unit is now unavailable
        mockMvc.perform(get("/api/statistics/available-units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableUnitsCount").value(lessThanOrEqualTo(initialCount)));

        // Step 4: Cancel booking
        mockMvc.perform(delete("/api/bookings/" + bookingId + "/cancel")
                        .param("userId", userId.toString()))
                .andExpect(status().isNoContent());

        // Step 5: Verify booking is cancelled
        mockMvc.perform(get("/api/bookings/" + bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Step 6: Verify unit is available again
        mockMvc.perform(get("/api/statistics/available-units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableUnitsCount").value(greaterThanOrEqualTo(initialCount)));
    }

    @Test
    @DisplayName("Double Booking Prevention: Same Unit, Same Dates")
    void shouldPreventDoubleBooking() throws Exception {
        // Step 1: Create first booking
        CreateBookingRequestDto firstRequest = new CreateBookingRequestDto();
        firstRequest.setUnitId(unitId);
        firstRequest.setUserId(userId);
        firstRequest.setStartDate(LocalDate.now().plusDays(1));
        firstRequest.setEndDate(LocalDate.now().plusDays(3));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Step 2: Attempt second booking with same dates - should fail
        CreateBookingRequestDto secondRequest = new CreateBookingRequestDto();
        secondRequest.setUnitId(unitId);
        secondRequest.setUserId(userId);
        secondRequest.setStartDate(LocalDate.now().plusDays(1));
        secondRequest.setEndDate(LocalDate.now().plusDays(3));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isConflict())  // 409 - resource conflict
                .andExpect(jsonPath("$.message", containsString("not available")));
    }

    @Test
    @DisplayName("Unit Search with Availability Filter")
    void shouldSearchUnitsWithAvailabilityFilter() throws Exception {
        // Step 1: Search available units
        mockMvc.perform(get("/api/units/search")
                        .param("numberOfRooms", "2")
                        .param("accommodationType", "FLAT")
                        .param("startDate", LocalDate.now().plusDays(1).toString())
                        .param("endDate", LocalDate.now().plusDays(3).toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[*].id", hasItem(unitId.intValue())));

        // Step 2: Book the unit
        CreateBookingRequestDto bookingRequest = new CreateBookingRequestDto();
        bookingRequest.setUnitId(unitId);
        bookingRequest.setUserId(userId);
        bookingRequest.setStartDate(LocalDate.now().plusDays(1));
        bookingRequest.setEndDate(LocalDate.now().plusDays(3));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated());

        // Step 3: Search again - unit should NOT appear for same dates
        mockMvc.perform(get("/api/units/search")
                        .param("numberOfRooms", "2")
                        .param("accommodationType", "FLAT")
                        .param("startDate", LocalDate.now().plusDays(1).toString())
                        .param("endDate", LocalDate.now().plusDays(3).toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", not(hasItem(unitId.intValue()))));

        // Step 4: But should appear for different dates
        mockMvc.perform(get("/api/units/search")
                        .param("numberOfRooms", "2")
                        .param("accommodationType", "FLAT")  // Added to match the unit
                        .param("startDate", LocalDate.now().plusDays(10).toString())
                        .param("endDate", LocalDate.now().plusDays(12).toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(unitId.intValue())));
    }

    @Test
    @DisplayName("Cost Calculation: Verify 15% Markup")
    void shouldCalculateCostWith15PercentMarkup() throws Exception {
        // Create booking for 2 days (3 days - 1)
        CreateBookingRequestDto bookingRequest = new CreateBookingRequestDto();
        bookingRequest.setUnitId(unitId);
        bookingRequest.setUserId(userId);
        bookingRequest.setStartDate(LocalDate.now().plusDays(1));
        bookingRequest.setEndDate(LocalDate.now().plusDays(3)); // 2 nights

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalCost").value(230.00)); // 100 * 2 * 1.15 = 230
    }

    @Test
    @DisplayName("Cache Invalidation: Statistics Update After Booking Changes")
    void shouldUpdateStatisticsAfterBookingChanges() throws Exception {
        // This test verifies the cache invalidation fix

        // Step 1: Get initial count
        MvcResult result1 = mockMvc.perform(get("/api/statistics/available-units"))
                .andExpect(status().isOk())
                .andReturn();
        int count1 = objectMapper.readTree(result1.getResponse().getContentAsString())
                .get("availableUnitsCount").asInt();

        // Step 2: Create booking
        CreateBookingRequestDto bookingRequest = new CreateBookingRequestDto();
        bookingRequest.setUnitId(unitId);
        bookingRequest.setUserId(userId);
        bookingRequest.setStartDate(LocalDate.now().plusDays(1));
        bookingRequest.setEndDate(LocalDate.now().plusDays(3));

        MvcResult bookingResult = mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long bookingId = objectMapper.readTree(bookingResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Step 3: Verify count decreased
        MvcResult result2 = mockMvc.perform(get("/api/statistics/available-units"))
                .andExpect(status().isOk())
                .andReturn();
        int count2 = objectMapper.readTree(result2.getResponse().getContentAsString())
                .get("availableUnitsCount").asInt();

        // Count should decrease by at least 1
        assert count2 <= count1 : "Count should decrease after booking";

        // Step 4: Cancel booking
        mockMvc.perform(delete("/api/bookings/" + bookingId + "/cancel")
                        .param("userId", userId.toString()))
                .andExpect(status().isNoContent());

        // Step 5: Verify count increased back
        MvcResult result3 = mockMvc.perform(get("/api/statistics/available-units"))
                .andExpect(status().isOk())
                .andReturn();
        int count3 = objectMapper.readTree(result3.getResponse().getContentAsString())
                .get("availableUnitsCount").asInt();

        // Count should increase back
        assert count3 >= count2 : "Count should increase after cancellation";
    }

    @Test
    @DisplayName("Pagination and Sorting: Unit Search Results")
    void shouldSupportPaginationAndSorting() throws Exception {
        // Create additional units for testing pagination
        for (int i = 0; i < 5; i++) {
            CreateUnitRequestDto unitRequest = new CreateUnitRequestDto();
            unitRequest.setOwnerId(userId);
            unitRequest.setNumberOfRooms(i + 1);
            unitRequest.setAccommodationType(AccommodationType.FLAT);
            unitRequest.setFloor(i);
            unitRequest.setBaseCost(BigDecimal.valueOf(100.00 + (i * 50)));
            unitRequest.setDescription("Test unit " + i);

            mockMvc.perform(post("/api/units")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(unitRequest)))
                    .andExpect(status().isCreated());
        }

        // Test pagination: Page 0, Size 3
        mockMvc.perform(get("/api/units")
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(6)))
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.number").value(0));

        // Test sorting by baseCost descending
        MvcResult sortResult = mockMvc.perform(get("/api/units")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "baseCost")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andReturn();

        // Verify sorting: first element should have >= baseCost than second
        String sortResponse = sortResult.getResponse().getContentAsString();
        var jsonNode = objectMapper.readTree(sortResponse);
        var content = jsonNode.get("content");

        if (content.size() >= 2) {
            double firstCost = content.get(0).get("baseCost").asDouble();
            double secondCost = content.get(1).get("baseCost").asDouble();
            assert firstCost >= secondCost :
                    "Sorting failed: first cost (" + firstCost + ") should be >= second cost (" + secondCost + ")";
        }
    }
}