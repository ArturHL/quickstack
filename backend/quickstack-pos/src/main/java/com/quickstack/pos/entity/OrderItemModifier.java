package com.quickstack.pos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Modifier applied to a specific order item.
 * <p>
 * Modifier names and prices are copied at order time to preserve historical
 * accuracy.
 * Once created, modifier records are NEVER modified or deleted (part of the
 * financial record).
 * <p>
 * Business Rules:
 * - modifier_name is the historical snapshot — not linked to live modifier
 * catalog for display
 * - price_adjustment can be negative (discount) or zero (free add-on)
 * - quantity allows for doubled/tripled modifiers (e.g., "double cheese")
 */
@Entity
@Table(name = "order_item_modifiers")
public class OrderItemModifier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    // Read-only shortcut — the actual FK is managed by the @ManyToOne below
    @Column(name = "order_item_id", insertable = false, updatable = false)
    private UUID orderItemId;

    // Optional reference to the original modifier — may be null if modifier was
    // deleted
    @Column(name = "modifier_id")
    private UUID modifierId;

    // Historical snapshot fields — DO NOT update after creation
    @Column(name = "modifier_name", nullable = false, length = 100)
    private String modifierName;

    @Column(name = "price_adjustment", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAdjustment = BigDecimal.ZERO;

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Owning side of the bidirectional relationship — manages the order_item_id FK
    // column
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

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

    public UUID getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(UUID orderItemId) {
        this.orderItemId = orderItemId;
    }

    public UUID getModifierId() {
        return modifierId;
    }

    public void setModifierId(UUID modifierId) {
        this.modifierId = modifierId;
    }

    public String getModifierName() {
        return modifierName;
    }

    public void setModifierName(String modifierName) {
        this.modifierName = modifierName;
    }

    public BigDecimal getPriceAdjustment() {
        return priceAdjustment;
    }

    public void setPriceAdjustment(BigDecimal priceAdjustment) {
        this.priceAdjustment = priceAdjustment;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public void setOrderItem(OrderItem orderItem) {
        this.orderItem = orderItem;
    }
}
