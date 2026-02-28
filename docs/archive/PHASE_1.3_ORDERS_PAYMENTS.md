# Phase 1.3: Sistema de Pedidos y Pagos — Order Management Roadmap

> **Version:** 1.3.0
> **Fecha:** 2026-02-28
> **Status:** ✅ COMPLETADA — 6/6 sprints
> **Modulo Maven:** `quickstack-branch` (Branch/Area/Table) + `quickstack-pos` (Customer/Orders/Payments)
> **Parte de:** Phase 1: Core POS - Ventas Completas

---

## Resumen Ejecutivo

Este documento define el plan de implementacion de la **tercera sub-fase de Phase 1**: el sistema completo de pedidos, pagos y gestion operativa para QuickStack POS.

**Importante:** Esta sub-fase utiliza dos modulos Maven: `quickstack-branch` (infraestructura fisica: sucursales, areas, mesas) y `quickstack-pos` (transacciones comerciales: clientes, ordenes, pagos). Ambos integran los modulos anteriores (product, tenant, auth) para completar el flujo de ventas end-to-end.

| Aspecto | Detalle |
|---------|---------|
| **Timeline** | 6 sprints (~3 semanas) |
| **Tareas Backend** | 30 tareas (~85 horas) |
| **Tareas QA** | 10 tareas (~30 horas) |
| **Tareas Frontend** | 0 (fuera de scope esta phase) |
| **Checkpoints de Seguridad** | 2 (Post-Sprint 3, Post-Sprint 5) |
| **Endpoints nuevos** | 28 endpoints REST |
| **Migracion Flyway** | Reutilizar V4-V6 ya existentes (orders, payments, customers) |

---

## Decisiones de Diseno Confirmadas

### Modulo Maven

| Decision | Valor | Justificacion |
|----------|-------|---------------|
| Modulo infraestructura | `quickstack-branch` | Branch/Area/Table — ciclo de vida de configuracion fisica, actor: admin/gerente, soft delete |
| Modulo transaccional | `quickstack-pos` | Customer/Order/Payment — ciclo de vida de ventas, actor: cajero/mesero, orders never delete |
| Dependencias pos | `quickstack-common`, `quickstack-tenant`, `quickstack-branch`, `quickstack-product` | Acceso a productos y ubicaciones para validacion y calculo de precios |
| Controladores | En cada modulo de feature | Patron establecido: controllers viven en el modulo de dominio |
| **ADR-002** | No crear `quickstack-order` | Branch y Order tienen ciclos de cambio, actores y politicas de datos distintos — separar en dos modulos es mas limpio que uno monolitico |

### Base de Datos

El esquema de base de datos para este modulo **ya fue definido en V4__create_pos_module.sql, V5__create_customer_branch_module.sql**. No se requiere nueva migracion Flyway. Las tablas relevantes para esta phase son:

| Tabla | Descripcion |
|-------|-------------|
| `branches` | Sucursales del tenant (multi-branch support) |
| `areas` | Zonas del restaurante (Terraza, Interior), soft delete |
| `tables` | Mesas dentro de areas, soft delete |
| `customers` | Clientes para delivery y tickets digitales, soft delete |
| `orders` | Pedidos de venta, **NEVER DELETE** (audit) |
| `order_items` | Items de pedidos con precios historicos, **NEVER DELETE** |
| `order_item_modifiers` | Modifiers seleccionados por item, **NEVER DELETE** |
| `payments` | Pagos registrados, **NEVER DELETE** (audit) |
| `order_status_history` | Log de cambios de estado, **NEVER DELETE** |

### Tipos de Servicio

| Tipo | Codigo | Requiere | Descripcion |
|------|--------|----------|-------------|
| Mesa | `DINE_IN` | `table_id` | Cliente come en restaurante, mesero toma orden |
| Mostrador | `COUNTER` | - | Cliente ordena y espera en mostrador (sin mesa) |
| Delivery | `DELIVERY` | `customer_id` | Entrega a domicilio del restaurante (no Uber/Rappi) |
| Para llevar | `TAKEOUT` | `customer_id` (opcional) | Cliente recoge en local |

### Order Number Format

**Formato:** `ORD-YYYYMMDD-XXX`
- Ejemplo: `ORD-20260219-042`
- Unico por tenant (constraint)

**Daily Sequence:** `daily_sequence` INTEGER
- Se reinicia cada dia por branch
- Para mostrar en cocina/KDS como "Orden #42"
- Constraint: UNIQUE (tenant_id, branch_id, DATE(opened_at), daily_sequence)

### Calculo de Totales

```
subtotal = sum(order_items.line_total)
         = sum(quantity * (unit_price + modifiers_total))

tax = subtotal * tax_rate

discount = aplicado manualmente (opcional, fase futura)

total = subtotal + tax - discount
```

**Tax Rate:** Copiado de `tenants.tax_rate` al crear orden (desnormalizacion intencional para preservar historico).

### Precios Desnormalizados (ADR-001)

Los order items copian nombre y precio del producto/variante al momento de la venta. **Esto es intencional** para preservar el precio historico aunque el producto cambie despues.

| Campo en order_items | Fuente |
|----------------------|--------|
| `product_name` | `products.name` (snapshot) |
| `variant_name` | `product_variants.name` (snapshot) |
| `unit_price` | `products.base_price + variants.price_adjustment` (snapshot) |
| `modifiers_total` | Suma de `modifiers.price_adjustment` seleccionados (snapshot) |

### Estados de Orden

| Estado | Codigo | Descripcion | Transiciones permitidas |
|--------|--------|-------------|------------------------|
| Abierta | `OPEN` | En proceso de armado, items pueden agregarse/modificarse | → SUBMITTED, CANCELLED |
| Enviada | `SUBMITTED` | Enviada a cocina (KDS), no modificable | → IN_PROGRESS, CANCELLED |
| En progreso | `IN_PROGRESS` | Cocina preparando | → READY |
| Lista | `READY` | Lista para entregar/servir | → COMPLETED |
| Completada | `COMPLETED` | Entregada y pagada, orden cerrada | - (final) |
| Cancelada | `CANCELLED` | Cancelada antes de completar | - (final) |

**Nota:** Los estados se almacenan como UUIDs de la tabla global `order_status_types` sembrada en V7__seed_data.sql.

### Pagos en MVP

Solo se soporta **efectivo (CASH)** en Phase 1. Pagos con tarjeta, QR, etc. quedan fuera del MVP.

| Campo | Validacion |
|-------|-----------|
| `payment_method` | Solo acepta `'CASH'` en Phase 1 |
| `amount` | Debe ser >= `order.total` |
| `change_given` | Calculado automaticamente: `amount - order.total` |

### Multi-tenancy

Todas las operaciones filtran por `tenant_id` extraido del JWT. Ningun endpoint acepta `tenant_id` como parametro de path o body.

### Roles y Permisos

| Operacion | OWNER | MANAGER | CASHIER |
|-----------|-------|---------|---------|
| CRUD de branches | SI | NO | NO |
| CRUD de areas/mesas | SI | SI | NO |
| CRUD de customers | SI | SI | SI (solo lectura) |
| Crear orden | SI | SI | SI |
| Modificar orden OPEN | SI | SI | SI (solo propias) |
| Cancelar orden | SI | SI | NO |
| Registrar pago | SI | SI | SI |
| Ver todas las ordenes del dia | SI | SI | NO (solo propias) |

