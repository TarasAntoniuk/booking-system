package com.tarasantoniuk.booking.dto;

import com.tarasantoniuk.booking.entity.Booking;
import com.tarasantoniuk.booking.enums.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Booking details response")
public class BookingResponseDto {

    @Schema(description = "Unique booking identifier", example = "1")
    private Long id;

    @Schema(description = "ID of the booked accommodation unit", example = "1")
    private Long unitId;

    @Schema(description = "ID of the user who made the booking", example = "1")
    private Long userId;

    @Schema(description = "Booking start date", example = "2026-02-01")
    private LocalDate startDate;

    @Schema(description = "Booking end date", example = "2026-02-05")
    private LocalDate endDate;

    @Schema(description = "Current booking status", example = "PENDING", allowableValues = {"PENDING", "CONFIRMED", "CANCELLED"})
    private BookingStatus status;

    @Schema(description = "Timestamp when booking was created", example = "2026-01-25T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Payment deadline for PENDING bookings (15 minutes from creation)", example = "2026-01-25T10:45:00")
    private LocalDateTime expiresAt;

    @Schema(description = "Total cost for the booking period (base cost * nights + 15% markup)", example = "460.00")
    private BigDecimal totalCost;

    public static BookingResponseDto from(Booking booking, BigDecimal totalCost) {
        BookingResponseDto response = new BookingResponseDto();
        response.setId(booking.getId());
        response.setUnitId(booking.getUnit().getId());
        response.setUserId(booking.getUser().getId());
        response.setStartDate(booking.getStartDate());
        response.setEndDate(booking.getEndDate());
        response.setStatus(booking.getStatus());
        response.setCreatedAt(booking.getCreatedAt());
        response.setExpiresAt(booking.getExpiresAt());
        response.setTotalCost(totalCost);
        return response;
    }
}
