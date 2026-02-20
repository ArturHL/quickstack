# Phase 1.4: Frontend POS — Complete Point of Sale UI

> **Version:** 1.0.0
> **Fecha:** 2026-02-19
> **Status:** PENDIENTE - Sprint 0/6
> **Modulo:** Frontend (React + Vite + MUI)
> **Parte de:** Phase 1: Core POS - Ventas Completas

---

## Resumen Ejecutivo

Este documento define el plan de implementacion de la **cuarta y ultima sub-fase de Phase 1**: la interfaz completa del punto de venta (POS) para QuickStack.

**Importante:** Esta sub-fase construye sobre el frontend base de Phase 0.4 (auth, layout, dashboard) e implementa todas las pantallas necesarias para operar el POS: catalogo, carrito, selector de servicio, pago, gestion de pedidos, y administracion de productos/sucursales.

| Aspecto | Detalle |
|---------|---------|
| **Timeline** | 6 sprints (~3 semanas) |
| **Tareas Frontend** | 28 tareas (~80 horas) |
| **Tareas Backend** | 0 (backend ya completado en Phases 1.1-1.3) |
| **Tareas QA** | 6 tareas (~18 horas) |
| **Pantallas nuevas** | 15 pantallas/componentes principales |
| **Tests nuevos** | ~120 tests frontend |
| **Integracion con backend** | 28 endpoints REST ya disponibles |

---

## Decisiones de Diseno Confirmadas

### Stack Frontend (ya establecido en Phase 0.4)

| Tecnologia | Version | Uso |
|------------|---------|-----|
| React | 19 | UI Library |
| Vite | 5.x | Build tool |
| TypeScript | 5.x | Type safety |
| Material UI | 5.17+ | Component library |
| Zustand | 4.5+ | State management |
| TanStack Query | 5.76+ | Server state / API caching |
| Axios | 1.13+ | HTTP client |
| React Router | 6.30+ | Routing |
| Vitest | 3.2+ | Testing framework |
| MSW | 2.7+ | API mocking |

### Estructura de Archivos (Package by Feature)

```
frontend/src/
├── features/
│   ├── auth/              (EXISTENTE - Phase 0.4)
│   ├── pos/               (NUEVO - Sprint 1-4)
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── api/
│   │   ├── stores/
│   │   └── types/
│   ├── products/          (NUEVO - Sprint 5)
│   ├── branches/          (NUEVO - Sprint 5)
│   └── orders/            (NUEVO - Sprint 4)
├── components/
│   └── ui/                (Compartidos: Dialog, Card, etc.)
├── routes/                (Definiciones de rutas)
└── lib/                   (Axios, utils)
```

### Flujo de Usuario Principal (POS - Cashier)

```
1. Login → Dashboard
2. Seleccionar "Nuevo Pedido"
3. Seleccionar tipo de servicio (DINE_IN/COUNTER/DELIVERY/TAKEOUT)
   3a. Si DINE_IN: Seleccionar mesa
   3b. Si DELIVERY: Seleccionar/crear cliente
4. Navegar catalogo de productos (por categoria)
5. Agregar productos al carrito (seleccionar variantes, modifiers)
6. Revisar carrito con totales calculados
7. Confirmar y enviar orden a cocina
8. Pantalla de pago (solo efectivo en MVP)
9. Registrar pago → orden completada
10. Imprimir/enviar ticket digital (fuera de scope Phase 1.4)
```

### Pantallas Principales

| Pantalla | Ruta | Rol Minimo | Descripcion |
|----------|------|-----------|-------------|
| Dashboard | `/dashboard` | CASHIER | Home con accesos rapidos (EXISTENTE) |
| Nuevo Pedido | `/pos/new` | CASHIER | Selector de tipo de servicio |
| Selector de Mesa | `/pos/new/table` | CASHIER | Grid de mesas disponibles (solo DINE_IN) |
| Selector de Cliente | `/pos/new/customer` | CASHIER | Search/crear cliente (solo DELIVERY) |
| Catalogo POS | `/pos/catalog` | CASHIER | Grid de productos por categoria |
| Detalle de Producto | `/pos/product/:id` | CASHIER | Modal con variantes y modifiers |
| Carrito | `/pos/cart` | CASHIER | Lista de items + totales |
| Pago | `/pos/payment` | CASHIER | Formulario de pago en efectivo |
| Pedidos del Dia | `/orders` | CASHIER | Lista de pedidos (filtrable) |
| Detalle de Pedido | `/orders/:id` | CASHIER | Ver orden completa con items |
| Productos (Admin) | `/admin/products` | MANAGER | CRUD de productos |
| Producto Nuevo/Editar | `/admin/products/new` | MANAGER | Formulario de producto |
| Sucursales (Admin) | `/admin/branches` | OWNER | CRUD de sucursales |
| Areas y Mesas (Admin) | `/admin/areas` | MANAGER | CRUD de areas/mesas |
| Clientes (Admin) | `/admin/customers` | CASHIER | CRUD de clientes |

### State Management

| Store | Responsabilidad | Persistence |
|-------|-----------------|-------------|
| `authStore` | Token, user info | EXISTENTE (in-memory) |
| `posStore` | Orden actual en progreso (items, totales, servicio) | sessionStorage (recuperar si refresh) |
| `branchStore` | Sucursal activa del usuario | localStorage |
| `cartStore` | Carrito temporal antes de crear orden | sessionStorage |

### API Integration Pattern

**Patron establecido en Phase 0.4:**
1. Definir tipos TypeScript en `features/{feature}/types/`
2. Crear funciones API en `features/{feature}/api/{feature}Api.ts` usando axios
3. Crear hooks de TanStack Query en `features/{feature}/hooks/use{Feature}Query.ts`
4. Consumir hooks en componentes React

**Ejemplo:**
```typescript
// features/pos/api/menuApi.ts
export const menuApi = {
  getMenu: () => axios.get<MenuResponse>('/api/v1/menu')
};

// features/pos/hooks/useMenuQuery.ts
export const useMenuQuery = () => {
  return useQuery({
    queryKey: ['menu'],
    queryFn: () => menuApi.getMenu()
  });
};
```

### Roles y Permisos (Frontend)

