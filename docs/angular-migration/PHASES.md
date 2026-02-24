# Angular Migration — Fases Detalladas

> **Prerequisito:** Leer [README.md](./README.md) primero.
> **Estrategia:** Strangler Fig con Edge Middleware en Vercel.

---

## Phase M0 — Setup + Arquitectura (1.5 semanas)

**Objetivo:** Angular y React conviven en el mismo repositorio. El usuario no nota nada.

### Sprint M0.1 — Proyecto Angular + CI/CD (1 semana)

| Tarea | Notas |
|-------|-------|
| Crear `/frontend-angular` con Angular CLI 17+ | `ng new quickstack-angular --routing --style=scss --standalone` |
| Configurar Angular Material + tema equivalente al MUI actual | Misma paleta de colores |
| Configurar Jest en lugar de Karma/Jasmine | `jest-preset-angular` |
| Configurar path aliases en `tsconfig.json` | `@core/`, `@features/`, `@shared/` |
| Configurar ESLint + Prettier alineados con el proyecto | Reglas consistentes con `/frontend` |
| Crear segundo proyecto Vercel `quickstack-angular` desde `/frontend-angular` | Deploy automático desde `main` |
| GitHub Actions: agregar job de build + test para `frontend-angular` | Job paralelo al de React, no reemplaza |

**Definition of Done:**
- `ng build` produce bundle sin errores
- `ng test` corre y pasa 1 test de ejemplo
- Deploy en `quickstack-angular.vercel.app` funciona (landing page Angular vacía)
- CI verde en PR de prueba

### Sprint M0.2 — Edge Middleware + Estructura (0.5 semanas)

| Tarea | Notas |
|-------|-------|
| Implementar `middleware.ts` en el proyecto principal (o crear proyecto Vercel "router") | Enruta por pathname a React o Angular |
| Configurar `vercel.json` con rewrites para rutas Angular → `quickstack-angular.vercel.app` | Inicialmente 0 rutas → Angular |
| Documentar proceso de rollback | Modificar `vercel.json` + push = rollback en < 2 min |
| Definir estructura de carpetas Angular (`/features`, `/core`, `/shared`) | Package by feature como el React |

**Definition of Done:**
- El middleware está desplegado pero sin rutas activas (React sigue manejando todo)
- Se puede agregar una ruta al middleware y el rewrite funciona en producción
- Proceso de rollback documentado y probado una vez

---

## Phase M1 — Auth + Layout (2 semanas)

**Objetivo:** El flujo de autenticación completo migra a Angular. El usuario del POS no nota la diferencia.

**Rutas que migran:** `/login`, `/register`, `/forgot-password`, `/reset-password`

**Por qué empezar aquí:** Auth es el módulo más aislado — no depende de ningún feature del POS. Si algo falla, el usuario simplemente ve un error de login y se puede hacer rollback inmediato.

### Sprint M1.1 — HTTP Layer + AuthService (1 semana)

| Tarea | Esfuerzo | Descripción |
|-------|----------|-------------|
| `AuthService` con Signals: `currentUser`, `isAuthenticated`, `isLoading` | M | Equivalente a `authStore.ts` de Zustand |
| `AuthApiService`: métodos `login()`, `register()`, `logout()`, `forgotPassword()`, `resetPassword()`, `refresh()` | M | Equivalente a `authApi.ts` |
| `AuthInterceptor` (HttpInterceptor): adjunta Bearer token, detecta 401, hace refresh, reintenta | L | El reto más complejo — equivalente a `axiosInstance.ts`. Ver nota abajo. |
| `TokenService`: acceso token en memoria (no localStorage) | S | Singleton con señal privada |
| Configurar MSW 2.x para Angular | S | Los handlers de `authHandlers.ts` son reutilizables |
| Tests `AuthService`: 10+ tests | M | Paridad con tests de `useAuthQuery.ts` |
| Tests `AuthInterceptor`: 8+ tests | M | Paridad con tests de `axiosInterceptor.test.ts` |

> **Nota sobre el interceptor:** La lógica de queuing de requests paralelos durante el refresh cambia de Promises a RxJS. El patrón con `BehaviorSubject` + `filter` + `take(1)` es el equivalente RxJS al array de callbacks del Axios actual.

**Definition of Done M1.1:**
- Silent refresh funciona (verificable en DevTools: request original se reintenta automáticamente)
- 18+ tests pasan
- Sin tokens en `localStorage` ni en el DOM

### Sprint M1.2 — Páginas Auth + Layout Base (1 semana)

