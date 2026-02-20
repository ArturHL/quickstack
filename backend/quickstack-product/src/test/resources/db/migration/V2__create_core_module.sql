-- =============================================================================
-- QuickStack POS - Database Migration V2
-- Module: Core (Tenants, Branches, Users, Auth)
-- Description: Multi-tenancy foundation and user management with native auth
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
-- Purpose: System users with native authentication (no external IdP)
-- Strategy: Soft delete (audit trail for actions)
-- Security: OWASP ASVS L2 compliant password storage and account security
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

-- -----------------------------------------------------------------------------
-- PASSWORD RESET TOKENS
-- Purpose: Secure password recovery flow (ASVS V2.5)
-- Strategy: Hard delete after use or expiration
-- Security: Tokens are hashed, single-use, time-limited
-- -----------------------------------------------------------------------------
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

-- -----------------------------------------------------------------------------
-- REFRESH TOKENS
-- Purpose: JWT refresh token rotation for extended sessions
-- Strategy: Soft revoke (keep for audit), hard delete after 30 days
-- Security: Tokens are hashed, rotated on each use, family tracking for reuse detection
-- -----------------------------------------------------------------------------
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

-- -----------------------------------------------------------------------------
-- LOGIN ATTEMPTS
-- Purpose: Security audit trail and rate limiting analysis (ASVS V2.2, V7)
-- Strategy: Retention 90 days, then archive/delete
-- Security: Used for anomaly detection and brute force analysis
-- -----------------------------------------------------------------------------
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
