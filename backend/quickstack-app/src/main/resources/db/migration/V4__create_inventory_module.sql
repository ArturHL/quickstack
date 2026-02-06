-- =============================================================================
-- QuickStack POS - Database Migration V4
-- Module: Inventory (Ingredients, Recipes, Stock, Suppliers)
-- Description: Inventory management with automatic stock deduction
-- =============================================================================

-- -----------------------------------------------------------------------------
-- INGREDIENTS
-- Purpose: Raw materials used in recipes
-- Strategy: Soft delete (recipes reference ingredients)
-- -----------------------------------------------------------------------------
CREATE TABLE ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    -- Information
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sku VARCHAR(50),

    -- Unit & Cost
    unit_type_id UUID NOT NULL REFERENCES unit_types(id),
    cost_per_unit DECIMAL(10,4) NOT NULL DEFAULT 0 CHECK (cost_per_unit >= 0),

    -- Stock Levels
    current_stock DECIMAL(12,4) NOT NULL DEFAULT 0,
    min_stock_level DECIMAL(12,4) NOT NULL DEFAULT 0,
    max_stock_level DECIMAL(12,4),

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES users(id)
);

-- Composite unique for FK references
ALTER TABLE ingredients ADD CONSTRAINT uq_ingredients_tenant_id UNIQUE (tenant_id, id);

-- Name and SKU unique within tenant
ALTER TABLE ingredients ADD CONSTRAINT uq_ingredients_tenant_name UNIQUE (tenant_id, name);
ALTER TABLE ingredients ADD CONSTRAINT uq_ingredients_tenant_sku UNIQUE (tenant_id, sku);

-- Indexes
CREATE INDEX idx_ingredients_tenant ON ingredients(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_ingredients_low_stock ON ingredients(tenant_id, current_stock, min_stock_level)
    WHERE deleted_at IS NULL AND is_active = true;
CREATE INDEX idx_ingredients_unit_type ON ingredients(unit_type_id);

COMMENT ON TABLE ingredients IS 'Raw materials/supplies for recipes. Stock tracked per tenant (not per branch in MVP).';
COMMENT ON COLUMN ingredients.current_stock IS 'Current quantity on hand, in unit_type units';
COMMENT ON COLUMN ingredients.cost_per_unit IS 'Average cost per unit for COGS calculation';

-- -----------------------------------------------------------------------------
-- SUPPLIERS
-- Purpose: Vendors who provide ingredients
-- Strategy: Soft delete (purchase orders reference suppliers)
-- -----------------------------------------------------------------------------
CREATE TABLE suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    -- Information
    name VARCHAR(255) NOT NULL,
    code VARCHAR(20),

    -- Contact
    contact_name VARCHAR(255),
    phone VARCHAR(20),
    email VARCHAR(255),
    website VARCHAR(500),

    -- Address
    address_line1 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(10),

    -- Payment
    payment_terms TEXT,
    notes TEXT,

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES users(id)
);

-- Composite unique for FK references
ALTER TABLE suppliers ADD CONSTRAINT uq_suppliers_tenant_id UNIQUE (tenant_id, id);

-- Name and code unique within tenant
ALTER TABLE suppliers ADD CONSTRAINT uq_suppliers_tenant_name UNIQUE (tenant_id, name);
ALTER TABLE suppliers ADD CONSTRAINT uq_suppliers_tenant_code UNIQUE (tenant_id, code);

-- Indexes
CREATE INDEX idx_suppliers_tenant ON suppliers(tenant_id) WHERE deleted_at IS NULL;

COMMENT ON TABLE suppliers IS 'Vendors for ingredient purchases. Used in purchase orders.';

-- -----------------------------------------------------------------------------
-- RECIPES
-- Purpose: Define ingredient consumption per product/variant
-- Strategy: Hard delete (follows product lifecycle)
-- -----------------------------------------------------------------------------
CREATE TABLE recipes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    product_id UUID NOT NULL,
    variant_id UUID,
    ingredient_id UUID NOT NULL,

    -- Quantity consumed per unit sold
    quantity DECIMAL(10,4) NOT NULL CHECK (quantity > 0),

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- FKs with tenant isolation
ALTER TABLE recipes ADD CONSTRAINT fk_recipes_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES products(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE recipes ADD CONSTRAINT fk_recipes_variant
    FOREIGN KEY (tenant_id, variant_id) REFERENCES product_variants(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE recipes ADD CONSTRAINT fk_recipes_ingredient
    FOREIGN KEY (tenant_id, ingredient_id) REFERENCES ingredients(tenant_id, id);

-- Unique recipe per product+variant+ingredient
ALTER TABLE recipes ADD CONSTRAINT uq_recipes_product_variant_ingredient
    UNIQUE (tenant_id, product_id, variant_id, ingredient_id);

-- Indexes
CREATE INDEX idx_recipes_product ON recipes(tenant_id, product_id);
CREATE INDEX idx_recipes_ingredient ON recipes(tenant_id, ingredient_id);

COMMENT ON TABLE recipes IS 'Ingredient requirements per product. Used to auto-deduct stock on sales.';
COMMENT ON COLUMN recipes.variant_id IS 'NULL applies to all variants; specific variant_id overrides base recipe';
COMMENT ON COLUMN recipes.quantity IS 'Amount of ingredient consumed per 1 unit of product sold';

-- -----------------------------------------------------------------------------
-- STOCK MOVEMENTS
-- Purpose: Track all inventory changes for audit trail
-- Strategy: Never delete (audit log)
-- -----------------------------------------------------------------------------
CREATE TABLE stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    branch_id UUID,
    ingredient_id UUID NOT NULL,
    movement_type_id UUID NOT NULL REFERENCES stock_movement_types(id),

    -- Movement Details
    quantity DECIMAL(12,4) NOT NULL,
    unit_cost DECIMAL(10,4),

    -- Reference to source document
    reference_type VARCHAR(50),
    reference_id UUID,

    -- Balance after movement
    balance_after DECIMAL(12,4) NOT NULL,

    -- Notes
    notes TEXT,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id)
);