| Pantalla/Accion | OWNER | MANAGER | CASHIER |
|-----------------|-------|---------|---------|
| Dashboard | SI | SI | SI |
| Nuevo Pedido | SI | SI | SI |
| Pedidos del Dia | Todos | Todos | Solo propios |
| CRUD Productos | SI | SI | NO |
| CRUD Sucursales | SI | NO | NO |
| CRUD Areas/Mesas | SI | SI | NO |
| CRUD Clientes | SI | SI | Ver/Crear |

**Implementacion:** `ProtectedRoute` component con `requiredRole` prop (ya existe de Phase 0.4).

### Responsive Design

- Desktop first (POS primario en tablet/laptop)
- Mobile: layout colapsado, sidebar ocultable
- Grid de productos: 4 cols desktop, 2 cols tablet, 1 col mobile
- Carrito: fixed bottom bar en mobile, sidebar en desktop

---

## Deuda Tecnica Aceptada

| Item | Justificacion | Phase sugerida |
|------|---------------|----------------|
| Upload de imagenes de productos | Requiere storage externo (S3/GCS) y backend adicional | **Phase 2** |
| Drag & drop de productos en carrito | UX mejorada pero no critica | **Phase 2** |
| Selector visual de plano de mesas | Layout simple con Grid es suficiente para MVP | **Phase 2** |
| Filtros avanzados de pedidos | Filtro basico por fecha y estado es suficiente | **Phase 2** |
| Busqueda full-text en catalogo | Navegacion por categoria es suficiente | **Phase 2** |
| PWA / modo offline | Requiere service workers y estrategia de sync | **Phase 3+** |
| Impresion directa de tickets | Tickets digitales (WhatsApp/Email) en Phase 3 | **Phase 3** |

---

## Arquitectura de Componentes

### Feature: POS (Sprint 1-4)

```
features/pos/
├── components/
│   ├── ServiceTypeSelector.tsx           <- Selector DINE_IN/COUNTER/etc
│   ├── TableSelector.tsx                 <- Grid de mesas
│   ├── CustomerSelector.tsx              <- Search + crear cliente
│   ├── ProductCatalog.tsx                <- Grid de productos
│   ├── ProductCard.tsx                   <- Card individual de producto
│   ├── ProductDetailModal.tsx            <- Modal con variantes + modifiers
│   ├── VariantSelector.tsx               <- Radio buttons de variantes
│   ├── ModifierGroup.tsx                 <- Checkbox/Radio de modifiers
│   ├── Cart.tsx                          <- Lista de items + totales
│   ├── CartItem.tsx                      <- Item individual con qty
│   ├── PaymentForm.tsx                   <- Formulario de pago (CASH)
│   └── OrderConfirmation.tsx             <- Pantalla post-pago
├── hooks/
│   ├── useMenuQuery.ts                   <- GET /api/v1/menu
│   ├── useTablesQuery.ts                 <- GET /api/v1/areas/{id}/tables
│   ├── useCustomersQuery.ts              <- GET /api/v1/customers?search=...
│   ├── useCreateOrderMutation.ts         <- POST /api/v1/orders
│   ├── useAddItemMutation.ts             <- POST /api/v1/orders/{id}/items
│   ├── useSubmitOrderMutation.ts         <- POST /api/v1/orders/{id}/submit
│   └── useRegisterPaymentMutation.ts     <- POST /api/v1/payments
├── api/
│   ├── menuApi.ts
│   ├── orderApi.ts
│   ├── tableApi.ts
│   └── customerApi.ts
├── stores/
│   ├── posStore.ts                       <- Estado de orden en progreso
│   └── cartStore.ts                      <- Carrito temporal
└── types/
    ├── Menu.ts                           <- MenuResponse, MenuProductItem
    ├── Order.ts                          <- OrderCreateRequest, OrderResponse
    ├── Table.ts                          <- TableResponse
    └── Customer.ts                       <- CustomerResponse
```

### Feature: Orders (Sprint 4)

```
features/orders/
├── components/
│   ├── OrderList.tsx                     <- Lista de pedidos
│   ├── OrderCard.tsx                     <- Card de pedido
│   ├── OrderDetail.tsx                   <- Detalle completo
│   └── OrderFilters.tsx                  <- Filtros por fecha/estado
├── hooks/
│   ├── useOrdersQuery.ts                 <- GET /api/v1/orders
│   ├── useOrderQuery.ts                  <- GET /api/v1/orders/{id}
│   └── useCancelOrderMutation.ts         <- POST /api/v1/orders/{id}/cancel
└── types/
    └── Order.ts                          <- OrderResponse, OrderItemResponse
```

### Feature: Products (Sprint 5)

```
features/products/
├── components/
│   ├── ProductList.tsx                   <- Lista de productos
│   ├── ProductForm.tsx                   <- Formulario crear/editar
│   ├── VariantList.tsx                   <- Lista de variantes
│   ├── VariantForm.tsx                   <- Formulario de variante
│   ├── ModifierGroupList.tsx             <- Lista de modifier groups
│   └── ModifierGroupForm.tsx             <- Formulario de modifier group
├── hooks/
│   ├── useProductsQuery.ts               <- GET /api/v1/products
│   ├── useCreateProductMutation.ts       <- POST /api/v1/products
│   ├── useUpdateProductMutation.ts       <- PUT /api/v1/products/{id}
│   └── useDeleteProductMutation.ts       <- DELETE /api/v1/products/{id}
└── types/
    └── Product.ts                        <- ProductResponse, ProductCreateRequest
```

### Feature: Branches (Sprint 5)

```
features/branches/
├── components/
│   ├── BranchList.tsx                    <- Lista de sucursales
│   ├── BranchForm.tsx                    <- Formulario sucursal
│   ├── BranchSelector.tsx                <- Selector de sucursal activa
│   ├── AreaList.tsx                      <- Lista de areas
│   ├── AreaForm.tsx                      <- Formulario area
│   ├── TableList.tsx                     <- Lista de mesas
│   └── TableForm.tsx                     <- Formulario mesa
├── hooks/
│   ├── useBranchesQuery.ts               <- GET /api/v1/branches
│   ├── useAreasQuery.ts                  <- GET /api/v1/branches/{id}/areas
│   ├── useTablesQuery.ts                 <- GET /api/v1/areas/{id}/tables
│   └── ...mutations
└── stores/
    └── branchStore.ts                    <- Sucursal activa
```

---

## Sprint 1: Infraestructura y Catalogo POS

**Duracion:** 2.5 dias | **Objetivo:** Pantalla de catalogo de productos funcional

### [FRONTEND] Tarea 1.1: Tipos TypeScript — Menu y Products

