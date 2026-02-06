-- =============================================================================
-- QuickStack POS - Database Migration V3
-- Module: Catalog (Products, Categories, Modifiers, Combos)
-- Description: Menu management with variants and customization options
-- =============================================================================

-- -----------------------------------------------------------------------------
-- CATEGORIES
-- Purpose: Hierarchical product categories
-- Strategy: Soft delete (products reference categories)
-- -----------------------------------------------------------------------------
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    parent_id UUID,

    -- Information
    name VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(500),

    -- Display
    sort_order INTEGER NOT NULL DEFAULT 0,
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
ALTER TABLE categories ADD CONSTRAINT uq_categories_tenant_id UNIQUE (tenant_id, id);

-- Self-referencing FK with tenant isolation
ALTER TABLE categories ADD CONSTRAINT fk_categories_parent
    FOREIGN KEY (tenant_id, parent_id) REFERENCES categories(tenant_id, id);

-- Unique name within same parent level
ALTER TABLE categories ADD CONSTRAINT uq_categories_tenant_name_parent
    UNIQUE (tenant_id, name, parent_id);

-- Indexes
CREATE INDEX idx_categories_tenant ON categories(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_categories_parent ON categories(tenant_id, parent_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_categories_sort ON categories(tenant_id, sort_order) WHERE deleted_at IS NULL;

COMMENT ON TABLE categories IS 'Hierarchical product categories. Max 2 levels recommended.';
COMMENT ON COLUMN categories.parent_id IS 'NULL for top-level categories';

-- -----------------------------------------------------------------------------
-- PRODUCTS
-- Purpose: Menu items that can be sold
-- Strategy: Soft delete (orders reference products historically)
-- -----------------------------------------------------------------------------
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    category_id UUID,

    -- Information
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sku VARCHAR(50),
    image_url VARCHAR(500),

    -- Pricing
    base_price DECIMAL(10,2) NOT NULL CHECK (base_price >= 0),
    cost_price DECIMAL(10,2) CHECK (cost_price >= 0),

    -- Type: SIMPLE (direct sale), VARIANT (has variants), COMBO (bundle)
    product_type VARCHAR(20) NOT NULL DEFAULT 'SIMPLE'
        CHECK (product_type IN ('SIMPLE', 'VARIANT', 'COMBO')),

    -- Availability
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_available BOOLEAN NOT NULL DEFAULT true,
    available_from TIME,
    available_until TIME,

    -- Inventory Integration
    track_inventory BOOLEAN NOT NULL DEFAULT false,

    -- Display
    sort_order INTEGER NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES users(id)
);

-- Composite unique for FK references
ALTER TABLE products ADD CONSTRAINT uq_products_tenant_id UNIQUE (tenant_id, id);

-- SKU unique within tenant
ALTER TABLE products ADD CONSTRAINT uq_products_tenant_sku UNIQUE (tenant_id, sku);

-- Category FK with tenant isolation
ALTER TABLE products ADD CONSTRAINT fk_products_category
    FOREIGN KEY (tenant_id, category_id) REFERENCES categories(tenant_id, id);

-- Indexes
CREATE INDEX idx_products_tenant ON products(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_category ON products(tenant_id, category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_active ON products(tenant_id, is_active, is_available) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_type ON products(tenant_id, product_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_sort ON products(tenant_id, sort_order) WHERE deleted_at IS NULL;

COMMENT ON TABLE products IS 'Menu items. Can be simple products, products with variants, or combos.';
COMMENT ON COLUMN products.product_type IS 'SIMPLE: sold as-is. VARIANT: has size/options. COMBO: bundle of products.';
COMMENT ON COLUMN products.is_available IS 'False when temporarily out of stock (vs. is_active for permanent removal)';
COMMENT ON COLUMN products.track_inventory IS 'If true, sales auto-deduct from ingredient stock';

-- -----------------------------------------------------------------------------
-- PRODUCT VARIANTS
-- Purpose: Size/type variations of a product (Chico, Mediano, Grande)
-- Strategy: Soft delete with product
-- -----------------------------------------------------------------------------
CREATE TABLE product_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    product_id UUID NOT NULL,

    -- Information
    name VARCHAR(100) NOT NULL,
    sku VARCHAR(50),

    -- Pricing: Added to base_price
    price_adjustment DECIMAL(10,2) NOT NULL DEFAULT 0,

    -- Status
    is_default BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    sort_order INTEGER NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- Composite unique for FK references
ALTER TABLE product_variants ADD CONSTRAINT uq_product_variants_tenant_id UNIQUE (tenant_id, id);

-- Product FK with tenant isolation
ALTER TABLE product_variants ADD CONSTRAINT fk_product_variants_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES products(tenant_id, id);

-- SKU unique within tenant
ALTER TABLE product_variants ADD CONSTRAINT uq_product_variants_tenant_sku UNIQUE (tenant_id, sku);

-- Name unique within product
ALTER TABLE product_variants ADD CONSTRAINT uq_product_variants_product_name
    UNIQUE (tenant_id, product_id, name);

-- Indexes
CREATE INDEX idx_product_variants_product ON product_variants(tenant_id, product_id) WHERE deleted_at IS NULL;

COMMENT ON TABLE product_variants IS 'Product variations like sizes: Chico ($0), Mediano (+$10), Grande (+$20)';
COMMENT ON COLUMN product_variants.price_adjustment IS 'Amount added to product base_price. Can be negative.';
COMMENT ON COLUMN product_variants.is_default IS 'Pre-selected variant in POS. Only one per product should be true.';

-- -----------------------------------------------------------------------------
-- MODIFIER GROUPS
-- Purpose: Group related modifiers (e.g., "Extras", "Remove Items")
-- Strategy: Soft delete with product
-- -----------------------------------------------------------------------------
CREATE TABLE modifier_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    product_id UUID NOT NULL,

    -- Information
    name VARCHAR(100) NOT NULL,
    description TEXT,

    -- Selection Rules
    min_selections INTEGER NOT NULL DEFAULT 0 CHECK (min_selections >= 0),
    max_selections INTEGER CHECK (max_selections IS NULL OR max_selections >= 0),
    is_required BOOLEAN NOT NULL DEFAULT false,

    -- Display
    sort_order INTEGER NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- Composite unique for FK references
ALTER TABLE modifier_groups ADD CONSTRAINT uq_modifier_groups_tenant_id UNIQUE (tenant_id, id);

-- Product FK with tenant isolation
ALTER TABLE modifier_groups ADD CONSTRAINT fk_modifier_groups_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES products(tenant_id, id);

-- Validation: if required, min_selections must be >= 1
ALTER TABLE modifier_groups ADD CONSTRAINT chk_modifier_groups_required
    CHECK (NOT is_required OR min_selections >= 1);

-- Validation: max must be >= min if both specified
ALTER TABLE modifier_groups ADD CONSTRAINT chk_modifier_groups_max_min
    CHECK (max_selections IS NULL OR max_selections >= min_selections);

-- Indexes
CREATE INDEX idx_modifier_groups_product ON modifier_groups(tenant_id, product_id) WHERE deleted_at IS NULL;

COMMENT ON TABLE modifier_groups IS 'Groups of modifiers for a product. E.g., "Extras" with min=0, max=5.';
COMMENT ON COLUMN modifier_groups.max_selections IS 'NULL means unlimited selections allowed';
COMMENT ON COLUMN modifier_groups.is_required IS 'If true, customer must select at least min_selections';

-- -----------------------------------------------------------------------------
-- MODIFIERS
-- Purpose: Individual options within a modifier group
-- Strategy: Soft delete with group
-- -----------------------------------------------------------------------------
CREATE TABLE modifiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    group_id UUID NOT NULL,

    -- Information
    name VARCHAR(100) NOT NULL,

    -- Pricing
    price_adjustment DECIMAL(10,2) NOT NULL DEFAULT 0,

    -- Status
    is_default BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    sort_order INTEGER NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- Composite unique for FK references
ALTER TABLE modifiers ADD CONSTRAINT uq_modifiers_tenant_id UNIQUE (tenant_id, id);

-- Group FK with tenant isolation
ALTER TABLE modifiers ADD CONSTRAINT fk_modifiers_group
    FOREIGN KEY (tenant_id, group_id) REFERENCES modifier_groups(tenant_id, id);

-- Name unique within group
ALTER TABLE modifiers ADD CONSTRAINT uq_modifiers_group_name UNIQUE (tenant_id, group_id, name);

-- Indexes
CREATE INDEX idx_modifiers_group ON modifiers(tenant_id, group_id) WHERE deleted_at IS NULL;

COMMENT ON TABLE modifiers IS 'Individual modifier options. E.g., "Extra Cheese" +$15, "No Onion" +$0';
COMMENT ON COLUMN modifiers.price_adjustment IS 'Cost added to item price. Usually >= 0, but can be negative.';

-- -----------------------------------------------------------------------------
-- COMBOS
-- Purpose: Bundled products sold at a special price
-- Strategy: Soft delete (orders may reference combos)
-- -----------------------------------------------------------------------------
CREATE TABLE combos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    -- Information
    name VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(500),

    -- Pricing
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT true,
    sort_order INTEGER NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES users(id)
);

-- Composite unique for FK references
ALTER TABLE combos ADD CONSTRAINT uq_combos_tenant_id UNIQUE (tenant_id, id);

-- Name unique within tenant
ALTER TABLE combos ADD CONSTRAINT uq_combos_tenant_name UNIQUE (tenant_id, name);

-- Indexes
CREATE INDEX idx_combos_tenant ON combos(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_combos_active ON combos(tenant_id, is_active) WHERE deleted_at IS NULL;

COMMENT ON TABLE combos IS 'Product bundles with special pricing. E.g., "Combo 1" = Burger + Fries + Drink at $99.';

-- -----------------------------------------------------------------------------
-- COMBO ITEMS
-- Purpose: Products included in a combo
-- Strategy: Hard delete (follows combo lifecycle)
-- -----------------------------------------------------------------------------
CREATE TABLE combo_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    combo_id UUID NOT NULL,
    product_id UUID NOT NULL,

    -- Quantity
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),

    -- Substitution
    allow_substitutes BOOLEAN NOT NULL DEFAULT false,
    substitute_group VARCHAR(50),

    -- Display
    sort_order INTEGER NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Combo FK with tenant isolation (cascade delete)
ALTER TABLE combo_items ADD CONSTRAINT fk_combo_items_combo
    FOREIGN KEY (tenant_id, combo_id) REFERENCES combos(tenant_id, id) ON DELETE CASCADE;

-- Product FK with tenant isolation
ALTER TABLE combo_items ADD CONSTRAINT fk_combo_items_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES products(tenant_id, id);

-- Unique product per combo
ALTER TABLE combo_items ADD CONSTRAINT uq_combo_items_combo_product
    UNIQUE (tenant_id, combo_id, product_id);

-- Indexes
CREATE INDEX idx_combo_items_combo ON combo_items(tenant_id, combo_id);
CREATE INDEX idx_combo_items_product ON combo_items(tenant_id, product_id);

COMMENT ON TABLE combo_items IS 'Products in a combo. Each row is one product with its quantity.';
COMMENT ON COLUMN combo_items.allow_substitutes IS 'If true, customer can swap this for another product in same substitute_group';
COMMENT ON COLUMN combo_items.substitute_group IS 'Group name for substitutable items, e.g., "DRINKS" allows swapping between drinks';
