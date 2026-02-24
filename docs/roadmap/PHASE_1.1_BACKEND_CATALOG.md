# Phase 1.1: Catálogo Base — Product & Menu Management Roadmap

> **Version:** 1.1.0
> **Fecha:** 2026-02-24
> **Status:** ✅ COMPLETADA - 6/6 Sprints
> **Modulo Maven:** `quickstack-product`
> **Parte de:** Phase 1: Core POS - Ventas Completas

---

## Resumen Ejecutivo

Este documento define el plan de implementacion de la **primera sub-fase de Phase 1**: el modulo de catalogo base de productos y menus para QuickStack POS.

**Importante:** Esta es solo la **parte 1 de 4** de Phase 1. Cubre únicamente el catálogo básico (categorías, productos, variantes). Los modificadores, combos, pedidos, pagos y frontend se implementarán en las sub-fases posteriores (1.2, 1.3, 1.4).

| Aspecto | Detalle |
|---------|---------|
| **Timeline** | 6 sprints (~3 semanas) |
| **Tareas Backend** | 26 tareas (~70 horas) |
| **Tareas QA** | 9 tareas (~27 horas) |
| **Tareas Frontend** | 0 (fuera de scope esta phase) |
| **Checkpoints de Seguridad** | 2 (Post-Sprint 2, Post-Sprint 5) |
| **Endpoints nuevos** | 22 endpoints REST |
| **Migracion Flyway** | Reutilizar V3 ya existente (esquema ya definido) |

---

## Decisiones de Diseno Confirmadas

### Modulo Maven

| Decision | Valor | Justificacion |
|----------|-------|---------------|
| Modulo | `quickstack-product` (existente) | Ya creado con estructura correcta, dependencias ya en `quickstack-app` |
| Dependencias del modulo | `quickstack-common`, `quickstack-tenant` | Acceso a `ApiResponse`, excepciones, propiedades; Tenant para foreign key |
| Controladores | En `quickstack-product` | Decision arquitectonica actualizada: Modular Monolith. Los controladores viven junto al dominio. |

### Base de Datos

El esquema de base de datos para este modulo **ya fue definido en V3__create_catalog_module.sql**. No se requiere nueva migracion Flyway. Las tablas relevantes para esta phase son:

| Tabla | Descripcion |
|-------|-------------|
| `categories` | Categorias jerarquicas (max 2 niveles), soft delete |
| `products` | Productos del menu, soft delete |
| `product_variants` | Variantes de producto (Chico/Grande), soft delete |
| `modifier_groups` | Grupos de modificadores (Extras, Sin...), soft delete |
| `modifiers` | Opciones individuales dentro de un grupo, soft delete |
| `combos` | Paquetes de productos, fuera de scope esta phase |
| `combo_items` | Items de combos, fuera de scope esta phase |

### Disponibilidad de Productos

| Campo | Semantica |
|-------|-----------|
| `is_active = false` | Desactivado permanentemente. No aparece en ningun listado. Solo OWNER/MANAGER pueden ver con `?includeInactive=true` |
| `is_available = false` | Agotado temporalmente. Aparece en el catalogo del manager pero marcado como no disponible. El cashier lo ve pero no puede seleccionarlo |

### Variantes en esta Phase

Las variantes (`product_variants`) se incluyen en esta phase para productos de tipo `VARIANT`. El esquema ya existe. Los modifier groups/modifiers (personalizaciones libres como "sin cebolla") se declaran como **deuda tecnica aceptada** para Phase 0.5.

### Multi-tenancy

Todas las operaciones filtran por `tenant_id` extraido del JWT. Ningun endpoint acepta `tenant_id` como parametro de path o body — se extrae exclusivamente del `JwtAuthenticationPrincipal`.

### Paginacion y Filtros

Todos los endpoints de listado usan `Pageable` de Spring Data con parametros estandar:
- `page` (0-indexed), `size` (default 20, max 100), `sort` (campo,asc/desc)
- Filtros especificos por endpoint (ver tabla de endpoints)

### Roles y Permisos

| Operacion | OWNER | MANAGER | CASHIER |
|-----------|-------|---------|---------|
| Crear categoria/producto | SI | SI | NO |
| Editar categoria/producto | SI | SI | NO |
| Soft delete | SI | SI | NO |
| Restaurar (undelete) | SI | NO | NO |
| Ver inactivos (`includeInactive=true`) | SI | SI | NO |
| Listar menu activo | SI | SI | SI |
| Marcar producto como agotado/disponible | SI | SI | NO |

La verificacion de permisos usa el `roleId` del JWT. Los roles son UUIDs de la tabla global `roles` sembrada en V7__seed_data.sql. La autorizacion se implementa con `@PreAuthorize` y un `PermissionEvaluator` custom.

### Manejo de Errores

| Situacion | HTTP | Codigo |
|-----------|------|--------|
| Recurso no encontrado | 404 | `CATEGORY_NOT_FOUND`, `PRODUCT_NOT_FOUND` |
| Nombre duplicado en tenant | 409 | `DUPLICATE_NAME` |
| Precio negativo | 400 | `INVALID_PRICE` |
| Categoria con productos activos (delete) | 409 | `CATEGORY_HAS_PRODUCTS` |
| Acceso a recurso de otro tenant | 404 (no 403, para no revelar existencia) | `CATEGORY_NOT_FOUND` |
| Sin permiso de rol | 403 | `FORBIDDEN` |

---

## Deuda Tecnica Aceptada

| Item | Justificacion | Phase sugerida |
|------|---------------|----------------|
| Modifier groups y modifiers (personalizaciones) | Requiere UI compleja, se implementa después del catálogo base | **Phase 1.2** |
| Combos y combo items | Requiere logica de precios especial y validaciones complejas | **Phase 1.2** |
| Áreas y mesas | Parte del sistema de pedidos, no del catálogo | **Phase 1.3** |
| Clientes (delivery) | Parte del sistema de pedidos, no del catálogo | **Phase 1.3** |
| Sucursales (branches) | Se implementa junto con el módulo de pedidos | **Phase 1.3** |
| Imagenes de producto (upload) | Requiere storage externo (S3/GCS) y procesamiento | **Phase 1.3** o posterior |
| `available_from` / `available_until` (disponibilidad horaria) | Logica de timezone compleja, no crítico para MVP | **Phase 1.3** o posterior |
| Categorias jerarquicas mas de 2 niveles | No requerido por MVP | No planificado |
| Busqueda full-text con ponderacion | PostgreSQL FTS vs Elasticsearch decision pendiente | Phase 2+ |

