# QuickStack POS - Contexto del Proyecto

> Este archivo contiene el contexto necesario para continuar el desarrollo con Claude Code.
> **Última actualización:** 2026-02-05

## Resumen del Proyecto

**QuickStack POS** es un sistema de punto de venta SaaS multi-tenant para restaurantes en México. El objetivo es democratizar tecnología avanzada (predicciones, automatizaciones con IA) para pequeños empresarios.

**Timeline MVP:** 6 meses para validación con piloto

## Stack Tecnológico

| Componente | Tecnología | Hosting |
|------------|------------|---------|
| Frontend | React 18 + Vite + TypeScript + MUI | Vercel |
| Backend | Java 21 + Spring Boot 3.5 | Render (Docker) |
| Base de datos | PostgreSQL 16 | Neon (serverless) |
| Autenticación | Auth0 (OWASP ASVS L2) | - |
| State Management | Zustand | - |
| HTTP Client | TanStack Query + Axios | - |
| ORM | Spring Data JPA + Flyway | - |
| Real-time | Spring WebSocket (STOMP) | - |
| Automatizaciones | n8n | Self-hosted o Cloud |

## Decisiones de Arquitectura

- **Monorepo**: Frontend y backend en el mismo repositorio
- **Multi-module Maven**: Backend modular con dependencias independientes por módulo
- **Multi-tenancy**: BD compartida con `tenant_id` + composite FKs
- **Package by feature**: Cada módulo contiene su controller/service/repository
- **TDD**: Test-Driven Development completo
- **GitHub Flow**: main + feature branches con PRs
- **WebSockets**: KDS en tiempo real (no polling)
- **Tickets digitales**: WhatsApp/Email, sin impresión física

## Estructura del Proyecto

```
quickstack/
├── docs/
│   ├── ARCHITECTURE.md    # Decisiones técnicas detalladas
│   ├── DATABASE_SCHEMA.md # Esquema completo de BD
│   ├── SECURITY.md        # Arquitectura de seguridad (ASVS L2)
│   └── ROADMAP.md         # Plan de fases del MVP
├── frontend/              # React + Vite + TypeScript
│   └── src/
├── backend/               # Multi-module Maven
│   ├── pom.xml           # Parent POM
│   ├── quickstack-common/ # Utilidades compartidas
│   ├── quickstack-tenant/ # Módulo tenants
│   ├── quickstack-branch/ # Módulo sucursales
│   ├── quickstack-user/   # Módulo usuarios
│   ├── quickstack-product/# Módulo productos
│   ├── quickstack-pos/    # Módulo punto de venta
│   └── quickstack-app/    # Ensamblador (Spring Boot main)
│       └── src/main/resources/db/migration/  # Flyway migrations
└── .claude/
    └── agents/            # Agentes personalizados
```

## Fases del MVP

| Fase | Nombre | Estado |
|------|--------|--------|
| 0 | Foundation & Architecture | ✅ ~90% completo |
| 1 | Core POS (ventas, mesas, variantes, combos) | ⏳ Pendiente |
| 2 | Inventory (ingredientes, recetas, stock auto) | ⏳ Pendiente |
| 3 | Digital Tickets & KDS | ⏳ Pendiente |
| 4 | Basic Reporting | ⏳ Pendiente |
| 5 | WhatsApp Bot with AI | ⏳ Pendiente |
| 6 | Polish & Pilot Validation | ⏳ Pendiente |

## Estado Actual (Phase 0)

### Completado
- [x] Definición de arquitectura y stack
- [x] Creación de estructura monorepo
- [x] Inicialización de frontend (React + Vite)
- [x] Estructura multi-module Maven para backend
- [x] Parent POM creado
- [x] Documentación (ARCHITECTURE.md, ROADMAP.md)
- [x] Configuración de Git y GitHub
- [x] **Diseño de modelo de datos (27 tablas, 6 módulos)**
- [x] **7 migraciones Flyway creadas (V1-V7)**
- [x] **DATABASE_SCHEMA.md documentado**

