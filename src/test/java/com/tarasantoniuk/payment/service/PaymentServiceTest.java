package com.tarasantoniuk.payment.service;

import com.tarasantoniuk.booking.entity.Booking;
import com.tarasantoniuk.booking.enums.BookingStatus;
import com.tarasantoniuk.booking.repository.BookingRepository;
import com.tarasantoniuk.event.enums.EventType;
import com.tarasantoniuk.event.service.EventService;
import com.tarasantoniuk.payment.dto.PaymentResponseDto;
import com.tarasantoniuk.payment.dto.ProcessPaymentRequestDto;
import com.tarasantoniuk.payment.entity.Payment;
import com.tarasantoniuk.payment.enums.PaymentStatus;
import com.tarasantoniuk.payment.repository.PaymentRepository;
import com.tarasantoniuk.statistic.service.CacheService;
import com.tarasantoniuk.unit.entity.Unit;
import com.tarasantoniuk.unit.enums.AccommodationType;
import com.tarasantoniuk.user.entity.User;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventService eventService;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private Unit testUnit;
    private Booking testBooking;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testUnit = new Unit();
        testUnit.setId(1L);
        testUnit.setBaseCost(BigDecimal.valueOf(100));
        testUnit.setAccommodationType(AccommodationType.FLAT);

        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setUnit(testUnit);
        testBooking.setUser(testUser);
        testBooking.setStartDate(LocalDate.now().plusDays(1));
        testBooking.setEndDate(LocalDate.now().plusDays(3));
        testBooking.setStatus(BookingStatus.PENDING);
        testBooking.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setBooking(testBooking);
        testPayment.setAmount(BigDecimal.valueOf(230.00));
        testPayment.setStatus(PaymentStatus.PENDING);
        testPayment.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create payment successfully")
    void shouldCreatePaymentSuccessfully() {
        // Given
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // When
        Payment result = paymentService.createPayment(testBooking, BigDecimal.valueOf(230.00));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBooking()).isEqualTo(testBooking);
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(230.00));
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should process payment successfully")
    void shouldProcessPaymentSuccessfully() {
        // Given
        ProcessPaymentRequestDto request = new ProcessPaymentRequestDto();
        request.setBookingId(1L);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        // When
        PaymentResponseDto response = paymentService.processPayment(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        verify(bookingRepository).findById(1L);
        verify(paymentRepository).findByBookingId(1L);
        verify(paymentRepository).save(testPayment);
        verify(bookingRepository).save(testBooking);
        verify(eventService).createEvent(EventType.PAYMENT_COMPLETED, 1L);
        verify(eventService).createEvent(EventType.BOOKING_CONFIRMED, 1L);

        assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(testBooking.getExpiresAt()).isNull();
    }

    @Test
    @DisplayName("Should throw exception when booking not found")
    void shouldThrowExceptionWhenBookingNotFound() {
        // Given
        ProcessPaymentRequestDto request = new ProcessPaymentRequestDto();
        request.setBookingId(999L);

        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Booking not found");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should throw exception when booking not in PENDING status")
    void shouldThrowExceptionWhenBookingNotPending() {
        // Given
        testBooking.setStatus(BookingStatus.CONFIRMED);
        ProcessPaymentRequestDto request = new ProcessPaymentRequestDto();
        request.setBookingId(1L);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Booking is not in PENDING status");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should throw exception when payment not found")
    void shouldThrowExceptionWhenPaymentNotFound() {
        // Given
        ProcessPaymentRequestDto request = new ProcessPaymentRequestDto();
        request.setBookingId(1L);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment not found for booking");

        verify(eventService, never()).createEvent(any(), any());
    }

    @Test
    @DisplayName("Should get payment by booking id successfully")
    void shouldGetPaymentByBookingIdSuccessfully() {
        // Given
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(testPayment));

        // When
        PaymentResponseDto response = paymentService.getPaymentByBookingId(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getBookingId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);

        verify(paymentRepository).findByBookingId(1L);
    }

    @Test
    @DisplayName("Should throw exception when payment not found by booking id")
    void shouldThrowExceptionWhenPaymentNotFoundByBookingId() {
        // Given
        when(paymentRepository.findByBookingId(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.getPaymentByBookingId(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment not found for booking");

        verify(paymentRepository).findByBookingId(999L);
    }
}