---

## Arquitectura de Componentes

```
quickstack-product/
├── src/main/java/com/quickstack/product/
│   ├── controller/
│   │   ├── CategoryController.java
│   │   └── ProductController.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── CategoryCreateRequest.java
│   │   │   ├── CategoryUpdateRequest.java
│   │   │   ├── ProductCreateRequest.java
│   │   │   ├── ProductUpdateRequest.java
│   │   │   ├── ProductAvailabilityRequest.java
│   │   │   ├── VariantCreateRequest.java
│   │   │   └── VariantUpdateRequest.java
│   │   └── response/
│   │       ├── CategoryResponse.java
│   │       ├── CategorySummaryResponse.java
│   │       ├── ProductResponse.java
│   │       ├── ProductSummaryResponse.java
│   │       └── VariantResponse.java
│   ├── entity/
│   │   ├── Category.java
│   │   ├── Product.java
│   │   └── ProductVariant.java
│   ├── repository/
│   │   ├── CategoryRepository.java
│   │   └── ProductRepository.java
│   └── service/
│       ├── CategoryService.java
│       └── ProductService.java
│
quickstack-auth/
├── src/main/java/com/quickstack/auth/
│   └── config/
│       └── SecurityConfig.java              <- MODIFICAR: agregar rutas catalog

quickstack-product/
├── src/main/java/com/quickstack/product/
│   ├── security/
│   │   └── CatalogPermissionEvaluator.java  <- NUEVO
│   └── ... (servicios, entidades)

quickstack-common/
├── src/main/java/com/quickstack/common/
│   └── exception/
│       ├── ResourceNotFoundException.java   <- NUEVO
│       ├── DuplicateResourceException.java  <- NUEVO
│       └── GlobalExceptionHandler.java      <- MODIFICAR: handlers para nuevas excepciones
```

---

## Sprint 1: Entidades, Repositorios y Migracion

**Duracion:** 2 dias | **Objetivo:** Infraestructura de dominio completa y testeada

### [BACKEND] Tarea 1.1: Excepciones de Dominio en Common
**Prioridad:** Alta | **Dependencias:** Ninguna

Agregar excepciones de negocio reutilizables para el modulo de catalogo. Estas se usaran en todos los modulos futuros.

**Criterios de Aceptacion:**
- [x] `ResourceNotFoundException(String resourceType, UUID id)` extiende `ApiException` con HTTP 404, codigo `{RESOURCE_TYPE}_NOT_FOUND`
- [x] `DuplicateResourceException(String resourceType, String field, String value)` extiende `ApiException` con HTTP 409, codigo `DUPLICATE_{FIELD}`
- [x] `GlobalExceptionHandler` maneja ambas excepciones sin exponer stack trace
- [x] Tests unitarios: 8 tests (constructor, message format, HTTP status, handler mapping)

**Archivos:**
- `quickstack-common/src/main/java/com/quickstack/common/exception/ResourceNotFoundException.java`
- `quickstack-common/src/main/java/com/quickstack/common/exception/DuplicateResourceException.java`
- `quickstack-common/src/main/java/com/quickstack/common/exception/GlobalExceptionHandler.java` (modificar)
- `quickstack-common/src/test/java/com/quickstack/common/exception/ResourceNotFoundExceptionTest.java`
- `quickstack-common/src/test/java/com/quickstack/common/exception/DuplicateResourceExceptionTest.java`

---

### [BACKEND] Tarea 1.2: Entidad Category
**Prioridad:** Alta | **Dependencias:** Ninguna

Entidad JPA mapeada a la tabla `categories` ya existente en V3.

**Criterios de Aceptacion:**
- [x] `Category.java` con todos los campos de la tabla: `id`, `tenantId`, `parentId`, `name`, `description`, `imageUrl`, `sortOrder`, `isActive`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deletedAt`, `deletedBy`
- [x] Anotaciones JPA: `@Entity`, `@Table(name = "categories")`, `@Column` con nombres exactos de BD
- [x] `@CreationTimestamp` / `@UpdateTimestamp` en campos de audit
- [x] `isDeleted()` metodo conveniente que retorna `deletedAt != null`
- [x] Tests unitarios: 5 tests (constructor, isDeleted, equals por id)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/entity/Category.java`
- `quickstack-product/src/test/java/com/quickstack/product/entity/CategoryTest.java`

---

### [BACKEND] Tarea 1.3: Entidad Product y ProductVariant
**Prioridad:** Alta | **Dependencias:** 1.2

Entidades JPA para productos y sus variantes.

**Criterios de Aceptacion:**
- [x] `Product.java` con todos los campos: `id`, `tenantId`, `categoryId`, `name`, `description`, `sku`, `imageUrl`, `basePrice`, `costPrice`, `productType` (enum: SIMPLE, VARIANT, COMBO), `isActive`, `isAvailable`, `sortOrder`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deletedAt`, `deletedBy`
- [x] `ProductType.java` enum con valores `SIMPLE`, `VARIANT`, `COMBO`
- [x] `ProductVariant.java` con campos: `id`, `tenantId`, `productId`, `name`, `sku`, `priceAdjustment`, `isDefault`, `isActive`, `sortOrder`, `createdAt`, `updatedAt`, `deletedAt`
- [x] `Product` tiene `@OneToMany` a `ProductVariant` (fetch LAZY)
- [x] `Product.getEffectivePrice()`: retorna `basePrice` para SIMPLE, null para VARIANT (precio se calcula por variante)
- [x] Tests unitarios: 10 tests (constructor, enum parsing, getEffectivePrice, variant relationship)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/entity/Product.java`
- `quickstack-product/src/main/java/com/quickstack/product/entity/ProductType.java`
- `quickstack-product/src/main/java/com/quickstack/product/entity/ProductVariant.java`
- `quickstack-product/src/test/java/com/quickstack/product/entity/ProductTest.java`

---

### [BACKEND] Tarea 1.4: CategoryRepository
**Prioridad:** Alta | **Dependencias:** 1.2

Repositorio JPA con queries tenant-safe.

