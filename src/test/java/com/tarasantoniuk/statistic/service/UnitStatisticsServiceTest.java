package com.tarasantoniuk.statistic.service;

import com.tarasantoniuk.unit.repository.UnitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnitStatisticsService Unit Tests")
class UnitStatisticsServiceTest {

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private UnitStatisticsService unitStatisticsService;

    @Test
    @DisplayName("Should return cached value when present")
    void shouldReturnCachedValue() {
        // Given
        when(cacheService.getAvailableUnitsCount()).thenReturn(42L);

        // When
        Long result = unitStatisticsService.getAvailableUnitsCount();

        // Then
        assertThat(result).isEqualTo(42L);
        verify(cacheService).getAvailableUnitsCount();
        verify(unitRepository, never()).countAvailableUnits();
    }

    @Test
    @DisplayName("Should calculate and cache when cache miss")
    void shouldCalculateAndCacheWhenCacheMiss() {
        // Given
        when(cacheService.getAvailableUnitsCount()).thenReturn(null);
        when(unitRepository.countAvailableUnits()).thenReturn(15L);

        // When
        Long result = unitStatisticsService.getAvailableUnitsCount();

        // Then
        assertThat(result).isEqualTo(15L);
        verify(cacheService).getAvailableUnitsCount();
        verify(unitRepository).countAvailableUnits();
        verify(cacheService).cacheAvailableUnitsCount(15L);
    }

    @Test
    @DisplayName("Should calculate and cache available units")
    void shouldCalculateAndCacheAvailableUnits() {
        // Given
        when(unitRepository.countAvailableUnits()).thenReturn(25L);

        // When
        Long result = unitStatisticsService.calculateAndCacheAvailableUnits();

        // Then
        assertThat(result).isEqualTo(25L);
        verify(unitRepository).countAvailableUnits();
        verify(cacheService).cacheAvailableUnitsCount(25L);
    }

    @Test
    @DisplayName("Should invalidate cache")
    void shouldInvalidateCache() {
        // When
        unitStatisticsService.invalidateAvailableUnitsCache();

        // Then
        verify(cacheService).invalidateAvailableUnitsCount();
    }

    @Test
    @DisplayName("Should warm up cache on initialization")
    void shouldWarmUpCacheOnInit() {
        // Given
        when(unitRepository.countAvailableUnits()).thenReturn(50L);

        // When
        unitStatisticsService.warmUpCache();

        // Then
        verify(unitRepository).countAvailableUnits();
        verify(cacheService).cacheAvailableUnitsCount(50L);
    }

    @Test
    @DisplayName("Should handle zero available units")
    void shouldHandleZeroAvailableUnits() {
        // Given
        when(cacheService.getAvailableUnitsCount()).thenReturn(0L);

        // When
        Long result = unitStatisticsService.getAvailableUnitsCount();

        // Then
        assertThat(result).isEqualTo(0L);
        verify(unitRepository, never()).countAvailableUnits();
    }
}