**Prioridad:** Alta | **Dependencias:** Ninguna

Definir tipos para consumir API de menu.

**Criterios de Aceptacion:**
- [ ] `features/pos/types/Menu.ts`: tipos `MenuResponse`, `MenuCategoryItem`, `MenuProductItem`, `MenuVariantItem`, `MenuModifierGroupItem`, `MenuModifierItem`, `MenuComboItem`
- [ ] Todos los tipos coinciden exactamente con DTOs del backend (referencia: Phase 1.1 MenuResponse)
- [ ] Tipos exportados correctamente
- [ ] No requiere tests (solo tipos)

**Archivos:**
- `frontend/src/features/pos/types/Menu.ts`

---

### [FRONTEND] Tarea 1.2: Menu API y Query Hook

**Prioridad:** Alta | **Dependencias:** 1.1

Crear funciones de API y hook de TanStack Query.

**Criterios de Aceptacion:**
- [ ] `features/pos/api/menuApi.ts`: funcion `getMenu()` que llama `GET /api/v1/menu`
- [ ] `features/pos/hooks/useMenuQuery.ts`: hook `useMenuQuery()` usando `useQuery` de TanStack
- [ ] Query key: `['menu']`
- [ ] Cache time: 5 minutos (productos cambian poco)
- [ ] Retry: 2 intentos
- [ ] Tests con Vitest + MSW: 4 tests (loading, success, error, cached)

**Archivos:**
- `frontend/src/features/pos/api/menuApi.ts`
- `frontend/src/features/pos/hooks/useMenuQuery.ts`
- `frontend/src/features/pos/hooks/__tests__/useMenuQuery.test.ts`

---

### [FRONTEND] Tarea 1.3: ProductCard Component

**Prioridad:** Alta | **Dependencias:** 1.1

Card individual de producto para el grid.

**Criterios de Aceptacion:**
- [ ] `ProductCard.tsx`: MUI Card que muestra `name`, `basePrice`, `imageUrl` (placeholder si null), `isAvailable` badge
- [ ] Props: `product: MenuProductItem`, `onClick: () => void`
- [ ] Si `isAvailable == false`, mostrar overlay "No disponible"
- [ ] Hover effect (elevation change)
- [ ] Responsive: height fijo para consistencia en grid
- [ ] Tests con RTL: 5 tests (render, click, unavailable badge, placeholder image)

**Archivos:**
- `frontend/src/features/pos/components/ProductCard.tsx`
- `frontend/src/features/pos/components/__tests__/ProductCard.test.tsx`

---

### [FRONTEND] Tarea 1.4: ProductCatalog Component

**Prioridad:** Alta | **Dependencias:** 1.2, 1.3

Grid de productos organizado por categorias.

**Criterios de Aceptacion:**
- [ ] `ProductCatalog.tsx`: consume `useMenuQuery()`, muestra loading spinner, error alert, o grid de productos
- [ ] Productos agrupados por categoria (usar `MenuCategoryItem`)
- [ ] Tabs de MUI para navegar categorias
- [ ] Grid responsive: 4 cols desktop, 2 tablet, 1 mobile (MUI Grid2)
- [ ] Click en ProductCard abre modal de detalle (implementar en Sprint 2)
- [ ] Tests con RTL + MSW: 6 tests (loading, error, render categorias, render productos, click producto)

**Archivos:**
- `frontend/src/features/pos/components/ProductCatalog.tsx`
- `frontend/src/features/pos/components/__tests__/ProductCatalog.test.tsx`

---

### [FRONTEND] Tarea 1.5: Ruta y Pantalla /pos/catalog

**Prioridad:** Alta | **Dependencias:** 1.4

Crear ruta y pantalla de catalogo.

**Criterios de Aceptacion:**
- [ ] Crear `frontend/src/routes/posRoutes.tsx` con ruta `/pos/catalog`
- [ ] Pantalla `CatalogPage.tsx` que renderiza `ProductCatalog` component
- [ ] Agregar ruta protegida en `App.tsx` (requiere rol CASHIER+)
- [ ] Agregar link en Sidebar (icono Shopping Cart, label "Catalogo")
- [ ] Tests de navegacion: 3 tests (ruta accesible, requiere auth, link en sidebar)

**Archivos:**
- `frontend/src/routes/posRoutes.tsx`
- `frontend/src/features/pos/pages/CatalogPage.tsx`
- `frontend/src/components/layout/Sidebar.tsx` (modificar)
- `frontend/src/routes/posRoutes.test.tsx`

---

## Sprint 2: Producto Detail + Cart Store

**Duracion:** 2.5 dias | **Objetivo:** Modal de producto con variantes/modifiers + carrito funcional

### [FRONTEND] Tarea 2.1: Cart Store (Zustand)

**Prioridad:** Alta | **Dependencias:** 1.1

Store para manejar items en carrito.

**Criterios de Aceptacion:**
- [ ] `features/pos/stores/cartStore.ts`: Zustand store con estado `items: CartItem[]`, `serviceType`, `tableId`, `customerId`
- [ ] Tipo `CartItem`: `productId`, `variantId?`, `comboId?`, `productName`, `variantName?`, `quantity`, `unitPrice`, `selectedModifiers: SelectedModifier[]`, `lineTotal`
- [ ] Tipo `SelectedModifier`: `modifierId`, `modifierName`, `priceAdjustment`
- [ ] Actions: `addItem(item)`, `removeItem(index)`, `updateQuantity(index, qty)`, `clearCart()`, `setServiceDetails(type, tableId?, customerId?)`
- [ ] Computed: `subtotal`, `tax`, `total` (usando taxRate de tenant — obtener de authStore)
- [ ] Persist en sessionStorage (recuperar si refresh)
- [ ] Tests unitarios: 10 tests (add, remove, update qty, totals, persist)

**Archivos:**
- `frontend/src/features/pos/stores/cartStore.ts`
- `frontend/src/features/pos/stores/__tests__/cartStore.test.ts`

---

### [FRONTEND] Tarea 2.2: VariantSelector Component

**Prioridad:** Alta | **Dependencias:** 1.1

Radio buttons para seleccionar variante de producto.

**Criterios de Aceptacion:**
- [ ] `VariantSelector.tsx`: MUI RadioGroup para seleccionar variante
- [ ] Props: `variants: MenuVariantItem[]`, `selectedId: string | null`, `onChange: (id: string) => void`
- [ ] Mostrar nombre de variante + precio efectivo (base + adjustment)
- [ ] Pre-seleccionar variante default (`isDefault == true`)
- [ ] Tests con RTL: 5 tests (render, default selected, onChange, precio display)

