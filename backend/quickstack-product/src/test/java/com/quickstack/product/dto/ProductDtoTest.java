package com.quickstack.product.dto;

import com.quickstack.product.dto.request.ProductCreateRequest;
import com.quickstack.product.entity.ProductType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("ProductCreateRequest should be valid with all mandatory fields")
    void shouldBeValidWithMandatoryFields() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Valid Product",
                null,
                UUID.randomUUID(),
                "VALID-SKU-123",
                new BigDecimal("10.50"),
                null,
                null,
                ProductType.SIMPLE,
                0,
                null
        );

        Set<ConstraintViolation<ProductCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ProductCreateRequest should fail when name is blank")
    void shouldFailWhenNameIsBlank() {
        ProductCreateRequest request = new ProductCreateRequest(
                "",
                null,
                UUID.randomUUID(),
                null,
                BigDecimal.TEN,
                null,
                null,
                ProductType.SIMPLE,
                0,
                null
        );

        Set<ConstraintViolation<ProductCreateRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("NAME_REQUIRED");
    }

    @Test
    @DisplayName("ProductCreateRequest should fail when categoryId is null")
    void shouldFailWhenCategoryIdIsNull() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Product",
                null,
                null, // null categoryId
                null,
                BigDecimal.TEN,
                null,
                null,
                ProductType.SIMPLE,
                0,
                null
        );

        Set<ConstraintViolation<ProductCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("CATEGORY_REQUIRED"));
    }

    @Test
    @DisplayName("ProductCreateRequest should fail when basePrice is null or negative")
    void shouldFailWhenBasePriceIsInvalid() {
        // Null price
        ProductCreateRequest nullPrice = new ProductCreateRequest(
                "Product",
                null,
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                ProductType.SIMPLE,
                0,
                null
        );
        assertThat(validator.validate(nullPrice))
                .anyMatch(v -> v.getMessage().equals("BASE_PRICE_REQUIRED"));

        // Negative price
        ProductCreateRequest negativePrice = new ProductCreateRequest(
                "Product",
                null,
                UUID.randomUUID(),
                null,
                new BigDecimal("-1.00"),
                null,
                null,
                ProductType.SIMPLE,
                0,
                null
        );
        assertThat(validator.validate(negativePrice))
                .anyMatch(v -> v.getMessage().equals("INVALID_PRICE"));
    }

    @Test
    @DisplayName("ProductCreateRequest should validate SKU format")
    void shouldValidateSkuFormat() {
        ProductCreateRequest invalidSku = new ProductCreateRequest(
                "Product",
                null,
                UUID.randomUUID(),
                "invalid sku with spaces", // Spaces not allowed based on regex
                BigDecimal.TEN,
                null,
                null,
                ProductType.SIMPLE,
                0,
                null
        );

        Set<ConstraintViolation<ProductCreateRequest>> violations = validator.validate(invalidSku);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("INVALID_SKU_FORMAT"));
    }

    @Test
    @DisplayName("ProductCreateRequest should validate image URL format")
    void shouldValidateImageUrl() {
        ProductCreateRequest invalidUrl = new ProductCreateRequest(
                "Product",
                null,
                UUID.randomUUID(),
                null,
                BigDecimal.TEN,
                null,
                "not-a-url",
                ProductType.SIMPLE,
                0,
                null
        );

        Set<ConstraintViolation<ProductCreateRequest>> violations = validator.validate(invalidUrl);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("INVALID_IMAGE_URL"));
    }
}
