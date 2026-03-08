package com.quickstack.inventory.entity;

/**
 * Types of inventory movements.
 * SALE_DEDUCTION: automatic deduction triggered by a completed payment.
 * MANUAL_ADJUSTMENT: corrective adjustment by the OWNER.
 * PURCHASE: restocking after a purchase.
 */
public enum MovementType {
    SALE_DEDUCTION,
    MANUAL_ADJUSTMENT,
    PURCHASE
}
