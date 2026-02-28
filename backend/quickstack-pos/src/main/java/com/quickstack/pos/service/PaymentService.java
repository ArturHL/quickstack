package com.quickstack.pos.service;

import com.quickstack.branch.entity.TableStatus;
import com.quickstack.branch.repository.TableRepository;
import com.quickstack.common.exception.ApiException;
import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.common.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import com.quickstack.pos.dto.request.PaymentRequest;
import com.quickstack.pos.dto.response.PaymentResponse;
import com.quickstack.pos.entity.Order;
import com.quickstack.pos.entity.Payment;
import com.quickstack.pos.entity.PaymentMethod;
import com.quickstack.pos.entity.OrderStatusConstants;
import com.quickstack.pos.repository.OrderRepository;
import com.quickstack.pos.repository.PaymentRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for payment registration and order closing.
 * <p>
 * Business Rules:
 * - Only CASH payments are supported in Phase 1 MVP
 * - Payment can only be registered on orders in READY status
 * - Amount received must be >= order.total
 * - Change is calculated as: amount_received - order.total
 * - When total payments >= order.total, the order is COMPLETED and closed
 * - Closing an order releases the table (DINE_IN) and updates customer stats
 * <p>
 * ASVS Compliance:
 * - V4.1: tenantId always from JWT, never from request body
 * - V4.1: IDOR protection — cross-tenant returns 404
 * - V7: Full audit trail with created_by, order_status_history
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final TableRepository tableRepository;
    private final CustomerService customerService;
    private final EntityManager entityManager;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            TableRepository tableRepository,
            CustomerService customerService,
            EntityManager entityManager) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.tableRepository = tableRepository;
        this.customerService = customerService;
        this.entityManager = entityManager;
    }

    // -------------------------------------------------------------------------
    // Register Payment
    // -------------------------------------------------------------------------

    /**
     * Registers a cash payment for an order and closes it if fully paid.
     * <p>
     * Validates the order is in READY status and the amount is sufficient.
     * Calculates change automatically. If the sum of all payments covers
     * the order total, the order is marked COMPLETED, the table released
     * (if DINE_IN), and customer stats updated (if customer is linked).
     *
     * @param tenantId the tenant scope (from JWT)
     * @param userId   the cashier registering the payment (for audit)
     * @param request  the payment details
     * @return the registered payment as a response DTO
     * @throws ResourceNotFoundException if the order is not found or cross-tenant
     * @throws BusinessRuleException     if the order is not READY, amount is insufficient,
     *                                   or payment method is not supported
     */
    @Transactional
    public PaymentResponse registerPayment(UUID tenantId, UUID userId, PaymentRequest request) {
        // 1. Load order — returns 404 for cross-tenant (IDOR protection)
        Order order = orderRepository.findByIdAndTenantId(request.orderId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", request.orderId()));

        // 2. Only READY orders can be paid
        if (!OrderStatusConstants.READY.equals(order.getStatusId())) {
            throw new BusinessRuleException("ORDER_NOT_READY",
                    "Payment can only be registered for orders in READY status");
        }

        // 3. MVP: only CASH is supported
        if (request.paymentMethod() != PaymentMethod.CASH) {
            throw new BusinessRuleException("UNSUPPORTED_PAYMENT_METHOD",
                    "Only CASH payments are supported in this version");
        }

        // 4. Amount received must cover the order total (400, not 409 — client input error)
        if (request.amount().compareTo(order.getTotal()) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INSUFFICIENT_PAYMENT",
                    "Payment amount must be >= order total of " + order.getTotal());
        }

        // 5. Build and persist payment
        BigDecimal changeGiven = request.amount().subtract(order.getTotal());

        Payment payment = new Payment();
        payment.setTenantId(tenantId);
        payment.setOrderId(order.getId());
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setAmount(request.amount());
        payment.setAmountReceived(request.amount());
        payment.setChangeGiven(changeGiven);
        payment.setNotes(request.notes());
        payment.setCreatedBy(userId);

        Payment saved = paymentRepository.save(payment);

        // 6. Check if order is now fully paid
        BigDecimal totalPaid = paymentRepository.sumPaymentsByOrder(order.getId(), tenantId);
        if (totalPaid.compareTo(order.getTotal()) >= 0) {
            closeOrder(order, tenantId, userId);
        }

        log.info("[POS] ACTION=PAYMENT_REGISTERED tenantId={} userId={} resourceId={} resourceType=PAYMENT orderId={}",
                tenantId, userId, saved.getId(), order.getId());

        return PaymentResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * Returns all payments registered for a given order.
     * Cross-tenant access returns 404 via the order lookup.
     *
     * @param tenantId the tenant scope (from JWT)
     * @param orderId  the order whose payments to list
     * @return list of payment response DTOs (may be empty)
     * @throws ResourceNotFoundException if the order is not found or cross-tenant
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> listPaymentsForOrder(UUID tenantId, UUID orderId) {
        // Validate order exists and belongs to tenant (IDOR protection)
        orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        return paymentRepository.findAllByOrderIdAndTenantId(orderId, tenantId)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Closes an order by setting status to COMPLETED, recording closedAt,
     * releasing the table (DINE_IN), and updating customer stats.
     */
    private void closeOrder(Order order, UUID tenantId, UUID userId) {
        order.setStatusId(OrderStatusConstants.COMPLETED);
        order.setClosedAt(Instant.now());
        order.setUpdatedBy(userId);
        orderRepository.save(order);

        // Flush so native JDBC can see the updated order status
        entityManager.flush();

        // Insert COMPLETED status into audit history
        insertStatusHistory(tenantId, order.getId(), OrderStatusConstants.COMPLETED, userId);

        // Release table back to AVAILABLE for DINE_IN orders
        if (order.getTableId() != null) {
            tableRepository.findByIdAndTenantId(order.getTableId(), tenantId)
                    .ifPresent(table -> {
                        table.setStatus(TableStatus.AVAILABLE);
                        tableRepository.save(table);
                    });
        }

        // Update customer order statistics if linked
        if (order.getCustomerId() != null) {
            customerService.incrementOrderStats(tenantId, order.getCustomerId(), order.getTotal());
        }

        log.info("[POS] ACTION=ORDER_COMPLETED tenantId={} userId={} resourceId={} resourceType=ORDER",
                tenantId, userId, order.getId());
    }

    /**
     * Inserts a record into order_status_history for the audit trail.
     * Uses native JDBC because the order_status_history entity is not mapped.
     */
    private void insertStatusHistory(UUID tenantId, UUID orderId, UUID statusId, UUID changedBy) {
        entityManager.createNativeQuery(
                "INSERT INTO order_status_history (id, tenant_id, order_id, status_id, changed_by) " +
                "VALUES (gen_random_uuid(), :tenantId, :orderId, :statusId, :changedBy)")
                .setParameter("tenantId", tenantId)
                .setParameter("orderId", orderId)
                .setParameter("statusId", statusId)
                .setParameter("changedBy", changedBy)
                .executeUpdate();
    }
}
