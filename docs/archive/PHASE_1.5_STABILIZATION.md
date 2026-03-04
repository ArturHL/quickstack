# Phase 1.5: Estabilización — Flujos Completos antes de Phase 2

> **Versión:** 1.0.0
> **Fecha:** 2026-03-02
> **Archivado:** 2026-03-04
> **Status:** ✅ COMPLETADA (5/5 sprints) — 75 tests nuevos frontend
> **Objetivo:** Corregir bugs de producción y completar los flujos de UI que quedaron incompletos en Phase 1.4.

---

## Contexto

Phase 1.4 implementó la UI del POS, pero al navegar la aplicación en producción se detectaron bugs y flujos rotos. Esta fase no agrega funcionalidad nueva: consolida lo que ya existe en el backend pero carece de UI, y corrige errores encontrados en producción.

**Regla:** No se inicia Phase 2 hasta que todos los sprints de 1.5 estén completados y verificados en producción.

---

## Bugs Encontrados en Producción

| # | Descripción | Causa Raíz | Estado |
|---|-------------|------------|--------|
| B-01 | `k?.map is not a function` en `/admin/products` | `GET /api/v1/categories` devuelve `Page<T>` pero el frontend esperaba array directo | ✅ Corregido (commit d353b30) |
| B-02 | `POST /api/v1/products` 400 Bad Request al crear producto | `categoryId` es `@NotNull` en backend pero el form permitía "Sin categoría" | ✅ Corregido (commit a3080d5) |
| B-03 | SKU rechazado con 400 si contiene minúsculas | Backend valida `^[A-Z0-9_-]{0,50}$` pero el form no lo indica ni lo fuerza | ⏳ Pendiente — Sprint 1 |
| B-04 | Editar producto VARIANT no muestra variantes existentes | `ProductForm` edit mode no carga `GET /api/v1/products/{id}/variants` | ⏳ Pendiente — Sprint 2 |

---

## Flujos Sin UI (Backend Listo, Frontend Faltante)

| Área | Endpoints disponibles | Impacto |
|------|-----------------------|---------|
| **Categorías** | `GET/POST/PUT/DELETE /api/v1/categories` | **Crítico** — sin categorías no se pueden crear productos |
| **Variantes** | `GET/POST /api/v1/products/{id}/variants`, `PUT/DELETE /{variantId}` | Alto — editar productos VARIANT es incompleto |
| **Grupos de Modificadores** | `GET/POST /api/v1/products/{id}/modifier-groups`, `GET/PUT/DELETE /api/v1/modifier-groups/{id}` | Medio — modificadores no administrables desde UI |
| **Modificadores** | `GET/POST /api/v1/modifier-groups/{groupId}/modifiers`, `PUT/DELETE /api/v1/modifiers/{id}` | Medio — idem |
| **Combos** | `GET/POST/PUT/DELETE /api/v1/combos` | Medio — combos no administrables desde UI |
| **Reportes** | `GET /api/v1/reports/daily-summary` | Bajo — sidebar dice "Próximamente" |

---

## Sprints

### Sprint 1: Admin Categorías + Bug SKU (3–4 días)

**Objetivo:** Desbloquear la creación de productos desde cero (sin datos seed).

#### Tareas

| ID | Tarea | Detalle |
|----|-------|---------|
| 1.1 | `categoryApi.ts` | `getCategories`, `createCategory`, `updateCategory`, `deleteCategory` |
| 1.2 | Hooks | `useCategoriesQuery`, `useCreateCategoryMutation`, `useUpdateCategoryMutation`, `useDeleteCategoryMutation` |
| 1.3 | `CategoryList` | Tabla con nombre, descripción, estado, acciones Editar/Eliminar. Soft-delete con confirmación. |
| 1.4 | `CategoryForm` | Modal inline (create + edit). Campos: nombre (requerido), descripción. |
| 1.5 | Ruta `/admin/categories` | `RoleProtectedRoute minRole="MANAGER"` |
| 1.6 | Link en Sidebar | Sección Admin, encima de Productos, icono `Category` |
| 1.7 | Fix B-03 SKU | En `ProductForm`: transformar a mayúsculas `onChange` + validación `^[A-Z0-9_-]*$` con mensaje claro |
| 1.8 | Tests | `CategoryList.test.tsx`, `CategoryForm.test.tsx` (render, create, edit, delete, validación) |

**Criterio de completado:** Usuario puede crear una categoría, luego crear un producto asignándole esa categoría, sin errores en producción.

---

### Sprint 2: Variantes en Edición de Producto (2–3 días)

**Objetivo:** Corregir B-04 — editar un producto VARIANT muestra y permite modificar sus variantes.

#### Contexto técnico

- `GET /api/v1/products/{id}/variants` devuelve la lista de variantes existentes.
- `PUT /api/v1/products/{id}/variants/{variantId}` actualiza una variante.
- `POST /api/v1/products/{id}/variants` agrega una variante nueva.
- `DELETE /api/v1/products/{id}/variants/{variantId}` elimina una variante.
- El flujo de create ya envía variantes en el body de `POST /api/v1/products` (campo `variants[]`).

#### Tareas

| ID | Tarea | Detalle |
|----|-------|---------|
| 2.1 | `variantApi.ts` | `getVariants(productId)`, `createVariant`, `updateVariant`, `deleteVariant` |
| 2.2 | `useVariantsQuery` | Hook que carga variantes en edit mode cuando `productType === 'VARIANT'` |
| 2.3 | `ProductForm` edit mode | Cargar variantes existentes al montar; permitir editar nombre/precio, agregar nuevas, eliminar |
| 2.4 | Mutaciones variantes | Guardar cambios de variantes al hacer submit en edit mode (add/update/delete por separado) |
| 2.5 | Tests | Actualizar `ProductForm.test.tsx`: edit mode VARIANT carga variantes, permite modificarlas |

