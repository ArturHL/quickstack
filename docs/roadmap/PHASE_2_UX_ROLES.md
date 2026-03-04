# Phase 2: UX Roles + Piloto

> **Estado:** En Progreso
> **Inicio:** 2026-03-04
> **Objetivo:** Lanzar el piloto con el restaurante real. El OWNER puede crear un cajero. El cajero puede completar una venta completa de mostrador. El admin ve el reporte del dia.
> **Desarrollador:** 1 persona
> **Duracion estimada:** 5 sprints / ~5 semanas

---

## Contexto y Estado Actual

### Lo que ya existe (no construir de nuevo)

| Componente | Archivo | Estado |
|------------|---------|--------|
| Layouts por rol | `WaiterLayout`, `CashierLayout`, `KitchenLayout`, `AppLayout` | Scaffolding listo, sin contenido real |
| Enrutamiento por rol | `router.tsx`, `RoleBasedRedirect` | Funcional. CASHIER → `/cashier/pos`, OWNER → `/admin/dashboard` |
| Componentes POS | `ProductCatalog`, `ProductCard`, `ProductDetailModal`, `Cart`, `CartItem`, `PaymentForm`, `ServiceTypeSelector` | Completos, en `/features/pos/components` |
| Stores | `cartStore`, `posStore`, `branchStore` | Completos con persist (sessionStorage / localStorage) |
| Hooks y APIs | `useCreateOrderMutation`, `useSubmitOrderMutation`, `useMarkReadyMutation`, `useRegisterPaymentMutation`, `orderApi`, `paymentApi`, `menuApi` | Completos |
| Paginas POS legacy | `CatalogPage`, `CartPage`, `PaymentPage`, `OrderConfirmationPage`, `NewOrderPage` | Funcionales en `/admin/pos/*` — a deprecar |
| Sidebar admin | `Sidebar.tsx` | Funcional, con logica `hasMinRole` |
| TopBar + BranchSelector | `TopBar.tsx`, `BranchSelector.tsx` | Presentes en AppLayout |
| User entity + service | `User.java`, `UserService.java` | Entidad completa. `registerUser()` existe. Sin controller ni endpoints CRUD |
| UserRepository | `UserRepository.java` | `findByTenantIdAndEmail`, `findByTenantIdAndId` — sin `findAllByTenantId` |

### Lo que NO existe (hay que construir)

- `CashierPos` — pagina real (actualmente un placeholder de texto)
- `CashierLayout` — sin navegacion ni header funcional
- Backend User Management — sin `UserController`, sin endpoints CRUD de usuarios
- Sidebar admin con contexto dual (Owner view vs Manager view)
- `UserList`, `UserForm` — componentes frontend de gestion de usuarios
- Ruta `/admin/users` y link en Sidebar

---

## Criterio de Piloto Minimo Viable

> El piloto esta listo cuando se puede ejecutar este flujo de punta a punta en produccion, sin intervenciones manuales en la base de datos:
>
> 1. OWNER hace login → ve dashboard
> 2. OWNER navega a `/admin/users` → crea un usuario con rol CASHIER, asignado a su sucursal
> 3. CASHIER hace login → aterriza en `/cashier/pos`
> 4. CASHIER agrega productos → ve el total en el carrito → cobra en efectivo → ve confirmacion
> 5. OWNER va a `/admin/reports` → ve el reporte del dia con esa venta

**Sprint que habilita el piloto: Sprint 3** (Backend User Management completo) + **Sprint 4** (Frontend User Management).

---

## Mapa de Dependencias

```
Sprint 1 (CashierPos UI)           — independiente, empieza ya
Sprint 2 (CashierLayout + Rutas)   — depende de Sprint 1 (necesita la pagina terminada)
Sprint 3 (Backend User CRUD)       — independiente de Sprint 1 y 2
Sprint 4 (Frontend User Mgmt)      — depende de Sprint 3 (necesita los endpoints)
Sprint 5 (Admin Dual View)         — depende de Sprint 2 y 4 (contexto completo para ajustar Sidebar)
```

Sprints 1 y 3 pueden correr en paralelo si el calendario lo permite. En un solo desarrollador, el orden recomendado es el listado arriba (UI primero para validar con el restaurante, luego backend).

---

