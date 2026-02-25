package com.quickstack.branch.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Area entity business logic.
 */
@DisplayName("Area entity")
class AreaEntityTest {

    @Test
    @DisplayName("1. New area has correct default values")
    void newAreaHasDefaultValues() {
        Area area = new Area();

        assertThat(area.isActive()).isTrue();
        assertThat(area.getSortOrder()).isEqualTo(0);
        assertThat(area.isDeleted()).isFalse();
        assertThat(area.getDeletedAt()).isNull();
        assertThat(area.getDeletedBy()).isNull();
    }

    @Test
    @DisplayName("2. softDelete sets deletedAt, deletedBy, and deactivates")
    void softDeleteSetsFieldsCorrectly() {
        Area area = new Area();
        area.setId(UUID.randomUUID());
        UUID deletedBy = UUID.randomUUID();

        area.softDelete(deletedBy);

        assertThat(area.isDeleted()).isTrue();
        assertThat(area.getDeletedAt()).isNotNull();
        assertThat(area.getDeletedBy()).isEqualTo(deletedBy);
        assertThat(area.isActive()).isFalse();
    }

    @Test
    @DisplayName("3. isDeleted returns true when deletedAt is set")
    void isDeletedReturnsTrueWhenDeleted() {
        Area area = new Area();
        area.softDelete(UUID.randomUUID());
        assertThat(area.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("4. isDeleted returns false for a new area")
    void isDeletedReturnsFalseForNewArea() {
        Area area = new Area();
        area.setName("Terraza");
        area.setTenantId(UUID.randomUUID());
        area.setBranchId(UUID.randomUUID());

        assertThat(area.isDeleted()).isFalse();
    }
}
