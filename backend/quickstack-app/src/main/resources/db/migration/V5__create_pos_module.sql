-- =============================================================================
-- QuickStack POS - Database Migration V5
-- Module: POS (Areas, Tables, Customers, Orders, Payments)
-- Description: Point of sale operations and order management
-- =============================================================================

-- -----------------------------------------------------------------------------
-- AREAS
-- Purpose: Restaurant zones (Terraza, Interior, Barra)
-- Strategy: Soft delete (tables reference areas)
-- -----------------------------------------------------------------------------
CREATE TABLE areas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    branch_id UUID NOT NULL,

    -- Information
    name VARCHAR(100) NOT NULL,
    description TEXT,

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
ALTER TABLE areas ADD CONSTRAINT uq_areas_tenant_id UNIQUE (tenant_id, id);

-- FK with tenant isolation
ALTER TABLE areas ADD CONSTRAINT fk_areas_branch
    FOREIGN KEY (tenant_id, branch_id) REFERENCES branches(tenant_id, id);

-- Name unique within branch
ALTER TABLE areas ADD CONSTRAINT uq_areas_branch_name UNIQUE (tenant_id, branch_id, name);

-- Indexes
CREATE INDEX idx_areas_branch ON areas(tenant_id, branch_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_areas_sort ON areas(tenant_id, branch_id, sort_order) WHERE deleted_at IS NULL;

COMMENT ON TABLE areas IS 'Restaurant zones/sections. Tables belong to areas.';

-- -----------------------------------------------------------------------------
-- TABLES
-- Purpose: Physical or virtual tables/positions
-- Strategy: Soft delete (orders reference tables)
-- -----------------------------------------------------------------------------
CREATE TABLE tables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    area_id UUID NOT NULL,

    -- Information
    number VARCHAR(20) NOT NULL,
    name VARCHAR(100),
    capacity INTEGER CHECK (capacity > 0),

    -- Status
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'MAINTENANCE')),

    -- Display
    sort_order INTEGER NOT NULL DEFAULT 0,
    position_x INTEGER,
    position_y INTEGER,
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
ALTER TABLE tables ADD CONSTRAINT uq_tables_tenant_id UNIQUE (tenant_id, id);

-- FK with tenant isolation
ALTER TABLE tables ADD CONSTRAINT fk_tables_area
    FOREIGN KEY (tenant_id, area_id) REFERENCES areas(tenant_id, id);

-- Number unique within area
ALTER TABLE tables ADD CONSTRAINT uq_tables_area_number UNIQUE (tenant_id, area_id, number);

-- Indexes
CREATE INDEX idx_tables_area ON tables(tenant_id, area_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_tables_status ON tables(tenant_id, status) WHERE deleted_at IS NULL AND is_active = true;

COMMENT ON TABLE tables IS 'Restaurant tables/positions. Used for dine-in service.';
COMMENT ON COLUMN tables.number IS 'Display number like "1", "2A", "Barra-3"';
COMMENT ON COLUMN tables.position_x IS 'X coordinate for visual floor plan (optional)';

-- -----------------------------------------------------------------------------
-- CUSTOMERS
-- Purpose: Contact info for delivery and digital receipts
-- Strategy: Soft delete (GDPR-style data handling)
-- -----------------------------------------------------------------------------
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    -- Information
    name VARCHAR(255),
    phone VARCHAR(20),
    email VARCHAR(255),
    whatsapp VARCHAR(20),

    -- Delivery Address
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(100),
    city VARCHAR(100),
    postal_code VARCHAR(10),
    delivery_notes TEXT,

    -- Preferences
    preferences JSONB DEFAULT '{}',

    -- Stats (denormalized for quick access)
    total_orders INTEGER NOT NULL DEFAULT 0,
    total_spent DECIMAL(12,2) NOT NULL DEFAULT 0,
    last_order_at TIMESTAMPTZ,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES users(id)
);