### Pendiente Phase 0
- [ ] Crear `pom.xml` de cada módulo del backend
- [ ] Configurar Flyway + conexión a Neon
- [ ] Crear entidades JPA del módulo Core
- [ ] Configurar Auth0
- [ ] Configurar CI/CD (GitHub Actions)
- [ ] Estructura de carpetas del frontend
- [ ] Configurar variables de entorno

## Base de Datos - 27 Tablas en 6 Módulos

| Módulo | Tablas |
|--------|--------|
| Global Catalogs | subscription_plans, roles, order_status_types, stock_movement_types, unit_types |
| Core | tenants, branches, users, auth_identities |
| Catalog | categories, products, product_variants, modifier_groups, modifiers, combos, combo_items |
| Inventory | ingredients, suppliers, recipes, stock_movements, purchase_orders, purchase_order_items |
| POS | areas, tables, customers, orders, order_items, order_item_modifiers, payments, order_status_history |
| Notifications | notification_logs, notification_templates |

## Decisiones de Negocio Confirmadas

### Tipos de Servicio
- **DINE_IN**: Mesa con áreas (Terraza, Barra, etc.)
- **COUNTER**: Mostrador/para llevar rápido
- **DELIVERY**: Pedidos directos (sin Uber/Rappi)
- **TAKEOUT**: Para llevar

### Roles (sin mezcla)
| Rol | Acceso |
|-----|--------|
| OWNER (Dueño) | Todo el sistema |
| CASHIER (Cajero) | Solo POS |
| KITCHEN (Cocina) | Solo KDS |

### Pagos
- Solo efectivo en MVP
- Sin pagos parciales
- Propinas fuera del sistema

### Tickets
- **Digitales**: WhatsApp y Email
- **Sin impresión física**
- Envío manual y opcional por cajero

### Inventario
- Descuento automático de stock al vender
- Requiere configurar recetas por producto
- Stock a nivel de tenant (no por sucursal en MVP)

### Multi-sucursal
- Diseñado para soportar múltiples
- Solo 1 sucursal activa en MVP

## Funcionalidades del MVP

1. **POS completo** - productos, variantes, modificadores, combos
2. **Mesas y áreas** - gestión de mesas por zona
3. **Inventario automático** - recetas y descuento de stock
4. **KDS en tiempo real** - dashboard de cocina con WebSockets
5. **Tickets digitales** - WhatsApp/Email al cliente
6. **Bot WhatsApp con IA** - pedidos en lenguaje natural
7. **Reportes básicos** - ventas día/semana/mes
8. **Multi-sucursal** - preparado para escalar

## Validación

- Restaurante piloto disponible para testing
- Operación: 10 hrs/día
- Región: México
- Objetivo: Piloto acepta pagar ≥$500 MXN/mes

## Agentes Disponibles

Los siguientes agentes están configurados en `.claude/agents/`:

- `senior-software-architect` - Diseño de arquitectura
- `tech-product-manager` - Roadmaps y priorización
- `backend-architect` - Implementación backend
- `frontend-architect` - Implementación frontend
- `security-architect` - Revisión de seguridad
- `devops-automation-engineer` - CI/CD y Docker
- `tech-code-reviewer` - Code review
- `qa-engineer-preventivo` - Testing y QA
- `technical-mentor` - Explicación de conceptos

## Comandos Útiles

```bash
# Backend - compilar
cd backend && ./mvnw clean compile

# Backend - tests
cd backend && ./mvnw test

# Frontend - instalar dependencias
cd frontend && npm install

# Frontend - desarrollo
cd frontend && npm run dev
```

## Notas Importantes

- Java 21 requerido (usar SDKMAN: `sdk install java 21.0.5-tem`)
- Node.js 20+ requerido para frontend
- Seguir OWASP ASVS L2 para seguridad (ver docs/SECURITY.md)
- TDD obligatorio para lógica de negocio
- WebSockets para KDS (no polling)
- Soft delete en la mayoría de entidades
- Orders y payments nunca se borran (auditoría)