| Tarea | Esfuerzo | Descripción |
|-------|----------|-------------|
| `LoginComponent` (Angular Material form) | M | Manejo de errores 401/423/429 |
| `RegisterComponent` | M | Validación de password |
| `ForgotPasswordComponent` | S | |
| `ResetPasswordComponent` | S | Lee `?token` de query params con `ActivatedRoute` |
| `AppLayoutComponent` con `<router-outlet>` | M | Equivalente a `AppLayout.tsx` |
| `SidebarComponent` con `routerLinkActive` | M | |
| `TopBarComponent` con nombre usuario + logout | S | |
| `AuthGuard` (`CanActivateFn`) | S | Equivalente a `ProtectedRoute.tsx` |
| Routing: `app.routes.ts` con lazy loading | S | |
| Tests de componentes: 20+ tests | M | |

**Definition of Done M1.2:**
- Flujo completo: login → dashboard → logout funciona en `quickstack-angular.vercel.app`
- Token se refresca silenciosamente (mismo comportamiento que React)
- 38+ tests pasan en Angular (paridad)
- Angular Material visualmente equivalente al MUI actual

**Activación en producción:**
```json
// vercel.json — agregar estas rutas al middleware
{ "source": "/login", "destination": "https://quickstack-angular.vercel.app/login" },
{ "source": "/register", "destination": "https://quickstack-angular.vercel.app/register" },
{ "source": "/forgot-password", "destination": "https://quickstack-angular.vercel.app/forgot-password" },
{ "source": "/reset-password", "destination": "https://quickstack-angular.vercel.app/reset-password" }
```

**Verificación post-activación:**
- El piloto puede hacer login normalmente (sin saberlo, ya es Angular)
- Sesión persiste al navegar a rutas React (la cookie de refresh es del mismo dominio)
- Si hay bug: rollback en 2 minutos revirtiendo el `vercel.json`

---

## Phase M2 — Dashboard (1 semana)

**Objetivo:** La pantalla principal del POS migra a Angular.

**Rutas que migran:** `/dashboard`

| Tarea | Descripción |
|-------|-------------|
| `DashboardComponent` con widgets de resumen | Equivalente a `DashboardPage.tsx` |
| `BranchSelectorComponent` en TopBar | Dropdown de sucursal activa |
| `BranchService` con Signals: sucursal activa persistida en `localStorage` | Equivalente al `branchStore` |
| Tests: 8+ tests | |

**Definition of Done:**
- Dashboard carga correctamente con datos del backend
- El selector de sucursal funciona
- Activación en producción sin incidentes

---

## Phase M3 — Catálogo Admin (2 semanas)

**Objetivo:** Las pantallas de administración del catálogo migran a Angular.

**Rutas que migran:** `/catalog/*` (categorías, productos, variantes, modificadores, combos)

### Sprint M3.1 — CatalogService + API Layer (0.5 semanas)

| Tarea | Descripción |
|-------|-------------|
| `CatalogService`: `getCategories()`, `getProducts()`, `getVariants()`, `getModifierGroups()`, `getCombos()` | Con `@tanstack/angular-query-experimental` para caching |
| Types equivalentes a DTOs del backend | Reutilizables directamente del proyecto React |
| MSW handlers para endpoints del catálogo | |
| Tests del servicio: 10+ tests | |

### Sprint M3.2 — Pantallas CRUD del Catálogo (1.5 semanas)

| Tarea | Esfuerzo |
|-------|----------|
| `CategoriesComponent` — listado + CRUD | M |
| `ProductsComponent` — tabla con búsqueda + CRUD | L |
| `VariantsComponent` — gestión de variantes por producto | M |
| `ModifierGroupsComponent` — CRUD de grupos + modificadores | L |
| `CombosComponent` — CRUD de combos con selector de productos | L |
| Tests: 25+ tests | L |

**Definition of Done:**
- CRUD completo de catálogo funciona en Angular
- TanStack Query cachea correctamente (sin requests duplicados)
- 35+ tests nuevos pasan
- Activación en producción: `/catalog/*` → Angular

---

## Phase M4 — Menú POS (2 semanas)

**Objetivo:** La pantalla del menú del POS (selección de productos para armar pedidos) migra a Angular.

**Rutas que migran:** `/menu`

Esta es la pantalla más visual e interactiva del catálogo — productos agrupados por categoría con selección de variantes y modificadores.

| Tarea | Esfuerzo | Descripción |
|-------|----------|-------------|
| `MenuComponent` — grid de productos por categoría | L | Búsqueda, filtros, scroll |
| `ProductCardComponent` — card individual | S | Imagen, precio, badge de variantes |
| `ProductDetailDialogComponent` — modal de variantes + modificadores | L | Validación min/max en modifier groups |
| `CategoryChipsComponent` — filtro de categorías | S | |
| Tests: 20+ tests | M | |

**Definition of Done:**
- El menú carga todos los productos del restaurante
- Selección de variantes y modificadores con validación funciona
- `GET /api/v1/menu` se consume correctamente
- 20+ tests pasan

---

## Phase M5 — Pedidos + Pagos (2.5 semanas)

