# QuickStack POS - Roadmap del MVP

> **Última actualización:** 2026-02-18
> **Estado:** Phase 0.3 COMPLETADO (340 tests, 6/6 sprints) | Próximo: Phase 0.4

## Vision Summary

Sistema de punto de venta multi-sucursal con inventario automático y bot WhatsApp/IA que permita a restaurantes mexicanos gestionar pedidos, ventas, inventario y reportes básicos, validado con un restaurante piloto antes del lanzamiento comercial.

**Timeline:** 6 meses hasta piloto validado.

---

## Stack Tecnológico

| Componente | Tecnología | Hosting |
|------------|------------|---------|
| Frontend | React 19 + Vite + TypeScript + MUI | Vercel |
| Backend | Java 17 + Spring Boot 3.5 | Render (Docker) |
| Base de datos | PostgreSQL (29 tablas, multi-tenant) | Neon (serverless) |
| Autenticación | Spring Security + JWT (OWASP ASVS L2) | - |
| State Management | Zustand | - |
| HTTP Client | TanStack Query + Axios | - |
| ORM | Spring Data JPA | - |
| Automatizaciones | n8n | Self-hosted o Cloud |

---

## Resumen de Fases

| Fase | Nombre | Objetivo | Estado |
|------|--------|----------|--------|
| 0 | Foundation | Auth nativo (ASVS L2) + BD + Deploy + CI/CD | ✅ 100% (0.1-0.4 completadas) |
| 1 | Core POS | Crear pedidos con productos, variantes, modificadores | ⏳ Pendiente |
| 2 | Inventory Management | Ingredientes, recetas, descuento automático de stock | ⏳ Pendiente |
| 3 | Digital Tickets & KDS | Tickets digitales (WhatsApp/Email) + KDS en tiempo real | ⏳ Pendiente |
| 4 | Basic Reporting | Dashboard de ventas día/semana/mes | ⏳ Pendiente |
| 5 | WhatsApp Bot | Pedidos con IA integrados al POS | ⏳ Pendiente |
| 6 | Polish & Tables | Bugs, UX, mesas/áreas, onboarding, validación final | ⏳ Pendiente |

---

## Critical Path

```
Phase 0 → Phase 1 → Phase 2 → Phase 3 → Phase 6
```

> Phase 4 y 5 pueden desarrollarse en paralelo si es necesario acelerar.

---

## Phase 0: Foundation & Architecture

**Goal**: Establecer la arquitectura base con autenticación nativa segura (OWASP ASVS L2).

**Est. Effort:** 4-5 semanas

**Enfoque:** Auth First - El módulo de autenticación se implementa completo antes de features de negocio.

### Sub-fases

| Sub-fase | Nombre | Estado |
|----------|--------|--------|
| 0.1 | Diseño y Documentación | ✅ Completado |
| 0.2 | Infraestructura (CI/CD, BD, Deploy) | ✅ Completado |
| 0.3 | Módulo de Autenticación (ASVS L2) | ✅ Completado (340 tests backend, 8 endpoints) |
| 0.4 | Frontend Base + Integración Auth | ✅ Completado (38 tests frontend, 4/4 sprints) |

---

### Phase 0.1: Diseño y Documentación ✅

- [x] Multi-tenancy: BD compartida con `tenant_id`
- [x] Monorepo: Frontend y backend en mismo repo
- [x] ORM: JPA/Hibernate con Flyway migrations
- [x] State management: Zustand
- [x] Multi-module Maven: Backend modular por feature
- [x] Esquema de 29 tablas diseñado (6 módulos)
- [x] Documentación ASVS L2 (SECURITY.md)
- [x] Threat model documentado
- [x] Migraciones Flyway (V1-V7)

---

### Phase 0.2: Infraestructura

**Est. Effort:** 3-4 días

#### CI/CD Pipeline (GitHub Actions)
- [x] Workflow: Build + Test en cada PR
- [x] SAST: Semgrep para análisis estático
- [x] SCA: OWASP Dependency-Check
- [x] npm audit para frontend
- [ ] Branch protection en `main` (configurar en GitHub UI)

#### Base de Datos
- [x] Crear proyecto en Neon (PostgreSQL 17, us-west-2)
- [x] Connection pooling habilitado (pooler endpoint)
- [x] Ejecutar migraciones V1-V7 (29 tablas creadas)
- [x] Seed data inicial (roles, plans, status types, unit types)
- [ ] Crear roles de BD adicionales (quickstack_readonly) - opcional

#### Backend Base
- [x] Crear POMs de cada módulo Maven (7 módulos)
- [x] Configurar Spring Boot application.yml (con profiles dev/prod)
- [x] Configurar Flyway (config lista, pendiente ejecución)
- [x] Health check endpoint (`/actuator/health` + `/api/v1/health`)
- [x] Logback JSON estructurado
- [x] GlobalExceptionHandler (errores sin leak de info)
- [x] CORS configurado
- [x] SecurityConfig con Argon2id password encoder

#### Deploy
- [x] Dockerfile multi-stage (usuario non-root)
- [x] Configurar Render (backend - Docker)
- [x] Configurar Vercel (frontend - Vite)
- [x] Variables de entorno en Render
- [x] CORS configurado con URL de Vercel

