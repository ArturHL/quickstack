package com.quickstack.inventory.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InventoryMovement entity")
class InventoryMovementEntityTest {

    @Test
    @DisplayName("1. InventoryMovement has no deletedAt field (immutable audit record)")
    void inventoryMovementHasNoSoftDelete() throws NoSuchFieldException {
        // InventoryMovement must NOT have a deletedAt field — it is an immutable audit log
        var fields = java.util.Arrays.stream(InventoryMovement.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .toList();

        assertThat(fields).doesNotContain("deletedAt");
        assertThat(fields).doesNotContain("deletedBy");
    }

    @Test
    @DisplayName("2. SALE_DEDUCTION has negative quantityDelta by convention")
    void saleDeductionHasNegativeQuantityDelta() {
        InventoryMovement movement = new InventoryMovement();
        movement.setMovementType(MovementType.SALE_DEDUCTION);
        movement.setQuantityDelta(new BigDecimal("-2.5"));

        assertThat(movement.getQuantityDelta()).isNegative();
        assertThat(movement.getMovementType()).isEqualTo(MovementType.SALE_DEDUCTION);
    }

    @Test
    @DisplayName("3. PURCHASE has positive quantityDelta by convention")
    void purchaseHasPositiveQuantityDelta() {
        InventoryMovement movement = new InventoryMovement();
        movement.setMovementType(MovementType.PURCHASE);
        movement.setQuantityDelta(new BigDecimal("10.0"));

        assertThat(movement.getQuantityDelta()).isPositive();
        assertThat(movement.getMovementType()).isEqualTo(MovementType.PURCHASE);
    }

    @Test
    @DisplayName("4. referenceId is nullable for manual adjustments")
    void referenceIdIsNullableForManualAdjustments() {
        InventoryMovement movement = new InventoryMovement();
        movement.setMovementType(MovementType.MANUAL_ADJUSTMENT);
        movement.setQuantityDelta(new BigDecimal("1.0"));

        assertThat(movement.getReferenceId()).isNull();
    }

    @Test
    @DisplayName("5. unitCostAtTime captures historical cost")
    void unitCostAtTimeCapturesHistoricalCost() {
        InventoryMovement movement = new InventoryMovement();
        BigDecimal historicalCost = new BigDecimal("25.5000");
        movement.setUnitCostAtTime(historicalCost);

        assertThat(movement.getUnitCostAtTime()).isEqualByComparingTo(historicalCost);
    }

    @Test
    @DisplayName("6. referenceId links SALE_DEDUCTION to its order")
    void referenceIdLinksToOrder() {
        UUID orderId = UUID.randomUUID();
        InventoryMovement movement = new InventoryMovement();
        movement.setMovementType(MovementType.SALE_DEDUCTION);
        movement.setReferenceId(orderId);

        assertThat(movement.getReferenceId()).isEqualTo(orderId);
    }
}