### Manejo de Errores

| Situacion | HTTP | Codigo |
|-----------|------|--------|
| Orden no encontrada | 404 | `ORDER_NOT_FOUND` |
| Mesa ocupada | 409 | `TABLE_OCCUPIED` |
| Producto no disponible | 409 | `PRODUCT_NOT_AVAILABLE` |
| Modificacion de orden SUBMITTED | 409 | `ORDER_NOT_MODIFIABLE` |
| Pago insuficiente | 400 | `INSUFFICIENT_PAYMENT` |
| Metodo de pago no soportado | 400 | `UNSUPPORTED_PAYMENT_METHOD` |
| Acceso a orden de otro tenant | 404 (no 403) | `ORDER_NOT_FOUND` |

---

## Deuda Tecnica Aceptada

| Item | Justificacion | Phase sugerida |
|------|---------------|----------------|
| Pagos con tarjeta/QR | Requiere integracion con pasarelas (Stripe/Clip) | **Phase 2** |
| Split payments (pago parcial) | Logica compleja de estado, no critico para MVP | **Phase 2** |
| Propinas dentro del sistema | Requiere UI adicional y calculo de totales | **Phase 2** |
| Transferencias de stock entre branches | Diseñado en schema, no implementado | **Phase 2** |
| KDS en tiempo real (WebSockets) | Backend listo, frontend en Phase 1.4 | **Phase 1.4** |
| Notificaciones digitales (WhatsApp/Email) | Tickets digitales | **Phase 3** |
| Imagenes de branches (upload) | Requiere storage externo | **Phase 2** |

---

## Arquitectura de Componentes

> **ADR-002 (2026-02-25):** Se descarto `quickstack-order` como modulo unico. En su lugar se usan dos modulos con ciclos de vida y actores distintos: `quickstack-branch` (infraestructura fisica, soft delete) y `quickstack-pos` (transacciones, never delete). Los controllers viven en cada modulo de feature, no en `quickstack-app`.

```
quickstack-branch/                             <- infraestructura fisica (Sprint 1) ✅
├── src/main/java/com/quickstack/branch/
│   ├── dto/request/   BranchCreateRequest, BranchUpdateRequest,
│   │                  AreaCreateRequest, AreaUpdateRequest,
│   │                  TableCreateRequest, TableUpdateRequest, TableStatusUpdateRequest
│   ├── dto/response/  BranchResponse, AreaResponse, TableResponse
│   ├── entity/        Branch, Area, RestaurantTable, TableStatus
│   ├── repository/    BranchRepository, AreaRepository, TableRepository
│   ├── service/       BranchService, AreaService, TableService
│   ├── controller/    BranchController, AreaController, TableController
│   └── security/      BranchPermissionEvaluator

quickstack-pos/                                <- transacciones comerciales (Sprints 2-6) Sprint 5 ✅
├── src/main/java/com/quickstack/pos/
│   ├── dto/request/   CustomerCreateRequest, CustomerUpdateRequest,
│   │                  OrderCreateRequest, OrderItemRequest, OrderItemModifierRequest,
│   │                  PaymentRequest, OrderStatusUpdateRequest
│   ├── dto/response/  CustomerResponse, OrderResponse, OrderItemResponse,
│   │                  PaymentResponse, DailySummaryResponse
│   ├── entity/        Customer (Sprint 2 ✅), Order, OrderItem,
│   │                  OrderItemModifier (Sprint 3 ✅), Payment (Sprint 5 ✅), OrderStatusHistory
│   ├── repository/    CustomerRepository (Sprint 2 ✅), OrderRepository (Sprint 3 ✅), PaymentRepository (Sprint 5 ✅)
│   ├── service/       CustomerService (Sprint 2 ✅), OrderService (Sprint 3+4 ✅),
│   │                  PaymentService (Sprint 5 ✅), OrderCalculationService (Sprint 3 ✅)
│   ├── controller/    CustomerController (Sprint 2 ✅), OrderController (Sprint 4 ✅), PaymentController (Sprint 5 ✅)
│   └── security/      PosPermissionEvaluator (Sprint 2 ✅)
```

---

## Sprint 1: Infraestructura — Branches, Areas, Tables ✅ COMPLETADO

**Duracion:** 2 dias | **Objetivo:** CRUD completo de entidades operativas | **Tests:** 60 unit + 14 integration = 74 tests

### [BACKEND] Tarea 1.1: Activar modulo quickstack-branch ✅

**Prioridad:** Alta | **Dependencias:** Ninguna

Modulo `quickstack-branch` ya existia como stub. Se activaron dependencias JPA, Validation y Testcontainers.

**Criterios de Aceptacion:**
- [x] `quickstack-branch/pom.xml` actualizado con JPA, Validation, Testcontainers
- [x] `quickstack-pos/pom.xml` depende de `quickstack-branch`
- [x] Estructura de paquetes: dto, entity, repository, service, controller, security
- [x] `mvn compile` pasa sin errores

**Archivos:**
- `quickstack-branch/pom.xml` (actualizado)

---

### [BACKEND] Tarea 1.2: Entidades Branch, Area, Table

**Prioridad:** Alta | **Dependencias:** 1.1

Entidades JPA mapeadas a tablas ya existentes en V5 y V4.

**Criterios de Aceptacion:**
- [ ] `Branch.java` con campos: `id`, `tenantId`, `name`, `address`, `city`, `phone`, `email`, `isActive`, `createdAt`, `updatedAt`, `deletedAt`, `deletedBy`
- [ ] `Area.java` con campos: `id`, `tenantId`, `branchId`, `name`, `description`, `sortOrder`, `isActive`, `createdAt`, `updatedAt`, `deletedAt`
- [ ] `Table.java` con campos: `id`, `tenantId`, `areaId`, `number`, `name`, `capacity`, `status` (enum: AVAILABLE, OCCUPIED, RESERVED, MAINTENANCE), `sortOrder`, `positionX`, `positionY`, `isActive`, `createdAt`, `updatedAt`, `deletedAt`
- [ ] Enum `TableStatus`: AVAILABLE, OCCUPIED, RESERVED, MAINTENANCE
- [ ] `@OneToMany` de Area → Table (fetch LAZY)
- [ ] Tests unitarios: 8 tests (constructor, enum parsing, relationships)

**Archivos:**
- `quickstack-branch/src/main/java/com/quickstack/branch/entity/Branch.java`
- `quickstack-branch/src/main/java/com/quickstack/branch/entity/Area.java`
- `quickstack-branch/src/main/java/com/quickstack/branch/entity/Table.java`
- `quickstack-branch/src/main/java/com/quickstack/branch/entity/TableStatus.java`
- `quickstack-branch/src/test/java/com/quickstack/branch/entity/BranchEntityTest.java`

---

### [BACKEND] Tarea 1.3: Repositorios Branch, Area, Table

**Prioridad:** Alta | **Dependencias:** 1.2

Repositorios JPA con queries tenant-safe.

