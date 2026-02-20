# Phase 1.2: Modificadores y Combos — Product Customization Roadmap

> **Version:** 1.0.0
> **Fecha:** 2026-02-19
> **Status:** PENDIENTE - Sprint 0/4
> **Modulo Maven:** `quickstack-product` (expandir modulo existente)
> **Parte de:** Phase 1: Core POS - Ventas Completas

---

## Resumen Ejecutivo

Este documento define el plan de implementacion de la **segunda sub-fase de Phase 1**: el sistema de modificadores y combos para personalizacion avanzada de productos en QuickStack POS.

**Importante:** Esta sub-fase extiende el modulo `quickstack-product` con modificadores (personalizaciones de producto tipo "sin cebolla", "extra queso") y combos (paquetes con precio especial). El catalogo base de Phase 1.1 debe estar completado.

| Aspecto | Detalle |
|---------|---------|
| **Timeline** | 4 sprints (~2 semanas) |
| **Tareas Backend** | 16 tareas (~45 horas) |
| **Tareas QA** | 6 tareas (~18 horas) |
| **Tareas Frontend** | 0 (fuera de scope esta phase) |
| **Checkpoints de Seguridad** | 1 (Post-Sprint 3) |
| **Endpoints nuevos** | 12 endpoints REST |
| **Migracion Flyway** | Reutilizar V3 ya existente (esquema ya definido) |

---

## Decisiones de Diseno Confirmadas

### Modulo Maven

| Decision | Valor | Justificacion |
|----------|-------|---------------|
| Modulo | `quickstack-product` (expandir) | Reutilizar infraestructura de catalogo existente |
| Dependencias del modulo | `quickstack-common`, `quickstack-tenant` | Ya establecidas en Phase 1.1 |
| Controladores | En `quickstack-app` | Patron consistente con Phase 1.1 |

### Base de Datos

El esquema de base de datos para este modulo **ya fue definido en V3__create_catalog_module.sql**. No se requiere nueva migracion Flyway. Las tablas relevantes para esta phase son:

| Tabla | Descripcion |
|-------|-------------|
| `modifier_groups` | Grupos de modificadores (Extras, Sin ingredientes), soft delete |
| `modifiers` | Opciones individuales dentro de un grupo, soft delete |
| `combos` | Paquetes de productos con precio especial, soft delete |
| `combo_items` | Items incluidos en un combo, cascade delete con combo |

### Modifier Groups: Semantica de Seleccion

| Campo | Semantica |
|-------|-----------|
| `min_selections` | Minimo de modifiers que el usuario DEBE seleccionar (0 = opcional) |
| `max_selections` | Maximo de modifiers permitidos (NULL = ilimitado) |
| `is_required` | Si true, `min_selections` debe ser >= 1 (validado por constraint) |

**Ejemplo:**
- Grupo "Extras" (is_required=false, min=0, max=NULL): Cliente puede agregar 0 o mas extras
- Grupo "Tamano de bebida" (is_required=true, min=1, max=1): Cliente DEBE seleccionar exactamente 1

### Combos: Pricing Logic

| Campo | Semantica |
|-------|-----------|
| `combo_price` | Precio fijo del combo completo |
| `is_fixed_price` | Si true, `combo_price` es el total. Si false, `combo_price` es descuento aplicado a la suma de items individuales |

**Ejemplo:**
- Combo "2 Tacos + Refresco" (is_fixed_price=true, combo_price=75.00): Cliente paga $75 sin importar los productos seleccionados
- Combo "Desayuno" (is_fixed_price=false, combo_price=-15.00): Cliente paga (suma de items - $15)

### Multi-tenancy

Todas las operaciones filtran por `tenant_id` extraido del JWT. Ningun endpoint acepta `tenant_id` como parametro de path o body.

### Roles y Permisos

| Operacion | OWNER | MANAGER | CASHIER |
|-----------|-------|---------|---------|
| Crear modifier group/combo | SI | SI | NO |
| Editar modifier group/combo | SI | SI | NO |
| Soft delete | SI | SI | NO |
| Restaurar (undelete) | SI | NO | NO |
| Ver inactivos (`includeInactive=true`) | SI | SI | NO |
| Listar menu activo con modifiers | SI | SI | SI |

### Manejo de Errores

