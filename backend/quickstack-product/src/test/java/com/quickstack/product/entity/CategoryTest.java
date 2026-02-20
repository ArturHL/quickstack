package com.quickstack.product.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Category entity.
 */
@DisplayName("Category Entity")
class CategoryTest {

    @Test
    @DisplayName("Should create category with all required fields")
    void shouldCreateCategoryWithRequiredFields() {
        // Given
        UUID tenantId = UUID.randomUUID();
        String name = "Bebidas";

        // When
        Category category = new Category();
        category.setTenantId(tenantId);
        category.setName(name);

        // Then
        assertThat(category.getTenantId()).isEqualTo(tenantId);
        assertThat(category.getName()).isEqualTo(name);
        assertThat(category.isActive()).isTrue(); // Default value
        assertThat(category.getSortOrder()).isZero(); // Default value
    }

    @Test
    @DisplayName("Should identify as not deleted when deletedAt is null")
    void shouldNotBeDeletedWhenDeletedAtIsNull() {
        // Given
        Category category = new Category();
        category.setDeletedAt(null);

        // When
        boolean isDeleted = category.isDeleted();

        // Then
        assertThat(isDeleted).isFalse();
    }

    @Test
    @DisplayName("Should identify as deleted when deletedAt is set")
    void shouldBeDeletedWhenDeletedAtIsSet() {
        // Given
        Category category = new Category();
        category.setDeletedAt(Instant.now());

        // When
        boolean isDeleted = category.isDeleted();

        // Then
        assertThat(isDeleted).isTrue();
    }

    @Test
    @DisplayName("Should support hierarchical structure with parentId")
    void shouldSupportHierarchicalStructure() {
        // Given
        UUID parentId = UUID.randomUUID();
        Category category = new Category();

        // When
        category.setParentId(parentId);

        // Then
        assertThat(category.getParentId()).isEqualTo(parentId);
    }

    @Test
    @DisplayName("Should store audit information")
    void shouldStoreAuditInformation() {
        // Given
        UUID createdBy = UUID.randomUUID();
        UUID updatedBy = UUID.randomUUID();
        Instant now = Instant.now();

        Category category = new Category();

        // When
        category.setCreatedBy(createdBy);
        category.setUpdatedBy(updatedBy);
        category.setCreatedAt(now);
        category.setUpdatedAt(now);

        // Then
        assertThat(category.getCreatedBy()).isEqualTo(createdBy);
        assertThat(category.getUpdatedBy()).isEqualTo(updatedBy);
        assertThat(category.getCreatedAt()).isEqualTo(now);
        assertThat(category.getUpdatedAt()).isEqualTo(now);
    }
}