**Criterios de Aceptacion:**
- [ ] `BranchRepository`: `findAllByTenantId()`, `findByIdAndTenantId()`, `existsByNameAndTenantId()`
- [ ] `AreaRepository`: `findAllByBranchIdAndTenantId()`, `findByIdAndTenantId()`, `existsByNameAndBranchIdAndTenantId()`
- [ ] `TableRepository`: `findAllByAreaIdAndTenantId()`, `findByIdAndTenantId()`, `existsByNumberAndAreaIdAndTenantId()`, `findAvailableTablesByBranch(UUID branchId, UUID tenantId)` (status=AVAILABLE, not deleted)
- [ ] Todos los metodos excluyen `deleted_at IS NOT NULL`
- [ ] Tests de repositorio con `@DataJpaTest` + Testcontainers: 18 tests

**Archivos:**
- `quickstack-branch/src/main/java/com/quickstack/branch/repository/BranchRepository.java`
- `quickstack-branch/src/main/java/com/quickstack/branch/repository/AreaRepository.java`
- `quickstack-branch/src/main/java/com/quickstack/branch/repository/TableRepository.java`
- `quickstack-branch/src/test/java/com/quickstack/branch/repository/BranchRepositoryTest.java`

---

### [BACKEND] Tarea 1.4: DTOs y Services — Branch, Area, Table CRUD

**Prioridad:** Alta | **Dependencias:** 1.3

DTOs, services y logica de negocio basica.

**Criterios de Aceptacion:**
- [ ] DTOs de request: `BranchCreateRequest`, `BranchUpdateRequest`, `AreaCreateRequest`, `AreaUpdateRequest`, `TableCreateRequest`, `TableUpdateRequest`
- [ ] DTOs de response: `BranchResponse`, `AreaResponse`, `TableResponse`
- [ ] `BranchService`: CRUD completo (create, update, delete=soft, get, list)
- [ ] `AreaService`: CRUD completo, validar que `branchId` pertenece al tenant
- [ ] `TableService`: CRUD completo, metodo `setTableStatus(UUID tableId, TableStatus status)` para cambiar estado
- [ ] Validaciones: nombre unico por tenant (branch), nombre unico por branch (area), numero unico por area (table)
- [ ] Tests unitarios con mocks: 36 tests (12 por servicio)

**Archivos:**
- `quickstack-pos/src/main/java/com/quickstack/pos/dto/...` (6 request + 3 response DTOs)
- `quickstack-branch/src/main/java/com/quickstack/branch/service/BranchService.java`
- `quickstack-branch/src/main/java/com/quickstack/branch/service/AreaService.java`
- `quickstack-branch/src/main/java/com/quickstack/branch/service/TableService.java`
- `quickstack-branch/src/test/java/com/quickstack/branch/service/BranchServiceTest.java`

---

### [BACKEND] Tarea 1.5: Controllers — Branch, Area, Table

**Prioridad:** Alta | **Dependencias:** 1.4

Controllers REST para entidades operativas.

**Criterios de Aceptacion:**
- [ ] `BranchController`: GET /api/v1/branches, POST (OWNER only), GET /{id}, PUT /{id} (OWNER), DELETE /{id} (OWNER)
- [ ] `AreaController`: GET /api/v1/branches/{branchId}/areas, POST (MANAGER+), GET /api/v1/areas/{id}, PUT /{id} (MANAGER+), DELETE /{id} (MANAGER+)
- [ ] `TableController`: GET /api/v1/areas/{areaId}/tables, POST (MANAGER+), GET /api/v1/tables/{id}, PUT /{id} (MANAGER+), DELETE /{id} (MANAGER+), PATCH /api/v1/tables/{id}/status (MANAGER+, body: `{"status": "OCCUPIED"}`)
- [ ] Todos extraen `tenantId` del JWT
- [ ] Tests unitarios con `@WebMvcTest`: 24 tests

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/controller/BranchController.java`
- `quickstack-app/src/main/java/com/quickstack/app/controller/AreaController.java`
- `quickstack-app/src/main/java/com/quickstack/app/controller/TableController.java`
- `quickstack-app/src/test/java/com/quickstack/app/controller/BranchControllerTest.java`

---

### [QA] Tarea 1.6: Tests de Integracion — Branches, Areas, Tables

**Prioridad:** Alta | **Dependencias:** 1.5

Tests end-to-end con base de datos real.

**Criterios de Aceptacion:**
- [ ] Extienden `BaseIntegrationTest`
- [ ] Setup crea tenant y JWTs (OWNER, MANAGER, CASHIER)
- [ ] `POST /api/v1/branches` con OWNER: retorna 201
- [ ] `POST /api/v1/branches` con MANAGER: retorna 403
- [ ] `POST /api/v1/areas` con branch de otro tenant: retorna 404
- [ ] `PATCH /api/v1/tables/{id}/status` cambia estado en BD
- [ ] Cross-tenant: branch/area/table de tenant A con JWT de tenant B retorna 404
- [ ] 14 tests de integracion pasando

**Archivos:**
- `quickstack-app/src/test/java/com/quickstack/app/order/BranchIntegrationTest.java`

---

## Sprint 2: Customers

**Duracion:** 1.5 dias | **Objetivo:** CRUD completo de clientes para delivery

### [BACKEND] Tarea 2.1: Entidad Customer

**Prioridad:** Alta | **Dependencias:** 1.1

Entidad JPA para clientes.

**Criterios de Aceptacion:**
- [ ] `Customer.java` con campos: `id`, `tenantId`, `name`, `phone`, `email`, `whatsapp`, `addressLine1`, `addressLine2`, `city`, `postalCode`, `deliveryNotes`, `preferences` (JSONB), `totalOrders`, `totalSpent`, `lastOrderAt`, `createdAt`, `updatedAt`, `deletedAt`
- [ ] Constraint: al menos uno de `phone`, `email`, `whatsapp` debe estar presente (validado en service)
- [ ] Tests unitarios: 5 tests

**Archivos:**
- `quickstack-pos/src/main/java/com/quickstack/pos/entity/Customer.java`
- `quickstack-pos/src/test/java/com/quickstack/pos/entity/CustomerTest.java`

---

### [BACKEND] Tarea 2.2: CustomerRepository

**Prioridad:** Alta | **Dependencias:** 2.1

Repositorio JPA para clientes.

**Criterios de Aceptacion:**
- [ ] `findAllByTenantId(UUID tenantId, Pageable pageable)` retorna solo no-borrados
- [ ] `findByIdAndTenantId(UUID id, UUID tenantId)` excluye borrados
- [ ] `findByPhoneAndTenantId(String phone, UUID tenantId)` para lookup rapido
- [ ] `existsByPhoneAndTenantId()`, `existsByEmailAndTenantId()` para validacion de unicidad
- [ ] `searchCustomers(UUID tenantId, String searchTerm, Pageable pageable)` — busca por nombre, phone o email con `ILIKE`
- [ ] Tests de repositorio con `@DataJpaTest` + Testcontainers: 12 tests

**Archivos:**
- `quickstack-pos/src/main/java/com/quickstack/pos/repository/CustomerRepository.java`
- `quickstack-pos/src/test/java/com/quickstack/pos/repository/CustomerRepositoryTest.java`

---

### [BACKEND] Tarea 2.3: DTOs y CustomerService

**Prioridad:** Alta | **Dependencias:** 2.2

DTOs y logica de negocio para clientes.

**Criterios de Aceptacion:**
- [ ] `CustomerCreateRequest`: `name` (nullable, max 255), `phone` (nullable, max 20), `email` (nullable, email format), `whatsapp` (nullable), `addressLine1`, `addressLine2`, `city`, `postalCode`, `deliveryNotes`
- [ ] Validacion cross-field: al menos uno de `phone`, `email`, `whatsapp` debe estar presente
- [ ] `CustomerUpdateRequest`: mismos campos, todos opcionales
- [ ] `CustomerResponse`: todos los campos de Customer
- [ ] `CustomerService`: CRUD completo + metodo `incrementOrderStats(UUID customerId, BigDecimal orderTotal)` para actualizar `totalOrders`, `totalSpent`, `lastOrderAt`
- [ ] Tests unitarios con mocks: 18 tests

**Archivos:**
- `quickstack-pos/src/main/java/com/quickstack/pos/dto/request/CustomerCreateRequest.java`
- `quickstack-pos/src/main/java/com/quickstack/pos/dto/request/CustomerUpdateRequest.java`
- `quickstack-pos/src/main/java/com/quickstack/pos/dto/response/CustomerResponse.java`
- `quickstack-pos/src/main/java/com/quickstack/pos/service/CustomerService.java`
- `quickstack-pos/src/test/java/com/quickstack/pos/service/CustomerServiceTest.java`

---

### [BACKEND] Tarea 2.4: CustomerController

**Prioridad:** Alta | **Dependencias:** 2.3

Controller REST para clientes.

**Criterios de Aceptacion:**
- [ ] `GET /api/v1/customers` — todos los roles. Params: `search` (string). Retorna `Page<CustomerResponse>` HTTP 200
- [ ] `POST /api/v1/customers` — CASHIER+. Retorna `CustomerResponse` HTTP 201
- [ ] `GET /api/v1/customers/{id}` — todos los roles. Retorna `CustomerResponse` HTTP 200
- [ ] `PUT /api/v1/customers/{id}` — CASHIER+. Retorna `CustomerResponse` HTTP 200
- [ ] `DELETE /api/v1/customers/{id}` — MANAGER+. HTTP 204 (soft delete)
- [ ] Tests unitarios con `@WebMvcTest`: 12 tests

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/controller/CustomerController.java`
- `quickstack-app/src/test/java/com/quickstack/app/controller/CustomerControllerTest.java`

