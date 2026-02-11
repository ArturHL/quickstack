# QuickStack POS - Contexto del Proyecto

> Este archivo contiene el contexto necesario para continuar el desarrollo con Claude Code.
> **Última actualización:** 2026-02-10

## Resumen del Proyecto

**QuickStack POS** es un sistema de punto de venta SaaS multi-tenant para restaurantes en México. El objetivo es democratizar tecnología avanzada (predicciones, automatizaciones con IA) para pequeños empresarios.

**Timeline MVP:** 6 meses para validación con piloto

## Stack Tecnológico

| Componente | Tecnología | Hosting |
|------------|------------|---------|
| Frontend | React 18 + Vite + TypeScript + MUI | Vercel |
| Backend | Java 17 + Spring Boot 3.5 | Render (Docker) |
| Base de datos | PostgreSQL 16 | Neon (serverless) |
| Autenticación | Spring Security + JWT (OWASP ASVS L2) | - |
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
- **Auth nativo**: Spring Security + JWT (sin Auth0)

## Estructura del Proyecto

```
quickstack/
├── docs/
│   ├── ARCHITECTURE.md        # Decisiones técnicas detalladas
│   ├── DATABASE_SCHEMA.md     # Esquema completo de BD
│   ├── SECURITY.md            # Visión general de seguridad
│   ├── ROADMAP.md             # Plan de fases del MVP
│   └── security/
│       └── asvs/              # Requisitos OWASP ASVS por capítulo
│           ├── README.md      # Índice y progreso (272 requisitos)
│           └── V01-V14*.md    # 14 capítulos documentados
├── frontend/                  # React + Vite + TypeScript
│   └── src/
├── backend/                   # Multi-module Maven
│   ├── pom.xml               # Parent POM (Java 17)
│   ├── Dockerfile            # Multi-stage, non-root user
│   ├── quickstack-common/    # Utilidades compartidas, seguridad
│   ├── quickstack-tenant/    # Módulo tenants
│   ├── quickstack-branch/    # Módulo sucursales
│   ├── quickstack-user/      # Módulo usuarios
│   ├── quickstack-product/   # Módulo productos
│   ├── quickstack-pos/       # Módulo punto de venta
│   └── quickstack-app/       # Ensamblador (Spring Boot main)
│       └── src/main/resources/
│           ├── application.yml
│           ├── application-dev.yml
│           ├── application-prod.yml
│           ├── logback-spring.xml
│           └── db/migration/  # Flyway migrations (V1-V7)
├── .github/
│   └── workflows/
│       └── ci.yml            # CI: build, test, Semgrep, OWASP Dependency-Check
└── .claude/
    └── agents/               # Agentes personalizados
```

## Fases del MVP

| Fase | Nombre | Estado |
|------|--------|--------|
| 0 | Foundation & Architecture | 🔄 ~50% (0.1 ✅, 0.2 ✅) |
| 1 | Core POS (ventas, mesas, variantes, combos) | ⏳ Pendiente |
| 2 | Inventory (ingredientes, recetas, stock auto) | ⏳ Pendiente |
| 3 | Digital Tickets & KDS | ⏳ Pendiente |
| 4 | Basic Reporting | ⏳ Pendiente |
| 5 | WhatsApp Bot with AI | ⏳ Pendiente |
| 6 | Polish & Pilot Validation | ⏳ Pendiente |

### Sub-fases de Phase 0

| Sub-fase | Nombre | Estado |
|----------|--------|--------|
| 0.1 | Diseño y Documentación | ✅ Completado |
| 0.2 | Infraestructura (CI/CD, BD, Deploy) | ✅ Completado |
| 0.3 | Módulo de Autenticación (ASVS L2) | ⏳ Pendiente |
| 0.4 | Frontend Base + Integración Auth | ⏳ Pendiente |

## Estado Actual (Phase 0.3)

