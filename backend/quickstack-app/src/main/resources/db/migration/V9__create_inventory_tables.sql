-- =============================================================================
-- QuickStack POS - Database Migration V9
-- Module: Inventory (Ingredients, Recipes, Stock Movements, Expenses)
-- Description: Owner Intelligence — food cost tracking, COGS, P&L foundation
-- =============================================================================
--
-- REPLACES preliminary V4 inventory schema (V4 was an early design draft).
-- V4 tables used external unit_type_id / stock_movement_type_id FKs which
-- added unnecessary coupling. The new design uses Java enums stored as VARCHAR.
-- No production data exists in V4 tables (V7 seed did not insert inventory rows).
--
-- Drop old V4 inventory tables before recreating with the canonical schema.
-- CASCADE handles any FK dependencies between these tables.
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS purchase_order_items CASCADE;
DROP TABLE IF EXISTS purchase_orders CASCADE;
DROP TABLE IF EXISTS stock_movements CASCADE;
DROP TABLE IF EXISTS recipes CASCADE;
DROP TABLE IF EXISTS suppliers CASCADE;
DROP TABLE IF EXISTS ingredients CASCADE;
-- =============================================================================

-- -----------------------------------------------------------------------------
-- INGREDIENTS
-- Purpose: Raw materials/supplies consumed when a product is sold.
-- Strategy: Soft delete (recipes and movements reference ingredients).
-- unit: stored as VARCHAR (Java enum UnitType: KILOGRAM/GRAM/LITER/MILLILITER/UNIT/PORTION)
-- branchId: nullable — ingredient can be global (tenant-level) or branch-specific.
-- -----------------------------------------------------------------------------
CREATE TABLE ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    branch_id UUID,

    -- Information
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(20) NOT NULL,

    -- Cost & Stock
    cost_per_unit DECIMAL(10,4) NOT NULL CHECK (cost_per_unit >= 0),
    current_stock DECIMAL(12,4) NOT NULL DEFAULT 0,
    minimum_stock DECIMAL(12,4) NOT NULL DEFAULT 0 CHECK (minimum_stock >= 0),

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);

-- Name unique within tenant (soft delete aware)
CREATE UNIQUE INDEX uq_ingredients_tenant_name
    ON ingredients(tenant_id, name)
    WHERE deleted_at IS NULL;

-- Indexes
CREATE INDEX idx_ingredients_tenant ON ingredients(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_ingredients_low_stock ON ingredients(tenant_id, current_stock, minimum_stock)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE ingredients IS 'Raw materials used in recipes. Stock tracked per tenant. Soft delete.';
COMMENT ON COLUMN ingredients.unit IS 'Java enum UnitType: KILOGRAM | GRAM | LITER | MILLILITER | UNIT | PORTION';
COMMENT ON COLUMN ingredients.cost_per_unit IS 'Current purchase cost per unit. Historical costs stored in inventory_movements.';
COMMENT ON COLUMN ingredients.branch_id IS 'NULL = global ingredient shared across all branches of the tenant.';

-- -----------------------------------------------------------------------------
-- RECIPES
-- Purpose: Define which ingredients (and how much) are consumed per product sold.
-- Strategy: Managed via upsert — OWNER replaces the full recipe each time.
-- variantId: NULL means the recipe applies to all variants (base recipe).
--            A variant-specific recipe takes priority over the base recipe.
-- -----------------------------------------------------------------------------
CREATE TABLE recipes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    -- Product reference (no DB-level FK — cross-module boundary)
    product_id UUID NOT NULL,
    variant_id UUID,

    is_active BOOLEAN NOT NULL DEFAULT true,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Unique constraints for recipes:
-- PostgreSQL treats NULL != NULL in UNIQUE constraints, so we need partial indexes.
-- Base recipe (variant_id IS NULL): one per product per tenant.
CREATE UNIQUE INDEX uq_recipes_base_per_product
    ON recipes(tenant_id, product_id)
    WHERE variant_id IS NULL;
-- Variant-specific recipe: one per product+variant per tenant.
CREATE UNIQUE INDEX uq_recipes_variant_per_product
    ON recipes(tenant_id, product_id, variant_id)
    WHERE variant_id IS NOT NULL;

-- Indexes
CREATE INDEX idx_recipes_tenant_product ON recipes(tenant_id, product_id);

COMMENT ON TABLE recipes IS 'Ingredient list per product/variant. Replaced atomically on update.';
COMMENT ON COLUMN recipes.variant_id IS 'NULL = base recipe (all variants). Specific variant overrides base.';

-- -----------------------------------------------------------------------------
-- RECIPE ITEMS
-- Purpose: Individual ingredient lines within a recipe.
-- Strategy: Cascade delete when recipe is replaced (managed via RecipeItemRepository.deleteAllByRecipeId).
-- Unique constraint: same ingredient cannot appear twice in one recipe.
-- -----------------------------------------------------------------------------
CREATE TABLE recipe_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,

    -- Ingredient reference (no DB-level FK — IDOR validated in service layer)
    ingredient_id UUID NOT NULL,

    -- Amount consumed per 1 unit of product sold (same unit as Ingredient.unit)
    quantity DECIMAL(10,4) NOT NULL CHECK (quantity > 0),

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Same ingredient cannot appear twice in one recipe
ALTER TABLE recipe_items ADD CONSTRAINT uq_recipe_items_recipe_ingredient
    UNIQUE (recipe_id, ingredient_id);

-- Indexes
CREATE INDEX idx_recipe_items_recipe ON recipe_items(recipe_id);
CREATE INDEX idx_recipe_items_ingredient ON recipe_items(ingredient_id);

COMMENT ON TABLE recipe_items IS 'Ingredient quantities per recipe. Unique per (recipe, ingredient).';

-- -----------------------------------------------------------------------------
-- INVENTORY MOVEMENTS
-- Purpose: Immutable audit log of all stock changes.
-- Strategy: NEVER deleted — financial audit trail for COGS (ADR-007).
-- movement_type: SALE_DEDUCTION | MANUAL_ADJUSTMENT | PURCHASE
-- quantity_delta: negative for SALE_DEDUCTION, positive for restocking.
-- unit_cost_at_time: ingredient cost at the moment of the movement (ADR-007).
-- reference_id: orderId for SALE_DEDUCTION, or other document ID.
-- -----------------------------------------------------------------------------
CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    ingredient_id UUID NOT NULL,
    movement_type VARCHAR(30) NOT NULL
        CHECK (movement_type IN ('SALE_DEDUCTION', 'MANUAL_ADJUSTMENT', 'PURCHASE')),

    quantity_delta DECIMAL(12,4) NOT NULL,
    unit_cost_at_time DECIMAL(10,4),

    -- Source document (orderId for SALE_DEDUCTION)
    reference_id UUID,

    notes VARCHAR(500),

    -- Audit (no updated_at, no deleted_at — immutable record)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID
);