**Objetivo:** El flujo core del POS migra a Angular. Esta es la fase de mayor riesgo — cualquier bug afecta directamente las ventas del piloto.

**Rutas que migran:** `/pos/*`, `/orders/*`

**Estrategia de riesgo:** Activar en producción en horario de baja afluencia (ej. lunes a las 9am). Monitorear activamente las primeras 2 horas. Rollback inmediato si hay cualquier anomalía en ventas o pagos.

### Sprint M5.1 — Cart State + Order Service (1 semana)

| Tarea | Esfuerzo | Descripción |
|-------|----------|-------------|
| `CartService` con Signals: `items`, `subtotal`, `tax`, `total` | L | El estado más complejo — equivalente al `cartStore` de Zustand |
| `OrderService`: crear orden, agregar items, cambiar estado, registrar pago | M | |
| `PosStateService`: orquestador del flujo (SERVICE_TYPE → CART → PAYMENT) | M | |
| MSW handlers para endpoints de orders (28 endpoints) | M | |
| Tests del cart: 20+ tests | M | Casos edge: modificadores, descuentos, cambio de cantidad |

### Sprint M5.2 — Pantallas del Flujo POS (1.5 semanas)

| Tarea | Esfuerzo | Descripción |
|-------|----------|-------------|
| `ServiceTypeSelectorComponent` — DINE_IN / COUNTER / DELIVERY / TAKEOUT | S | |
| `TableSelectorComponent` — grid de mesas | M | Solo visible en DINE_IN |
| `CustomerFormComponent` — formulario cliente para DELIVERY | M | |
| `CartComponent` — panel lateral reactivo | L | Signals → auto-update en tiempo real |
| `PaymentComponent` — pantalla de cobro en efectivo | M | Cálculo de cambio |
| `OrdersListComponent` — pedidos del día | L | |
| `OrderDetailComponent` — detalle de pedido | M | |
| Tests: 40+ tests | L | Flujos completos por tipo de servicio |

**Definition of Done M5:**
- Cajero puede completar venta de mostrador con 3+ productos en < 60 segundos
- Flujo DINE_IN, DELIVERY, COUNTER y TAKEOUT funcionan
- Cálculo de totales correcto (base + variantes + modificadores + impuesto)
- 60+ tests nuevos pasan
- Activado en producción sin incidentes de ventas

---

## Phase M6 — Admin Modules + Cutover Final (1.5 semanas)

**Objetivo:** Migrar los módulos administrativos restantes y eliminar React del proyecto.

### Sprint M6.1 — Módulos Admin (1 semana)

| Tarea | Descripción |
|-------|-------------|
| `BranchesAdminComponent` — CRUD de sucursales (solo OWNER) | |
| `TablesAdminComponent` — gestión de mesas y áreas | |
| `UsersAdminComponent` — gestión de usuarios del tenant | |
| Tests: 20+ tests | |

### Sprint M6.2 — Cutover y Limpieza (0.5 semanas)

| Tarea | Descripción |
|-------|-------------|
| Migrar el middleware de Edge a configuración directa en el proyecto Angular | Ya no se necesita proxy entre los dos deployments |
| Actualizar dominio principal → Angular directamente | Sin intermediarios |
| Archivar `/frontend` React en branch `archive/react-frontend` | Mantener para referencia histórica |
| Eliminar `/frontend` del `main` | Reducir el tamaño del repo |
| Actualizar CI/CD: eliminar jobs del React | |
| Actualizar `CLAUDE.md` y `ROADMAP.md` con el nuevo stack | |
| Auditoría ASVS final del Angular | Sin regresiones de seguridad |

**Definition of Done M6 (= Migración Completa):**
- [ ] Angular maneja el 100% del tráfico en producción
- [ ] 120+ tests pasan en Angular
- [ ] ASVS L2 compliance equivalente al React original
- [ ] `npm audit` sin vulnerabilidades críticas
- [ ] Bundle inicial ≤ 700KB gzipped
- [ ] El piloto ha operado 1 semana completa en Angular sin incidentes
- [ ] `/frontend` React eliminado del branch `main`
- [ ] Documentación actualizada con el nuevo stack

---

## Resumen de Esfuerzo

| Fase | Duración | Tests nuevos | Rutas activadas |
|------|----------|-------------|-----------------|
| M0 Setup | 1.5 sem | ~0 | 0 |
| M1 Auth + Layout | 2 sem | ~40 | 4 rutas |
| M2 Dashboard | 1 sem | ~8 | 1 ruta |
| M3 Catálogo Admin | 2 sem | ~35 | 5+ rutas |
| M4 Menú POS | 2 sem | ~20 | 1 ruta |
| M5 Pedidos + Pagos | 2.5 sem | ~60 | 10+ rutas |
| M6 Admin + Cutover | 1.5 sem | ~20 | Todas restantes |
| **Total** | **~12.5 sem** | **~183 tests** | **React eliminado** |