### Phase 0.2 Completado
- [x] Definición de arquitectura y stack
- [x] Creación de estructura monorepo
- [x] Inicialización de frontend (React + Vite)
- [x] Estructura multi-module Maven para backend
- [x] Parent POM con Java 17
- [x] POMs de los 7 módulos Maven
- [x] Documentación (ARCHITECTURE.md, ROADMAP.md, SECURITY.md)
- [x] Configuración de Git y GitHub
- [x] Diseño de modelo de datos (29 tablas, 6 módulos)
- [x] 7 migraciones Flyway creadas (V1-V7)
- [x] DATABASE_SCHEMA.md documentado
- [x] CI/CD con GitHub Actions (Semgrep + OWASP Dependency-Check)
- [x] Spring Boot configurado (application.yml, profiles)
- [x] Logback JSON estructurado
- [x] GlobalExceptionHandler (sin leak de info)
- [x] SecurityConfig con Argon2id
- [x] CORS configurado
- [x] Dockerfile multi-stage con usuario non-root
- [x] Documentación ASVS reorganizada por capítulos
- [x] Proyecto creado en Neon (PostgreSQL 17, us-west-2)
- [x] Migraciones V1-V7 ejecutadas (29 tablas)
- [x] Backend desplegado en Render (Docker)
- [x] Frontend desplegado en Vercel
- [x] Variables de entorno configuradas
- [x] CORS configurado con URL de Vercel

### Pendiente Phase 0.3 (Auth)
- [ ] Endpoints: register, login, refresh, logout, forgot-password, reset-password
- [ ] Argon2id password hashing (configurado, falta implementar)
- [ ] JWT RS256 signing
- [ ] Rate limiting (Bucket4j)
- [ ] Account lockout
- [ ] Refresh token rotation
- [ ] Tests de seguridad

## Base de Datos - 29 Tablas en 6 Módulos

| Módulo | Tablas |
|--------|--------|
| Global Catalogs | subscription_plans, roles, order_status_types, stock_movement_types, unit_types |
| Core | tenants, branches, users, password_reset_tokens, refresh_tokens, login_attempts |
| Catalog | categories, products, product_variants, modifier_groups, modifiers, combos, combo_items |
| Inventory | ingredients, suppliers, recipes, stock_movements, purchase_orders, purchase_order_items |
| POS | areas, tables, customers, orders, order_items, order_item_modifiers, payments, order_status_history |
| Notifications | notification_logs, notification_templates |

## Seguridad (OWASP ASVS L2)

### Progreso

| Capítulo | Cumplidos | Total | Archivo |
|----------|-----------|-------|---------|
| V1 - Architecture | 12 | 38 | `V01-architecture.md` |
| V2 - Authentication | 1 | 57 | `V02-authentication.md` |
| V3 - Session Management | 0 | 19 | `V03-session-management.md` |
| V4 - Access Control | 0 | 9 | `V04-access-control.md` |
| V5 - Validation | 0 | 30 | `V05-validation.md` |
| V6 - Cryptography | 0 | 16 | `V06-cryptography.md` |
| V7 - Error/Logging | 4 | 12 | `V07-error-logging.md` |
| V8 - Data Protection | 0 | 15 | `V08-data-protection.md` |
| V9 - Communication | 0 | 8 | `V09-communication.md` |
| V10 - Malicious Code | 1 | 9 | `V10-malicious-code.md` |
| V11 - Business Logic | 0 | 8 | `V11-business-logic.md` |
| V12 - Files | 0 | 15 | `V12-files-resources.md` |
| V13 - API | 0 | 13 | `V13-api.md` |
| V14 - Configuration | 2 | 23 | `V14-configuration.md` |
| **Total** | **20** | **272** | **7%** |

> Archivos en `docs/security/asvs/`. 41 requisitos marcados N/A (no aplican al MVP).

### Documentación de Seguridad

- `docs/SECURITY.md` - Visión general, threat model, protocolos, compliance
- `docs/security/asvs/README.md` - Índice completo de requisitos ASVS (14 capítulos)

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

# Backend - verificar (compile + test)
cd backend && ./mvnw verify

# Frontend - instalar dependencias
cd frontend && npm install

# Frontend - desarrollo
cd frontend && npm run dev

# Frontend - build
cd frontend && npm run build
```

## Notas Importantes

- **Java 17** requerido (usar SDKMAN: `sdk install java 17.0.10-tem`)
- Node.js 20+ requerido para frontend
- Seguir OWASP ASVS L2 para seguridad (ver `docs/security/asvs/`)
- TDD obligatorio para lógica de negocio
- WebSockets para KDS (no polling)
- Soft delete en la mayoría de entidades
- Orders y payments nunca se borran (auditoría)
- GlobalExceptionHandler evita leak de información interna
- Passwords con Argon2id (Spring Security 6)
