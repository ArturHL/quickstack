-- Inventory tables for quickstack-inventory @DataJpaTest
-- This is a copy of the production migration V9.

CREATE TABLE ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    branch_id UUID,
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    cost_per_unit DECIMAL(10,4) NOT NULL CHECK (cost_per_unit >= 0),
    current_stock DECIMAL(12,4) NOT NULL DEFAULT 0,
    minimum_stock DECIMAL(12,4) NOT NULL DEFAULT 0 CHECK (minimum_stock >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);

CREATE UNIQUE INDEX uq_ingredients_tenant_name
    ON ingredients(tenant_id, name)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_ingredients_tenant ON ingredients(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_ingredients_low_stock ON ingredients(tenant_id, current_stock, minimum_stock)
    WHERE deleted_at IS NULL;

CREATE TABLE recipes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    product_id UUID NOT NULL,
    variant_id UUID,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- PostgreSQL NULL != NULL in UNIQUE constraints — use partial indexes for correct semantics.
CREATE UNIQUE INDEX uq_recipes_base_per_product
    ON recipes(tenant_id, product_id)
    WHERE variant_id IS NULL;
CREATE UNIQUE INDEX uq_recipes_variant_per_product
    ON recipes(tenant_id, product_id, variant_id)
    WHERE variant_id IS NOT NULL;

CREATE INDEX idx_recipes_tenant_product ON recipes(tenant_id, product_id);

CREATE TABLE recipe_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    ingredient_id UUID NOT NULL,
    quantity DECIMAL(10,4) NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE recipe_items ADD CONSTRAINT uq_recipe_items_recipe_ingredient
    UNIQUE (recipe_id, ingredient_id);

CREATE INDEX idx_recipe_items_recipe ON recipe_items(recipe_id);
CREATE INDEX idx_recipe_items_ingredient ON recipe_items(ingredient_id);

CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    ingredient_id UUID NOT NULL,
    movement_type VARCHAR(30) NOT NULL
        CHECK (movement_type IN ('SALE_DEDUCTION', 'MANUAL_ADJUSTMENT', 'PURCHASE')),
    quantity_delta DECIMAL(12,4) NOT NULL,
    unit_cost_at_time DECIMAL(10,4),
    reference_id UUID,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID
);

CREATE INDEX idx_inventory_movements_tenant ON inventory_movements(tenant_id);
CREATE INDEX idx_inventory_movements_ingredient ON inventory_movements(tenant_id, ingredient_id);
CREATE INDEX idx_inventory_movements_type ON inventory_movements(tenant_id, movement_type);
CREATE INDEX idx_inventory_movements_reference ON inventory_movements(tenant_id, reference_id)
    WHERE reference_id IS NOT NULL;
CREATE INDEX idx_inventory_movements_date ON inventory_movements(tenant_id, created_at);

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
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);

CREATE INDEX idx_expenses_tenant ON expenses(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_expenses_date ON expenses(tenant_id, expense_date) WHERE deleted_at IS NULL;
CREATE INDEX idx_expenses_category ON expenses(tenant_id, expense_category) WHERE deleted_at IS NULL;
