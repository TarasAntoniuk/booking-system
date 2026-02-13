package com.tarasantoniuk.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarasantoniuk.booking.dto.CreateBookingRequestDto;
import com.tarasantoniuk.booking.exception.UnitNotAvailableException;
import com.tarasantoniuk.booking.service.BookingService;
import com.tarasantoniuk.common.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Global Exception Handler Tests")
class GlobalExceptionHandlerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    @Test
    @DisplayName("Should return 409 Conflict when unit is not available")
    void shouldReturn409WhenUnitNotAvailable() throws Exception {
        // Given
        CreateBookingRequestDto request = new CreateBookingRequestDto();
        request.setUnitId(1L);
        request.setUserId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));

        when(bookingService.createBooking(any()))
                .thenThrow(new UnitNotAvailableException("Unit is not available for selected dates"));

        // When & Then
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Unit is not available for selected dates"))
                .andExpect(jsonPath("$.path").value("/api/v1/bookings"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should return 404 Not Found when ResourceNotFoundException is thrown")
    void shouldReturn404WhenResourceNotFound() throws Exception {
        // Given
        CreateBookingRequestDto request = new CreateBookingRequestDto();
        request.setUnitId(999L);
        request.setUserId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));

        when(bookingService.createBooking(any()))
                .thenThrow(new ResourceNotFoundException("Unit not found with id: 999"));

        // When & Then
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Unit not found with id: 999"))
                .andExpect(jsonPath("$.path").value("/api/v1/bookings"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should return 400 Bad Request for invalid JSON")
    void shouldReturn400ForInvalidJson() throws Exception {
        // Given - Invalid JSON (missing quotes)
        String invalidJson = "{unitId: 1, userId: 1}";

        // When & Then
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid JSON format or missing required fields"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error for unexpected exceptions")
    void shouldReturn500ForUnexpectedException() throws Exception {
        // Given
        CreateBookingRequestDto request = new CreateBookingRequestDto();
        request.setUnitId(1L);
        request.setUserId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));

        when(bookingService.createBooking(any()))
                .thenThrow(new RuntimeException("Unexpected database error"));

        // When & Then
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should return 400 with field details when @Valid fails")
    void shouldReturn400WithFieldDetailsWhenValidationFails() throws Exception {
        // Given - empty request body triggers @NotNull violations
        CreateBookingRequestDto request = new CreateBookingRequestDto();
        // All fields are null, violating @NotNull constraints

        // When & Then
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value("Invalid request data"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should return 400 with type info when path variable type mismatches")
    void shouldReturn400WhenPathVariableTypeMismatches() throws Exception {
        // Given - "abc" cannot be parsed as Long for {id} path variable

        // When & Then
        mockMvc.perform(get("/api/v1/bookings/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Invalid value for parameter")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should format message with 'unknown' when requiredType is null")
    void shouldHandleTypeMismatchWithNullRequiredType() {
        // Given - directly invoke handler with a crafted exception where requiredType is null
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", null, "id", null, null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bookings/abc");

        // When
        var response = handler.handleTypeMismatch(ex, request);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .contains("Invalid value for parameter 'id'")
                .contains("unknown");
    }
}
