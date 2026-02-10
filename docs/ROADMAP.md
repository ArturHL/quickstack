# QuickStack POS - Roadmap del MVP

> **Última actualización:** 2026-02-09
> **Estado:** Phase 0.2 ~70% completado, pendiente Neon/Render/Vercel

## Vision Summary

Sistema de punto de venta multi-sucursal con inventario automático y bot WhatsApp/IA que permita a restaurantes mexicanos gestionar pedidos, ventas, inventario y reportes básicos, validado con un restaurante piloto antes del lanzamiento comercial.

**Timeline:** 6 meses hasta piloto validado.

---

## Stack Tecnológico

| Componente | Tecnología | Hosting |
|------------|------------|---------|
| Frontend | React 18 + Vite + TypeScript + MUI | Vercel |
| Backend | Java 21 + Spring Boot 3.5 | Render (Docker) |
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
| 0 | Foundation | Auth nativo (ASVS L2) + BD + Deploy + CI/CD | 🔄 40% (0.1 ✅, 0.2 ~70%) |
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
| 0.2 | Infraestructura (CI/CD, BD, Deploy) | 🔄 ~70% (falta Neon, Render, Vercel) |
| 0.3 | Módulo de Autenticación (ASVS L2) | ⏳ Pendiente |
| 0.4 | Frontend Base + Integración Auth | ⏳ Pendiente |

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
- [ ] Crear proyecto en Neon
- [ ] Configurar connection pooling
- [ ] Ejecutar migraciones V1-V7
- [ ] Crear roles de BD (quickstack_app, quickstack_readonly)
- [ ] Seed data inicial (roles, plans, status types)

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
- [ ] Configurar Render (backend)
- [ ] Configurar Vercel (frontend)
- [ ] Variables de entorno en Render

**Success Criteria 0.2:**
- [x] `mvn compile` pasa localmente
- [ ] `mvn verify` pasa en CI (requiere push a GitHub)
- [ ] Migraciones ejecutadas en Neon
- [ ] Health check responde en Render
- [ ] Deploy automático funciona

---

### Phase 0.3: Módulo de Autenticación (ASVS L2)

**Est. Effort:** 5-7 días

**Objetivo:** Implementación completa de auth nativo cumpliendo OWASP ASVS L2.

#### Endpoints de Auth API
| Endpoint | Método | Descripción | ASVS |
|----------|--------|-------------|------|
| `/api/v1/auth/register` | POST | Crear cuenta | V2.1 |
| `/api/v1/auth/login` | POST | Obtener tokens | V2.2 |
| `/api/v1/auth/refresh` | POST | Rotar refresh token | V3.5 |
| `/api/v1/auth/logout` | POST | Revocar tokens | V3.3 |
| `/api/v1/auth/forgot-password` | POST | Solicitar reset | V2.5 |
| `/api/v1/auth/reset-password` | POST | Cambiar password | V2.5 |
| `/api/v1/auth/me` | GET | Info usuario actual | - |

#### Seguridad de Passwords (ASVS V2.1, V2.4)
- [ ] Argon2id para hashing (Spring Security 6)
- [ ] Mínimo 12 caracteres
- [ ] Sin reglas de composición (no "requiere mayúscula")
- [ ] Check contra HaveIBeenPwned API
- [ ] Validación de fuerza (zxcvbn o nbvcxz)

#### Protección de Cuentas (ASVS V2.2)
- [ ] Rate limiting por IP/email (Bucket4j)
- [ ] Account lockout después de 5 intentos
- [ ] Lockout duration: 15 minutos
- [ ] Registro en login_attempts

#### Tokens JWT (ASVS V3)
- [ ] Access token: RS256, 15-30 min expiry
- [ ] Refresh token: almacenado en BD, 7 días expiry
- [ ] Refresh token rotation en cada uso
- [ ] Family tracking para detectar reuso
- [ ] Revocación de familia completa si reuso detectado

#### Password Recovery (ASVS V2.5)
- [ ] Token único, hasheado en BD (SHA-256)
- [ ] Expira en 1 hora
- [ ] Single use (marked as used_at)
- [ ] Invalida tokens anteriores del mismo usuario

#### Tests de Seguridad
- [ ] Test: Login con credenciales inválidas
- [ ] Test: Account lockout después de N intentos
- [ ] Test: Refresh token rotation
- [ ] Test: Refresh token reuse detection
- [ ] Test: Password reset flow completo
- [ ] Test: Token expirado rechazado
- [ ] Test: Rate limiting funciona

**Success Criteria 0.3:**
- Todos los endpoints funcionan
- Tests de seguridad pasan al 100%
- Rate limiting bloquea después de umbral
- Lockout funciona correctamente
- Refresh token rotation implementado

---

### Phase 0.4: Frontend Base + Integración Auth

**Est. Effort:** 3-4 días

#### Estructura Frontend
- [ ] Configurar Vite + React + TypeScript
- [ ] Configurar MUI theme
- [ ] Configurar React Router
- [ ] Configurar Axios con interceptors
- [ ] Configurar Zustand stores
- [ ] Configurar TanStack Query

#### Auth en Frontend
- [ ] AuthContext/Store para estado de auth
- [ ] Login page
- [ ] Register page (solo OWNER en MVP)
- [ ] Forgot password page
- [ ] Reset password page
- [ ] Protected routes (redirect si no autenticado)
- [ ] Auto-refresh de token (silencioso)
- [ ] Logout (limpia tokens, redirige)

#### Dashboard Base
- [ ] Layout con sidebar
- [ ] Header con info de usuario
- [ ] Dashboard vacío (placeholder)
- [ ] Manejo de errores global

**Success Criteria 0.4:**
- Usuario puede registrarse
- Usuario puede hacer login
- Usuario ve dashboard después de login
- Token se refresca automáticamente
- Logout funciona correctamente

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

**Est. Effort:** 5-6 semanas

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
