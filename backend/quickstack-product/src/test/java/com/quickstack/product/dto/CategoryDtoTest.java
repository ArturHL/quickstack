package com.quickstack.product.dto;

import com.quickstack.product.dto.request.CategoryCreateRequest;
import com.quickstack.product.dto.request.CategoryUpdateRequest;
import com.quickstack.product.dto.response.CategoryResponse;
import com.quickstack.product.dto.response.CategorySummaryResponse;
import com.quickstack.product.entity.Category;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Category DTOs")
class CategoryDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // -------------------------------------------------------------------------
    // CategoryCreateRequest
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("CategoryCreateRequest")
    class CategoryCreateRequestTests {

        @Test
        @DisplayName("should be valid with only required name")
        void shouldBeValidWithOnlyName() {
            CategoryCreateRequest request = new CategoryCreateRequest(
                "Bebidas", null, null, null, null);

            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("should fail validation when name is blank")
        void shouldFailWhenNameIsBlank() {
            CategoryCreateRequest request = new CategoryCreateRequest(
                "", null, null, null, null);

            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        @DisplayName("should fail validation when name exceeds 255 characters")
        void shouldFailWhenNameExceeds255() {
            String longName = "A".repeat(256);
            CategoryCreateRequest request = new CategoryCreateRequest(
                longName, null, null, null, null);

            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        @DisplayName("should default sortOrder to 0 when null")
        void shouldDefaultSortOrderToZero() {
            CategoryCreateRequest request = new CategoryCreateRequest(
                "Bebidas", null, null, null, null);

            assertThat(request.effectiveSortOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("should use provided sortOrder when present")
        void shouldUseProvidedSortOrder() {
            CategoryCreateRequest request = new CategoryCreateRequest(
                "Bebidas", null, null, null, 5);

            assertThat(request.effectiveSortOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("should fail validation when imageUrl is not a valid URL")
        void shouldFailWhenImageUrlIsInvalid() {
            CategoryCreateRequest request = new CategoryCreateRequest(
                "Bebidas", null, "not-a-url", null, null);

            Set<ConstraintViolation<CategoryCreateRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("imageUrl"));
        }
    }

    // -------------------------------------------------------------------------
    // CategoryResponse
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("CategoryResponse")
    class CategoryResponseTests {

        @Test
        @DisplayName("should map all entity fields correctly")
        void shouldMapEntityFieldsCorrectly() {
            UUID id = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();
            UUID parentId = UUID.randomUUID();
            UUID createdBy = UUID.randomUUID();

            Category category = new Category();
            category.setId(id);
            category.setTenantId(tenantId);
            category.setParentId(parentId);
            category.setName("Bebidas");
            category.setDescription("Todo tipo de bebidas");
            category.setImageUrl("https://example.com/image.png");
            category.setSortOrder(3);
            category.setActive(true);
            category.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
            category.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));
            category.setCreatedBy(createdBy);

            CategoryResponse response = CategoryResponse.from(category, 5);

            assertThat(response.id()).isEqualTo(id);
            assertThat(response.tenantId()).isEqualTo(tenantId);
            assertThat(response.parentId()).isEqualTo(parentId);
            assertThat(response.name()).isEqualTo("Bebidas");
            assertThat(response.description()).isEqualTo("Todo tipo de bebidas");
            assertThat(response.imageUrl()).isEqualTo("https://example.com/image.png");
            assertThat(response.sortOrder()).isEqualTo(3);
            assertThat(response.isActive()).isTrue();
            assertThat(response.productCount()).isEqualTo(5);
            assertThat(response.createdBy()).isEqualTo(createdBy);
        }
    }

    // -------------------------------------------------------------------------
    // CategorySummaryResponse
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("CategorySummaryResponse")
    class CategorySummaryResponseTests {

        @Test
        @DisplayName("should map only summary fields from entity")
        void shouldMapSummaryFields() {
            UUID id = UUID.randomUUID();
            UUID parentId = UUID.randomUUID();

            Category category = new Category();
            category.setId(id);
            category.setName("Comidas");
            category.setSortOrder(2);
            category.setActive(false);
            category.setParentId(parentId);

            CategorySummaryResponse response = CategorySummaryResponse.from(category);

            assertThat(response.id()).isEqualTo(id);
            assertThat(response.name()).isEqualTo("Comidas");
            assertThat(response.sortOrder()).isEqualTo(2);
            assertThat(response.isActive()).isFalse();
            assertThat(response.parentId()).isEqualTo(parentId);
        }
    }
}