**Success Criteria 0.2:** ✅ Completado
- [x] `mvn compile` pasa localmente
- [x] `mvn verify` pasa en CI
- [x] Migraciones ejecutadas en Neon (V1-V7)
- [x] Health check responde en Render
- [x] Deploy automático funciona (push → deploy)

**URLs de Producción:**
- Backend: https://quickstack-api.onrender.com
- Frontend: https://quickstack-drab.vercel.app
- Database: Neon (us-west-2)

---

### Phase 0.3: Módulo de Autenticación (ASVS L2) ✅

**340 tests | 6 sprints | 8 endpoints**

> **Roadmap detallado archivado:** `docs/archive/PHASE_0.3_AUTH_ROADMAP.md`

- [x] Sprint 1: Properties, excepciones custom, SecureTokenGenerator, IpAddressExtractor (61 tests)
- [x] Sprint 2: Argon2id + pepper, HIBP k-Anonymity, UserService multi-tenant (61 tests)
- [x] Sprint 3: JWT RS256 2048-bit, algorithm confusion protection, key rotation (55 tests)
- [x] Sprint 4: Login/Refresh/Logout, account lockout (5 intentos/15 min), cookie `__Host-` (40 tests)
- [x] Sprint 5: Rate limiting Bucket4j (IP: 10/min, email: 5/min), password reset timing-safe (~80 tests)
- [x] Sprint 6: SessionService, register endpoint, multi-tenant isolation, penetration tests (123 tests)

**Endpoints:** `/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`,
`/auth/forgot-password`, `/auth/reset-password`, `/users/me/sessions` (GET + DELETE)

**ASVS Compliance:** V2 26% | V3 74% | V6 56%

**Success Criteria 0.3:** ✅ Completado

---

### Phase 0.4: Frontend Base + Integración Auth ✅

**Estado:** COMPLETADA (4/4 sprints) | **38 tests frontend**

> **Roadmap detallado:** `docs/roadmap/PHASE_0.4_FRONTEND_BASE_ROADMAP.md`

#### Sprint 1: Setup de Infraestructura ✅
- [x] Configurar Vite + React 19 + TypeScript
- [x] Configurar MUI theme (paleta QuickStack)
- [x] Configurar React Router 6.30 (createBrowserRouter)
- [x] Configurar Axios con interceptors de auth
- [x] Configurar Zustand stores (authStore, access token in-memory)
- [x] Configurar TanStack Query 5.76
- [x] Configurar MSW 2.7 para tests
- [x] Configurar Vitest con jsdom
- [x] 5 tests (authStore)

#### Sprint 2: Login y Registro ✅
- [x] authApi.ts con 8 funciones (endpoints)
- [x] useAuthQuery.ts hooks (TanStack Query)
- [x] LoginPage con manejo de errores 401/423/429
- [x] RegisterPage con validación de password
- [x] MSW handlers expandidos (factories de error)
- [x] renderWithProviders test utils
- [x] 23 tests (5 authStore + 10 LoginPage + 8 RegisterPage)

#### Sprint 3: Auth Completo + Rutas Protegidas ✅
- [x] ForgotPasswordPage (timing-safe submit)
- [x] ResetPasswordPage (lee ?token de URL)
- [x] ProtectedRoute component (Navigate + Outlet pattern)
- [x] imperativeNavigate para navegación fuera de React
- [x] axiosInterceptor mejorado con manejo de 401
- [x] Auto-refresh de token (silencioso)
- [x] Post-login redirect con state.from
- [x] 38 tests totales (8 axiosInterceptor + 7 ProtectedRoute + 23 previos)

#### Sprint 4: Dashboard Base + Calidad ✅
- [x] AppLayout (Sidebar 240px + TopBar responsive)
- [x] Sidebar con navegación activa y placeholders
- [x] TopBar con nombre usuario, logout, hamburger mobile
- [x] DashboardPage con Grid, Cards y bienvenida personalizada
- [x] ErrorBoundary global con fallback UI y stack trace dev
- [x] GlobalErrorSnackbar para errores 5xx/network con MUI
- [x] Auditoría de seguridad frontend (0 vulnerabilidades)
- [x] Build de producción exitoso (581KB gzipped)
- [x] 38 tests pasando, ESLint y TypeScript sin errores

**Success Criteria 0.4:** ✅ TODOS CUMPLIDOS
- ✅ Usuario puede registrarse
- ✅ Usuario puede hacer login
- ✅ Usuario ve dashboard después de login
- ✅ Token se refresca automáticamente
- ✅ Logout funciona correctamente
- ✅ Flujo completo de recuperación de password
- ✅ Rutas protegidas redirigen correctamente
- ✅ 100% tests pasan sin errores (38/38)
- ✅ npm audit sin vulnerabilidades críticas (0 vulnerabilidades)
- ✅ Layout responsive con sidebar colapsable
- ✅ Error handling global (ErrorBoundary + Snackbar)

---

### Success Criteria Phase 0 Completa

| Criterio | Métrica |
|----------|---------|
| CI/CD funciona | PRs bloqueados sin checks verdes |
| Auth seguro | 100% tests de seguridad pasan |
| Deploys automáticos | Push a main → deploy en <5 min |
| ASVS L2 V2 cumplido | Checklist de auth completado |
| Frontend funcional | Flujo login → dashboard funciona |
| Zero secrets en código | Ningún secret hardcodeado |

