package com.tarasantoniuk.statistic.service;

import com.tarasantoniuk.unit.repository.UnitRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for calculating unit-related statistics.
 * Handles business logic for unit availability metrics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UnitStatisticsService {

    private final UnitRepository unitRepository;
    private final CacheService cacheService;

    /**
     * Get the count of currently available units.
     * Uses cache if available, otherwise calculates and caches.
     *
     * @return number of available units
     */
    public Long getAvailableUnitsCount() {
        Long cached = cacheService.getAvailableUnitsCount();

        if (cached != null) {
            log.debug("Retrieved available units count from cache: {}", cached);
            return cached;
        }

        log.debug("Cache miss for available units count, calculating...");
        return calculateAndCacheAvailableUnits();
    }

    /**
     * Calculate available units count and update cache.
     *
     * @return number of available units
     */
    public Long calculateAndCacheAvailableUnits() {
        Long count = unitRepository.countAvailableUnits();
        cacheService.cacheAvailableUnitsCount(count);
        log.info("Calculated and cached available units count: {}", count);
        return count;
    }

    /**
     * Invalidate the available units cache.
     * Next request will trigger recalculation.
     */
    public void invalidateAvailableUnitsCache() {
        cacheService.invalidateAvailableUnitsCount();
        log.debug("Invalidated available units cache");
    }

    /**
     * Warm up cache on application startup.
     */
    @PostConstruct
    public void warmUpCache() {
        log.info("Warming up unit statistics cache on startup...");
        calculateAndCacheAvailableUnits();
    }
}
