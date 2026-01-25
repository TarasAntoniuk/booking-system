package com.tarasantoniuk.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to process payment for a booking")
public class ProcessPaymentRequestDto {

    @Schema(description = "ID of the booking to pay for", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Booking ID is required")
    private Long bookingId;
}
