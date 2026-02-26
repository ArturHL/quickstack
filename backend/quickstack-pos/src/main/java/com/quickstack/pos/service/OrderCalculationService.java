package com.quickstack.pos.service;

import com.quickstack.pos.entity.Order;
import com.quickstack.pos.entity.OrderItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Pure calculation service for order totals.
 * <p>
 * No database access — all inputs are in-memory entities.
 * All BigDecimal operations use HALF_UP rounding to 2 decimal places (MXN currency standard).
 * <p>
 * Formula:
 * <pre>
 *   item.lineTotal = quantity * (unitPrice + modifiersTotal)
 *   subtotal       = SUM(item.lineTotal)
 *   tax            = subtotal * taxRate
 *   total          = subtotal + tax - discount
 * </pre>
 *
 * This service intentionally has no knowledge of persistence or security.
 * It is called by OrderService after mutating the items collection.
 */
@Service
public class OrderCalculationService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Calculates the line total for a single item.
     * Mirrors the DB generated column: quantity * (unit_price + modifiers_total).
     * Used in Java before the entity is persisted so the in-memory total is accurate.
     */
    public BigDecimal calculateItemTotal(OrderItem item) {
        BigDecimal pricePerUnit = item.getUnitPrice().add(item.getModifiersTotal());
        return pricePerUnit
                .multiply(BigDecimal.valueOf(item.getQuantity()))
                .setScale(SCALE, ROUNDING);
    }

    /**
     * Sums all item line totals for the given item list.
     * Returns ZERO (with scale 2) for an empty list.
     */
    public BigDecimal calculateSubtotal(List<OrderItem> items) {
        return items.stream()
                    .map(this::calculateItemTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(SCALE, ROUNDING);
    }

    /**
     * Calculates the tax amount: subtotal * taxRate, rounded HALF_UP to 2 decimal places.
     * taxRate is expressed as a decimal fraction (e.g., 0.1600 for 16%).
     */
    public BigDecimal calculateTax(BigDecimal subtotal, BigDecimal taxRate) {
        return subtotal.multiply(taxRate).setScale(SCALE, ROUNDING);
    }

    /**
     * Calculates the order total: subtotal + tax - discount.
     * This method performs no validation — the caller is responsible for ensuring
     * discount does not exceed subtotal + tax.
     */
    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal tax, BigDecimal discount) {
        return subtotal.add(tax).subtract(discount).setScale(SCALE, ROUNDING);
    }

    /**
     * Recalculates and writes subtotal, tax, and total onto the given order.
     * Call this after any change to the items list or tax rate.
     * The order's current discount value is preserved.
     */
    public void recalculateOrder(Order order) {
        BigDecimal subtotal = calculateSubtotal(order.getItems());
        BigDecimal tax      = calculateTax(subtotal, order.getTaxRate());
        BigDecimal total    = calculateTotal(subtotal, tax, order.getDiscount());

        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setTotal(total);
    }
}
