package com.quickstack.pos.service;

import com.quickstack.branch.entity.RestaurantTable;
import com.quickstack.branch.entity.TableStatus;
import com.quickstack.branch.repository.BranchRepository;
import com.quickstack.branch.repository.TableRepository;
import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.pos.dto.request.OrderCreateRequest;
import com.quickstack.pos.dto.request.OrderItemModifierRequest;
import com.quickstack.pos.dto.request.OrderItemRequest;
import com.quickstack.pos.dto.response.DailySummaryResponse;
import com.quickstack.pos.dto.response.OrderResponse;
import com.quickstack.pos.entity.KdsStatus;
import com.quickstack.pos.entity.Order;
import com.quickstack.pos.entity.OrderItem;
import com.quickstack.pos.entity.OrderItemModifier;
import com.quickstack.pos.entity.OrderStatusConstants;
import com.quickstack.pos.entity.ServiceType;
import com.quickstack.pos.repository.CustomerRepository;
import com.quickstack.pos.repository.OrderRepository;
import com.quickstack.product.repository.ComboRepository;
import com.quickstack.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for order lifecycle management.
 * <p>
 * Orchestrates order creation and state transitions, enforcing business rules:
 * - DINE_IN orders require a valid, AVAILABLE table in the branch
 * - DELIVERY orders require a valid customer
 * - Only PENDING orders can be modified (items added/removed)
 * - Only managers can cancel orders
 * - Orders are NEVER deleted — they are financial audit records
 * <p>
 * State machine:
 * PENDING -> IN_PROGRESS (submitOrder)
 * PENDING -> CANCELLED (cancelOrder)
 * IN_PROGRESS -> CANCELLED (cancelOrder, manager only)
 * <p>
 * ASVS Compliance:
 * - V4.1: tenantId always from JWT, never from request body
 * - V4.1: IDOR protection — cross-tenant returns 404
 * - V7: Full audit trail with order_status_history inserts
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.16");

    private final OrderRepository orderRepository;
    private final OrderCalculationService orderCalculationService;
    private final BranchRepository branchRepository;
    private final TableRepository tableRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ComboRepository comboRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    public OrderService(
            OrderRepository orderRepository,
            OrderCalculationService orderCalculationService,
            BranchRepository branchRepository,
            TableRepository tableRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            ComboRepository comboRepository,
            JdbcTemplate jdbcTemplate,
            EntityManager entityManager) {
        this.orderRepository = orderRepository;
        this.orderCalculationService = orderCalculationService;
        this.branchRepository = branchRepository;
        this.tableRepository = tableRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.comboRepository = comboRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    /**
     * Creates a new order for the tenant.
     * <p>
     * Validates branch, table (if DINE_IN), customer (if DELIVERY), and all items.
     * Marks the table as OCCUPIED for DINE_IN orders.
     * Inserts the initial PENDING status into order_status_history.
     *
     * @param tenantId the tenant creating the order
     * @param userId   the authenticated user creating the order (for audit)
     * @param request  the order creation request
     * @return the created order as a response DTO
     * @throws ResourceNotFoundException if branch, table, customer, or product not
     *                                   found
     * @throws BusinessRuleException     if the table is not available
     */
    @Transactional
    public OrderResponse createOrder(UUID tenantId, UUID userId, OrderCreateRequest request) {
        // 1. Validate branch belongs to tenant
        branchRepository.findByIdAndTenantId(request.branchId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", request.branchId()));

        // 2. DINE_IN: validate table availability and mark as occupied
        if (request.serviceType() == ServiceType.DINE_IN) {
            RestaurantTable table = tableRepository
                    .findByIdAndTenantId(request.tableId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Table", request.tableId()));

            validateTableBelongsToBranch(request.tableId(), request.branchId(), tenantId);

            if (table.getStatus() != TableStatus.AVAILABLE) {
                throw new BusinessRuleException("TABLE_NOT_AVAILABLE", "Table is not available");
            }

            table.setStatus(TableStatus.OCCUPIED);
            tableRepository.save(table);
        }

        // 3. DELIVERY: validate customer belongs to tenant
        if (request.serviceType() == ServiceType.DELIVERY) {
            customerRepository.findByIdAndTenantId(request.customerId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", request.customerId()));
        }

        // 4. Validate each item (product or combo must be active/available)
        for (OrderItemRequest itemRequest : request.items()) {
            validateItemAvailability(itemRequest, tenantId);
        }

        // 5. Resolve tenant tax rate (fallback to MX default if not configured)
        BigDecimal taxRate = resolveTaxRate(tenantId);

        // 6. Generate order number with daily sequence
        int dailySequence = orderRepository.getNextDailySequence(
                tenantId, request.branchId(), LocalDate.now());
        String orderNumber = "ORD-" + LocalDate.now().format(DATE_FORMAT)
                + "-" + String.format("%03d", dailySequence);

        // 7. Build the Order entity
        Order order = new Order();
        order.setTenantId(tenantId);
        order.setBranchId(request.branchId());
        order.setTableId(request.tableId());
        order.setCustomerId(request.customerId());
        order.setOrderNumber(orderNumber);
        order.setDailySequence(dailySequence);
        order.setServiceType(request.serviceType());
        order.setTaxRate(taxRate);
        order.setNotes(request.notes());
        order.setKitchenNotes(request.kitchenNotes());
        order.setCreatedBy(userId);
        order.setUpdatedBy(userId);
        // statusId defaults to PENDING in entity

        // 8. Build and attach items
        for (OrderItemRequest itemRequest : request.items()) {
            OrderItem item = buildOrderItem(itemRequest);
            order.addItem(item);
        }

        // 9. Persist order
        Order saved = orderRepository.save(order);

        // 10. Recalculate totals with the DB-generated line totals
        orderCalculationService.recalculateOrder(saved);
        saved = orderRepository.save(saved);

        // 11. Flush JPA writes so native JDBC can see the order (composite FK
        // constraint)
        entityManager.flush();

        // 12. Insert initial PENDING status into audit history
        insertStatusHistory(tenantId, saved.getId(), OrderStatusConstants.PENDING, userId);

        log.info("[POS] ACTION=ORDER_CREATED tenantId={} userId={} resourceId={} resourceType=ORDER",
                tenantId, userId, saved.getId());

        return OrderResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * Retrieves a single order by ID.
     * <p>
     * Cashiers can only see their own orders. Managers see any order in their
     * tenant.
     * Cross-tenant access always returns 404 (IDOR protection).
     *
     * @param tenantId  the tenant scope
     * @param userId    the requesting user (for cashier-scoped access)
     * @param isManager true if the user has MANAGER or OWNER role
     * @param orderId   the order to retrieve
     * @return the order as a response DTO
     * @throws ResourceNotFoundException if the order is not found or access is
     *                                   denied
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID tenantId, UUID userId, boolean isManager, UUID orderId) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // Cashiers can only access their own orders — return 404, not 403 (IDOR
        // protection)
        if (!isManager && !order.getCreatedBy().equals(userId)) {
            throw new ResourceNotFoundException("Order", orderId);
        }

        return OrderResponse.from(order);
    }

    /**
     * Lists orders with optional filters.
     * <p>
     * Managers see all orders in the tenant. Cashiers only see their own orders.
     *
     * @param tenantId  the tenant scope
     * @param userId    the requesting user (for cashier-scoped access)
     * @param isManager true if the user has MANAGER or OWNER role
     * @param branchId  optional branch filter
     * @param statusId  optional status filter
     * @param pageable  pagination parameters
     * @return page of orders
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(UUID tenantId, UUID userId, boolean isManager,
            UUID branchId, UUID statusId, Pageable pageable) {
        // Cashiers are scoped to their own orders; managers see everything
        UUID createdByFilter = isManager ? null : userId;

        return orderRepository.findAllWithFilters(tenantId, branchId, statusId, createdByFilter, pageable)
                .map(OrderResponse::from);
    }

    // -------------------------------------------------------------------------
    // Modify
    // -------------------------------------------------------------------------

    /**
     * Adds a new item to a PENDING order.
     *
     * @param tenantId    the tenant scope
     * @param userId      the requesting user
     * @param orderId     the order to modify
     * @param itemRequest the item to add
     * @return the updated order as a response DTO
     * @throws ResourceNotFoundException if the order or product/combo is not found
     * @throws BusinessRuleException     if the order is not in PENDING state
     */
    @Transactional
    public OrderResponse addItemToOrder(UUID tenantId, UUID userId, UUID orderId,
            OrderItemRequest itemRequest) {
        Order order = loadModifiableOrder(tenantId, orderId);

        validateItemAvailability(itemRequest, tenantId);

        OrderItem item = buildOrderItem(itemRequest);
        order.addItem(item);
        orderCalculationService.recalculateOrder(order);
        order.setUpdatedBy(userId);

        Order saved = orderRepository.save(order);

        log.info("[POS] ACTION=ORDER_ITEM_ADDED tenantId={} userId={} resourceId={} resourceType=ORDER",
                tenantId, userId, orderId);

        return OrderResponse.from(saved);
    }

    /**
     * Removes an item from a PENDING order.
     * <p>
     * Relies on orphanRemoval=true in the @OneToMany mapping — removing from the
     * collection triggers a DELETE on the item and its modifiers.
     *
     * @param tenantId the tenant scope
     * @param userId   the requesting user
     * @param orderId  the order to modify
     * @param itemId   the item to remove
     * @return the updated order as a response DTO
     * @throws ResourceNotFoundException if the order or item is not found
     * @throws BusinessRuleException     if the order is not in PENDING state
     */
    @Transactional
    public OrderResponse removeItemFromOrder(UUID tenantId, UUID userId, UUID orderId, UUID itemId) {
        Order order = loadModifiableOrder(tenantId, orderId);

        OrderItem itemToRemove = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", itemId));

        order.getItems().remove(itemToRemove);
        orderCalculationService.recalculateOrder(order);
        order.setUpdatedBy(userId);

        Order saved = orderRepository.save(order);

        log.info("[POS] ACTION=ORDER_ITEM_REMOVED tenantId={} userId={} resourceId={} resourceType=ORDER",
                tenantId, userId, orderId);

        return OrderResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // State transitions
    // -------------------------------------------------------------------------

    /**
     * Submits a PENDING order to the kitchen, transitioning it to IN_PROGRESS.
     * <p>
     * Sets kdsStatus and kdsSentAt on all items. Inserts status history record.
     *
     * @param tenantId the tenant scope
     * @param userId   the requesting user
     * @param orderId  the order to submit
     * @return the updated order as a response DTO
     * @throws BusinessRuleException     if the order is not PENDING or has no items
     * @throws ResourceNotFoundException if the order is not found
     */
    @Transactional
    public OrderResponse submitOrder(UUID tenantId, UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.isModifiable()) {
            throw new BusinessRuleException("ORDER_NOT_MODIFIABLE",
                    "Order cannot be modified in its current state");
        }

        if (order.getItems().isEmpty()) {
            throw new BusinessRuleException("ORDER_HAS_NO_ITEMS",
                    "Cannot submit an empty order");
        }

        order.setStatusId(OrderStatusConstants.IN_PROGRESS);

        Instant now = Instant.now();
        for (OrderItem item : order.getItems()) {
            item.setKdsStatus(KdsStatus.PENDING);
            item.setKdsSentAt(now);
        }

        order.setUpdatedBy(userId);
        Order saved = orderRepository.save(order);

        entityManager.flush();
        insertStatusHistory(tenantId, saved.getId(), OrderStatusConstants.IN_PROGRESS, userId);

        log.info("[POS] ACTION=ORDER_SUBMITTED tenantId={} userId={} resourceId={} resourceType=ORDER",
                tenantId, userId, orderId);

        return OrderResponse.from(saved);
    }

    /**
     * Marks an IN_PROGRESS order as READY.
     * <p>
     * Represents the kitchen confirming the order is ready to serve/deliver.
     * Inserts a status history record for the audit trail.
     *
     * @param tenantId the tenant scope
     * @param userId   the requesting user
     * @param orderId  the order to mark as ready
     * @return the updated order as a response DTO
     * @throws BusinessRuleException     if the order is not IN_PROGRESS
     * @throws ResourceNotFoundException if the order is not found
     */
    @Transactional
    public OrderResponse markOrderReady(UUID tenantId, UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!OrderStatusConstants.IN_PROGRESS.equals(order.getStatusId())) {
            throw new BusinessRuleException("ORDER_NOT_IN_PROGRESS",
                    "Order must be IN_PROGRESS to be marked as READY");
        }

        order.setStatusId(OrderStatusConstants.READY);
        order.setUpdatedBy(userId);
        Order saved = orderRepository.save(order);

        entityManager.flush();
        insertStatusHistory(tenantId, saved.getId(), OrderStatusConstants.READY, userId);

        log.info("[POS] ACTION=ORDER_MARKED_READY tenantId={} userId={} resourceId={} resourceType=ORDER",
                tenantId, userId, orderId);

        return OrderResponse.from(saved);
    }

    /**
     * Cancels an order, transitioning it to CANCELLED state.
     * <p>
     * Releases the table (if DINE_IN) back to AVAILABLE.
     * Terminal orders (already COMPLETED or CANCELLED) cannot be cancelled again.
     * Inserts status history record.
     *
     * @param tenantId the tenant scope
     * @param userId   the requesting user (must have MANAGER or OWNER role —
     *                 enforced at controller)
     * @param orderId  the order to cancel
     * @throws BusinessRuleException     if the order is already terminal
     * @throws ResourceNotFoundException if the order is not found
     */
    @Transactional
    public void cancelOrder(UUID tenantId, UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.isTerminal()) {
            throw new BusinessRuleException("ORDER_ALREADY_TERMINAL",
                    "Order is already in a terminal state");
        }

        order.setStatusId(OrderStatusConstants.CANCELLED);
        order.setClosedAt(Instant.now());
        order.setUpdatedBy(userId);

        // Release table back to AVAILABLE for DINE_IN orders
        if (order.getTableId() != null) {
            tableRepository.findByIdAndTenantId(order.getTableId(), tenantId)
                    .ifPresent(table -> {
                        table.setStatus(TableStatus.AVAILABLE);
                        tableRepository.save(table);
                    });
        }

        orderRepository.save(order);

        entityManager.flush();
        insertStatusHistory(tenantId, orderId, OrderStatusConstants.CANCELLED, userId);

        log.info("[POS] ACTION=ORDER_CANCELLED tenantId={} userId={} resourceId={} resourceType=ORDER",
                tenantId, userId, orderId);
    }

    // -------------------------------------------------------------------------
    // Reporting
    // -------------------------------------------------------------------------

    /**
     * Returns the daily sales summary for a branch on a given date.
     * <p>
     * Only COMPLETED orders are included. Branch must belong to the tenant (IDOR protection).
     *
     * @param tenantId the tenant scope
     * @param branchId the branch to report on
     * @param date     the date to report on
     * @return aggregated metrics for the day
     * @throws ResourceNotFoundException if the branch is not found (IDOR protection)
     */
    @Transactional(readOnly = true)
    public DailySummaryResponse getDailySummary(UUID tenantId, UUID branchId, LocalDate date) {
        // IDOR protection: validate branch belongs to tenant
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));

        // Query 1: total orders count and total sales for completed orders
        // COALESCE uses 0.00 to ensure numeric type is preserved (not integer 0)
        Map<String, Object> stats = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS total_orders, COALESCE(SUM(total), 0.00) AS total_sales " +
                "FROM orders WHERE tenant_id = ? AND branch_id = ? " +
                "AND status_id = ? AND DATE(opened_at) = ?",
                tenantId, branchId, OrderStatusConstants.COMPLETED, date);

        int totalOrders = ((Number) stats.get("total_orders")).intValue();
        // Use toString() conversion to handle both BigDecimal and Integer/Long JDBC types,
        // then normalize to scale 2 for consistent JSON serialization (e.g. 0.00 not 0)
        BigDecimal totalSales = new BigDecimal(stats.get("total_sales").toString())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal averageTicket = totalOrders > 0
                ? totalSales.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2);

        // Query 2: breakdown by service type
        List<Map<String, Object>> serviceTypeRows = jdbcTemplate.queryForList(
                "SELECT service_type, COUNT(*) AS cnt FROM orders " +
                "WHERE tenant_id = ? AND branch_id = ? " +
                "AND status_id = ? AND DATE(opened_at) = ? " +
                "GROUP BY service_type",
                tenantId, branchId, OrderStatusConstants.COMPLETED, date);

        Map<String, Long> ordersByServiceType = new LinkedHashMap<>();
        for (Map<String, Object> row : serviceTypeRows) {
            ordersByServiceType.put(
                    (String) row.get("service_type"),
                    ((Number) row.get("cnt")).longValue());
        }

        // Query 3: top 5 products by quantity sold
        List<DailySummaryResponse.TopProductEntry> topProducts = jdbcTemplate.query(
                "SELECT oi.product_name, SUM(oi.quantity) AS qty " +
                "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
                "WHERE o.tenant_id = ? AND o.branch_id = ? " +
                "AND o.status_id = ? AND DATE(o.opened_at) = ? " +
                "GROUP BY oi.product_name ORDER BY qty DESC LIMIT 5",
                (rs, rowNum) -> new DailySummaryResponse.TopProductEntry(
                        rs.getString("product_name"),
                        rs.getLong("qty")),
                tenantId, branchId, OrderStatusConstants.COMPLETED, date);

        log.info("[POS] ACTION=DAILY_SUMMARY_READ tenantId={} branchId={} date={} totalOrders={}",
                tenantId, branchId, date, totalOrders);

        return new DailySummaryResponse(date, branchId, totalOrders, totalSales, averageTicket,
                ordersByServiceType, topProducts);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Loads an order and verifies it is still in PENDING (modifiable) state.
     */
    private Order loadModifiableOrder(UUID tenantId, UUID orderId) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.isModifiable()) {
            throw new BusinessRuleException("ORDER_NOT_MODIFIABLE",
                    "Order cannot be modified in its current state");
        }

        return order;
    }

    /**
     * Validates that the product or combo referenced by an item is active and
     * available.
     * Throws BusinessRuleException if the item cannot be ordered.
     */
    private void validateItemAvailability(OrderItemRequest itemRequest, UUID tenantId) {
        if (itemRequest.productId() != null) {
            var product = productRepository.findByIdAndTenantId(itemRequest.productId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", itemRequest.productId()));
            if (!product.isActive() || !product.isAvailable()) {
                throw new BusinessRuleException("PRODUCT_NOT_AVAILABLE",
                        "Product is not available: " + itemRequest.productName());
            }
        } else {
            var combo = comboRepository.findByIdAndTenantId(itemRequest.comboId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Combo", itemRequest.comboId()));
            if (!combo.isActive()) {
                throw new BusinessRuleException("PRODUCT_NOT_AVAILABLE",
                        "Combo is not available: " + itemRequest.productName());
            }
        }
    }

    /**
     * Validates that a table belongs to the specified branch via its area.
     * Uses a native query because JPQL cannot join across module boundaries here.
     */
    private void validateTableBelongsToBranch(UUID tableId, UUID branchId, UUID tenantId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tables t JOIN areas a ON t.area_id = a.id " +
                        "WHERE t.id = ? AND a.branch_id = ? AND t.tenant_id = ?",
                Integer.class, tableId, branchId, tenantId);

        if (count == null || count == 0) {
            throw new ResourceNotFoundException("Table", tableId);
        }
    }

    /**
     * Resolves the tenant's configured tax rate.
     * Falls back to Mexico's standard 16% IVA if not set.
     */
    private BigDecimal resolveTaxRate(UUID tenantId) {
        try {
            BigDecimal rate = jdbcTemplate.queryForObject(
                    "SELECT tax_rate FROM tenants WHERE id = ?",
                    BigDecimal.class, tenantId);
            return rate != null ? rate : DEFAULT_TAX_RATE;
        } catch (Exception e) {
            log.warn("[POS] Could not resolve tax rate for tenantId={}, using default", tenantId);
            return DEFAULT_TAX_RATE;
        }
    }

    /**
     * Inserts a record into order_status_history for the audit trail.
     */
    private void insertStatusHistory(UUID tenantId, UUID orderId, UUID statusId, UUID changedBy) {
        jdbcTemplate.update(
                "INSERT INTO order_status_history (id, tenant_id, order_id, status_id, changed_by) " +
                        "VALUES (gen_random_uuid(), ?, ?, ?, ?)",
                tenantId, orderId, statusId, changedBy);
    }

    /**
     * Builds an OrderItem from a request DTO, including all its modifiers.
     * The modifiersTotal is computed in-memory as the sum of all modifier price
     * adjustments.
     */
    private OrderItem buildOrderItem(OrderItemRequest itemRequest) {
        OrderItem item = new OrderItem();
        item.setProductId(itemRequest.productId());
        item.setVariantId(itemRequest.variantId());
        item.setComboId(itemRequest.comboId());
        item.setProductName(itemRequest.productName());
        item.setVariantName(itemRequest.variantName());
        item.setQuantity(itemRequest.quantity());
        item.setUnitPrice(itemRequest.unitPrice());
        item.setNotes(itemRequest.notes());
        item.setKdsStatus(KdsStatus.PENDING);

        // Build modifiers and compute modifiersTotal in-memory
        BigDecimal modifiersTotal = BigDecimal.ZERO;
        for (OrderItemModifierRequest modReq : itemRequest.modifiers()) {
            int qty = modReq.quantity() <= 0 ? 1 : modReq.quantity();
            OrderItemModifier modifier = new OrderItemModifier();
            modifier.setModifierId(modReq.modifierId());
            modifier.setModifierName(modReq.modifierName());
            modifier.setPriceAdjustment(modReq.priceAdjustment());
            modifier.setQuantity(qty);
            modifier.setOrderItem(item);
            item.getModifiers().add(modifier);

            modifiersTotal = modifiersTotal.add(
                    modReq.priceAdjustment().multiply(BigDecimal.valueOf(qty)));
        }

        item.setModifiersTotal(modifiersTotal);
        return item;
    }
}
