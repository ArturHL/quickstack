package com.quickstack.product.dto;

import com.quickstack.product.dto.request.ModifierCreateRequest;
import com.quickstack.product.dto.request.ModifierGroupCreateRequest;
import com.quickstack.product.dto.request.ModifierGroupUpdateRequest;
import com.quickstack.product.dto.request.ModifierUpdateRequest;
import com.quickstack.product.dto.response.ModifierGroupResponse;
import com.quickstack.product.dto.response.ModifierResponse;
import com.quickstack.product.entity.Modifier;
import com.quickstack.product.entity.ModifierGroup;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Modifier and ModifierGroup DTOs.
 */
@DisplayName("Modifier DTOs")
class ModifierDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // -------------------------------------------------------------------------
    // ModifierGroupCreateRequest
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ModifierGroupCreateRequest")
    class ModifierGroupCreateRequestTests {

        @Test
        @DisplayName("Should pass validation with valid required group")
        void shouldPassWithValidRequiredGroup() {
            ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                    UUID.randomUUID(), "Tamaño de bebida", null, 1, 1, true, null);

            Set<ConstraintViolation<ModifierGroupCreateRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should pass validation with optional group (min=0, not required)")
        void shouldPassWithValidOptionalGroup() {
            ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                    UUID.randomUUID(), "Extras", "Personaliza tu orden", 0, null, false, 1);

            Set<ConstraintViolation<ModifierGroupCreateRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail when name is blank")
        void shouldFailWhenNameIsBlank() {
            ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                    UUID.randomUUID(), "", null, 0, null, false, null);

            Set<ConstraintViolation<ModifierGroupCreateRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        @DisplayName("Should fail cross-field validation when isRequired=true and minSelections=0")
        void shouldFailWhenRequiredButMinSelectionsIsZero() {
            ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                    UUID.randomUUID(), "Tamaño", null, 0, null, true, null);

            Set<ConstraintViolation<ModifierGroupCreateRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("validRequiredConfig"));
        }

        @Test
        @DisplayName("Should fail cross-field validation when maxSelections < minSelections")
        void shouldFailWhenMaxSelectionsLessThanMin() {
            ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                    UUID.randomUUID(), "Extras", null, 3, 1, false, null);

            Set<ConstraintViolation<ModifierGroupCreateRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("validSelectionRange"));
        }

        @Test
        @DisplayName("Should pass with null maxSelections (unlimited)")
        void shouldPassWithNullMaxSelections() {
            ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                    UUID.randomUUID(), "Extras", null, 0, null, false, null);

            Set<ConstraintViolation<ModifierGroupCreateRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should default sortOrder to 0 when null")
        void shouldDefaultSortOrderToZero() {
            ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                    UUID.randomUUID(), "Extras", null, 0, null, false, null);

            assertThat(request.effectiveSortOrder()).isEqualTo(0);
        }
    }

    // -------------------------------------------------------------------------
    // ModifierGroupUpdateRequest
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ModifierGroupUpdateRequest")
    class ModifierGroupUpdateRequestTests {

        @Test
        @DisplayName("Should allow all null fields")
        void shouldAllowAllNullFields() {
            ModifierGroupUpdateRequest request = new ModifierGroupUpdateRequest(
                    null, null, null, null, null, null);

            Set<ConstraintViolation<ModifierGroupUpdateRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // ModifierCreateRequest
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ModifierCreateRequest")
    class ModifierCreateRequestTests {

        @Test
        @DisplayName("Should pass validation with valid modifier")
        void shouldPassWithValidModifier() {
            ModifierCreateRequest request = new ModifierCreateRequest(
                    UUID.randomUUID(), "Extra Queso", new BigDecimal("15.00"), false, 0);

            Set<ConstraintViolation<ModifierCreateRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail when name is blank")
        void shouldFailWhenNameIsBlank() {
            ModifierCreateRequest request = new ModifierCreateRequest(
                    UUID.randomUUID(), "  ", new BigDecimal("0.00"), false, null);

            Set<ConstraintViolation<ModifierCreateRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        @DisplayName("Should fail when priceAdjustment is null")
        void shouldFailWhenPriceAdjustmentIsNull() {
            ModifierCreateRequest request = new ModifierCreateRequest(
                    UUID.randomUUID(), "Extra Queso", null, false, null);

            Set<ConstraintViolation<ModifierCreateRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("priceAdjustment"));
        }
    }

    // -------------------------------------------------------------------------
    // ModifierUpdateRequest
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ModifierUpdateRequest")
    class ModifierUpdateRequestTests {

        @Test
        @DisplayName("Should allow all null fields")
        void shouldAllowAllNullFields() {
            ModifierUpdateRequest request = new ModifierUpdateRequest(null, null, null, null, null);

            Set<ConstraintViolation<ModifierUpdateRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Response DTOs
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ModifierResponse.from()")
    class ModifierResponseTests {

        @Test
        @DisplayName("Should map Modifier entity to ModifierResponse correctly")
        void shouldMapModifierToResponse() {
            Modifier modifier = new Modifier();
            modifier.setId(UUID.randomUUID());
            modifier.setTenantId(UUID.randomUUID());
            modifier.setModifierGroupId(UUID.randomUUID());
            modifier.setName("Extra Queso");
            modifier.setPriceAdjustment(new BigDecimal("15.00"));
            modifier.setDefault(true);
            modifier.setActive(true);
            modifier.setSortOrder(1);

            ModifierResponse response = ModifierResponse.from(modifier);

            assertThat(response.id()).isEqualTo(modifier.getId());
            assertThat(response.name()).isEqualTo("Extra Queso");
            assertThat(response.priceAdjustment()).isEqualByComparingTo("15.00");
            assertThat(response.isDefault()).isTrue();
            assertThat(response.isActive()).isTrue();
            assertThat(response.sortOrder()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("ModifierGroupResponse.from()")
    class ModifierGroupResponseTests {

        @Test
        @DisplayName("Should map ModifierGroup entity to ModifierGroupResponse correctly")
        void shouldMapModifierGroupToResponse() {
            ModifierGroup group = new ModifierGroup();
            group.setId(UUID.randomUUID());
            group.setTenantId(UUID.randomUUID());
            group.setProductId(UUID.randomUUID());
            group.setName("Extras");
            group.setMinSelections(0);
            group.setMaxSelections(3);
            group.setRequired(false);
            group.setSortOrder(0);

            List<ModifierResponse> modifiers = List.of();

            ModifierGroupResponse response = ModifierGroupResponse.from(group, modifiers);

            assertThat(response.id()).isEqualTo(group.getId());
            assertThat(response.name()).isEqualTo("Extras");
            assertThat(response.minSelections()).isEqualTo(0);
            assertThat(response.maxSelections()).isEqualTo(3);
            assertThat(response.isRequired()).isFalse();
            assertThat(response.modifiers()).isEmpty();
        }
    }
}