**Criterios de Aceptacion:**
- [x] `findAllByTenantId(UUID tenantId, Pageable pageable)` retorna solo no-borrados (`WHERE deleted_at IS NULL`)
- [x] `findAllByTenantIdIncludingInactive(UUID tenantId, Pageable pageable)` retorna activos e inactivos pero no borrados
- [x] `findByIdAndTenantId(UUID id, UUID tenantId)` retorna `Optional<Category>` — incluye inactivos pero excluye borrados
- [x] `existsByNameAndTenantIdAndParentId(String name, UUID tenantId, UUID parentId)` para validacion de unicidad
- [x] `existsByNameAndTenantIdAndParentIdAndIdNot(String name, UUID tenantId, UUID parentId, UUID excludeId)` para validacion en update
- [x] `countActiveProductsByCategory(UUID categoryId, UUID tenantId)`: cuenta productos activos no borrados en esa categoria (para bloquear delete)
- [x] Tests de repositorio con `@DataJpaTest` + Testcontainers: 12 tests

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/repository/CategoryRepository.java`
- `quickstack-product/src/test/java/com/quickstack/product/repository/CategoryRepositoryTest.java`

---

### [BACKEND] Tarea 1.5: ProductRepository
**Prioridad:** Alta | **Dependencias:** 1.3

Repositorio JPA para productos con soporte de busqueda y filtrado.

**Criterios de Aceptacion:**
- [x] `findAllByTenantId(UUID tenantId, Pageable pageable)` retorna activos y disponibles, no borrados
- [x] `findAllByTenantIdWithFilters(UUID tenantId, UUID categoryId, Boolean isAvailable, String nameSearch, Pageable pageable)` — todos los filtros son opcionales (null = sin filtrar). Usa `@Query` JPQL o `Specification`
- [x] `findByIdAndTenantId(UUID id, UUID tenantId)` retorna `Optional<Product>` incluyendo inactivos, excluye borrados
- [x] `existsBySkuAndTenantId(String sku, UUID tenantId)` para validacion de SKU unico
- [x] `existsBySkuAndTenantIdAndIdNot(String sku, UUID tenantId, UUID excludeId)` para update
- [x] `existsByNameAndTenantIdAndCategoryId(String name, UUID tenantId, UUID categoryId)` para unicidad de nombre
- [x] Tests de repositorio con `@DataJpaTest` + Testcontainers: 15 tests (incluyendo tests de filtros combinados)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/repository/ProductRepository.java`
- `quickstack-product/src/test/java/com/quickstack/product/repository/ProductRepositoryTest.java`

---

### [QA] Tarea 1.6: Verificar Migracion V3 con Testcontainers
**Prioridad:** Alta | **Dependencias:** 1.4, 1.5

Validar que el esquema V3 existente carga correctamente y las constraints funcionan.

**Criterios de Aceptacion:**
- [x] Test de integracion que arranca contexto Spring completo con Testcontainers
- [x] Verificar que `INSERT` con `tenant_id` invalido falla con FK violation
- [x] Verificar que `INSERT` de categoria con nombre duplicado en mismo tenant falla con unique constraint
- [x] Verificar que `INSERT` de producto con SKU duplicado en mismo tenant falla
- [x] Verificar que categoria con `parent_id` de otro tenant falla con FK violation (constraint cross-tenant)
- [x] Verificar que soft delete (solo `deleted_at`) no viola constraints
- [x] 6 tests de integracion pasando

**Archivos:**
- `quickstack-app/src/test/java/com/quickstack/app/catalog/CatalogSchemaE2ETest.java`

---

## Sprint 2: Category Management ✅ COMPLETADO

**Duracion:** 2 dias | **Objetivo:** CRUD completo de categorias con permisos

### [BACKEND] Tarea 2.1: DTOs de Categoria ✅
**Prioridad:** Alta | **Dependencias:** 1.2

Objetos de transferencia para requests y responses de categorias.

**Criterios de Aceptacion:**
- [x] `CategoryCreateRequest`: `name` (NotBlank, max 255), `description` (nullable, max 1000), `imageUrl` (nullable, URL format, max 500), `parentId` (nullable UUID), `sortOrder` (nullable int, default 0)
- [x] `CategoryUpdateRequest`: mismos campos que Create, todos opcionales excepto que al menos uno debe estar presente (validacion custom o `@Validated`)
- [x] `CategoryResponse`: todos los campos de la entidad + campo `productCount` (int, numero de productos activos no borrados)
- [x] `CategorySummaryResponse`: solo `id`, `name`, `sortOrder`, `isActive`, `parentId` — para embeds en respuestas de producto
- [x] Tests unitarios: 8 tests (Bean Validation, edge cases de campos)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/dto/request/CategoryCreateRequest.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/request/CategoryUpdateRequest.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/CategoryResponse.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/CategorySummaryResponse.java`
- `quickstack-product/src/test/java/com/quickstack/product/dto/CategoryDtoTest.java`

---

### [BACKEND] Tarea 2.2: CategoryService ✅
**Prioridad:** Alta | **Dependencias:** 1.1, 1.4, 2.1

Logica de negocio para categorias.

**Criterios de Aceptacion:**
- [x] `createCategory(UUID tenantId, UUID userId, CategoryCreateRequest)`: valida nombre unico por tenant+parentId, persiste, retorna `CategoryResponse`
- [x] `updateCategory(UUID tenantId, UUID userId, UUID categoryId, CategoryUpdateRequest)`: valida que existe y es del tenant, valida nombre unico (excluyendo el propio), actualiza `updatedBy` y `updatedAt`, retorna `CategoryResponse`
- [x] `deleteCategory(UUID tenantId, UUID userId, UUID categoryId)`: valida que no tiene productos activos (lanza `BusinessRuleException` con 409 si los tiene), hace soft delete (setea `deletedAt`, `deletedBy`), retorna void
- [x] `getCategory(UUID tenantId, UUID categoryId)`: retorna `CategoryResponse` o lanza `ResourceNotFoundException`
- [x] `listCategories(UUID tenantId, boolean includeInactive, Pageable)`: retorna `Page<CategoryResponse>`
- [x] Toda operacion de escritura es `@Transactional`
- [x] Tests unitarios con mocks: 18 tests (happy path + cada caso de error)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/service/CategoryService.java`
- `quickstack-common/src/main/java/com/quickstack/common/exception/BusinessRuleException.java` (NUEVO, HTTP 409, codigo configurable)
- `quickstack-product/src/test/java/com/quickstack/product/service/CategoryServiceTest.java`

---

### [BACKEND] Tarea 2.3: CatalogPermissionEvaluator ✅
**Prioridad:** Alta | **Dependencias:** Ninguna (solo usa JwtAuthenticationPrincipal)

Componente de autorizacion para operaciones de catalogo.

