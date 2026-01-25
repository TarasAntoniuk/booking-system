package com.tarasantoniuk.statistic.service;

import com.tarasantoniuk.statistic.dto.AvailableUnitsStatisticDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticService {

    private final CacheService cacheService;

    public AvailableUnitsStatisticDto getAvailableUnits() {
        Long count = cacheService.getAvailableUnitsCount();
        return new AvailableUnitsStatisticDto(count);
    }

    public AvailableUnitsStatisticDto refreshAvailableUnits() {
        Long count = cacheService.recalculateAndCache();
        return new AvailableUnitsStatisticDto(count);
    }
}