| Situacion | HTTP | Codigo |
|-----------|------|--------|
| Modifier group no encontrado | 404 | `MODIFIER_GROUP_NOT_FOUND` |
| Combo no encontrado | 404 | `COMBO_NOT_FOUND` |
| Producto no valido para combo | 409 | `INVALID_COMBO_ITEM` |
| min_selections > max_selections | 400 | `INVALID_SELECTION_RANGE` |
| is_required=true pero min_selections=0 | 400 | `INVALID_REQUIRED_CONFIG` |
| Acceso a recurso de otro tenant | 404 (no 403) | `MODIFIER_GROUP_NOT_FOUND` / `COMBO_NOT_FOUND` |

---

## Deuda Tecnica Aceptada

| Item | Justificacion | Phase sugerida |
|------|---------------|----------------|
| Imagenes de modifiers/combos (upload) | Requiere storage externo (S3/GCS) | **Phase 1.3** o posterior |
| Combos con variantes seleccionables | Requiere UI compleja de configuracion | **Phase 2+** |
| Modifier groups compartidos entre productos | Actualmente 1 modifier group pertenece a 1 producto | **Phase 2+** |
| Disponibilidad horaria de combos | Logica timezone compleja | **Phase 2+** |

---

## Arquitectura de Componentes

```
quickstack-product/
├── src/main/java/com/quickstack/product/
│   ├── dto/
│   │   ├── request/
│   │   │   ├── ModifierGroupCreateRequest.java       <- NUEVO
│   │   │   ├── ModifierGroupUpdateRequest.java       <- NUEVO
│   │   │   ├── ModifierCreateRequest.java            <- NUEVO
│   │   │   ├── ModifierUpdateRequest.java            <- NUEVO
│   │   │   ├── ComboCreateRequest.java               <- NUEVO
│   │   │   ├── ComboUpdateRequest.java               <- NUEVO
│   │   │   └── ComboItemRequest.java                 <- NUEVO
│   │   └── response/
│   │       ├── ModifierGroupResponse.java            <- NUEVO
│   │       ├── ModifierResponse.java                 <- NUEVO
│   │       ├── ComboResponse.java                    <- NUEVO
│   │       └── MenuResponse.java                     <- MODIFICAR: agregar modifiers
│   ├── entity/
│   │   ├── ModifierGroup.java                        <- NUEVO
│   │   ├── Modifier.java                             <- NUEVO
│   │   ├── Combo.java                                <- NUEVO
│   │   └── ComboItem.java                            <- NUEVO
│   ├── repository/
│   │   ├── ModifierGroupRepository.java              <- NUEVO
│   │   ├── ModifierRepository.java                   <- NUEVO
│   │   └── ComboRepository.java                      <- NUEVO
│   └── service/
│       ├── ModifierGroupService.java                 <- NUEVO
│       ├── ComboService.java                         <- NUEVO
│       └── MenuService.java                          <- MODIFICAR: incluir modifiers/combos

quickstack-app/
├── src/main/java/com/quickstack/app/
│   └── controller/
│       ├── ModifierGroupController.java              <- NUEVO
│       └── ComboController.java                      <- NUEVO
```

---

## Sprint 1: Entidades y Repositorios de Modifiers

**Duracion:** 2 dias | **Objetivo:** Infraestructura de modifier groups y modifiers completa

### [BACKEND] Tarea 1.1: Entidad ModifierGroup

**Prioridad:** Alta | **Dependencias:** Ninguna

Entidad JPA mapeada a la tabla `modifier_groups` ya existente en V3.

**Criterios de Aceptacion:**
- [ ] `ModifierGroup.java` con todos los campos: `id`, `tenantId`, `productId`, `name`, `description`, `minSelections`, `maxSelections`, `isRequired`, `sortOrder`, `createdAt`, `updatedAt`, `deletedAt`
- [ ] Anotaciones JPA: `@Entity`, `@Table(name = "modifier_groups")`, `@Column` con nombres exactos de BD
- [ ] `@CreationTimestamp` / `@UpdateTimestamp` en campos de audit
- [ ] `@OneToMany` a `Modifier` (fetch LAZY)
- [ ] Metodo `isDeleted()` que retorna `deletedAt != null`
- [ ] Validacion: si `isRequired == true`, entonces `minSelections >= 1` (validado en service, no en DB constraint ya existente)
- [ ] Tests unitarios: 6 tests (constructor, isDeleted, validacion de required, relationship con modifiers)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/entity/ModifierGroup.java`
- `quickstack-product/src/test/java/com/quickstack/product/entity/ModifierGroupTest.java`

---

### [BACKEND] Tarea 1.2: Entidad Modifier

**Prioridad:** Alta | **Dependencias:** 1.1

Entidad JPA para opciones individuales dentro de un modifier group.

**Criterios de Aceptacion:**
- [ ] `Modifier.java` con todos los campos: `id`, `tenantId`, `modifierGroupId`, `name`, `priceAdjustment`, `isDefault`, `isActive`, `sortOrder`, `createdAt`, `updatedAt`, `deletedAt`
- [ ] Anotaciones JPA completas
- [ ] `priceAdjustment` puede ser positivo (extra queso +$10), negativo (descuento), o 0 (sin cambio de precio)
- [ ] `isDefault`: marcado como seleccionado por defecto en UI (solo 1 por grupo deberia ser true)
- [ ] Tests unitarios: 5 tests (constructor, isDeleted, price adjustment logic)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/entity/Modifier.java`
- `quickstack-product/src/test/java/com/quickstack/product/entity/ModifierTest.java`