-- Indexes
CREATE INDEX idx_inventory_movements_tenant ON inventory_movements(tenant_id);
CREATE INDEX idx_inventory_movements_ingredient ON inventory_movements(tenant_id, ingredient_id);
CREATE INDEX idx_inventory_movements_type ON inventory_movements(tenant_id, movement_type);
CREATE INDEX idx_inventory_movements_reference ON inventory_movements(tenant_id, reference_id)
    WHERE reference_id IS NOT NULL;
CREATE INDEX idx_inventory_movements_date ON inventory_movements(tenant_id, created_at);

COMMENT ON TABLE inventory_movements IS 'Immutable stock change audit log. NEVER delete. Used for COGS calculation.';
COMMENT ON COLUMN inventory_movements.quantity_delta IS 'Negative for SALE_DEDUCTION. Positive for PURCHASE/MANUAL_ADJUSTMENT (adds stock).';
COMMENT ON COLUMN inventory_movements.unit_cost_at_time IS 'Ingredient cost per unit at movement time (ADR-007: historical cost pricing).';
COMMENT ON COLUMN inventory_movements.reference_id IS 'orderId for SALE_DEDUCTION. NULL for manual adjustments.';

-- -----------------------------------------------------------------------------
-- EXPENSES
-- Purpose: Business expenses recorded by the OWNER.
-- Strategy: Soft delete (OWNER can correct mistakes).
-- expense_category: FOOD_COST | LABOR | RENT | UTILITIES | SUPPLIES | OTHER
-- Used for P&L: Ventas - COGS - Gastos = Margen bruto.
-- -----------------------------------------------------------------------------
CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    branch_id UUID,

    amount DECIMAL(12,2) NOT NULL CHECK (amount > 0),
    expense_category VARCHAR(30) NOT NULL
        CHECK (expense_category IN ('FOOD_COST', 'LABOR', 'RENT', 'UTILITIES', 'SUPPLIES', 'OTHER')),
    expense_date DATE NOT NULL,

    description VARCHAR(500),
    receipt_url VARCHAR(500),

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);

-- Indexes
CREATE INDEX idx_expenses_tenant ON expenses(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_expenses_date ON expenses(tenant_id, expense_date) WHERE deleted_at IS NULL;
CREATE INDEX idx_expenses_category ON expenses(tenant_id, expense_category) WHERE deleted_at IS NULL;

COMMENT ON TABLE expenses IS 'Business expenses for P&L. Soft delete. Recorded by OWNER.';
COMMENT ON COLUMN expenses.branch_id IS 'NULL = tenant-wide expense (e.g., rent).';
COMMENT ON COLUMN expenses.expense_date IS 'Date of the expense (not timestamp — expenses are recorded by day).';
