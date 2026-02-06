-- =============================================================================
-- QuickStack POS - Database Migration V2
-- Module: Core (Tenants, Branches, Users, Auth)
-- Description: Multi-tenancy foundation and user management
-- =============================================================================

-- -----------------------------------------------------------------------------
-- TENANTS
-- Purpose: Root entity for multi-tenancy - represents a restaurant business
-- Strategy: Soft delete (legal/audit requirements)
-- -----------------------------------------------------------------------------
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

-- -----------------------------------------------------------------------------
-- BRANCHES
-- Purpose: Physical locations (sucursales) belonging to a tenant
-- Strategy: Soft delete (orders reference branches historically)
-- -----------------------------------------------------------------------------
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

-- Composite unique for FK references
ALTER TABLE branches ADD CONSTRAINT uq_branches_tenant_id UNIQUE (tenant_id, id);

-- Indexes
CREATE INDEX idx_branches_tenant ON branches(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_branches_active ON branches(tenant_id, is_active) WHERE deleted_at IS NULL;

COMMENT ON TABLE branches IS 'Physical restaurant locations. Users are assigned to one branch.';
COMMENT ON COLUMN branches.code IS 'Short internal code (e.g., "CENTRO", "NORTE"). Unique within tenant.';
COMMENT ON COLUMN branches.settings IS 'Branch-specific settings: {"printer_ip": "192.168.1.100"}';

-- -----------------------------------------------------------------------------
-- USERS
-- Purpose: System users with single role assignment
-- Strategy: Soft delete (audit trail for actions)
-- Decision: One role per user (not many-to-many) - simplifies auth
-- -----------------------------------------------------------------------------
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

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_login_at TIMESTAMPTZ,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,

    -- Constraints: Prevent cross-tenant FK references
    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT fk_users_branch FOREIGN KEY (tenant_id, branch_id)
        REFERENCES branches(tenant_id, id) DEFERRABLE INITIALLY DEFERRED
);

-- Indexes
CREATE INDEX idx_users_tenant ON users(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_tenant_branch ON users(tenant_id, branch_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_role ON users(role_id) WHERE deleted_at IS NULL;

COMMENT ON TABLE users IS 'System users. Auth handled by Auth0; we store profile data only.';
COMMENT ON COLUMN users.branch_id IS 'NULL for OWNER role (access to all branches), required for CASHIER/KITCHEN';
COMMENT ON COLUMN users.role_id IS 'Single role per user. OWNER/CASHIER/KITCHEN in MVP.';

-- -----------------------------------------------------------------------------
-- AUTH IDENTITIES
-- Purpose: Link users to external auth providers (Auth0)
-- Strategy: Hard delete (no business value in keeping orphaned identities)
-- -----------------------------------------------------------------------------
CREATE TABLE auth_identities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Provider Information
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,

    -- Metadata
    last_login_at TIMESTAMPTZ,
    provider_data JSONB DEFAULT '{}',

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uq_auth_provider_user UNIQUE (provider, provider_user_id)
);

-- Indexes
CREATE INDEX idx_auth_identities_user ON auth_identities(user_id);
CREATE INDEX idx_auth_identities_provider ON auth_identities(provider, provider_user_id);

COMMENT ON TABLE auth_identities IS 'Links app users to Auth0 identities. Supports multiple providers per user.';
COMMENT ON COLUMN auth_identities.provider IS 'Auth provider: "auth0", "google-oauth2", "facebook"';
COMMENT ON COLUMN auth_identities.provider_user_id IS 'ID from provider, e.g., "auth0|507f1f77bcf86cd799439011"';

-- -----------------------------------------------------------------------------
-- Add created_by/updated_by FK constraints (deferred due to circular refs)
-- -----------------------------------------------------------------------------
ALTER TABLE branches ADD CONSTRAINT fk_branches_created_by
    FOREIGN KEY (created_by) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE branches ADD CONSTRAINT fk_branches_updated_by
    FOREIGN KEY (updated_by) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE branches ADD CONSTRAINT fk_branches_deleted_by
    FOREIGN KEY (deleted_by) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE users ADD CONSTRAINT fk_users_created_by
    FOREIGN KEY (created_by) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE users ADD CONSTRAINT fk_users_updated_by
    FOREIGN KEY (updated_by) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE users ADD CONSTRAINT fk_users_deleted_by
    FOREIGN KEY (deleted_by) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;