---

### [QA] Tarea 2.5: Tests de Integracion — Customers

**Prioridad:** Alta | **Dependencias:** 2.4

Tests end-to-end.

**Criterios de Aceptacion:**
- [ ] `POST /api/v1/customers` sin phone/email/whatsapp: retorna 400
- [ ] `POST` con phone duplicado en mismo tenant: retorna 409
- [ ] `GET /api/v1/customers?search=555` retorna clientes con phone que contiene "555"
- [ ] Cross-tenant: customer de tenant A con JWT de tenant B retorna 404
- [ ] 8 tests de integracion pasando

**Archivos:**
- `quickstack-app/src/test/java/com/quickstack/app/order/CustomerIntegrationTest.java`

---

## Sprint 3: Order Core — Entidades y Calculo de Totales ✅ COMPLETADO

**Duracion:** 2.5 dias | **Objetivo:** Entidades de Order + logica de calculo de precios | **Tests:** 38 (12 entity + 12 service + 14 repository)

### [BACKEND] Tarea 3.1: Entidades Order, OrderItem, OrderItemModifier ✅

**Prioridad:** Alta | **Dependencias:** 1.2, 2.1

Entidades JPA para pedidos.

**Criterios de Aceptacion:**
- [x] `Order.java` con campos: `id`, `tenantId`, `branchId`, `tableId`, `customerId`, `orderNumber`, `dailySequence`, `serviceType` (enum), `statusId`, `subtotal`, `taxRate`, `tax`, `discount`, `total`, `source` (enum: POS, WHATSAPP, WEB, PHONE), `notes`, `kitchenNotes`, `openedAt`, `closedAt`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`
- [x] Enum `ServiceType`: DINE_IN, COUNTER, DELIVERY, TAKEOUT
- [x] Enum `OrderSource`: POS, WHATSAPP, WEB, PHONE
- [x] `@OneToMany` a `OrderItem` (fetch LAZY, cascade ALL)
- [ ] `@OneToMany` a `Payment` (fetch LAZY) — diferido a Sprint 5
- [x] `OrderItem.java` con campos: `id`, `tenantId`, `orderId`, `productId`, `variantId`, `comboId`, `productName`, `variantName`, `quantity`, `unitPrice`, `modifiersTotal`, `lineTotal` (GENERATED ALWAYS AS computed column), `kdsStatus` (enum), `kdsSentAt`, `kdsReadyAt`, `notes`, `sortOrder`, `createdAt`, `updatedAt`
- [x] Enum `KdsStatus`: PENDING, PREPARING, READY, DELIVERED
- [x] `@OneToMany` a `OrderItemModifier` (fetch LAZY, cascade ALL)
- [x] `OrderItemModifier.java` con campos: `id`, `tenantId`, `orderItemId`, `modifierId`, `modifierName`, `priceAdjustment`, `quantity`
- [x] Tests unitarios: 12 tests (constructors, enums, relationships, computed lineTotal)

**ADR Sprint 3:** `OrderItem.modifiers` mapeado como `Set<OrderItemModifier>` (no List) para evitar `MultipleBagFetchException` al hacer `@EntityGraph` simultáneo con `Order.items`.

**Archivos:**
- `quickstack-pos/src/main/java/com/quickstack/pos/entity/Order.java`
- `quickstack-pos/src/main/java/com/quickstack/pos/entity/OrderItem.java`
- `quickstack-pos/src/main/java/com/quickstack/pos/entity/OrderItemModifier.java`
- `quickstack-pos/src/main/java/com/quickstack/pos/entity/ServiceType.java`
- `quickstack-pos/src/main/java/com/quickstack/pos/entity/OrderSource.java`
- `quickstack-pos/src/main/java/com/quickstack/pos/entity/KdsStatus.java`
- `quickstack-pos/src/main/java/com/quickstack/pos/entity/OrderStatusConstants.java`
- `quickstack-pos/src/test/java/com/quickstack/pos/entity/OrderEntityTest.java`

---

### [BACKEND] Tarea 3.2: OrderRepository ✅

**Prioridad:** Alta | **Dependencias:** 3.1

Repositorio JPA para orders.

**Criterios de Aceptacion:**
- [x] `findByIdAndTenantId(UUID id, UUID tenantId)` con `@EntityGraph` que incluye `items` y `items.modifiers` (evitar N+1)
- [x] `findAllByBranchIdAndTenantId(UUID branchId, UUID tenantId, Pageable pageable)` para listar ordenes
- [x] `findOrdersByDateRange(UUID tenantId, UUID branchId, LocalDate startDate, LocalDate endDate, Pageable pageable)` para reportes (native query con DATE(opened_at))
- [x] `findOpenOrdersByTable(UUID tableId, UUID tenantId, List<UUID> terminalStatusIds)` retorna ordenes con status != COMPLETED y != CANCELLED para una mesa
- [x] `getNextDailySequence(UUID tenantId, UUID branchId, LocalDate date)` — query nativa para obtener MAX(daily_sequence) + 1 del dia
- [x] `existsByOrderNumberAndTenantId(String orderNumber, UUID tenantId)` para unicidad
- [x] Tests de repositorio con `@DataJpaTest` + Testcontainers: 14 tests

**Archivos:**
- `quickstack-pos/src/main/java/com/quickstack/pos/repository/OrderRepository.java`
- `quickstack-pos/src/test/java/com/quickstack/pos/repository/OrderRepositoryTest.java`

---

### [BACKEND] Tarea 3.3: OrderCalculationService ✅

**Prioridad:** Alta | **Dependencias:** 3.1

Servicio para calculo de totales de orden.

**Criterios de Aceptacion:**
- [x] `calculateItemTotal(OrderItem item)`: retorna `quantity * (unit_price + modifiers_total)`
- [x] `calculateSubtotal(List<OrderItem> items)`: suma de todos los line totals
- [x] `calculateTax(BigDecimal subtotal, BigDecimal taxRate)`: `subtotal * taxRate`
- [x] `calculateTotal(BigDecimal subtotal, BigDecimal tax, BigDecimal discount)`: `subtotal + tax - discount`
- [x] Metodo `recalculateOrder(Order order)`: recalcula subtotal, tax, total de la orden completa y actualiza los campos
- [x] Todos los calculos usan `BigDecimal` con `RoundingMode.HALF_UP` a 2 decimales
- [x] Tests unitarios: 12 tests (precision decimal, casos edge: subtotal=0, discount > subtotal, etc.)

**Archivos:**
- `quickstack-pos/src/main/java/com/quickstack/pos/service/OrderCalculationService.java`
- `quickstack-pos/src/test/java/com/quickstack/pos/service/OrderCalculationServiceTest.java`

---

### Checkpoint de Seguridad Post-Sprint 3

**Validaciones requeridas antes de continuar:**

- [ ] Verificar que orders solo se crean con `branchId` que pertenece al tenant
- [ ] Verificar que orders con `tableId` solo aceptan mesa del mismo branch y tenant
- [ ] Verificar que orders con `customerId` solo aceptan customer del mismo tenant
- [ ] Test de IDOR: usuario de tenant B intenta GET order de tenant A — debe recibir 404
- [ ] Confirmar que `daily_sequence` no tiene race conditions (usar lock pesimista o SERIAL en generacion)
- [x] Verificar que calculos de totales son precisos con BigDecimal

---

## Sprint 4: Order Management — Crear, Modificar, Enviar a Cocina ✅ COMPLETADO

**Duracion:** 2.5 dias | **Objetivo:** API completa para crear y gestionar ordenes | **Tests:** 145 unit + 16 integration = 161 tests

### [BACKEND] Tarea 4.1: DTOs de Order ✅

**Prioridad:** Alta | **Dependencias:** 3.1, 3.3

Objetos de transferencia para orders.

**Criterios de Aceptacion:**
- [x] `OrderItemModifierRequest`: `modifierId` (NotNull UUID), `modifierName` (NotBlank), `priceAdjustment` (NotNull)
- [x] `OrderItemRequest`: `productId` (UUID, condicional), `variantId` (UUID, nullable), `comboId` (UUID, condicional), `productName` (NotBlank), `variantName` (nullable), `quantity` (NotNull, min 1), `unitPrice` (NotNull, min 0), `modifiers` (lista de OrderItemModifierRequest), `notes` (nullable)
- [x] Validacion: `productId XOR comboId` (exactamente uno debe estar presente)
- [x] `OrderCreateRequest`: `branchId` (NotNull), `serviceType` (NotNull enum), `tableId` (condicional: requerido si DINE_IN), `customerId` (condicional: requerido si DELIVERY), `items` (NotEmpty lista), `notes` (nullable), `kitchenNotes` (nullable)
- [x] `OrderResponse`: todos los campos de Order + lista de `OrderItemResponse` + lista de `PaymentResponse`
- [x] `OrderItemResponse`: todos los campos de OrderItem + lista de modifiers
- [x] Tests unitarios: 14 tests (Bean Validation, cross-field validation)

**Archivos:**
- `quickstack-pos/src/main/java/com/quickstack/pos/dto/request/OrderCreateRequest.java`
- `quickstack-pos/src/main/java/com/quickstack/pos/dto/request/OrderItemRequest.java`
- `quickstack-pos/src/main/java/com/quickstack/pos/dto/request/OrderItemModifierRequest.java`
- `quickstack-pos/src/main/java/com/quickstack/pos/dto/response/OrderResponse.java`
- `quickstack-pos/src/main/java/com/quickstack/pos/dto/response/OrderItemResponse.java`
- `quickstack-pos/src/test/java/com/quickstack/pos/dto/OrderDtoTest.java`

---

### [BACKEND] Tarea 4.2: OrderService — Crear Orden ✅

**Prioridad:** Alta | **Dependencias:** 3.2, 3.3, 4.1

Logica de negocio para crear ordenes.

**Criterios de Aceptacion:**
- [x] `createOrder(UUID tenantId, UUID userId, OrderCreateRequest request)`:
  - Valida que `branchId` pertenece al tenant
  - Si `serviceType == DINE_IN`, valida que `tableId` pertenece al branch y esta AVAILABLE, la marca como OCCUPIED
  - Si `serviceType == DELIVERY`, valida que `customerId` pertenece al tenant
  - Valida que todos los `productId`/`comboId` en items pertenecen al tenant y estan activos y disponibles (lanza `BusinessRuleException` con 409 si no)
  - Genera `orderNumber` (formato `ORD-YYYYMMDD-XXX`)
  - Genera `dailySequence` usando `getNextDailySequence()`
  - Obtiene `taxRate` de `tenants.tax_rate` y lo copia a la orden
  - Persiste orden con status=OPEN
  - Calcula totales usando `OrderCalculationService`
  - Retorna `OrderResponse`
- [x] Toda la operacion es `@Transactional`
- [x] Tests unitarios con mocks: 20 tests (happy paths para cada service type, producto no disponible, mesa ocupada, etc.)

**Archivos:**
- `quickstack-pos/src/main/java/com/quickstack/pos/service/OrderService.java`
- `quickstack-pos/src/test/java/com/quickstack/pos/service/OrderServiceTest.java`

---

### [BACKEND] Tarea 4.3: OrderService — Modificar y Enviar Orden ✅

**Prioridad:** Alta | **Dependencias:** 4.2

Metodos adicionales de OrderService.

**Criterios de Aceptacion:**
- [x] `addItemToOrder(UUID tenantId, UUID orderId, OrderItemRequest item)`:
  - Solo permite modificar ordenes con status=OPEN (lanza `BusinessRuleException` con 409 si no)
  - Valida producto disponible
  - Agrega item a la orden
  - Recalcula totales
  - Retorna `OrderResponse`
- [x] `removeItemFromOrder(UUID tenantId, UUID orderId, UUID itemId)`:
  - Solo si status=OPEN
  - Remueve item (soft delete o hard delete? — DECISION: hard delete ya que order no se ha enviado a cocina)
  - Recalcula totales
  - Retorna `OrderResponse`
- [x] `submitOrder(UUID tenantId, UUID orderId)`:
  - Cambia status de OPEN → SUBMITTED
  - Marca `kdsStatus` de todos los items como PENDING
  - Setea `kdsSentAt` timestamp
  - Registra cambio en `order_status_history`
  - Retorna `OrderResponse`
- [x] `cancelOrder(UUID tenantId, UUID userId, UUID orderId)`:
  - Solo MANAGER+ puede cancelar
  - Cambia status a CANCELLED
  - Si tiene `tableId`, libera la mesa (status=AVAILABLE)
  - Registra en `order_status_history`
  - Retorna void
- [x] Tests unitarios con mocks: 22 tests

**Archivos:**
- `quickstack-pos/src/main/java/com/quickstack/pos/service/OrderService.java` (expandir)
- `quickstack-pos/src/test/java/com/quickstack/pos/service/OrderServiceTest.java` (expandir)

---

### [BACKEND] Tarea 4.4: OrderController ✅

**Prioridad:** Alta | **Dependencias:** 4.3

Controller REST para orders.

**Criterios de Aceptacion:**
- [x] `POST /api/v1/orders` — CASHIER+. Retorna `OrderResponse` HTTP 201 + `Location` header
- [x] `GET /api/v1/orders/{id}` — CASHIER+. CASHIER solo ve propias ordenes (validar `createdBy == userId`). Retorna `OrderResponse` HTTP 200
- [x] `POST /api/v1/orders/{id}/items` — CASHIER+ (solo si orden es propia y status=OPEN). Retorna `OrderResponse` HTTP 200
- [x] `DELETE /api/v1/orders/{orderId}/items/{itemId}` — CASHIER+ (solo propias, status=OPEN). HTTP 204
- [x] `POST /api/v1/orders/{id}/submit` — CASHIER+ (solo propias, status=OPEN). Retorna `OrderResponse` HTTP 200
- [x] `POST /api/v1/orders/{id}/cancel` — MANAGER+. HTTP 204
- [x] `GET /api/v1/orders` — MANAGER+ ve todas, CASHIER solo propias. Params: `branchId`, `status`, `startDate`, `endDate`. Retorna `Page<OrderResponse>` HTTP 200
- [x] Tests unitarios con `@WebMvcTest`: 20 tests

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/controller/OrderController.java`
- `quickstack-app/src/main/java/com/quickstack/app/security/OrderPermissionEvaluator.java` (validar ownership de ordenes)
- `quickstack-app/src/test/java/com/quickstack/app/controller/OrderControllerTest.java`

