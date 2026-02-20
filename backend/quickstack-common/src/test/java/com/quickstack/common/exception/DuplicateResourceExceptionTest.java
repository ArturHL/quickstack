package com.quickstack.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DuplicateResourceException.
 */
@DisplayName("DuplicateResourceException")
class DuplicateResourceExceptionTest {

    @Test
    @DisplayName("Should construct with resource type, field and value")
    void shouldConstructWithResourceTypeFieldAndValue() {
        // When
        DuplicateResourceException exception = new DuplicateResourceException(
            "Category",
            "name",
            "Bebidas"
        );

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getCode()).isEqualTo("DUPLICATE_NAME");
        assertThat(exception.getMessage())
            .isEqualTo("Category with name 'Bebidas' already exists");
    }

    @Test
    @DisplayName("Should format message correctly with different field names")
    void shouldFormatMessageWithDifferentFields() {
        // When
        DuplicateResourceException exception = new DuplicateResourceException(
            "Product",
            "sku",
            "TACO-001"
        );

        // Then
        assertThat(exception.getCode()).isEqualTo("DUPLICATE_SKU");
        assertThat(exception.getMessage())
            .isEqualTo("Product with sku 'TACO-001' already exists");
    }

    @Test
    @DisplayName("Should uppercase field name in error code")
    void shouldUppercaseFieldNameInCode() {
        // When
        DuplicateResourceException exception = new DuplicateResourceException(
            "Product",
            "email",
            "test@example.com"
        );

        // Then
        assertThat(exception.getCode()).isEqualTo("DUPLICATE_EMAIL");
    }

    @Test
    @DisplayName("Should extend ApiException")
    void shouldExtendApiException() {
        // When
        DuplicateResourceException exception = new DuplicateResourceException(
            "Category",
            "name",
            "Bebidas"
        );

        // Then
        assertThat(exception).isInstanceOf(ApiException.class);
    }
}