**Archivos:**
- `frontend/src/features/pos/components/VariantSelector.tsx`
- `frontend/src/features/pos/components/__tests__/VariantSelector.test.tsx`

---

### [FRONTEND] Tarea 2.3: ModifierGroup Component

**Prioridad:** Alta | **Dependencias:** 1.1

Checkbox/Radio para modifiers de un grupo.

**Criterios de Aceptacion:**
- [ ] `ModifierGroup.tsx`: renderiza lista de modifiers con Checkbox (si max > 1) o Radio (si max == 1)
- [ ] Props: `group: MenuModifierGroupItem`, `selectedIds: string[]`, `onChange: (ids: string[]) => void`
- [ ] Validar `minSelections` y `maxSelections` (deshabilitar checkboxes si max alcanzado)
- [ ] Mostrar `isRequired` badge si aplica
- [ ] Mostrar precio de cada modifier (`+$10`, `-$5`, etc.)
- [ ] Tests con RTL: 8 tests (render, select, max limit, required badge)

**Archivos:**
- `frontend/src/features/pos/components/ModifierGroup.tsx`
- `frontend/src/features/pos/components/__tests__/ModifierGroup.test.tsx`

---

### [FRONTEND] Tarea 2.4: ProductDetailModal Component

**Prioridad:** Alta | **Dependencias:** 2.1, 2.2, 2.3

Modal completo de producto con variantes y modifiers.

**Criterios de Aceptacion:**
- [ ] `ProductDetailModal.tsx`: MUI Dialog fullWidth maxWidth="md"
- [ ] Props: `product: MenuProductItem | null`, `open: boolean`, `onClose: () => void`
- [ ] Renderizar imagen, nombre, descripcion
- [ ] Si `productType == VARIANT`, renderizar `VariantSelector`
- [ ] Renderizar lista de `ModifierGroup` components (map sobre `modifierGroups`)
- [ ] Input de cantidad (NumberInput con +/- buttons)
- [ ] Calcular `lineTotal` en tiempo real (base/variant price + modifiers * qty)
- [ ] Boton "Agregar al Carrito": valida selecciones, llama `cartStore.addItem()`, cierra modal
- [ ] Validaciones: variante requerida si VARIANT, modifiers required cumplen minSelections
- [ ] Tests con RTL: 10 tests (render, variant selection, modifier selection, add to cart, validation errors)

**Archivos:**
- `frontend/src/features/pos/components/ProductDetailModal.tsx`
- `frontend/src/features/pos/components/__tests__/ProductDetailModal.test.tsx`

---

### [FRONTEND] Tarea 2.5: Integrar Modal en ProductCatalog

**Prioridad:** Alta | **Dependencias:** 2.4

Abrir modal al hacer click en ProductCard.

**Criterios de Aceptacion:**
- [ ] `ProductCatalog.tsx`: agregar estado `selectedProduct: MenuProductItem | null`
- [ ] Click en ProductCard setea `selectedProduct`
- [ ] Renderizar `<ProductDetailModal product={selectedProduct} open={!!selectedProduct} onClose={() => setSelectedProduct(null)} />`
- [ ] Tests actualizados: 2 tests nuevos (click abre modal, cerrar modal)

**Archivos:**
- `frontend/src/features/pos/components/ProductCatalog.tsx` (modificar)
- `frontend/src/features/pos/components/__tests__/ProductCatalog.test.tsx` (expandir)

---

## Sprint 3: Carrito y Flujo de Servicio

**Duracion:** 2.5 dias | **Objetivo:** Carrito funcional + selector de tipo de servicio/mesa/cliente

### [FRONTEND] Tarea 3.1: CartItem Component

**Prioridad:** Alta | **Dependencias:** 2.1

Item individual en carrito con qty controls.

**Criterios de Aceptacion:**
- [ ] `CartItem.tsx`: MUI ListItem que muestra `productName`, `variantName?`, modifiers seleccionados, `quantity`, `lineTotal`
- [ ] Props: `item: CartItem`, `index: number`, `onUpdateQty: (index, qty) => void`, `onRemove: (index) => void`
- [ ] Mostrar modifiers como chips pequenos debajo del nombre
- [ ] Botones +/- para cambiar qty (min 1)
- [ ] Boton delete (IconButton con trash icon)
- [ ] Tests con RTL: 6 tests (render, update qty, remove, modifiers display)

**Archivos:**
- `frontend/src/features/pos/components/CartItem.tsx`
- `frontend/src/features/pos/components/__tests__/CartItem.test.tsx`

---

### [FRONTEND] Tarea 3.2: Cart Component

**Prioridad:** Alta | **Dependencies:** 3.1, 2.1

Lista de items del carrito + totales.

**Criterios de Aceptacion:**
- [ ] `Cart.tsx`: consume `cartStore`, renderiza lista de `CartItem` components
- [ ] Mostrar subtotal, tax, total en la parte inferior (MUI Card con dividers)
- [ ] Si carrito vacio, mostrar empty state (ilustracion + texto)
- [ ] Boton "Limpiar Carrito" (solo si items > 0)
- [ ] Boton "Continuar" (navega a selector de servicio)
- [ ] Tests con RTL + MSW: 8 tests (empty state, render items, update qty, remove, totals calculation, clear cart)

**Archivos:**
- `frontend/src/features/pos/components/Cart.tsx`
- `frontend/src/features/pos/components/__tests__/Cart.test.tsx`

---

### [FRONTEND] Tarea 3.3: ServiceTypeSelector Component

**Prioridad:** Alta | **Dependencias:** 2.1

Selector de tipo de servicio (DINE_IN/COUNTER/etc).

**Criterios de Aceptacion:**
- [ ] `ServiceTypeSelector.tsx`: Grid de 4 Cards grandes con iconos para cada service type
- [ ] Props: `onSelect: (type: ServiceType) => void`
- [ ] Cards: DINE_IN (icon: Restaurant), COUNTER (icon: Store), DELIVERY (icon: DeliveryDining), TAKEOUT (icon: TakeoutDining)
- [ ] Click en card guarda en `cartStore.setServiceDetails(type)` y navega a siguiente paso
- [ ] Tests con RTL: 5 tests (render, click cada tipo, navigation)

