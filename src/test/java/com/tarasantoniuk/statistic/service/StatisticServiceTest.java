package com.tarasantoniuk.statistic.service;

import com.tarasantoniuk.statistic.dto.AvailableUnitsStatisticDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatisticService Unit Tests")
class StatisticServiceTest {

    @Mock
    private UnitStatisticsService unitStatisticsService;

    @InjectMocks
    private StatisticService statisticService;

    @Test
    @DisplayName("Should get available units from cache")
    void shouldGetAvailableUnitsFromCache() {
        // Given
        when(unitStatisticsService.getAvailableUnitsCount()).thenReturn(42L);

        // When
        AvailableUnitsStatisticDto result = statisticService.getAvailableUnits();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAvailableUnitsCount()).isEqualTo(42L);
        verify(unitStatisticsService).getAvailableUnitsCount();
    }

    @Test
    @DisplayName("Should refresh available units and return new count")
    void shouldRefreshAvailableUnits() {
        // Given
        when(unitStatisticsService.calculateAndCacheAvailableUnits()).thenReturn(25L);

        // When
        AvailableUnitsStatisticDto result = statisticService.refreshAvailableUnits();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAvailableUnitsCount()).isEqualTo(25L);
        verify(unitStatisticsService).calculateAndCacheAvailableUnits();
    }

    @Test
    @DisplayName("Should handle zero available units")
    void shouldHandleZeroAvailableUnits() {
        // Given
        when(unitStatisticsService.getAvailableUnitsCount()).thenReturn(0L);

        // When
        AvailableUnitsStatisticDto result = statisticService.getAvailableUnits();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAvailableUnitsCount()).isEqualTo(0L);
    }
}