---

### [BACKEND] Tarea 1.3: ModifierGroupRepository

**Prioridad:** Alta | **Dependencias:** 1.1

Repositorio JPA con queries tenant-safe.

**Criterios de Aceptacion:**
- [ ] `findAllByProductIdAndTenantId(UUID productId, UUID tenantId)` retorna solo no-borrados, ordenados por `sort_order`
- [ ] `findByIdAndTenantId(UUID id, UUID tenantId)` retorna `Optional<ModifierGroup>` — excluye borrados
- [ ] `existsByNameAndProductIdAndTenantId(String name, UUID productId, UUID tenantId)` para validacion de unicidad
- [ ] `existsByNameAndProductIdAndTenantIdAndIdNot(String name, UUID productId, UUID tenantId, UUID excludeId)` para update
- [ ] Tests de repositorio con `@DataJpaTest` + Testcontainers: 10 tests

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/repository/ModifierGroupRepository.java`
- `quickstack-product/src/test/java/com/quickstack/product/repository/ModifierGroupRepositoryTest.java`

---

### [BACKEND] Tarea 1.4: ModifierRepository

**Prioridad:** Alta | **Dependencias:** 1.2

Repositorio JPA para modifiers.

**Criterios de Aceptacion:**
- [ ] `findAllByModifierGroupIdAndTenantId(UUID modifierGroupId, UUID tenantId)` retorna solo activos y no-borrados, ordenados por `sort_order`
- [ ] `findByIdAndTenantId(UUID id, UUID tenantId)` retorna `Optional<Modifier>` excluye borrados
- [ ] `existsByNameAndModifierGroupIdAndTenantId(String name, UUID modifierGroupId, UUID tenantId)` para unicidad
- [ ] `countActiveByModifierGroupId(UUID modifierGroupId, UUID tenantId)` para validar que no se borre el ultimo modifier activo
- [ ] Tests de repositorio con `@DataJpaTest` + Testcontainers: 10 tests

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/repository/ModifierRepository.java`
- `quickstack-product/src/test/java/com/quickstack/product/repository/ModifierRepositoryTest.java`

---

## Sprint 2: Modifier Management CRUD

**Duracion:** 2 dias | **Objetivo:** CRUD completo de modifier groups y modifiers

### [BACKEND] Tarea 2.1: DTOs de Modifier Groups y Modifiers

**Prioridad:** Alta | **Dependencias:** 1.1, 1.2

Objetos de transferencia para requests y responses.