## Sprint 1: CashierPos — Split Screen POS

**Duracion:** 1 semana
**Objetivo:** La pagina `/cashier/pos` muestra una interfaz split-screen funcional donde el cajero puede agregar productos al carrito, cobrar en efectivo y ver la confirmacion. Sin navegacion entre paginas.

**Por que primero:** Es el core del piloto. Todo lo demas (usuarios, admin views) no tiene sentido sin esto. Ademas valida la integracion del stack de componentes existentes en el nuevo layout.

### Tareas

| # | Tarea | Tamano | Notas |
|---|-------|--------|-------|
| 1.1 | Construir `CashierPos.tsx` como layout split-screen 60/40 (catalogo izquierda, panel derecha) usando `Box` con `display: flex` | S | Archivo existe como placeholder — reemplazar contenido completo |
| 1.2 | Panel izquierdo: montar `ProductCatalog` directamente (importar componente existente) | S | Sin cambios al componente — solo composicion |
| 1.3 | Panel derecho — estado "carrito": montar `Cart` con totales siempre visibles | S | `Cart.tsx` ya tiene toda la logica. Quitar el boton "Ir al Catalogo" cuando `items.length === 0` o adaptar el mensaje vacio para split-screen |
| 1.4 | Panel derecho — estado "cobro": cuando Cart llama a `handleEnviarOrden` exitosamente para COUNTER/TAKEOUT, el panel derecho cambia a `PaymentPanel` (inline, sin navegar a otra pagina) | M | Crear `PaymentPanel.tsx` como wrapper de `PaymentForm` + resumen de orden. `PaymentPage.tsx` existente puede servir de referencia para la logica |
| 1.5 | Panel derecho — estado "confirmacion": despues del pago exitoso, mostrar `OrderConfirmationPanel` con boton "Nueva Venta" que resetea el panel al estado carrito | S | Extraer logica de `OrderConfirmationPage.tsx`. No navegar fuera de `/cashier/pos` |
| 1.6 | Manejar `ServiceTypeSelector` en CashierPos: en el piloto solo COUNTER y TAKEOUT. Pre-seleccionar COUNTER por defecto (o mostrar selector minimo al inicio de cada venta) | S | El store `cartStore` ya tiene el campo `serviceType`. Pre-setear en mount evita que el cajero tenga que elegir en cada venta |
| 1.7 | Tests: `CashierPos.test.tsx` — renderiza catalogo, agrega un item al carrito, panel cambia a cobro, registra pago, muestra confirmacion | M | Usar MSW handlers existentes en `orderHandlers.ts`. Minimo 8 tests |

**Success Criteria:**
- [ ] `/cashier/pos` muestra catalogo y carrito lado a lado sin navegacion entre paginas
- [ ] Flujo completo COUNTER funciona sin salir de la pagina: seleccion → carrito → cobro → confirmacion → nueva venta
- [ ] "Nueva Venta" limpia carrito y vuelve al estado inicial
- [ ] Suite de tests pasa (`npm test`)
- [ ] No hay regresiones en los 319 tests existentes

**Riesgos:**
- `Cart.tsx` usa `useNavigate` para ir a `/pos/payment` — la logica de navegacion debe refactorizarse para que el componente pueda operar en modo "inline" (callback prop en lugar de navigate). Esto requiere un cambio cuidadoso para no romper el flujo legacy de `/admin/pos/*` mientras existe.
- **Mitigacion:** Agregar prop opcional `onPaymentReady?: (orderId: string) => void` a `Cart.tsx`. Si no se pasa, el comportamiento por defecto (navigate) se mantiene. CashierPos pasa el callback y maneja el estado del panel internamente.

---

## Sprint 2: CashierLayout — Shell Operativo del Cajero

**Duracion:** 0.5 semanas (3 dias)
**Objetivo:** `CashierLayout` tiene un header con nombre del cajero y boton de logout. El scaffolding de rutas bajo `/cashier/*` esta limpio y listo para escalar.

**Depende de:** Sprint 1 completado.

**Por que separado de Sprint 1:** El layout es infraestructura de shell — no afecta la logica del POS. Separarlo permite validar Sprint 1 primero y luego pulir el contenedor.

### Tareas

