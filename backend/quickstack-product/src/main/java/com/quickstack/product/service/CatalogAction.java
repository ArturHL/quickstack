package com.quickstack.product.service;

/**
 * Audit actions for catalog operations.
 */
public enum CatalogAction {
    CATEGORY_CREATED,
    CATEGORY_UPDATED,
    CATEGORY_DELETED,
    CATEGORY_RESTORED,
    PRODUCT_CREATED,
    PRODUCT_UPDATED,
    PRODUCT_DELETED,
    PRODUCT_RESTORED,
    PRODUCT_AVAILABILITY_CHANGED,
    VARIANT_ADDED,
    VARIANT_UPDATED,
    VARIANT_DELETED
}
