package com.quickstack.pos.controller;

import com.quickstack.common.dto.ApiResponse;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.pos.dto.response.DailySummaryResponse;
import com.quickstack.pos.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportController using Mockito (no Spring context).
 * Verifies HTTP status codes, delegation to OrderService, and JWT principal extraction.
 * Role-based authorization (@PreAuthorize) is verified in ReportIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReportController")
class ReportControllerTest {

    @Mock
    private OrderService orderService;

    private ReportController controller;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 2, 27);

    private JwtAuthenticationPrincipal principal;

    @BeforeEach
    void setUp() {
        controller = new ReportController(orderService);
        principal = new JwtAuthenticationPrincipal(USER_ID, TENANT_ID, ROLE_ID, null, "manager@test.com");
    }

    // =========================================================================
    // GET /api/v1/reports/daily-summary
    // =========================================================================

    @Nested
    @DisplayName("getDailySummary")
    class GetDailySummaryTests {

        @Test
        @DisplayName("1. Returns 200 with daily summary")
        void returnsOkWithSummary() {
            when(orderService.getDailySummary(any(), any(), any())).thenReturn(buildSummary(TODAY));

            ResponseEntity<ApiResponse<DailySummaryResponse>> result =
                    controller.getDailySummary(principal, BRANCH_ID, TODAY);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().data()).isNotNull();
        }

        @Test
        @DisplayName("2. Delegates to service with tenantId from JWT and provided branchId")
        void delegatesWithCorrectTenantAndBranch() {
            when(orderService.getDailySummary(any(), any(), any())).thenReturn(buildSummary(TODAY));

            controller.getDailySummary(principal, BRANCH_ID, TODAY);

            verify(orderService).getDailySummary(eq(TENANT_ID), eq(BRANCH_ID), eq(TODAY));
        }

        @Test
        @DisplayName("3. Uses today's Mexico City date when date param is null")
        void usesTodayWhenDateIsNull() {
            when(orderService.getDailySummary(any(), any(), any())).thenReturn(buildSummary(LocalDate.now()));

            controller.getDailySummary(principal, BRANCH_ID, null);

            // Verify the service was called with a non-null date (today's date)
            verify(orderService).getDailySummary(eq(TENANT_ID), eq(BRANCH_ID), any(LocalDate.class));
        }

        @Test
        @DisplayName("4. Uses provided date when date param is present")
        void usesProvidedDate() {
            LocalDate specificDate = LocalDate.of(2026, 2, 1);
            when(orderService.getDailySummary(any(), any(), any())).thenReturn(buildSummary(specificDate));

            controller.getDailySummary(principal, BRANCH_ID, specificDate);

            verify(orderService).getDailySummary(eq(TENANT_ID), eq(BRANCH_ID), eq(specificDate));
        }

        @Test
        @DisplayName("5. ResourceNotFoundException propagates (cross-tenant branchId)")
        void resourceNotFoundPropagates() {
            when(orderService.getDailySummary(any(), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Branch", BRANCH_ID));

            assertThatThrownBy(() -> controller.getDailySummary(principal, BRANCH_ID, TODAY))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("6. Response body contains data from service")
        void responseBodyContainsServiceData() {
            DailySummaryResponse expected = buildSummary(TODAY);
            when(orderService.getDailySummary(any(), any(), any())).thenReturn(expected);

            ResponseEntity<ApiResponse<DailySummaryResponse>> result =
                    controller.getDailySummary(principal, BRANCH_ID, TODAY);

            DailySummaryResponse data = result.getBody().data();
            assertThat(data.totalOrders()).isEqualTo(3);
            assertThat(data.totalSales()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(data.branchId()).isEqualTo(BRANCH_ID);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private DailySummaryResponse buildSummary(LocalDate date) {
        return new DailySummaryResponse(
                date,
                BRANCH_ID,
                3,
                new BigDecimal("300.00"),
                new BigDecimal("100.00"),
                Map.of("COUNTER", 3L),
                List.of(new DailySummaryResponse.TopProductEntry("Pizza", 5L)));
    }
}