**Criterios de Aceptacion:**
- [ ] `ModifierGroupCreateRequest`: `productId` (NotNull UUID), `name` (NotBlank, max 100), `description` (nullable, max 500), `minSelections` (NotNull, min 0), `maxSelections` (nullable, >= minSelections), `isRequired` (NotNull), `sortOrder` (nullable, default 0)
- [ ] Validacion cross-field: si `isRequired == true`, entonces `minSelections >= 1`
- [ ] `ModifierGroupUpdateRequest`: mismos campos que Create, todos opcionales
- [ ] `ModifierCreateRequest`: `modifierGroupId` (NotNull UUID), `name` (NotBlank, max 100), `priceAdjustment` (NotNull, puede ser negativo), `isDefault` (boolean, default false), `sortOrder` (nullable)
- [ ] `ModifierUpdateRequest`: mismos campos que Create, todos opcionales
- [ ] `ModifierGroupResponse`: todos los campos de la entidad + lista `modifiers` (List<ModifierResponse>)
- [ ] `ModifierResponse`: todos los campos de Modifier
- [ ] Tests unitarios: 12 tests (Bean Validation, cross-field validation)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/dto/request/ModifierGroupCreateRequest.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/request/ModifierGroupUpdateRequest.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/request/ModifierCreateRequest.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/request/ModifierUpdateRequest.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/ModifierGroupResponse.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/ModifierResponse.java`
- `quickstack-product/src/test/java/com/quickstack/product/dto/ModifierDtoTest.java`

---

### [BACKEND] Tarea 2.2: ModifierGroupService

**Prioridad:** Alta | **Dependencias:** 1.3, 2.1

Logica de negocio para modifier groups.

**Criterios de Aceptacion:**
- [ ] `createModifierGroup(UUID tenantId, UUID userId, ModifierGroupCreateRequest)`: valida que `productId` pertenece al tenant, valida nombre unico por producto+tenant, persiste, retorna `ModifierGroupResponse`
- [ ] `updateModifierGroup(UUID tenantId, UUID userId, UUID modifierGroupId, ModifierGroupUpdateRequest)`: valida existencia, valida unicidad de nombre (excluyendo propio), actualiza `updatedAt`, retorna `ModifierGroupResponse`
- [ ] `deleteModifierGroup(UUID tenantId, UUID userId, UUID modifierGroupId)`: soft delete (setea `deletedAt`), retorna void. Los modifiers hijos quedan huerfanos pero con `deleted_at` tambien (cascade soft delete en service)
- [ ] `getModifierGroup(UUID tenantId, UUID modifierGroupId)`: retorna `ModifierGroupResponse` con lista de modifiers activos, lanza `ResourceNotFoundException` si no existe
- [ ] `listModifierGroupsByProduct(UUID tenantId, UUID productId)`: retorna lista de `ModifierGroupResponse` ordenada por `sort_order`
- [ ] Toda operacion de escritura es `@Transactional`
- [ ] Tests unitarios con mocks: 18 tests

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/service/ModifierGroupService.java`
- `quickstack-product/src/test/java/com/quickstack/product/service/ModifierGroupServiceTest.java`

---

### [BACKEND] Tarea 2.3: ModifierService (CRUD de Modifiers como sub-recurso)

**Prioridad:** Alta | **Dependencias:** 1.4, 2.1

Logica de negocio para modifiers individuales.

**Criterios de Aceptacion:**
- [ ] `addModifier(UUID tenantId, UUID userId, ModifierCreateRequest)`: valida que `modifierGroupId` existe y pertenece al tenant, valida nombre unico dentro del grupo, persiste, retorna `ModifierResponse`
- [ ] Si `isDefault == true`, resetea `isDefault = false` en todos los otros modifiers del mismo grupo (en misma transaccion)
- [ ] `updateModifier(UUID tenantId, UUID userId, UUID modifierId, ModifierUpdateRequest)`: valida existencia, maneja cambio de `isDefault`, retorna `ModifierResponse`
- [ ] `deleteModifier(UUID tenantId, UUID userId, UUID modifierId)`: soft delete, no permite borrar si es el ultimo modifier activo del grupo (lanza `BusinessRuleException`)
- [ ] `listModifiers(UUID tenantId, UUID modifierGroupId)`: retorna lista de `ModifierResponse` ordenada por `sort_order`
- [ ] Tests unitarios con mocks: 16 tests

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/service/ModifierService.java`
- `quickstack-product/src/test/java/com/quickstack/product/service/ModifierServiceTest.java`

---

### [BACKEND] Tarea 2.4: ModifierGroupController

**Prioridad:** Alta | **Dependencias:** 2.2, 2.3

Controller REST para modifier groups y modifiers.

**Criterios de Aceptacion:**
- [ ] `GET /api/v1/products/{productId}/modifier-groups` — requiere JWT. Retorna lista de `ModifierGroupResponse` (con modifiers incluidos) HTTP 200
- [ ] `POST /api/v1/products/{productId}/modifier-groups` — OWNER/MANAGER. Retorna `ModifierGroupResponse` HTTP 201 + `Location` header
- [ ] `GET /api/v1/modifier-groups/{id}` — requiere JWT. Retorna `ModifierGroupResponse` HTTP 200
- [ ] `PUT /api/v1/modifier-groups/{id}` — OWNER/MANAGER. Retorna `ModifierGroupResponse` HTTP 200
- [ ] `DELETE /api/v1/modifier-groups/{id}` — OWNER/MANAGER. HTTP 204
- [ ] `GET /api/v1/modifier-groups/{groupId}/modifiers` — requiere JWT. Retorna lista de `ModifierResponse` HTTP 200
- [ ] `POST /api/v1/modifier-groups/{groupId}/modifiers` — OWNER/MANAGER. Retorna `ModifierResponse` HTTP 201
- [ ] `PUT /api/v1/modifiers/{id}` — OWNER/MANAGER. Retorna `ModifierResponse` HTTP 200
- [ ] `DELETE /api/v1/modifiers/{id}` — OWNER/MANAGER. HTTP 204
- [ ] Tests unitarios con `@WebMvcTest`: 18 tests

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/controller/ModifierGroupController.java`
- `quickstack-app/src/test/java/com/quickstack/app/controller/ModifierGroupControllerTest.java`

