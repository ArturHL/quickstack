package com.quickstack.inventory.entity;

/**
 * Measurement units for ingredients.
 * Stored as STRING in the database to be self-descriptive and migration-safe.
 */
public enum UnitType {
    KILOGRAM,
    GRAM,
    LITER,
    MILLILITER,
    UNIT,
    PORTION
}