**Criterio de completado:** Editar un producto de tipo VARIANT muestra las variantes actuales y guarda los cambios correctamente.

---

### Sprint 3: Admin Modificadores (3–4 días)

**Objetivo:** Permitir administrar grupos de modificadores y sus opciones desde la UI.

#### Contexto técnico

Los modificadores son anidados: un `Product` tiene `ModifierGroup`s, cada grupo tiene `Modifier`s. La UI se implementa como una sub-sección dentro de la edición de producto.

#### Tareas

| ID | Tarea | Detalle |
|----|-------|---------|
| 3.1 | `modifierApi.ts` | Todos los endpoints de modifier-groups y modifiers |
| 3.2 | Hooks | `useModifierGroupsQuery`, `useCreateModifierGroupMutation`, `useUpdateModifierGroupMutation`, `useDeleteModifierGroupMutation`, `useCreateModifierMutation`, `useUpdateModifierMutation`, `useDeleteModifierMutation` |
| 3.3 | `ModifierGroupList` | Componente dentro de `ProductFormPage` (solo en edit mode). Lista grupos con nombre, tipo (SINGLE/MULTIPLE), requerido. Botones: agregar grupo, editar, eliminar. |
| 3.4 | `ModifierGroupForm` | Modal para crear/editar grupo. Campos: nombre, tipo de selección, requerido, mínimo/máximo. |
| 3.5 | `ModifierList` | Sub-lista dentro de cada grupo. Muestra modificadores con nombre y precio adicional. |
| 3.6 | `ModifierForm` | Modal para crear/editar modificador. Campos: nombre, precio adicional (default 0). |
| 3.7 | Tests | `ModifierGroupList.test.tsx`, `ModifierForm.test.tsx` |

**Criterio de completado:** Un MANAGER puede agregar modificadores a un producto existente y verlos reflejados en el POS al seleccionar ese producto.

---

### Sprint 4: Admin Combos (2–3 días)

**Objetivo:** Permitir crear y editar combos desde la UI.

#### Contexto técnico

Un combo (`/api/v1/combos`) es un producto especial que agrupa otros productos/variantes con precio propio. El endpoint de menú ya los incluye.

#### Tareas

| ID | Tarea | Detalle |
|----|-------|---------|
| 4.1 | `comboApi.ts` | `getCombos`, `getCombo`, `createCombo`, `updateCombo`, `deleteCombo` |
| 4.2 | Hooks | `useCombosQuery`, `useCreateComboMutation`, `useUpdateComboMutation`, `useDeleteComboMutation` |
| 4.3 | `ComboList` | Tabla con nombre, precio, estado. Acciones: crear, editar, eliminar. |
| 4.4 | `ComboForm` | Formulario: nombre, descripción, precio. Selector de productos/variantes que componen el combo. |
| 4.5 | Ruta `/admin/combos` | `RoleProtectedRoute minRole="MANAGER"` |
| 4.6 | Link en Sidebar | Sección Admin, icono `LocalOffer` o similar |
| 4.7 | Tests | `ComboList.test.tsx`, `ComboForm.test.tsx` |

**Criterio de completado:** Un MANAGER puede crear un combo, y el combo aparece en el catálogo del POS.

---

### Sprint 5: Reportes UI (2 días)

**Objetivo:** Conectar `GET /api/v1/reports/daily-summary` con una pantalla básica de reportes.

#### Datos disponibles en el endpoint

- `totalSales` (BigDecimal)
- `orderCount` (int)
- `averageTicket` (BigDecimal)
- `topProducts` (lista de nombre + cantidad vendida)
- `date` (LocalDate)

#### Tareas

| ID | Tarea | Detalle |
|----|-------|---------|
| 5.1 | `reportApi.ts` | `getDailySummary(date?: string): Promise<DailySummaryResponse>` |
| 5.2 | `useDailySummaryQuery` | Hook con `queryKey: ['reports', 'daily-summary', date]` |
| 5.3 | `DailySummaryPage` | Tarjetas: Total ventas, # Pedidos, Ticket promedio. Tabla: Top productos del día. Selector de fecha. |
| 5.4 | Ruta `/admin/reports` | `RoleProtectedRoute minRole="MANAGER"` |
| 5.5 | Sidebar | Habilitar link "Reportes" (quitar "Próximamente") |
| 5.6 | Tests | `DailySummaryPage.test.tsx` (carga, muestra métricas, selector de fecha) |

**Criterio de completado:** Un OWNER/MANAGER puede ver el resumen del día actual y de días anteriores.

---

## Resumen de Timeline

| Sprint | Nombre | Días est. | Prioridad |
|--------|--------|-----------|-----------|
| 1 | Admin Categorías + Bug SKU | 3–4 | 🔴 Crítico |
| 2 | Variantes en Edición | 2–3 | 🟠 Alto |
| 3 | Admin Modificadores | 3–4 | 🟡 Medio |
| 4 | Admin Combos | 2–3 | 🟡 Medio |
| 5 | Reportes UI | 2 | 🟢 Bajo |
| **Total** | | **~14 días** | |

---

## Definition of Done (Phase 1.5)

- [ ] Todos los bugs listados en la tabla marcados como ✅ Corregido
- [ ] Un usuario con rol MANAGER puede crear categorías, productos (con variantes, modificadores, combos) sin errores 400/500
- [ ] El flujo completo POS (catálogo → carrito → pago) funciona en producción con datos reales
- [ ] Todos los tests del frontend pasan (`npm run test`)
- [ ] No hay errores en consola de producción al navegar las rutas implementadas