---

## Phase 1: Core POS - Ventas Completas

**Goal**: Crear y completar pedidos con productos, variantes, modificadores, combos y múltiples tipos de servicio.

**Dependencies**: Phase 0 completado

**Est. Effort:** 8-10 semanas

**Status**: En progreso — Phase 1.1 (Sprint 2/6 completado)

> **Nota:** Phase 1 se divide en sub-fases para facilitar desarrollo incremental y validación temprana con el piloto.

### Sub-fases de Phase 1

| Sub-fase | Nombre | Duración | Estado |
|----------|--------|----------|--------|
| 1.1 | Catálogo Base (Productos + Variantes) | 3 semanas | ⏳ En Progreso (Sprint 2/6) |
| 1.2 | Modificadores + Combos | 2 semanas | ⏳ Pendiente |
| 1.3 | Sistema de Pedidos + Pagos | 2-3 semanas | ⏳ Pendiente |
| 1.4 | Frontend POS | 2-3 semanas | ⏳ Pendiente |

### Scope de Phase 1

**Tipos de servicio soportados:**
- DINE_IN (Mesa - con mesas y áreas)
- COUNTER (Mostrador)
- DELIVERY (Entrega directa, sin Uber/Rappi)
- TAKEOUT (Para llevar)

**Features de producto:**
- Productos simples con precio base
- Variantes de producto (tamaños: Chico, Mediano, Grande)
- Modificadores con grupos (Extras, Quitar ingredientes)
- Combos con pricing especial

### Entregables

#### Backend
- [ ] CRUD de categorías jerárquicas
- [ ] CRUD de productos simples (nombre, precio, categoría, imagen)
- [ ] CRUD de variantes de producto con price_adjustment
- [ ] CRUD de modifier groups y modifiers
- [ ] CRUD de combos con combo_items
- [ ] CRUD de áreas y mesas
- [ ] CRUD de clientes (para delivery)
- [ ] API de pedidos: crear, agregar items, modificar, cerrar
- [ ] API: registrar pago en efectivo (solo CASH en MVP)
- [ ] Cálculo de totales (subtotal, tax, modifiers, total)
- [ ] Endpoints de listado de pedidos del día
- [ ] Multi-sucursal: selector de sucursal activa
- [ ] CRUD de sucursales (admin only)

#### Frontend
- [ ] Pantalla de catálogo (productos agrupados por categoría)
- [ ] Vista de producto con selección de variantes
- [ ] Selector de modificadores (min/max selections)
- [ ] Carrito de compras con totales calculados
- [ ] Selector de tipo de servicio (DINE_IN/COUNTER/DELIVERY/TAKEOUT)
- [ ] Selector de mesa (si DINE_IN)
- [ ] Formulario de cliente (si DELIVERY)
- [ ] Pantalla de pago (solo efectivo, calcular cambio)
- [ ] Vista de pedidos del día (filtros por estado)
- [ ] CRUD de productos (admin)
- [ ] CRUD de sucursales (admin)

### Success Criteria

- Cajero puede completar venta de mostrador con 3+ productos en <60 segundos
- Sistema soporta pedidos con variantes + modificadores
- Cajero puede crear pedido para mesa específica
- Cajero puede registrar pedido de delivery con datos de cliente
- Sistema calcula totales correctamente (base + variant + modifiers + tax)
- Sistema mantiene historial de pedidos por sucursal

### Validation Checkpoint

- **Demo con piloto**: Mostrar flujo completo (mesa, mostrador, delivery)
- **Pregunta clave**: ¿El flujo es más rápido que su método actual?

---

### Phase 1.1: Catálogo Base (Productos + Variantes)

**Duración:** 3 semanas (6 sprints) | **Status:** ⏳ En Progreso (Sprint 3/6 completado)

> **Roadmap detallado:** `docs/roadmap/PHASE_1.1_BACKEND_CATALOG.md`

**Scope:**
- [x] Entidades: Category, Product, ProductVariant
- [x] CRUD completo de categorías jerárquicas (2 niveles)
- [x] CRUD completo de productos (SIMPLE y VARIANT)
- [ ] CRUD de variantes detallado (Sprint 4)
- [ ] Endpoint `/api/v1/menu` optimizado para POS (Sprint 6)
- [x] Permisos por rol (OWNER/MANAGER/CASHIER)
- [x] Soporte para Docker 29 (Testcontainers 2.0.3)
- [x] Integridad referencial en tests (fixtures de Tenant/Plan)
- [x] ~500 tests backend totales acumulados

**Deuda técnica aceptada:**
- Modifier groups y modifiers → Phase 1.2
- Combos → Phase 1.2
- Imágenes de productos (upload) → Phase 1.3
- Disponibilidad horaria → Phase 1.3

---

### Phase 1.2: Modificadores y Combos

**Duración:** 2 semanas (4 sprints) | **Status:** ⏳ Pendiente

> **Roadmap detallado:** `docs/roadmap/PHASE_1.2_MODIFIERS_COMBOS.md`

**Dependencies:** Phase 1.1 completada

**Scope:**
- [ ] CRUD de modifier groups (Extras, Sin ingredientes, etc.)
- [ ] CRUD de modifiers con price_adjustment
- [ ] Validaciones min/max selections por grupo
- [ ] CRUD de combos con combo_items
- [ ] Pricing especial de combos
- [ ] Actualizar endpoint `/api/v1/menu` con modifiers
- [ ] 12 endpoints REST nuevos
- [ ] ~80 tests nuevos (acumulado: ~830 tests backend)

