package com.quickstack.branch.entity;

/**
 * Represents the operational status of a restaurant table.
 * <p>
 * Status transitions:
 * - AVAILABLE   → can accept new orders (default state)
 * - OCCUPIED    → has an active open order
 * - RESERVED    → reserved in advance for a customer
 * - MAINTENANCE → table is out of service (cleaning, repair)
 * <p>
 * Status updates are made via PATCH /api/v1/tables/{id}/status.
 * The POS system automatically transitions between AVAILABLE and OCCUPIED
 * when orders are opened and closed.
 */
public enum TableStatus {
    AVAILABLE,
    OCCUPIED,
    RESERVED,
    MAINTENANCE
}
