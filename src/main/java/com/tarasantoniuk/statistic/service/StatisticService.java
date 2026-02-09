package com.tarasantoniuk.statistic.service;

import com.tarasantoniuk.statistic.dto.AvailableUnitsStatisticDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticService {

    private final UnitStatisticsService unitStatisticsService;

    public AvailableUnitsStatisticDto getAvailableUnits() {
        Long count = unitStatisticsService.getAvailableUnitsCount();
        return new AvailableUnitsStatisticDto(count);
    }

    public AvailableUnitsStatisticDto refreshAvailableUnits() {
        Long count = unitStatisticsService.calculateAndCacheAvailableUnits();
        return new AvailableUnitsStatisticDto(count);
    }
}