---

### Phase 1.3: Sistema de Pedidos y Pagos

**Duración:** 3 semanas (6 sprints) | **Status:** ⏳ Pendiente

> **Roadmap detallado:** `docs/roadmap/PHASE_1.3_ORDERS_PAYMENTS.md`

**Dependencies:** Phase 1.2 completada

**Scope Backend:**
- [ ] Nuevo módulo: `quickstack-order`
- [ ] CRUD de sucursales (branches) con selector activo
- [ ] CRUD de áreas y mesas por sucursal
- [ ] CRUD de clientes (para delivery)
- [ ] Entidades: Order, OrderItem, OrderItemModifier, Payment
- [ ] API: crear pedido (4 tipos de servicio: DINE_IN/COUNTER/DELIVERY/TAKEOUT)
- [ ] API: agregar/modificar items al pedido
- [ ] API: calcular totales (base + variants + modifiers + tax)
- [ ] API: cerrar pedido con cambio de estado
- [ ] API: registrar pago en efectivo
- [ ] API: listar pedidos del día (filtros por estado, tipo servicio, sucursal)
- [ ] Order number format: `ORD-YYYYMMDD-XXX` (daily sequence)
- [ ] Validaciones de negocio (producto disponible, mesa ocupada, etc.)
- [ ] 28 endpoints REST nuevos
- [ ] ~150 tests nuevos (acumulado: ~1157 tests backend)

---

### Phase 1.4: Frontend POS

**Duración:** 3 semanas (6 sprints) | **Status:** ⏳ Pendiente

> **Roadmap detallado:** `docs/roadmap/PHASE_1.4_FRONTEND_POS.md`

**Dependencies:** Phase 1.3 completada

**Scope Frontend:**
- [ ] Pantalla de catálogo (grid responsive con búsqueda)
- [ ] Modal de producto con variantes + modificadores
- [ ] Carrito de compras con cálculo de totales en tiempo real
- [ ] Selector de tipo de servicio (DINE_IN/COUNTER/DELIVERY/TAKEOUT)
- [ ] Selector de mesa (si DINE_IN) con visualización de disponibilidad
- [ ] Formulario de cliente (si DELIVERY) con validación
- [ ] Pantalla de pago (solo efectivo, calcular cambio)
- [ ] Vista de pedidos del día con filtros
- [ ] CRUD de productos (admin) con upload de imágenes
- [ ] CRUD de sucursales (admin)
- [ ] Selector de sucursal activa (persistido en localStorage)
- [ ] State management: posStore, cartStore, branchStore (Zustand)
- [ ] 15 pantallas/componentes principales
- [ ] ~120 tests frontend nuevos (acumulado: ~158 tests frontend)

---

## Phase 2: Inventory Management

**Goal**: Gestión de inventario con descuento automático de stock basado en recetas.

**Dependencies**: Phase 1 validado con piloto

**Est. Effort:** 4-5 semanas

### Scope de Phase 2

**Features de inventario:**
- Ingredientes con unidades de medida
- Recetas por producto/variante
- Descuento automático de stock al cerrar pedido
- Alertas de stock bajo
- Órdenes de compra a proveedores
- Historial de movimientos de stock (audit trail)

### Entregables

#### Backend
- [ ] CRUD de ingredientes (nombre, unidad, cost_per_unit, stock actual/mínimo)
- [ ] CRUD de proveedores
- [ ] CRUD de recetas (producto/variante → ingredientes con cantidades)
- [ ] CRUD de órdenes de compra
- [ ] Lógica: auto-deducción de stock al cerrar pedido
- [ ] Registro de stock_movements para cada cambio
- [ ] API: alertas de ingredientes con stock bajo
- [ ] API: reporte de costo de bienes vendidos (COGS)

#### Frontend
- [ ] CRUD de ingredientes con stock actual
- [ ] Asociar recetas a productos/variantes
- [ ] Pantalla de ingredientes con alerta visual (stock bajo)
- [ ] CRUD de proveedores
- [ ] Crear orden de compra
- [ ] Recibir orden de compra (actualiza stock)
- [ ] Historial de movimientos de inventario
- [ ] Dashboard de stock actual

### Success Criteria

- Al cerrar pedido, stock de ingredientes se reduce automáticamente según recetas
- Sistema registra 100% de movimientos en stock_movements
- Admin puede ver ingredientes con stock bajo en tiempo real
- Órdenes de compra actualizan stock al marcarse como "RECEIVED"
- Cálculo de COGS es preciso

### Validation Checkpoint

- **Demo con piloto**: Mostrar reducción automática de stock
- **Pregunta clave**: ¿Elimina el conteo manual de inventario?

---

## Phase 3: Digital Tickets & KDS

**Goal**: Tickets digitales enviados por WhatsApp/Email + Kitchen Display System en tiempo real.

**Dependencies**: Phase 1 completado

**Est. Effort:** 4 semanas

### Decisión Clave: NO Impresión Física

- **NO impresoras térmicas** en MVP
- Tickets digitales enviados **opcionalmente** por cajero
- Canales: WhatsApp y Email
- KDS en pantalla de cocina con actualización en tiempo real (WebSockets)