---

### [QA] Tarea 4.5: Tests de Integracion — Orders ✅

**Prioridad:** Alta | **Dependencias:** 4.4

Tests end-to-end.

**Criterios de Aceptacion:**
- [x] `POST /api/v1/orders` con DINE_IN y mesa disponible: retorna 201, mesa queda OCCUPIED
- [x] `POST` con DINE_IN y mesa ocupada: retorna 409
- [x] `POST` con producto no disponible: retorna 409
- [x] `POST /api/v1/orders/{id}/submit` cambia status a SUBMITTED en BD
- [x] `POST /api/v1/orders/{id}/items` en orden SUBMITTED: retorna 409
- [x] `GET /api/v1/orders` con CASHIER retorna solo ordenes creadas por el (verifica `created_by`)
- [x] Cross-tenant: orden de tenant A con JWT de tenant B retorna 404
- [x] 16 tests de integracion pasando

**Archivos:**
- `quickstack-app/src/test/java/com/quickstack/app/order/OrderIntegrationTest.java`

---

## Sprint 5: Payments ✅ COMPLETADO

**Duracion:** 2 dias | **Objetivo:** Registrar pagos y cerrar ordenes

### [BACKEND] Tarea 5.1: Entidad Payment ✅

**Prioridad:** Alta | **Dependencias:** 3.1

