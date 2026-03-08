# Phase 3: Owner Intelligence

> **Estado:** ⏳ EN PROGRESO — Sprint 1 completado (2026-03-08)
> **Inicio real:** 2026-03-08
> **Objetivo:** Darle al OWNER visibilidad financiera real por primera vez — saber cuánto le cuesta producir cada platillo, registrar sus gastos, conocer qué necesita comprar y ver un P&L real de su negocio.
> **Desarrollador:** 1 persona
> **Duración estimada:** 7 sprints / ~7 semanas

---

## Contexto y Motivación

El OWNER de un restaurante pequeño mexicano hoy opera completamente ciego. Después de Phase 2, ya puede registrar ventas (CASHIER completa cobros en `/cashier/pos`) y ver el reporte del día. Pero sigue sin respuesta a las preguntas que más importan:

- **"¿Cuánto me cuesta hacer una torta ahachada?"** — No sabe el costo de producción de ningún platillo.
- **"¿Cuánto gasté esta semana en insumos, renta y servicios?"** — No registra nada, lo hace de memoria o en papel.
- **"¿Qué necesito comprar para el lunes?"** — Lo calcula a ojo sin datos históricos.
- **"¿Estoy ganando o perdiendo dinero este mes?"** — No tiene forma de saberlo.

Phase 3 cierra esa brecha. Es la fase que convierte a QuickStack de "una caja registradora digital" en "el sistema financiero del restaurante".

### Entregables de Phase 3

| # | Entregable | Descripción |
|---|-----------|-------------|
| 1 | **Inventario por Receta** | OWNER define los ingredientes de cada platillo. El sistema los descuenta automáticamente al cobrar. |
| 2 | **Registro de Gastos** | Formulario rápido para que el OWNER registre gastos del negocio desde el celular. |
| 3 | **Lista de Compras Auto-generada** | Algoritmo que identifica ingredientes bajo el mínimo y sugiere cantidades a comprar basado en consumo histórico. |
| 4 | **Reporte P&L** | Ventas − COGS − Gastos = Margen bruto. Primera vez que el OWNER ve esto en su vida. |

---

## Decisiones de Arquitectura (ADRs Phase 3)

### ADR-004: Nuevo módulo `quickstack-inventory`

**Decisión:** Crear el módulo Maven `quickstack-inventory` para contener `Ingredient`, `Recipe`, `RecipeItem`, `Expense` e `InventoryMovement`.

**Justificación — Regla de 3 condiciones:**

| Condición | Evaluación |
|-----------|-----------|
| **Actores distintos** | El OWNER gestiona ingredientes y recetas manualmente. El sistema (`PaymentService`) auto-descuenta al cobrar. Son dos actores con motivaciones opuestas. |
| **Ciclos de cambio distintos** | Las recetas cambian cuando el OWNER reformula un platillo (infrecuente). Los movimientos de inventario cambian con cada pago (continuo). |
| **Políticas de datos distintas** | Los ingredientes son soft-delete. Los movimientos de inventario son nunca-borrar (igual que las órdenes — son el audit trail financiero). |

**Módulos que extiende vs. nuevo:**
- `quickstack-inventory` — nuevo módulo. Depende de `quickstack-common`.
- `quickstack-pos` — se extiende `PaymentService` para llamar al módulo de inventario al completar un pago.
- `quickstack-pos` — se extiende `ReportController` para el endpoint P&L.

**Árbol de dependencias resultante:**
```
quickstack-app
├── quickstack-inventory   (nuevo)
│   └── quickstack-common
├── quickstack-pos
│   ├── quickstack-product
│   ├── quickstack-inventory  (nueva dependencia — auto-descuento)
│   └── quickstack-common
└── ... (resto sin cambios)
```

### ADR-005: Auto-deducción de inventario via ApplicationEvent

**Decisión:** Usar el mecanismo de eventos de Spring (`ApplicationEventPublisher` / `@EventListener`) para desacoplar `PaymentService` del módulo de inventario.

**Justificación:** `PaymentService` en `quickstack-pos` ya maneja una transacción JPA compleja (estado de orden, historial de estado, pago). Inyectar directamente `InventoryService` crea acoplamiento fuerte entre módulos y riesgo de transacciones anidadas. El evento se publica después de que la transacción de pago se confirma (`@TransactionalEventListener(phase = AFTER_COMMIT)`), garantizando que el inventario nunca se descuenta si el pago falla.

**Implicación:** Si el descuento de inventario falla (ingrediente no tiene receta, error de DB), el pago ya está confirmado — no se revierte. Se loguea como `[INVENTORY] ACTION=DEDUCTION_FAILED`. Para el MVP esto es aceptable: el OWNER verá la discrepancia en el reporte y puede hacer un ajuste manual. Se documenta como deuda técnica para Phase 4 (compensación asíncrona).

### ADR-006: Gastos en `quickstack-inventory`, no en `quickstack-pos`

**Decisión:** La entidad `Expense` vive en `quickstack-inventory`.

**Justificación:** El actor que registra gastos es el OWNER — el mismo que gestiona ingredientes y recetas. El ciclo de cambio de gastos (manual, infrecuente) es igual al de ingredientes. El COGS para el P&L se calcula cruzando `InventoryMovement` + `Expense` — tenerlos en el mismo módulo simplifica esa query. Meterlos en `quickstack-pos` mezclaría la lógica transaccional del cajero con la gestión financiera del dueño.

### ADR-007: COGS calculado a precio de costo al momento del movimiento

**Decisión:** `InventoryMovement` guarda `unit_cost` en el momento del descuento. No se recalcula retrospectivamente si el OWNER cambia el costo del ingrediente después.

**Justificación:** Es el estándar contable correcto (costo histórico). Si el OWNER actualiza el costo del aguacate de $25 a $30/kg, los pedidos pasados mantienen el costo $25 que era correcto cuando se hicieron. El P&L refleja la realidad histórica, no una revisión ficticia.

---

## Mapa de Dependencias entre Sprints

```
Sprint 1 (quickstack-inventory: entidades + migraciones)
    └── Sprint 2 (CRUD Ingredientes API)
            └── Sprint 3 (Recetas API)
                    └── Sprint 4 (Auto-deducción al pagar)
                            └── Sprint 6 (Lista de Compras API)
                            └── Sprint 7 (Reporte P&L)

Sprint 5 (Gastos API)         — independiente de Sprint 3/4, depende de Sprint 1
    └── Sprint 7 (Reporte P&L)

Sprint 2 → Sprint 8 (Frontend Inventario: ingredientes + recetas)
Sprint 5 → Sprint 9 (Frontend Gastos)
Sprint 6+7 → Sprint 10 (Frontend: Lista de Compras + P&L)
```

> **Nota de implementación:** Los sprints 1–7 son backend. Los sprints 8–10 son frontend. Se pueden arrancar en paralelo a partir de Sprint 2 si el frontend puede usar MSW handlers mientras el backend no está desplegado. El orden recomendado para un solo desarrollador es el listado arriba: backend primero para tener contratos de API estables.

---

## Resumen de Sprints

