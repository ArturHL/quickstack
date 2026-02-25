-- =============================================================================
-- QuickStack POS - Database Migration V6
-- Module: Notifications (Digital receipts via WhatsApp/Email)
-- Description: Track delivery of digital tickets and notifications
-- =============================================================================

-- -----------------------------------------------------------------------------
-- NOTIFICATION LOGS
-- Purpose: Track digital ticket/receipt delivery attempts
-- Strategy: Never delete (delivery audit)
-- -----------------------------------------------------------------------------
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
        CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'READ')),
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,

    -- External tracking
    external_id VARCHAR(255),
    external_status VARCHAR(100),

    -- Timestamps
    scheduled_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- FKs with tenant isolation
ALTER TABLE notification_logs ADD CONSTRAINT fk_notif_customer
    FOREIGN KEY (tenant_id, customer_id) REFERENCES customers(tenant_id, id);
ALTER TABLE notification_logs ADD CONSTRAINT fk_notif_order
    FOREIGN KEY (tenant_id, order_id) REFERENCES orders(tenant_id, id);

-- Indexes
CREATE INDEX idx_notif_tenant ON notification_logs(tenant_id);
CREATE INDEX idx_notif_order ON notification_logs(tenant_id, order_id) WHERE order_id IS NOT NULL;
CREATE INDEX idx_notif_customer ON notification_logs(tenant_id, customer_id) WHERE customer_id IS NOT NULL;
CREATE INDEX idx_notif_status ON notification_logs(tenant_id, status);
CREATE INDEX idx_notif_channel ON notification_logs(tenant_id, channel);
CREATE INDEX idx_notif_date ON notification_logs(tenant_id, created_at);
CREATE INDEX idx_notif_pending ON notification_logs(tenant_id, status, scheduled_at)
    WHERE status = 'PENDING';
CREATE INDEX idx_notif_external ON notification_logs(external_id) WHERE external_id IS NOT NULL;

COMMENT ON TABLE notification_logs IS 'Audit log of digital receipt delivery attempts.';
COMMENT ON COLUMN notification_logs.content_type IS 'Type of notification: "RECEIPT", "ORDER_CONFIRMATION", "ORDER_READY", "PROMO"';
COMMENT ON COLUMN notification_logs.external_id IS 'ID from messaging provider (Twilio, WhatsApp Business API) for tracking';
COMMENT ON COLUMN notification_logs.retry_count IS 'Number of delivery retry attempts';

-- -----------------------------------------------------------------------------
-- NOTIFICATION TEMPLATES (Optional - for future use)
-- Purpose: Store reusable message templates
-- Strategy: Soft delete
-- -----------------------------------------------------------------------------
CREATE TABLE notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    -- Template Info
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    channel VARCHAR(30) NOT NULL CHECK (channel IN ('WHATSAPP', 'EMAIL', 'SMS')),
    content_type VARCHAR(50) NOT NULL,

    -- Content
    subject VARCHAR(255),
    body_template TEXT NOT NULL,
    variables JSONB DEFAULT '[]',

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    deleted_at TIMESTAMPTZ
);

-- Code unique within tenant+channel
ALTER TABLE notification_templates ADD CONSTRAINT uq_notif_templates_code
    UNIQUE (tenant_id, channel, code);

-- Indexes
CREATE INDEX idx_notif_templates_tenant ON notification_templates(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_notif_templates_lookup ON notification_templates(tenant_id, channel, content_type, is_active)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE notification_templates IS 'Reusable message templates for notifications.';
COMMENT ON COLUMN notification_templates.body_template IS 'Template with placeholders: "Hola {{customer_name}}, tu pedido #{{order_number}} está listo"';
COMMENT ON COLUMN notification_templates.variables IS 'JSON array of available variables: ["customer_name", "order_number", "total"]';
