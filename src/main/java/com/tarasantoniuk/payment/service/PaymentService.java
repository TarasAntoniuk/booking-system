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
import com.tarasantoniuk.statistic.service.UnitStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final EventService eventService;
    private final UnitStatisticsService unitStatisticsService;

    @Transactional
    public Payment createPayment(Booking booking, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);

        return paymentRepository.save(payment);
    }

    @Transactional
    public PaymentResponseDto processPayment(ProcessPaymentRequestDto request) {
        // 1. Find booking
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + request.getBookingId()));

        // 2. Check if booking is still pending
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Booking is not in PENDING status");
        }

        // 3. Find payment
        Payment payment = paymentRepository.findByBookingId(request.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for booking: " + request.getBookingId()));

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

        return PaymentResponseDto.from(payment);
    }

    public PaymentResponseDto getPaymentByBookingId(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for booking: " + bookingId));
        return PaymentResponseDto.from(payment);
    }
}