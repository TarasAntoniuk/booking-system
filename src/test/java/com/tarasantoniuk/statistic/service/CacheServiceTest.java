package com.tarasantoniuk.statistic.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import org.springframework.data.redis.RedisConnectionFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CacheService Unit Tests")
class CacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private CacheService cacheService;

    private static final String AVAILABLE_UNITS_KEY = "stats:available_units_count";

    @Test
    @DisplayName("Should return cached value when present")
    void shouldReturnCachedValueWhenPresent() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(AVAILABLE_UNITS_KEY)).thenReturn(42);

        // When
        Long result = cacheService.getAvailableUnitsCount();

        // Then
        assertThat(result).isEqualTo(42L);
        verify(valueOperations).get(AVAILABLE_UNITS_KEY);
    }

    @Test
    @DisplayName("Should return null when cache is empty")
    void shouldReturnNullWhenCacheEmpty() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(AVAILABLE_UNITS_KEY)).thenReturn(null);

        // When
        Long result = cacheService.getAvailableUnitsCount();

        // Then
        assertThat(result).isNull();
        verify(valueOperations).get(AVAILABLE_UNITS_KEY);
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
    }

    @Test
    @DisplayName("Should store value in cache")
    void shouldStoreValueInCache() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        cacheService.cacheAvailableUnitsCount(15L);

        // Then
        verify(valueOperations).set(AVAILABLE_UNITS_KEY, 15L);
    }

    @Test
    @DisplayName("Should invalidate cache successfully")
    void shouldInvalidateCacheSuccessfully() {
        // Given
        when(redisTemplate.delete(AVAILABLE_UNITS_KEY)).thenReturn(true);

        // When
        cacheService.invalidateAvailableUnitsCount();

        // Then
        verify(redisTemplate).delete(AVAILABLE_UNITS_KEY);
    }

    @Test
    @DisplayName("Should handle cache invalidation when key doesn't exist")
    void shouldHandleCacheInvalidationWhenKeyDoesntExist() {
        // Given
        when(redisTemplate.delete(AVAILABLE_UNITS_KEY)).thenReturn(false);

        // When
        cacheService.invalidateAvailableUnitsCount();

        // Then
        verify(redisTemplate).delete(AVAILABLE_UNITS_KEY);
    }

    @Test
    @DisplayName("Should return null when Redis is down during read")
    void shouldReturnNullWhenRedisDownDuringRead() {
        // Given
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("Connection refused"));

        // When
        Long result = cacheService.getAvailableUnitsCount();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should not throw when Redis is down during cache write")
    void shouldNotThrowWhenRedisDownDuringWrite() {
        // Given
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("Connection refused"));

        // When & Then
        assertThatCode(() -> cacheService.cacheAvailableUnitsCount(15L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should not throw when Redis is down during invalidation")
    void shouldNotThrowWhenRedisDownDuringInvalidation() {
        // Given
        when(redisTemplate.delete(AVAILABLE_UNITS_KEY))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));

        // When & Then
        assertThatCode(() -> cacheService.invalidateAvailableUnitsCount())
                .doesNotThrowAnyException();
    }
}
