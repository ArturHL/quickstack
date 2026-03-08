-- Minimal global catalogs for quickstack-inventory @DataJpaTest
-- Only includes tables required as FK dependencies for the inventory module.

CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    price_monthly_mxn DECIMAL(10,2) NOT NULL,
    max_branches INTEGER NOT NULL DEFAULT 1,
    max_users_per_branch INTEGER NOT NULL DEFAULT 5,
    features JSONB NOT NULL DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