**Archivos:**
- `frontend/src/features/pos/components/ServiceTypeSelector.tsx`
- `frontend/src/features/pos/components/__tests__/ServiceTypeSelector.test.tsx`

---

### [FRONTEND] Tarea 3.4: TableSelector Component

**Prioridad:** Alta | **Dependencias:** 2.1

Grid de mesas disponibles (solo para DINE_IN).

**Criterios de Aceptacion:**
- [ ] `TableSelector.tsx`: consume `useTablesQuery()` (GET /api/v1/areas/{areaId}/tables)
- [ ] Tabs de MUI para navegar areas
- [ ] Grid de Cards de mesas (numero, capacity, status badge)
- [ ] Solo mesas AVAILABLE son clickeables
- [ ] Click en mesa guarda `tableId` en cartStore y navega a /pos/cart
- [ ] Tests con RTL + MSW: 8 tests (loading, render areas, render tables, only available clickable, select table)

**Archivos:**
- `frontend/src/features/pos/components/TableSelector.tsx`
- `frontend/src/features/pos/hooks/useTablesQuery.ts`
- `frontend/src/features/pos/api/tableApi.ts`
- `frontend/src/features/pos/components/__tests__/TableSelector.test.tsx`

---

### [FRONTEND] Tarea 3.5: CustomerSelector Component

**Prioridad:** Alta | **Dependencias:** 2.1

Search + crear cliente (solo para DELIVERY/TAKEOUT).

**Criterios de Aceptacion:**
- [ ] `CustomerSelector.tsx`: MUI TextField con search + lista de resultados
- [ ] Consume `useCustomersQuery(searchTerm)` con debounce de 300ms
- [ ] Boton "Crear Cliente Nuevo" abre dialog con formulario simple (nombre, phone, address)
- [ ] Click en cliente guarda `customerId` en cartStore y navega a /pos/cart
- [ ] Tests con RTL + MSW: 10 tests (search, debounce, select customer, create new customer, validation)

**Archivos:**
- `frontend/src/features/pos/components/CustomerSelector.tsx`
- `frontend/src/features/pos/hooks/useCustomersQuery.ts`
- `frontend/src/features/pos/hooks/useCreateCustomerMutation.ts`
- `frontend/src/features/pos/api/customerApi.ts`
- `frontend/src/features/pos/components/__tests__/CustomerSelector.test.tsx`

---

### [FRONTEND] Tarea 3.6: Rutas y Flujo de Servicio

**Prioridad:** Alta | **Dependencias:** 3.3, 3.4, 3.5

Conectar pantallas en flujo completo.

**Criterios de Aceptacion:**
- [ ] Ruta `/pos/new` renderiza `ServiceTypeSelector`
- [ ] Ruta `/pos/new/table` renderiza `TableSelector` (solo accesible si serviceType == DINE_IN)
- [ ] Ruta `/pos/new/customer` renderiza `CustomerSelector` (solo accesible si serviceType == DELIVERY o TAKEOUT)
- [ ] Ruta `/pos/cart` renderiza `Cart` component
- [ ] Navegacion condicional basada en serviceType
- [ ] Tests de navegacion: 6 tests (flujos completos para cada service type)

**Archivos:**
- `frontend/src/routes/posRoutes.tsx` (expandir)
- `frontend/src/features/pos/pages/NewOrderPage.tsx`
- `frontend/src/features/pos/pages/TableSelectionPage.tsx`
- `frontend/src/features/pos/pages/CustomerSelectionPage.tsx`
- `frontend/src/features/pos/pages/CartPage.tsx`

---

## Sprint 4: Order Creation y Payment

**Duracion:** 2.5 dias | **Objetivo:** Crear orden, pagar y completar flujo

### [FRONTEND] Tarea 4.1: Tipos TypeScript — Orders y Payments

**Prioridad:** Alta | **Dependencias:** Ninguna

Definir tipos para ordenes y pagos.

**Criterios de Aceptacion:**
- [ ] `features/pos/types/Order.ts`: tipos `OrderCreateRequest`, `OrderItemRequest`, `OrderItemModifierRequest`, `OrderResponse`, `OrderItemResponse`, `ServiceType`, `OrderStatus`, `PaymentRequest`, `PaymentResponse`, `PaymentMethod`
- [ ] Todos coinciden con DTOs del backend (referencia: Phase 1.3)
- [ ] No requiere tests

**Archivos:**
- `frontend/src/features/pos/types/Order.ts`

---

### [FRONTEND] Tarea 4.2: Order API y Mutations

**Prioridad:** Alta | **Dependencias:** 4.1, 2.1

Funciones de API y hooks de TanStack Query para ordenes.

**Criterios de Aceptacion:**
- [ ] `features/pos/api/orderApi.ts`: funciones `createOrder(request)`, `submitOrder(orderId)`, `getOrder(orderId)`
- [ ] `features/pos/hooks/useCreateOrderMutation.ts`: hook usando `useMutation`
- [ ] `features/pos/hooks/useSubmitOrderMutation.ts`: hook para enviar orden a cocina
- [ ] `features/pos/hooks/useRegisterPaymentMutation.ts`: hook para POST /api/v1/payments
- [ ] Invalidar query cache de orders al crear/pagar
- [ ] Tests con Vitest + MSW: 8 tests (create success, create error, submit, payment success)

**Archivos:**
- `frontend/src/features/pos/api/orderApi.ts`
- `frontend/src/features/pos/hooks/useCreateOrderMutation.ts`
- `frontend/src/features/pos/hooks/useSubmitOrderMutation.ts`
- `frontend/src/features/pos/hooks/useRegisterPaymentMutation.ts`
- `frontend/src/features/pos/hooks/__tests__/orderMutations.test.ts`

---

### [FRONTEND] Tarea 4.3: Logica de Crear Orden desde Cart

**Prioridad:** Alta | **Dependencias:** 4.2

Transformar items de cartStore a OrderCreateRequest.

**Criterios de Aceptacion:**
- [ ] Funcion `buildOrderRequest(cartStore): OrderCreateRequest` en utils
- [ ] Mapea `CartItem[]` a `OrderItemRequest[]` con estructura correcta (productId, variantId, modifiers)
- [ ] Incluye `branchId` (desde branchStore), `serviceType`, `tableId?`, `customerId?`
- [ ] Tests unitarios: 6 tests (diferentes service types, con/sin modifiers, con/sin variantes)

