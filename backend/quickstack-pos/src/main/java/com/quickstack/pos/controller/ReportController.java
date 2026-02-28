package com.quickstack.pos.controller;

import com.quickstack.common.dto.ApiResponse;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.pos.dto.response.DailySummaryResponse;
import com.quickstack.pos.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * REST controller for reporting endpoints.
 * <p>
 * Endpoints:
 * - GET /api/v1/reports/daily-summary — Returns daily sales summary for a branch (MANAGER+)
 * <p>
 * ASVS Compliance:
 * - V4.1: tenantId always extracted from JWT, never from request body or path
 * - V4.1: IDOR protection — cross-tenant branchId returns 404 (enforced in service layer)
 * - V4.2: @PreAuthorize restricts access to MANAGER and OWNER roles
 */
@RestController
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);
    private static final ZoneId MEXICO_CITY = ZoneId.of("America/Mexico_City");

    private final OrderService orderService;

    public ReportController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Returns the daily sales summary for a branch.
     * <p>
     * Only includes COMPLETED orders. Defaults to today (Mexico City timezone) if
     * no date is provided.
     *
     * @param principal the authenticated user
     * @param branchId  the branch to report on (required)
     * @param date      the date to report on (optional, defaults to today)
     * @return daily summary metrics
     */
    @GetMapping("/api/v1/reports/daily-summary")
    @PreAuthorize("@posPermissionEvaluator.isManagerOrAbove(authentication)")
    public ResponseEntity<ApiResponse<DailySummaryResponse>> getDailySummary(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @RequestParam UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate reportDate = date != null ? date : LocalDate.now(MEXICO_CITY);

        log.debug("Daily summary request tenant={} branchId={} date={}", principal.tenantId(), branchId, reportDate);

        DailySummaryResponse response = orderService.getDailySummary(
                principal.tenantId(), branchId, reportDate);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
