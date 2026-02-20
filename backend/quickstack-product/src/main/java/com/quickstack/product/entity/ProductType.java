package com.quickstack.product.entity;

/**
 * Enumeration of product types supported by the POS system.
 * <p>
 * Product types define how a product is sold and priced:
 * <ul>
 * <li><b>SIMPLE</b>: Direct sale product with a single price (e.g., Taco, Refresco)</li>
 * <li><b>VARIANT</b>: Product with size/type variations that affect pricing (e.g., Café Chico/Grande)</li>
 * <li><b>COMBO</b>: Bundle of multiple products sold at a special price (e.g., Combo 1: Burger + Fries + Drink)</li>
 * </ul>
 * <p>
 * Business Rules:
 * - SIMPLE products use base_price directly
 * - VARIANT products require at least one ProductVariant; price = base_price + variant.price_adjustment
 * - COMBO products reference other products via combo_items table (not implemented in Phase 1.1)
 */
public enum ProductType {
    /**
     * Simple product sold as-is with a single price.
     */
    SIMPLE,

    /**
     * Product with variants (sizes/options) that modify the base price.
     * Requires at least one ProductVariant.
     */
    VARIANT,

    /**
     * Bundle of products sold at a special combo price.
     * Not implemented in Phase 1.1 - deferred to Phase 1.2.
     */
    COMBO
}
