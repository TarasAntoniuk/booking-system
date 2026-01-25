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
     * Count available units (without confirmed bookings in the future)
     */
    @Query("""
        SELECT COUNT(DISTINCT u.id) FROM Unit u
        WHERE u.id NOT IN (
            SELECT DISTINCT b.unit.id FROM Booking b
            WHERE b.status = 'CONFIRMED'
            AND b.endDate >= CURRENT_DATE
        )
    """)
    Long countAvailableUnits();

    /**
     * Find available units for specific date range
     */
    @Query("""
        SELECT DISTINCT u FROM Unit u
        WHERE u.id NOT IN (
            SELECT b.unit.id FROM Booking b
            WHERE b.status = 'CONFIRMED'
            AND (
                (b.startDate <= :endDate AND b.endDate >= :startDate)
            )
        )
    """)
    List<Unit> findAvailableUnits(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<Unit> findByOwnerId(Long ownerId);
}