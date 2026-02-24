package com.quickstack.product.dto;

import com.quickstack.product.dto.request.ComboCreateRequest;
import com.quickstack.product.dto.request.ComboItemRequest;
import com.quickstack.product.dto.request.ComboUpdateRequest;
import com.quickstack.product.dto.response.ComboItemResponse;
import com.quickstack.product.dto.response.ComboResponse;
import com.quickstack.product.entity.Combo;
import com.quickstack.product.entity.ComboItem;
import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Combo DTOs")
class ComboDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // -------------------------------------------------------------------------
    // ComboItemRequest
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ComboItemRequest")
    class ComboItemRequestTests {

        @Test
        @DisplayName("Valid request passes validation")
        void validRequestPassesValidation() {
            ComboItemRequest request = new ComboItemRequest(UUID.randomUUID(), 2, null, null, null);
            Set<ConstraintViolation<ComboItemRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Null productId fails validation")
        void nullProductIdFailsValidation() {
            ComboItemRequest request = new ComboItemRequest(null, 1, null, null, null);
            Set<ConstraintViolation<ComboItemRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("productId"));
        }

        @Test
        @DisplayName("Quantity 0 fails validation")
        void quantityZeroFailsValidation() {
            ComboItemRequest request = new ComboItemRequest(UUID.randomUUID(), 0, null, null, null);
            Set<ConstraintViolation<ComboItemRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("quantity"));
        }

        @Test
        @DisplayName("effectiveAllowSubstitutes returns false when null")
        void effectiveAllowSubstitutesReturnsFalseWhenNull() {
            ComboItemRequest request = new ComboItemRequest(UUID.randomUUID(), 1, null, null, null);
            assertThat(request.effectiveAllowSubstitutes()).isFalse();
        }

        @Test
        @DisplayName("effectiveSortOrder returns 0 when null")
        void effectiveSortOrderReturnsZeroWhenNull() {
            ComboItemRequest request = new ComboItemRequest(UUID.randomUUID(), 1, null, null, null);
            assertThat(request.effectiveSortOrder()).isEqualTo(0);
        }
    }

    // -------------------------------------------------------------------------
    // ComboCreateRequest
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ComboCreateRequest")
    class ComboCreateRequestTests {

        @Test
        @DisplayName("Valid request with 2 items passes validation")
        void validRequestPassesValidation() {
            List<ComboItemRequest> items = List.of(
                    new ComboItemRequest(UUID.randomUUID(), 1, null, null, null),
                    new ComboItemRequest(UUID.randomUUID(), 1, null, null, null)
            );
            ComboCreateRequest request = new ComboCreateRequest(
                    "Combo 1", null, null, new BigDecimal("99.00"), items, null);
            Set<ConstraintViolation<ComboCreateRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Blank name fails validation")
        void blankNameFailsValidation() {
            List<ComboItemRequest> items = List.of(
                    new ComboItemRequest(UUID.randomUUID(), 1, null, null, null),
                    new ComboItemRequest(UUID.randomUUID(), 1, null, null, null)
            );
            ComboCreateRequest request = new ComboCreateRequest(
                    "", null, null, new BigDecimal("99.00"), items, null);
            Set<ConstraintViolation<ComboCreateRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        @DisplayName("Only 1 item fails validation")
        void onlyOneItemFailsValidation() {
            List<ComboItemRequest> items = List.of(
                    new ComboItemRequest(UUID.randomUUID(), 1, null, null, null)
            );
            ComboCreateRequest request = new ComboCreateRequest(
                    "Combo 1", null, null, new BigDecimal("99.00"), items, null);
            Set<ConstraintViolation<ComboCreateRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("items"));
        }

        @Test
        @DisplayName("Negative price fails validation")
        void negativePriceFailsValidation() {
            List<ComboItemRequest> items = List.of(
                    new ComboItemRequest(UUID.randomUUID(), 1, null, null, null),
                    new ComboItemRequest(UUID.randomUUID(), 1, null, null, null)
            );
            ComboCreateRequest request = new ComboCreateRequest(
                    "Combo 1", null, null, new BigDecimal("-1.00"), items, null);
            Set<ConstraintViolation<ComboCreateRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("price"));
        }

        @Test
        @DisplayName("effectiveSortOrder defaults to 0 when null")
        void effectiveSortOrderDefaultsToZero() {
            List<ComboItemRequest> items = List.of(
                    new ComboItemRequest(UUID.randomUUID(), 1, null, null, null),
                    new ComboItemRequest(UUID.randomUUID(), 1, null, null, null)
            );
            ComboCreateRequest request = new ComboCreateRequest(
                    "Combo", null, null, BigDecimal.TEN, items, null);
            assertThat(request.effectiveSortOrder()).isEqualTo(0);
        }
    }

    // -------------------------------------------------------------------------
    // ComboUpdateRequest
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ComboUpdateRequest")
    class ComboUpdateRequestTests {

        @Test
        @DisplayName("All null fields are valid (partial update)")
        void allNullFieldsAreValid() {
            ComboUpdateRequest request = new ComboUpdateRequest(null, null, null, null, null, null, null);
            Set<ConstraintViolation<ComboUpdateRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Single item update fails validation (requires min 2)")
        void singleItemUpdateFailsValidation() {
            List<ComboItemRequest> items = List.of(
                    new ComboItemRequest(UUID.randomUUID(), 1, null, null, null)
            );
            ComboUpdateRequest request = new ComboUpdateRequest(null, null, null, null, null, items, null);
            Set<ConstraintViolation<ComboUpdateRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("items"));
        }
    }

    // -------------------------------------------------------------------------
    // ComboItemResponse.from()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ComboItemResponse.from()")
    class ComboItemResponseTests {

        @Test
        @DisplayName("Maps ComboItem and Product correctly")
        void mapsCorrectly() {
            UUID productId = UUID.randomUUID();
            ComboItem item = new ComboItem();
            item.setId(UUID.randomUUID());
            item.setProductId(productId);
            item.setQuantity(2);
            item.setAllowSubstitutes(true);
            item.setSubstituteGroup("DRINKS");
            item.setSortOrder(0);

            Product product = new Product();
            product.setId(productId);
            product.setName("Refresco");

            ComboItemResponse response = ComboItemResponse.from(item, product);

            assertThat(response.productId()).isEqualTo(productId);
            assertThat(response.productName()).isEqualTo("Refresco");
            assertThat(response.quantity()).isEqualTo(2);
            assertThat(response.allowSubstitutes()).isTrue();
            assertThat(response.substituteGroup()).isEqualTo("DRINKS");
        }

        @Test
        @DisplayName("Handles null product gracefully")
        void handlesNullProduct() {
            ComboItem item = new ComboItem();
            item.setId(UUID.randomUUID());
            item.setProductId(UUID.randomUUID());
            item.setQuantity(1);

            ComboItemResponse response = ComboItemResponse.from(item, null);

            assertThat(response.productName()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // ComboResponse.from()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ComboResponse.from()")
    class ComboResponseTests {

        @Test
        @DisplayName("Maps Combo entity with items correctly")
        void mapsComboWithItems() {
            UUID tenantId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            Combo combo = new Combo();
            combo.setId(UUID.randomUUID());
            combo.setTenantId(tenantId);
            combo.setName("Combo 1");
            combo.setPrice(new BigDecimal("99.00"));
            combo.setActive(true);
            combo.setSortOrder(1);

            ComboItem item = new ComboItem();
            item.setId(UUID.randomUUID());
            item.setProductId(productId);
            item.setQuantity(1);

            Product product = new Product();
            product.setId(productId);
            product.setName("Hamburguesa");
            product.setProductType(ProductType.SIMPLE);

            ComboResponse response = ComboResponse.from(combo, List.of(item), Map.of(productId, product));

            assertThat(response.name()).isEqualTo("Combo 1");
            assertThat(response.price()).isEqualByComparingTo("99.00");
            assertThat(response.isActive()).isTrue();
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).productName()).isEqualTo("Hamburguesa");
        }

        @Test
        @DisplayName("Maps Combo with empty items list")
        void mapsComboWithNoItems() {
            Combo combo = new Combo();
            combo.setId(UUID.randomUUID());
            combo.setTenantId(UUID.randomUUID());
            combo.setName("Combo Vacío");
            combo.setPrice(BigDecimal.ZERO);

            ComboResponse response = ComboResponse.from(combo, List.of(), Map.of());

            assertThat(response.items()).isEmpty();
        }
    }
}
