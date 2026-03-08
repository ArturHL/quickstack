-- Minimal core module for quickstack-inventory @DataJpaTest
-- Only the tenants table, required for multi-tenancy (tenant_id FK in inventory tables).

CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    legal_name VARCHAR(255),
    rfc VARCHAR(13),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'CANCELLED')),
    trial_ends_at TIMESTAMPTZ,
    settings JSONB NOT NULL DEFAULT '{}',
    timezone VARCHAR(50) NOT NULL DEFAULT 'America/Mexico_City',
    currency VARCHAR(3) NOT NULL DEFAULT 'MXN',
    tax_rate DECIMAL(5,4) NOT NULL DEFAULT 0.16,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_tenants_slug_format CHECK (slug ~ '^[a-z0-9-]+$')
);
