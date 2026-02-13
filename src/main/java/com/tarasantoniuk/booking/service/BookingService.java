package com.tarasantoniuk.booking.service;

import com.tarasantoniuk.booking.dto.BookingResponseDto;
import com.tarasantoniuk.booking.dto.CreateBookingRequestDto;
import com.tarasantoniuk.booking.entity.Booking;
import com.tarasantoniuk.booking.enums.BookingStatus;
import com.tarasantoniuk.booking.event.BookingEvent;
import com.tarasantoniuk.booking.exception.UnitNotAvailableException;
import com.tarasantoniuk.booking.repository.BookingRepository;
import com.tarasantoniuk.common.exception.ResourceNotFoundException;
import com.tarasantoniuk.unit.entity.Unit;
import com.tarasantoniuk.unit.repository.UnitRepository;
import com.tarasantoniuk.user.entity.User;
import com.tarasantoniuk.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static com.tarasantoniuk.booking.config.BookingTimeConstants.BOOKING_EXPIRATION_MINUTES;
import static com.tarasantoniuk.booking.config.PricingConstants.MARKUP_MULTIPLIER;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    public BookingResponseDto createBooking(CreateBookingRequestDto request) {
        log.info("Creating booking for unitId={}, userId={}, dates={} to {}",
                request.getUnitId(), request.getUserId(), request.getStartDate(), request.getEndDate());

        // 1. Acquire pessimistic lock on unit to prevent race conditions
        // This ensures only one transaction can create a booking for this unit at a time
        Unit unit = unitRepository.findByIdWithLock(request.getUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id: " + request.getUnitId()));

        // 2. Validate user exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        // 3. Validate date range
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }

        // 4. Check unit availability (now safe from race conditions due to lock)
        if (!isUnitAvailable(request.getUnitId(), request.getStartDate(), request.getEndDate())) {
            throw new UnitNotAvailableException("Unit is not available for selected dates");
        }

        // 4. Create booking
        Booking booking = new Booking();
        booking.setUnit(unit);
        booking.setUser(user);
        booking.setStartDate(request.getStartDate());
        booking.setEndDate(request.getEndDate());
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(BOOKING_EXPIRATION_MINUTES));

        Booking saved = bookingRepository.save(booking);

        // 5. Calculate total cost
        BigDecimal totalCost = calculateTotalCost(unit, request.getStartDate(), request.getEndDate());

        // 6. Publish event (triggers payment creation, audit event, cache invalidation)
        eventPublisher.publishEvent(BookingEvent.created(saved.getId(), totalCost));

        log.info("Booking created successfully: bookingId={}, unitId={}, userId={}",
                saved.getId(), unit.getId(), user.getId());

        return BookingResponseDto.from(saved, totalCost);
    }

    public BookingResponseDto getBookingById(Long id) {
        Booking booking = bookingRepository.findByIdWithUnit(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        BigDecimal totalCost = calculateTotalCost(
                booking.getUnit(),
                booking.getStartDate(),
                booking.getEndDate()
        );

        return BookingResponseDto.from(booking, totalCost);
    }

    public Page<BookingResponseDto> getUserBookings(Long userId, Pageable pageable) {
        return bookingRepository.findByUserIdWithUnit(userId, pageable)
                .map(booking -> {
                    BigDecimal totalCost = calculateTotalCost(
                            booking.getUnit(),
                            booking.getStartDate(),
                            booking.getEndDate()
                    );
                    return BookingResponseDto.from(booking, totalCost);
                });
    }

    @Transactional
    public void cancelBooking(Long bookingId, Long userId) {
        log.info("Cancelling booking: bookingId={}, userId={}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            log.warn("Unauthorized cancel attempt: bookingId={}, requestUserId={}, ownerUserId={}",
                    bookingId, userId, booking.getUser().getId());
            throw new IllegalArgumentException("You can only cancel your own bookings");
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.warn("Cancelling confirmed booking {} - refund logic not yet implemented", bookingId);
        }

        booking.cancel();
        bookingRepository.save(booking);

        eventPublisher.publishEvent(BookingEvent.cancelled(bookingId));

        log.info("Booking cancelled successfully: bookingId={}", bookingId);
    }

    @Transactional
    public void confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        booking.confirm();
        bookingRepository.save(booking);

        eventPublisher.publishEvent(BookingEvent.confirmed(bookingId));

        log.info("Booking confirmed: bookingId={}", bookingId);
    }

    private boolean isUnitAvailable(Long unitId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return bookingRepository.findConflictingBookings(unitId, startDate, endDate).isEmpty();
    }

    private BigDecimal calculateTotalCost(Unit unit, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days <= 0) {
            days = 1; // minimum 1 day
        }

        BigDecimal baseCost = unit.getBaseCost().multiply(BigDecimal.valueOf(days));
        return baseCost.multiply(MARKUP_MULTIPLIER);
    }
}