package com.tarasantoniuk.unit.dto;

import com.tarasantoniuk.unit.entity.Unit;
import com.tarasantoniuk.unit.enums.AccommodationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Accommodation unit details response")
public class UnitResponseDto {

    @Schema(description = "Unique unit identifier", example = "1")
    private Long id;

    @Schema(description = "Number of rooms in the unit", example = "2")
    private Integer numberOfRooms;

    @Schema(description = "Type of accommodation", example = "FLAT", allowableValues = {"HOME", "FLAT", "APARTMENT"})
    private AccommodationType accommodationType;

    @Schema(description = "Floor number", example = "3")
    private Integer floor;

    @Schema(description = "Base cost per night in USD", example = "100.00")
    private BigDecimal baseCost;

    @Schema(description = "Total cost per night with 15% markup", example = "115.00")
    private BigDecimal totalCost;

    @Schema(description = "Description of the unit", example = "Cozy 2-room apartment in city center with balcony")
    private String description;

    @Schema(description = "ID of the unit owner", example = "1")
    private Long ownerId;

    @Schema(description = "Timestamp when unit was created", example = "2026-01-20T14:30:00")
    private LocalDateTime createdAt;

    public static UnitResponseDto from(Unit unit) {
        UnitResponseDto response = new UnitResponseDto();
        response.setId(unit.getId());
        response.setNumberOfRooms(unit.getNumberOfRooms());
        response.setAccommodationType(unit.getAccommodationType());
        response.setFloor(unit.getFloor());
        response.setBaseCost(unit.getBaseCost());
        response.setTotalCost(unit.getTotalCost());
        response.setDescription(unit.getDescription());
        response.setOwnerId(unit.getOwner() != null ? unit.getOwner().getId() : null);
        response.setCreatedAt(unit.getCreatedAt());
        return response;
    }
}
