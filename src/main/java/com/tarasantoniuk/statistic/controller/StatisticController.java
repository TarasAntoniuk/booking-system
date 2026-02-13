package com.tarasantoniuk.statistic.controller;

import com.tarasantoniuk.statistic.dto.AvailableUnitsStatisticDto;
import com.tarasantoniuk.statistic.service.StatisticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "Statistics and caching API - view and refresh cached statistics")
public class StatisticController {

    private final StatisticService statisticService;

    @GetMapping("/available-units")
    @Operation(
            summary = "Get available units count",
            description = "Returns the cached count of accommodation units currently available for booking. " +
                    "This value is cached in Redis and automatically invalidated when bookings change."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AvailableUnitsStatisticDto.class))),
            @ApiResponse(responseCode = "503", description = "Cache service unavailable"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AvailableUnitsStatisticDto> getAvailableUnitsCount() {
        AvailableUnitsStatisticDto stats = statisticService.getAvailableUnits();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/available-units/refresh")
    @Operation(
            summary = "Refresh available units cache",
            description = "Forces a refresh of the available units count cache. " +
                    "Use this endpoint if you suspect the cached value is stale."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cache refreshed successfully",
                    content = @Content(schema = @Schema(implementation = AvailableUnitsStatisticDto.class))),
            @ApiResponse(responseCode = "503", description = "Cache service unavailable"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AvailableUnitsStatisticDto> refreshAvailableUnitsCache() {
        AvailableUnitsStatisticDto stats = statisticService.refreshAvailableUnits();
        return ResponseEntity.ok(stats);
    }
}
