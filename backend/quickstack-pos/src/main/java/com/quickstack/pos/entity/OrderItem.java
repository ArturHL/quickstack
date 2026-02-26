package com.quickstack.pos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A single line item within an order.
 * <p>
 * Product/variant names and prices are copied at order time to preserve
 * historical accuracy. Changing a product's price after an order is placed
 * must NOT affect existing order items.
 * <p>
 * Business Rules:
 * - Must reference either a product_id OR a combo_id, never both (DB
 * constraint)
 * - line_total is a DB-generated column: quantity * (unit_price +
 * modifiers_total)
 * - kds_status tracks kitchen preparation lifecycle independently per item
 * - Records are NEVER deleted — part of the financial audit trail
 * <p>
 * ASVS Compliance:
 * - V4.1: tenant_id on all queries ensures multi-tenant isolation
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    // Read-only shortcut — the actual FK is managed by the @ManyToOne below
    @Column(name = "order_id", insertable = false, updatable = false)
    private UUID orderId;

    // Optional references — for linking to catalog; null is valid if catalog item
    // was deleted
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "combo_id")
    private UUID comboId;

    // Historical snapshot — DO NOT update after order creation
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "variant_name", length = 100)
    private String variantName;

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "modifiers_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal modifiersTotal = BigDecimal.ZERO;

    // Generated column — DB computes this. Never insert or update.
    @Column(name = "line_total", insertable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "kds_status", nullable = false, length = 30)
    private KdsStatus kdsStatus = KdsStatus.PENDING;

    @Column(name = "kds_sent_at")
    private Instant kdsSentAt;

    @Column(name = "kds_ready_at")
    private Instant kdsReadyAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // -------------------------------------------------------------------------
    // Relationships
    // -------------------------------------------------------------------------

    /**
     * Owning side of the bidirectional relationship — manages the order_id FK
     * column.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @OneToMany(mappedBy = "orderItem", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderItemModifier> modifiers = new HashSet<>();

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

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public void setVariantId(UUID variantId) {
        this.variantId = variantId;
    }

    public UUID getComboId() {
        return comboId;
    }

    public void setComboId(UUID comboId) {
        this.comboId = comboId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getModifiersTotal() {
        return modifiersTotal;
    }

    public void setModifiersTotal(BigDecimal modifiersTotal) {
        this.modifiersTotal = modifiersTotal;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public KdsStatus getKdsStatus() {
        return kdsStatus;
    }

    public void setKdsStatus(KdsStatus kdsStatus) {
        this.kdsStatus = kdsStatus;
    }

    public Instant getKdsSentAt() {
        return kdsSentAt;
    }

    public void setKdsSentAt(Instant kdsSentAt) {
        this.kdsSentAt = kdsSentAt;
    }

    public Instant getKdsReadyAt() {
        return kdsReadyAt;
    }

    public void setKdsReadyAt(Instant kdsReadyAt) {
        this.kdsReadyAt = kdsReadyAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
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

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Set<OrderItemModifier> getModifiers() {
        return modifiers;
    }

    public void setModifiers(Set<OrderItemModifier> modifiers) {
        this.modifiers = modifiers;
    }
}
