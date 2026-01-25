package com.tarasantoniuk.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorResponse Unit Tests")
class ErrorResponseTest {

    @Test
    @DisplayName("Should create ErrorResponse with basic fields")
    void shouldCreateErrorResponseWithBasicFields() {
        // When
        ErrorResponse errorResponse = ErrorResponse.of(
                400,
                "Bad Request",
                "Invalid input",
                "/api/test"
        );

        // Then
        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getError()).isEqualTo("Bad Request");
        assertThat(errorResponse.getMessage()).isEqualTo("Invalid input");
        assertThat(errorResponse.getPath()).isEqualTo("/api/test");
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getTimestamp()).isBetween(
                LocalDateTime.now().minusSeconds(5),
                LocalDateTime.now().plusSeconds(5)
        );
        assertThat(errorResponse.getDetails()).isNull();
    }

    @Test
    @DisplayName("Should create ErrorResponse with details")
    void shouldCreateErrorResponseWithDetails() {
        // Given
        List<String> details = Arrays.asList("Field 'name' is required", "Field 'email' is invalid");

        // When
        ErrorResponse errorResponse = ErrorResponse.of(
                400,
                "Validation Failed",
                "Invalid request data",
                "/api/users",
                details
        );

        // Then
        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getError()).isEqualTo("Validation Failed");
        assertThat(errorResponse.getMessage()).isEqualTo("Invalid request data");
        assertThat(errorResponse.getPath()).isEqualTo("/api/users");
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getDetails()).hasSize(2);
        assertThat(errorResponse.getDetails()).containsExactly(
                "Field 'name' is required",
                "Field 'email' is invalid"
        );
    }

    @Test
    @DisplayName("Should create ErrorResponse using builder")
    void shouldCreateErrorResponseUsingBuilder() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(now)
                .status(404)
                .error("Not Found")
                .message("Resource not found")
                .path("/api/resource/123")
                .build();

        // Then
        assertThat(errorResponse.getStatus()).isEqualTo(404);
        assertThat(errorResponse.getError()).isEqualTo("Not Found");
        assertThat(errorResponse.getMessage()).isEqualTo("Resource not found");
        assertThat(errorResponse.getPath()).isEqualTo("/api/resource/123");
        assertThat(errorResponse.getTimestamp()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should create ErrorResponse with no-args constructor and setters")
    void shouldCreateErrorResponseWithSetters() {
        // When
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(500);
        errorResponse.setError("Internal Server Error");
        errorResponse.setMessage("Something went wrong");
        errorResponse.setPath("/api/error");
        errorResponse.setTimestamp(LocalDateTime.now());

        // Then
        assertThat(errorResponse.getStatus()).isEqualTo(500);
        assertThat(errorResponse.getError()).isEqualTo("Internal Server Error");
        assertThat(errorResponse.getMessage()).isEqualTo("Something went wrong");
        assertThat(errorResponse.getPath()).isEqualTo("/api/error");
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should create ErrorResponse with all args constructor")
    void shouldCreateErrorResponseWithAllArgsConstructor() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        List<String> details = Arrays.asList("Detail 1", "Detail 2");

        // When
        ErrorResponse errorResponse = new ErrorResponse(
                timestamp,
                400,
                "Bad Request",
                "Invalid data",
                "/api/test",
                details
        );

        // Then
        assertThat(errorResponse.getTimestamp()).isEqualTo(timestamp);
        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getError()).isEqualTo("Bad Request");
        assertThat(errorResponse.getMessage()).isEqualTo("Invalid data");
        assertThat(errorResponse.getPath()).isEqualTo("/api/test");
        assertThat(errorResponse.getDetails()).isEqualTo(details);
    }

    @Test
    @DisplayName("Should handle empty details list")
    void shouldHandleEmptyDetailsList() {
        // When
        ErrorResponse errorResponse = ErrorResponse.of(
                400,
                "Bad Request",
                "Invalid input",
                "/api/test",
                Arrays.asList()
        );

        // Then
        assertThat(errorResponse.getDetails()).isEmpty();
    }

    @Test
    @DisplayName("Should support equals and hashCode")
    void shouldSupportEqualsAndHashCode() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        ErrorResponse error1 = ErrorResponse.builder()
                .timestamp(timestamp)
                .status(400)
                .error("Bad Request")
                .message("Invalid input")
                .path("/api/test")
                .build();

        ErrorResponse error2 = ErrorResponse.builder()
                .timestamp(timestamp)
                .status(400)
                .error("Bad Request")
                .message("Invalid input")
                .path("/api/test")
                .build();

        // Then
        assertThat(error1).isEqualTo(error2);
        assertThat(error1.hashCode()).isEqualTo(error2.hashCode());
    }

    @Test
    @DisplayName("Should support toString")
    void shouldSupportToString() {
        // When
        ErrorResponse errorResponse = ErrorResponse.of(
                404,
                "Not Found",
                "Resource not found",
                "/api/test"
        );

        // Then
        String toString = errorResponse.toString();
        assertThat(toString).contains("404");
        assertThat(toString).contains("Not Found");
        assertThat(toString).contains("Resource not found");
        assertThat(toString).contains("/api/test");
    }
}
