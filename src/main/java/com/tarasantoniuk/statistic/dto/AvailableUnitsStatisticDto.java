package com.tarasantoniuk.statistic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Statistics about available units")
public class AvailableUnitsStatisticDto {

    @Schema(description = "Number of units currently available for booking", example = "42")
    private Long availableUnitsCount;
}