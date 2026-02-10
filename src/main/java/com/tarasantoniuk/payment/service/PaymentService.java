package com.tarasantoniuk.payment.service;

import com.tarasantoniuk.common.exception.ResourceNotFoundException;
import com.tarasantoniuk.booking.entity.Booking;
import com.tarasantoniuk.booking.enums.BookingStatus;
import com.tarasantoniuk.booking.repository.BookingRepository;
import com.tarasantoniuk.booking.service.BookingService;
import com.tarasantoniuk.event.enums.EventType;
import com.tarasantoniuk.event.service.EventService;
import com.tarasantoniuk.payment.dto.PaymentResponseDto;
import com.tarasantoniuk.payment.dto.ProcessPaymentRequestDto;
import com.tarasantoniuk.payment.entity.Payment;
import com.tarasantoniuk.payment.enums.PaymentStatus;
import com.tarasantoniuk.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final EventService eventService;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository,
                          @Lazy BookingService bookingService,
                          EventService eventService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.eventService = eventService;
    }

    @Transactional
    public Payment createPaymentForBooking(Long bookingId, BigDecimal amount) {
        log.info("Creating payment for bookingId={}, amount={}", bookingId, amount);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);

        Payment saved = paymentRepository.save(payment);
        log.info("Payment created: paymentId={}, bookingId={}", saved.getId(), bookingId);
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

        // 5. Create payment event
        eventService.createEvent(EventType.PAYMENT_COMPLETED, payment.getId());

        // 6. Confirm booking (delegates to BookingService to respect domain boundary)
        bookingService.confirmBooking(booking.getId());

        log.info("Payment processed successfully: paymentId={}, bookingId={}", payment.getId(), booking.getId());

        return PaymentResponseDto.from(payment);
    }

    public PaymentResponseDto getPaymentByBookingId(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking: " + bookingId));
        return PaymentResponseDto.from(payment);
    }
}