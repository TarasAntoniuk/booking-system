package com.tarasantoniuk.statistic.controller;

import com.tarasantoniuk.statistic.dto.AvailableUnitsStatisticDto;
import com.tarasantoniuk.statistic.service.StatisticService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StatisticController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("StatisticController Unit Tests")
class StatisticControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatisticService statisticService;

    @Test
    @DisplayName("Should get available units count")
    void shouldGetAvailableUnitsCount() throws Exception {
        // Given
        AvailableUnitsStatisticDto stats = new AvailableUnitsStatisticDto(42L);
        when(statisticService.getAvailableUnits()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/statistics/available-units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableUnitsCount").value(42));

        verify(statisticService).getAvailableUnits();
    }

    @Test
    @DisplayName("Should refresh available units cache")
    void shouldRefreshAvailableUnitsCache() throws Exception {
        // Given
        AvailableUnitsStatisticDto stats = new AvailableUnitsStatisticDto(25L);
        when(statisticService.refreshAvailableUnits()).thenReturn(stats);

        // When & Then
        mockMvc.perform(post("/api/statistics/available-units/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableUnitsCount").value(25));

        verify(statisticService).refreshAvailableUnits();
    }

    @Test
    @DisplayName("Should handle zero available units")
    void shouldHandleZeroAvailableUnits() throws Exception {
        // Given
        AvailableUnitsStatisticDto stats = new AvailableUnitsStatisticDto(0L);
        when(statisticService.getAvailableUnits()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/statistics/available-units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableUnitsCount").value(0));

        verify(statisticService).getAvailableUnits();
    }
}