| Sprint | Nombre | Duración | Módulo | Tests nuevos est. |
|--------|--------|----------|--------|-------------------|
| 1 ✅ | Fundación del módulo inventario | 1 semana | `quickstack-inventory` (nuevo) | 57 backend (real) |
| 2 | CRUD de Ingredientes API | 1 semana | `quickstack-inventory` | ~25 backend |
| 3 | Recetas API | 1 semana | `quickstack-inventory` | ~30 backend |
| 4 | Auto-deducción al completar pago | 1 semana | `quickstack-pos` + `quickstack-inventory` | ~25 backend |
| 5 | Registro de Gastos API | 1 semana | `quickstack-inventory` | ~25 backend |
| 6 | Lista de Compras API | 0.5 semanas | `quickstack-inventory` | ~15 backend |
| 7 | Reporte P&L API | 0.5 semanas | `quickstack-pos` | ~15 backend |
| 8 | Frontend: Ingredientes + Recetas | 1.5 semanas | Frontend `/admin/inventory` | ~30 frontend |
| 9 | Frontend: Gastos | 1 semana | Frontend `/admin/expenses` | ~20 frontend |
| 10 | Frontend: Lista de Compras + P&L | 1 semana | Frontend `/admin/reports` | ~25 frontend |

**Total Phase 3:** ~10 semanas | ~240 tests nuevos estimados (150 backend + 75 frontend + 15 integration)

---

## Sprint 1: Fundación del Módulo Inventario

**Duración:** 1 semana
**Objetivo:** El módulo `quickstack-inventory` existe en el monorepo Maven, con sus entidades base, migraciones de BD y tests de persistencia. No hay endpoints todavía — solo el modelo de datos validado.

**Por qué primero:** Todas las features de Phase 3 dependen de este modelo de datos. Construirlo bien aquí evita migraciones destructivas después. TDD obliga a definir los invariantes del dominio antes de escribir servicios.

**Depende de:** Phase 2 completada (no hay dependencia técnica, pero se asume el equipo libre).

### Tareas Backend

| # | Tarea | Tamaño | Notas |
|---|-------|--------|-------|
| 1.1 | Crear módulo Maven `quickstack-inventory`: `pom.xml` con dependencias (`quickstack-common`, Spring Data JPA, Spring Boot Starter) | S | Seguir la estructura de `quickstack-branch/pom.xml`. Agregar el módulo al `pom.xml` raíz y a `quickstack-app/pom.xml` |
| 1.2 | Crear entidad `Ingredient.java`: campos `id UUID`, `tenantId UUID`, `branchId UUID` (nullable — ingrediente puede ser global del tenant), `name @NotBlank String`, `unit @NotNull UnitType enum`, `costPerUnit @NotNull BigDecimal`, `currentStock BigDecimal`, `minimumStock BigDecimal`, `deletedAt`, audit fields | M | Sin Lombok — getters/setters manuales. `UnitType`: `KILOGRAM, GRAM, LITER, MILLILITER, UNIT, PORTION`. `currentStock` y `minimumStock` en la misma unidad que `unit`. `@CreationTimestamp`/`@UpdateTimestamp` en audit fields |
| 1.3 | Crear entidad `Recipe.java`: representa la receta de un producto/variante. Campos: `id UUID`, `tenantId UUID`, `productId UUID`, `variantId UUID` (nullable — receta del producto base si no hay variante), `isActive boolean` | S | Una receta puede tener muchos `RecipeItem`. `productId` es FK lógica a `products` (sin FK de BD cross-module — mismo patrón que `OrderItem.productId`) |
| 1.4 | Crear entidad `RecipeItem.java`: línea de ingrediente en una receta. Campos: `id UUID`, `tenantId UUID`, `recipeId UUID`, `ingredientId UUID`, `quantity @Positive BigDecimal` | S | `quantity` en la misma unidad que `Ingredient.unit`. Constraint: una receta no puede tener el mismo ingrediente dos veces (unique por `(recipeId, ingredientId)`) |
| 1.5 | Crear entidad `InventoryMovement.java`: audit trail de cada cambio de stock. Campos: `id UUID`, `tenantId UUID`, `ingredientId UUID`, `movementType @NotNull MovementType enum`, `quantityDelta BigDecimal` (negativo para SALE_DEDUCTION), `unitCostAtTime BigDecimal`, `referenceId UUID` (nullable — el `orderId` para SALE_DEDUCTION), `notes String`, `createdAt`, `createdBy UUID` | M | `MovementType`: `SALE_DEDUCTION, MANUAL_ADJUSTMENT, PURCHASE`. Nunca se borra — sin `deletedAt`. Registra `unitCostAtTime` para cálculo histórico de COGS (ADR-007) |
| 1.6 | Crear entidad `Expense.java`: gasto del negocio. Campos: `id UUID`, `tenantId UUID`, `branchId UUID` (nullable), `amount @Positive BigDecimal`, `expenseCategory @NotNull ExpenseCategory enum`, `expenseDate @NotNull LocalDate`, `description String`, `receiptUrl String` (nullable — comprobante opcional), `deletedAt`, audit fields | M | `ExpenseCategory`: `FOOD_COST, LABOR, RENT, UTILITIES, SUPPLIES, OTHER`. Soft delete — el OWNER puede corregir errores |
| 1.7 | Crear migración Flyway `V8__create_inventory_tables.sql`: tablas `ingredients`, `recipes`, `recipe_items`, `inventory_movements`, `expenses`. Índices en `tenant_id` y en columnas de filtrado frecuente (`ingredient_id`, `movement_type`, `expense_date`) | M | Seguir convenciones de naming del proyecto (`idx_`, `uq_`, `chk_`). `inventory_movements` sin soft delete — no agregar `deleted_at`. Unique constraint `uq_recipe_items_recipe_ingredient` en `(recipe_id, ingredient_id)` |
| 1.8 | Crear repositorios JPA: `IngredientRepository`, `RecipeRepository`, `RecipeItemRepository`, `InventoryMovementRepository`, `ExpenseRepository` — cada uno con métodos básicos por `tenantId` | S | Seguir patrón de `CustomerRepository`. Los métodos de búsqueda complejos se agregan en sprints posteriores |
| 1.9 | Tests de persistencia `@DataJpaTest`: validar que las entidades se persisten correctamente, que los constraints funcionan (unique `(recipeId, ingredientId)`), que `InventoryMovement` no tiene `deleted_at` | M | Mínimo 20 tests. Un test por cada constraint relevante. Usar `@Sql` para seed data de test |

### Tareas Frontend

Ninguna en este sprint — solo fundación backend.

### Success Criteria

- [x] `./mvnw install -pl quickstack-inventory -DskipTests` compila sin errores
- [x] `./mvnw verify -pl quickstack-inventory` pasa todos los tests de persistencia (57/57)
- [x] `./mvnw verify` en el monorepo completo pasa (no hay regresiones en otros módulos)
- [x] La migración V9 ejecuta limpia (V9 reemplaza tablas draft de V4 via DROP CASCADE)
- [x] Los constraints de BD funcionan (RecipeItem duplicado y base recipe duplicado lanzan excepción)
- [x] 57 tests nuevos (superó el mínimo de 20)

**Nota implementación:** Migración fue V9 (no V8 — V8 ya existía con `change_ip_to_varchar`).
Unique constraint para `recipes` requirió **partial indexes** (no `UNIQUE` estándar) porque PostgreSQL trata `NULL != NULL` — dos partial indexes: uno `WHERE variant_id IS NULL` y otro `WHERE variant_id IS NOT NULL`.

**Riesgos:**
- El campo `branchId` en `Ingredient` es nullable para soportar ingredientes "globales del tenant" (ej. sal, aceite). En restaurantes pequeños el OWNER probablemente no necesita esto ahora, pero si lo hardcodeamos como NOT NULL, migrar después es costoso.
- **Mitigación:** Nullable desde el inicio. El service lo valida si se pasa un `branchId` que no pertenece al tenant.

---

## Sprint 2: CRUD de Ingredientes API

**Duración:** 1 semana
**Objetivo:** El OWNER puede crear, listar, actualizar y eliminar ingredientes vía API. Incluye el endpoint de alertas de stock bajo.