| # | Tarea | Tamano | Notas |
|---|-------|--------|-------|
| 2.1 | Actualizar `CashierLayout.tsx`: reemplazar la sidebar placeholder (80px con texto "POS") por un header horizontal compacto con: logo/nombre del negocio (izq), nombre del cajero (centro), boton Logout (der) | S | Sin drawer — el cajero no necesita navegacion lateral. Header fijo arriba, `Outlet` ocupa el resto de la pantalla |
| 2.2 | Conectar boton Logout en `CashierLayout` al hook `useLogout` de `authStore` | S | Mismo patron que `TopBar.tsx` |
| 2.3 | Verificar que `RoleBasedRedirect` envia a CASHIER a `/cashier/pos` correctamente (ya implementado) — agregar test si no existe | S | `RoleBasedRedirect.tsx` ya tiene `CASHIER: '/cashier/pos'` en `ROLE_ROUTES` |
| 2.4 | Deprecar rutas `/admin/pos/*`: agregar redirects de `/admin/pos/*` → `/cashier/pos` para no romper bookmarks. Alternativamente, eliminarlas si no hay riesgo de uso activo | S | Las `posRoutes` estan montadas bajo `/admin`. Evaluar si hay tests que dependen de esas rutas antes de eliminar |
| 2.5 | Tests: `CashierLayout.test.tsx` — renderiza el nombre del usuario, boton logout visible, `Outlet` renderiza contenido hijo | S | Minimo 4 tests |

**Success Criteria:**
- [ ] `CashierLayout` muestra nombre del cajero autenticado y boton de logout funcional
- [ ] Logout desde el layout redirige a `/login`
- [ ] Las rutas `/admin/pos/*` estan deprecadas o redirigidas
- [ ] Tests pasan

**Riesgos:**
- Eliminar `posRoutes` puede romper tests existentes de `posRoutes.test.tsx` y cualquier test que navegue a `/pos/*`.
- **Mitigacion:** Revisar `posRoutes.test.tsx` y los tests de `Cart.tsx` antes de eliminar. Si hay dependencias, mantener las rutas temporalmente y solo agregar redirects.

---

## Sprint 3: Backend User Management

**Duracion:** 1.5 semanas
**Objetivo:** El OWNER puede crear, listar, actualizar y desactivar usuarios desde la API. Los endpoints respetan multi-tenancy y solo son accesibles con rol OWNER.

**Depende de:** Nada. Puede correr en paralelo con Sprints 1 y 2.

**Por que es el cuello de botella del piloto:** Sin estos endpoints, el OWNER no puede crear al cajero desde la app — habria que insertarlo manualmente en la DB, lo que bloquea el lanzamiento del piloto.

**Estado del backend:** `UserService.registerUser()` ya existe y funciona. `User` entity tiene el campo `active` para soft-delete. Lo que falta es exponer CRUD via REST y agregar las operaciones de listado y actualizacion.

### Tareas

