package com.quickstack.pos.entity;

/**
 * Payment method types supported in QuickStack POS.
 * <p>
 * MVP Phase 1 only supports CASH. Other methods are defined here to match
 * the database CHECK constraint but are rejected at the service level.
 */
public enum PaymentMethod {
    CASH,
    CARD,
    TRANSFER,
    OTHER
}