**Depende de:** Sprint 1 completado y migración V8 ejecutada.

### Tareas Backend

| # | Tarea | Tamaño | Notas |
|---|-------|--------|-------|
| 2.1 | Crear `IngredientCreateRequest.java`: record con `name @NotBlank`, `unit @NotNull UnitType`, `costPerUnit @NotNull @Positive BigDecimal`, `currentStock @NotNull @PositiveOrZero BigDecimal`, `minimumStock @NotNull @PositiveOrZero BigDecimal`, `branchId UUID` (nullable) | S | Bean Validation en todos los campos. `@AssertTrue` si se quisiera validar cross-field (no necesario aquí) |
| 2.2 | Crear `IngredientUpdateRequest.java`: record con los mismos campos pero todos opcionales (nullables) — semántica PATCH. Validar que si se pasa `costPerUnit`, sea positivo | S | Misma convención que `UserUpdateRequest` — actualizar solo campos no-null |
| 2.3 | Crear `IngredientResponse.java`: record con factory `static IngredientResponse from(Ingredient)`. Incluir campo calculado `isBelowMinimum = currentStock < minimumStock` | S | `isBelowMinimum` calculado en Java, no en DB — evita lógica en SQL para un campo simple. Útil para que el frontend pinte el indicador visual sin lógica adicional |
| 2.4 | Crear `IngredientService.java`: métodos `createIngredient`, `listIngredients` (con filtro opcional por `branchId`), `getIngredient`, `updateIngredient`, `softDeleteIngredient`, `listLowStockIngredients` | L | `listLowStockIngredients`: query `WHERE current_stock < minimum_stock AND deleted_at IS NULL AND tenant_id = :tenantId`. `updateIngredient`: si se actualiza `costPerUnit`, NO crear un movimiento automático (el movimiento se crea cuando el OWNER registra una compra — Sprint posterior) |
| 2.5 | Crear `IngredientPermissionEvaluator.java`: `@Component("ingredientPermissionEvaluator")`. Solo OWNER puede gestionar ingredientes | S | Patrón idéntico a `UserPermissionEvaluator`. Bean con nombre único, inyecta `JdbcTemplate` para validar IDOR (el ingrediente pertenece al tenant del JWT antes de operar) |
| 2.6 | Crear `IngredientController.java`: endpoints `POST /api/v1/ingredients`, `GET /api/v1/ingredients`, `GET /api/v1/ingredients/{id}`, `PATCH /api/v1/ingredients/{id}`, `DELETE /api/v1/ingredients/{id}`, `GET /api/v1/inventory/low-stock` | M | Sin `@RequestMapping` a nivel clase — rutas completas en cada método. Inyectar `JwtAuthenticationPrincipal`. `@PreAuthorize("@ingredientPermissionEvaluator.canAccess(...)")` en cada endpoint |
| 2.7 | Agregar `/api/v1/ingredients/**` y `/api/v1/inventory/**` a `SecurityConfig.java` | S | Accesible para `OWNER`. Agregar al bloque de `authorizeHttpRequests`. Seguir patrón de rutas existentes en `SecurityConfig` |
| 2.8 | Tests unitarios `IngredientServiceTest.java`: crear ingrediente, listar todos, listar con filtro de sucursal, actualizar, soft delete, listar low-stock | M | Mínimo 15 tests. `@ExtendWith(MockitoExtension.class)`. Validar que `softDeleteIngredient` setea `deletedAt` y no borra el registro |
| 2.9 | Tests de integración `IngredientE2ETest.java` en `quickstack-app`: OWNER crea ingrediente → lista → actualiza costo → list low-stock (vacío) → setear stock < mínimo → list low-stock (aparece) → eliminar → no aparece en lista | L | Extender `BaseIntegrationTest`. Validar multi-tenancy: ingrediente de tenant A no visible para tenant B. Mínimo 10 tests de integración |

### Tareas Frontend

Ninguna — se trabaja en paralelo solo si hay MSW handlers listos.

> **Opción de paralelismo:** Si se quiere avanzar frontend en paralelo, crear `inventoryHandlers.ts` en MSW con los handlers de ingredientes mockeados. El desarrollador puede construir los componentes UI con datos falsos mientras el backend se termina.

### Success Criteria

- [ ] `POST /api/v1/ingredients` crea un ingrediente con todos los campos
- [ ] `GET /api/v1/ingredients` retorna lista paginada del tenant (sin ingredientes de otros tenants)
- [ ] `PATCH /api/v1/ingredients/{id}` actualiza solo los campos enviados
- [ ] `DELETE /api/v1/ingredients/{id}` hace soft delete (el ingrediente no aparece en el `GET /api/v1/ingredients` subsecuente)
- [ ] `GET /api/v1/inventory/low-stock` retorna solo ingredientes con `currentStock < minimumStock`
- [ ] Cross-tenant retorna 404 (no 403)
- [ ] Todos los endpoints retornan 403 si el rol no es OWNER
- [ ] `./mvnw verify` pasa sin regresiones
- [ ] Mínimo 25 tests nuevos

---

## Sprint 3: Recetas API

**Duración:** 1 semana
**Objetivo:** El OWNER puede asignar una receta a un producto (o variante) — una lista de ingredientes con cantidades. La API valida que los ingredientes referenciados pertenezcan al mismo tenant.

**Depende de:** Sprint 2 (ingredientes deben existir para asignarse a una receta).

### Tareas Backend

| # | Tarea | Tamaño | Notas |
|---|-------|--------|-------|
| 3.1 | Crear `RecipeItemRequest.java`: record con `ingredientId @NotNull UUID`, `quantity @NotNull @Positive BigDecimal` | S | Usado tanto en creación como en actualización de receta |
| 3.2 | Crear `RecipeCreateRequest.java`: record con `productId @NotNull UUID`, `variantId UUID` (nullable), `items @NotEmpty List<RecipeItemRequest>` | S | `@AssertTrue isItemsUnique()`: validar que no hay `ingredientId` duplicado en la lista — mismo ingrediente dos veces en una receta es un error del OWNER |
| 3.3 | Crear `RecipeResponse.java` y `RecipeItemResponse.java`: incluir nombre del ingrediente (join lógico), cantidad y unidad | S | `RecipeItemResponse`: `{ ingredientId, ingredientName, unit, quantity }`. El nombre se obtiene del `Ingredient` al construir la respuesta — evitar N+1 con un fetch join |
| 3.4 | Crear `RecipeService.java`: métodos `createOrUpdateRecipe` (upsert — si ya existe receta para ese productId+variantId, reemplaza los items), `getRecipeByProduct`, `deleteRecipe`, `findRecipeForOrderItem` (usado por el sistema de auto-deducción en Sprint 4) | L | `createOrUpdateRecipe`: en una sola transacción: delete items existentes + insert nuevos. Validar que todos los `ingredientId` en el request pertenecen al tenant (IDOR cross-resource). `findRecipeForOrderItem`: busca por `productId` primero, luego por `variantId` si aplica |
| 3.5 | Crear `RecipeController.java`: `PUT /api/v1/products/{productId}/recipe` (crear/actualizar — upsert), `GET /api/v1/products/{productId}/recipe`, `DELETE /api/v1/products/{productId}/recipe` | M | `PUT` es la operación principal — el OWNER redefine la receta completa cada vez (no edición incremental por item). Esto simplifica el UI: el OWNER ve la lista completa y la reemplaza. `@PreAuthorize` con OWNER |
| 3.6 | Tests unitarios `RecipeServiceTest.java`: crear receta, obtener, actualizar (reemplaza items), eliminar, validación de ingredientes duplicados en request, validar IDOR cross-tenant en ingredientes | M | Mínimo 15 tests. Incluir test que valida que `createOrUpdateRecipe` con ingrediente de otro tenant lanza `ResourceNotFoundException` |
| 3.7 | Tests de integración `RecipeE2ETest.java`: OWNER crea ingredientes → crea producto (via endpoint existente) → asigna receta → obtiene receta → actualiza receta (cambia cantidades) → elimina receta | L | Mínimo 10 tests. El test de "actualiza receta" verifica que los items anteriores ya no existen y los nuevos sí |