**Criterios de Aceptacion:**
- [x] `CatalogPermissionEvaluator` es un `@Component` que expone metodos para usar con `@PreAuthorize`
- [x] `canManageCatalog(Authentication auth)`: retorna true si rol es OWNER o MANAGER
- [x] `canDeleteCategory(Authentication auth)`: retorna true si rol es OWNER o MANAGER
- [x] `canRestoreCategory(Authentication auth)`: retorna true solo si rol es OWNER
- [x] `canViewInactive(Authentication auth)`: retorna true si rol es OWNER o MANAGER
- [x] Metodos son evaluados comparando el `roleCode` del JWT claim (`rol` claim: "OWNER", "MANAGER", "CASHIER")
- [x] Tests unitarios: 12 tests (cada metodo con cada rol + token invalido)

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/security/CatalogPermissionEvaluator.java`
- `quickstack-app/src/test/java/com/quickstack/app/security/CatalogPermissionEvaluatorTest.java`

---

### [BACKEND] Tarea 2.4: CategoryController ✅
**Prioridad:** Alta | **Dependencias:** 2.2, 2.3

Controller REST para categorias.

**Criterios de Aceptacion:**
- [x] `GET /api/v1/categories` — requiere JWT. CASHIER: solo activas. OWNER/MANAGER: acepta `?includeInactive=true`. Retorna `Page<CategoryResponse>` con HTTP 200
- [x] `POST /api/v1/categories` — requiere OWNER o MANAGER. `@Valid` en body. Retorna `CategoryResponse` con HTTP 201 + header `Location: /api/v1/categories/{id}`
- [x] `GET /api/v1/categories/{id}` — requiere JWT. CASHIER solo ve activas (404 si inactiva). Retorna `CategoryResponse` con HTTP 200
- [x] `PUT /api/v1/categories/{id}` — requiere OWNER o MANAGER. Retorna `CategoryResponse` con HTTP 200
- [x] `DELETE /api/v1/categories/{id}` — requiere OWNER o MANAGER. Retorna HTTP 204 (no body)
- [x] `POST /api/v1/categories/{id}/restore` — requiere solo OWNER. Retorna `CategoryResponse` con HTTP 200. (Restaura soft-deleted)
- [x] Todos los endpoints extraen `tenantId` del `JwtAuthenticationPrincipal`, nunca de params
- [x] Tests unitarios con `@WebMvcTest`: 20 tests (happy path + 401 + 403 por rol + 404 + 409)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/controller/CategoryController.java`
- `quickstack-app/src/test/java/com/quickstack/app/controller/CategoryControllerTest.java`

---

### [QA] Tarea 2.5: Tests de Integracion de Categorias ✅
**Prioridad:** Alta | **Dependencias:** 2.4

Tests end-to-end que ejercen el stack completo con base de datos real.

**Criterios de Aceptacion:**
- [x] Extienden `BaseIntegrationTest`
- [x] Setup crea tenant real en BD y genera JWTs con roles OWNER, MANAGER, CASHIER
- [x] `POST /api/v1/categories` con OWNER: retorna 201 con `id` generado
- [x] `POST /api/v1/categories` con CASHIER: retorna 403
- [x] `POST /api/v1/categories` nombre duplicado en mismo tenant: retorna 409
- [x] `GET /api/v1/categories` con CASHIER: no incluye categorias inactivas aunque existan
- [x] `GET /api/v1/categories` con MANAGER + `includeInactive=true`: incluye inactivas
- [x] `DELETE /api/v1/categories/{id}` con productos activos: retorna 409
- [x] `DELETE /api/v1/categories/{id}` sin productos: retorna 204, registro sigue en BD con `deleted_at` seteado
- [x] Cross-tenant: categoria de tenant A no visible para JWT de tenant B (404)
- [x] 12 tests de integracion implementados (requieren Docker/Testcontainers para ejecutar)

**Archivos:**
- `quickstack-app/src/test/java/com/quickstack/app/catalog/CategoryE2ETest.java`

---

### Checkpoint de Seguridad Post-Sprint 2

**Validaciones requeridas antes de continuar:**

- [ ] Verificar que ningun endpoint filtra solo por `id` sin incluir `tenant_id` en la query (inspeccion manual de queries generadas con `spring.jpa.show-sql=true`)
- [ ] Test de IDOR: usuario de tenant B intenta GET/PUT/DELETE sobre categoria de tenant A — debe recibir 404, no 403 (para no revelar existencia)
- [ ] Test de escalada de privilegios: CASHIER intenta POST, PUT, DELETE — todos deben ser 403
- [ ] Verificar que el campo `deletedBy` se setea correctamente (registra quien borro)
- [ ] Confirmar que el esquema de paginacion no expone categorias borradas bajo ninguna combinacion de parametros
- [ ] Revisar logs: operaciones exitosas loguean `tenantId` y `userId` (audit trail minimo)

---

## Sprint 3: Product Management (SIMPLE y VARIANT) ✅ COMPLETADO

**Duracion:** 3 dias | **Objetivo:** CRUD completo de productos incluyendo variantes

### [BACKEND] Tarea 3.1: DTOs de Producto ✅
**Prioridad:** Alta | **Dependencias:** 1.3, 2.1

