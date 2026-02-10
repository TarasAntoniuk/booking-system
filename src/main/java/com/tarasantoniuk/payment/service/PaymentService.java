package com.tarasantoniuk.payment.service;

import com.tarasantoniuk.common.exception.ResourceNotFoundException;
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
import com.tarasantoniuk.statistic.service.UnitStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final EventService eventService;
    private final UnitStatisticsService unitStatisticsService;

    @Transactional
    public Payment createPayment(Booking booking, BigDecimal amount) {
        log.info("Creating payment for bookingId={}, amount={}", booking.getId(), amount);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);

        Payment saved = paymentRepository.save(payment);
        log.info("Payment created: paymentId={}, bookingId={}", saved.getId(), booking.getId());
        return saved;
    }

    @Transactional
    public PaymentResponseDto processPayment(ProcessPaymentRequestDto request) {
        log.info("Processing payment for bookingId={}", request.getBookingId());

        // 1. Find booking with pessimistic lock to prevent concurrent payment processing
        Booking booking = bookingRepository.findByIdWithLock(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + request.getBookingId()));

        // 2. Check if booking is still pending
        if (booking.getStatus() != BookingStatus.PENDING) {
            log.warn("Payment attempt for non-PENDING booking: bookingId={}, status={}",
                    request.getBookingId(), booking.getStatus());
            throw new IllegalArgumentException("Booking is not in PENDING status");
        }

        // 3. Find payment
        Payment payment = paymentRepository.findByBookingId(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking: " + request.getBookingId()));

        // 4. Process payment (emulation)
        payment.setStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);

        // 5. Confirm booking
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setExpiresAt(null); // Remove expiration
        bookingRepository.save(booking);

        // 6. Create events
        eventService.createEvent(EventType.PAYMENT_COMPLETED, payment.getId());
        eventService.createEvent(EventType.BOOKING_CONFIRMED, booking.getId());

        // 7. Invalidate cache
        unitStatisticsService.invalidateAvailableUnitsCache();

        log.info("Payment processed successfully: paymentId={}, bookingId={}", payment.getId(), booking.getId());

        return PaymentResponseDto.from(payment);
    }

    public PaymentResponseDto getPaymentByBookingId(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking: " + bookingId));
        return PaymentResponseDto.from(payment);
    }
}