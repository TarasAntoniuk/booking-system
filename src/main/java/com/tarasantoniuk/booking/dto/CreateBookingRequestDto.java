package com.tarasantoniuk.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new booking")
public class CreateBookingRequestDto {

    @Schema(description = "ID of the accommodation unit to book", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Unit ID is required")
    private Long unitId;

    @Schema(description = "ID of the user making the booking", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "User ID is required")
    private Long userId;

    @Schema(description = "Booking start date", example = "2026-02-01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @Schema(description = "Booking end date (must be after start date)", example = "2026-02-05", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "End date is required")
    @FutureOrPresent(message = "End date must be today or in the future")
    private LocalDate endDate;
}