| # | Tarea | Tamano | Notas |
|---|-------|--------|-------|
| 3.1 | Crear `UserCreateRequest.java` en `quickstack-user/dto/request/` — fields: `email @Email @NotBlank`, `fullName @NotBlank`, `password @NotBlank @Size(min=12)`, `roleId @NotNull UUID`, `branchId UUID` (nullable — OWNER puede no tener sucursal) | S | Record con Bean Validation. Siguiendo patron de otros `*CreateRequest` del proyecto |
| 3.2 | Crear `UserUpdateRequest.java` — fields: `fullName String`, `roleId UUID`, `branchId UUID`, `active Boolean` (todos opcionales — PATCH semantics) | S | Solo actualiza campos no-null. Password change es un flujo separado (ya existe en `UserService.changePassword`) |
| 3.3 | Ampliar `UserResponse.java` para incluir: `isActive boolean`, `roleName String` (join logico con tabla roles — pasar como parametro desde el service), `phone String` | S | `UserResponse` ya existe como record — agregar campos. `roleName` requiere lookup de la tabla `roles` por `roleId` (query simple con JdbcTemplate o constante mapeada en el service) |
| 3.4 | Agregar `findAllByTenantId(UUID tenantId)` a `UserRepository` — query JPQL con `WHERE u.tenantId = :tenantId AND u.deletedAt IS NULL ORDER BY u.createdAt DESC` | S | Seguir patron de queries existentes en el repo |
| 3.5 | Agregar metodos al `UserService`: `listUsers(UUID tenantId)`, `updateUser(UUID tenantId, UUID userId, UserUpdateRequest)`, `deactivateUser(UUID tenantId, UUID userId, UUID deletedBy)` | M | `deactivateUser` llama a `user.softDelete(deletedBy)` — el metodo ya existe en la entidad. `updateUser` actualiza solo campos presentes (null = no cambiar) |
| 3.6 | Crear `UserController.java` en `quickstack-user/controller/` — endpoints: `POST /api/v1/users`, `GET /api/v1/users`, `PUT /api/v1/users/{id}`, `DELETE /api/v1/users/{id}` | M | Siguiendo convencion del proyecto: controller en el modulo de feature. Sin `@RequestMapping` a nivel clase — rutas completas en cada metodo. Inyectar `JwtAuthenticationPrincipal` para extraer `tenantId` del token |
| 3.7 | Agregar `/api/v1/users/**` a `SecurityConfig.java` — proteger con `hasAnyRole('OWNER')` o via `@PreAuthorize` en el controller | S | Agregar al bloque de `authorizeHttpRequests` en `SecurityConfig`. Seguir patron de `@PreAuthorize("@branchPermissionEvaluator.canAccess(...)")` si se quiere granularidad, o simplemente `hasRole('OWNER')` a nivel ruta para MVP |
| 3.8 | Tests unitarios `UserServiceTest.java` — cubrir: crear usuario duplicado (EMAIL_EXISTS), listar usuarios del tenant, actualizar fullName/role, desactivar usuario | M | Minimo 12 tests nuevos. Seguir patron de tests existentes con `@ExtendWith(MockitoExtension.class)` |
| 3.9 | Tests de integracion `UserManagementE2ETest.java` en `quickstack-app` — flujo completo: autenticar como OWNER, crear cajero, listar usuarios (ve el nuevo), desactivar cajero, intentar login con cajero desactivado (debe fallar) | L | Extender `BaseIntegrationTest`. El ultimo assertion valida que `active=false` bloquea el login — revisar `canLogin()` en `User.java` que ya tiene esa logica |

**Success Criteria:**
- [ ] `POST /api/v1/users` crea un usuario con rol CASHIER asignado a una sucursal
- [ ] `GET /api/v1/users` retorna lista de usuarios del tenant (sin usuarios de otros tenants)
- [ ] `PUT /api/v1/users/{id}` actualiza fullName y/o rol
- [ ] `DELETE /api/v1/users/{id}` desactiva el usuario (soft delete, `active=false`)
- [ ] Todos los endpoints retornan 403 si el rol no es OWNER
- [ ] Cross-tenant retorna 404 (no 403)
- [ ] `./mvnw verify` pasa con los nuevos tests incluidos
- [ ] Minimo 20 tests nuevos (unit + integration)

**Riesgos:**
- `roleName` en `UserResponse` requiere resolver el nombre del rol a partir de `roleId`. La tabla `roles` existe en el seed (V7) pero no hay una entity `Role` en el modulo `quickstack-user`.
- **Mitigacion:** Para el MVP, usar un mapa de constantes en el service con los UUIDs de roles del seed V7 (`aaaaaaaa... = OWNER`, `bbbbbbbb... = CASHIER`, etc.). Evitar crear una entity `Role` por ahora — eso va en Phase 3 cuando se agreguen roles custom por tenant.
- La invalidacion de sesiones activas al desactivar un usuario requiere revocar los refresh tokens. El modulo `quickstack-auth` maneja los refresh tokens — hay acoplamiento entre modulos.
- **Mitigacion para MVP:** Al desactivar un usuario, el `deactivateUser` solo setea `active=false`. El login ya valida `canLogin()` que chequea `active`. Los tokens existentes expiran en 15 minutos (access) y la proxima vez que intenten refresh, el sistema rechazara porque el usuario ya no puede hacer login. Para el piloto esto es aceptable. Documentar como deuda tecnica para Phase 3.

---

## Sprint 4: Frontend User Management

**Duracion:** 1 semana
**Objetivo:** El OWNER puede crear, ver y desactivar usuarios desde `/admin/users`. Flujo completo sin tocar la base de datos.

