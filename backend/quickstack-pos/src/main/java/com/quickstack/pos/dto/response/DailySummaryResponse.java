package com.quickstack.pos.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for the daily sales summary report.
 * Contains aggregated metrics for completed orders in a branch on a given date.
 */
public record DailySummaryResponse(
        LocalDate date,
        UUID branchId,
        int totalOrders,
        BigDecimal totalSales,
        BigDecimal averageTicket,
        Map<String, Long> ordersByServiceType,
        List<TopProductEntry> topProducts) {

    /**
     * Represents a product and the total quantity sold on the report date.
     */
    public record TopProductEntry(String productName, long quantitySold) {}
}