### Decisiones Técnicas

- [ ] Estrategia de notificaciones (n8n + WhatsApp Business API + SMTP)
- [ ] WebSocket server para KDS real-time updates
- [ ] Formato de ticket digital (HTML responsive)

### Entregables

#### Backend
- [ ] Diseño de template HTML de ticket digital (responsive)
- [ ] API: enviar ticket por WhatsApp (integración n8n)
- [ ] API: enviar ticket por Email (SMTP)
- [ ] Registro en notification_logs (audit trail)
- [ ] WebSocket server: emitir eventos de order status change
- [ ] API de KDS: obtener pedidos activos por sucursal
- [ ] API de KDS: actualizar kds_status de order_items
- [ ] Endpoint de reenvío de tickets históricos

#### Frontend
- [ ] Botón "Enviar ticket" post-cierre de pedido (modal: WhatsApp o Email)
- [ ] Input de número WhatsApp/Email en modal
- [ ] Opción de reenviar tickets desde historial de pedidos
- [ ] Pantalla KDS (Kitchen Display System)
- [ ] Vista de pedidos activos agrupados por estado
- [ ] Drag & drop o botones para cambiar estado de items
- [ ] Actualización en tiempo real vía WebSocket
- [ ] Indicador de tiempo transcurrido por pedido

### Success Criteria

- Cajero puede enviar ticket digital por WhatsApp en <10 segundos
- Ticket digital incluye: fecha, hora, sucursal, items, modificadores, total
- 100% de envíos registrados en notification_logs
- KDS muestra pedidos activos en tiempo real
- Cocina puede marcar items como PREPARING → READY
- WebSocket actualiza pantallas sin refresh

### Validation Checkpoint

- **Demo con piloto**: Envío de ticket y uso de KDS
- **Pregunta clave**: ¿Preferible a tickets impresos? ¿KDS mejora flujo de cocina?

---

## Phase 4: Basic Reporting

**Goal**: Proveer reportes básicos de ventas.

**Dependencies**: Phase 1 completado, datos reales generándose

**Est. Effort:** 3 semanas

### Entregables