**Depende de:** Sprint 3 completado y desplegado (o al menos con MSW handlers para desarrollo local).

### Tareas

| # | Tarea | Tamano | Notas |
|---|-------|--------|-------|
| 4.1 | Crear `userApi.ts` en `frontend/src/features/users/api/` — funciones: `listUsers()`, `createUser(data)`, `updateUser(id, data)`, `deactivateUser(id)` — patron: `axiosInstance.xxx(...).then(r => r.data.data)` | S | Siguiendo patron de `branchApi.ts` y otros APIs del proyecto |
| 4.2 | Crear hooks TanStack Query: `useUsersQuery`, `useCreateUserMutation`, `useUpdateUserMutation`, `useDeactivateUserMutation` | S | Patron de `useBranchesQuery`, `useBranchMutations`. `useUsersQuery` invalida cache en mutaciones |
| 4.3 | Crear tipos TypeScript: `UserSummary` (lista), `UserCreateRequest`, `UserUpdateRequest` en `frontend/src/features/users/types/User.ts` | S | `UserSummary` incluye: `id`, `email`, `fullName`, `roleName`, `isActive`, `branchId`, `createdAt` |
| 4.4 | Crear `UserList.tsx` — tabla MUI con columnas: nombre, email, rol, estado (chip activo/inactivo), sucursal asignada, acciones (editar, desactivar). Boton "Nuevo Usuario" en header | M | Patron de `BranchList.tsx`. Confirmacion antes de desactivar (Dialog de MUI) |
| 4.5 | Crear `UserForm.tsx` — modal MUI con campos: nombre completo, email, contrasena (solo en creacion), rol (Select con opciones CASHIER/KITCHEN/WAITER), sucursal (Select poblado con `useBranchesQuery`). Validacion de formulario | M | Patron de `BranchForm.tsx`. En modo edicion: no mostrar campo contrasena, pre-poblar valores actuales |
| 4.6 | Crear `UserListPage.tsx` — composicion de `UserList` + `UserForm` modal. Ruta: `/admin/users` | S | Patron de `BranchListPage.tsx` |
| 4.7 | Agregar ruta `/admin/users` a `adminRoutes.tsx` con `RoleProtectedRoute minRole="OWNER"` | S | Seguir patron de ruta `/admin/branches` que tiene el mismo nivel de proteccion |
| 4.8 | Agregar link "Usuarios" al `Sidebar.tsx` dentro del bloque `hasMinRole(user?.role, 'OWNER')` con icono `PeopleOutlined` — ruta `/admin/users` | S | Ya existe el icono `PeopleIcon` importado en Sidebar — esta asignado a "Clientes". Usar `GroupOutlined` o `ManageAccountsOutlined` para diferenciar |
| 4.9 | Crear MSW handlers para desarrollo: `userHandlers.ts` — `GET /users`, `POST /users`, `PUT /users/:id`, `DELETE /users/:id` | S | Siguiendo patron de `orderHandlers.ts`. Necesario para que los tests no dependan del backend real |
| 4.10 | Tests: `UserList.test.tsx` (renderiza lista, boton desactivar abre confirmacion), `UserForm.test.tsx` (campos requeridos, submit llama a mutation, modo edicion pre-puebla valores) | M | Minimo 15 tests nuevos |

**Success Criteria:**
- [ ] OWNER navega a `/admin/users` y ve la lista de usuarios del tenant
- [ ] OWNER puede crear un cajero con nombre, email, contrasena temporal y sucursal asignada
- [ ] OWNER puede desactivar un usuario con confirmacion
- [ ] El link "Usuarios" aparece en el Sidebar solo para OWNER
- [ ] Tests pasan, sin regresiones

**Riesgos:**
- El campo "sucursal" en `UserForm` requiere llamar a `useBranchesQuery` dentro del formulario. Si el OWNER tiene muchas sucursales, el Select puede ser lento.
- **Mitigacion:** Para el piloto (1 sucursal), no es un problema. Documentar para Phase 3 si crece el numero de sucursales.
- La contrasena inicial del cajero: el OWNER la escribe en el formulario. No hay flujo de "invitacion por email" todavia.
- **Decision de MVP:** El OWNER le dice la contrasena al cajero por WhatsApp o en persona. El campo `mustChangePassword` existe en la entidad — se puede setear a `true` al crear para forzar cambio en primer login (Phase 3).

