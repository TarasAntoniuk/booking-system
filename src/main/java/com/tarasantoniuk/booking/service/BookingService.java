package com.tarasantoniuk.booking.service;

import com.tarasantoniuk.booking.dto.BookingResponseDto;
import com.tarasantoniuk.booking.dto.CreateBookingRequestDto;
import com.tarasantoniuk.booking.entity.Booking;
import com.tarasantoniuk.booking.enums.BookingStatus;
import com.tarasantoniuk.booking.exception.UnitNotAvailableException;
import com.tarasantoniuk.booking.repository.BookingRepository;
import com.tarasantoniuk.event.enums.EventType;
import com.tarasantoniuk.event.service.EventService;
import com.tarasantoniuk.payment.service.PaymentService;
import com.tarasantoniuk.statistic.service.CacheService;
import com.tarasantoniuk.unit.entity.Unit;
import com.tarasantoniuk.unit.repository.UnitRepository;
import com.tarasantoniuk.user.entity.User;
import com.tarasantoniuk.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final EventService eventService;
    private final CacheService cacheService;


    @Transactional
    public BookingResponseDto createBooking(CreateBookingRequestDto request) {
        // 1. Validate unit exists
        Unit unit = unitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new IllegalArgumentException("Unit not found with id: " + request.getUnitId()));

        // 2. Validate user exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.getUserId()));

        // 3. Check unit availability
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
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        Booking saved = bookingRepository.save(booking);

        // 5. Calculate total cost
        BigDecimal totalCost = calculateTotalCost(unit, request.getStartDate(), request.getEndDate());

        // 6. Create payment
        paymentService.createPayment(saved, totalCost);

        // 7. Create event
        eventService.createEvent(EventType.BOOKING_CREATED, saved.getId());

        //8. Invalidate cache
        cacheService.invalidateCache();

        return BookingResponseDto.from(saved, totalCost);
    }

    public BookingResponseDto getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));

        BigDecimal totalCost = calculateTotalCost(
                booking.getUnit(),
                booking.getStartDate(),
                booking.getEndDate()
        );

        return BookingResponseDto.from(booking, totalCost);
    }

    public List<BookingResponseDto> getUserBookings(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        return bookings.stream()
                .map(booking -> {
                    BigDecimal totalCost = calculateTotalCost(
                            booking.getUnit(),
                            booking.getStartDate(),
                            booking.getEndDate()
                    );
                    return BookingResponseDto.from(booking, totalCost);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only cancel your own bookings");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Create event
        eventService.createEvent(EventType.BOOKING_CANCELLED, bookingId);

        cacheService.invalidateCache();
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
        BigDecimal markup = baseCost.multiply(BigDecimal.valueOf(0.15)); // 15%
        return baseCost.add(markup);
    }
}