### Tareas Frontend

Ninguna en este sprint.

### Success Criteria

- [ ] `PUT /api/v1/products/{productId}/recipe` crea la receta si no existe, la reemplaza si ya existe
- [ ] `GET /api/v1/products/{productId}/recipe` retorna la receta con nombres de ingredientes y cantidades
- [ ] `DELETE /api/v1/products/{productId}/recipe` elimina la receta (el producto sigue existiendo sin receta)
- [ ] Asignar un ingrediente de otro tenant a la receta retorna 404
- [ ] Asignar el mismo ingrediente dos veces en el mismo request retorna 400
- [ ] `./mvnw verify` pasa sin regresiones
- [ ] Mínimo 25 tests nuevos (unit + integration)

**Riesgos:**
- Un producto con variantes puede necesitar recetas distintas por variante (ej. Tamal de pollo vs. Tamal de cerdo). El modelo soporta esto con `variantId` nullable.
- **Mitigación:** Si el OWNER define receta solo en el `productId` (sin `variantId`), esa receta aplica a todas las variantes. Si define una para `variantId` específico, esa tiene prioridad. `findRecipeForOrderItem` implementa esta lógica de fallback en Sprint 4.

---

## Sprint 4: Auto-deducción de Inventario al Completar Pago

**Duración:** 1 semana
**Objetivo:** Al completar un pago exitosamente, el sistema descuenta automáticamente los ingredientes de cada item del pedido según su receta. Si un producto no tiene receta, no pasa nada (graceful skip). Se registra un `InventoryMovement` por cada ingrediente descontado.

**Depende de:** Sprint 3 (recetas deben existir). Sprint 4 extiende `quickstack-pos`.

**Este es el sprint de mayor riesgo técnico de Phase 3** — modifica el flujo de pago que ya está en producción.

### Tareas Backend

| # | Tarea | Tamaño | Notas |
|---|-------|--------|-------|
| 4.1 | Crear `InventoryDeductionEvent.java` en `quickstack-inventory`: record con `tenantId UUID`, `orderId UUID`, `orderItems List<OrderItemSnapshot>` donde `OrderItemSnapshot` tiene `productId UUID`, `variantId UUID`, `quantity int` | S | Record inmutable. `OrderItemSnapshot` es un DTO interno — no exponer la entidad `OrderItem` de `quickstack-pos` fuera de su módulo |
| 4.2 | Modificar `PaymentService.java` en `quickstack-pos`: después de confirmar el pago y actualizar el estado de la orden a COMPLETED, publicar `InventoryDeductionEvent` via `ApplicationEventPublisher` | M | La publicación debe ser **después** de que la transacción JPA se confirme — usar `applicationEventPublisher.publishEvent(event)` dentro del método `@Transactional`. Spring garantiza que el `@TransactionalEventListener` se ejecuta tras el commit. Agregar dependencia de `quickstack-inventory` en el `pom.xml` de `quickstack-pos` |
| 4.3 | Crear `InventoryDeductionListener.java` en `quickstack-inventory`: `@Component` con `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` que escucha `InventoryDeductionEvent` | M | Para cada `OrderItemSnapshot`: buscar receta con `RecipeService.findRecipeForOrderItem` → para cada `RecipeItem`: calcular `delta = quantity × recipeItem.quantity` → actualizar `ingredient.currentStock -= delta` → crear `InventoryMovement` (tipo `SALE_DEDUCTION`, `referenceId = orderId`, `unitCostAtTime = ingredient.costPerUnit`) |
| 4.4 | Manejo de errores en `InventoryDeductionListener`: si falla la deducción (ingrediente no encontrado, error de DB), loguear `[INVENTORY] ACTION=DEDUCTION_FAILED tenantId=X orderId=Y ingredientId=Z error=MSG` y continuar con el siguiente ingrediente — no relanzar la excepción | M | El pago ya está confirmado. Fallar silenciosamente (con log) es correcto para MVP. Un ingrediente fallido no debe impedir descontar los demás del mismo pedido. Usar `try/catch` por ingrediente |
| 4.5 | Crear `InventoryMovementService.java`: método `createMovement` que persiste un `InventoryMovement` y actualiza el `currentStock` del ingrediente en la misma transacción local | S | Separar esta lógica en un service propio facilita el test unitario independiente del listener |
| 4.6 | Tests unitarios `InventoryDeductionListenerTest.java`: evento con pedido de 2 items → verifica que se crean los movimientos correctos, que el stock se reduce en la cantidad esperada, que un item sin receta se skippea gracefully, que un error en un ingrediente no impide procesar los demás | L | Mínimo 12 tests. Usar `@ExtendWith(MockitoExtension.class)`. Mockear `RecipeService` y `InventoryMovementService` |
| 4.7 | Tests de integración `InventoryDeductionE2ETest.java` en `quickstack-app`: crear ingredientes → crear producto → asignar receta → crear orden → completar pago → verificar que `currentStock` bajó y existe `InventoryMovement` del tipo `SALE_DEDUCTION` | L | Este es el test más importante del sprint. Verificar también el caso de producto sin receta (stock no cambia). Usar `Thread.sleep(100ms)` o `awaitility` si el evento es asíncrono en el contexto de test |

### Tareas Frontend

Ninguna — este sprint es invisible para el usuario (automático).

### Success Criteria

- [ ] Al completar un pago, los ingredientes de los productos con receta se descuentan del `currentStock`
- [ ] Se crea un `InventoryMovement` de tipo `SALE_DEDUCTION` por cada ingrediente descontado, con `referenceId = orderId` y `unitCostAtTime` correcto
- [ ] Productos sin receta: el stock no cambia, no hay error
- [ ] Un fallo en el descuento de un ingrediente no revierte el pago ni impide descontar los demás ingredientes del pedido
- [ ] `./mvnw verify` pasa sin regresiones — especialmente en `PaymentServiceTest` y los tests E2E de pagos existentes
- [ ] Mínimo 25 tests nuevos

**Riesgos:**
- `PaymentService` ya tiene una transacción JPA compleja. Agregar la publicación del evento sin entender la secuencia de commits puede causar que el inventario se descuente cuando el pago aún no se confirmó (si la excepción ocurre después del evento pero antes del commit).
- **Mitigación:** `@TransactionalEventListener(phase = AFTER_COMMIT)` garantiza que el listener solo se ejecuta si la transacción principal confirmó. Documentar en el código el motivo de este choice con un comentario explicativo.

---

## Sprint 5: Registro de Gastos API

**Duración:** 1 semana
**Objetivo:** El OWNER puede registrar, listar, actualizar y eliminar gastos del negocio vía API. Filtrado por rango de fechas y categoría.

**Depende de:** Sprint 1 (migración V8 con tabla `expenses`). Independiente de Sprints 2–4.

### Tareas Backend

