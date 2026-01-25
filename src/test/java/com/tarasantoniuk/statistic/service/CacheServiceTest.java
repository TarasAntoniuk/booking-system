package com.tarasantoniuk.statistic.service;

import com.tarasantoniuk.unit.repository.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CacheService Unit Tests")
class CacheServiceTest {

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private CacheService cacheService;

    private static final String AVAILABLE_UNITS_KEY = "stats:available_units_count";

    @Test
    @DisplayName("Should get available units count from cache when present")
    void shouldGetAvailableUnitsCountFromCache() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(AVAILABLE_UNITS_KEY)).thenReturn(42);

        // When
        Long result = cacheService.getAvailableUnitsCount();

        // Then
        assertThat(result).isEqualTo(42L);
        verify(valueOperations).get(AVAILABLE_UNITS_KEY);
        verify(unitRepository, never()).countAvailableUnits();
    }

    @Test
    @DisplayName("Should recalculate when cache is empty")
    void shouldRecalculateWhenCacheEmpty() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(AVAILABLE_UNITS_KEY)).thenReturn(null);
        when(unitRepository.countAvailableUnits()).thenReturn(25L);

        // When
        Long result = cacheService.getAvailableUnitsCount();

        // Then
        assertThat(result).isEqualTo(25L);
        verify(valueOperations).get(AVAILABLE_UNITS_KEY);
        verify(unitRepository).countAvailableUnits();
        verify(valueOperations).set(AVAILABLE_UNITS_KEY, 25L);
    }

    @Test
    @DisplayName("Should handle Integer type from cache")
    void shouldHandleIntegerTypeFromCache() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(AVAILABLE_UNITS_KEY)).thenReturn(Integer.valueOf(30));

        // When
        Long result = cacheService.getAvailableUnitsCount();

        // Then
        assertThat(result).isEqualTo(30L);
        verify(valueOperations).get(AVAILABLE_UNITS_KEY);
        verify(unitRepository, never()).countAvailableUnits();
    }

    @Test
    @DisplayName("Should recalculate and cache successfully")
    void shouldRecalculateAndCacheSuccessfully() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(unitRepository.countAvailableUnits()).thenReturn(15L);

        // When
        Long result = cacheService.recalculateAndCache();

        // Then
        assertThat(result).isEqualTo(15L);
        verify(unitRepository).countAvailableUnits();
        verify(valueOperations).set(AVAILABLE_UNITS_KEY, 15L);
    }

    @Test
    @DisplayName("Should invalidate cache successfully")
    void shouldInvalidateCacheSuccessfully() {
        // Given
        when(redisTemplate.delete(AVAILABLE_UNITS_KEY)).thenReturn(true);

        // When
        cacheService.invalidateCache();

        // Then
        verify(redisTemplate).delete(AVAILABLE_UNITS_KEY);
    }

    @Test
    @DisplayName("Should handle cache invalidation when key doesn't exist")
    void shouldHandleCacheInvalidationWhenKeyDoesntExist() {
        // Given
        when(redisTemplate.delete(AVAILABLE_UNITS_KEY)).thenReturn(false);

        // When
        cacheService.invalidateCache();

        // Then
        verify(redisTemplate).delete(AVAILABLE_UNITS_KEY);
    }

    @Test
    @DisplayName("Should warm up cache on initialization")
    void shouldWarmUpCacheOnInit() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(unitRepository.countAvailableUnits()).thenReturn(50L);

        // When
        cacheService.warmUpCache();

        // Then
        verify(unitRepository).countAvailableUnits();
        verify(valueOperations).set(AVAILABLE_UNITS_KEY, 50L);
    }
}
