package com.tarasantoniuk.booking.repository;

import com.tarasantoniuk.booking.entity.Booking;
import com.tarasantoniuk.booking.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
     * Find bookings by user ID with unit eagerly fetched (avoids N+1)
     */
    @Query("SELECT b FROM Booking b JOIN FETCH b.unit WHERE b.user.id = :userId")
    List<Booking> findByUserIdWithUnit(@Param("userId") Long userId);

    /**
     * Find bookings by user ID with pagination and unit eagerly fetched (avoids N+1).
     * Uses countQuery to avoid FETCH in COUNT query.
     */
    @Query(value = "SELECT b FROM Booking b JOIN FETCH b.unit WHERE b.user.id = :userId",
            countQuery = "SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId")
    Page<Booking> findByUserIdWithUnit(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find booking by ID with unit eagerly fetched (avoids N+1)
     */
    @Query("SELECT b FROM Booking b JOIN FETCH b.unit WHERE b.id = :id")
    Optional<Booking> findByIdWithUnit(@Param("id") Long id);

    /**
     * Find booking by ID with pessimistic write lock (prevents concurrent payment processing)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdWithLock(@Param("id") Long id);

    /**
     * Find expired bookings with PENDING status
     */
    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime expiresAt);

    /**
     * Find IDs of expired pending bookings.
     * Used by scheduler to create audit events before bulk cancellation.
     */
    @Query("""
        SELECT b.id FROM Booking b
        WHERE b.status = 'PENDING'
        AND b.expiresAt < :now
    """)
    List<Long> findExpiredPendingBookingIds(@Param("now") LocalDateTime now);

    /**
     * Bulk cancel all expired pending bookings in a single UPDATE query.
     *
     * @return number of cancelled bookings
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Booking b
        SET b.status = 'CANCELLED'
        WHERE b.status = 'PENDING'
        AND b.expiresAt < :now
    """)
    int bulkCancelExpiredBookings(@Param("now") LocalDateTime now);

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