| # | Tarea | Tamaño | Notas |
|---|-------|--------|-------|
| 5.1 | Crear `ExpenseCreateRequest.java`: record con `amount @NotNull @Positive BigDecimal`, `expenseCategory @NotNull ExpenseCategory`, `expenseDate @NotNull LocalDate`, `description String`, `receiptUrl String` (nullable), `branchId UUID` (nullable) | S | Bean Validation. `expenseDate` en `LocalDate` — no timestamp, el gasto se registra por día |
| 5.2 | Crear `ExpenseUpdateRequest.java`: todos los campos opcionales — semántica PATCH | S | El OWNER puede corregir un gasto mal registrado |
| 5.3 | Crear `ExpenseResponse.java`: record con factory `static ExpenseResponse from(Expense)`. Incluir `expenseCategoryLabel` en español para display en frontend | S | `expenseCategoryLabel`: `FOOD_COST → "Costo de Alimentos"`, `LABOR → "Mano de Obra"`, `RENT → "Renta"`, `UTILITIES → "Servicios"`, `SUPPLIES → "Insumos"`, `OTHER → "Otro"`. Calculado en Java |
| 5.4 | Ampliar `ExpenseRepository`: agregar `findByTenantIdAndDateRangeAndCategory(UUID tenantId, LocalDate from, LocalDate to, ExpenseCategory category)` — parámetros opcionales usando `:category IS NULL OR expense_category = :category` | M | Patrón de query con parámetros opcionales ya existe en `UserRepository.findByTenantIdAndSearch` — misma técnica |
| 5.5 | Crear `ExpenseService.java`: métodos `createExpense`, `listExpenses` (con filtros opcionales de rango de fechas y categoría), `getExpense`, `updateExpense`, `softDeleteExpense`, `getTotalByCategory` (para el P&L — suma de gastos por categoría en un rango) | L | `listExpenses`: ordenar por `expense_date DESC`. `getTotalByCategory`: retorna `Map<ExpenseCategory, BigDecimal>` — usado por `ReportService` en Sprint 7 |
| 5.6 | Crear `ExpenseController.java`: `POST /api/v1/expenses`, `GET /api/v1/expenses` (con query params `from`, `to`, `category`), `GET /api/v1/expenses/{id}`, `PATCH /api/v1/expenses/{id}`, `DELETE /api/v1/expenses/{id}` | M | Misma convención del proyecto. Solo OWNER. Cross-tenant → 404. Parsear `from`/`to` como `LocalDate` con `@DateTimeFormat(iso = DATE)` |
| 5.7 | Agregar `/api/v1/expenses/**` a `SecurityConfig.java` con `hasRole('OWNER')` | S | Mismo patrón que otras rutas |
| 5.8 | Tests unitarios `ExpenseServiceTest.java`: crear, listar con filtros (por categoría, por rango de fechas, combinado), actualizar, soft delete, `getTotalByCategory` | M | Mínimo 12 tests |
| 5.9 | Tests de integración `ExpenseE2ETest.java`: OWNER crea 3 gastos de distintas categorías en distintas fechas → filtra por categoría → filtra por rango → filtra combinado → actualiza uno → lo elimina → no aparece en lista | L | Mínimo 8 tests de integración |

### Tareas Frontend

Ninguna en este sprint.

### Success Criteria

- [ ] `POST /api/v1/expenses` crea un gasto con todos los campos requeridos
- [ ] `GET /api/v1/expenses?from=2026-03-01&to=2026-03-31` retorna gastos del rango
- [ ] `GET /api/v1/expenses?category=FOOD_COST` retorna solo gastos de esa categoría
- [ ] Los filtros son combinables (fecha + categoría juntos)
- [ ] `PATCH /api/v1/expenses/{id}` actualiza solo los campos enviados
- [ ] `DELETE /api/v1/expenses/{id}` hace soft delete
- [ ] Cross-tenant retorna 404
- [ ] `./mvnw verify` pasa sin regresiones
- [ ] Mínimo 25 tests nuevos

---

## Sprint 6: Lista de Compras Auto-generada API

**Duración:** 0.5 semanas (3 días)
**Objetivo:** Un solo endpoint que retorna la lista de ingredientes cuyo stock está bajo el mínimo, con una cantidad sugerida de compra basada en consumo histórico de los últimos N días.

**Depende de:** Sprint 4 (deben existir `InventoryMovements` de tipo `SALE_DEDUCTION` para calcular consumo histórico).

### Tareas Backend

| # | Tarea | Tamaño | Notas |
|---|-------|--------|-------|
| 6.1 | Crear `ShoppingListItemResponse.java`: record con `ingredientId`, `ingredientName`, `unit`, `currentStock`, `minimumStock`, `deficit` (= `minimumStock - currentStock`), `suggestedPurchaseQuantity`, `averageDailyConsumption` | S | `suggestedPurchaseQuantity` = `deficit + (averageDailyConsumption × 7)` — el OWNER compra para cubrir el déficit más 7 días de consumo futuro. Fórmula simple y explicable |
| 6.2 | Implementar lógica de consumo histórico en `InventoryMovementService`: método `getAverageDailyConsumption(UUID tenantId, UUID ingredientId, int days)` — suma de `abs(quantityDelta)` de movimientos `SALE_DEDUCTION` en los últimos `days` días, dividido entre `days` | M | Usar `JdbcTemplate` para esta query agregada (más limpio que JPQL para agregaciones con fechas). `COALESCE(SUM(ABS(quantity_delta)), 0) / :days` en SQL. Retornar `BigDecimal` |
| 6.3 | Implementar `ShoppingListService.java`: método `generateShoppingList(UUID tenantId, int lookbackDays)` — obtener ingredientes con stock bajo (ya existe en Sprint 2), para cada uno calcular consumo histórico, construir `ShoppingListItemResponse`. `lookbackDays` default: 30 | M | Ordenar resultado por `deficit DESC` — los ingredientes más críticos primero |
| 6.4 | Agregar endpoint `GET /api/v1/inventory/shopping-list` a `InventoryController` (o `ShoppingListController` si se prefiere separar): query param opcional `days` (default 30) | S | Ruta ya cubierta por el `SecurityConfig` de Sprint 2 (`/api/v1/inventory/**`). Solo OWNER |
| 6.5 | Tests unitarios `ShoppingListServiceTest.java`: lista vacía cuando todo está sobre mínimo, lista con un ingrediente bajo mínimo y consumo histórico de 0, lista con consumo histórico real, orden correcto por déficit | S | Mínimo 8 tests |

### Success Criteria

- [ ] `GET /api/v1/inventory/shopping-list` retorna lista de ingredientes bajo el mínimo
- [ ] Cada item incluye stock actual, mínimo, déficit y cantidad sugerida de compra
- [ ] La cantidad sugerida incorpora el consumo histórico de los últimos 30 días (configurable con `?days=N`)
- [ ] Ingredientes con stock suficiente no aparecen en la lista
- [ ] Ingredientes sin movimientos históricos aparecen con `averageDailyConsumption = 0` y `suggestedPurchaseQuantity = deficit`
- [ ] `./mvnw verify` pasa sin regresiones
- [ ] Mínimo 12 tests nuevos

---

## Sprint 7: Reporte P&L API

**Duración:** 0.5 semanas (3 días)
**Objetivo:** Un endpoint que retorna el P&L del restaurante para un rango de fechas: ventas, COGS, gastos y margen bruto.

**Depende de:** Sprint 5 (gastos) y Sprint 4 (movimientos de inventario con `unitCostAtTime`).

### Tareas Backend

