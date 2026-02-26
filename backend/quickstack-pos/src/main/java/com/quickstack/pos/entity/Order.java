package com.quickstack.pos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order entity representing a sales transaction.
 * <p>
 * Orders are NEVER deleted — they are financial records for audit purposes.
 * Prices and tax rates are denormalized at order creation time to preserve
 * historical accuracy even when product prices or tax rates change later.
 * <p>
 * Business Rules:
 * - service_type DINE_IN requires a table_id (validated at service level)
 * - service_type DELIVERY requires a customer_id (validated at service level)
 * - order_number is unique per tenant (format: ORD-YYYYMMDD-NNN)
 * - daily_sequence resets daily per branch — used by kitchen display
 * - tax_rate is copied from tenant.default_tax_rate at creation (historical snapshot)
 * <p>
 * ASVS Compliance:
 * - V4.1: Multi-tenant isolation via tenant_id on all queries
 * - V7: Full audit trail with created_by, updated_by, opened_at, closed_at
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    // Optional — required only for DINE_IN (validated at service level)
    @Column(name = "table_id")
    private UUID tableId;

    // Optional — required only for DELIVERY (validated at service level)
    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Column(name = "daily_sequence", nullable = false)
    private int dailySequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 30)
    private ServiceType serviceType;

    // References order_status_types table — seeded with known UUIDs in V7
    @Column(name = "status_id", nullable = false)
    private UUID statusId = OrderStatusConstants.PENDING;

    // Financial fields — denormalized for historical accuracy
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "tax", nullable = false, precision = 12, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(name = "discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private OrderSource source = OrderSource.POS;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "kitchen_notes", columnDefinition = "TEXT")
    private String kitchenNotes;

    // Business timestamp — not an audit field, represents when the customer's session began
    @Column(name = "opened_at", nullable = false)
    private Instant openedAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Business methods
    // -------------------------------------------------------------------------

    /**
     * Returns true when the order is in a terminal state (COMPLETED or CANCELLED).
     * Terminal orders cannot be modified or transitioned to any other status.
     */
    public boolean isTerminal() {
        return OrderStatusConstants.TERMINAL_STATUS_IDS.contains(statusId);
    }

    /**
     * Returns true only while the order is PENDING — the only state where
     * items can still be added, modified, or removed before sending to kitchen.
     */
    public boolean isModifiable() {
        return OrderStatusConstants.PENDING.equals(statusId);
    }

    /**
     * Adds an item to the order and wires the bidirectional relationship.
     * The item's orderId and tenantId are set here to ensure consistency.
     * Must be called instead of manipulating the items list directly.
     */
    public void addItem(OrderItem item) {
        item.setOrderId(this.id);
        item.setTenantId(this.tenantId);
        item.setOrder(this);
        this.items.add(item);
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public UUID getTableId() {
        return tableId;
    }

    public void setTableId(UUID tableId) {
        this.tableId = tableId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public int getDailySequence() {
        return dailySequence;
    }

    public void setDailySequence(int dailySequence) {
        this.dailySequence = dailySequence;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public UUID getStatusId() {
        return statusId;
    }

    public void setStatusId(UUID statusId) {
        this.statusId = statusId;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public OrderSource getSource() {
        return source;
    }

    public void setSource(OrderSource source) {
        this.source = source;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getKitchenNotes() {
        return kitchenNotes;
    }

    public void setKitchenNotes(String kitchenNotes) {
        this.kitchenNotes = kitchenNotes;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}
