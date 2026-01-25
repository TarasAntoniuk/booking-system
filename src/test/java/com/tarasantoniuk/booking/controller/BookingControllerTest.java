package com.tarasantoniuk.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarasantoniuk.booking.dto.BookingResponseDto;
import com.tarasantoniuk.booking.dto.CreateBookingRequestDto;
import com.tarasantoniuk.booking.enums.BookingStatus;
import com.tarasantoniuk.booking.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookingController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("BookingController Unit Tests")
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    @Test
    @DisplayName("Should create booking and return 201")
    void shouldCreateBookingAndReturn201() throws Exception {
        // Given
        CreateBookingRequestDto request = new CreateBookingRequestDto();
        request.setUnitId(1L);
        request.setUserId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));

        BookingResponseDto response = new BookingResponseDto();
        response.setId(1L);
        response.setUnitId(1L);
        response.setUserId(1L);
        response.setStartDate(request.getStartDate());
        response.setEndDate(request.getEndDate());
        response.setStatus(BookingStatus.PENDING);
        response.setTotalCost(BigDecimal.valueOf(230.00));
        response.setCreatedAt(LocalDateTime.now());

        when(bookingService.createBooking(any(CreateBookingRequestDto.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalCost").value(230.00));

        verify(bookingService).createBooking(any(CreateBookingRequestDto.class));
    }

    @Test
    @DisplayName("Should get booking by id")
    void shouldGetBookingById() throws Exception {
        // Given
        BookingResponseDto response = new BookingResponseDto();
        response.setId(1L);
        response.setUnitId(1L);
        response.setUserId(1L);
        response.setStatus(BookingStatus.CONFIRMED);
        response.setTotalCost(BigDecimal.valueOf(230.00));

        when(bookingService.getBookingById(1L)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/bookings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(bookingService).getBookingById(1L);
    }

    @Test
    @DisplayName("Should get all bookings for a user")
    void shouldGetAllBookingsForUser() throws Exception {
        // Given
        BookingResponseDto booking1 = new BookingResponseDto();
        booking1.setId(1L);
        booking1.setUserId(1L);
        booking1.setStatus(BookingStatus.CONFIRMED);

        BookingResponseDto booking2 = new BookingResponseDto();
        booking2.setId(2L);
        booking2.setUserId(1L);
        booking2.setStatus(BookingStatus.PENDING);

        when(bookingService.getUserBookings(1L)).thenReturn(List.of(booking1, booking2));

        // When & Then
        mockMvc.perform(get("/api/bookings/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(bookingService).getUserBookings(1L);
    }

    @Test
    @DisplayName("Should cancel booking")
    void shouldCancelBooking() throws Exception {
        // Given
        doNothing().when(bookingService).cancelBooking(1L, 1L);

        // When & Then
        mockMvc.perform(delete("/api/bookings/1/cancel")
                        .param("userId", "1"))
                .andExpect(status().isNoContent());

        verify(bookingService).cancelBooking(1L, 1L);
    }
}
