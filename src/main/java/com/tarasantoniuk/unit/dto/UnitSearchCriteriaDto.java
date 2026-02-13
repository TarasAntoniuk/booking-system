package com.tarasantoniuk.unit.dto;

import com.tarasantoniuk.unit.enums.AccommodationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Search criteria for filtering accommodation units")
public class UnitSearchCriteriaDto {

    @Schema(description = "Filter by exact number of rooms", example = "2")
    @Min(value = 1, message = "Number of rooms must be at least 1")
    private Integer numberOfRooms;

    @Schema(description = "Filter by accommodation type", example = "FLAT", allowableValues = {"HOME", "FLAT", "APARTMENT"})
    private AccommodationType accommodationType;

    @Schema(description = "Filter by exact floor number", example = "3")
    private Integer floor;

    @Schema(description = "Minimum cost per night filter", example = "50.00")
    @DecimalMin(value = "0.00", message = "Minimum cost cannot be negative")
    private BigDecimal minCost;

    @Schema(description = "Maximum cost per night filter", example = "200.00")
    @DecimalMin(value = "0.00", message = "Maximum cost cannot be negative")
    private BigDecimal maxCost;

    @Schema(description = "Filter for units available from this date", example = "2026-02-01")
    private LocalDate startDate;

    @Schema(description = "Filter for units available until this date", example = "2026-02-05")
    private LocalDate endDate;
}
