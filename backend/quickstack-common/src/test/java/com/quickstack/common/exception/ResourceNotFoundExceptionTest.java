package com.quickstack.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ResourceNotFoundException.
 */
@DisplayName("ResourceNotFoundException")
class ResourceNotFoundExceptionTest {

    @Test
    @DisplayName("Should construct with resource type and UUID")
    void shouldConstructWithResourceTypeAndUuid() {
        // Given
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException("Category", id);

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getCode()).isEqualTo("CATEGORY_NOT_FOUND");
        assertThat(exception.getMessage())
            .isEqualTo("Category not found: 123e4567-e89b-12d3-a456-426614174000");
    }

    @Test
    @DisplayName("Should construct with resource type and string identifier")
    void shouldConstructWithResourceTypeAndString() {
        // When
        ResourceNotFoundException exception = new ResourceNotFoundException("User", "email@example.com");

        // Then
        assertThat(exception.getCode()).isEqualTo("USER_NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo("User not found: email@example.com");
    }

    @Test
    @DisplayName("Should construct with custom message")
    void shouldConstructWithCustomMessage() {
        // When
        ResourceNotFoundException exception = new ResourceNotFoundException("Custom error message");

        // Then
        assertThat(exception.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo("Custom error message");
    }

    @Test
    @DisplayName("Should generate correct error code from resource type")
    void shouldGenerateCorrectErrorCode() {
        // When
        ResourceNotFoundException productException = new ResourceNotFoundException(
            "Product",
            UUID.randomUUID()
        );
        ResourceNotFoundException categoryException = new ResourceNotFoundException(
            "Category",
            "cat-123"
        );

        // Then
        assertThat(productException.getCode()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(categoryException.getCode()).isEqualTo("CATEGORY_NOT_FOUND");
    }
}