| # | Tarea | Tamaño | Notas |
|---|-------|--------|-------|
| 7.1 | Crear `PnlReportResponse.java`: record con `fromDate LocalDate`, `toDate LocalDate`, `revenue BigDecimal` (ventas del período), `cogs BigDecimal` (costo de ingredientes usados), `totalExpenses BigDecimal` (suma de `Expense` del período), `grossMargin BigDecimal` (= `revenue - cogs - totalExpenses`), `grossMarginPct BigDecimal` (porcentaje), `expensesByCategory Map<String, BigDecimal>` | S | Todos los `BigDecimal` con `.setScale(2, HALF_UP)`. `grossMarginPct = (grossMargin / revenue × 100)` — retornar `0.00` si `revenue = 0` para evitar división por cero |
| 7.2 | Implementar `PnlReportService.java` en `quickstack-pos`: método `getPnlReport(UUID tenantId, LocalDate from, LocalDate to)` — 3 queries con `JdbcTemplate`: (1) suma de ventas de `payments` + `orders` en el período, (2) suma de `ABS(quantity_delta × unit_cost_at_time)` de `InventoryMovements` SALE_DEDUCTION en el período, (3) suma y agrupación de `expenses` por `expense_category` en el período | L | Inyectar `JdbcTemplate` directamente (igual que `ReportService` existente). Las 3 queries son independientes — se pueden ejecutar en paralelo con `CompletableFuture` si el performance lo requiere (no optimizar prematuramente para MVP). Cross-module query: `quickstack-pos` consulta tabla `inventory_movements` y `expenses` por nombre de tabla — sin cruzar entities JPA de otro módulo |
| 7.3 | Agregar endpoint `GET /api/v1/reports/pnl` a `ReportController.java` existente en `quickstack-pos`: query params `from @NotNull LocalDate`, `to @NotNull LocalDate` | S | Validar que `from <= to` con un `@AssertTrue` o validación en el controller. Solo OWNER. `ReportController` ya está en `SecurityConfig` bajo `/api/v1/reports/**` |
| 7.4 | Tests unitarios `PnlReportServiceTest.java`: P&L con ventas y gastos, P&L con ventas sin gastos, P&L con COGS, P&L con todo en cero, validar que porcentaje es 0 cuando revenue es 0 | M | Mínimo 8 tests. Mockear `JdbcTemplate` como en `ReportServiceTest` existente — mismo patrón `@SuppressWarnings({"unchecked","rawtypes"})` |
| 7.5 | Test de integración `PnlReportE2ETest.java`: crear ingredientes + receta + gastos → completar un pago → verificar que el P&L refleja: revenue = monto del pago, cogs = costo calculado de los ingredientes, totalExpenses = suma de gastos, grossMargin = diferencia correcta | L | Mínimo 5 tests de integración. Es el test más representativo del valor del feature completo |

### Success Criteria

- [ ] `GET /api/v1/reports/pnl?from=2026-03-01&to=2026-03-31` retorna el P&L del mes
- [ ] `revenue` = suma real de ventas pagadas en el período
- [ ] `cogs` = suma de `abs(quantityDelta × unitCostAtTime)` de `InventoryMovements` tipo `SALE_DEDUCTION` en el período
- [ ] `totalExpenses` = suma de `Expense` no eliminados del período
- [ ] `grossMargin = revenue - cogs - totalExpenses`
- [ ] `grossMarginPct` no rompe con division by zero cuando `revenue = 0`
- [ ] `./mvnw verify` pasa sin regresiones, incluyendo los tests existentes de `ReportController`
- [ ] Mínimo 15 tests nuevos

---

## Sprint 8: Frontend — Ingredientes y Recetas

**Duración:** 1.5 semanas
**Objetivo:** El OWNER puede gestionar ingredientes desde `/admin/inventory` y asignar recetas a sus productos desde la página de detalle del producto.

**Depende de:** Sprints 2 y 3 completados y desplegados (o MSW handlers disponibles).

### Tareas Frontend

| # | Tarea | Tamaño | Notas |
|---|-------|--------|-------|
| 8.1 | Crear tipos TypeScript en `frontend/src/features/inventory/types/Inventory.ts`: `Ingredient`, `IngredientCreateRequest`, `IngredientUpdateRequest`, `IngredientResponse`, `RecipeItemRequest`, `RecipeResponse`, `ShoppingListItem` | S | Tipos que coincidan exactamente con los records Java del backend |
| 8.2 | Crear `inventoryApi.ts` en `features/inventory/api/`: funciones `listIngredients`, `getIngredient`, `createIngredient`, `updateIngredient`, `deleteIngredient`, `listLowStock`, `getShoppingList`, `getRecipeByProduct`, `saveRecipe`, `deleteRecipe` | M | Patrón: `axiosInstance.xxx(...).then(r => r.data.data)`. Misma estructura que `branchApi.ts` |
| 8.3 | Crear hooks TanStack Query: `useIngredientsQuery`, `useLowStockQuery`, `useCreateIngredientMutation`, `useUpdateIngredientMutation`, `useDeleteIngredientMutation`, `useRecipeQuery`, `useSaveRecipeMutation`, `useDeleteRecipeMutation` | M | `useCreateIngredientMutation` invalida `useIngredientsQuery` y `useLowStockQuery` en `onSuccess`. Misma convención que hooks existentes |
| 8.4 | Crear `IngredientList.tsx`: tabla MUI con columnas nombre, unidad, costo/unidad, stock actual, stock mínimo, estado (chip "Bajo Stock" / "OK"). Botón "Nuevo Ingrediente". Filtro por estado de stock (todos / bajo mínimo) | M | El chip "Bajo Stock" usa `IngredientResponse.isBelowMinimum`. Color rojo para bajo mínimo, verde para OK. Patrón de tabla como `BranchList.tsx` |
| 8.5 | Crear `IngredientForm.tsx`: modal MUI — campos nombre, unidad (Select con `UnitType`), costo por unidad, stock actual, stock mínimo, sucursal (Select nullable). Validación de formulario con `react-hook-form` o validación manual | M | En modo edición pre-poblar valores. El campo "stock actual" en modo edición debe mostrar advertencia: "Para ajustes de stock, usa un movimiento manual (próximamente)" — el OWNER no debe editar stock directamente (debe quedar registrado como `MANUAL_ADJUSTMENT`). Para MVP, permitirlo con la advertencia |
| 8.6 | Crear `InventoryPage.tsx` en `pages/admin/`: composición de `IngredientList` + `IngredientForm` modal. Ruta: `/admin/inventory` | S | Patrón de `BranchListPage.tsx` |
| 8.7 | Crear `RecipeEditor.tsx`: componente para asignar/editar receta de un producto. Lista de `RecipeItem` editable: cada fila tiene Select de ingrediente + campo de cantidad + botón eliminar. Botón "Agregar ingrediente". Botón "Guardar Receta" | L | Usar `PUT /api/v1/products/{productId}/recipe` (upsert). El OWNER ve la receta completa y la guarda entera — no edición incremental. Si no hay receta, mostrar estado vacío con CTA "Definir Receta" |
| 8.8 | Integrar `RecipeEditor.tsx` en la página de detalle de producto existente (o en un modal desde `ProductCatalog`): agregar tab o sección "Receta de Costo" al ver/editar un producto | M | No crear una ruta nueva — el OWNER accede a la receta desde la gestión del producto. Evaluar si la UI actual de productos tiene un modal de edición que se pueda extender |
| 8.9 | Agregar link "Inventario" al `Sidebar.tsx` en el bloque Owner Global View con ícono `Inventory2Icon` (MUI). Ruta `/admin/inventory`. Solo visible para OWNER | S | Seguir patrón del Sidebar — bloque `isGlobalView` |
| 8.10 | Crear `inventoryHandlers.ts` en MSW: handlers para todos los endpoints de inventario y recetas | M | Necesario para los tests. Datos de mock realistas: 5-6 ingredientes con distintos estados de stock |
| 8.11 | Tests: `IngredientList.test.tsx` (renderiza lista, chip de bajo stock, abrir form), `IngredientForm.test.tsx` (crear, editar, validación), `RecipeEditor.test.tsx` (agregar ingrediente, guardar, estado vacío) | M | Mínimo 20 tests. Usar `renderWithProviders`. Validar que el chip "Bajo Stock" aparece cuando `isBelowMinimum = true` |