---

## Sprint 5: Admin Dual View — Sidebar Contextual

**Duracion:** 0.5 semanas (3 dias)
**Objetivo:** El `Sidebar` en `/admin/*` muestra secciones diferentes dependiendo de si hay una sucursal activa seleccionada o no. El `BranchSelector` tiene la opcion "Todas las sucursales".

**Depende de:** Sprint 2 (posRoutes deprecadas) y Sprint 4 (usuarios en sidebar).

**Nota:** Este sprint es el ultimo porque requiere que el Sidebar tenga todos sus items finales antes de reorganizarlos por contexto. Si se hace antes, habra que revisitarlo.

### Tareas

| # | Tarea | Tamano | Notas |
|---|-------|--------|-------|
| 5.1 | Agregar opcion "Todas las sucursales" a `BranchSelector.tsx` — cuando se selecciona, `branchStore.activeBranchId` se setea a `null` | S | `BranchSelector.tsx` existe en `features/branches/components/`. El valor `null` en `activeBranchId` es el estado inicial del store — solo agregar la opcion al UI |
| 5.2 | Refactorizar `Sidebar.tsx` — reemplazar la logica `hasMinRole` por logica dual basada en `activeBranchId`: si `activeBranchId === null` mostrar "Owner View" (metricas globales, usuarios, sucursales), si `activeBranchId !== null` mostrar "Manager View" (catalogo, reportes de sucursal, mesas) | M | Leer `branchStore.activeBranchId` en el Sidebar. Crear dos bloques de `NavItem` mutuamente excluyentes. Mantener el bloque de usuario/logout al fondo siempre |
| 5.3 | Owner View Sidebar items: Dashboard (`/admin/dashboard`), Usuarios (`/admin/users`), Sucursales (`/admin/branches`), Reportes globales (disabled — Phase 3) | S | Los links ya existen como `NavItem` sueltos — reorganizarlos bajo un bloque "Owner View" |
| 5.4 | Manager View Sidebar items: Reporte del dia (`/admin/reports`), Categorias (`/admin/categories`), Productos (`/admin/products`), Combos (`/admin/combos`), Mesas (`/admin/branches` filtrado por sucursal activa), Pedidos (`/orders`), Clientes (`/admin/customers`) | S | Igual — reorganizar items existentes bajo un bloque "Manager View" |
| 5.5 | Etiqueta de contexto en Sidebar: mostrar el nombre de la sucursal activa cuando `activeBranchId !== null`, o "Vista Global" cuando es null | S | Usar la query `useBranchesQuery` que ya existe para obtener el nombre de la sucursal por ID |
| 5.6 | Tests: `Sidebar.test.tsx` — renderiza Owner View cuando `activeBranchId = null`, renderiza Manager View cuando `activeBranchId = 'branch-1'`, cambiar sucursal cambia vista | M | Minimo 8 tests. Setear `branchStore` con `useBranchStore.setState(...)` en beforeEach como se hace en otros tests |

**Success Criteria:**
- [ ] Con `activeBranchId = null`: Sidebar muestra Dashboard, Usuarios, Sucursales
- [ ] Con `activeBranchId !== null`: Sidebar muestra Reporte del dia, Catalogo, Pedidos
- [ ] `BranchSelector` tiene opcion "Todas" que setea `activeBranchId = null`
- [ ] El nombre de la sucursal activa o "Vista Global" es visible en el Sidebar
- [ ] Tests pasan sin regresiones en `BranchSelector.test.tsx`

**Riesgos:**
- El cambio de Sidebar afecta a todos los tests existentes que renderizan `AppLayout` o `Sidebar` directamente.
- **Mitigacion:** Ejecutar la suite completa de tests antes de hacer el PR. Los tests que fallen por el cambio de contexto se corrigen actualizando el `beforeEach` para setear `branchStore` al estado esperado.

---

## Resumen de Estimaciones

| Sprint | Nombre | Duracion | Tests nuevos estimados | Piloto? |
|--------|--------|----------|----------------------|---------|
| Sprint 1 | CashierPos Split-Screen | 1 semana | ~10 frontend | No |
| Sprint 2 | CashierLayout Shell | 3 dias | ~5 frontend | No |
| Sprint 3 | Backend User Management | 1.5 semanas | ~20+ backend | Habilita piloto |
| Sprint 4 | Frontend User Management | 1 semana | ~15 frontend | Piloto listo aqui |
| Sprint 5 | Admin Dual View | 3 dias | ~8 frontend | Post-piloto |

