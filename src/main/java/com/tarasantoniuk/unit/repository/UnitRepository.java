package com.tarasantoniuk.unit.repository;

import com.tarasantoniuk.unit.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long>, JpaSpecificationExecutor<Unit> {

    /**
     * Count available units (without active or future bookings)
     * Excludes units with PENDING or CONFIRMED bookings that end today or later
     */
    @Query("""
        SELECT COUNT(DISTINCT u.id)
            FROM Unit u
            LEFT JOIN Booking b ON b.unit.id = u.id\s
                AND b.status IN ('PENDING', 'CONFIRMED')
                AND b.endDate >= CURRENT_DATE
            WHERE b.id IS NULL
    """)
    Long countAvailableUnits();

    /**
     * Find available units for specific date range
     * Uses LEFT JOIN for better performance than NOT IN subquery
     */
    @Query("""
    SELECT DISTINCT u\s
    FROM Unit u
    LEFT JOIN Booking b ON b.unit.id = u.id
        AND b.status IN ('PENDING', 'CONFIRMED')
        AND b.startDate <= :endDate\s
        AND b.endDate >= :startDate
    WHERE b.id IS NULL
   \s""")
    List<Unit> findAvailableUnits(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<Unit> findByOwnerId(Long ownerId);
}