### Success Criteria

- [ ] OWNER navega a `/admin/inventory` y ve la lista de ingredientes con estado visual de stock
- [ ] OWNER puede crear un ingrediente con todos sus campos
- [ ] OWNER puede editar nombre, costo y stock mínimo de un ingrediente existente
- [ ] OWNER puede asignar una receta a un producto desde la gestión del producto
- [ ] El link "Inventario" aparece en el Sidebar solo para OWNER
- [ ] Tests pasan sin regresiones en los 319+ tests existentes
- [ ] No hay errores de TypeScript (`npm run type-check`)

**Riesgos:**
- Integrar `RecipeEditor` en la UI de productos existente puede requerir refactorizar el modal/página de producto. Evaluar el impacto antes de empezar.
- **Mitigación:** Si el refactor es >M en esfuerzo, crear una ruta separada `/admin/products/{id}/recipe` en lugar de integrar en la página de producto. Aceptable como primera versión — el OWNER puede navegar a ella desde la lista de productos.

---

## Sprint 9: Frontend — Registro de Gastos

**Duración:** 1 semana
**Objetivo:** El OWNER puede registrar y gestionar gastos desde `/admin/expenses`. El formulario está optimizado para entrada rápida desde el celular.

**Depende de:** Sprint 5 completado y desplegado (o MSW handlers disponibles).

### Tareas Frontend

| # | Tarea | Tamaño | Notas |
|---|-------|--------|-------|
| 9.1 | Crear tipos TypeScript en `features/expenses/types/Expense.ts`: `Expense`, `ExpenseCreateRequest`, `ExpenseUpdateRequest`, `ExpenseResponse`, `ExpenseSummary` | S | `ExpenseSummary`: para vistas de totales por categoría |
| 9.2 | Crear `expenseApi.ts` en `features/expenses/api/`: funciones `listExpenses(filters)`, `createExpense`, `updateExpense`, `deleteExpense` | S | Pasar `from`, `to`, `category` como query params opcionales |
| 9.3 | Crear hooks TanStack Query: `useExpensesQuery(filters)`, `useCreateExpenseMutation`, `useUpdateExpenseMutation`, `useDeleteExpenseMutation` | S | `useExpensesQuery` recibe filtros como parámetro — la query se re-ejecuta cuando cambian los filtros |
| 9.4 | Crear `ExpenseForm.tsx`: formulario de entrada rápida — campo monto (prominente, tipo number), Select de categoría, DatePicker de fecha (default hoy), descripción (opcional). Botón "Registrar Gasto" | M | Optimizado para móvil — MUI `TextField` con `type="number"` para que el celular muestre teclado numérico al tocar el campo de monto. El campo más grande e importante es el monto. Categoría como Select, no chips (más fácil en celular) |
| 9.5 | Crear `ExpenseList.tsx`: tabla con columnas fecha, categoría (con label en español), monto, descripción, acciones (editar, eliminar). Filtros en la parte superior: rango de fechas (mes actual por defecto) y categoría | M | Mostrar total del período en el footer de la tabla: `"Total: $X,XXX.XX"`. Ordenar por fecha descendente |
| 9.6 | Crear `ExpensePage.tsx` en `pages/admin/`: `ExpenseForm` en la parte superior (entrada rápida) + `ExpenseList` debajo. Ruta: `/admin/expenses` | S | Diseño: el formulario de registro siempre visible arriba, sin necesidad de abrir un modal. El OWNER llega a la página y puede registrar inmediatamente |
| 9.7 | Agregar link "Gastos" al `Sidebar.tsx` en el bloque Owner Global View con ícono `ReceiptLongIcon`. Ruta `/admin/expenses`. Solo OWNER | S | Junto al link de Inventario |
| 9.8 | Crear `expenseHandlers.ts` en MSW: handlers para todos los endpoints de gastos con datos de mock | S | 5-8 gastos de distintas categorías y fechas |
| 9.9 | Tests: `ExpenseForm.test.tsx` (campos requeridos, submit, default date = hoy), `ExpenseList.test.tsx` (renderiza lista, filtros, total del período), `ExpensePage.test.tsx` (composición completa) | M | Mínimo 15 tests |

### Success Criteria

- [ ] OWNER entra a `/admin/expenses`, ve el formulario de registro y puede agregar un gasto en menos de 10 segundos
- [ ] La lista muestra gastos del mes actual por defecto con el total del período
- [ ] El OWNER puede filtrar por categoría y rango de fechas
- [ ] El link "Gastos" aparece en el Sidebar solo para OWNER
- [ ] Tests pasan sin regresiones
- [ ] No hay errores de TypeScript

---

## Sprint 10: Frontend — Lista de Compras y Reporte P&L

**Duración:** 1 semana
**Objetivo:** El OWNER puede ver la lista de compras auto-generada en `/admin/inventory/shopping-list` y el reporte P&L con gráfica de tendencia en `/admin/reports`.

**Depende de:** Sprints 6, 7, 8 y 9 completados.

### Tareas Frontend

| # | Tarea | Tamaño | Notas |
|---|-------|--------|-------|
| 10.1 | Crear tipos TypeScript: `ShoppingListItem`, `PnlReport`, `PnlTrend` | S | `PnlTrend`: para gráfica — lista de `{ period: string, revenue: number, grossMargin: number }` calculada en el frontend desde múltiples llamadas al endpoint P&L o desde un endpoint futuro (MVP: calcular en el frontend con llamadas por mes) |
| 10.2 | Crear hooks TanStack Query: `useShoppingListQuery(days?)`, `usePnlReportQuery(from, to)` | S | `usePnlReportQuery` tiene `enabled: !!from && !!to` para no llamar con parámetros vacíos |
| 10.3 | Crear `ShoppingListPage.tsx` en `pages/admin/`: tabla con ingrediente, stock actual, mínimo, cantidad sugerida. Ícono de alerta para ingredientes con déficit alto. Indicador de días de lookback (default 30). Botón "Exportar" (fase futura — disabled con tooltip "Próximamente") | M | Ordenar por cantidad de déficit descendente. Color de fondo rojo suave para filas con stock = 0. Ruta: `/admin/inventory/shopping-list` |
| 10.4 | Agregar link "Lista de Compras" al Sidebar dentro del bloque de Inventario (como subitem o link separado). Alternativamente, como botón/tab dentro de `InventoryPage` | S | Si el Sidebar ya está denso, integrar como tab dentro de `/admin/inventory` en lugar de link separado |
| 10.5 | Crear `PnlDashboard.tsx`: sección nueva en `/admin/reports` (página ya existe). DateRangePicker para seleccionar período. Cards con métricas: Ventas, COGS, Gastos Totales, Margen Bruto, % de Margen. Gráfica de barras con Recharts: `revenue` vs `grossMargin` por mes | L | Instalar Recharts (`npm install recharts`). Gráfica simple: BarChart con dos barras por período. Para la tendencia mensual: llamar `usePnlReportQuery` por cada mes del año (12 llamadas). Si es muy lento, simplificar a un solo período de barra. El valor diferenciador es ver el número, no la gráfica perfecta |
| 10.6 | Integrar `PnlDashboard.tsx` en la página de reportes existente (`/admin/reports`): agregar como sección debajo del reporte del día o como tab "P&L" | M | No reemplazar el reporte del día — complementarlo. El OWNER necesita ambas vistas: operacional (hoy) y financiera (el mes) |
| 10.7 | Crear MSW handlers para P&L y shopping list | S | Datos de mock con valores realistas para un restaurante pequeño |
| 10.8 | Tests: `ShoppingListPage.test.tsx` (renderiza lista, orden por déficit), `PnlDashboard.test.tsx` (renderiza cards con valores, gráfica presente) | M | Mínimo 15 tests. Para la gráfica: verificar que el componente `BarChart` renderiza (no verificar valores internos de Recharts — es una librería externa) |