**Criterios de Aceptacion:**
- [x] `ProductCreateRequest`: `name` (NotBlank, max 255), `description` (nullable, max 2000), `categoryId` (NotNull UUID), `sku` (nullable, max 50, pattern `^[A-Z0-9_-]{1,50}$`), `basePrice` (NotNull, min 0.01, max 99999.99 — 2 decimales), `costPrice` (nullable, min 0), `imageUrl` (nullable, URL, max 500), `productType` (NotNull, enum), `sortOrder` (nullable, default 0), `variants` (lista nullable de `VariantCreateRequest`, solo valida si `productType == VARIANT`)
- [x] `VariantCreateRequest`: `name` (NotBlank, max 100), `sku` (nullable, mismo patron), `priceAdjustment` (NotNull, puede ser negativo pero `basePrice + priceAdjustment >= 0`), `isDefault` (boolean, default false), `sortOrder` (nullable)
- [x] `ProductUpdateRequest`: mismos campos que Create, todos opcionales
- [x] `ProductAvailabilityRequest`: `isAvailable` (NotNull boolean) — para el endpoint de marcar agotado
- [x] `ProductResponse`: todos los campos de la entidad + `category` (CategorySummaryResponse) + `variants` (lista de VariantResponse, solo si productType == VARIANT)
- [x] `ProductSummaryResponse`: `id`, `name`, `basePrice`, `productType`, `isAvailable`, `isActive`, `categoryId`, `imageUrl` — para listados paginados
- [x] `VariantResponse`: todos los campos de ProductVariant + `effectivePrice` (basePrice + priceAdjustment)
- [x] Tests unitarios: 15 tests (validaciones Bean Validation, precio minimo, crossField validation en variantes)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/dto/request/ProductCreateRequest.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/request/ProductUpdateRequest.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/request/ProductAvailabilityRequest.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/request/VariantCreateRequest.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/request/VariantUpdateRequest.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/ProductResponse.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/ProductSummaryResponse.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/VariantResponse.java`
- `quickstack-product/src/test/java/com/quickstack/product/dto/ProductDtoTest.java`

---

### [BACKEND] Tarea 3.2: ProductService — CRUD basico ✅
**Prioridad:** Alta | **Dependencias:** 1.1, 1.5, 3.1

Logica de negocio: crear y leer productos.

**Criterios de Aceptacion:**
- [x] `createProduct(UUID tenantId, UUID userId, ProductCreateRequest)`:
  - Valida que `categoryId` pertenece al tenant (lanza `ResourceNotFoundException` si no)
  - Valida SKU unico por tenant (lanza `DuplicateResourceException` si duplicado)
  - Si `productType == VARIANT`, requiere al menos una variante en el request (lanza `BusinessRuleException` si vacia)
  - Si `productType == VARIANT`, exactamente una variante debe tener `isDefault == true` (valida, si ninguna la primera se marca como default)
  - Persiste producto y variantes en transaccion unica
  - Retorna `ProductResponse`
- [x] `getProduct(UUID tenantId, UUID productId)`: retorna `ProductResponse` con variantes incluidas, lanza `ResourceNotFoundException` si no existe o es de otro tenant
- [x] `listProducts(UUID tenantId, UUID categoryId, Boolean isAvailable, String nameSearch, boolean includeInactive, Pageable)`: retorna `Page<ProductSummaryResponse>`. `nameSearch` usa `ILIKE '%term%'`
- [x] Tests unitarios con mocks: 18 tests

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/service/ProductService.java`
- `quickstack-product/src/test/java/com/quickstack/product/service/ProductServiceTest.java`

---

### [BACKEND] Tarea 3.3: ProductService — Update, Delete, Availability ✅
**Prioridad:** Alta | **Dependencias:** 3.2

**Criterios de Aceptacion:**
- [x] `updateProduct(UUID tenantId, UUID userId, UUID productId, ProductUpdateRequest)`:
  - Solo actualiza campos presentes en el request (partial update semantics)
  - Re-valida unicidad de SKU excluyendo el propio
  - Si se cambia `categoryId`, valida que la nueva categoria existe y pertenece al tenant
  - Si `productType` cambia de SIMPLE a VARIANT, requiere variantes en el request
  - Actualiza `updatedBy` y `updatedAt`
  - Retorna `ProductResponse`
- [x] `deleteProduct(UUID tenantId, UUID userId, UUID productId)`: soft delete (setea `deletedAt`, `deletedBy`). No verifica si hay ordenes — las ordenes guardan snapshot del producto al momento de la orden (ver deuda tecnica)
- [x] `setAvailability(UUID tenantId, UUID userId, UUID productId, boolean isAvailable)`: actualiza solo `is_available`. Retorna `ProductResponse`. Disponible para OWNER y MANAGER
- [x] `restoreProduct(UUID tenantId, UUID userId, UUID productId)`: limpia `deletedAt` y `deletedBy`. Solo OWNER. Lanza `ResourceNotFoundException` si no existe (borrado o nunca existio)
- [x] Tests unitarios con mocks: 18 tests (incluyendo intentar restaurar producto que nunca existio)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/service/ProductService.java` (modificar)
- `quickstack-product/src/test/java/com/quickstack/product/service/ProductServiceTest.java` (expandir)

---

### [BACKEND] Tarea 3.4: ProductController ✅
**Prioridad:** Alta | **Dependencias:** 3.3, 2.3

**Criterios de Aceptacion:**
- [x] `GET /api/v1/products` — requiere JWT. Params: `categoryId` (UUID), `available` (boolean), `search` (string, max 100), `includeInactive` (boolean, solo OWNER/MANAGER). Retorna `Page<ProductSummaryResponse>` HTTP 200
- [x] `POST /api/v1/products` — OWNER/MANAGER. Retorna `ProductResponse` HTTP 201 + `Location` header
- [x] `GET /api/v1/products/{id}` — JWT. CASHIER no ve inactivos (404). Retorna `ProductResponse` HTTP 200
- [x] `PUT /api/v1/products/{id}` — OWNER/MANAGER. Retorna `ProductResponse` HTTP 200
- [x] `DELETE /api/v1/products/{id}` — OWNER/MANAGER. HTTP 204
- [x] `PATCH /api/v1/products/{id}/availability` — OWNER/MANAGER. Body: `ProductAvailabilityRequest`. Retorna `ProductResponse` HTTP 200
- [x] `POST /api/v1/products/{id}/restore` — solo OWNER. Retorna `ProductResponse` HTTP 200
- [x] Tests unitarios con `@WebMvcTest`: 21 tests

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/controller/ProductController.java`
- `quickstack-app/src/test/java/com/quickstack/app/controller/ProductControllerTest.java`

---

### [QA] Tarea 3.5: Tests de Integracion de Productos ✅
**Prioridad:** Alta | **Dependencias:** 3.4

**Criterios de Aceptacion:**
- [x] Extienden `BaseIntegrationTest`
- [x] Setup crea tenant, categorias de prueba y JWTs con los 3 roles
- [x] `POST /api/v1/products` tipo SIMPLE: retorna 201 con precio correcto
- [x] `POST /api/v1/products` tipo VARIANT sin variantes: retorna 400
- [x] `POST /api/v1/products` con SKU duplicado: retorna 409
- [x] `POST /api/v1/products` con `categoryId` de otro tenant: retorna 404
- [x] `GET /api/v1/products?categoryId={id}`: retorna solo productos de esa categoria
- [x] `GET /api/v1/products?search=taco`: retorna solo productos que contienen "taco" (case-insensitive)
- [x] `PATCH /api/v1/products/{id}/availability` con CASHIER: retorna 403
- [x] `PATCH /api/v1/products/{id}/availability` con MANAGER: retorna 200, campo `isAvailable` cambiado en BD
- [x] Cross-tenant: producto de tenant A con JWT de tenant B retorna 404
- [x] 14 tests de integracion pasando

