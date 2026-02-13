package com.tarasantoniuk.booking.controller;

import com.tarasantoniuk.booking.dto.BookingResponseDto;
import com.tarasantoniuk.booking.dto.CreateBookingRequestDto;
import com.tarasantoniuk.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management API - create, view and cancel accommodation bookings")
public class BookingController {

    private static final int MAX_PAGE_SIZE = 100;

    private final BookingService bookingService;

    @PostMapping
    @Operation(
            summary = "Create a new booking",
            description = "Creates a new booking with automatic 15-minute payment window. " +
                    "Booking status will be PENDING until payment is processed. " +
                    "If payment is not received within 15 minutes, booking will be automatically cancelled."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created successfully",
                    content = @Content(schema = @Schema(implementation = BookingResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data or dates"),
            @ApiResponse(responseCode = "404", description = "Unit or user not found"),
            @ApiResponse(responseCode = "409", description = "Unit not available for selected dates (already booked)"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BookingResponseDto> createBooking(
            @Valid @RequestBody CreateBookingRequestDto request
    ) {
        BookingResponseDto response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get booking by ID",
            description = "Retrieves detailed information about a specific booking including status, dates, and costs"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking found",
                    content = @Content(schema = @Schema(implementation = BookingResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BookingResponseDto> getBookingById(
            @Parameter(description = "Booking ID", example = "1")
            @PathVariable Long id
    ) {
        BookingResponseDto response = bookingService.getBookingById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get all bookings for a user",
            description = "Retrieves paginated bookings (PENDING, CONFIRMED, CANCELLED) made by a specific user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of bookings retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<BookingResponseDto>> getUserBookings(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long userId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Field to sort by", example = "id")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction", example = "asc", schema = @Schema(allowableValues = {"asc", "desc"}))
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, safeSize, sort);

        Page<BookingResponseDto> bookings = bookingService.getUserBookings(userId, pageable);
        return ResponseEntity.ok(bookings);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel a booking",
            description = "Cancels an existing booking. Only the user who made the booking can cancel it. " +
                    "Confirmed bookings may have cancellation restrictions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Booking cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or booking cannot be cancelled"),
            @ApiResponse(responseCode = "403", description = "User not authorized to cancel this booking"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> cancelBooking(
            @Parameter(description = "Booking ID to cancel", example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID of the user requesting cancellation", example = "1")
            @RequestParam Long userId
    ) {
        bookingService.cancelBooking(id, userId);
        return ResponseEntity.noContent().build();
    }
}
