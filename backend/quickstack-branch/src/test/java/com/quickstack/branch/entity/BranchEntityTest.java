package com.quickstack.branch.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Branch entity business logic.
 */
@DisplayName("Branch entity")
class BranchEntityTest {

    @Test
    @DisplayName("1. New branch has correct default values")
    void newBranchHasDefaultValues() {
        Branch branch = new Branch();

        assertThat(branch.isActive()).isTrue();
        assertThat(branch.getSettings()).isEqualTo("{}");
        assertThat(branch.isDeleted()).isFalse();
        assertThat(branch.getDeletedAt()).isNull();
        assertThat(branch.getDeletedBy()).isNull();
    }

    @Test
    @DisplayName("2. softDelete sets deletedAt, deletedBy, and deactivates")
    void softDeleteSetsFieldsCorrectly() {
        Branch branch = new Branch();
        branch.setId(UUID.randomUUID());
        UUID deletedBy = UUID.randomUUID();

        branch.softDelete(deletedBy);

        assertThat(branch.isDeleted()).isTrue();
        assertThat(branch.getDeletedAt()).isNotNull();
        assertThat(branch.getDeletedBy()).isEqualTo(deletedBy);
        assertThat(branch.isActive()).isFalse();
    }

    @Test
    @DisplayName("3. isDeleted returns true when deletedAt is set")
    void isDeletedReturnsTrueWhenDeletedAtIsSet() {
        Branch branch = new Branch();
        assertThat(branch.isDeleted()).isFalse();

        branch.softDelete(UUID.randomUUID());

        assertThat(branch.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("4. isDeleted returns false when deletedAt is null")
    void isDeletedReturnsFalseWhenNotDeleted() {
        Branch branch = new Branch();
        branch.setName("Sucursal Centro");
        branch.setTenantId(UUID.randomUUID());

        assertThat(branch.isDeleted()).isFalse();
    }
}