Entidad JPA para pagos.

**Criterios de Aceptacion:**
- [x] `Payment.java` con campos: `id`, `tenantId`, `orderId`, `paymentMethod` (enum: CASH en MVP), `amount`, `amountReceived`, `changeGiven`, `status`, `referenceNumber`, `notes`, `createdAt`, `createdBy`
- [x] Enum `PaymentMethod`: CASH, CARD, TRANSFER, OTHER (solo CASH aceptado en service MVP)
- [x] `amount >= order.total` (validado en service)
- [x] `changeGiven` calculado automaticamente: `amountReceived - order.total`
- [x] Payment NO tiene `deletedAt` — NEVER DELETE (audit)
- [x] Tests unitarios: 5 tests

**Archivos:**
- `quickstack-pos/src/main/java/com/quickstack/pos/entity/Payment.java`
- `quickstack-pos/src/main/java/com/quickstack/pos/entity/PaymentMethod.java`
- `quickstack-pos/src/test/java/com/quickstack/pos/entity/PaymentTest.java`

---

### [BACKEND] Tarea 5.2: PaymentRepository ✅

**Prioridad:** Alta | **Dependencias:** 5.1

Repositorio JPA para pagos.

**Criterios de Aceptacion:**
- [x] `findAllByOrderIdAndTenantId(UUID orderId, UUID tenantId)` retorna todos los pagos de una orden
- [x] `sumPaymentsByOrder(UUID orderId, UUID tenantId)` retorna suma de `amount` de todos los pagos de la orden (COALESCE 0 si sin pagos)
- [x] Tests de repositorio con `@DataJpaTest` + Testcontainers: 8 tests

**Archivos:**
- `quickstack-pos/src/main/java/com/quickstack/pos/repository/PaymentRepository.java`
- `quickstack-pos/src/test/java/com/quickstack/pos/repository/PaymentRepositoryTest.java`

---

### [BACKEND] Tarea 5.3: DTOs y PaymentService ✅

**Prioridad:** Alta | **Dependencias:** 5.2

DTOs y logica de negocio para pagos.

**Criterios de Aceptacion:**
- [x] `PaymentRequest`: `orderId` (NotNull), `paymentMethod` (NotNull), `amount` (NotNull, min 0.01), `notes` (nullable)
- [x] Validacion: `amount >= order.total` — lanza `ApiException(HttpStatus.BAD_REQUEST, "INSUFFICIENT_PAYMENT")` (400, no 409)
- [x] `PaymentResponse`: todos los campos de Payment con factory `from(Payment)`
- [x] `PaymentService.registerPayment(UUID tenantId, UUID userId, PaymentRequest request)`:
  - Valida orden READY — lanza `BusinessRuleException("ORDER_NOT_READY")` (409) si no
  - Solo CASH — lanza `BusinessRuleException("UNSUPPORTED_PAYMENT_METHOD")` (409) si otro metodo
  - Calcula `changeGiven = amountReceived - order.total`
  - Persiste payment con `createdBy = userId`
  - Si `sumPaymentsByOrder >= order.total`: cierra orden (status=COMPLETED, closedAt=NOW(), inserta order_status_history via native query)
  - Si `tableId != null`: libera mesa (status=AVAILABLE via `TableRepository`)
  - Si `customerId != null`: llama `customerService.incrementOrderStats(tenantId, customerId, total)`
  - Retorna `PaymentResponse`
