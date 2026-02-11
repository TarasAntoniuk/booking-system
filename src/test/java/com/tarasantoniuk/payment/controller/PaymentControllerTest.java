package com.tarasantoniuk.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarasantoniuk.payment.dto.PaymentResponseDto;
import com.tarasantoniuk.payment.dto.ProcessPaymentRequestDto;
import com.tarasantoniuk.payment.enums.PaymentStatus;
import com.tarasantoniuk.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PaymentController Unit Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    @DisplayName("Should process payment and return 200")
    void shouldProcessPaymentAndReturn200() throws Exception {
        // Given
        ProcessPaymentRequestDto request = new ProcessPaymentRequestDto();
        request.setBookingId(1L);

        PaymentResponseDto response = new PaymentResponseDto();
        response.setId(1L);
        response.setBookingId(1L);
        response.setAmount(BigDecimal.valueOf(230.00));
        response.setStatus(PaymentStatus.COMPLETED);
        response.setCreatedAt(LocalDateTime.now());

        when(paymentService.processPayment(any(ProcessPaymentRequestDto.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bookingId").value(1))
                .andExpect(jsonPath("$.amount").value(230.00))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(paymentService).processPayment(any(ProcessPaymentRequestDto.class));
    }

    @Test
    @DisplayName("Should get payment by booking id")
    void shouldGetPaymentByBookingId() throws Exception {
        // Given
        PaymentResponseDto response = new PaymentResponseDto();
        response.setId(1L);
        response.setBookingId(1L);
        response.setAmount(BigDecimal.valueOf(230.00));
        response.setStatus(PaymentStatus.PENDING);
        response.setCreatedAt(LocalDateTime.now());

        when(paymentService.getPaymentByBookingId(1L)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/payments/booking/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bookingId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(paymentService).getPaymentByBookingId(1L);
    }
}
