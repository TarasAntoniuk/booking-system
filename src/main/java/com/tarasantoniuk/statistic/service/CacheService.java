package com.tarasantoniuk.statistic.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for caching operations using Redis.
 * Handles ONLY cache storage and retrieval - NO business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private static final String AVAILABLE_UNITS_KEY = "stats:available_units_count";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Retrieve available units count from cache.
     *
     * @return cached count, or null if not in cache
     */
    public Long getAvailableUnitsCount() {
        try {
            Object cached = redisTemplate.opsForValue().get(AVAILABLE_UNITS_KEY);

            if (cached == null) {
                log.debug("Cache miss for available units count");
                return null;
            }

            log.debug("Cache hit for available units count: {}", cached);
            return ((Number) cached).longValue();
        } catch (Exception e) {
            log.warn("Redis unavailable for cache read, falling back to database", e);
            return null;
        }
    }

    /**
     * Cache the available units count.
     *
     * @param count the count to cache
     */
    public void cacheAvailableUnitsCount(Long count) {
        try {
            redisTemplate.opsForValue().set(AVAILABLE_UNITS_KEY, count);
            log.debug("Cached available units count: {}", count);
        } catch (Exception e) {
            log.warn("Redis unavailable for cache write, result not cached", e);
        }
    }

    /**
     * Invalidate (delete) the available units count from cache.
     *
     * Note: Uses lazy invalidation for simplicity. Next request will recalculate.
     * For production with high traffic, consider eager invalidation or async refresh.
     *
     * Trade-off: Fast invalidation vs first user after invalidation waits for DB query.
     */
    public void invalidateAvailableUnitsCount() {
        try {
            Boolean deleted = redisTemplate.delete(AVAILABLE_UNITS_KEY);
            log.debug("Invalidated available units cache, deleted: {}", deleted);
        } catch (Exception e) {
            log.warn("Redis unavailable for cache invalidation", e);
        }
    }
}