#### Backend
- [ ] API: métricas del día (total ventas, # pedidos, ticket promedio)
- [ ] API: reporte de ventas por rango de fechas
- [ ] API: ventas agrupadas por sucursal
- [ ] API: productos más vendidos (top 10)
- [ ] API: ventas por tipo de servicio (DINE_IN/COUNTER/DELIVERY/TAKEOUT)
- [ ] Exportar reportes a CSV

#### Frontend
- [ ] Dashboard con métricas del día (cards)
- [ ] Gráfica de ventas por día (últimos 30 días)
- [ ] Filtros: sucursal, rango de fechas, tipo de servicio
- [ ] Tabla de productos más vendidos
- [ ] Botón "Exportar a CSV"

### Success Criteria

- Admin puede ver ventas totales del día en <5 segundos
- Gráficas se actualizan al cambiar filtros sin lag
- Exportación funciona con dataset de 1000+ pedidos
- Reportes son precisos (match con sumas directas de BD)

---

## Phase 5: WhatsApp Bot with AI

**Goal**: Pedidos vía WhatsApp con lenguaje natural integrados al POS.

**Dependencies**: Phase 1 completado (catálogo existe), Phase 3 opcional (reutiliza n8n)

**Est. Effort:** 5-6 semanas

### Decisiones Técnicas

- [ ] Proveedor WhatsApp Business API (Meta Cloud API recomendado)
- [ ] Modelo de IA (OpenAI GPT-4o-mini recomendado)
- [ ] Estrategia de context management (conversaciones por cliente)

### Entregables

#### Backend
- [ ] API: webhook para recibir mensajes de WhatsApp
- [ ] API: obtener catálogo de productos para IA context
- [ ] API: crear pedido desde estructura parseada por IA
- [ ] Validaciones: stock disponible, branch abierto

#### n8n Workflows
- [ ] Setup WhatsApp Business API
- [ ] Workflow: recibir mensaje → IA parsea intención → responder
- [ ] Workflow: crear pedido en POS desde intención confirmada
- [ ] Workflow: enviar confirmación de pedido al cliente
- [ ] Manejo de casos edge (producto no existe, fuera de horario)
- [ ] Configurar horarios de atención por sucursal

#### Frontend
- [ ] Admin: configuración de horarios de atención
- [ ] Admin: ver log de conversaciones de WhatsApp
- [ ] Admin: pedidos de WhatsApp marcados con source=WHATSAPP

### Success Criteria

- Bot entiende 80%+ de pedidos simples en lenguaje natural
- Pedidos de bot aparecen en POS marcados como source="WHATSAPP"
- Bot responde en <10 segundos
- Bot maneja casos edge sin romper conversación
- Piloto recibe ≥20% de pedidos vía WhatsApp después de 2 semanas

### Validation Checkpoint

- **Pregunta clave**: ¿Clientes prefieren WhatsApp vs. llamada telefónica?

---

## Phase 6: Polish & Pilot Validation

**Goal**: Refinar producto, agregar mesas/áreas, y preparar para onboarding de más clientes.

**Dependencies**: Phases 1-5 completados

**Est. Effort:** 3-4 semanas

### Entregables

#### Backend
- [ ] Optimizaciones de performance (indexing, query optimization)
- [ ] Rate limiting en APIs públicas
- [ ] Testing de carga básico (JMeter o similar)

#### Frontend
- [ ] Fixing de bugs críticos reportados por piloto
- [ ] Mejoras de UX basadas en uso real
- [ ] Flujo de onboarding para nuevos restaurantes
- [ ] Pantalla de gestión de mesas/áreas
- [ ] Vista de plano de mesas (opcional, simple grid)

#### DevOps
- [ ] Backup automático de BD (Neon snapshots)
- [ ] Monitoring básico (uptime, error rate)
- [ ] Recovery plan documentado
- [ ] Alertas de errores críticos

#### Documentación
- [ ] Manual de usuario (PDF o web)
- [ ] Videos de onboarding (opcional)

### Success Criteria

- Piloto ha operado 100% con QuickStack durante 2+ semanas
- Cero pérdida de datos
- Tiempo de setup para nuevo restaurante <2 horas
- Zero-downtime deployments funcionando
- Bugs críticos: 0, bugs menores: <5 pendientes

---

## Checkpoints de Validación con Piloto

| Milestone | Métrica de Éxito | Señal de Alerta |
|-----------|-----------------|-----------------|
| Post-Phase 1 | Piloto usa POS para ≥80% de ventas | Rechazo del sistema, workarounds manuales |
| Post-Phase 2 | 100% de productos con recetas configuradas | Stock manual sigue siendo necesario |
| Post-Phase 3 | ≥50% de clientes reciben ticket digital | Clientes solicitan tickets impresos |
| Post-Phase 4 | Dueño revisa reportes ≥3 veces/semana | Reportes ignorados o erróneos |
| Post-Phase 5 | ≥20% de pedidos de WhatsApp | Cero adopción después de 2 semanas |
| Post-Phase 6 | Piloto acepta pagar ≥$500 MXN/mes | Rechazo de pago |

---

## Features Explícitamente Fuera del MVP

**Características confirmadas FUERA del alcance inicial:**

- Pagos con tarjeta (solo efectivo en MVP)
- Pagos parciales / split payments
- Propinas dentro del sistema
- Roles mixtos (un usuario = un rol)
- Integración con Uber Eats / Rappi
- Programas de lealtad / puntos
- Facturación electrónica (CFDI)
- Multi-idioma (solo español)
- App móvil nativa (solo web responsive)
- Impresión física de tickets (solo digital)
- Transferencias de stock entre sucursales (diseñado, no implementado)

**Aclaración sobre multi-sucursal:**
- Esquema de BD diseñado para multi-sucursal desde el inicio
- MVP funcionará con **1 sucursal** por tenant
- Expansión a múltiples sucursales es Phase 7+ (post-piloto)

---

## Risk Matrix

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Free tiers se agotan (Neon/Render) | Media | Alto | Monitoreo diario, upgrade plan ready |
| Piloto abandona proyecto | Baja | Crítico | Comunicación semanal constante |
| WhatsApp Business API costoso | Media | Medio | Validar pricing antes de Phase 5 |
| Bot de IA no es preciso | Alta | Medio | Scope reducido, fallback a humano |
| Performance issues con inventario | Media | Alto | Load testing, indexing agresivo |
| WebSockets no escalan | Baja | Alto | Plan B: polling cada 5s |
| Timeline de 6 meses muy agresivo | Alta | Alto | Priorizar ruthlessly, MVP estricto |

---

## Changelog

### 2026-02-21
- **Phase 1.1 Sprint 3 completado — Product Management:**
  - `ProductCreateRequest`, `ProductUpdateRequest`, `ProductResponse`, `ProductSummaryResponse` DTOs.
  - Soporte para productos `SIMPLE` y `VARIANT` con gestión de precios y SKUs.
  - `ProductService`: CRUD completo con validaciones multi-tenant y reglas de negocio.
  - **Infraestructura:** Actualización a Testcontainers 2.0.3 para compatibilidad con Docker 29.
  - **Integridad:** Implementación de fixtures automáticos (Tenant/Plan) en tests de repositorio para cumplir FKs.
  - **Fix SQL:** CAST explícito en búsquedas de ProductRepository para evitar errores de tipo en Postgres.
  - ~500 tests backend acumulados pasando al 100%.
  - **Phase 1.1 COMPLETADA.**

### 2026-02-20
- **Phase 1.1 Sprint 2 completado — Category Management:**
  - `CategoryCreateRequest`, `CategoryUpdateRequest`, `CategoryResponse`, `CategorySummaryResponse` DTOs
  - `BusinessRuleException` (HTTP 409, código configurable) en quickstack-common
  - `CategoryService`: create, update, delete (soft), get, restore, list — todo @Transactional, multi-tenant seguro
  - `CatalogPermissionEvaluator`: canManageCatalog, canDeleteCategory, canRestoreCategory, canViewInactive
  - `CategoryController`: 6 endpoints REST (GET list, POST create, GET by id, PUT update, DELETE, POST restore)
  - `CategoryIntegrationTest`: 12 tests end-to-end (requieren Docker)
  - SecurityConfig actualizado con rutas de catálogo
  - 58 tests nuevos pasando (32 quickstack-app + 26 quickstack-product)
  - Acumulado: ~436 tests backend (378 Phase 0 + 58 Sprint 2)

### 2026-02-19 (Noche)
- **Phase 1.1 Sprint 1 completado:**
  - Entidades y repositorios para Category, Product y ProductVariant implementados con soft delete.
  - Soporte multi-tenant integrado a nivel base de datos y repositorios.
  - Configurado Testcontainers + Flyway para tests de integración.
- **Phase 1 reestructurada en 4 sub-fases:**
  - Phase 1.1: Catálogo Base (3 semanas, 6 sprints, ~250 tests, 22 endpoints)
  - Phase 1.2: Modificadores + Combos (2 semanas, 4 sprints, ~80 tests, 12 endpoints)
  - Phase 1.3: Sistema de Pedidos + Pagos (3 semanas, 6 sprints, ~150 tests, 28 endpoints)
  - Phase 1.4: Frontend POS (3 semanas, 6 sprints, ~120 tests, 15 pantallas)
  - Estimación total actualizada: 11 semanas (antes 5-6)
  - Total estimado: ~600 tests nuevos, 62 endpoints REST
- **4 roadmaps detallados creados:**
  - `PHASE_1.1_BACKEND_CATALOG.md` (47KB, renombrado desde DRAFT)
  - `PHASE_1.2_MODIFIERS_COMBOS.md` (33KB, nuevo)
  - `PHASE_1.3_ORDERS_PAYMENTS.md` (45KB, nuevo)
  - `PHASE_1.4_FRONTEND_POS.md` (43KB, nuevo)
  - Total: 168KB de documentación detallada con sprints, tareas y criterios de aceptación
- **Documentación actualizada:**
  - `CLAUDE.md`: Phase 0 marcada 100% completada, Phase 1 con 4 sub-fases
  - `.context/completed-sprints.md`: Resumen Phase 0 + estructura Phase 1 planeada
  - `ROADMAP.md`: Sub-fases con referencias a roadmaps detallados
- **Archivado:** `docs/archive/PHASE_0.4_FRONTEND_BASE_ROADMAP.md`

### 2026-02-19 (Tarde)
- **Phase 0.4 COMPLETADA — Sprint 4/4 Dashboard + Calidad:**
  - AppLayout: Sidebar (240px) + TopBar responsive con navegación
  - DashboardPage: Grid con Cards de estado, bienvenida personalizada
  - ErrorBoundary: captura errores global, fallback UI, stack trace dev
  - GlobalErrorSnackbar: Snackbar MUI para errores 5xx/network
  - Calidad: 38/38 tests, 0 vulnerabilidades, build exitoso (581KB)
  - Seguridad auditada: tokens en memoria, sin dangerouslySetInnerHTML
  - **Phase 0 (Foundation) 100% COMPLETADA**

### 2026-02-19 (Mañana)
- **Documentación actualizada con progreso Sprint 3:**
  - ROADMAP.md, PHASE_0.4_FRONTEND_BASE_ROADMAP.md actualizados
  - ASVS: 70/272 requisitos (26%), +5 controles frontend
  - Sprints 1-3 marcados completos con Definition of Done
- **Phase 0.4 Sprint 3 completado (38 tests totales):**
  - ForgotPasswordPage y ResetPasswordPage implementadas
  - ProtectedRoute component con Navigate + Outlet pattern
  - imperativeNavigate para navegación desde fuera de React
  - axiosInterceptor mejorado con manejo de 401 y auto-refresh
  - Tests: axiosInterceptor (8), ProtectedRoute (7)
  - Progreso: 3/4 sprints completados, 66 tests frontend
- **Phase 0.4 Sprint 2 completado (23 tests):**
  - authApi.ts con 8 funciones para endpoints backend
  - useAuthQuery.ts con hooks TanStack Query
  - LoginPage y RegisterPage completos con validación
  - MSW handlers expandidos con factories de error
- **Phase 0.4 Sprint 1 completado (5 tests):**
  - Setup de infraestructura: MUI 5.17, Zustand 4.5, TanStack Query 5.76
  - React Router 6.30, Axios 1.13, MSW 2.7, Vitest 3.2
  - authStore (Zustand), axiosInstance con interceptors, QueryClient, MUI theme
- **Stack actualizado:** React 19 confirmado en producción

### 2026-02-18
- **Reorganizacion de documentacion:**
  - `PHASE_0.3_AUTH_ROADMAP.md` archivado en `docs/archive/`
  - ROADMAP.md actualizado con estado real de Phase 0.3 (completado)
  - Decisiones criticas de auth migradas a ARCHITECTURE.md y SECURITY.md
  - Convencion de archivado documentada en `docs/archive/README.md`

### 2026-02-17
- **Phase 0.3 COMPLETADO (340 tests totales, 6/6 sprints):**
  - Sprint 6: SessionService, register endpoint, multi-tenant isolation, penetration tests (123 tests nuevos)
  - Sprint 5: Rate limiting Bucket4j (IP: 10/min, email: 5/min), password reset (SHA-256, timing-safe, ~80 tests)
  - Sprint 4: Login/Refresh/Logout, account lockout, refresh token rotation con family tracking (40 tests)
  - ASVS: V2 26%, V3 74%, V6 56%
  - Checkpoint de Seguridad #3 completado (penetration tests, multi-tenant isolation)

### 2026-02-16
- **Phase 0.3 Sprint 3 completado (55 tests nuevos, 177 total):**
  - JwtConfig: Carga RSA keys desde Base64 o PEM, validación 2048 bits, rotación (15 tests)
  - JwtService: Generación y validación JWT con RS256, algorithm confusion protection (25 tests)
  - JwtAuthenticationFilter: Extracción Bearer token, SecurityContext con principal (15 tests)
  - SecurityConfig actualizado para incluir JWT filter
  - ASVS: V3.5.3, V6.2.1, V6.2.2, V6.2.5, V6.2.6, V6.2.7, V6.3.1, V6.3.2 cumplidos

### 2026-02-11
- **Phase 0.3 Sprint 2 completado (61 tests nuevos, 122 total):**
  - PasswordService: Argon2id con pepper versionado y timing-safe comparison (29 tests)
  - HibpClient: k-Anonymity breach detection con retry y WireMock tests (16 tests)
  - UserService: Registro multi-tenant con validación completa (16 tests)
  - User entity y UserRepository con queries multi-tenant
  - PasswordBreachChecker interface para desacoplar HIBP de quickstack-user
  - Checkpoint de Seguridad #1 completado (Argon2id, timing-safe, k-Anonymity)
  - ASVS: V2.1.1, V2.1.7, V2.4.1, V2.4.5 cumplidos

- **Phase 0.3 Sprint 1 completado (61 tests):**
  - Properties Classes: JwtProperties, PasswordProperties, RateLimitProperties, CookieProperties
  - Excepciones Custom con headers HTTP correctos (Retry-After, WWW-Authenticate, X-Locked-Until)
  - SecureTokenGenerator (32 bytes URL-safe Base64, SecureRandom)
  - IpAddressExtractor con protección contra header injection
  - GlobalExceptionHandler actualizado para excepciones de auth
  - Configuración `quickstack:` en application.yml
- **Documentación:**
  - Creado PHASE_0.3_AUTH_ROADMAP.md (807 líneas, 6 sprints, 22 tareas backend, 11 QA)
  - Procedimiento de rotación de JWT keys documentado
  - Decisiones de seguridad confirmadas (HIBP blockOnFailure=true)
- **CI fixes:**
  - .semgrepignore para excluir skill reference docs
  - package-lock.json generado para npm ci

### 2026-02-10
- **Phase 0.2 completado:**
  - Proyecto Neon creado (PostgreSQL 17, región us-west-2)
  - Migraciones V1-V7 ejecutadas exitosamente (29 tablas)
  - Backend desplegado en Render (Docker, auto-deploy desde main)
  - Frontend desplegado en Vercel (Vite, auto-deploy desde main)
  - Variables de entorno configuradas
  - CORS configurado con URL de producción
  - CI/CD funcionando con GitHub Actions
- **Documentación ASVS completa:**
  - Creados 14 capítulos de requisitos (V01-V14)
  - 272 requisitos L2 documentados
  - 20 requisitos ya cumplidos (7%)
  - 41 requisitos marcados N/A para MVP
- Fix: Índice `idx_orders_daily` corregido (DATE() no es IMMUTABLE)

### 2026-02-09
- **CAMBIO MAYOR:** Auth0 reemplazado por autenticación nativa (Spring Security + JWT)
- **CAMBIO MAYOR:** Phase 0 reestructurada en 4 sub-fases con enfoque "Auth First"
- **CAMBIO MAYOR:** Nivel de seguridad elevado de ASVS L1 a ASVS L2
- Agregadas 3 tablas de auth: password_reset_tokens, refresh_tokens, login_attempts
- Eliminada tabla auth_identities (no necesaria sin IdP externo)
- Total de tablas: 27 → 29
- Phase 0.3 ahora incluye endpoints detallados y requisitos ASVS específicos
- Agregados tests de seguridad como entregables obligatorios
- Success criteria actualizado con métricas de seguridad
- **Phase 0.2 implementada (~70%):**
  - GitHub Actions CI/CD con Semgrep + OWASP Dependency-Check
  - 7 módulos Maven creados con POMs
  - Spring Boot configurado (application.yml, profiles, Flyway)
  - GlobalExceptionHandler + ApiResponse/ApiError DTOs
  - SecurityConfig con Argon2id password encoder
  - Dockerfile multi-stage con usuario non-root
  - Pendiente: Neon, Render, Vercel

### 2026-02-05
- **CAMBIO MAYOR:** Inventario ahora parte del MVP (Phase 2)
- **CAMBIO MAYOR:** Phase 3 es "Digital Tickets & KDS" (NO impresión física)
- **CAMBIO MAYOR:** Mesas y áreas movidas a Phase 1 (confirmado con cliente)
- Tipos de servicio confirmados: DINE_IN, COUNTER, DELIVERY, TAKEOUT
- Variantes de producto, modificadores y combos confirmados en Phase 1
- Multi-sucursal: diseñado, pero solo 1 branch activa en MVP
- Actualización de progreso Phase 0: 7 migraciones SQL creadas (V1-V7), 27 tablas diseñadas
- Timeline actualizado: 6 meses para piloto validado
- Actualización de "Features fuera del MVP"
- Actualización de checkpoints de validación

### 2026-01-26
- Creación inicial del roadmap
- Definición de 6 fases del MVP
- Identificación de decisiones técnicas pendientes
