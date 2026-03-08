/**
 * QuickStack Inventory module.
 *
 * Manages ingredients, recipes, inventory movements and business expenses.
 * This module enables the OWNER to track food costs (COGS), register expenses,
 * and generate a P&L report — the financial intelligence layer of Phase 3.
 *
 * Actor: OWNER (manages data manually). System (auto-deducts stock on payment).
 *
 * Depends on: quickstack-common
 * Used by: quickstack-pos (auto-deduction event), quickstack-app (reporting)
 */
package com.quickstack.inventory;