**Total Phase 2:** ~5 semanas | ~58 tests nuevos estimados

**Punto de lanzamiento del piloto:** Fin de Sprint 4.
Sprint 5 mejora la experiencia del OWNER pero no bloquea el lanzamiento.

---

## Deuda Tecnica Conocida — Phase 2

Estas decisiones son correctas para el piloto pero deben revisitarse en Phase 3:

1. **Invalidacion de sesiones al desactivar usuario:** Actualmente el access token sigue valido 15 min despues del soft delete. Para Phase 3: al desactivar, llamar a `refreshTokenRepository.revokeAllByUserId(userId)` desde `UserService` (requiere dependencia entre `quickstack-user` y `quickstack-auth` — evaluar si se rompe el ADR-002 de separacion).

2. **Contrasena temporal sin flujo de invitacion:** El OWNER escribe la contrasena del cajero manualmente. Para Phase 3: `POST /api/v1/users` genera una contrasena aleatoria + setea `mustChangePassword=true` + envia email con link de primer acceso.

3. **roleName hardcodeado como mapa de constantes:** El service resuelve el nombre del rol usando UUIDs del seed V7. Para Phase 3: crear entidad `Role` con repositorio, permitir roles custom por tenant.

4. **`posRoutes` legacy bajo `/admin`:** Si se mantienen como redirects, limpiarlos en Phase 3 cuando no haya riesgo de regresion.

5. **`CartStore` serviceType pre-seteado a COUNTER en CashierPos:** Para el piloto esta bien. Para Phase 3 con DINE_IN y DELIVERY desde el cashier, necesitara un selector de tipo de servicio en el panel del cajero.

---

## Definition of Done — Phase 2

Un sprint esta completo cuando:
- [ ] Todos los tests nuevos pasan (`npm test` / `./mvnw verify`)
- [ ] No hay regresiones en tests existentes (319 frontend, ~1060 backend)
- [ ] El flujo descrito en los success criteria se puede ejecutar manualmente en el entorno de staging
- [ ] El codigo esta en `main` via Pull Request con descripcion clara
- [ ] No hay errores de TypeScript (`npm run type-check`)
- [ ] Las rutas protegidas por rol retornan correctamente para usuarios sin permiso

---

## Notas de Implementacion

### Patron para CashierPos split-screen (Sprint 1)

La clave del Sprint 1 es que `Cart.tsx` actualmente navega a `/pos/payment` usando `useNavigate`. Para el modo split-screen, se necesita que en lugar de navegar, notifique al componente padre. El patron propuesto:

```
// Cart.tsx — agregar prop opcional
interface CartProps {
  onReadyToPay?: (orderId: string) => void   // Si presente, no navegar — llamar callback
}

// CashierPos.tsx — manejar estado del panel derecho
type PanelState = 'cart' | 'payment' | 'confirmation'
```

Si `onReadyToPay` no se pasa, `Cart.tsx` mantiene su comportamiento actual (navegar a `/pos/payment`). Esto evita romper el flujo legacy sin condiciones complejas.

### Patron para `roleName` en Backend (Sprint 3)

```java
// En UserService.java — constantes privadas mapeadas del seed V7
private static final Map<UUID, String> ROLE_NAMES = Map.of(
    UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), "OWNER",
    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), "CASHIER",
    UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"), "KITCHEN"
);

// En el metodo que construye UserResponse
String roleName = ROLE_NAMES.getOrDefault(user.getRoleId(), "UNKNOWN");
```

### SecurityConfig — agregar `/api/v1/users/**` (Sprint 3)

El patron del proyecto protege rutas a nivel de `SecurityConfig` con `hasRole` y complementa con `@PreAuthorize` en el controller para logica de permiso mas granular. Para User Management en MVP, proteger a nivel de ruta es suficiente:

```java
.requestMatchers("/api/v1/users/**")
    .hasRole("OWNER")
```

Agregar junto a los otros bloques de rutas protegidas en `SecurityConfig.java`.