-- FKs with tenant isolation
ALTER TABLE stock_movements ADD CONSTRAINT fk_stock_movements_ingredient
    FOREIGN KEY (tenant_id, ingredient_id) REFERENCES ingredients(tenant_id, id);
ALTER TABLE stock_movements ADD CONSTRAINT fk_stock_movements_branch
    FOREIGN KEY (tenant_id, branch_id) REFERENCES branches(tenant_id, id);

-- Indexes
CREATE INDEX idx_stock_movements_tenant ON stock_movements(tenant_id);
CREATE INDEX idx_stock_movements_ingredient ON stock_movements(tenant_id, ingredient_id);
CREATE INDEX idx_stock_movements_date ON stock_movements(tenant_id, created_at);
CREATE INDEX idx_stock_movements_reference ON stock_movements(tenant_id, reference_type, reference_id);
CREATE INDEX idx_stock_movements_type ON stock_movements(movement_type_id);

COMMENT ON TABLE stock_movements IS 'Immutable audit log of all inventory changes.';
COMMENT ON COLUMN stock_movements.reference_type IS 'Source: "ORDER", "PURCHASE_ORDER", "ADJUSTMENT", "WASTE"';
COMMENT ON COLUMN stock_movements.reference_id IS 'ID of the source document (order_id, purchase_order_id, etc.)';
COMMENT ON COLUMN stock_movements.balance_after IS 'Ingredient stock balance after this movement';

-- -----------------------------------------------------------------------------
-- PURCHASE ORDERS
-- Purpose: Track ingredient purchases from suppliers
-- Strategy: Soft delete (financial records)
-- -----------------------------------------------------------------------------
CREATE TABLE purchase_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    branch_id UUID,
    supplier_id UUID,

    -- Order Info
    order_number VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'RECEIVED', 'CANCELLED')),

    -- Dates
    order_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expected_date DATE,
    received_date DATE,

    -- Totals
    subtotal DECIMAL(12,2) NOT NULL DEFAULT 0,
    tax DECIMAL(12,2) NOT NULL DEFAULT 0,
    total DECIMAL(12,2) NOT NULL DEFAULT 0,

    -- Notes
    notes TEXT,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMPTZ
);

-- Composite unique for FK references
ALTER TABLE purchase_orders ADD CONSTRAINT uq_purchase_orders_tenant_id UNIQUE (tenant_id, id);

-- Order number unique within tenant
ALTER TABLE purchase_orders ADD CONSTRAINT uq_purchase_orders_tenant_number UNIQUE (tenant_id, order_number);

-- FKs with tenant isolation
ALTER TABLE purchase_orders ADD CONSTRAINT fk_purchase_orders_branch
    FOREIGN KEY (tenant_id, branch_id) REFERENCES branches(tenant_id, id);
ALTER TABLE purchase_orders ADD CONSTRAINT fk_purchase_orders_supplier
    FOREIGN KEY (tenant_id, supplier_id) REFERENCES suppliers(tenant_id, id);

-- Indexes
CREATE INDEX idx_purchase_orders_tenant ON purchase_orders(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_purchase_orders_status ON purchase_orders(tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_purchase_orders_date ON purchase_orders(tenant_id, order_date) WHERE deleted_at IS NULL;
CREATE INDEX idx_purchase_orders_supplier ON purchase_orders(tenant_id, supplier_id);

COMMENT ON TABLE purchase_orders IS 'Ingredient purchase orders from suppliers.';

-- -----------------------------------------------------------------------------
-- PURCHASE ORDER ITEMS
-- Purpose: Line items in a purchase order
-- Strategy: Cascade delete with parent
-- -----------------------------------------------------------------------------
CREATE TABLE purchase_order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    purchase_order_id UUID NOT NULL,
    ingredient_id UUID NOT NULL,

    -- Quantities
    quantity_ordered DECIMAL(12,4) NOT NULL CHECK (quantity_ordered > 0),
    quantity_received DECIMAL(12,4) DEFAULT 0,

    -- Pricing
    unit_cost DECIMAL(10,4) NOT NULL CHECK (unit_cost >= 0),
    total_cost DECIMAL(12,2) GENERATED ALWAYS AS (quantity_ordered * unit_cost) STORED,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- FKs with tenant isolation
ALTER TABLE purchase_order_items ADD CONSTRAINT fk_po_items_order
    FOREIGN KEY (tenant_id, purchase_order_id) REFERENCES purchase_orders(tenant_id, id) ON DELETE CASCADE;
ALTER TABLE purchase_order_items ADD CONSTRAINT fk_po_items_ingredient
    FOREIGN KEY (tenant_id, ingredient_id) REFERENCES ingredients(tenant_id, id);

-- Indexes
CREATE INDEX idx_po_items_order ON purchase_order_items(tenant_id, purchase_order_id);
CREATE INDEX idx_po_items_ingredient ON purchase_order_items(tenant_id, ingredient_id);

COMMENT ON TABLE purchase_order_items IS 'Line items for purchase orders.';
