package com.quickstack.pos.entity;

import java.util.List;
import java.util.UUID;

/**
 * Known UUIDs for order_status_types seeded in V7__seed_data.sql.
 * <p>
 * These constants avoid magic strings throughout the codebase and make
 * status checks self-documenting. Values must match V7 exactly.
 */
public final class OrderStatusConstants {

    public static final UUID PENDING     = UUID.fromString("d1111111-1111-1111-1111-111111111111");
    public static final UUID IN_PROGRESS = UUID.fromString("d2222222-2222-2222-2222-222222222222");
    public static final UUID READY       = UUID.fromString("d3333333-3333-3333-3333-333333333333");
    public static final UUID DELIVERED   = UUID.fromString("d4444444-4444-4444-4444-444444444444");
    public static final UUID COMPLETED   = UUID.fromString("d5555555-5555-5555-5555-555555555555");
    public static final UUID CANCELLED   = UUID.fromString("d6666666-6666-6666-6666-666666666666");

    /** Status IDs that represent end states — orders cannot be modified after reaching these. */
    public static final List<UUID> TERMINAL_STATUS_IDS = List.of(COMPLETED, CANCELLED);

    private OrderStatusConstants() {
        // Utility class — no instances
    }
}
