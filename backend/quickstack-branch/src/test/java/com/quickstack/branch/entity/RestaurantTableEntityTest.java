package com.quickstack.branch.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RestaurantTable entity business logic.
 */
@DisplayName("RestaurantTable entity")
class RestaurantTableEntityTest {

    @Test
    @DisplayName("1. New table has correct default values")
    void newTableHasDefaultValues() {
        RestaurantTable table = new RestaurantTable();

        assertThat(table.isActive()).isTrue();
        assertThat(table.getSortOrder()).isEqualTo(0);
        assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);
        assertThat(table.isDeleted()).isFalse();
        assertThat(table.getDeletedAt()).isNull();
        assertThat(table.getDeletedBy()).isNull();
    }

    @Test
    @DisplayName("2. softDelete sets deletedAt, deletedBy, and deactivates")
    void softDeleteSetsFieldsCorrectly() {
        RestaurantTable table = new RestaurantTable();
        table.setId(UUID.randomUUID());
        UUID deletedBy = UUID.randomUUID();

        table.softDelete(deletedBy);

        assertThat(table.isDeleted()).isTrue();
        assertThat(table.getDeletedAt()).isNotNull();
        assertThat(table.getDeletedBy()).isEqualTo(deletedBy);
        assertThat(table.isActive()).isFalse();
    }

    @Test
    @DisplayName("3. isDeleted returns true when deletedAt is set")
    void isDeletedReturnsTrueWhenDeleted() {
        RestaurantTable table = new RestaurantTable();
        table.softDelete(UUID.randomUUID());
        assertThat(table.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("4. TableStatus enum has all expected values")
    void tableStatusEnumHasExpectedValues() {
        assertThat(TableStatus.values()).containsExactlyInAnyOrder(
                TableStatus.AVAILABLE,
                TableStatus.OCCUPIED,
                TableStatus.RESERVED,
                TableStatus.MAINTENANCE
        );
    }

    @Test
    @DisplayName("5. Status can be changed from AVAILABLE to OCCUPIED")
    void statusCanBeChanged() {
        RestaurantTable table = new RestaurantTable();

        assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);

        table.setStatus(TableStatus.OCCUPIED);
        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);

        table.setStatus(TableStatus.RESERVED);
        assertThat(table.getStatus()).isEqualTo(TableStatus.RESERVED);

        table.setStatus(TableStatus.MAINTENANCE);
        assertThat(table.getStatus()).isEqualTo(TableStatus.MAINTENANCE);
    }
}