- [x] `PaymentService.listPaymentsForOrder()`: valida orden primero (IDOR protection) — 404 si cross-tenant
- [x] Toda operacion es `@Transactional`
- [x] Tests unitarios con mocks: 16 tests (`@MockitoSettings(strictness = Strictness.LENIENT)`)

**Archivos:**
- `quickstack-pos/src/main/java/com/quickstack/pos/dto/request/PaymentRequest.java`
- `quickstack-pos/src/main/java/com/quickstack/pos/dto/response/PaymentResponse.java`
- `quickstack-pos/src/main/java/com/quickstack/pos/service/PaymentService.java`
- `quickstack-pos/src/test/java/com/quickstack/pos/service/PaymentServiceTest.java`

**Notas de implementacion:**
- `INSUFFICIENT_PAYMENT` usa `ApiException(BAD_REQUEST)`, no `BusinessRuleException` (que daría 409)
- `insertStatusHistory` usa `entityManager.createNativeQuery()` dentro de la misma transaccion JPA

---

### [BACKEND] Tarea 5.4: PaymentController ✅

**Prioridad:** Alta | **Dependencias:** 5.3

Controller REST para pagos.

**Criterios de Aceptacion:**
- [x] `POST /api/v1/payments` — CASHIER+. Retorna `PaymentResponse` HTTP 201 con Location header
- [x] `GET /api/v1/orders/{orderId}/payments` — CASHIER+. Retorna lista de `PaymentResponse` HTTP 200
- [x] Sin `@RequestMapping` a nivel clase — rutas completas en cada metodo
- [x] Vive en `quickstack-pos` (no en `quickstack-app`)
- [x] Tests unitarios: 8 tests

**Archivos:**
- `quickstack-pos/src/main/java/com/quickstack/pos/controller/PaymentController.java`
- `quickstack-pos/src/test/java/com/quickstack/pos/controller/PaymentControllerTest.java`
- `quickstack-auth/src/main/java/com/quickstack/auth/config/SecurityConfig.java` (agregada ruta `/api/v1/payments/**`)

---

### [QA] Tarea 5.5: Tests de Integracion — Payments ✅

**Prioridad:** Alta | **Dependencias:** 5.4

Tests end-to-end.

**Criterios de Aceptacion:**
- [x] Setup: orden READY insertada via JDBC (`createReadyOrder()` — serviceType calculado en Java, no SQL CASE)
- [x] `POST /api/v1/payments` exact amount: orden → COMPLETED, `closedAt` seteado ✅
- [x] Overpayment: `changeGiven` calculado correctamente ✅
- [x] DINE_IN payment libera mesa (status=AVAILABLE) ✅
- [x] Payment con customerId actualiza `totalOrders` en BD ✅
- [x] `POST` con amount < total: retorna 400 ✅
- [x] `POST` en orden PENDING (no READY): retorna 409 ✅
- [x] Cross-tenant: orden de otro tenant retorna 404 ✅
- [x] Sin auth: retorna 403 (Spring Security sin AuthenticationEntryPoint → 403, no 401) ✅
- [x] GET payments: lista correctamente despues de registrar pago ✅
- [x] GET payments cross-tenant: retorna 404 ✅
- [x] 10 tests de integracion pasando

**Archivos:**
- `quickstack-app/src/test/java/com/quickstack/app/pos/PaymentIntegrationTest.java`

---

### Checkpoint de Seguridad Post-Sprint 5 ✅

**Validaciones requeridas antes de continuar:**

- [x] Pagos solo se pueden registrar en ordenes del mismo tenant (IDOR — cross-tenant → 404)
- [x] Pagos en orden no-READY lanza 409
- [x] Pago insuficiente lanza 400 (no 409)
- [x] Sin autenticacion → 403
- [x] Logs registran PAYMENT_REGISTERED y ORDER_COMPLETED con tenantId/userId/resourceId
- [ ] CASHIER ownership validation (deuda tecnica — post-Phase 1.3)
- [ ] Race condition en daily_sequence (deuda tecnica — aceptado)

---

## Sprint 6: Reporting y Utilidades ✅ COMPLETADO

**Duracion:** 1.5 dias | **Objetivo:** Endpoints de reportes basicos del dia

### [BACKEND] Tarea 6.1: Daily Summary Endpoint ✅

**Prioridad:** Media | **Dependencias:** 4.4, 5.4

Endpoint de resumen de ventas del dia.

**Criterios de Aceptacion:**
- [x] `DailySummaryResponse`: `date`, `branchId`, `totalOrders` (count), `totalSales` (sum of totals), `averageTicket`, `ordersByServiceType` (map: DINE_IN → count, etc.), `topProducts` (lista de top 5 productos mas vendidos)
- [x] `OrderService.getDailySummary(UUID tenantId, UUID branchId, LocalDate date)`:
  - 3 queries JdbcTemplate (queryForMap, queryForList, query con RowMapper)
  - Solo ordenes con status=COMPLETED
  - IDOR protection: branchId validado contra tenant antes de ejecutar queries
  - Retorna `DailySummaryResponse`
- [x] Tests unitarios: 8 tests (tests 36-43 en OrderServiceTest)

**Archivos:**
- `quickstack-pos/src/main/java/com/quickstack/pos/dto/response/DailySummaryResponse.java`
- `quickstack-pos/src/main/java/com/quickstack/pos/service/OrderService.java` (metodo agregado)
- `quickstack-pos/src/test/java/com/quickstack/pos/service/OrderServiceTest.java` (expandido)

**Notas de implementacion:**
- `COALESCE(SUM(total), 0.00)` + `.setScale(2)` en Java para asegurar BigDecimal escala 2 en JSON
- `averageTicket = BigDecimal.ZERO.setScale(2)` cuando no hay ordenes (evita `0` vs `0.00`)
- `order_items.product_id` tiene FK en `products` — en integration tests NO insertar items directamente

---

### [BACKEND] Tarea 6.2: Daily Summary Controller ✅

**Prioridad:** Media | **Dependencias:** 6.1

Controller para reportes.

**Criterios de Aceptacion:**
- [x] `GET /api/v1/reports/daily-summary` — MANAGER+. Params: `branchId` (required), `date` (optional, default=today Mexico City). Retorna `DailySummaryResponse` HTTP 200
- [x] Controller en `quickstack-pos` (convencion feature module, no quickstack-app)
- [x] Tests unitarios: 6 tests

**Archivos:**
- `quickstack-pos/src/main/java/com/quickstack/pos/controller/ReportController.java`
- `quickstack-pos/src/test/java/com/quickstack/pos/controller/ReportControllerTest.java`
- `quickstack-auth/src/main/java/com/quickstack/auth/config/SecurityConfig.java` (agregada ruta `/api/v1/reports/**`)

---

### [QA] Tarea 6.3: Tests de Integracion — Reportes ✅

**Prioridad:** Media | **Dependencias:** 6.2

Tests end-to-end.