**Archivos:**
- `quickstack-app/src/test/java/com/quickstack/app/catalog/ProductE2ETest.java`

---

## Sprint 4: Variant Management ✅

**Duracion:** 1.5 dias | **Objetivo:** CRUD de variantes como sub-recurso de producto

### [BACKEND] Tarea 4.1: VariantRepository
**Prioridad:** Alta | **Dependencias:** 1.3

**Criterios de Aceptacion:**
- [x] `findAllByProductIdAndTenantId(UUID productId, UUID tenantId)` retorna variantes no borradas ordenadas por `sort_order`
- [x] `findByIdAndProductIdAndTenantId(UUID id, UUID productId, UUID tenantId)` retorna `Optional<ProductVariant>`
- [x] `countByProductIdAndTenantIdAndDeletedAtIsNull(UUID productId, UUID tenantId)` para validar que VARIANT tiene al menos 1 variante antes de borrar
- [x] `existsBySkuAndTenantId(String sku, UUID tenantId)` para unicidad de SKU
- [x] Tests de repositorio `@DataJpaTest`: 8 tests

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/repository/VariantRepository.java`
- `quickstack-product/src/test/java/com/quickstack/product/repository/VariantRepositoryTest.java`

---

### [BACKEND] Tarea 4.2: VariantService
**Prioridad:** Alta | **Dependencias:** 4.1, 3.1

**Criterios de Aceptacion:**
- [x] `addVariant(UUID tenantId, UUID userId, UUID productId, VariantCreateRequest)`:
  - Valida que el producto existe, es del tenant, y su `productType == VARIANT`
  - Valida SKU unico por tenant
  - Valida nombre unico dentro del producto
  - Si `isDefault == true`, resetea `isDefault = false` en todas las otras variantes del producto (en misma transaccion)
  - Retorna `VariantResponse`
- [x] `updateVariant(UUID tenantId, UUID userId, UUID productId, UUID variantId, VariantUpdateRequest)`:
  - Valida que variante pertenece al producto y al tenant
  - Maneja cambio de `isDefault` (resetea otras)
  - Retorna `VariantResponse`
- [x] `deleteVariant(UUID tenantId, UUID userId, UUID productId, UUID variantId)`:
  - Lanza `BusinessRuleException` si el producto solo tiene 1 variante (no puede quedar sin ninguna)
  - Si la variante a borrar es `isDefault`, asigna `isDefault = true` a la primera variante restante
  - Soft delete
- [x] `listVariants(UUID tenantId, UUID productId)`: retorna lista de `VariantResponse` ordenada por `sort_order`
- [x] Tests unitarios con mocks: 16 tests

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/service/VariantService.java`
- `quickstack-product/src/test/java/com/quickstack/product/service/VariantServiceTest.java`

---

### [BACKEND] Tarea 4.3: VariantController (sub-recurso de producto)
**Prioridad:** Alta | **Dependencias:** 4.2, 2.3

**Criterios de Aceptacion:**
- [x] `GET /api/v1/products/{productId}/variants` — JWT. Retorna `List<VariantResponse>` HTTP 200
- [x] `POST /api/v1/products/{productId}/variants` — OWNER/MANAGER. Retorna `VariantResponse` HTTP 201
- [x] `PUT /api/v1/products/{productId}/variants/{variantId}` — OWNER/MANAGER. Retorna `VariantResponse` HTTP 200
- [x] `DELETE /api/v1/products/{productId}/variants/{variantId}` — OWNER/MANAGER. HTTP 204
- [x] Si el producto no existe o es de otro tenant: todos los endpoints retornan 404
- [x] Si el producto es de tipo SIMPLE: `POST` retorna 409 con codigo `PRODUCT_NOT_VARIANT`
- [x] Tests unitarios con `@WebMvcTest`: 12 tests

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/controller/VariantController.java`
- `quickstack-app/src/test/java/com/quickstack/app/controller/VariantControllerTest.java`

---

## Sprint 5: Security Hardening y Ordering

**Duracion:** 2 dias | **Objetivo:** Completar permisos, ordering y tests de seguridad

### [BACKEND] Tarea 5.1: Reorder Endpoint para Categorias y Productos
**Prioridad:** Media | **Dependencias:** 2.4, 3.4

Los restaurantes necesitan controlar el orden de aparicion en el menu del POS.

**Criterios de Aceptacion:**
- [x] `ReorderRequest`: lista de objetos `{id: UUID, sortOrder: int}`. Validacion: lista no vacia, max 500 items, `sortOrder >= 0`
- [x] `PATCH /api/v1/categories/reorder` — OWNER/MANAGER. Body: `ReorderRequest`. Valida que todos los IDs pertenecen al tenant (lanza 400 si hay ID de otro tenant). Actualiza `sort_order` en batch con una sola query. Retorna HTTP 204
- [x] `PATCH /api/v1/products/reorder` — OWNER/MANAGER. Mismo comportamiento para productos
- [x] Operacion atomica: todos los updates en una sola transaccion
- [x] Tests unitarios de servicio: 8 tests (happy path, ID de otro tenant, lista vacia)
- [x] Tests unitarios de controller `@WebMvcTest`: 6 tests

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/dto/request/ReorderRequest.java`
- `quickstack-product/src/main/java/com/quickstack/product/service/CategoryService.java` (modificar: agregar `reorderCategories`)
- `quickstack-product/src/main/java/com/quickstack/product/service/ProductService.java` (modificar: agregar `reorderProducts`)
- `quickstack-product/src/main/java/com/quickstack/product/controller/CategoryController.java` (modificar)
- `quickstack-product/src/main/java/com/quickstack/product/controller/ProductController.java` (modificar)

---

### [BACKEND] Tarea 5.2: Actualizar SecurityConfig para rutas de Catalog
**Prioridad:** Alta | **Dependencias:** 2.4, 3.4, 4.3