**Archivos:**
- `frontend/src/features/pos/utils/orderUtils.ts`
- `frontend/src/features/pos/utils/__tests__/orderUtils.test.ts`

---

### [FRONTEND] Tarea 4.4: PaymentForm Component

**Prioridad:** Alta | **Dependencias:** 4.2

Formulario de pago en efectivo.

**Criterios de Aceptacion:**
- [ ] `PaymentForm.tsx`: formulario con input de "Monto Recibido" (NumberInput)
- [ ] Props: `orderTotal: number`, `onSubmit: (amount: number) => void`
- [ ] Validacion: amount >= orderTotal (mostrar error si no)
- [ ] Mostrar "Cambio a Devolver" calculado automaticamente (amount - total)
- [ ] Botones rapidos: "Exacto", "$100", "$200", "$500"
- [ ] Boton "Registrar Pago" disabled si validacion falla
- [ ] Tests con RTL: 8 tests (render, validation, quick buttons, calculate change, submit)

**Archivos:**
- `frontend/src/features/pos/components/PaymentForm.tsx`
- `frontend/src/features/pos/components/__tests__/PaymentForm.test.tsx`

---

### [FRONTEND] Tarea 4.5: Flujo Completo — Cart → Orden → Pago

**Prioridad:** Alta | **Dependencias:** 4.3, 4.4

Integrar creacion de orden y pago.

**Criterios de Aceptacion:**
- [ ] Boton "Enviar Orden" en Cart.tsx:
  - Llama `useCreateOrderMutation` con `buildOrderRequest(cartStore)`
  - Si success, guarda `orderId` en posStore y navega a `/pos/payment`
  - Si error, muestra Snackbar con mensaje de error
- [ ] Pantalla `PaymentPage.tsx`:
  - Consume `orderId` de posStore
  - Llama `useOrderQuery(orderId)` para obtener detalles de orden
  - Renderiza resumen de orden + `PaymentForm`
  - Submit de PaymentForm llama `useRegisterPaymentMutation`
  - Si payment success, navega a `/pos/confirmation`
- [ ] Pantalla `OrderConfirmationPage.tsx`:
  - Muestra "Pedido Completado" con numero de orden
  - Boton "Nueva Venta" limpia cartStore y navega a `/pos/new`
- [ ] Tests end-to-end con MSW: 6 tests (flujo completo desde cart hasta confirmation)

**Archivos:**
- `frontend/src/features/pos/components/Cart.tsx` (modificar)
- `frontend/src/features/pos/pages/PaymentPage.tsx`
- `frontend/src/features/pos/pages/OrderConfirmationPage.tsx`
- `frontend/src/features/pos/stores/posStore.ts` (guardar orderId actual)
- `frontend/src/features/pos/__tests__/posFlow.test.tsx`

---

## Sprint 5: Admin — Products y Branches

**Duracion:** 2.5 dias | **Objetivo:** CRUD de productos y sucursales

### [FRONTEND] Tarea 5.1: ProductList Component (Admin)

**Prioridad:** Alta | **Dependencias:** Ninguna

Lista de productos para admin.

**Criterios de Aceptacion:**
- [ ] `features/products/components/ProductList.tsx`: consume `useProductsQuery()` (GET /api/v1/products)
- [ ] MUI DataGrid con columnas: imagen, nombre, categoria, precio, estado (activo/inactivo)
- [ ] Filtros: categoria, busqueda por nombre, mostrar inactivos
- [ ] Paginacion server-side (params: page, size, search, categoryId)
- [ ] Botones de accion por fila: Editar, Eliminar
- [ ] Boton "Nuevo Producto" navega a `/admin/products/new`
- [ ] Tests con RTL + MSW: 8 tests (render, pagination, filters, search, delete confirmation)

**Archivos:**
- `frontend/src/features/products/components/ProductList.tsx`
- `frontend/src/features/products/hooks/useProductsQuery.ts`
- `frontend/src/features/products/api/productApi.ts`
- `frontend/src/features/products/components/__tests__/ProductList.test.tsx`

---

### [FRONTEND] Tarea 5.2: ProductForm Component

**Prioridad:** Alta | **Dependencias:** Ninguna

Formulario crear/editar producto.

**Criterios de Aceptacion:**
- [ ] `features/products/components/ProductForm.tsx`: formulario completo con validacion
- [ ] Props: `productId?: string` (si presente, modo edit)
- [ ] Campos: nombre, descripcion, categoria (Select), SKU, precio base, precio costo, tipo (SIMPLE/VARIANT/COMBO), imagen URL, ordenamiento
- [ ] Si tipo == VARIANT, mostrar sub-formulario para agregar variantes (lista dinamica)
- [ ] Validaciones: nombre requerido, precio >= 0, SKU unico (validar en backend)
- [ ] Submit llama `useCreateProductMutation` o `useUpdateProductMutation`
- [ ] Tests con RTL: 10 tests (render, validation, create, edit, add variant)

**Archivos:**
- `frontend/src/features/products/components/ProductForm.tsx`
- `frontend/src/features/products/hooks/useCreateProductMutation.ts`
- `frontend/src/features/products/hooks/useUpdateProductMutation.ts`
- `frontend/src/features/products/components/__tests__/ProductForm.test.tsx`

---

### [FRONTEND] Tarea 5.3: BranchList y BranchForm Components

**Prioridad:** Media | **Dependencias:** Ninguna

CRUD de sucursales (solo OWNER).

**Criterios de Aceptacion:**
- [ ] `features/branches/components/BranchList.tsx`: lista simple de sucursales con botones editar/eliminar
- [ ] `features/branches/components/BranchForm.tsx`: formulario con campos: nombre, direccion, ciudad, telefono, email
- [ ] Validaciones: nombre requerido
- [ ] Tests con RTL + MSW: 8 tests (render list, create, edit, delete)

**Archivos:**
- `frontend/src/features/branches/components/BranchList.tsx`
- `frontend/src/features/branches/components/BranchForm.tsx`
- `frontend/src/features/branches/hooks/useBranchesQuery.ts`
- `frontend/src/features/branches/hooks/useBranchMutations.ts`
- `frontend/src/features/branches/api/branchApi.ts`
- Tests

---

### [FRONTEND] Tarea 5.4: BranchSelector Component (TopBar)

**Prioridad:** Alta | **Dependencias:** 5.3

Selector de sucursal activa en TopBar.

