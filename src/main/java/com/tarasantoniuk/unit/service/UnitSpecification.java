package com.tarasantoniuk.unit.service;

import com.tarasantoniuk.booking.entity.Booking;
import com.tarasantoniuk.booking.enums.BookingStatus;
import com.tarasantoniuk.unit.dto.UnitSearchCriteriaDto;
import com.tarasantoniuk.unit.entity.Unit;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static com.tarasantoniuk.booking.config.PricingConstants.MARKUP_MULTIPLIER;

/**
 * JPA Specification for filtering Unit entities based on search criteria.
 * Provides dynamic query building for unit searches with various filters.
 */
public class UnitSpecification {

    /**
     * Creates a specification for filtering units based on provided criteria.
     * Orchestrates all individual filter methods.
     *
     * @param criteria the search criteria
     * @return specification combining all applicable filters
     */
    public static Specification<Unit> withCriteria(UnitSearchCriteriaDto criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            addNumberOfRoomsFilter(criteria, root, cb, predicates);
            addAccommodationTypeFilter(criteria, root, cb, predicates);
            addFloorFilter(criteria, root, cb, predicates);
            addMinCostFilter(criteria, root, cb, predicates);
            addMaxCostFilter(criteria, root, cb, predicates);
            addDateAvailabilityFilter(criteria, root, query, cb, predicates);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addNumberOfRoomsFilter(
            UnitSearchCriteriaDto criteria,
            Root<Unit> root,
            CriteriaBuilder cb,
            List<Predicate> predicates
    ) {
        if (criteria.getNumberOfRooms() != null) {
            predicates.add(cb.equal(root.get("numberOfRooms"), criteria.getNumberOfRooms()));
        }
    }

    private static void addAccommodationTypeFilter(
            UnitSearchCriteriaDto criteria,
            Root<Unit> root,
            CriteriaBuilder cb,
            List<Predicate> predicates
    ) {
        if (criteria.getAccommodationType() != null) {
            predicates.add(cb.equal(root.get("accommodationType"), criteria.getAccommodationType()));
        }
    }

    private static void addFloorFilter(
            UnitSearchCriteriaDto criteria,
            Root<Unit> root,
            CriteriaBuilder cb,
            List<Predicate> predicates
    ) {
        if (criteria.getFloor() != null) {
            predicates.add(cb.equal(root.get("floor"), criteria.getFloor()));
        }
    }

    /**
     * Adds filter for minimum cost.
     * Converts user-facing cost (with markup) to base cost for DB query.
     */
    private static void addMinCostFilter(
            UnitSearchCriteriaDto criteria,
            Root<Unit> root,
            CriteriaBuilder cb,
            List<Predicate> predicates
    ) {
        if (criteria.getMinCost() != null) {
            BigDecimal minBaseCost = convertUserCostToBaseCost(criteria.getMinCost());
            predicates.add(cb.greaterThanOrEqualTo(root.get("baseCost"), minBaseCost));
        }
    }

    /**
     * Adds filter for maximum cost.
     * Converts user-facing cost (with markup) to base cost for DB query.
     */
    private static void addMaxCostFilter(
            UnitSearchCriteriaDto criteria,
            Root<Unit> root,
            CriteriaBuilder cb,
            List<Predicate> predicates
    ) {
        if (criteria.getMaxCost() != null) {
            BigDecimal maxBaseCost = convertUserCostToBaseCost(criteria.getMaxCost());
            predicates.add(cb.lessThanOrEqualTo(root.get("baseCost"), maxBaseCost));
        }
    }

    /**
     * Converts user-facing cost (including markup) to base cost.
     * Users see prices with markup, but database stores base cost.
     */
    private static BigDecimal convertUserCostToBaseCost(BigDecimal userCost) {
        return userCost.divide(MARKUP_MULTIPLIER, 2, RoundingMode.HALF_UP);
    }

    /**
     * Adds filter for date availability if date range specified.
     * Excludes units that have conflicting bookings (PENDING or CONFIRMED)
     * in the requested date range.
     */
    private static void addDateAvailabilityFilter(
            UnitSearchCriteriaDto criteria,
            Root<Unit> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            List<Predicate> predicates
    ) {
        if (criteria.getStartDate() == null || criteria.getEndDate() == null) {
            return;
        }

        Subquery<Long> unavailableUnits = createUnavailableUnitsSubquery(
                criteria, root, query, cb
        );
        predicates.add(cb.not(root.get("id").in(unavailableUnits)));
    }

    /**
     * Creates subquery selecting unit IDs that have conflicting bookings
     * for the requested date range.
     */
    private static Subquery<Long> createUnavailableUnitsSubquery(
            UnitSearchCriteriaDto criteria,
            Root<Unit> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb
    ) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<Booking> bookingRoot = subquery.from(Booking.class);

        subquery.select(bookingRoot.get("unit").get("id"));
        subquery.where(cb.and(
                cb.equal(bookingRoot.get("unit").get("id"), root.get("id")),
                createBookingStatusPredicate(bookingRoot, cb),
                createDateOverlapPredicate(criteria, bookingRoot, cb)
        ));

        return subquery;
    }

    /**
     * Creates predicate to filter bookings by active status.
     * Only PENDING and CONFIRMED bookings block availability.
     */
    private static Predicate createBookingStatusPredicate(
            Root<Booking> bookingRoot,
            CriteriaBuilder cb
    ) {
        return cb.or(
                cb.equal(bookingRoot.get("status"), BookingStatus.CONFIRMED),
                cb.equal(bookingRoot.get("status"), BookingStatus.PENDING)
        );
    }

    /**
     * Creates predicate to check if booking dates overlap with requested dates.
     * Handles three overlap scenarios:
     * 1. Requested period starts during existing booking
     * 2. Requested period ends during existing booking
     * 3. Requested period completely encompasses existing booking
     */
    private static Predicate createDateOverlapPredicate(
            UnitSearchCriteriaDto criteria,
            Root<Booking> bookingRoot,
            CriteriaBuilder cb
    ) {
        return cb.or(
                // Requested start falls within existing booking
                cb.and(
                        cb.lessThanOrEqualTo(bookingRoot.get("startDate"), criteria.getStartDate()),
                        cb.greaterThanOrEqualTo(bookingRoot.get("endDate"), criteria.getStartDate())
                ),
                // Requested end falls within existing booking
                cb.and(
                        cb.lessThanOrEqualTo(bookingRoot.get("startDate"), criteria.getEndDate()),
                        cb.greaterThanOrEqualTo(bookingRoot.get("endDate"), criteria.getEndDate())
                ),
                // Requested period completely covers existing booking
                cb.and(
                        cb.greaterThanOrEqualTo(bookingRoot.get("startDate"), criteria.getStartDate()),
                        cb.lessThanOrEqualTo(bookingRoot.get("endDate"), criteria.getEndDate())
                )
        );
    }
}