---

### [QA] Tarea 2.5: Tests de Integracion de Modifiers

**Prioridad:** Alta | **Dependencias:** 2.4

Tests end-to-end con base de datos real.

**Criterios de Aceptacion:**
- [ ] Extienden `BaseIntegrationTest`
- [ ] Setup crea tenant, productos de prueba y JWTs con los 3 roles
- [ ] `POST /api/v1/products/{productId}/modifier-groups` con OWNER: retorna 201
- [ ] `POST` con `isRequired=true` y `minSelections=0`: retorna 400 (validacion cross-field)
- [ ] `POST` con nombre duplicado en mismo producto: retorna 409
- [ ] `POST /api/v1/modifier-groups/{groupId}/modifiers` con `isDefault=true`: resetea otros modifiers del grupo
- [ ] `DELETE /api/v1/modifiers/{id}` del ultimo modifier activo: retorna 409
- [ ] Cross-tenant: modifier group de tenant A con JWT de tenant B retorna 404
- [ ] 12 tests de integracion pasando

**Archivos:**
- `quickstack-app/src/test/java/com/quickstack/app/catalog/ModifierIntegrationTest.java`

---

## Sprint 3: Combos Management

**Duracion:** 2.5 dias | **Objetivo:** CRUD completo de combos con items

### [BACKEND] Tarea 3.1: Entidades Combo y ComboItem

**Prioridad:** Alta | **Dependencias:** Ninguna (ya existe tabla products)

Entidades JPA para combos.

**Criterios de Aceptacion:**
- [ ] `Combo.java` con todos los campos: `id`, `tenantId`, `categoryId`, `name`, `description`, `imageUrl`, `comboPrice`, `isFixedPrice`, `isActive`, `sortOrder`, `createdAt`, `updatedAt`, `deletedAt`, `deletedBy`
- [ ] `@OneToMany` a `ComboItem` (fetch LAZY, cascade ALL, orphanRemoval true)
- [ ] Metodo `getEffectivePrice(List<Product> selectedItems)`: si `isFixedPrice`, retorna `comboPrice`; si no, retorna suma de precios de items + `comboPrice` (que es descuento negativo)
- [ ] `ComboItem.java` con campos: `id`, `tenantId`, `comboId`, `productId`, `quantity`, `sortOrder`
- [ ] `ComboItem` NO tiene `deletedAt` (hard delete cuando se borra de combo)
- [ ] Tests unitarios: 8 tests (constructor, getEffectivePrice con ambos modos, relationship con items)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/entity/Combo.java`
- `quickstack-product/src/main/java/com/quickstack/product/entity/ComboItem.java`
- `quickstack-product/src/test/java/com/quickstack/product/entity/ComboTest.java`

---

### [BACKEND] Tarea 3.2: ComboRepository

**Prioridad:** Alta | **Dependencias:** 3.1

Repositorio JPA para combos.

**Criterios de Aceptacion:**
- [ ] `findAllByTenantId(UUID tenantId, Pageable pageable)` retorna solo activos y no-borrados
- [ ] `findByIdAndTenantId(UUID id, UUID tenantId)` retorna `Optional<Combo>` con `@EntityGraph` que incluye `comboItems` (evitar N+1)
- [ ] `existsByNameAndTenantId(String name, UUID tenantId)` para unicidad
- [ ] `existsByNameAndTenantIdAndIdNot(String name, UUID tenantId, UUID excludeId)` para update
- [ ] Tests de repositorio con `@DataJpaTest` + Testcontainers: 10 tests

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/repository/ComboRepository.java`
- `quickstack-product/src/test/java/com/quickstack/product/repository/ComboRepositoryTest.java`

---

### [BACKEND] Tarea 3.3: DTOs de Combo

**Prioridad:** Alta | **Dependencias:** 3.1

Objetos de transferencia para combos.

