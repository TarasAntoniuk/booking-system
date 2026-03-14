package com.tarasantoniuk.user.controller;

import com.tarasantoniuk.user.dto.UserRequestDto;
import com.tarasantoniuk.user.dto.UserResponseDto;
import com.tarasantoniuk.user.service.InfoService;
import com.tarasantoniuk.user.service.UserService;
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
@RequestMapping("/api/v1/tests")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management API - create and view users")
public class InfoControllerTest {

    private static final int MAX_PAGE_SIZE = 100;

    private final InfoService infoService;

    @GetMapping("/{someString}")
    @Operation(
            summary = "Get some information by ID",
            description = "Retrieves some information by unique identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Info found",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Info not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> getInfoById(
            @Parameter(description = "Some string", example = "asdffdsa")
            @PathVariable String someString
    ) {
        String response = infoService.getTestResult(someString);
        return ResponseEntity.ok(response);
    }

}
