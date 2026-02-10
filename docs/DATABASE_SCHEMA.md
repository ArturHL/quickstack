# QuickStack POS - Database Schema Design

> **Version:** 1.0.0
> **Last Updated:** 2026-02-05
> **Author:** Senior Software Architect
> **Status:** Ready for Implementation

---

## Table of Contents

1. [Design Principles](#design-principles)
2. [ERD Diagram](#erd-diagram)
3. [Module 1: Core (Tenant, Branch, User)](#module-1-core)
4. [Module 2: Catalog (Products)](#module-2-catalog)
5. [Module 3: Inventory](#module-3-inventory)
6. [Module 4: POS (Orders)](#module-4-pos)
7. [Module 5: Notifications](#module-5-notifications)
8. [Seed Data](#seed-data)
9. [Design Decisions](#design-decisions)
10. [Migration Strategy](#migration-strategy)

---

## Design Principles

### Multi-tenancy Strategy

**Approach:** Shared database with `tenant_id` discriminator column.

**Rationale:**
- Cost-effective for SaaS with many small tenants
- Simpler operations (one database to manage)
- Easy to query across tenants for analytics (with proper authorization)

**Implementation:**
- Every table (except global catalogs) has `tenant_id UUID NOT NULL`
- All queries MUST include `tenant_id` filter (enforced at application layer via JPA filters)
- Foreign keys include tenant_id in composite references to prevent cross-tenant data access

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Tables | snake_case, plural | `order_items` |
| Columns | snake_case | `created_at` |
| Primary Keys | `id` (UUID) | `id UUID PRIMARY KEY` |
| Foreign Keys | `{singular}_id` | `product_id`, `branch_id` |
| Indexes | `idx_{table}_{columns}` | `idx_orders_tenant_branch` |
| Unique Constraints | `uq_{table}_{columns}` | `uq_users_tenant_email` |
| Check Constraints | `chk_{table}_{description}` | `chk_orders_total_positive` |

### Audit Columns (All Tables)

```sql
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
created_by UUID,  -- NULL for system-generated records
updated_by UUID
```

### Soft Delete Strategy

| Entity | Strategy | Reason |
|--------|----------|--------|
| Tenants | Soft delete | Legal/audit requirements |
| Branches | Soft delete | Historical orders reference them |
| Users | Soft delete | Audit trail for actions |
| Products | Soft delete | Orders reference historical products |
| Categories | Soft delete | Products reference them |
| Orders | Never delete | Financial records |
| Ingredients | Soft delete | Recipes reference them |
| Customers | Soft delete | GDPR-style compliance |

---

## ERD Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              GLOBAL CATALOGS (No tenant_id)                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐          │
│  │ subscription_    │    │     roles        │    │  order_status    │          │
│  │     plans        │    │                  │    │    _types        │          │
│  ├──────────────────┤    ├──────────────────┤    ├──────────────────┤          │
│  │ id (PK)          │    │ id (PK)          │    │ id (PK)          │          │
│  │ name             │    │ name             │    │ code             │          │
│  │ price_monthly    │    │ description      │    │ name             │          │
│  │ max_branches     │    │ permissions      │    │ sequence         │          │
│  │ max_users        │    └──────────────────┘    └──────────────────┘          │
│  │ features         │                                                           │
│  └──────────────────┘    ┌──────────────────┐    ┌──────────────────┐          │
│                          │ stock_movement   │    │   unit_types     │          │
│                          │    _types        │    │                  │          │
│                          ├──────────────────┤    ├──────────────────┤          │
│                          │ id (PK)          │    │ id (PK)          │          │
│                          │ code             │    │ code             │          │
│                          │ name             │    │ name             │          │
│                          │ affects_stock    │    │ abbreviation     │          │
│                          └──────────────────┘    └──────────────────┘          │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              MODULE 1: CORE                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌──────────────────┐         ┌──────────────────┐                              │
│  │     tenants      │────────<│    branches      │                              │
│  ├──────────────────┤    1:N  ├──────────────────┤                              │
│  │ id (PK)          │         │ id (PK)          │                              │
│  │ name             │         │ tenant_id (FK)   │                              │
│  │ slug             │         │ name             │                              │
│  │ plan_id (FK)     │         │ address          │                              │
│  │ status           │         │ phone            │                              │
│  │ settings         │         │ is_active        │                              │
│  └────────┬─────────┘         └────────┬─────────┘                              │
│           │                            │                                         │
│           │ 1:N                        │ 1:N                                     │
│           ▼                            ▼                                         │
│  ┌──────────────────┐         ┌──────────────────┐    ┌──────────────────┐      │
│  │     users        │────────>│ password_reset   │    │  login_attempts  │      │
│  ├──────────────────┤    1:N  │    _tokens       │    ├──────────────────┤      │
│  │ id (PK)          │         ├──────────────────┤    │ id (PK)          │      │
│  │ tenant_id (FK)   │         │ id (PK)          │    │ email            │      │
│  │ branch_id (FK)   │         │ user_id (FK)     │    │ user_id (FK)     │      │
│  │ role_id (FK)     │         │ token_hash       │    │ ip_address       │      │
│  │ email            │         │ expires_at       │    │ success          │      │
│  │ full_name        │         └──────────────────┘    │ failure_reason   │      │
│  │ password_hash    │                                 └──────────────────┘      │
│  │ is_active        │         ┌──────────────────┐                              │
│  │ email_verified   │────────>│  refresh_tokens  │                              │
│  │ locked_until     │    1:N  ├──────────────────┤                              │
│  └──────────────────┘         │ id (PK)          │                              │
│                               │ user_id (FK)     │                              │
│                               │ token_hash       │                              │
│                               │ family_id        │                              │
│                               │ expires_at       │                              │
│                               └──────────────────┘                              │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              MODULE 2: CATALOG                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌──────────────────┐         ┌──────────────────┐                              │
│  │   categories     │◄───────┐│    products      │                              │
│  ├──────────────────┤   N:1  │├──────────────────┤                              │
│  │ id (PK)          │        ││ id (PK)          │                              │
│  │ tenant_id        │        ││ tenant_id        │                              │
│  │ parent_id (FK)───┼────────┘│ category_id (FK) │                              │
│  │ name             │         │ name             │                              │
│  │ sort_order       │         │ description      │                              │
│  └──────────────────┘         │ base_price       │                              │
│                               │ is_active        │                              │
│         ┌─────────────────────┤ product_type     │                              │
│         │                     └────────┬─────────┘                              │
│         │ 1:N                          │                                         │
│         ▼                              │ 1:N                                     │
│  ┌──────────────────┐                  ▼                                         │
│  │ modifier_groups  │         ┌──────────────────┐                              │
│  ├──────────────────┤         │ product_variants │                              │
│  │ id (PK)          │         ├──────────────────┤                              │
│  │ tenant_id        │         │ id (PK)          │                              │
│  │ product_id (FK)  │         │ tenant_id        │                              │
│  │ name             │         │ product_id (FK)  │                              │
│  │ min_selections   │         │ sku              │                              │
│  │ max_selections   │         │ name             │                              │
│  │ is_required      │         │ price_adjustment │                              │
│  └────────┬─────────┘         │ is_default       │                              │
│           │ 1:N               └──────────────────┘                              │
│           ▼                                                                      │
│  ┌──────────────────┐                                                           │
│  │   modifiers      │         ┌──────────────────┐                              │
│  ├──────────────────┤         │     combos       │                              │
│  │ id (PK)          │         ├──────────────────┤                              │
│  │ tenant_id        │         │ id (PK)          │                              │
│  │ group_id (FK)    │         │ tenant_id        │                              │
│  │ name             │         │ name             │                              │
│  │ price_adjustment │         │ price            │                              │
│  │ is_default       │         │ is_active        │                              │
│  └──────────────────┘         └────────┬─────────┘                              │
│                                        │ 1:N                                     │
│                                        ▼                                         │
│                               ┌──────────────────┐                              │
│                               │   combo_items    │                              │
│                               ├──────────────────┤                              │
│                               │ id (PK)          │                              │
│                               │ tenant_id        │                              │
│                               │ combo_id (FK)    │                              │
│                               │ product_id (FK)  │                              │
│                               │ quantity         │                              │
│                               │ allow_substitutes│                              │
│                               └──────────────────┘                              │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              MODULE 3: INVENTORY                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌──────────────────┐         ┌──────────────────┐                              │
│  │   suppliers      │────────<│ purchase_orders  │                              │
│  ├──────────────────┤   1:N   ├──────────────────┤                              │
│  │ id (PK)          │         │ id (PK)          │                              │
│  │ tenant_id        │         │ tenant_id        │                              │
│  │ name             │         │ supplier_id (FK) │                              │
│  │ contact_name     │         │ branch_id (FK)   │                              │
│  │ phone            │         │ status           │                              │
│  │ email            │         │ total_amount     │                              │
│  └──────────────────┘         └────────┬─────────┘                              │
│                                        │ 1:N                                     │
│  ┌──────────────────┐                  ▼                                         │
│  │   ingredients    │         ┌──────────────────┐                              │
│  ├──────────────────┤         │purchase_order_   │                              │
│  │ id (PK)          │         │     items        │                              │
│  │ tenant_id        │         ├──────────────────┤                              │
│  │ name             │         │ id (PK)          │                              │
│  │ unit_type_id(FK) │         │ tenant_id        │                              │
│  │ cost_per_unit    │         │ order_id (FK)    │                              │
│  │ min_stock_level  │         │ ingredient_id(FK)│                              │
│  │ current_stock    │◄────────│ quantity         │                              │
│  └────────┬─────────┘         │ unit_cost        │                              │
│           │                   └──────────────────┘                              │
│           │ 1:N                                                                  │
│           ▼                                                                      │
│  ┌──────────────────┐         ┌──────────────────┐                              │
│  │    recipes       │         │ stock_movements  │                              │
│  ├──────────────────┤         ├──────────────────┤                              │
│  │ id (PK)          │         │ id (PK)          │                              │
│  │ tenant_id        │         │ tenant_id        │                              │
│  │ product_id (FK)  │         │ ingredient_id(FK)│                              │
│  │ variant_id (FK)  │         │ branch_id (FK)   │                              │
│  │ ingredient_id(FK)│         │ movement_type_id │                              │
│  │ quantity         │         │ quantity         │                              │
│  │                  │         │ reference_type   │                              │
│  └──────────────────┘         │ reference_id     │                              │
│                               └──────────────────┘                              │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              MODULE 4: POS (Orders)                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌──────────────────┐         ┌──────────────────┐                              │
│  │     areas        │────────<│     tables       │                              │
│  ├──────────────────┤   1:N   ├──────────────────┤                              │
│  │ id (PK)          │         │ id (PK)          │                              │
│  │ tenant_id        │         │ tenant_id        │                              │
│  │ branch_id (FK)   │         │ area_id (FK)     │                              │
│  │ name             │         │ number           │                              │
│  │ sort_order       │         │ capacity         │                              │
│  └──────────────────┘         │ status           │                              │
│                               └────────┬─────────┘                              │
│                                        │                                         │
│                                        │ 0:N                                     │
│                                        ▼                                         │
│  ┌──────────────────┐         ┌──────────────────┐        ┌──────────────────┐ │
│  │   customers      │────────<│     orders       │───────>│    payments      │ │
│  ├──────────────────┤  0:N    ├──────────────────┤  1:N   ├──────────────────┤ │
│  │ id (PK)          │         │ id (PK)          │        │ id (PK)          │ │
│  │ tenant_id        │         │ tenant_id        │        │ tenant_id        │ │
│  │ name             │         │ branch_id (FK)   │        │ order_id (FK)    │ │
│  │ phone            │         │ table_id (FK)    │        │ amount           │ │
│  │ email            │         │ customer_id (FK) │        │ payment_method   │ │
│  │ whatsapp         │         │ order_number     │        │ status           │ │
│  └──────────────────┘         │ service_type     │        └──────────────────┘ │
│                               │ status_id (FK)   │                              │
│                               │ subtotal         │                              │
│                               │ tax              │                              │
│                               │ total            │                              │
│                               └────────┬─────────┘                              │
│                                        │                                         │
│           ┌────────────────────────────┼────────────────────────────┐           │
│           │ 1:N                        │ 1:N                        │           │
│           ▼                            ▼                            ▼           │
│  ┌──────────────────┐         ┌──────────────────┐        ┌──────────────────┐ │
│  │   order_items    │         │order_status_     │        │ order_item_      │ │
│  ├──────────────────┤         │    history       │        │   modifiers      │ │
│  │ id (PK)          │         ├──────────────────┤        ├──────────────────┤ │
│  │ tenant_id        │         │ id (PK)          │        │ id (PK)          │ │
│  │ order_id (FK)    │         │ tenant_id        │        │ tenant_id        │ │
│  │ product_id (FK)  │         │ order_id (FK)    │        │ order_item_id(FK)│ │
│  │ variant_id (FK)  │         │ status_id (FK)   │        │ modifier_id (FK) │ │
│  │ combo_id (FK)    │         │ changed_by (FK)  │        │ modifier_name    │ │
│  │ quantity         │         │ notes            │        │ price_adjustment │ │
│  │ unit_price       │◄────────┤                  │        └──────────────────┘ │
│  │ product_name     │         └──────────────────┘                              │
│  │ variant_name     │                                                           │
│  │ notes            │         (Prices denormalized for                          │
│  │ kds_status       │          historical accuracy)                             │
│  └──────────────────┘                                                           │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              MODULE 5: NOTIFICATIONS                             │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌──────────────────┐         ┌──────────────────┐                              │
│  │   customers      │────────<│notification_logs │                              │
│  │   (from POS)     │   1:N   ├──────────────────┤                              │
│  │                  │         │ id (PK)          │                              │
│  │                  │         │ tenant_id        │                              │
│  │                  │         │ customer_id (FK) │                              │
│  │                  │         │ order_id (FK)    │                              │
│  │                  │         │ channel          │                              │
│  │                  │         │ recipient        │                              │
│  │                  │         │ content_type     │                              │
│  │                  │         │ status           │                              │
│  │                  │         │ sent_at          │                              │
│  │                  │         │ error_message    │                              │
│  └──────────────────┘         └──────────────────┘                              │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Module 1: Core

### 1.1 Global Catalogs (No tenant_id)

```sql
-- =============================================================================
-- SUBSCRIPTION PLANS
-- Purpose: Define available SaaS plans and their limits
-- Strategy: Global catalog, no tenant_id
-- =============================================================================
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

-- =============================================================================
-- ROLES
-- Purpose: System roles for authorization
-- Strategy: Global catalog, predefined roles only
-- =============================================================================
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

-- =============================================================================
-- ORDER STATUS TYPES
-- Purpose: Define valid order statuses for state machine
-- Strategy: Global catalog, defines workflow
-- =============================================================================
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

-- =============================================================================
-- STOCK MOVEMENT TYPES
-- Purpose: Categorize inventory changes
-- Strategy: Global catalog
-- =============================================================================
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

-- =============================================================================
-- UNIT TYPES
-- Purpose: Measurement units for ingredients
-- Strategy: Global catalog
-- =============================================================================
CREATE TABLE unit_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    abbreviation VARCHAR(10) NOT NULL,
    category VARCHAR(30) NOT NULL CHECK (category IN ('WEIGHT', 'VOLUME', 'COUNT', 'LENGTH')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE unit_types IS 'Measurement units: KG, G, L, ML, UNIT, etc.';
```

### 1.2 Tenants

```sql
-- =============================================================================
-- TENANTS
-- Purpose: Root entity for multi-tenancy - represents a restaurant business
-- Strategy: Soft delete (legal/audit requirements)
-- =============================================================================
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Business Information
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    legal_name VARCHAR(255),
    rfc VARCHAR(13),

    -- Subscription
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'CANCELLED')),
    trial_ends_at TIMESTAMPTZ,

    -- Settings
    settings JSONB NOT NULL DEFAULT '{}',
    timezone VARCHAR(50) NOT NULL DEFAULT 'America/Mexico_City',
    currency VARCHAR(3) NOT NULL DEFAULT 'MXN',
    tax_rate DECIMAL(5,4) NOT NULL DEFAULT 0.16,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,

    -- Constraints
    CONSTRAINT chk_tenants_slug_format CHECK (slug ~ '^[a-z0-9-]+$')
);

-- Indexes
CREATE INDEX idx_tenants_slug ON tenants(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_tenants_status ON tenants(status) WHERE deleted_at IS NULL;

COMMENT ON TABLE tenants IS 'Restaurant business entity - root of all tenant-scoped data';
COMMENT ON COLUMN tenants.slug IS 'URL-safe identifier, lowercase alphanumeric with hyphens';
COMMENT ON COLUMN tenants.settings IS 'JSON config: {"receipt_header": "...", "default_language": "es"}';
COMMENT ON COLUMN tenants.tax_rate IS 'IVA rate as decimal: 0.16 = 16%';
```

### 1.3 Branches

```sql
-- =============================================================================
-- BRANCHES
-- Purpose: Physical locations (sucursales) belonging to a tenant
-- Strategy: Soft delete (orders reference branches historically)
-- =============================================================================
CREATE TABLE branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    -- Information
    name VARCHAR(255) NOT NULL,
    code VARCHAR(20),

    -- Contact & Location
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(100),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(10),
    phone VARCHAR(20),
    email VARCHAR(255),

    -- Operations
    is_active BOOLEAN NOT NULL DEFAULT true,
    timezone VARCHAR(50),
    settings JSONB NOT NULL DEFAULT '{}',

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,

    -- Constraints
    CONSTRAINT uq_branches_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uq_branches_tenant_name UNIQUE (tenant_id, name)
);

-- Indexes
CREATE INDEX idx_branches_tenant ON branches(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_branches_active ON branches(tenant_id, is_active) WHERE deleted_at IS NULL;

COMMENT ON TABLE branches IS 'Physical restaurant locations. Users are assigned to one branch.';
COMMENT ON COLUMN branches.code IS 'Short internal code (e.g., "CENTRO", "NORTE"). Unique within tenant.';
COMMENT ON COLUMN branches.settings IS 'Branch-specific settings: {"printer_ip": "192.168.1.100"}';
```

### 1.4 Users

```sql
-- =============================================================================
-- USERS
-- Purpose: System users with native authentication (no external IdP)
-- Strategy: Soft delete (audit trail for actions)
-- Security: OWASP ASVS L2 compliant password storage and account security
-- =============================================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    branch_id UUID,
    role_id UUID NOT NULL REFERENCES roles(id),

    -- Identity
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    avatar_url VARCHAR(500),

    -- Authentication (ASVS V2.4 - Credential Storage)
    password_hash VARCHAR(255) NOT NULL,
    password_changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    must_change_password BOOLEAN NOT NULL DEFAULT false,

    -- Account Security (ASVS V2.2 - Anti-automation)
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_failed_login_at TIMESTAMPTZ,

    -- Email Verification (ASVS V2.1)
    email_verified BOOLEAN NOT NULL DEFAULT false,
    email_verification_token VARCHAR(100),
    email_verification_expires_at TIMESTAMPTZ,

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_login_at TIMESTAMPTZ,
    last_login_ip INET,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,

    -- Constraints
    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT fk_users_branch FOREIGN KEY (tenant_id, branch_id)
        REFERENCES branches(tenant_id, id) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT chk_users_failed_attempts CHECK (failed_login_attempts >= 0)
);

-- Indexes
CREATE INDEX idx_users_tenant ON users(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_tenant_branch ON users(tenant_id, branch_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_role ON users(role_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_locked ON users(locked_until) WHERE locked_until IS NOT NULL AND deleted_at IS NULL;

COMMENT ON TABLE users IS 'System users with native authentication. Passwords stored as Argon2id hash.';
COMMENT ON COLUMN users.branch_id IS 'NULL for OWNER role (access to all branches), required for CASHIER/KITCHEN';
COMMENT ON COLUMN users.role_id IS 'Single role per user. OWNER/CASHIER/KITCHEN in MVP.';
COMMENT ON COLUMN users.password_hash IS 'Argon2id hash. Never store plaintext. ASVS 2.4.1 compliant.';
COMMENT ON COLUMN users.failed_login_attempts IS 'Reset to 0 on successful login. Lock account after threshold.';
COMMENT ON COLUMN users.locked_until IS 'Account locked until this time. NULL = not locked.';
COMMENT ON COLUMN users.must_change_password IS 'Force password change on next login (admin reset, first login).';
```

### 1.5 Password Reset Tokens

```sql
-- =============================================================================
-- PASSWORD RESET TOKENS
-- Purpose: Secure password recovery flow (ASVS V2.5)
-- Strategy: Hard delete after use or expiration
-- Security: Tokens are hashed, single-use, time-limited
-- =============================================================================
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Token (hashed for security - ASVS 2.5.1)
    token_hash VARCHAR(255) NOT NULL,

    -- Lifecycle
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_ip INET,

    -- Constraints
    CONSTRAINT uq_password_reset_token UNIQUE (token_hash)
);

-- Indexes
CREATE INDEX idx_password_reset_user ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_expires ON password_reset_tokens(expires_at) WHERE used_at IS NULL;

COMMENT ON TABLE password_reset_tokens IS 'Password recovery tokens. Hashed, single-use, expire in 1 hour.';
COMMENT ON COLUMN password_reset_tokens.token_hash IS 'SHA-256 hash of token. Original token sent to user email.';
COMMENT ON COLUMN password_reset_tokens.used_at IS 'Set when token is used. Prevents reuse.';
```

### 1.6 Refresh Tokens

```sql
-- =============================================================================
-- REFRESH TOKENS
-- Purpose: JWT refresh token rotation for extended sessions
-- Strategy: Soft revoke (keep for audit), hard delete after 30 days
-- Security: Tokens are hashed, rotated on each use, family tracking for reuse detection
-- =============================================================================
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Token (hashed for security)
    token_hash VARCHAR(255) NOT NULL,

    -- Token family for rotation tracking (reuse detection)
    family_id UUID NOT NULL,

    -- Lifecycle
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revoked_reason VARCHAR(50),

    -- Context
    user_agent TEXT,
    ip_address INET,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uq_refresh_token UNIQUE (token_hash)
);

-- Indexes
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at) WHERE revoked_at IS NULL;

COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens with rotation. Hashed, family-tracked for reuse detection.';
COMMENT ON COLUMN refresh_tokens.family_id IS 'Groups tokens from same login session. If old token reused, revoke entire family.';
COMMENT ON COLUMN refresh_tokens.revoked_reason IS 'Why revoked: "rotated", "logout", "password_change", "suspicious_reuse"';
```

### 1.7 Login Attempts

```sql
-- =============================================================================
-- LOGIN ATTEMPTS
-- Purpose: Security audit trail and rate limiting analysis (ASVS V2.2, V7)
-- Strategy: Retention 90 days, then archive/delete
-- Security: Used for anomaly detection and brute force analysis
-- =============================================================================
CREATE TABLE login_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Target
    email VARCHAR(255) NOT NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    tenant_id UUID REFERENCES tenants(id) ON DELETE SET NULL,

    -- Result
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(50),

    -- Context
    ip_address INET NOT NULL,
    user_agent TEXT,
    country_code VARCHAR(2),

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for rate limiting queries
CREATE INDEX idx_login_attempts_email_time ON login_attempts(email, created_at DESC);
CREATE INDEX idx_login_attempts_ip_time ON login_attempts(ip_address, created_at DESC);
CREATE INDEX idx_login_attempts_user_time ON login_attempts(user_id, created_at DESC) WHERE user_id IS NOT NULL;
CREATE INDEX idx_login_attempts_created ON login_attempts(created_at);

COMMENT ON TABLE login_attempts IS 'Audit log for all login attempts. Used for rate limiting and anomaly detection.';
COMMENT ON COLUMN login_attempts.failure_reason IS 'Values: "invalid_credentials", "account_locked", "account_inactive", "email_not_verified"';
COMMENT ON COLUMN login_attempts.user_id IS 'Set if email matched a user, even on failed login. NULL if user not found.';
```

---

## Module 2: Catalog

### 2.1 Categories

```sql
-- =============================================================================
-- CATEGORIES
-- Purpose: Hierarchical product categories
-- Strategy: Soft delete (products reference categories)
-- =============================================================================
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
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,

    -- Constraints: Prevent cross-tenant parent reference
    CONSTRAINT uq_categories_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_categories_parent FOREIGN KEY (tenant_id, parent_id)
        REFERENCES categories(tenant_id, id),
    CONSTRAINT uq_categories_tenant_name_parent UNIQUE (tenant_id, name, parent_id)
);

-- Indexes
CREATE INDEX idx_categories_tenant ON categories(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_categories_parent ON categories(tenant_id, parent_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_categories_sort ON categories(tenant_id, sort_order) WHERE deleted_at IS NULL;

COMMENT ON TABLE categories IS 'Hierarchical product categories. Max 2 levels recommended.';
COMMENT ON COLUMN categories.parent_id IS 'NULL for top-level categories';
```

### 2.2 Products

```sql
-- =============================================================================
-- PRODUCTS
-- Purpose: Menu items that can be sold
-- Strategy: Soft delete (orders reference products historically)
-- =============================================================================
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
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,

    -- Constraints
    CONSTRAINT uq_products_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_products_tenant_sku UNIQUE (tenant_id, sku),
    CONSTRAINT fk_products_category FOREIGN KEY (tenant_id, category_id)
        REFERENCES categories(tenant_id, id)
);

-- Indexes
CREATE INDEX idx_products_tenant ON products(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_category ON products(tenant_id, category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_active ON products(tenant_id, is_active, is_available) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_type ON products(tenant_id, product_type) WHERE deleted_at IS NULL;

COMMENT ON TABLE products IS 'Menu items. Can be simple products, products with variants, or combos.';
COMMENT ON COLUMN products.product_type IS 'SIMPLE: sold as-is. VARIANT: has size/options. COMBO: bundle of products.';
COMMENT ON COLUMN products.is_available IS 'False when temporarily out of stock (vs. is_active for permanent removal)';
COMMENT ON COLUMN products.track_inventory IS 'If true, sales auto-deduct from ingredient stock';
```

### 2.3 Product Variants

```sql
-- =============================================================================
-- PRODUCT VARIANTS
-- Purpose: Size/type variations of a product (Chico, Mediano, Grande)
-- Strategy: Soft delete with product
-- =============================================================================
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
    deleted_at TIMESTAMPTZ,

    -- Constraints
    CONSTRAINT uq_product_variants_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_product_variants_product FOREIGN KEY (tenant_id, product_id)
        REFERENCES products(tenant_id, id),
    CONSTRAINT uq_product_variants_tenant_sku UNIQUE (tenant_id, sku),
    CONSTRAINT uq_product_variants_product_name UNIQUE (tenant_id, product_id, name)
);

-- Indexes
CREATE INDEX idx_product_variants_product ON product_variants(tenant_id, product_id)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE product_variants IS 'Product variations like sizes: Chico ($0), Mediano (+$10), Grande (+$20)';
COMMENT ON COLUMN product_variants.price_adjustment IS 'Amount added to product base_price. Can be negative.';
COMMENT ON COLUMN product_variants.is_default IS 'Pre-selected variant in POS. Only one per product should be true.';
```

### 2.4 Modifier Groups

```sql
-- =============================================================================
-- MODIFIER GROUPS
-- Purpose: Group related modifiers (e.g., "Extras", "Remove Items")
-- Strategy: Soft delete with product
-- =============================================================================
CREATE TABLE modifier_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    product_id UUID NOT NULL,

    -- Information
    name VARCHAR(100) NOT NULL,
    description TEXT,

    -- Selection Rules
    min_selections INTEGER NOT NULL DEFAULT 0 CHECK (min_selections >= 0),
    max_selections INTEGER CHECK (max_selections IS NULL OR max_selections >= min_selections),
    is_required BOOLEAN NOT NULL DEFAULT false,

    -- Display
    sort_order INTEGER NOT NULL DEFAULT 0,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,

    -- Constraints
    CONSTRAINT uq_modifier_groups_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_modifier_groups_product FOREIGN KEY (tenant_id, product_id)
        REFERENCES products(tenant_id, id),
    CONSTRAINT chk_modifier_groups_required CHECK (
        NOT is_required OR min_selections >= 1
    )
);

-- Indexes
CREATE INDEX idx_modifier_groups_product ON modifier_groups(tenant_id, product_id)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE modifier_groups IS 'Groups of modifiers for a product. E.g., "Extras" with min=0, max=5.';
COMMENT ON COLUMN modifier_groups.max_selections IS 'NULL means unlimited selections allowed';
COMMENT ON COLUMN modifier_groups.is_required IS 'If true, customer must select at least min_selections';
```

### 2.5 Modifiers

```sql
-- =============================================================================
-- MODIFIERS
-- Purpose: Individual options within a modifier group
-- Strategy: Soft delete with group
-- =============================================================================
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
    deleted_at TIMESTAMPTZ,

    -- Constraints
    CONSTRAINT uq_modifiers_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_modifiers_group FOREIGN KEY (tenant_id, group_id)
        REFERENCES modifier_groups(tenant_id, id),
    CONSTRAINT uq_modifiers_group_name UNIQUE (tenant_id, group_id, name)
);

-- Indexes
CREATE INDEX idx_modifiers_group ON modifiers(tenant_id, group_id) WHERE deleted_at IS NULL;

COMMENT ON TABLE modifiers IS 'Individual modifier options. E.g., "Extra Cheese" +$15, "No Onion" +$0';
COMMENT ON COLUMN modifiers.price_adjustment IS 'Cost added to item price. Usually >= 0, but can be negative for removals with credit.';
```

### 2.6 Combos

```sql
-- =============================================================================
-- COMBOS
-- Purpose: Bundled products sold at a special price
-- Strategy: Soft delete (orders may reference combos)
-- =============================================================================
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
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,

    -- Constraints
    CONSTRAINT uq_combos_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_combos_tenant_name UNIQUE (tenant_id, name)
);

-- Indexes
CREATE INDEX idx_combos_tenant ON combos(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_combos_active ON combos(tenant_id, is_active) WHERE deleted_at IS NULL;

COMMENT ON TABLE combos IS 'Product bundles with special pricing. E.g., "Combo 1" = Burger + Fries + Drink at $99.';
```

### 2.7 Combo Items

```sql
-- =============================================================================
-- COMBO ITEMS
-- Purpose: Products included in a combo
-- Strategy: Hard delete (follows combo lifecycle)
-- =============================================================================
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
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT fk_combo_items_combo FOREIGN KEY (tenant_id, combo_id)
        REFERENCES combos(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_combo_items_product FOREIGN KEY (tenant_id, product_id)
        REFERENCES products(tenant_id, id),
    CONSTRAINT uq_combo_items_combo_product UNIQUE (tenant_id, combo_id, product_id)
);

-- Indexes
CREATE INDEX idx_combo_items_combo ON combo_items(tenant_id, combo_id);
CREATE INDEX idx_combo_items_product ON combo_items(tenant_id, product_id);

COMMENT ON TABLE combo_items IS 'Products in a combo. Each row is one product with its quantity.';
COMMENT ON COLUMN combo_items.allow_substitutes IS 'If true, customer can swap this for another product in same substitute_group';
COMMENT ON COLUMN combo_items.substitute_group IS 'Group name for substitutable items, e.g., "DRINKS" allows swapping between drinks';
```

---

## Module 3: Inventory

### 3.1 Ingredients

```sql
-- =============================================================================
-- INGREDIENTS
-- Purpose: Raw materials used in recipes
-- Strategy: Soft delete (recipes reference ingredients)
-- =============================================================================
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
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,

    -- Constraints
    CONSTRAINT uq_ingredients_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_ingredients_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT uq_ingredients_tenant_sku UNIQUE (tenant_id, sku)
);

-- Indexes
CREATE INDEX idx_ingredients_tenant ON ingredients(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_ingredients_low_stock ON ingredients(tenant_id, current_stock, min_stock_level)
    WHERE deleted_at IS NULL AND is_active = true;

COMMENT ON TABLE ingredients IS 'Raw materials/supplies for recipes. Stock tracked per tenant (not per branch in MVP).';
COMMENT ON COLUMN ingredients.current_stock IS 'Current quantity on hand, in unit_type units';
COMMENT ON COLUMN ingredients.cost_per_unit IS 'Average cost per unit for COGS calculation';
```

### 3.2 Suppliers

```sql
-- =============================================================================
-- SUPPLIERS
-- Purpose: Vendors who provide ingredients
-- Strategy: Soft delete (purchase orders reference suppliers)
-- =============================================================================
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
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,

    -- Constraints
    CONSTRAINT uq_suppliers_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_suppliers_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT uq_suppliers_tenant_code UNIQUE (tenant_id, code)
);

-- Indexes
CREATE INDEX idx_suppliers_tenant ON suppliers(tenant_id) WHERE deleted_at IS NULL;

COMMENT ON TABLE suppliers IS 'Vendors for ingredient purchases. Used in purchase orders.';
```

### 3.3 Recipes

```sql
-- =============================================================================
-- RECIPES
-- Purpose: Define ingredient consumption per product/variant
-- Strategy: Hard delete (follows product lifecycle)
-- =============================================================================
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
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT fk_recipes_product FOREIGN KEY (tenant_id, product_id)
        REFERENCES products(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_recipes_variant FOREIGN KEY (tenant_id, variant_id)
        REFERENCES product_variants(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_recipes_ingredient FOREIGN KEY (tenant_id, ingredient_id)
        REFERENCES ingredients(tenant_id, id),
    CONSTRAINT uq_recipes_product_variant_ingredient
        UNIQUE (tenant_id, product_id, variant_id, ingredient_id)
);

-- Indexes
CREATE INDEX idx_recipes_product ON recipes(tenant_id, product_id);
CREATE INDEX idx_recipes_ingredient ON recipes(tenant_id, ingredient_id);

COMMENT ON TABLE recipes IS 'Ingredient requirements per product. Used to auto-deduct stock on sales.';
COMMENT ON COLUMN recipes.variant_id IS 'NULL applies to all variants; specific variant_id overrides base recipe';
COMMENT ON COLUMN recipes.quantity IS 'Amount of ingredient consumed per 1 unit of product sold';
```

### 3.4 Stock Movements

```sql
-- =============================================================================
-- STOCK MOVEMENTS
-- Purpose: Track all inventory changes for audit trail
-- Strategy: Never delete (audit log)
-- =============================================================================
CREATE TABLE stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    branch_id UUID REFERENCES branches(id),
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
    created_by UUID,

    -- Constraints
    CONSTRAINT fk_stock_movements_ingredient FOREIGN KEY (tenant_id, ingredient_id)
        REFERENCES ingredients(tenant_id, id),
    CONSTRAINT fk_stock_movements_branch FOREIGN KEY (tenant_id, branch_id)
        REFERENCES branches(tenant_id, id)
);

-- Indexes
CREATE INDEX idx_stock_movements_tenant ON stock_movements(tenant_id);
CREATE INDEX idx_stock_movements_ingredient ON stock_movements(tenant_id, ingredient_id);
CREATE INDEX idx_stock_movements_date ON stock_movements(tenant_id, created_at);
CREATE INDEX idx_stock_movements_reference ON stock_movements(tenant_id, reference_type, reference_id);

COMMENT ON TABLE stock_movements IS 'Immutable audit log of all inventory changes.';
COMMENT ON COLUMN stock_movements.reference_type IS 'Source: "ORDER", "PURCHASE_ORDER", "ADJUSTMENT", "WASTE"';
COMMENT ON COLUMN stock_movements.reference_id IS 'ID of the source document (order_id, purchase_order_id, etc.)';
COMMENT ON COLUMN stock_movements.balance_after IS 'Ingredient stock balance after this movement';
```

### 3.5 Purchase Orders (Optional for MVP)

```sql
-- =============================================================================
-- PURCHASE ORDERS
-- Purpose: Track ingredient purchases from suppliers
-- Strategy: Soft delete (financial records)
-- =============================================================================
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
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,

    -- Constraints
    CONSTRAINT uq_purchase_orders_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_purchase_orders_tenant_number UNIQUE (tenant_id, order_number),
    CONSTRAINT fk_purchase_orders_branch FOREIGN KEY (tenant_id, branch_id)
        REFERENCES branches(tenant_id, id),
    CONSTRAINT fk_purchase_orders_supplier FOREIGN KEY (tenant_id, supplier_id)
        REFERENCES suppliers(tenant_id, id)
);

-- Indexes
CREATE INDEX idx_purchase_orders_tenant ON purchase_orders(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_purchase_orders_status ON purchase_orders(tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_purchase_orders_date ON purchase_orders(tenant_id, order_date) WHERE deleted_at IS NULL;

COMMENT ON TABLE purchase_orders IS 'Ingredient purchase orders from suppliers.';
```

### 3.6 Purchase Order Items

```sql
-- =============================================================================
-- PURCHASE ORDER ITEMS
-- Purpose: Line items in a purchase order
-- Strategy: Cascade delete with parent
-- =============================================================================
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
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT fk_po_items_order FOREIGN KEY (tenant_id, purchase_order_id)
        REFERENCES purchase_orders(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_po_items_ingredient FOREIGN KEY (tenant_id, ingredient_id)
        REFERENCES ingredients(tenant_id, id)
);

-- Indexes
CREATE INDEX idx_po_items_order ON purchase_order_items(tenant_id, purchase_order_id);

COMMENT ON TABLE purchase_order_items IS 'Line items for purchase orders.';
```

---

## Module 4: POS

### 4.1 Areas

```sql
-- =============================================================================
-- AREAS
-- Purpose: Restaurant zones (Terraza, Interior, Barra)
-- Strategy: Soft delete (tables reference areas)
-- =============================================================================
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
    deleted_at TIMESTAMPTZ,

    -- Constraints
    CONSTRAINT uq_areas_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_areas_branch FOREIGN KEY (tenant_id, branch_id)
        REFERENCES branches(tenant_id, id),
    CONSTRAINT uq_areas_branch_name UNIQUE (tenant_id, branch_id, name)
);

-- Indexes
CREATE INDEX idx_areas_branch ON areas(tenant_id, branch_id) WHERE deleted_at IS NULL;

COMMENT ON TABLE areas IS 'Restaurant zones/sections. Tables belong to areas.';
```

### 4.2 Tables

```sql
-- =============================================================================
-- TABLES
-- Purpose: Physical or virtual tables/positions
-- Strategy: Soft delete (orders reference tables)
-- =============================================================================
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
    deleted_at TIMESTAMPTZ,

    -- Constraints
    CONSTRAINT uq_tables_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_tables_area FOREIGN KEY (tenant_id, area_id)
        REFERENCES areas(tenant_id, id),
    CONSTRAINT uq_tables_area_number UNIQUE (tenant_id, area_id, number)
);

-- Indexes
CREATE INDEX idx_tables_area ON tables(tenant_id, area_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_tables_status ON tables(tenant_id, status) WHERE deleted_at IS NULL;

COMMENT ON TABLE tables IS 'Restaurant tables/positions. Used for dine-in service.';
COMMENT ON COLUMN tables.number IS 'Display number like "1", "2A", "Barra-3"';
COMMENT ON COLUMN tables.position_x IS 'X coordinate for visual floor plan (optional)';
```

### 4.3 Customers

```sql
-- =============================================================================
-- CUSTOMERS
-- Purpose: Contact info for delivery and digital receipts
-- Strategy: Soft delete (GDPR-style data handling)
-- =============================================================================
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

    -- Stats
    total_orders INTEGER NOT NULL DEFAULT 0,
    total_spent DECIMAL(12,2) NOT NULL DEFAULT 0,
    last_order_at TIMESTAMPTZ,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,

    -- Constraints
    CONSTRAINT uq_customers_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_customers_tenant_phone UNIQUE (tenant_id, phone),
    CONSTRAINT uq_customers_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT chk_customers_has_contact CHECK (
        phone IS NOT NULL OR email IS NOT NULL OR whatsapp IS NOT NULL
    )
);

-- Indexes
CREATE INDEX idx_customers_tenant ON customers(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_phone ON customers(tenant_id, phone) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_email ON customers(tenant_id, email) WHERE deleted_at IS NULL;

COMMENT ON TABLE customers IS 'Customer contact data for delivery and digital receipts.';
COMMENT ON COLUMN customers.preferences IS 'JSON: {"favorite_items": [...], "dietary_restrictions": [...]}';
```

### 4.4 Orders

```sql
-- =============================================================================
-- ORDERS
-- Purpose: Sales transactions
-- Strategy: Never delete (financial records)
-- Decision: Prices denormalized to preserve historical accuracy
-- =============================================================================
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
    created_by UUID,
    updated_by UUID,

    -- Constraints
    CONSTRAINT uq_orders_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_orders_tenant_number UNIQUE (tenant_id, order_number),
    CONSTRAINT uq_orders_branch_daily_sequence UNIQUE (tenant_id, branch_id, DATE(opened_at), daily_sequence),
    CONSTRAINT fk_orders_branch FOREIGN KEY (tenant_id, branch_id)
        REFERENCES branches(tenant_id, id),
    CONSTRAINT fk_orders_table FOREIGN KEY (tenant_id, table_id)
        REFERENCES tables(tenant_id, id),
    CONSTRAINT fk_orders_customer FOREIGN KEY (tenant_id, customer_id)
        REFERENCES customers(tenant_id, id)
);

-- Indexes
CREATE INDEX idx_orders_tenant ON orders(tenant_id);
CREATE INDEX idx_orders_branch ON orders(tenant_id, branch_id);
CREATE INDEX idx_orders_status ON orders(tenant_id, status_id);
CREATE INDEX idx_orders_date ON orders(tenant_id, opened_at);
CREATE INDEX idx_orders_daily ON orders(tenant_id, branch_id, DATE(opened_at));
CREATE INDEX idx_orders_table ON orders(tenant_id, table_id) WHERE table_id IS NOT NULL;
CREATE INDEX idx_orders_customer ON orders(tenant_id, customer_id) WHERE customer_id IS NOT NULL;

COMMENT ON TABLE orders IS 'Sales transactions. Never deleted for audit purposes.';
COMMENT ON COLUMN orders.order_number IS 'Human-readable order ID, e.g., "ORD-20260205-001"';
COMMENT ON COLUMN orders.daily_sequence IS 'Sequential number within branch+date, resets daily';
COMMENT ON COLUMN orders.tax_rate IS 'Tax rate at time of order (copied from tenant settings)';
```

### 4.5 Order Items

```sql
-- =============================================================================
-- ORDER ITEMS
-- Purpose: Line items in an order
-- Strategy: Never delete (part of order record)
-- Decision: Product/variant names and prices COPIED to preserve history
-- =============================================================================
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
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uq_order_items_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (tenant_id, order_id)
        REFERENCES orders(tenant_id, id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (tenant_id, product_id)
        REFERENCES products(tenant_id, id),
    CONSTRAINT fk_order_items_variant FOREIGN KEY (tenant_id, variant_id)
        REFERENCES product_variants(tenant_id, id),
    CONSTRAINT fk_order_items_combo FOREIGN KEY (tenant_id, combo_id)
        REFERENCES combos(tenant_id, id),
    CONSTRAINT chk_order_items_product_or_combo CHECK (
        (product_id IS NOT NULL AND combo_id IS NULL) OR
        (product_id IS NULL AND combo_id IS NOT NULL)
    )
);

-- Indexes
CREATE INDEX idx_order_items_order ON order_items(tenant_id, order_id);
CREATE INDEX idx_order_items_kds ON order_items(tenant_id, kds_status)
    WHERE kds_status != 'DELIVERED';

COMMENT ON TABLE order_items IS 'Line items in orders. Prices denormalized to preserve history.';
COMMENT ON COLUMN order_items.product_name IS 'HISTORICAL: Product name at time of order. DO NOT UPDATE if product changes.';
COMMENT ON COLUMN order_items.unit_price IS 'HISTORICAL: Price at time of order. DO NOT UPDATE if product price changes.';
COMMENT ON COLUMN order_items.kds_status IS 'Kitchen Display System status for this item.';
```

### 4.6 Order Item Modifiers

```sql
-- =============================================================================
-- ORDER ITEM MODIFIERS
-- Purpose: Modifiers applied to order items
-- Strategy: Never delete (part of order record)
-- Decision: Modifier names and prices COPIED to preserve history
-- =============================================================================
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
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT fk_oim_order_item FOREIGN KEY (tenant_id, order_item_id)
        REFERENCES order_items(tenant_id, id),
    CONSTRAINT fk_oim_modifier FOREIGN KEY (tenant_id, modifier_id)
        REFERENCES modifiers(tenant_id, id)
);

-- Indexes
CREATE INDEX idx_oim_order_item ON order_item_modifiers(tenant_id, order_item_id);

COMMENT ON TABLE order_item_modifiers IS 'Modifiers applied to order items. Prices denormalized for history.';
```

### 4.7 Payments

```sql
-- =============================================================================
-- PAYMENTS
-- Purpose: Payment records for orders
-- Strategy: Never delete (financial records)
-- =============================================================================
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
    created_by UUID,

    -- Constraints
    CONSTRAINT fk_payments_order FOREIGN KEY (tenant_id, order_id)
        REFERENCES orders(tenant_id, id),
    CONSTRAINT chk_payments_cash_received CHECK (
        payment_method != 'CASH' OR amount_received IS NOT NULL
    )
);

-- Indexes
CREATE INDEX idx_payments_order ON payments(tenant_id, order_id);
CREATE INDEX idx_payments_date ON payments(tenant_id, created_at);
CREATE INDEX idx_payments_method ON payments(tenant_id, payment_method);

COMMENT ON TABLE payments IS 'Payment records. MVP supports only CASH.';
COMMENT ON COLUMN payments.amount_received IS 'For cash: amount customer gave. NULL for other methods.';
```

### 4.8 Order Status History

```sql
-- =============================================================================
-- ORDER STATUS HISTORY
-- Purpose: Track order status changes for KDS and audit
-- Strategy: Never delete (audit log)
-- =============================================================================
CREATE TABLE order_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    order_id UUID NOT NULL,
    status_id UUID NOT NULL REFERENCES order_status_types(id),

    -- Context
    changed_by UUID,
    notes TEXT,

    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT fk_osh_order FOREIGN KEY (tenant_id, order_id)
        REFERENCES orders(tenant_id, id)
);

-- Indexes
CREATE INDEX idx_osh_order ON order_status_history(tenant_id, order_id);
CREATE INDEX idx_osh_date ON order_status_history(tenant_id, created_at);

COMMENT ON TABLE order_status_history IS 'Audit log of order status transitions for KDS and analytics.';
```

---

## Module 5: Notifications

### 5.1 Notification Logs

```sql
-- =============================================================================
-- NOTIFICATION LOGS
-- Purpose: Track digital ticket/receipt delivery attempts
-- Strategy: Never delete (delivery audit)
-- =============================================================================
CREATE TABLE notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    -- References
    customer_id UUID,
    order_id UUID,

    -- Delivery Info
    channel VARCHAR(30) NOT NULL CHECK (channel IN ('WHATSAPP', 'EMAIL', 'SMS')),
    recipient VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL,

    -- Status
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED')),
    error_message TEXT,

    -- Tracking
    external_id VARCHAR(255),

    -- Timestamps
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT fk_notif_customer FOREIGN KEY (tenant_id, customer_id)
        REFERENCES customers(tenant_id, id),
    CONSTRAINT fk_notif_order FOREIGN KEY (tenant_id, order_id)
        REFERENCES orders(tenant_id, id)
);

-- Indexes
CREATE INDEX idx_notif_tenant ON notification_logs(tenant_id);
CREATE INDEX idx_notif_order ON notification_logs(tenant_id, order_id);
CREATE INDEX idx_notif_status ON notification_logs(tenant_id, status);
CREATE INDEX idx_notif_date ON notification_logs(tenant_id, created_at);

COMMENT ON TABLE notification_logs IS 'Audit log of digital receipt delivery attempts.';
COMMENT ON COLUMN notification_logs.content_type IS 'Type of notification: "RECEIPT", "ORDER_CONFIRMATION", "ORDER_READY"';
COMMENT ON COLUMN notification_logs.external_id IS 'ID from messaging provider for tracking';
```

---

## Seed Data

```sql
-- =============================================================================
-- SEED DATA: Run after schema creation
-- =============================================================================

-- Subscription Plans
INSERT INTO subscription_plans (id, name, code, price_monthly_mxn, max_branches, max_users_per_branch, features, description) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Starter', 'STARTER', 500.00, 1, 3,
     '{"pos": true, "kds": false, "whatsapp_bot": false, "reports": "basic"}',
     'Plan inicial para pequeños negocios'),
    ('22222222-2222-2222-2222-222222222222', 'Pro', 'PRO', 1000.00, 3, 10,
     '{"pos": true, "kds": true, "whatsapp_bot": true, "reports": "advanced", "inventory": true}',
     'Plan completo con todas las funciones'),
    ('33333333-3333-3333-3333-333333333333', 'Enterprise', 'ENTERPRISE', 2500.00, 10, 50,
     '{"pos": true, "kds": true, "whatsapp_bot": true, "reports": "advanced", "inventory": true, "api_access": true}',
     'Plan empresarial con API y soporte prioritario');

-- Roles
INSERT INTO roles (id, name, code, description, permissions, is_system) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Owner', 'OWNER',
     'Full access to all features and settings',
     '["*"]', true),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Cashier', 'CASHIER',
     'POS access only - can create and manage orders',
     '["pos:read", "pos:write", "products:read", "customers:read", "customers:write"]', true),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Kitchen', 'KITCHEN',
     'KDS access only - can view and update order status',
     '["kds:read", "kds:write", "orders:read"]', true);

-- Order Status Types
INSERT INTO order_status_types (id, code, name, description, color_hex, sequence, is_terminal) VALUES
    ('d1111111-1111-1111-1111-111111111111', 'PENDING', 'Pending', 'Order created, not yet sent to kitchen', '#FFA500', 1, false),
    ('d2222222-2222-2222-2222-222222222222', 'IN_PROGRESS', 'In Progress', 'Kitchen is preparing the order', '#3498DB', 2, false),
    ('d3333333-3333-3333-3333-333333333333', 'READY', 'Ready', 'Order ready for pickup/delivery', '#2ECC71', 3, false),
    ('d4444444-4444-4444-4444-444444444444', 'DELIVERED', 'Delivered', 'Order delivered to customer', '#27AE60', 4, false),
    ('d5555555-5555-5555-5555-555555555555', 'COMPLETED', 'Completed', 'Order paid and closed', '#1ABC9C', 5, true),
    ('d6666666-6666-6666-6666-666666666666', 'CANCELLED', 'Cancelled', 'Order was cancelled', '#E74C3C', 6, true);

-- Stock Movement Types
INSERT INTO stock_movement_types (id, code, name, affects_stock, description) VALUES
    ('e1111111-1111-1111-1111-111111111111', 'PURCHASE', 'Purchase', 1, 'Inventory received from supplier'),
    ('e2222222-2222-2222-2222-222222222222', 'SALE', 'Sale', -1, 'Inventory consumed by order'),
    ('e3333333-3333-3333-3333-333333333333', 'ADJUSTMENT_IN', 'Adjustment (In)', 1, 'Manual inventory increase'),
    ('e4444444-4444-4444-4444-444444444444', 'ADJUSTMENT_OUT', 'Adjustment (Out)', -1, 'Manual inventory decrease'),
    ('e5555555-5555-5555-5555-555555555555', 'WASTE', 'Waste', -1, 'Spoilage or damage'),
    ('e6666666-6666-6666-6666-666666666666', 'TRANSFER_IN', 'Transfer (In)', 1, 'Transfer received from another branch'),
    ('e7777777-7777-7777-7777-777777777777', 'TRANSFER_OUT', 'Transfer (Out)', -1, 'Transfer sent to another branch'),
    ('e8888888-8888-8888-8888-888888888888', 'RETURN', 'Return to Supplier', -1, 'Inventory returned to supplier');

-- Unit Types
INSERT INTO unit_types (id, code, name, abbreviation, category) VALUES
    ('f1111111-1111-1111-1111-111111111111', 'KG', 'Kilogram', 'kg', 'WEIGHT'),
    ('f2222222-2222-2222-2222-222222222222', 'G', 'Gram', 'g', 'WEIGHT'),
    ('f3333333-3333-3333-3333-333333333333', 'L', 'Liter', 'L', 'VOLUME'),
    ('f4444444-4444-4444-4444-444444444444', 'ML', 'Milliliter', 'ml', 'VOLUME'),
    ('f5555555-5555-5555-5555-555555555555', 'UNIT', 'Unit', 'u', 'COUNT'),
    ('f6666666-6666-6666-6666-666666666666', 'DOZEN', 'Dozen', 'dz', 'COUNT'),
    ('f7777777-7777-7777-7777-777777777777', 'OZ', 'Ounce', 'oz', 'WEIGHT'),
    ('f8888888-8888-8888-8888-888888888888', 'LB', 'Pound', 'lb', 'WEIGHT');
```

---

## Design Decisions

### ADR-001: Denormalized Prices in Order Items

**Context:** Products change prices over time. Historical orders must preserve the price at time of sale.

**Decision:** Copy `product_name`, `variant_name`, `unit_price`, and modifier prices into order items.

**Rationale:**
- Guarantees accurate financial records regardless of future product changes
- Simplifies reporting (no need for "price history" tables)
- Minor storage overhead (~500 bytes per order item)

**Trade-offs:**
- Increased storage (acceptable for MVP scale)
- Product renames don't auto-update past orders (intentional)

**Revisit if:** Storage costs become significant at >100K orders.

---

### ADR-002: Single Role per User

**Context:** Requirements specify users cannot have multiple roles.

**Decision:** Direct FK from `users.role_id` to `roles.id` instead of junction table.

**Rationale:**
- Simpler authorization logic
- Meets stated requirements
- One less table and join

**Trade-offs:**
- Cannot assign multiple roles without schema change
- OK for MVP scope

**Revisit if:** Business requires composite roles like "Cashier+Kitchen".

---

### ADR-003: Composite FK with tenant_id

**Context:** Multi-tenant system must prevent cross-tenant data references.

**Decision:** All FKs include `tenant_id` in composite constraint where referencing tenant-scoped tables.

**Example:**
```sql
CONSTRAINT fk_orders_branch FOREIGN KEY (tenant_id, branch_id)
    REFERENCES branches(tenant_id, id)
```

**Rationale:**
- Database-level guarantee against cross-tenant bugs
- Cannot accidentally reference another tenant's branch

**Trade-offs:**
- Requires composite unique constraints on referenced tables
- Slightly more complex FK definitions

---

### ADR-004: Stock at Tenant Level (Not Branch Level)

**Context:** MVP supports one branch per tenant. Design for multi-branch future.

**Decision:** `ingredients.current_stock` tracks inventory at tenant level. `stock_movements.branch_id` optional for future branch-level tracking.

**Rationale:**
- Simpler for MVP single-branch scenario
- Movement history preserves branch info for future migration

**Trade-offs:**
- Cannot see per-branch stock levels without sum query
- Transfer between branches not fully supported yet

**Revisit if:** Multi-branch tenants need separate stock per location.

---

### ADR-005: Order Number Format

**Context:** Orders need human-readable identifiers.

**Decision:**
- `order_number`: Full ID like "ORD-20260205-001" (unique per tenant)
- `daily_sequence`: Integer reset daily per branch (for kitchen display)

**Rationale:**
- Full order_number for receipts and customer references
- Short daily_sequence for kitchen/KDS display ("Order #7")

**Trade-offs:**
- Need to generate both on order creation
- daily_sequence requires branch+date unique constraint

---

### ADR-006: Soft Delete Strategy

**Decision:** Apply soft delete based on entity purpose:

| Strategy | Entities | Reason |
|----------|----------|--------|
| Soft delete | Tenants, Branches, Users, Products, Categories, Ingredients, Customers | Referenced by historical data or legal requirements |
| Hard delete | Auth identities, Combo items, Recipes | No historical value, cascade with parent |
| Never delete | Orders, Payments, Stock movements, Status history | Financial/audit records |

---

## Migration Strategy

### Flyway Migration File Structure

```
backend/src/main/resources/db/migration/
├── V1__create_global_catalogs.sql
├── V2__create_core_module.sql
├── V3__create_catalog_module.sql
├── V4__create_inventory_module.sql
├── V5__create_pos_module.sql
├── V6__create_notifications_module.sql
├── V7__seed_data.sql
```

### Recommended Implementation Order

1. **V1: Global Catalogs** - No dependencies
2. **V2: Core Module** - Depends on global catalogs
3. **V3: Catalog Module** - Depends on core (tenants)
4. **V4: Inventory Module** - Depends on core + catalog
5. **V5: POS Module** - Depends on all above
6. **V6: Notifications Module** - Depends on POS (orders)
7. **V7: Seed Data** - All reference data

### Pre-migration Checklist

- [ ] Neon database instance created
- [ ] Connection string configured in application.yml
- [ ] Flyway configured in Spring Boot
- [ ] Test environment mirrors production schema

---

## Appendix: Common Queries

### Get Active Products with Variants (for POS)

```sql
SELECT
    p.id, p.name, p.base_price, p.product_type, p.image_url,
    c.name as category_name,
    jsonb_agg(
        DISTINCT jsonb_build_object(
            'id', pv.id,
            'name', pv.name,
            'price_adjustment', pv.price_adjustment,
            'is_default', pv.is_default
        )
    ) FILTER (WHERE pv.id IS NOT NULL) as variants
FROM products p
LEFT JOIN categories c ON c.tenant_id = p.tenant_id AND c.id = p.category_id
LEFT JOIN product_variants pv ON pv.tenant_id = p.tenant_id
    AND pv.product_id = p.id AND pv.deleted_at IS NULL
WHERE p.tenant_id = :tenantId
    AND p.deleted_at IS NULL
    AND p.is_active = true
    AND p.is_available = true
GROUP BY p.id, c.name
ORDER BY p.sort_order, p.name;
```

### Daily Sales Report

```sql
SELECT
    DATE(o.opened_at) as sale_date,
    COUNT(*) as total_orders,
    SUM(o.subtotal) as subtotal,
    SUM(o.tax) as tax,
    SUM(o.total) as total,
    jsonb_object_agg(p.payment_method, p.method_total) as by_payment_method
FROM orders o
LEFT JOIN (
    SELECT order_id, payment_method, SUM(amount) as method_total
    FROM payments
    WHERE tenant_id = :tenantId AND status = 'COMPLETED'
    GROUP BY order_id, payment_method
) p ON p.order_id = o.id
WHERE o.tenant_id = :tenantId
    AND o.branch_id = :branchId
    AND o.status_id = (SELECT id FROM order_status_types WHERE code = 'COMPLETED')
    AND o.opened_at >= :startDate
    AND o.opened_at < :endDate
GROUP BY DATE(o.opened_at)
ORDER BY sale_date DESC;
```

### KDS Active Orders

```sql
SELECT
    o.id, o.order_number, o.daily_sequence, o.service_type,
    t.number as table_number,
    o.kitchen_notes,
    o.opened_at,
    EXTRACT(EPOCH FROM (NOW() - o.opened_at)) / 60 as minutes_elapsed,
    jsonb_agg(
        jsonb_build_object(
            'id', oi.id,
            'product_name', oi.product_name,
            'variant_name', oi.variant_name,
            'quantity', oi.quantity,
            'notes', oi.notes,
            'kds_status', oi.kds_status,
            'modifiers', (
                SELECT jsonb_agg(oim.modifier_name)
                FROM order_item_modifiers oim
                WHERE oim.order_item_id = oi.id
            )
        ) ORDER BY oi.sort_order
    ) as items
FROM orders o
LEFT JOIN tables t ON t.tenant_id = o.tenant_id AND t.id = o.table_id
JOIN order_items oi ON oi.tenant_id = o.tenant_id AND oi.order_id = o.id
WHERE o.tenant_id = :tenantId
    AND o.branch_id = :branchId
    AND o.status_id IN (
        SELECT id FROM order_status_types WHERE code IN ('PENDING', 'IN_PROGRESS')
    )
GROUP BY o.id, t.number
ORDER BY o.opened_at;
```

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0.0 | 2026-02-05 | Senior Software Architect | Initial schema design |