-- Composite unique for FK references
ALTER TABLE customers ADD CONSTRAINT uq_customers_tenant_id UNIQUE (tenant_id, id);

-- Phone and email unique within tenant (allowing nulls)
CREATE UNIQUE INDEX uq_customers_tenant_phone ON customers(tenant_id, phone)
    WHERE phone IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_customers_tenant_email ON customers(tenant_id, email)
    WHERE email IS NOT NULL AND deleted_at IS NULL;

-- Must have at least one contact method
ALTER TABLE customers ADD CONSTRAINT chk_customers_has_contact CHECK (
    phone IS NOT NULL OR email IS NOT NULL OR whatsapp IS NOT NULL
);

-- Indexes
CREATE INDEX idx_customers_tenant ON customers(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_phone ON customers(tenant_id, phone) WHERE deleted_at IS NULL AND phone IS NOT NULL;
CREATE INDEX idx_customers_whatsapp ON customers(tenant_id, whatsapp) WHERE deleted_at IS NULL AND whatsapp IS NOT NULL;

COMMENT ON TABLE customers IS 'Customer contact data for delivery and digital receipts.';
COMMENT ON COLUMN customers.preferences IS 'JSON: {"favorite_items": [...], "dietary_restrictions": [...]}';

-- -----------------------------------------------------------------------------
-- ORDERS
-- Purpose: Sales transactions
-- Strategy: Never delete (financial records)
-- Decision: Prices denormalized to preserve historical accuracy
-- -----------------------------------------------------------------------------
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    branch_id UUID NOT NULL,

    -- References (nullable for different service types)
    table_id UUID,
    customer_id UUID,

    -- Order Identification
    order_number VARCHAR(50) NOT NULL,
    daily_sequence INTEGER NOT NULL,

    -- Service Type
    service_type VARCHAR(30) NOT NULL DEFAULT 'COUNTER'
        CHECK (service_type IN ('DINE_IN', 'COUNTER', 'DELIVERY', 'TAKEOUT')),

    -- Status
    status_id UUID NOT NULL REFERENCES order_status_types(id),

    -- Financials (denormalized for historical accuracy)
    subtotal DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    tax_rate DECIMAL(5,4) NOT NULL,
    tax DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (tax >= 0),
    discount DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (discount >= 0),
    total DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (total >= 0),

    -- Source
    source VARCHAR(30) NOT NULL DEFAULT 'POS'
        CHECK (source IN ('POS', 'WHATSAPP', 'WEB', 'PHONE')),

    -- Notes
    notes TEXT,
    kitchen_notes TEXT,

    -- Timestamps
    opened_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMPTZ,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

-- Composite unique for FK references
ALTER TABLE orders ADD CONSTRAINT uq_orders_tenant_id UNIQUE (tenant_id, id);

-- Order number unique within tenant
ALTER TABLE orders ADD CONSTRAINT uq_orders_tenant_number UNIQUE (tenant_id, order_number);

-- FKs with tenant isolation
ALTER TABLE orders ADD CONSTRAINT fk_orders_branch
    FOREIGN KEY (tenant_id, branch_id) REFERENCES branches(tenant_id, id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_table
    FOREIGN KEY (tenant_id, table_id) REFERENCES tables(tenant_id, id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_customer
    FOREIGN KEY (tenant_id, customer_id) REFERENCES customers(tenant_id, id);

-- Indexes
CREATE INDEX idx_orders_tenant ON orders(tenant_id);
CREATE INDEX idx_orders_branch ON orders(tenant_id, branch_id);
CREATE INDEX idx_orders_status ON orders(tenant_id, status_id);
CREATE INDEX idx_orders_date ON orders(tenant_id, opened_at);
CREATE INDEX idx_orders_daily ON orders(tenant_id, branch_id, DATE(opened_at));
CREATE INDEX idx_orders_table ON orders(tenant_id, table_id) WHERE table_id IS NOT NULL;
CREATE INDEX idx_orders_customer ON orders(tenant_id, customer_id) WHERE customer_id IS NOT NULL;
CREATE INDEX idx_orders_service_type ON orders(tenant_id, service_type);
CREATE INDEX idx_orders_source ON orders(tenant_id, source);

COMMENT ON TABLE orders IS 'Sales transactions. Never deleted for audit purposes.';
COMMENT ON COLUMN orders.order_number IS 'Human-readable order ID, e.g., "ORD-20260205-001"';
COMMENT ON COLUMN orders.daily_sequence IS 'Sequential number within branch+date, resets daily. For KDS display.';
COMMENT ON COLUMN orders.tax_rate IS 'Tax rate at time of order (copied from tenant settings)';

-- -----------------------------------------------------------------------------
-- ORDER ITEMS
-- Purpose: Line items in an order
-- Strategy: Never delete (part of order record)
-- Decision: Product/variant names and prices COPIED to preserve history
-- -----------------------------------------------------------------------------
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    order_id UUID NOT NULL,

    -- Product Reference (for linking, not for display/pricing)
    product_id UUID,
    variant_id UUID,
    combo_id UUID,

    -- Denormalized Product Info (HISTORICAL - DO NOT UPDATE)
    product_name VARCHAR(255) NOT NULL,
    variant_name VARCHAR(100),

    -- Pricing (HISTORICAL - copied at order time)
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price >= 0),
    modifiers_total DECIMAL(10,2) NOT NULL DEFAULT 0,
    line_total DECIMAL(12,2) GENERATED ALWAYS AS (quantity * (unit_price + modifiers_total)) STORED,

    -- KDS Status (for kitchen display)
    kds_status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (kds_status IN ('PENDING', 'PREPARING', 'READY', 'DELIVERED')),
    kds_sent_at TIMESTAMPTZ,
    kds_ready_at TIMESTAMPTZ,

    -- Notes
    notes TEXT,

    -- Sequence for display order
    sort_order INTEGER NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Composite unique for FK references
ALTER TABLE order_items ADD CONSTRAINT uq_order_items_tenant_id UNIQUE (tenant_id, id);

-- FKs with tenant isolation
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_order
    FOREIGN KEY (tenant_id, order_id) REFERENCES orders(tenant_id, id);
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_product
    FOREIGN KEY (tenant_id, product_id) REFERENCES products(tenant_id, id);
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_variant
    FOREIGN KEY (tenant_id, variant_id) REFERENCES product_variants(tenant_id, id);
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_combo
    FOREIGN KEY (tenant_id, combo_id) REFERENCES combos(tenant_id, id);

-- Must have either product or combo (not both, not neither)
ALTER TABLE order_items ADD CONSTRAINT chk_order_items_product_or_combo CHECK (
    (product_id IS NOT NULL AND combo_id IS NULL) OR
    (product_id IS NULL AND combo_id IS NOT NULL)
);

-- Indexes
CREATE INDEX idx_order_items_order ON order_items(tenant_id, order_id);
CREATE INDEX idx_order_items_product ON order_items(tenant_id, product_id) WHERE product_id IS NOT NULL;
CREATE INDEX idx_order_items_kds ON order_items(tenant_id, kds_status) WHERE kds_status != 'DELIVERED';
CREATE INDEX idx_order_items_kds_pending ON order_items(tenant_id, kds_status, created_at)
    WHERE kds_status IN ('PENDING', 'PREPARING');

COMMENT ON TABLE order_items IS 'Line items in orders. Prices denormalized to preserve history.';
COMMENT ON COLUMN order_items.product_name IS 'HISTORICAL: Product name at time of order. DO NOT UPDATE if product changes.';
COMMENT ON COLUMN order_items.unit_price IS 'HISTORICAL: Price at time of order. DO NOT UPDATE if product price changes.';
COMMENT ON COLUMN order_items.kds_status IS 'Kitchen Display System status for this item.';

-- -----------------------------------------------------------------------------
-- ORDER ITEM MODIFIERS
-- Purpose: Modifiers applied to order items
-- Strategy: Never delete (part of order record)
-- Decision: Modifier names and prices COPIED to preserve history
-- -----------------------------------------------------------------------------
CREATE TABLE order_item_modifiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    order_item_id UUID NOT NULL,

    -- Modifier Reference
    modifier_id UUID,

    -- Denormalized Modifier Info (HISTORICAL)
    modifier_name VARCHAR(100) NOT NULL,
    price_adjustment DECIMAL(10,2) NOT NULL DEFAULT 0,

    -- Quantity (for modifiers that can be doubled, etc.)
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- FKs with tenant isolation
ALTER TABLE order_item_modifiers ADD CONSTRAINT fk_oim_order_item
    FOREIGN KEY (tenant_id, order_item_id) REFERENCES order_items(tenant_id, id);
ALTER TABLE order_item_modifiers ADD CONSTRAINT fk_oim_modifier
    FOREIGN KEY (tenant_id, modifier_id) REFERENCES modifiers(tenant_id, id);

-- Indexes
CREATE INDEX idx_oim_order_item ON order_item_modifiers(tenant_id, order_item_id);

COMMENT ON TABLE order_item_modifiers IS 'Modifiers applied to order items. Prices denormalized for history.';

-- -----------------------------------------------------------------------------
-- PAYMENTS
-- Purpose: Payment records for orders
-- Strategy: Never delete (financial records)
-- -----------------------------------------------------------------------------
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    order_id UUID NOT NULL,

    -- Payment Info
    amount DECIMAL(12,2) NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(30) NOT NULL DEFAULT 'CASH'
        CHECK (payment_method IN ('CASH', 'CARD', 'TRANSFER', 'OTHER')),

    -- For cash payments
    amount_received DECIMAL(12,2),
    change_given DECIMAL(12,2),

    -- Status
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED'
        CHECK (status IN ('PENDING', 'COMPLETED', 'REFUNDED', 'FAILED')),

    -- Reference
    reference_number VARCHAR(100),
    notes TEXT,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id)
);

