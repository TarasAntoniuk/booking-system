package com.tarasantoniuk.statistic.service;

import com.tarasantoniuk.unit.repository.UnitRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private static final String AVAILABLE_UNITS_KEY = "stats:available_units_count";

    private final UnitRepository unitRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Get available units count from cache or database
     */
    public Long getAvailableUnitsCount() {
        Object cached = redisTemplate.opsForValue().get(AVAILABLE_UNITS_KEY);

        if (cached != null) {
            log.debug("Retrieved available units count from cache: {}", cached);
            return ((Number) cached).longValue();
        }

        log.debug("Cache miss for available units count, recalculating...");
        return recalculateAndCache();
    }

    /**
     * Recalculate and update cache
     */
    public Long recalculateAndCache() {
        Long count = unitRepository.countAvailableUnits();
        redisTemplate.opsForValue().set(AVAILABLE_UNITS_KEY, count);
        log.info("Cached available units count: {}", count);
        return count;
    }

    /**
     * Invalidate cache (lazy approach)*
     * <p>
     * Note: Uses lazy invalidation for simplicity. Next request will recalculate.
     * For production with high traffic, consider eager invalidation or async refresh.
     * <p>
     * Trade-off: Fast invalidation vs first user after invalidation waits for DB query.
     */
    public void invalidateCache() {
        Boolean deleted = redisTemplate.delete(AVAILABLE_UNITS_KEY);
        log.debug("Invalidated available units cache, deleted: {}", deleted);
    }

    /**
     * Warm up cache on application startup
     */
    @PostConstruct
    public void warmUpCache() {
        log.info("Warming up cache on application startup...");
        recalculateAndCache();
    }
}