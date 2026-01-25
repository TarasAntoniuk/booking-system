package com.tarasantoniuk.unit.dto;

import com.tarasantoniuk.unit.enums.AccommodationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new accommodation unit")
public class CreateUnitRequestDto {

    @Schema(description = "Number of rooms in the unit", example = "2", minimum = "1", maximum = "20")
    @NotNull(message = "Number of rooms is required")
    @Min(value = 1, message = "Number of rooms must be at least 1")
    @Max(value = 20, message = "Number of rooms cannot exceed 20")
    private Integer numberOfRooms;

    @Schema(description = "Type of accommodation", example = "FLAT", allowableValues = {"HOME", "FLAT", "APARTMENTS"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Accommodation type is required")
    private AccommodationType accommodationType;

    @Schema(description = "Floor number (0 for ground floor, negative for basement)", example = "3")
    @NotNull(message = "Floor is required")
    @Min(value = -5, message = "Floor cannot be below -5")
    @Max(value = 100, message = "Floor cannot exceed 100")
    private Integer floor;

    @Schema(description = "Base cost per night in USD", example = "100.00", minimum = "1")
    @NotNull(message = "Base cost is required")
    @DecimalMin(value = "1.00", message = "Base cost must be at least 1.00")
    @DecimalMax(value = "100000.00", message = "Base cost cannot exceed 100000.00")
    private BigDecimal baseCost;

    @Schema(description = "Description of the unit", example = "Cozy 2-room apartment in city center with balcony", maxLength = 1000)
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Schema(description = "ID of the unit owner (user)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Owner ID is required")
    private Long ownerId;
}
