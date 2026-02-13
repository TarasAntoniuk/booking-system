package com.tarasantoniuk.unit.controller;

import com.tarasantoniuk.unit.dto.CreateUnitRequestDto;
import com.tarasantoniuk.unit.dto.UnitResponseDto;
import com.tarasantoniuk.unit.dto.UnitSearchCriteriaDto;
import com.tarasantoniuk.unit.service.UnitService;
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
@RequestMapping("/api/v1/units")
@RequiredArgsConstructor
@Tag(name = "Units", description = "Accommodation unit management API - create, view and search units")
public class UnitController {

    private static final int MAX_PAGE_SIZE = 100;

    private final UnitService unitService;

    @PostMapping
    @Operation(
            summary = "Create a new unit",
            description = "Creates a new accommodation unit. Total cost is automatically calculated as base cost + 15% markup."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Unit created successfully",
                    content = @Content(schema = @Schema(implementation = UnitResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Owner (user) not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<UnitResponseDto> createUnit(
            @Valid @RequestBody CreateUnitRequestDto request
    ) {
        UnitResponseDto response = unitService.createUnit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get unit by ID",
            description = "Retrieves detailed information about a specific accommodation unit"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unit found",
                    content = @Content(schema = @Schema(implementation = UnitResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Unit not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<UnitResponseDto> getUnitById(
            @Parameter(description = "Unit ID", example = "1")
            @PathVariable Long id
    ) {
        UnitResponseDto response = unitService.getUnitById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "Get all units with pagination",
            description = "Retrieves a paginated list of all accommodation units with optional sorting"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of units retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<UnitResponseDto>> getAllUnits(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Field to sort by (id, baseCost, numberOfRooms, floor)", example = "id")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction", example = "asc", schema = @Schema(allowableValues = {"asc", "desc"}))
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, safeSize, sort);

        Page<UnitResponseDto> units = unitService.getAllUnits(pageable);
        return ResponseEntity.ok(units);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search units by criteria",
            description = "Search for accommodation units using various filter criteria. " +
                    "All criteria are optional. When startDate and endDate are provided, " +
                    "only units available for booking during that period are returned."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<UnitResponseDto>> searchUnits(
            @Valid @ModelAttribute UnitSearchCriteriaDto criteria,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Field to sort by", example = "baseCost")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction", example = "asc", schema = @Schema(allowableValues = {"asc", "desc"}))
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, safeSize, sort);

        Page<UnitResponseDto> units = unitService.searchUnits(criteria, pageable);
        return ResponseEntity.ok(units);
    }
}
