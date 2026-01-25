package com.tarasantoniuk.unit.service;

import com.tarasantoniuk.booking.entity.Booking;
import com.tarasantoniuk.booking.enums.BookingStatus;
import com.tarasantoniuk.unit.dto.UnitSearchCriteriaDto;
import com.tarasantoniuk.unit.entity.Unit;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class UnitSpecification {

    public static Specification<Unit> withCriteria(UnitSearchCriteriaDto criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by number of rooms
            if (criteria.getNumberOfRooms() != null) {
                predicates.add(cb.equal(root.get("numberOfRooms"), criteria.getNumberOfRooms()));
            }

            // Filter by accommodation type
            if (criteria.getAccommodationType() != null) {
                predicates.add(cb.equal(root.get("accommodationType"), criteria.getAccommodationType()));
            }

            // Filter by floor
            if (criteria.getFloor() != null) {
                predicates.add(cb.equal(root.get("floor"), criteria.getFloor()));
            }

            // Filter by minimum cost (considering 15% markup)
            if (criteria.getMinCost() != null) {
                BigDecimal minBaseCost = criteria.getMinCost().divide(
                        BigDecimal.valueOf(1.15), 2, RoundingMode.HALF_UP
                );
                predicates.add(cb.greaterThanOrEqualTo(root.get("baseCost"), minBaseCost));
            }

            // Filter by maximum cost (considering 15% markup)
            if (criteria.getMaxCost() != null) {
                BigDecimal maxBaseCost = criteria.getMaxCost().divide(
                        BigDecimal.valueOf(1.15), 2, RoundingMode.HALF_UP
                );
                predicates.add(cb.lessThanOrEqualTo(root.get("baseCost"), maxBaseCost));
            }

            // Filter by availability in date range
            if (criteria.getStartDate() != null && criteria.getEndDate() != null) {
                Subquery<Long> bookingSubquery = query.subquery(Long.class);
                Root<Booking> bookingRoot = bookingSubquery.from(Booking.class);
                bookingSubquery.select(bookingRoot.get("unit").get("id"));
                bookingSubquery.where(
                        cb.and(
                                cb.equal(bookingRoot.get("unit").get("id"), root.get("id")),
                                // Include both PENDING and CONFIRMED bookings
                                cb.or(
                                        cb.equal(bookingRoot.get("status"), BookingStatus.CONFIRMED),
                                        cb.equal(bookingRoot.get("status"), BookingStatus.PENDING)
                                ),
                                cb.or(
                                        // New booking starts during existing booking
                                        cb.and(
                                                cb.lessThanOrEqualTo(bookingRoot.get("startDate"), criteria.getStartDate()),
                                                cb.greaterThanOrEqualTo(bookingRoot.get("endDate"), criteria.getStartDate())
                                        ),
                                        // New booking ends during existing booking
                                        cb.and(
                                                cb.lessThanOrEqualTo(bookingRoot.get("startDate"), criteria.getEndDate()),
                                                cb.greaterThanOrEqualTo(bookingRoot.get("endDate"), criteria.getEndDate())
                                        ),
                                        // New booking completely covers existing booking
                                        cb.and(
                                                cb.greaterThanOrEqualTo(bookingRoot.get("startDate"), criteria.getStartDate()),
                                                cb.lessThanOrEqualTo(bookingRoot.get("endDate"), criteria.getEndDate())
                                        )
                                )
                        )
                );
                predicates.add(cb.not(root.get("id").in(bookingSubquery)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}