**Criterios de Aceptacion:**
- [ ] `ComboItemRequest`: `productId` (NotNull UUID), `quantity` (NotNull, min 1), `sortOrder` (nullable)
- [ ] `ComboCreateRequest`: `name` (NotBlank, max 255), `description` (nullable, max 1000), `categoryId` (nullable UUID), `imageUrl` (nullable URL, max 500), `comboPrice` (NotNull, min 0.01 si isFixedPrice, puede ser negativo si no), `isFixedPrice` (NotNull), `items` (NotEmpty lista de ComboItemRequest, min 2 items), `sortOrder` (nullable)
- [ ] Validacion: debe haber al menos 2 items en `items`
- [ ] `ComboUpdateRequest`: mismos campos que Create, todos opcionales
- [ ] `ComboResponse`: todos los campos de Combo + `items` (lista de objetos con `productId`, `productName`, `quantity`)
- [ ] Tests unitarios: 10 tests (Bean Validation, combo con 1 solo item debe fallar)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/dto/request/ComboCreateRequest.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/request/ComboUpdateRequest.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/request/ComboItemRequest.java`
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/ComboResponse.java`
- `quickstack-product/src/test/java/com/quickstack/product/dto/ComboDtoTest.java`

---

### [BACKEND] Tarea 3.4: ComboService

**Prioridad:** Alta | **Dependencias:** 3.2, 3.3

Logica de negocio para combos.

**Criterios de Aceptacion:**
- [ ] `createCombo(UUID tenantId, UUID userId, ComboCreateRequest)`:
  - Valida que todos los `productId` en `items` pertenecen al tenant (lanza `ResourceNotFoundException` si alguno no existe)
  - Valida nombre unico por tenant
  - Valida que hay al menos 2 items
  - Persiste combo y combo_items en transaccion unica
  - Retorna `ComboResponse`
- [ ] `updateCombo(UUID tenantId, UUID userId, UUID comboId, ComboUpdateRequest)`:
  - Si `items` esta presente, reemplaza completamente los items existentes (orphan removal automatico)
  - Re-valida productos
  - Retorna `ComboResponse`
- [ ] `deleteCombo(UUID tenantId, UUID userId, UUID comboId)`: soft delete, retorna void
- [ ] `getCombo(UUID tenantId, UUID comboId)`: retorna `ComboResponse` con items, lanza `ResourceNotFoundException` si no existe
- [ ] `listCombos(UUID tenantId, Pageable pageable)`: retorna `Page<ComboResponse>`
- [ ] Tests unitarios con mocks: 18 tests

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/service/ComboService.java`
- `quickstack-product/src/test/java/com/quickstack/product/service/ComboServiceTest.java`

---

### [BACKEND] Tarea 3.5: ComboController

**Prioridad:** Alta | **Dependencias:** 3.4

Controller REST para combos.

**Criterios de Aceptacion:**
- [ ] `GET /api/v1/combos` — requiere JWT. Retorna `Page<ComboResponse>` HTTP 200
- [ ] `POST /api/v1/combos` — OWNER/MANAGER. Retorna `ComboResponse` HTTP 201 + `Location` header
- [ ] `GET /api/v1/combos/{id}` — requiere JWT. Retorna `ComboResponse` HTTP 200
- [ ] `PUT /api/v1/combos/{id}` — OWNER/MANAGER. Retorna `ComboResponse` HTTP 200
- [ ] `DELETE /api/v1/combos/{id}` — OWNER/MANAGER. HTTP 204
- [ ] Tests unitarios con `@WebMvcTest`: 12 tests

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/controller/ComboController.java`
- `quickstack-app/src/test/java/com/quickstack/app/controller/ComboControllerTest.java`

---

### [QA] Tarea 3.6: Tests de Integracion de Combos

**Prioridad:** Alta | **Dependencias:** 3.5

Tests end-to-end con base de datos real.

**Criterios de Aceptacion:**
- [ ] Extienden `BaseIntegrationTest`
- [ ] Setup crea tenant, productos de prueba y JWTs
- [ ] `POST /api/v1/combos` con 2+ productos validos: retorna 201
- [ ] `POST` con solo 1 producto en items: retorna 400
- [ ] `POST` con `productId` de otro tenant: retorna 404
- [ ] `PUT /api/v1/combos/{id}` reemplazando items: elimina items antiguos de BD (orphan removal)
- [ ] Cross-tenant: combo de tenant A con JWT de tenant B retorna 404
- [ ] 10 tests de integracion pasando

**Archivos:**
- `quickstack-app/src/test/java/com/quickstack/app/catalog/ComboIntegrationTest.java`

