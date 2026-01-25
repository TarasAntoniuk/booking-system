package com.tarasantoniuk.payment.dto;

import com.tarasantoniuk.payment.entity.Payment;
import com.tarasantoniuk.payment.enums.PaymentStatus;
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
@Schema(description = "Payment details response")
public class PaymentResponseDto {

    @Schema(description = "Unique payment identifier", example = "1")
    private Long id;

    @Schema(description = "ID of the associated booking", example = "1")
    private Long bookingId;

    @Schema(description = "Payment amount in USD", example = "460.00")
    private BigDecimal amount;

    @Schema(description = "Current payment status", example = "COMPLETED", allowableValues = {"PENDING", "COMPLETED", "FAILED"})
    private PaymentStatus status;

    @Schema(description = "Timestamp when payment was processed", example = "2026-01-25T10:35:00")
    private LocalDateTime createdAt;

    public static PaymentResponseDto from(Payment payment) {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setId(payment.getId());
        response.setBookingId(payment.getBooking().getId());
        response.setAmount(payment.getAmount());
        response.setStatus(payment.getStatus());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }
}