**Criterios de Aceptacion:**
- [x] `GET /api/v1/reports/daily-summary?branchId={id}`: retorna metricas correctas (totalOrders, totalSales, averageTicket, ordersByServiceType) ✅
- [x] Dia vacio: retorna ceros ✅
- [x] Solo cuenta ordenes COMPLETED (PENDING y CANCELLED excluidos) ✅
- [x] CASHIER intenta acceder: retorna 403 ✅
- [x] branchId faltante: retorna 400 ✅
- [x] Cross-tenant branchId: retorna 404 ✅
- [x] 6 tests de integracion pasando

**Archivos:**
- `quickstack-app/src/test/java/com/quickstack/app/pos/ReportIntegrationTest.java`

---

## Endpoints Finales

| Metodo | Endpoint | Rol Minimo | Descripcion |
|--------|----------|-----------|-------------|
| `GET` | `/api/v1/branches` | CASHIER | Listar branches del tenant |
| `POST` | `/api/v1/branches` | OWNER | Crear branch |
| `GET` | `/api/v1/branches/{id}` | CASHIER | Ver branch por ID |
| `PUT` | `/api/v1/branches/{id}` | OWNER | Actualizar branch |
| `DELETE` | `/api/v1/branches/{id}` | OWNER | Soft delete de branch |
| `GET` | `/api/v1/branches/{branchId}/areas` | CASHIER | Listar areas de un branch |
| `POST` | `/api/v1/branches/{branchId}/areas` | MANAGER | Crear area |
| `GET` | `/api/v1/areas/{id}` | CASHIER | Ver area por ID |
| `PUT` | `/api/v1/areas/{id}` | MANAGER | Actualizar area |
| `DELETE` | `/api/v1/areas/{id}` | MANAGER | Soft delete de area |
| `GET` | `/api/v1/areas/{areaId}/tables` | CASHIER | Listar mesas de un area |
| `POST` | `/api/v1/areas/{areaId}/tables` | MANAGER | Crear mesa |
| `GET` | `/api/v1/tables/{id}` | CASHIER | Ver mesa por ID |
| `PUT` | `/api/v1/tables/{id}` | MANAGER | Actualizar mesa |
| `DELETE` | `/api/v1/tables/{id}` | MANAGER | Soft delete de mesa |
| `PATCH` | `/api/v1/tables/{id}/status` | MANAGER | Cambiar estado de mesa |
| `GET` | `/api/v1/customers` | CASHIER | Listar clientes (paginado, con search) |
| `POST` | `/api/v1/customers` | CASHIER | Crear cliente |
| `GET` | `/api/v1/customers/{id}` | CASHIER | Ver cliente por ID |
| `PUT` | `/api/v1/customers/{id}` | CASHIER | Actualizar cliente |
| `DELETE` | `/api/v1/customers/{id}` | MANAGER | Soft delete de cliente |
| `POST` | `/api/v1/orders` | CASHIER | Crear orden |
| `GET` | `/api/v1/orders/{id}` | CASHIER | Ver orden por ID (solo propias si CASHIER) |
| `GET` | `/api/v1/orders` | CASHIER | Listar ordenes (filtrable, paginado) |
| `POST` | `/api/v1/orders/{id}/items` | CASHIER | Agregar item a orden OPEN |
| `DELETE` | `/api/v1/orders/{orderId}/items/{itemId}` | CASHIER | Quitar item de orden OPEN |
| `POST` | `/api/v1/orders/{id}/submit` | CASHIER | Enviar orden a cocina (OPEN → SUBMITTED) |
| `POST` | `/api/v1/orders/{id}/cancel` | MANAGER | Cancelar orden |
| `POST` | `/api/v1/payments` | CASHIER | Registrar pago |
| `GET` | `/api/v1/orders/{orderId}/payments` | CASHIER | Listar pagos de una orden |
| `GET` | `/api/v1/reports/daily-summary` | MANAGER | Resumen de ventas del dia |

**Total: 28 endpoints**

---

## Resumen de Tests Esperados

| Sprint | Tipo | Tests Nuevos | Tests Acumulados | Estado |
|--------|------|-------------|------------------|--------|
| Sprint 1 | Unit + Integration | 74 | ~897 | ✅ |
| Sprint 2 | Unit + Integration | 46 | ~943 | ✅ |
| Sprint 3 | Unit + Integration | ~38 | ~981 | ✅ |
| Sprint 4 | Unit + Integration | ~72 | ~1053 | ✅ |
| Sprint 5 | Unit + Integration | 47 | ~1,040 real | ✅ |
| Sprint 6 | Unit + Integration | 20 | ~1,060 real | ✅ |

*Tests reales al cierre de Sprint 6: quickstack-pos=196, quickstack-app=135, quickstack-auth=234, quickstack-product=~406, quickstack-branch=60. Total ~1,060.*

---

## Notas de Implementacion para el Desarrollador

### Generacion de Order Number

```java
// En OrderService.java
@Transactional
public OrderResponse createOrder(UUID tenantId, UUID userId, OrderCreateRequest request) {
    LocalDate today = LocalDate.now(ZoneId.of("America/Mexico_City"));
    String dateStr = today.format(DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD

    int sequence = orderRepository.getNextDailySequence(tenantId, request.getBranchId(), today);
    String orderNumber = String.format("ORD-%s-%03d", dateStr, sequence);

    // Verificar unicidad (por si acaso)
    while (orderRepository.existsByOrderNumberAndTenantId(orderNumber, tenantId)) {
        sequence++;
        orderNumber = String.format("ORD-%s-%03d", dateStr, sequence);
    }

    Order order = new Order();
    order.setOrderNumber(orderNumber);
    order.setDailySequence(sequence);
    // ... resto de campos
}
```

### Lock pesimista para daily_sequence

```java
// En OrderRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query(value = """
    SELECT COALESCE(MAX(o.daily_sequence), 0) + 1
    FROM orders o
    WHERE o.tenant_id = :tenantId
      AND o.branch_id = :branchId
      AND DATE(o.opened_at) = :date
    """, nativeQuery = true)
Integer getNextDailySequence(
    @Param("tenantId") UUID tenantId,
    @Param("branchId") UUID branchId,
    @Param("date") LocalDate date
);
```

### Validation de ownership de ordenes (CASHIER)

```java
// En OrderPermissionEvaluator.java
public boolean canViewOrder(Authentication auth, Order order) {
    JwtAuthenticationPrincipal principal = (JwtAuthenticationPrincipal) auth.getPrincipal();
    String roleCode = principal.getRoleCode();

    if ("OWNER".equals(roleCode) || "MANAGER".equals(roleCode)) {
        return true; // Pueden ver todas las ordenes
    }

    if ("CASHIER".equals(roleCode)) {
        return order.getCreatedBy().equals(principal.getUserId()); // Solo propias
    }

    return false;
}
```

---

## Definition of Done

**Cada sprint se considera completo cuando:**

- [ ] Todas las tareas del sprint estan marcadas como completadas
- [ ] Todos los tests unitarios y de integracion del sprint pasan (`mvn verify`)
- [ ] No hay regresiones en tests de sprints anteriores
- [ ] Codigo revisado (self-review minimo)
- [ ] Coverage de services >80% (verificar con JaCoCo)
- [ ] Documentacion JavaDoc en metodos publicos de services
- [ ] Logs de auditoria implementados en operaciones de escritura
- [ ] Commit con mensaje descriptivo: `feat(order): Sprint X complete - [descripcion]`