**Criterios de Aceptacion:**
- [ ] `features/branches/components/BranchSelector.tsx`: MUI Select en TopBar
- [ ] Consume `useBranchesQuery()`, muestra lista de sucursales
- [ ] Seleccion guarda en `branchStore` (localStorage)
- [ ] Si solo hay 1 branch, auto-seleccionar
- [ ] Tests con RTL: 6 tests (render, select, persist, auto-select single)

**Archivos:**
- `frontend/src/features/branches/components/BranchSelector.tsx`
- `frontend/src/features/branches/stores/branchStore.ts`
- `frontend/src/components/layout/TopBar.tsx` (modificar: agregar BranchSelector)
- Tests

---

### [FRONTEND] Tarea 5.5: Rutas Admin + Proteccion de Rol

**Prioridad:** Alta | **Dependencias:** 5.1, 5.2, 5.3

Crear rutas de admin protegidas por rol.

**Criterios de Aceptacion:**
- [ ] Ruta `/admin/products` renderiza `ProductListPage` (requiere MANAGER+)
- [ ] Ruta `/admin/products/new` renderiza `ProductFormPage` (requiere MANAGER+)
- [ ] Ruta `/admin/products/:id/edit` renderiza `ProductFormPage` con productId (requiere MANAGER+)
- [ ] Ruta `/admin/branches` renderiza `BranchListPage` (requiere OWNER)
- [ ] Actualizar Sidebar con links de admin (solo visibles para roles apropiados)
- [ ] Tests de navegacion: 6 tests (acceso con roles correctos, 403 si rol insuficiente)

**Archivos:**
- `frontend/src/routes/adminRoutes.tsx`
- `frontend/src/features/products/pages/ProductListPage.tsx`
- `frontend/src/features/products/pages/ProductFormPage.tsx`
- `frontend/src/features/branches/pages/BranchListPage.tsx`
- `frontend/src/components/layout/Sidebar.tsx` (modificar)

---

## Sprint 6: Order Management y Polish

**Duracion:** 2 dias | **Objetivo:** Vista de pedidos del dia + refinamiento final

### [FRONTEND] Tarea 6.1: OrderList Component

**Prioridad:** Alta | **Dependencias:** 4.1

Lista de pedidos del dia.

**Criterios de Aceptacion:**
- [ ] `features/orders/components/OrderList.tsx`: consume `useOrdersQuery()` (GET /api/v1/orders)
- [ ] Filtros: fecha (default=hoy), estado (OPEN/SUBMITTED/etc.), branch
- [ ] MUI Cards de ordenes con: orderNumber, dailySequence, serviceType, status badge, total, timestamp
- [ ] Click en orden navega a `/orders/{id}`
- [ ] Auto-refresh cada 30 segundos (refetch interval)
- [ ] Tests con RTL + MSW: 8 tests (render, filters, click, auto-refresh)

**Archivos:**
- `frontend/src/features/orders/components/OrderList.tsx`
- `frontend/src/features/orders/hooks/useOrdersQuery.ts`
- `frontend/src/features/orders/api/orderApi.ts`
- `frontend/src/features/orders/components/__tests__/OrderList.test.tsx`

---

### [FRONTEND] Tarea 6.2: OrderDetail Component

**Prioridad:** Alta | **Dependencias:** 4.1

Detalle completo de orden.

**Criterios de Aceptacion:**
- [ ] `features/orders/components/OrderDetail.tsx`: consume `useOrderQuery(orderId)`
- [ ] Mostrar todos los campos: orderNumber, serviceType, status, items con modifiers, totales, pagos
- [ ] Si status == OPEN, mostrar boton "Cancelar Orden" (solo MANAGER+)
- [ ] Si status == SUBMITTED, mostrar estado de KDS de cada item
- [ ] Tests con RTL + MSW: 6 tests (render, cancel order, payments display)

**Archivos:**
- `frontend/src/features/orders/components/OrderDetail.tsx`
- `frontend/src/features/orders/hooks/useOrderQuery.ts`
- `frontend/src/features/orders/hooks/useCancelOrderMutation.ts`
- `frontend/src/features/orders/components/__tests__/OrderDetail.test.tsx`

---

### [FRONTEND] Tarea 6.3: Rutas Orders

**Prioridad:** Alta | **Dependencias:** 6.1, 6.2

Crear rutas de ordenes.

**Criterios de Aceptacion:**
- [ ] Ruta `/orders` renderiza `OrderListPage` (requiere CASHIER+)
- [ ] Ruta `/orders/:id` renderiza `OrderDetailPage` (requiere CASHIER+)
- [ ] CASHIER solo ve ordenes propias (filtro client-side o server-side)
- [ ] Agregar link en Sidebar (icono Receipt, label "Pedidos")
- [ ] Tests de navegacion: 4 tests

**Archivos:**
- `frontend/src/routes/orderRoutes.tsx`
- `frontend/src/features/orders/pages/OrderListPage.tsx`
- `frontend/src/features/orders/pages/OrderDetailPage.tsx`
- `frontend/src/components/layout/Sidebar.tsx` (modificar)

---

### [QA] Tarea 6.4: Tests End-to-End — Flujo Completo POS

**Prioridad:** Alta | **Dependencias:** Sprint 1-5

Suite de tests que ejercitan el flujo completo.

**Criterios de Aceptacion:**
- [ ] Test: Usuario CASHIER crea orden COUNTER con 2 productos, paga, completa
- [ ] Test: Usuario CASHIER crea orden DINE_IN, selecciona mesa, agrega productos con modifiers, paga
- [ ] Test: Usuario CASHIER crea orden DELIVERY, busca cliente, agrega productos, paga
- [ ] Test: Usuario MANAGER ve todas las ordenes del dia, cancela una
- [ ] Test: Usuario intenta acceder a /admin/products sin rol MANAGER — recibe 403
- [ ] 10 tests end-to-end pasando

**Archivos:**
- `frontend/src/__tests__/e2e/posFlow.test.tsx`

---

### [QA] Tarea 6.5: Auditoria de Accesibilidad

**Prioridad:** Media | **Dependencias:** Sprint 1-6

Asegurar accesibilidad basica.

**Criterios de Aceptacion:**
- [ ] Todos los botones tienen `aria-label` descriptivo
- [ ] Forms tienen `<label>` asociado a inputs
- [ ] Modals tienen `aria-labelledby` y `aria-describedby`
- [ ] Focus management correcto (modal trap, navigation keyboard)
- [ ] Contrastes de color cumplen WCAG AA (verificar con herramienta)
- [ ] No requiere tests automaticos — inspeccion manual con axe DevTools

