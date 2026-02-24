package com.quickstack.product.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Combo Entity")
class ComboTest {

    @Test
    @DisplayName("New combo is not deleted by default")
    void newComboIsNotDeleted() {
        Combo combo = new Combo();
        assertThat(combo.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("isDeleted returns true when deletedAt is set")
    void isDeletedReturnsTrueWhenDeletedAtIsSet() {
        Combo combo = new Combo();
        combo.setDeletedAt(Instant.now());
        assertThat(combo.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("isDeleted returns false when deletedAt is cleared")
    void isDeletedReturnsFalseWhenDeletedAtIsCleared() {
        Combo combo = new Combo();
        combo.setDeletedAt(Instant.now());
        combo.setDeletedAt(null);
        assertThat(combo.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("New combo has isActive = true by default")
    void newComboIsActiveByDefault() {
        Combo combo = new Combo();
        assertThat(combo.isActive()).isTrue();
    }

    @Test
    @DisplayName("New combo has sortOrder = 0 by default")
    void newComboHasSortOrderZeroByDefault() {
        Combo combo = new Combo();
        assertThat(combo.getSortOrder()).isEqualTo(0);
    }

    @Test
    @DisplayName("Can set and get all fields")
    void canSetAndGetAllFields() {
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal price = new BigDecimal("99.00");

        Combo combo = new Combo();
        combo.setId(id);
        combo.setTenantId(tenantId);
        combo.setName("Combo 1");
        combo.setDescription("Hamburguesa + Refresco");
        combo.setImageUrl("https://example.com/combo1.jpg");
        combo.setPrice(price);
        combo.setActive(false);
        combo.setSortOrder(5);
        combo.setCreatedBy(userId);
        combo.setUpdatedBy(userId);

        assertThat(combo.getId()).isEqualTo(id);
        assertThat(combo.getTenantId()).isEqualTo(tenantId);
        assertThat(combo.getName()).isEqualTo("Combo 1");
        assertThat(combo.getDescription()).isEqualTo("Hamburguesa + Refresco");
        assertThat(combo.getImageUrl()).isEqualTo("https://example.com/combo1.jpg");
        assertThat(combo.getPrice()).isEqualByComparingTo(price);
        assertThat(combo.isActive()).isFalse();
        assertThat(combo.getSortOrder()).isEqualTo(5);
        assertThat(combo.getCreatedBy()).isEqualTo(userId);
        assertThat(combo.getUpdatedBy()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Soft delete sets deletedAt and deletedBy")
    void softDeleteSetsFields() {
        UUID deletedByUserId = UUID.randomUUID();
        Instant now = Instant.now();

        Combo combo = new Combo();
        combo.setDeletedAt(now);
        combo.setDeletedBy(deletedByUserId);

        assertThat(combo.isDeleted()).isTrue();
        assertThat(combo.getDeletedAt()).isEqualTo(now);
        assertThat(combo.getDeletedBy()).isEqualTo(deletedByUserId);
    }

    @Test
    @DisplayName("Items list is initialized as empty by default")
    void itemsListIsEmptyByDefault() {
        Combo combo = new Combo();
        assertThat(combo.getItems()).isNotNull();
        assertThat(combo.getItems()).isEmpty();
    }
}
