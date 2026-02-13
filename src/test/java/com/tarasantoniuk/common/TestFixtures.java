package com.tarasantoniuk.common;

import com.tarasantoniuk.booking.entity.Booking;
import com.tarasantoniuk.booking.enums.BookingStatus;
import com.tarasantoniuk.payment.entity.Payment;
import com.tarasantoniuk.payment.enums.PaymentStatus;
import com.tarasantoniuk.unit.entity.Unit;
import com.tarasantoniuk.unit.enums.AccommodationType;
import com.tarasantoniuk.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TestFixtures {

    private TestFixtures() {
    }

    /**
     * Sets the ID on any entity using reflection.
     * Entity IDs are protected from public setters (@Setter(AccessLevel.NONE)),
     * but tests need to set them for mock return values.
     */
    public static void setId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    public static User createTestUser() {
        User user = new User();
        setId(user, 1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        return user;
    }

    public static Unit createTestUnit() {
        Unit unit = new Unit();
        setId(unit, 1L);
        unit.setNumberOfRooms(2);
        unit.setAccommodationType(AccommodationType.FLAT);
        unit.setFloor(3);
        unit.setBaseCost(BigDecimal.valueOf(100));
        unit.setDescription("Test unit");
        return unit;
    }

    public static Booking createTestBooking(Unit unit, User user) {
        Booking booking = new Booking();
        setId(booking, 1L);
        booking.setUnit(unit);
        booking.setUser(user);
        booking.setStartDate(LocalDate.now().plusDays(1));
        booking.setEndDate(LocalDate.now().plusDays(3));
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        return booking;
    }

    public static Payment createTestPayment(Booking booking) {
        Payment payment = new Payment();
        setId(payment, 1L);
        payment.setBooking(booking);
        payment.setAmount(BigDecimal.valueOf(230.00));
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        return payment;
    }
}