---

### Checkpoint de Seguridad Post-Sprint 3

**Validaciones requeridas antes de continuar:**

- [ ] Verificar que modifier groups solo se pueden crear sobre productos del mismo tenant (query inspection)
- [ ] Verificar que combos solo pueden incluir productos del mismo tenant
- [ ] Test de IDOR: usuario de tenant B intenta GET/PUT/DELETE sobre modifier group/combo de tenant A — debe recibir 404
- [ ] Verificar que cascade soft delete de modifier group → modifiers funciona correctamente
- [ ] Verificar que orphan removal de combo_items funciona al actualizar combo
- [ ] Confirmar que logs registran todas las operaciones de escritura con tenantId y userId

---

## Sprint 4: Integracion con Menu Endpoint

**Duracion:** 1.5 dias | **Objetivo:** Actualizar endpoint `/api/v1/menu` para incluir modifiers y combos

### [BACKEND] Tarea 4.1: Actualizar MenuResponse DTOs

**Prioridad:** Alta | **Dependencias:** Sprint 1, Sprint 3

Expandir DTOs de menu para incluir modifiers y combos.

**Criterios de Aceptacion:**
- [ ] `MenuProductItem` (ya existente): agregar campo `modifierGroups` (lista de `MenuModifierGroupItem`)
- [ ] `MenuModifierGroupItem`: `id`, `name`, `minSelections`, `maxSelections`, `isRequired`, `modifiers` (lista de `MenuModifierItem`)
- [ ] `MenuModifierItem`: `id`, `name`, `priceAdjustment`, `isDefault`, `sortOrder`
- [ ] `MenuCategoryItem` (ya existente): agregar campo `combos` (lista de `MenuComboItem`)
- [ ] `MenuComboItem`: `id`, `name`, `comboPrice`, `isFixedPrice`, `imageUrl`, `items` (lista con `productId`, `productName`, `quantity`)
- [ ] Tests unitarios: 6 tests (construccion de DTOs con modifiers y combos)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/MenuResponse.java` (modificar)
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/MenuProductItem.java` (modificar)
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/MenuCategoryItem.java` (modificar)
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/MenuModifierGroupItem.java` (NUEVO)
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/MenuModifierItem.java` (NUEVO)
- `quickstack-product/src/main/java/com/quickstack/product/dto/response/MenuComboItem.java` (NUEVO)
- `quickstack-product/src/test/java/com/quickstack/product/dto/MenuResponseTest.java` (expandir)

---

### [BACKEND] Tarea 4.2: Actualizar MenuService

**Prioridad:** Alta | **Dependencias:** 4.1, 2.2, 3.4

Modificar servicio existente para incluir modifiers y combos.

**Criterios de Aceptacion:**
- [ ] `getMenu(UUID tenantId)`: retorna `MenuResponse` completo con modifiers y combos
- [ ] Implementado con queries optimizadas (maximo 3 queries: categorias+productos, modifier groups, combos)
- [ ] Modifiers solo se incluyen para productos activos y disponibles
- [ ] Combos solo se incluyen si estan activos y todos sus productos estan activos
- [ ] Verificar con `spring.jpa.show-sql=true` que no hay N+1 queries
- [ ] Tests unitarios: 8 tests (menu con modifiers, menu con combos, combo con producto inactivo excluido)

**Archivos:**
- `quickstack-product/src/main/java/com/quickstack/product/service/MenuService.java` (modificar)
- `quickstack-product/src/test/java/com/quickstack/product/service/MenuServiceTest.java` (expandir)

---

### [QA] Tarea 4.3: Tests de Integracion de Menu Completo

**Prioridad:** Alta | **Dependencias:** 4.2

Tests end-to-end del menu con modifiers y combos.

**Criterios de Aceptacion:**
- [ ] Setup: crear tenant con 3 categorias, 10 productos (algunos con modifier groups), 2 combos
- [ ] `GET /api/v1/menu`: retorna productos con sus modifier groups incluidos
- [ ] Productos sin modifier groups retornan `modifierGroups: []`
- [ ] Combos aparecen dentro de sus categorias
- [ ] Combos con productos inactivos NO aparecen en el menu
- [ ] Verificar que se ejecutan maximo 3 queries SQL (verificacion con logs)
- [ ] 8 tests de integracion pasando

**Archivos:**
- `quickstack-app/src/test/java/com/quickstack/app/catalog/MenuIntegrationTest.java` (expandir)

---

## Endpoints Finales

| Metodo | Endpoint | Rol Minimo | Descripcion |
|--------|----------|-----------|-------------|
| `GET` | `/api/v1/products/{productId}/modifier-groups` | CASHIER | Listar modifier groups de un producto |
| `POST` | `/api/v1/products/{productId}/modifier-groups` | MANAGER | Crear modifier group |
| `GET` | `/api/v1/modifier-groups/{id}` | CASHIER | Ver modifier group por ID (con modifiers) |
| `PUT` | `/api/v1/modifier-groups/{id}` | MANAGER | Actualizar modifier group |
| `DELETE` | `/api/v1/modifier-groups/{id}` | MANAGER | Soft delete de modifier group |
| `GET` | `/api/v1/modifier-groups/{groupId}/modifiers` | CASHIER | Listar modifiers de un grupo |
| `POST` | `/api/v1/modifier-groups/{groupId}/modifiers` | MANAGER | Agregar modifier |
| `PUT` | `/api/v1/modifiers/{id}` | MANAGER | Actualizar modifier |
| `DELETE` | `/api/v1/modifiers/{id}` | MANAGER | Borrar modifier |
| `GET` | `/api/v1/combos` | CASHIER | Listar combos (paginado) |
| `POST` | `/api/v1/combos` | MANAGER | Crear combo |
| `GET` | `/api/v1/combos/{id}` | CASHIER | Ver combo por ID (con items) |
| `PUT` | `/api/v1/combos/{id}` | MANAGER | Actualizar combo |
| `DELETE` | `/api/v1/combos/{id}` | MANAGER | Soft delete de combo |
| `GET` | `/api/v1/menu` | CASHIER | Menu completo (ACTUALIZADO: incluye modifiers y combos) |

**Total: 12 endpoints nuevos + 1 endpoint modificado**

---

## Resumen de Tests Esperados

| Sprint | Tipo | Tests Nuevos | Tests Acumulados |
|--------|------|-------------|------------------|
| Sprint 1 | Unit + Integration | ~41 | ~692 |
| Sprint 2 | Unit + Integration | ~58 | ~750 |
| Sprint 3 | Unit + Integration | ~58 | ~808 |
| Sprint 4 | Unit + Integration | ~22 | ~830 |

*Los conteos son estimados. La meta es llegar a Phase 1.2 completo con ~830 tests pasando.*

---

## Notas de Implementacion para el Desarrollador

### Validacion cross-field en DTOs

```java
// En ModifierGroupCreateRequest.java
@AssertTrue(message = "Si isRequired es true, minSelections debe ser >= 1")
public boolean isValidRequiredConfig() {
    return !isRequired || minSelections >= 1;
}
```

### Cascade soft delete de modifier group a modifiers

```java
// En ModifierGroupService.java
@Transactional
public void deleteModifierGroup(UUID tenantId, UUID userId, UUID modifierGroupId) {
    ModifierGroup group = modifierGroupRepository.findByIdAndTenantId(modifierGroupId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("ModifierGroup", modifierGroupId));

    // Soft delete del grupo
    group.setDeletedAt(Instant.now());
    group.setDeletedBy(userId);

    // Soft delete de todos los modifiers hijos
    modifierRepository.findAllByModifierGroupIdAndTenantId(modifierGroupId, tenantId)
        .forEach(modifier -> {
            modifier.setDeletedAt(Instant.now());
        });

    modifierGroupRepository.save(group);
}
```

### Orphan removal de combo items

```java
// En Combo.java
@OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ComboItem> items = new ArrayList<>();

// En ComboService.java update
@Transactional
public ComboResponse updateCombo(UUID tenantId, UUID userId, UUID comboId, ComboUpdateRequest request) {
    Combo combo = comboRepository.findByIdAndTenantId(comboId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Combo", comboId));

    if (request.getItems() != null) {
        // Limpiar items existentes (orphan removal los borra automaticamente)
        combo.getItems().clear();

        // Agregar nuevos items
        request.getItems().forEach(itemReq -> {
            Product product = productRepository.findByIdAndTenantId(itemReq.getProductId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.getProductId()));

            ComboItem item = new ComboItem();
            item.setTenantId(tenantId);
            item.setCombo(combo);
            item.setProductId(product.getId());
            item.setQuantity(itemReq.getQuantity());
            item.setSortOrder(itemReq.getSortOrder());

            combo.getItems().add(item);
        });
    }

    comboRepository.save(combo);
    return mapToResponse(combo);
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
- [ ] Commit con mensaje descriptivo: `feat(product): Sprint X complete - [descripcion]`