**Criterios de Aceptacion:**
- [x] `GET /api/v1/categories/**` y `GET /api/v1/products/**` — requieren `authenticated()` (cualquier rol valido)
- [x] `POST /api/v1/categories`, `PUT /api/v1/categories/**`, `DELETE /api/v1/categories/**` — protegidos (la autorizacion de rol se hace con `@PreAuthorize` en el controller)
- [x] `PATCH /api/v1/categories/reorder`, `PATCH /api/v1/products/reorder` — protegidos
- [x] `POST /api/v1/*/restore` — protegidos
- [x] Ningun endpoint de catalog es publico (no requiere autenticacion)
- [x] Tests de integracion existentes siguen pasando (no regresiones)
- [x] Test especifico: request sin JWT a `GET /api/v1/products` retorna 401

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/config/SecurityConfig.java` (modificar)

---

### [QA] Tarea 5.3: Tests de Seguridad — Autorizacion y Tenant Isolation
**Prioridad:** Alta | **Dependencias:** 5.2

Bateria de tests de seguridad especificos del modulo catalog.

**Criterios de Aceptacion:**
- [x] **IDOR tests**: para cada endpoint con `{id}`, verificar que un JWT de tenant B con un ID valido de tenant A retorna 404 (no 403 ni 200)
- [x] **Escalada de privilegios**: CASHIER intentando POST/PUT/DELETE/PATCH retorna 403 en todos los casos
- [x] **Token expirado**: request con JWT expirado retorna 401 con formato `ApiError` estandar
- [x] **Rol invalido en JWT**: JWT con `roleCode` desconocido tratado como sin permisos — GET retorna 200 (autenticado), POST retorna 403
- [x] **Paginacion**: `?page=-1` retorna 400. `?size=101` retorna 400. `?sort=campoInexistente,asc` retorna 400
- [x] **Input injection**: nombre de categoria con `'; DROP TABLE categories; --` procesado como string literal, no ejecutado
- [x] **Reorder con IDs mixtos**: `PATCH /api/v1/categories/reorder` con mezcla de IDs propios y de otro tenant retorna 400 y no modifica ninguno
- [x] 14 tests de seguridad pasando

**Archivos:**
- `quickstack-app/src/test/java/com/quickstack/app/catalog/CatalogSecurityE2ETest.java`

---

### [BACKEND] Tarea 5.4: Logging y Audit Trail
**Prioridad:** Media | **Dependencias:** 2.2, 3.2, 3.3

Asegurar trazabilidad de operaciones de escritura.

**Criterios de Aceptacion:**
- [x] Cada metodo de escritura en `CategoryService` y `ProductService` emite un log nivel `INFO` con formato: `[CATALOG] ACTION={} tenantId={} userId={} resourceId={} resourceType={}`
- [x] Crear enum `CatalogAction` con valores: `CATEGORY_CREATED`, `CATEGORY_UPDATED`, `CATEGORY_DELETED`, `CATEGORY_RESTORED`, `PRODUCT_CREATED`, `PRODUCT_UPDATED`, `PRODUCT_DELETED`, `PRODUCT_RESTORED`, `PRODUCT_AVAILABILITY_CHANGED`
- [x] Logs de operaciones fallidas (excepciones de negocio) nivel `WARN` con mismo formato + campo `reason`
- [x] No loguear el contenido del body (datos potencialmente sensibles como precios de costo)
- [x] Tests unitarios: verificar que logs se emiten con Mockito verify o Logback captura (8 tests)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/service/CatalogAction.java`
- `quickstack-product/src/main/java/com/quickstack/product/service/CategoryService.java` (modificar)
- `quickstack-product/src/main/java/com/quickstack/product/service/ProductService.java` (modificar)

---

### Checkpoint de Seguridad Post-Sprint 5

**Validaciones requeridas antes de declarar Phase 0.4 completa:**

- [x] **ASVS V1.2.3**: Verificar que todas las operaciones de escritura validan tenant ownership antes de ejecutar (revision manual del codigo)
- [x] **ASVS V4.1.1**: Confirmar que el control de acceso se implementa en el servidor, no en el cliente (ningun permiso hardcodeado en queries sin validacion de rol)
- [x] **ASVS V4.1.3**: Principio de minimo privilegio — CASHIER tiene acceso de solo lectura confirmado por tests de integracion
- [x] **ASVS V4.1.5**: Logs de control de acceso para operaciones fallidas (Tarea 5.4 completada)
- [x] **ASVS V5.1.3**: Validacion de inputs en server-side confirmada — no se puede bypassear con headers custom
- [x] Ejecutar `./mvnw verify` en estado limpio: 0 fallos, todos los tests pasando
- [x] Revisar cobertura de tests: services deben tener >80% de line coverage (opcional pero recomendado)

---

## Sprint 6: Menu Endpoint Publico del Catalogo (Vista POS) ✅ COMPLETADO

**Duracion:** 1.5 dias | **Objetivo:** Endpoint optimizado para consumo desde la pantalla del POS

### [BACKEND] Tarea 6.1: MenuResponse DTO
**Prioridad:** Media | **Dependencias:** 2.1, 3.1

El POS necesita cargar todo el catalogo de una sola llamada, organizado por categorias.

**Criterios de Aceptacion:**
- [x] `MenuResponse`: estructura jerarquica — lista de categorias, cada una con sus productos disponibles
- [x] `MenuCategoryItem`: `id`, `name`, `sortOrder`, `imageUrl` + lista de `MenuProductItem`
- [x] `MenuProductItem`: `id`, `name`, `basePrice`, `imageUrl`, `isAvailable`, `productType` + lista de `MenuVariantItem` (solo si VARIANT)
- [x] `MenuVariantItem`: `id`, `name`, `priceAdjustment`, `effectivePrice`, `isDefault`, `sortOrder`
- [x] Incluye solo productos con `is_active = true` y `deleted_at IS NULL`. Incluye productos con `is_available = false` pero los marca (para que el cajero los vea como agotados)
- [x] Excluye categorias vacias (sin productos activos)
- [x] Ordenado por `sort_order` tanto en categorias como en productos
- [x] Tests unitarios del DTO: 5 tests

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/MenuResponse.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/MenuCategoryItem.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/MenuProductItem.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/MenuVariantItem.java`
- `quickstack-product/src/test/java/com/quickstack/product/dto/MenuResponseTest.java`

---

### [BACKEND] Tarea 6.2: MenuService
**Prioridad:** Media | **Dependencias:** 6.1, 1.4, 1.5

