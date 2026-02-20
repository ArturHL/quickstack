-- =============================================================================
-- QuickStack POS - Database Migration V1
-- Module: Global Catalogs (No tenant_id)
-- Description: Reference tables shared by all tenants
-- =============================================================================

-- -----------------------------------------------------------------------------
-- SUBSCRIPTION PLANS
-- Purpose: Define available SaaS plans and their limits
-- Strategy: Global catalog, no tenant_id
-- -----------------------------------------------------------------------------
CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    price_monthly_mxn DECIMAL(10,2) NOT NULL,
    max_branches INTEGER NOT NULL DEFAULT 1,
    max_users_per_branch INTEGER NOT NULL DEFAULT 5,
    features JSONB NOT NULL DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE subscription_plans IS 'SaaS pricing plans - global catalog shared by all tenants';
COMMENT ON COLUMN subscription_plans.features IS 'JSON with feature flags: {"kds": true, "whatsapp_bot": true, "reports": true}';

-- -----------------------------------------------------------------------------
-- ROLES
-- Purpose: System roles for authorization
-- Strategy: Global catalog, predefined roles only
-- -----------------------------------------------------------------------------
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    code VARCHAR(30) NOT NULL UNIQUE,
    description TEXT,
    permissions JSONB NOT NULL DEFAULT '[]',
    is_system BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE roles IS 'System roles - OWNER, CASHIER, KITCHEN. Users have exactly one role.';
COMMENT ON COLUMN roles.permissions IS 'JSON array of permission strings: ["pos:read", "pos:write", "products:manage"]';
COMMENT ON COLUMN roles.is_system IS 'True for built-in roles that cannot be deleted';

-- -----------------------------------------------------------------------------
-- ORDER STATUS TYPES
-- Purpose: Define valid order statuses for state machine
-- Strategy: Global catalog, defines workflow
-- -----------------------------------------------------------------------------
CREATE TABLE order_status_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    color_hex VARCHAR(7) DEFAULT '#808080',
    sequence INTEGER NOT NULL,
    is_terminal BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE order_status_types IS 'Order workflow states: PENDING -> IN_PROGRESS -> READY -> DELIVERED -> COMPLETED';
COMMENT ON COLUMN order_status_types.sequence IS 'Order in workflow (1=first, higher=later states)';
COMMENT ON COLUMN order_status_types.is_terminal IS 'True if order cannot change after reaching this status';

-- -----------------------------------------------------------------------------
-- STOCK MOVEMENT TYPES
-- Purpose: Categorize inventory changes
-- Strategy: Global catalog
-- -----------------------------------------------------------------------------
CREATE TABLE stock_movement_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    affects_stock INTEGER NOT NULL CHECK (affects_stock IN (-1, 1)),
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE stock_movement_types IS 'Types of inventory movements';
COMMENT ON COLUMN stock_movement_types.affects_stock IS '1 = adds to stock, -1 = removes from stock';

-- -----------------------------------------------------------------------------
-- UNIT TYPES
-- Purpose: Measurement units for ingredients
-- Strategy: Global catalog
-- -----------------------------------------------------------------------------
CREATE TABLE unit_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    abbreviation VARCHAR(10) NOT NULL,
    category VARCHAR(30) NOT NULL CHECK (category IN ('WEIGHT', 'VOLUME', 'COUNT', 'LENGTH')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE unit_types IS 'Measurement units: KG, G, L, ML, UNIT, etc.';