**Archivos:**
- Multiples componentes (modificar segun hallazgos)

---

### [QA] Tarea 6.6: Performance Audit y Optimizacion

**Prioridad:** Media | **Dependencias:** Sprint 1-6

Asegurar performance aceptable.

**Criterios de Aceptacion:**
- [ ] Lighthouse score Desktop: Performance > 90
- [ ] Bundle size < 600KB gzipped (verificar con `npm run build`)
- [ ] Lazy load de rutas de admin (React.lazy + Suspense)
- [ ] Memoization de componentes pesados (React.memo donde aplique)
- [ ] Debounce de search inputs (ya implementado en CustomerSelector)
- [ ] No requiere tests automaticos — mediciones con Lighthouse

**Archivos:**
- `frontend/src/routes/adminRoutes.tsx` (agregar lazy loading)
- Componentes pesados (agregar React.memo)

---

## Endpoints Consumidos (ya disponibles del backend)

| Metodo | Endpoint | Usado en |
|--------|----------|----------|
| `GET` | `/api/v1/menu` | ProductCatalog |
| `GET` | `/api/v1/areas/{id}/tables` | TableSelector |
| `GET` | `/api/v1/customers?search=...` | CustomerSelector |
| `POST` | `/api/v1/customers` | CustomerSelector (crear) |
| `POST` | `/api/v1/orders` | Cart (crear orden) |
| `GET` | `/api/v1/orders/{id}` | PaymentPage, OrderDetail |
| `GET` | `/api/v1/orders` | OrderList |
| `POST` | `/api/v1/orders/{id}/submit` | Cart (enviar a cocina) |
| `POST` | `/api/v1/orders/{id}/cancel` | OrderDetail (MANAGER) |
| `POST` | `/api/v1/payments` | PaymentForm |
| `GET` | `/api/v1/orders/{id}/payments` | OrderDetail |
| `GET` | `/api/v1/products` | ProductList (admin) |
| `POST` | `/api/v1/products` | ProductForm (crear) |
| `PUT` | `/api/v1/products/{id}` | ProductForm (editar) |
| `DELETE` | `/api/v1/products/{id}` | ProductList (eliminar) |
| `GET` | `/api/v1/branches` | BranchList, BranchSelector |
| `POST` | `/api/v1/branches` | BranchForm (crear) |
| `PUT` | `/api/v1/branches/{id}` | BranchForm (editar) |
| `DELETE` | `/api/v1/branches/{id}` | BranchList (eliminar) |

**Total: 18 endpoints consumidos** (de 28 disponibles — algunos quedan para futuras phases)

---

## Resumen de Tests Esperados

| Sprint | Tipo | Tests Nuevos | Tests Acumulados (Frontend) |
|--------|------|-------------|------------------------------|
| Sprint 1 | Unit + RTL | ~23 | ~61 |
| Sprint 2 | Unit + RTL | ~33 | ~94 |
| Sprint 3 | Unit + RTL | ~37 | ~131 |
| Sprint 4 | Unit + RTL | ~28 | ~159 |
| Sprint 5 | Unit + RTL | ~32 | ~191 |
| Sprint 6 | Unit + E2E | ~24 | ~215 |

*Los conteos son estimados. La meta es llegar a Phase 1.4 completo con ~215 tests frontend pasando (~158 tests totales incluyendo 38 de Phase 0.4).*

---

## Notas de Implementacion para el Desarrollador

### Patron de API calls con TanStack Query

```typescript
// features/pos/api/menuApi.ts
import { axiosInstance } from '@/lib/api/axiosInstance';
import type { MenuResponse } from '../types/Menu';

export const menuApi = {
  getMenu: async (): Promise<MenuResponse> => {
    const response = await axiosInstance.get<MenuResponse>('/api/v1/menu');
    return response.data;
  }
};

// features/pos/hooks/useMenuQuery.ts
import { useQuery } from '@tanstack/react-query';
import { menuApi } from '../api/menuApi';

export const useMenuQuery = () => {
  return useQuery({
    queryKey: ['menu'],
    queryFn: menuApi.getMenu,
    staleTime: 5 * 60 * 1000, // 5 minutos
    retry: 2
  });
};
```

### Zustand store con persist

```typescript
// features/pos/stores/cartStore.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface CartState {
  items: CartItem[];
  addItem: (item: CartItem) => void;
  clearCart: () => void;
}

export const useCartStore = create<CartState>()(
  persist(
    (set) => ({
      items: [],
      addItem: (item) => set((state) => ({ items: [...state.items, item] })),
      clearCart: () => set({ items: [] })
    }),
    {
      name: 'cart-storage', // key en sessionStorage
      storage: sessionStorage
    }
  )
);
```

### Protected route con rol requerido

```typescript
// components/auth/ProtectedRoute.tsx (expandir existente de Phase 0.4)
import { useAuthStore } from '@/features/auth/stores/authStore';

export const ProtectedRoute = ({
  requiredRole
}: {
  requiredRole?: 'OWNER' | 'MANAGER' | 'CASHIER'
}) => {
  const user = useAuthStore((state) => state.user);

  if (!user) {
    return <Navigate to="/login" />;
  }

  if (requiredRole && !hasRole(user.role, requiredRole)) {
    return <Navigate to="/403" />;
  }

  return <Outlet />;
};

function hasRole(userRole: string, requiredRole: string): boolean {
  const hierarchy = { OWNER: 3, MANAGER: 2, CASHIER: 1 };
  return hierarchy[userRole] >= hierarchy[requiredRole];
}
```

---

## Definition of Done

**Cada sprint se considera completo cuando:**

- [ ] Todas las tareas del sprint estan marcadas como completadas
- [ ] Todos los tests frontend del sprint pasan (`npm test`)
- [ ] No hay regresiones en tests de sprints anteriores
- [ ] Build de produccion pasa sin warnings (`npm run build`)
- [ ] ESLint sin errores (`npm run lint`)
- [ ] TypeScript sin errores (`npm run type-check`)
- [ ] Componentes nuevos tienen PropTypes o tipos TS correctos
- [ ] Coverage de componentes principales >70% (verificar con `npm run test:coverage`)
- [ ] Responsive design verificado en Desktop + Tablet + Mobile (inspeccion manual)
- [ ] Commit con mensaje descriptivo: `feat(frontend): Sprint X complete - [descripcion]`