-- FKs with tenant isolation
ALTER TABLE payments ADD CONSTRAINT fk_payments_order
    FOREIGN KEY (tenant_id, order_id) REFERENCES orders(tenant_id, id);

-- For cash, amount_received is required
ALTER TABLE payments ADD CONSTRAINT chk_payments_cash_received CHECK (
    payment_method != 'CASH' OR amount_received IS NOT NULL
);

-- Indexes
CREATE INDEX idx_payments_order ON payments(tenant_id, order_id);
CREATE INDEX idx_payments_date ON payments(tenant_id, created_at);
CREATE INDEX idx_payments_method ON payments(tenant_id, payment_method);
CREATE INDEX idx_payments_status ON payments(tenant_id, status);

COMMENT ON TABLE payments IS 'Payment records. MVP supports only CASH.';
COMMENT ON COLUMN payments.amount_received IS 'For cash: amount customer gave. NULL for other methods.';

-- -----------------------------------------------------------------------------
-- ORDER STATUS HISTORY
-- Purpose: Track order status changes for KDS and audit
-- Strategy: Never delete (audit log)
-- -----------------------------------------------------------------------------
CREATE TABLE order_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    order_id UUID NOT NULL,
    status_id UUID NOT NULL REFERENCES order_status_types(id),

    -- Context
    changed_by UUID REFERENCES users(id),
    notes TEXT,

    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- FKs with tenant isolation
ALTER TABLE order_status_history ADD CONSTRAINT fk_osh_order
    FOREIGN KEY (tenant_id, order_id) REFERENCES orders(tenant_id, id);

-- Indexes
CREATE INDEX idx_osh_order ON order_status_history(tenant_id, order_id);
CREATE INDEX idx_osh_date ON order_status_history(tenant_id, created_at);
CREATE INDEX idx_osh_status ON order_status_history(tenant_id, status_id);

COMMENT ON TABLE order_status_history IS 'Audit log of order status transitions for KDS and analytics.';