**Criterios de Aceptacion:**
- [x] `getMenu(UUID tenantId)`: retorna `MenuResponse` completo del tenant
- [x] Implementado con 3 queries JPQL separadas (categorias, productos, variantes) — sin N+1. Agrupado en memoria con `Map<UUID, List<T>>`
- [x] Resultado es ordenado: categorias por `sort_order`, productos dentro de categoria por `sort_order`
- [x] Tests unitarios: 6 tests (menu con multiples categorias, menu vacio, categoria sin productos excluida, producto agotado incluido pero marcado)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/service/MenuService.java`
- `quickstack-product/src/test/java/com/quickstack/product/service/MenuServiceTest.java`

---

### [BACKEND] Tarea 6.3: MenuController
**Prioridad:** Media | **Dependencias:** 6.2, 5.2

**Criterios de Aceptacion:**
- [x] `GET /api/v1/menu` — requiere JWT (cualquier rol). Retorna `MenuResponse` HTTP 200
- [x] Sin paginacion (retorna todo el menu — asuncion valida para MVP: un restaurante tiene <500 productos)
- [x] Header `Cache-Control: max-age=30, private` — el menu puede cachearse en cliente por 30 segundos
- [x] Si no hay productos activos: retorna `MenuResponse` con lista vacia (no 404)
- [x] Tests unitarios con Mockito: 6 tests (happy path, extraccion de tenantId, header Cache-Control, menu vacio, ApiResponse wrapper, variantes en VARIANT)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/controller/MenuController.java`
- `quickstack-app/src/test/java/com/quickstack/app/controller/MenuControllerTest.java`

---

### [QA] Tarea 6.4: Tests de Integracion de Menu y Tests de Rendimiento Basico
**Prioridad:** Media | **Dependencias:** 6.3

**Criterios de Aceptacion:**
- [x] Setup: crear tenant con categorias, productos (mix de activos/inactivos/agotados) y variantes
- [x] `GET /api/v1/menu`: retorna solo categorias con productos activos
- [x] Productos agotados (`is_available = false`) aparecen en el resultado con campo marcado
- [x] Productos inactivos (`is_active = false`) NO aparecen en el resultado
- [x] Verificar ordenamiento: categorias y productos respetan `sort_order`
- [x] `GET /api/v1/menu` sin JWT: retorna 403 (comportamiento de Spring Security sin AuthenticationEntryPoint)
- [x] Multi-tenant isolation: tenant A no ve productos de tenant B
- [x] 8 tests de integracion pasando

**Archivos:**
- `quickstack-app/src/test/java/com/quickstack/app/catalog/MenuE2ETest.java`

---

## Endpoints Finales

| Metodo | Endpoint | Rol Minimo | Descripcion |
|--------|----------|-----------|-------------|
| `GET` | `/api/v1/categories` | CASHIER | Listar categorias (paginado). `?includeInactive=true` solo OWNER/MANAGER |
| `POST` | `/api/v1/categories` | MANAGER | Crear categoria |
| `GET` | `/api/v1/categories/{id}` | CASHIER | Ver categoria por ID |
| `PUT` | `/api/v1/categories/{id}` | MANAGER | Actualizar categoria |
| `DELETE` | `/api/v1/categories/{id}` | MANAGER | Soft delete de categoria |
| `POST` | `/api/v1/categories/{id}/restore` | OWNER | Restaurar categoria borrada |
| `PATCH` | `/api/v1/categories/reorder` | MANAGER | Reordenar categorias |
| `GET` | `/api/v1/products` | CASHIER | Listar productos (paginado, filtrable) |
| `POST` | `/api/v1/products` | MANAGER | Crear producto |
| `GET` | `/api/v1/products/{id}` | CASHIER | Ver producto por ID (con variantes) |
| `PUT` | `/api/v1/products/{id}` | MANAGER | Actualizar producto |
| `DELETE` | `/api/v1/products/{id}` | MANAGER | Soft delete de producto |
| `POST` | `/api/v1/products/{id}/restore` | OWNER | Restaurar producto borrado |
| `PATCH` | `/api/v1/products/{id}/availability` | MANAGER | Marcar agotado / disponible |
| `PATCH` | `/api/v1/products/reorder` | MANAGER | Reordenar productos |
| `GET` | `/api/v1/products/{id}/variants` | CASHIER | Listar variantes de producto |
| `POST` | `/api/v1/products/{id}/variants` | MANAGER | Agregar variante |
| `PUT` | `/api/v1/products/{id}/variants/{vid}` | MANAGER | Actualizar variante |
| `DELETE` | `/api/v1/products/{id}/variants/{vid}` | MANAGER | Borrar variante |
| `GET` | `/api/v1/menu` | CASHIER | Menu completo organizado por categoria (para POS) |

**Total: 20 endpoints**

---

## Resumen de Tests Esperados

| Sprint | Tipo | Tests Nuevos | Tests Acumulados |
|--------|------|-------------|------------------|
| Sprint 1 | Unit + Integration | ~58 | ~398 |
| Sprint 2 | Unit + Integration | ~70 | ~468 |
| Sprint 3 | Unit + Integration | ~86 | ~554 |
| Sprint 4 | Unit + Integration | ~36 | ~590 |
| Sprint 5 | Unit + Integration | ~36 | ~626 |
| Sprint 6 | Unit + Integration | ~25 | ~651 |

*Los conteos son estimados. La meta es llegar a Phase 0.4 completo con >630 tests pasando.*

---

## Notas de Implementacion para el Desarrollador

### Extraccion de tenant_id del JWT

```java
// En cualquier controller, usar el principal inyectado por Spring Security
@GetMapping
public ResponseEntity<?> listProducts(
        @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
        Pageable pageable) {
    UUID tenantId = principal.tenantId();  // nunca de params
    // ...
}
```

### Patron de soft delete en repositorios

```java
// CORRECTO: el metodo de repositorio debe filtrar deleted_at IS NULL
@Query("SELECT c FROM Category c WHERE c.tenantId = :tenantId AND c.deletedAt IS NULL")
Page<Category> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

// INCORRECTO: nunca usar findAll() sin filtro de tenant ni de deleted_at
```

### Validacion de tenant ownership en servicios

```java
// CORRECTO: siempre buscar por id Y tenantId juntos
Category category = categoryRepository.findByIdAndTenantId(id, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Category", id));

// INCORRECTO: buscar solo por id y luego verificar tenant
Category category = categoryRepository.findById(id).orElseThrow(...);
if (!category.getTenantId().equals(tenantId)) throw new ForbiddenException();
// El incorrecto revela existencia del recurso via el timing de la respuesta
```

### Convencion de nombres de clases de test

- `*Test.java` — tests unitarios con mocks (no necesitan Spring context)
- `*E2ETest.java` — tests en modulo app con Testcontainers (extienden `BaseE2ETest`)
- `*RepositoryTest.java` — tests con `@DataJpaTest` + Testcontainers (contexto JPA reducido)
