package com.tarasantoniuk.booking.repository;

import com.tarasantoniuk.booking.entity.Booking;
import com.tarasantoniuk.booking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Find bookings by status
     */
    List<Booking> findByStatus(BookingStatus status);

    /**
     * Find bookings by user ID
     */
    List<Booking> findByUserId(Long userId);

    /**
     * Find expired bookings with PENDING status
     */
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime expiresAt);

    /**
     * Find conflicting bookings for a unit in a date range
     * Checks for both PENDING and CONFIRMED bookings to prevent double booking
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.unit.id = :unitId
        AND b.status IN ('PENDING', 'CONFIRMED')
        AND (
            (b.startDate <= :endDate AND b.endDate >= :startDate)
        )
    """)
    List<Booking> findConflictingBookings(
            @Param("unitId") Long unitId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}