### Success Criteria

- [ ] OWNER navega a `/admin/inventory/shopping-list` y ve la lista de ingredientes que necesita comprar, ordenada por urgencia
- [ ] OWNER navega a `/admin/reports`, ve las cards de P&L (Ventas, COGS, Gastos, Margen) para el período seleccionado
- [ ] La gráfica de tendencia renderiza sin errores con datos reales
- [ ] Los valores en las cards son coherentes: `Margen = Ventas - COGS - Gastos`
- [ ] Tests pasan sin regresiones
- [ ] No hay errores de TypeScript

---

## Success Criteria Globales — Phase 3

Al completar los 10 sprints, el siguiente flujo debe ejecutarse de punta a punta en producción sin intervenciones manuales en la base de datos:

1. OWNER define ingredientes en `/admin/inventory` (ej. pollo, tortilla, aguacate con costos y stocks)
2. OWNER asigna receta a un producto: "Quesadilla de Pollo = 150g pollo + 2 tortillas + 30g aguacate"
3. CASHIER completa una venta de 3 quesadillas de pollo en `/cashier/pos`
4. El sistema descuenta automáticamente: 450g pollo, 6 tortillas, 90g aguacate del stock
5. OWNER registra un gasto de renta: $8,500 en `/admin/expenses`
6. OWNER abre `/admin/inventory/shopping-list` y ve el pollo en la lista (stock bajó)
7. OWNER abre `/admin/reports` → sección P&L → selecciona el mes → ve: Ventas, COGS calculado, Gastos ($8,500 de renta), Margen Bruto

**Métricas de calidad de Phase 3:**
- [ ] Backend: ~1,210+ tests (1,060 existentes + ~150 nuevos)
- [ ] Frontend: ~395+ tests (319 existentes + ~75 nuevos)
- [ ] `./mvnw verify` pasa sin regresiones
- [ ] `npm test` pasa sin regresiones
- [ ] Todos los endpoints nuevos tienen IDOR protection (cross-tenant → 404)
- [ ] Los nuevos endpoints están en `SecurityConfig.java`
- [ ] No hay errores de TypeScript en el frontend

---

## Notas de Seguridad — ASVS L2

### Acceso a datos financieros (V4 — Access Control)

Los datos de Phase 3 son altamente sensibles: costos de ingredientes, gastos del negocio, márgenes. Aplicar los mismos principios de IDOR que el resto del sistema:

- Todo endpoint de Phase 3 extrae `tenantId` del JWT — nunca del request body o path variable
- Cross-tenant → 404 siempre (no 403, para no revelar existencia del recurso)
- Solo OWNER puede acceder a todos los endpoints de `quickstack-inventory` y al endpoint P&L
- `InventoryMovement`: nunca modificable via API — es solo lectura para el OWNER. La escritura es exclusivamente interna (auto-deducción del sistema + ajuste manual controlado)

### Validación de datos financieros (V5 — Validation)

- `amount` y `costPerUnit`: `@Positive BigDecimal` — nunca negativos desde el cliente
- `quantity` en `RecipeItem`: `@Positive BigDecimal` — nunca cero o negativo
- `expenseDate`: validar que no sea fecha futura (el OWNER no puede "pre-registrar" gastos) — `@PastOrPresent LocalDate`
- Rango de fechas en P&L: validar `from <= to` y que el rango no exceda 12 meses (previene queries excesivamente costosas para el MVP)

### Logging de operaciones financieras (V7 — Error and Logging)

Aplicar el patrón de logging del proyecto en todas las operaciones de Phase 3:

```
[INVENTORY] ACTION=INGREDIENT_CREATED tenantId=X userId=Y ingredientId=Z name=pollo
[INVENTORY] ACTION=RECIPE_SAVED tenantId=X userId=Y productId=Z items=3
[INVENTORY] ACTION=SALE_DEDUCTION tenantId=X orderId=Y ingredientId=Z delta=-150g
[INVENTORY] ACTION=DEDUCTION_FAILED tenantId=X orderId=Y ingredientId=Z error=MSG
[EXPENSE] ACTION=EXPENSE_CREATED tenantId=X userId=Y expenseId=Z amount=8500 category=RENT
[REPORT] ACTION=PNL_VIEWED tenantId=X userId=Y from=2026-03-01 to=2026-03-31
```

### Protección de datos de costos (V8 — Data Protection)

Los costos de ingredientes y márgenes son información confidencial del negocio. Verificar que no se filtran en logs de error ni en mensajes de excepción que lleguen al cliente. El `GlobalExceptionHandler` existente ya protege contra esto — mantener ese patrón.

---

## Deuda Técnica Conocida — Phase 3

Estas decisiones son correctas para el MVP pero deben revisitarse en Phase 4:

1. **Descuento de inventario sin compensación:** Si `InventoryDeductionListener` falla después de que el pago se confirmó, el stock no se descuenta. El OWNER verá stock incorrecto. Para Phase 4: tabla de `PendingDeductions` con worker que reintenta descuentos fallidos (patrón outbox).

2. **Stock puede quedar en negativo:** No hay validación que impida vender cuando el stock es cero. El sistema descuenta sin importar si el resultado es negativo. Para el MVP esto es correcto — el OWNER no quiere que las ventas se bloqueen por el inventario. Para Phase 4: alertas en tiempo real cuando el stock llega a cero durante una venta activa.

3. **Recetas sin versionado:** Si el OWNER cambia una receta, los movimientos históricos ya tienen `unitCostAtTime` capturado, pero no hay forma de saber qué receta estaba vigente cuando se hizo cada venta. Para Phase 4: tabla `recipe_versions` con snapshot de la receta al momento del movimiento.

4. **Lista de compras sin persistencia:** La lista se genera en tiempo real con cada request. Para Phase 4: permitir al OWNER "confirmar" la lista de compras (crear `InventoryMovement` de tipo `PURCHASE` para registrar que compró esos ingredientes y actualizar el stock).

5. **P&L sin desglose por sucursal:** El P&L de Phase 3 es a nivel tenant. Para Phase 4: filtrado por `branchId` para OWNER con múltiples sucursales.

6. **Recibos de gastos sin almacenamiento:** `receiptUrl` acepta una URL pero no hay storage propio. Para Phase 4: integrar con S3/Cloudflare R2 y agregar upload de imagen desde el celular.

---

## Definition of Done — Phase 3

Un sprint está completo cuando:

- [ ] Todos los tests nuevos pasan (`./mvnw verify` / `npm test`)
- [ ] No hay regresiones en tests existentes (backend + frontend)
- [ ] El flujo descrito en los Success Criteria del sprint se puede ejecutar manualmente en staging
- [ ] El código está en `main` via Pull Request con descripción del ADR aplicado
- [ ] No hay errores de TypeScript (`npm run type-check`)
- [ ] Los nuevos endpoints están registrados en `SecurityConfig.java`
- [ ] Todos los endpoints nuevos protegen contra IDOR (cross-tenant → 404)
- [ ] El logging sigue el patrón `[DOMAIN] ACTION=X tenantId=Y ...`
- [ ] No hay `TODO` críticos en el código mergeado (los `TODO` de deuda aceptada van documentados en `TECH_DEBT.md`)
