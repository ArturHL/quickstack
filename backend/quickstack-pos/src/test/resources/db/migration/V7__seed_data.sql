-- =============================================================================
-- QuickStack POS - Database Migration V7
-- Module: Seed Data
-- Description: Initial reference data for global catalogs
-- =============================================================================

-- -----------------------------------------------------------------------------
-- SUBSCRIPTION PLANS
-- Purpose: Define available SaaS plans and their limits
-- -----------------------------------------------------------------------------
INSERT INTO subscription_plans (id, name, code, price_monthly_mxn, max_branches, max_users_per_branch, features, description) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Starter', 'STARTER', 500.00, 1, 3,
     '{"pos": true, "kds": false, "whatsapp_bot": false, "reports": "basic", "inventory": false}',
     'Plan inicial para pequeños negocios'),
    ('22222222-2222-2222-2222-222222222222', 'Pro', 'PRO', 1000.00, 3, 10,
     '{"pos": true, "kds": true, "whatsapp_bot": true, "reports": "advanced", "inventory": true, "digital_receipts": true}',
     'Plan completo con todas las funciones'),
    ('33333333-3333-3333-3333-333333333333', 'Enterprise', 'ENTERPRISE', 2500.00, 10, 50,
     '{"pos": true, "kds": true, "whatsapp_bot": true, "reports": "advanced", "inventory": true, "digital_receipts": true, "api_access": true, "multi_tenant_analytics": true}',
     'Plan empresarial con API y soporte prioritario');

-- -----------------------------------------------------------------------------
-- ROLES
-- Purpose: System roles for authorization
-- Decision: Single role per user - OWNER has full access, CASHIER only POS, KITCHEN only KDS
-- -----------------------------------------------------------------------------
INSERT INTO roles (id, name, code, description, permissions, is_system) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Dueño', 'OWNER',
     'Acceso completo a todas las funciones y configuraciones',
     '["*"]', true),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Cajero', 'CASHIER',
     'Acceso solo al POS - puede crear y gestionar órdenes',
     '["pos:read", "pos:write", "products:read", "customers:read", "customers:write", "tables:read", "tables:write", "orders:read", "orders:write", "payments:write"]', true),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Cocina', 'KITCHEN',
     'Acceso solo al KDS - puede ver y actualizar estado de órdenes',
     '["kds:read", "kds:write", "orders:read"]', true);

-- -----------------------------------------------------------------------------
-- ORDER STATUS TYPES
-- Purpose: Define valid order statuses for state machine
-- Workflow: PENDING -> IN_PROGRESS -> READY -> DELIVERED -> COMPLETED
--           Any state can go to CANCELLED
-- -----------------------------------------------------------------------------
INSERT INTO order_status_types (id, code, name, description, color_hex, sequence, is_terminal) VALUES
    ('d1111111-1111-1111-1111-111111111111', 'PENDING', 'Pendiente',
     'Orden creada, aún no enviada a cocina', '#FFA500', 1, false),
    ('d2222222-2222-2222-2222-222222222222', 'IN_PROGRESS', 'En Preparación',
     'Cocina está preparando la orden', '#3498DB', 2, false),
    ('d3333333-3333-3333-3333-333333333333', 'READY', 'Listo',
     'Orden lista para entregar', '#2ECC71', 3, false),
    ('d4444444-4444-4444-4444-444444444444', 'DELIVERED', 'Entregado',
     'Orden entregada al cliente', '#27AE60', 4, false),
    ('d5555555-5555-5555-5555-555555555555', 'COMPLETED', 'Completado',
     'Orden pagada y cerrada', '#1ABC9C', 5, true),
    ('d6666666-6666-6666-6666-666666666666', 'CANCELLED', 'Cancelado',
     'Orden fue cancelada', '#E74C3C', 6, true);

-- -----------------------------------------------------------------------------
-- STOCK MOVEMENT TYPES
-- Purpose: Categorize inventory changes
-- affects_stock: 1 = adds to stock, -1 = removes from stock
-- -----------------------------------------------------------------------------
INSERT INTO stock_movement_types (id, code, name, affects_stock, description) VALUES
    ('e1111111-1111-1111-1111-111111111111', 'PURCHASE', 'Compra', 1,
     'Inventario recibido de proveedor'),
    ('e2222222-2222-2222-2222-222222222222', 'SALE', 'Venta', -1,
     'Inventario consumido por orden'),
    ('e3333333-3333-3333-3333-333333333333', 'ADJUSTMENT_IN', 'Ajuste (Entrada)', 1,
     'Incremento manual de inventario'),
    ('e4444444-4444-4444-4444-444444444444', 'ADJUSTMENT_OUT', 'Ajuste (Salida)', -1,
     'Decremento manual de inventario'),
    ('e5555555-5555-5555-5555-555555555555', 'WASTE', 'Merma', -1,
     'Desperdicio o daño'),
    ('e6666666-6666-6666-6666-666666666666', 'TRANSFER_IN', 'Transferencia (Entrada)', 1,
     'Transferencia recibida de otra sucursal'),
    ('e7777777-7777-7777-7777-777777777777', 'TRANSFER_OUT', 'Transferencia (Salida)', -1,
     'Transferencia enviada a otra sucursal'),
    ('e8888888-8888-8888-8888-888888888888', 'RETURN', 'Devolución a Proveedor', -1,
     'Inventario devuelto al proveedor'),
    ('e9999999-9999-9999-9999-999999999999', 'INITIAL', 'Inventario Inicial', 1,
     'Carga inicial de inventario');

-- -----------------------------------------------------------------------------
-- UNIT TYPES
-- Purpose: Measurement units for ingredients
-- Categories: WEIGHT, VOLUME, COUNT
-- -----------------------------------------------------------------------------
INSERT INTO unit_types (id, code, name, abbreviation, category) VALUES
    -- Weight units
    ('f1111111-1111-1111-1111-111111111111', 'KG', 'Kilogramo', 'kg', 'WEIGHT'),
    ('f2222222-2222-2222-2222-222222222222', 'G', 'Gramo', 'g', 'WEIGHT'),
    ('f3333333-3333-3333-3333-333333333333', 'MG', 'Miligramo', 'mg', 'WEIGHT'),
    ('f4444444-4444-4444-4444-444444444444', 'LB', 'Libra', 'lb', 'WEIGHT'),
    ('f5555555-5555-5555-5555-555555555555', 'OZ', 'Onza', 'oz', 'WEIGHT'),
    -- Volume units
    ('f6666666-6666-6666-6666-666666666666', 'L', 'Litro', 'L', 'VOLUME'),
    ('f7777777-7777-7777-7777-777777777777', 'ML', 'Mililitro', 'ml', 'VOLUME'),
    ('f8888888-8888-8888-8888-888888888888', 'GAL', 'Galón', 'gal', 'VOLUME'),
    -- Count units
    ('f9999999-9999-9999-9999-999999999999', 'UNIT', 'Unidad', 'u', 'COUNT'),
    ('fa111111-1111-1111-1111-111111111111', 'DOZEN', 'Docena', 'dz', 'COUNT'),
    ('fb222222-2222-2222-2222-222222222222', 'PACK', 'Paquete', 'paq', 'COUNT'),
    ('fc333333-3333-3333-3333-333333333333', 'BOX', 'Caja', 'caja', 'COUNT');
