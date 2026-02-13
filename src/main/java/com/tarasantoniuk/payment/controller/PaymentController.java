package com.tarasantoniuk.payment.controller;

import com.tarasantoniuk.payment.dto.PaymentResponseDto;
import com.tarasantoniuk.payment.dto.ProcessPaymentRequestDto;
import com.tarasantoniuk.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing API - process and view payments for bookings")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    @Operation(
            summary = "Process payment for a booking",
            description = "Processes payment for a PENDING booking. Payment must be made within 15 minutes " +
                    "of booking creation. Successful payment changes booking status to CONFIRMED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment processed successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or booking already paid/cancelled"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "408", description = "Payment window expired (booking was auto-cancelled)"),
            @ApiResponse(responseCode = "500", description = "Internal server error or payment processing failed")
    })
    public ResponseEntity<PaymentResponseDto> processPayment(
            @Valid @RequestBody ProcessPaymentRequestDto request
    ) {
        PaymentResponseDto response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(
            summary = "Get payment by booking ID",
            description = "Retrieves payment information for a specific booking"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Payment not found for this booking"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PaymentResponseDto> getPaymentByBookingId(
            @Parameter(description = "Booking ID", example = "1")
            @PathVariable Long bookingId
    ) {
        PaymentResponseDto response = paymentService.getPaymentByBookingId(bookingId);
        return ResponseEntity.ok(response);
    }
}
