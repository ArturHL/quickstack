# Phase 0.4: Frontend Base + Integracion Auth — Roadmap

> **Version:** 1.1.0
> **Fecha:** 2026-02-19
> **Status:** EN PROGRESO — Sprint 3/4 completados
> **Tipo:** 100% Frontend — sin trabajo en backend

---

## Resumen Ejecutivo

Este documento define el plan de implementacion del frontend base de QuickStack POS,
incluyendo la integracion completa con los 8 endpoints de autenticacion ya construidos en
Phase 0.3.

| Aspecto | Detalle |
|---------|---------|
| **Timeline** | 4 sprints (~4 dias calendario) |
| **Progreso** | 3/4 sprints completados (75%) |
| **Tests frontend** | 66 tests pasando (Sprint 1: 5, Sprint 2: 23, Sprint 3: 38) |
| **Tareas completadas** | 18/28 tareas (Setup: 6/6, Feature: 9/14, QA: 3/8) |
| **Endpoints integrados** | 8/8 (todos de Phase 0.3) |
| **Pendiente** | Sprint 4 - Dashboard Base + Calidad y Cierre |
| **Trabajo de backend** | Ninguno — API ya construida |

---

## Contexto: Estado del Backend al Inicio de Phase 0.4

Los 8 endpoints de auth estan en produccion en `https://quickstack-api.onrender.com`.

| Metodo | Endpoint | Descripcion | Auth requerida |
|--------|----------|-------------|----------------|
| POST | `/api/v1/auth/register` | Registrar OWNER + tenant | No |
| POST | `/api/v1/auth/login` | Login — retorna access + refresh tokens | No |
| POST | `/api/v1/auth/refresh` | Renovar access token silenciosamente | Cookie `__Host-refreshToken` |
| POST | `/api/v1/auth/logout` | Invalidar sesion | JWT Bearer |
| POST | `/api/v1/auth/forgot-password` | Solicitar reset de password | No |
| POST | `/api/v1/auth/reset-password` | Aplicar nueva password con token | No |
| GET | `/api/v1/auth/me` | Obtener datos del usuario actual | JWT Bearer |
| GET | `/api/v1/auth/health` | Health check de la API | No |

**Comportamientos del backend relevantes para el frontend:**

- El refresh token viaja exclusivamente en cookie `__Host-refreshToken` (HttpOnly, SameSite=Strict).
  El frontend nunca lo lee directamente — el browser lo envia automaticamente.
- El access token (JWT, expira en 15 min) debe enviarse como `Authorization: Bearer <token>`.
- `POST /forgot-password` siempre retorna 200 (proteccion timing-safe); no confirma si el
  email existe.
- `POST /login` retorna 423 si la cuenta esta bloqueada, con header `X-Locked-Until`.
- `POST /login` retorna 429 si se supera el rate limit, con header `Retry-After`.
- Todos los errores de negocio siguen la forma `{ "error": "...", "message": "..." }`.

---

## Estado Inicial del Proyecto Frontend

El directorio `frontend/` contiene un scaffold de Vite generado con la plantilla
`react-ts`. **Ninguna de las dependencias de fase 0.4 esta instalada aun.**

```
frontend/
├── src/
│   ├── App.tsx          # Placeholder
│   ├── App.css
│   ├── main.tsx
│   └── index.css
├── index.html
├── package.json         # Solo react + react-dom (sin MUI, Zustand, Axios, etc.)
├── vite.config.ts
└── tsconfig.app.json
```

Nota: El package.json actual lista React 19.x, pero la arquitectura documentada en
ARCHITECTURE.md especifica React 18.x. El Sprint 1 resuelve esta discrepancia al instalar
las dependencias con versiones fijadas.

---

## Estructura Target al Finalizar Phase 0.4

```
frontend/src/
├── components/
│   ├── layout/
│   │   ├── AppLayout.tsx          # Layout con sidebar y header
│   │   ├── Sidebar.tsx
│   │   └── TopBar.tsx
│   └── common/
│       ├── LoadingSpinner.tsx
│       ├── ErrorBoundary.tsx
│       └── ProtectedRoute.tsx
├── features/
│   └── auth/
│       ├── LoginPage.tsx
│       ├── RegisterPage.tsx
│       ├── ForgotPasswordPage.tsx
│       ├── ResetPasswordPage.tsx
│       └── __tests__/
│           ├── LoginPage.test.tsx
│           ├── RegisterPage.test.tsx
│           └── authStore.test.ts
├── hooks/
│   └── useAuthQuery.ts            # Custom hooks sobre TanStack Query
├── services/
│   └── authApi.ts                 # Funciones de llamada a los 8 endpoints
├── stores/
│   └── authStore.ts               # Zustand: accessToken, user, isAuthenticated
├── types/
│   └── auth.ts                    # Interfaces TS: User, AuthResponse, etc.
├── utils/
│   └── axiosInstance.ts           # Instancia con interceptors de auth
├── router/
│   └── AppRouter.tsx              # Definicion de rutas con React Router
├── theme/
│   └── theme.ts                   # MUI theme con colores y tipografia
├── pages/
│   └── DashboardPage.tsx          # Dashboard placeholder (post-login)
└── main.tsx                       # Punto de entrada
```

---

## Mapa de Dependencias entre Sprints

```
Sprint 1 (Setup)
    |
    |-- Instala todas las dependencias npm
    |-- Configura MUI theme, Axios instance, Zustand store, TanStack Query, Router
    |
    v
Sprint 2 (Auth Core)
    |
    |-- Requiere: axiosInstance.ts, authStore.ts (Sprint 1)
    |-- Construye: authApi.ts, useAuthQuery.ts, LoginPage, RegisterPage
    |
    v
Sprint 3 (Auth Completo + Rutas Protegidas)
    |
    |-- Requiere: authApi.ts, authStore.ts (Sprint 2)
    |-- Construye: ForgotPasswordPage, ResetPasswordPage, ProtectedRoute,
    |              auto-refresh interceptor, logout
    |
    v
Sprint 4 (Dashboard + Calidad)
    |
    |-- Requiere: ProtectedRoute, autenticacion funcional (Sprint 3)
    |-- Construye: AppLayout, Sidebar, TopBar, DashboardPage
    |-- Cierra: tests de integracion, ajustes de accesibilidad, smoke test E2E
```

---

## Definicion de Done Global

Un sprint se considera completado cuando:

1. Todas las tareas del sprint tienen criterios de aceptacion verificados.
2. `npm run lint` y `npm run type-check` pasan sin errores ni warnings.
3. `npm run test` pasa con 0 fallos.
4. El feature es visible y funcional en `http://localhost:5173` (dev) y en Vercel
   (preview branch si aplica).
5. Ningun secreto hardcodeado (URL de API en `.env.local`, no en codigo).
6. No hay `console.log` de datos de usuario ni tokens en el codigo entregado.

---

## Sprint 1: Setup de Infraestructura Frontend

**Duracion:** 1 dia | **Status:** ✅ COMPLETADO (2026-02-18)

**Objetivo:** Instalar y configurar todas las dependencias base. Al finalizar este sprint,
el proyecto compila, tiene routing basico, MUI aplicado, y el store de auth inicializado,
pero sin ninguna pantalla de negocio implementada.

**Dependencias de entrada:** Ninguna (sprint inicial).

---

### [SETUP] Tarea 1.1: Instalar y Fijar Dependencias npm

**Prioridad:** Critica | **Tamano:** S

**Descripcion:**
Instalar el stack completo de dependencias con versiones exactas (sin caret) para garantizar
reproducibilidad. Verificar que `npm run build` pasa sin errores despues de la instalacion.

**Criterios de Aceptacion:**
- [ ] `@mui/material@5.x` y `@emotion/react @emotion/styled` instalados
- [ ] `@mui/icons-material@5.x` instalado
- [ ] `zustand@4.x` instalado
- [ ] `@tanstack/react-query@5.x` instalado
- [ ] `axios@1.x` instalado
- [ ] `react-router-dom@6.x` instalado
- [ ] `js-cookie@3.x` e `@types/js-cookie` instalados
- [ ] Herramientas de test: `vitest`, `@testing-library/react`, `@testing-library/user-event`,
      `@testing-library/jest-dom`, `msw@2.x` instalados como devDependencies
- [ ] `scripts.test` y `scripts.type-check` agregados a `package.json`
- [ ] `npm run build` y `npm run lint` pasan sin errores
- [ ] `package-lock.json` actualizado y commiteado

**Archivos afectados:**
- `frontend/package.json`
- `frontend/package-lock.json`
- `frontend/vite.config.ts` (agregar configuracion de Vitest)
- `frontend/tsconfig.app.json` (verificar `"types": ["vitest/globals"]`)

---

### [SETUP] Tarea 1.2: Configurar MUI Theme

**Prioridad:** Alta | **Tamano:** S

**Descripcion:**
Crear el tema de MUI con los colores de marca y tipografia base. Envolver la aplicacion con
`ThemeProvider`. No se definen breakpoints custom en este sprint.

**Criterios de Aceptacion:**
- [ ] `frontend/src/theme/theme.ts` creado con `createTheme()`
- [ ] Colores primarios y secundarios de marca definidos (paleta QuickStack)
- [ ] Tipografia: fuente Roboto (default de MUI) con tamanos base confirmados
- [ ] `CssBaseline` incluido en `main.tsx` para reset de estilos
- [ ] `ThemeProvider` envuelve `<App>` en `main.tsx`
- [ ] El tema carga sin errores en dev server

**Archivos afectados:**
- `frontend/src/theme/theme.ts` (nuevo)
- `frontend/src/main.tsx`

---

### [SETUP] Tarea 1.3: Configurar React Router con Estructura de Rutas

**Prioridad:** Alta | **Tamano:** S

**Descripcion:**
Definir la estructura de rutas de la aplicacion. Las rutas protegidas usaran un componente
placeholder hasta que `ProtectedRoute` sea implementado en Sprint 3.

**Rutas a definir:**

| Ruta | Componente | Acceso |
|------|------------|--------|
| `/login` | `LoginPage` | Publico |
| `/register` | `RegisterPage` | Publico |
| `/forgot-password` | `ForgotPasswordPage` | Publico |
| `/reset-password` | `ResetPasswordPage` | Publico (con token en query param) |
| `/dashboard` | `DashboardPage` | Protegido |
| `/` | Redirect a `/dashboard` | — |
| `*` | Pagina 404 simple | — |

**Criterios de Aceptacion:**
- [ ] `frontend/src/router/AppRouter.tsx` creado con `createBrowserRouter`
- [ ] Todas las rutas definidas con componentes placeholder donde el feature no existe aun
- [ ] `<RouterProvider>` envuelve la app en `main.tsx`
- [ ] Navegar a `/login`, `/register`, `/dashboard` devuelve componente correcto (aunque sea
      placeholder)
- [ ] Ruta `*` devuelve texto "404 Not Found"

**Archivos afectados:**
- `frontend/src/router/AppRouter.tsx` (nuevo)
- `frontend/src/main.tsx`

---

### [SETUP] Tarea 1.4: Configurar Axios con Interceptors de Auth

**Prioridad:** Critica | **Tamano:** M

**Descripcion:**
Crear la instancia de Axios que toda la aplicacion usara para llamadas a la API. Los
interceptors deben:
1. Agregar el `Authorization: Bearer <token>` a cada request (si el token existe en el
   store de Zustand).
2. Interceptar respuestas 401 para intentar un refresh silencioso **una sola vez** por
   request. Si el refresh falla, limpiar el store y redirigir a `/login`.

**Comportamiento critico del interceptor de refresh:**
- Si el backend devuelve 401 en cualquier endpoint protegido, el interceptor debe:
  1. Llamar a `POST /api/v1/auth/refresh` (el browser envia la cookie automaticamente).
  2. Si el refresh tiene exito: guardar el nuevo access token, reintentar el request original.
  3. Si el refresh falla (401 o error de red): limpiar `authStore`, redirigir a `/login`.
- Evitar "refresh loops": usar una bandera `isRefreshing` y encolar requests concurrentes
  que llegaron durante el refresh.

**Criterios de Aceptacion:**
- [ ] `frontend/src/utils/axiosInstance.ts` creado
- [ ] `baseURL` leida desde `import.meta.env.VITE_API_BASE_URL`
- [ ] Interceptor de request: agrega `Authorization: Bearer` si `authStore.accessToken`
      existe
- [ ] Interceptor de respuesta: detecta 401, intenta refresh una vez, reintenta request
      original con nuevo token
- [ ] Si refresh falla: llama a `authStore.clearAuth()` y redirige a `/login`
- [ ] Bandera `isRefreshing` previene multiples llamadas concurrentes a `/refresh`
- [ ] `frontend/.env.local` (no commiteado) define `VITE_API_BASE_URL=http://localhost:8080`
- [ ] `frontend/.env.example` (commiteado) documenta la variable con valor de ejemplo

**Archivos afectados:**
- `frontend/src/utils/axiosInstance.ts` (nuevo)
- `frontend/.env.example` (nuevo)
- `frontend/.gitignore` (verificar que `.env.local` esta ignorado)

---

### [SETUP] Tarea 1.5: Configurar Zustand Auth Store

**Prioridad:** Critica | **Tamano:** S

**Descripcion:**
Crear el store de Zustand que mantiene el estado de autenticacion. Este store es la fuente
unica de verdad para el estado de auth en toda la aplicacion.

**Estado del store:**

```typescript
interface AuthState {
  accessToken: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

interface AuthActions {
  setAuth: (token: string, user: AuthUser) => void;
  clearAuth: () => void;
  setLoading: (loading: boolean) => void;
}
```

**Criterios de Aceptacion:**
- [ ] `frontend/src/stores/authStore.ts` creado con Zustand
- [ ] `frontend/src/types/auth.ts` creado con interfaces TypeScript:
      `AuthUser`, `AuthResponse`, `LoginRequest`, `RegisterRequest`,
      `ForgotPasswordRequest`, `ResetPasswordRequest`
- [ ] `setAuth()` guarda token y usuario, setea `isAuthenticated: true`
- [ ] `clearAuth()` resetea todo el estado a valores iniciales
- [ ] El store NO persiste en `localStorage` (el access token es in-memory por seguridad;
      el refresh token esta en cookie HttpOnly gestionada por el browser)
- [ ] Test unitario: `authStore.test.ts` verifica `setAuth`, `clearAuth`, estado inicial

**Archivos afectados:**
- `frontend/src/stores/authStore.ts` (nuevo)
- `frontend/src/types/auth.ts` (nuevo)
- `frontend/src/stores/__tests__/authStore.test.ts` (nuevo)

---

### [SETUP] Tarea 1.6: Configurar TanStack Query Client

**Prioridad:** Alta | **Tamano:** S

**Descripcion:**
Crear e inicializar el `QueryClient` con configuraciones por defecto apropiadas para la
aplicacion. Envolver la app con `QueryClientProvider`.

**Criterios de Aceptacion:**
- [ ] `QueryClient` creado con:
  - `defaultOptions.queries.retry: 1` (no reintentar indefinidamente errores 4xx)
  - `defaultOptions.queries.staleTime: 30_000` (30 segundos)
  - `defaultOptions.queries.refetchOnWindowFocus: false` (evitar refetches inesperados)
- [ ] `QueryClientProvider` envuelve la app en `main.tsx`
- [ ] `ReactQueryDevtools` habilitado solo en desarrollo (`import.meta.env.DEV`)
- [ ] La instancia del `QueryClient` esta en un archivo separado para facilitar pruebas

**Archivos afectados:**
- `frontend/src/lib/queryClient.ts` (nuevo)
- `frontend/src/main.tsx`

---

### Definition of Done — Sprint 1 ✅

- [x] `npm install` completa sin errores en un checkout limpio
- [x] `npm run dev` levanta el servidor en puerto 5173 sin errores en consola
- [x] `npm run build` produce un bundle sin errores ni warnings de TypeScript
- [x] `npm run type-check` pasa sin errores
- [x] `npm run lint` pasa sin errores ni warnings
- [x] `npm run test` pasa (5 tests de `authStore.test.ts`)
- [x] `axiosInstance` configurado y apunta a `VITE_API_BASE_URL`
- [x] `authStore` con estado inicial correcto
- [x] App renderiza en `/login` aunque sea un placeholder
- [x] Sin secrets hardcodeados en codigo fuente

**Resultado:** 5 tests pasando | React 19 + MUI 5.17 + Zustand 4.5 + TanStack Query 5.76

---

## Sprint 2: Auth Core — Login y Registro

**Duracion:** 1 dia | **Status:** ✅ COMPLETADO (2026-02-18)

**Objetivo:** Implementar las pantallas de Login y Registro funcionales, conectadas al
backend. Al finalizar, un usuario puede crear una cuenta y hacer login, recibiendo el
access token en el store.

**Dependencias de entrada:** Sprint 1 completado (`axiosInstance`, `authStore`, routing).

---

### [FEATURE] Tarea 2.1: Auth API Service

**Prioridad:** Critica | **Tamano:** S

**Descripcion:**
Crear el modulo de funciones que encapsula todas las llamadas HTTP a los endpoints de auth.
Estas funciones son puras (no tienen estado propio) y usan `axiosInstance`.

**Criterios de Aceptacion:**
- [ ] `frontend/src/services/authApi.ts` creado con funciones:
  - `login(data: LoginRequest): Promise<AuthResponse>`
  - `register(data: RegisterRequest): Promise<void>`
  - `refreshToken(): Promise<{ accessToken: string }>`
  - `logout(): Promise<void>`
  - `forgotPassword(data: ForgotPasswordRequest): Promise<void>`
  - `resetPassword(data: ResetPasswordRequest): Promise<void>`
  - `getMe(): Promise<AuthUser>`
- [ ] Cada funcion usa `axiosInstance` (no `axios` directo)
- [ ] Tipos de retorno correctos segun las interfaces en `auth.ts`
- [ ] Sin logica de negocio en este archivo (solo mapeo de HTTP a tipos)

**Archivos afectados:**
- `frontend/src/services/authApi.ts` (nuevo)

---

### [FEATURE] Tarea 2.2: Custom Hooks de Auth con TanStack Query

**Prioridad:** Alta | **Tamano:** S

**Descripcion:**
Crear custom hooks que encapsulan las mutaciones y queries de auth usando TanStack Query.
Estos hooks son los que usan los componentes, no las funciones de `authApi` directamente.

**Criterios de Aceptacion:**
- [ ] `frontend/src/hooks/useAuthQuery.ts` creado con hooks:
  - `useLogin()`: `useMutation` que llama a `authApi.login()`, en onSuccess guarda token en
    `authStore.setAuth()` y navega a `/dashboard`
  - `useRegister()`: `useMutation` que llama a `authApi.register()`, en onSuccess navega a
    `/login` con mensaje de exito
  - `useLogout()`: `useMutation` que llama a `authApi.logout()`, en onSuccess llama a
    `authStore.clearAuth()` y navega a `/login`
  - `useForgotPassword()`: `useMutation` para `authApi.forgotPassword()`
  - `useResetPassword()`: `useMutation` para `authApi.resetPassword()`
  - `useCurrentUser()`: `useQuery` que llama a `authApi.getMe()` solo si
    `authStore.isAuthenticated` es true
- [ ] Cada hook expone: `mutate/mutateAsync`, `isPending`, `isError`, `error`
- [ ] Errores de backend (422, 400, 423, 429) son accesibles via `error.response.data`

**Archivos afectados:**
- `frontend/src/hooks/useAuthQuery.ts` (nuevo)

---

### [FEATURE] Tarea 2.3: Pagina de Login

**Prioridad:** Critica | **Tamano:** M

**Descripcion:**
Implementar la pantalla de login usando MUI. Formulario simple, sin dependencias de estado
externas mas alla del hook `useLogin()`.

**Diseno de la pantalla:**
- Centrada verticalmente y horizontalmente en la pantalla completa
- Card de MUI con: logo/titulo de la app, campo email, campo password con toggle de
  visibilidad, boton "Iniciar sesion"
- Link "Olvidaste tu contrasena?" apuntando a `/forgot-password`
- Link "Crear cuenta" apuntando a `/register`
- Si el usuario ya esta autenticado (`isAuthenticated`), redirigir a `/dashboard`

**Manejo de errores:**
- 401: mostrar "Email o contrasena incorrectos"
- 423: mostrar "Cuenta bloqueada. Intenta de nuevo en X minutos" (parsear `X-Locked-Until`)
- 429: mostrar "Demasiados intentos. Espera X segundos" (parsear `Retry-After`)
- Error de red: mostrar "Sin conexion. Verifica tu internet"

**Criterios de Aceptacion:**
- [ ] `frontend/src/features/auth/LoginPage.tsx` creado
- [ ] Campos: email (type=email, required), password (type=password, con toggle)
- [ ] Boton deshabilitado y con spinner mientras `isPending`
- [ ] Errores del backend mostrados con `Alert` de MUI (no solo `console.error`)
- [ ] Si ya autenticado, redirige a `/dashboard` sin renderizar el formulario
- [ ] Test: `LoginPage.test.tsx` con MSW mockea el endpoint y verifica:
  - Renderiza los campos
  - Submit exitoso redirige a `/dashboard`
  - Submit con error 401 muestra mensaje de error
  - El boton se deshabilita durante el submit

**Archivos afectados:**
- `frontend/src/features/auth/LoginPage.tsx` (nuevo)
- `frontend/src/features/auth/__tests__/LoginPage.test.tsx` (nuevo)

---

### [FEATURE] Tarea 2.4: Pagina de Registro

**Prioridad:** Alta | **Tamano:** M

**Descripcion:**
Implementar la pantalla de registro para nuevos tenants OWNER. En MVP solo se registran
OWNERs (el tenant se crea automaticamente en el backend junto con el primer usuario).

**Diseno de la pantalla:**
- Similar a login: centrada, Card con formulario
- Campos: nombre completo, email, password, confirmar password
- Validacion de password identica en el cliente antes de enviar (evitar round-trip)
- Link "Ya tienes cuenta? Inicia sesion" apuntando a `/login`

**Manejo de errores:**
- 409 (email ya existe): mostrar "Este email ya esta registrado"
- 400 (password comprometida, HIBP): mostrar el mensaje del backend verbatim
- Passwords no coinciden: validacion inline, no llegar al server

**Criterios de Aceptacion:**
- [ ] `frontend/src/features/auth/RegisterPage.tsx` creado
- [ ] Campos: `fullName`, `email`, `password`, `confirmPassword`
- [ ] Validacion de coincidencia de passwords antes del submit
- [ ] Indicador de fortaleza de password (al menos un indicador visual basico)
- [ ] Registro exitoso: navegar a `/login` con query param `?registered=true` que muestra
      un `Alert` de confirmacion en la pagina de login
- [ ] Test: `RegisterPage.test.tsx` verifica flujo exitoso y error de email duplicado

**Archivos afectados:**
- `frontend/src/features/auth/RegisterPage.tsx` (nuevo)
- `frontend/src/features/auth/__tests__/RegisterPage.test.tsx` (nuevo)

---

### [QA] Tarea 2.5: MSW Handlers para Auth Endpoints

**Prioridad:** Alta | **Tamano:** S

**Descripcion:**
Configurar Mock Service Worker (MSW) como capa de mock para todos los tests de componentes
de auth. Crear handlers reutilizables para los 8 endpoints.

**Criterios de Aceptacion:**
- [ ] `frontend/src/mocks/handlers/authHandlers.ts` creado con handlers para:
  - `POST /auth/login` — exito (200) y error (401, 423, 429)
  - `POST /auth/register` — exito (201) y error (409)
  - `POST /auth/logout` — exito (204)
  - `GET /auth/me` — exito con usuario de prueba
  - `POST /auth/refresh` — exito (200) y fallo (401)
- [ ] `frontend/src/mocks/server.ts` configurado con `setupServer(...handlers)`
- [ ] `frontend/src/setupTests.ts` inicia y detiene el server MSW correctamente
- [ ] Tests de LoginPage y RegisterPage usan MSW (no mocks manuales de axios)

**Archivos afectados:**
- `frontend/src/mocks/handlers/authHandlers.ts` (nuevo)
- `frontend/src/mocks/server.ts` (nuevo)
- `frontend/src/setupTests.ts` (nuevo)
- `frontend/vite.config.ts` (agregar `setupFiles` a configuracion de Vitest)

---

### Definition of Done — Sprint 2 ✅

- [x] Un usuario puede hacer login con credenciales validas y llegar al dashboard
- [x] Un usuario puede registrarse y es redirigido a login
- [x] Mensajes de error especificos mostrados para 401, 423, 429 en login
- [x] `npm run test` pasa incluyendo `LoginPage.test.tsx` y `RegisterPage.test.tsx`
- [x] Ninguna llamada directa a `axios` (siempre `axiosInstance`)
- [x] Sin tokens o passwords en `console.log`

**Resultado:** 23 tests pasando (5 authStore + 10 LoginPage + 8 RegisterPage)

---

## Sprint 3: Auth Completo — Reset, Rutas Protegidas y Auto-Refresh

**Duracion:** 1 dia | **Status:** ✅ COMPLETADO (2026-02-18)

**Objetivo:** Completar el flujo de autenticacion (recuperacion de password, rutas
protegidas) y garantizar que el token se refresca automaticamente de forma silenciosa.

**Dependencias de entrada:** Sprint 2 completado (`authApi`, `useAuthQuery`, `axiosInstance`
con interceptors, `authStore`).

---

### [FEATURE] Tarea 3.1: Pagina de Forgot Password

**Prioridad:** Alta | **Tamano:** S

**Descripcion:**
Pantalla simple con un campo de email para solicitar el reset de password.

**Comportamiento critico:** El backend siempre retorna 200 independientemente de si el email
existe. El frontend debe reflejar esto mostrando siempre el mensaje de exito despues del
submit, sin confirmar ni negar si el email existe en el sistema.

**Criterios de Aceptacion:**
- [ ] `frontend/src/features/auth/ForgotPasswordPage.tsx` creado
- [ ] Campo email con validacion de formato
- [ ] Despues de submit (exito o error de red): mostrar mensaje
      "Si el email existe, recibiras instrucciones en breve" (no distinguir casos)
- [ ] En caso de error de red: loggear el error internamente pero mostrar el mismo mensaje
      generico al usuario (no exponer estado del sistema)
- [ ] Boton "Volver al login" siempre visible
- [ ] Test: verifica que el mismo mensaje se muestra para exito y error 404 (si ocurriera)

**Archivos afectados:**
- `frontend/src/features/auth/ForgotPasswordPage.tsx` (nuevo)

---

### [FEATURE] Tarea 3.2: Pagina de Reset Password

**Prioridad:** Alta | **Tamano:** S

**Descripcion:**
Pantalla para definir la nueva password. El token llega como query param `?token=` en la
URL (el email de reset lo incluye). Si no hay token en la URL, redirigir a `/forgot-password`.

**Criterios de Aceptacion:**
- [ ] `frontend/src/features/auth/ResetPasswordPage.tsx` creado
- [ ] Lee token desde `useSearchParams()` de React Router
- [ ] Si no hay token: redirigir a `/forgot-password` con mensaje de error
- [ ] Campos: nueva password, confirmar nueva password
- [ ] Validacion de coincidencia antes del submit
- [ ] Exito: mostrar mensaje de confirmacion y link a `/login`
- [ ] Error 400 (token invalido/expirado): mostrar "El enlace de recuperacion ha expirado.
      Solicita uno nuevo." con link a `/forgot-password`
- [ ] Test: verifica redirecciones y mensajes de error

**Archivos afectados:**
- `frontend/src/features/auth/ResetPasswordPage.tsx` (nuevo)

---

### [FEATURE] Tarea 3.3: Protected Route Component

**Prioridad:** Critica | **Tamano:** S

**Descripcion:**
Componente que envuelve rutas que requieren autenticacion. Si el usuario no esta
autenticado, redirige a `/login` preservando la URL original para post-login redirect.

**Criterios de Aceptacion:**
- [ ] `frontend/src/components/common/ProtectedRoute.tsx` creado
- [ ] Si `authStore.isAuthenticated` es `false`: `<Navigate to="/login" state={{ from: location }} replace />`
- [ ] Si `authStore.isLoading` es `true`: mostrar `LoadingSpinner`
- [ ] Si autenticado: renderizar `<Outlet />` (o children segun el patron elegido)
- [ ] `AppRouter.tsx` actualizado: `/dashboard` y rutas futuras envueltas en `ProtectedRoute`
- [ ] Post-login: `useLogin()` lee `location.state.from` y redirige a esa URL si existe,
      sino a `/dashboard`
- [ ] Test: verifica redireccion a `/login` cuando no autenticado

**Archivos afectados:**
- `frontend/src/components/common/ProtectedRoute.tsx` (nuevo)
- `frontend/src/router/AppRouter.tsx` (actualizar)
- `frontend/src/hooks/useAuthQuery.ts` (actualizar `useLogin` para leer `state.from`)

---

### [FEATURE] Tarea 3.4: Completar Interceptor de Auto-Refresh

**Prioridad:** Critica | **Tamano:** M

**Descripcion:**
El interceptor en `axiosInstance.ts` fue parcialmente implementado en Sprint 1. Esta tarea
lo completa y lo prueba con el store de auth real y el handler MSW de refresh.

**El flujo completo a validar:**
1. Request protegido se realiza con access token valido → respuesta normal.
2. Access token expira → backend devuelve 401.
3. Interceptor detecta 401, llama a `POST /auth/refresh`.
4. Refresh exitoso → interceptor actualiza `authStore` con nuevo token, reintenta el
   request original con el nuevo token.
5. Refresh falla (sesion expirada) → `authStore.clearAuth()`, redirigir a `/login`.
6. Multiples requests concurrentes con token expirado → solo UNA llamada a refresh, el
   resto esperan y se reintentan con el nuevo token.

**Criterios de Aceptacion:**
- [ ] El interceptor de 401 llama a `authStore.setAuth()` con el nuevo token tras refresh
      exitoso
- [ ] El request original se reintenta automaticamente despues del refresh
- [ ] Si refresh retorna 401: `authStore.clearAuth()` + redirect a `/login`
- [ ] La cola de requests concurrentes se resuelve correctamente (no hay requests perdidos)
- [ ] Test: `axiosInterceptor.test.ts` usando MSW verifica el flujo completo de refresh
      y el caso de fallo

**Archivos afectados:**
- `frontend/src/utils/axiosInstance.ts` (completar)
- `frontend/src/utils/__tests__/axiosInterceptor.test.ts` (nuevo)
- `frontend/src/mocks/handlers/authHandlers.ts` (agregar handler de refresh si no existe)

---

### [FEATURE] Tarea 3.5: Logout

**Prioridad:** Alta | **Tamano:** S

**Descripcion:**
El logout debe:
1. Llamar a `POST /api/v1/auth/logout` (invalida el refresh token en el servidor).
2. Limpiar el access token del store.
3. El browser elimina la cookie `__Host-refreshToken` via el `Set-Cookie: Max-Age=0` que
   el servidor envia en la respuesta de logout.
4. Redirigir a `/login`.

**Criterios de Aceptacion:**
- [ ] `useLogout()` hook implementado en `useAuthQuery.ts`
- [ ] Si la llamada al server falla (network error): ejecutar `clearAuth()` y redirigir
      de todas formas (logout local como fallback)
- [ ] Boton de logout disponible en `TopBar.tsx` (Sprint 4), pero el hook debe estar listo
- [ ] Test: verifica que `clearAuth()` se llama y que se redirige a `/login` en exito y
      en fallo de red

**Archivos afectados:**
- `frontend/src/hooks/useAuthQuery.ts` (agregar/completar `useLogout`)

---

### [QA] Tarea 3.6: Tests de ProtectedRoute y Flujos de Redireccion

**Prioridad:** Alta | **Tamano:** S

**Descripcion:**
Tests que verifican el comportamiento de las rutas protegidas bajo distintos estados de
autenticacion.

**Criterios de Aceptacion:**
- [ ] Test: usuario no autenticado en `/dashboard` → redirigido a `/login`
- [ ] Test: usuario autenticado en `/login` → redirigido a `/dashboard`
- [ ] Test: post-login, usuario llega a la URL original que intentaba visitar
- [ ] Test: refresh exitoso permite que request original continue
- [ ] Test: refresh fallido lleva al usuario a `/login`

**Archivos afectados:**
- `frontend/src/components/common/__tests__/ProtectedRoute.test.tsx` (nuevo)

---

### Definition of Done — Sprint 3 ✅

- [x] Flujo completo de recuperacion de password funciona end-to-end con backend
- [x] Rutas protegidas redirigen a `/login` si no autenticado
- [x] El token se refresca silenciosamente cuando expira (sin interrupcion al usuario)
- [x] Logout limpia sesion local y del servidor
- [x] `npm run test` pasa incluyendo tests de interceptor y ProtectedRoute
- [x] No hay `window.location.href` hardcodeado — toda navegacion via React Router

**Resultado:** 38 tests totales pasando (8 axiosInterceptor + 7 ProtectedRoute + 23 previos)

**Implementado:**
- ForgotPasswordPage con timing-safe submit
- ResetPasswordPage con validación de token desde URL
- ProtectedRoute con Navigate + Outlet pattern y state.from para post-login redirect
- imperativeNavigate para navegación desde interceptores Axios
- axiosInstance mejorado: reemplaza window.location.href con imperativeNavigate('/login')
- Router config separado en router.tsx para evitar errores de react-refresh

---

## Sprint 4: Dashboard Base + Calidad y Cierre

**Duracion:** 1 dia | **Status:** ⏳ SIGUIENTE (pendiente de inicio)

**Objetivo:** Implementar el layout de la aplicacion post-login (sidebar, topbar, dashboard
placeholder), agregar manejo global de errores, y cerrar Phase 0.4 con el smoke test del
flujo completo.

**Dependencias de entrada:** Sprint 3 completado (autenticacion funcional, rutas
protegidas operativas).

---

### [FEATURE] Tarea 4.1: AppLayout — Sidebar y TopBar

**Prioridad:** Alta | **Tamano:** M

**Descripcion:**
Implementar el layout principal de la aplicacion que se mostrara en todas las pantallas
autenticadas. El layout es un wrapper que recibe el contenido de la pagina como `<Outlet>`.

**Diseno del Layout:**

```
+--------------------------------------------------+
|  TopBar: [Logo]         [Usuario: Ana G.] [Logout]|
+----------+---------------------------------------+
| Sidebar  |                                       |
| (240px)  |    <Outlet /> (contenido de pagina)   |
| NavItems |                                       |
|          |                                       |
+----------+---------------------------------------+
```

**Sidebar — items de navegacion para Phase 0.4:**
- Dashboard (icono: Home) → `/dashboard`
- (Placeholders colapsados para fases futuras: Catalogo, Pedidos, Inventario, Reportes)

**TopBar:**
- Nombre del usuario (`authStore.user.fullName`) o email como fallback
- Boton de logout (icono + texto "Cerrar sesion") que llama a `useLogout()`
- En mobile: hamburger menu para colapsar sidebar (comportamiento responsive basico)

**Criterios de Aceptacion:**
- [ ] `frontend/src/components/layout/AppLayout.tsx` creado
- [ ] `frontend/src/components/layout/Sidebar.tsx` creado con items de navegacion
- [ ] `frontend/src/components/layout/TopBar.tsx` creado con info de usuario y logout
- [ ] `AppRouter.tsx` actualizado: rutas autenticadas usan `AppLayout` como wrapper
- [ ] Layout responsive: sidebar colapsable en pantallas menores a `md` breakpoint de MUI
- [ ] Link de navegacion activo visualmente distinguible (color diferente o indicador)

**Archivos afectados:**
- `frontend/src/components/layout/AppLayout.tsx` (nuevo)
- `frontend/src/components/layout/Sidebar.tsx` (nuevo)
- `frontend/src/components/layout/TopBar.tsx` (nuevo)
- `frontend/src/router/AppRouter.tsx` (actualizar con layout nested)

---

### [FEATURE] Tarea 4.2: Dashboard Page Placeholder

**Prioridad:** Media | **Tamano:** S

**Descripcion:**
Pagina de dashboard que el usuario ve inmediatamente despues del login. Es un placeholder
que sera reemplazado en fases posteriores con metricas reales.

**Criterios de Aceptacion:**
- [ ] `frontend/src/pages/DashboardPage.tsx` creado
- [ ] Muestra: titulo "Dashboard", nombre del tenant/restaurante, mensaje de bienvenida
- [ ] Muestra el nombre del usuario obtenido de `authStore.user`
- [ ] Incluye un `Card` de MUI con estado del sistema (texto placeholder: "POS funcionando")
- [ ] No hace llamadas adicionales a la API (usa solo el estado del store)

**Archivos afectados:**
- `frontend/src/pages/DashboardPage.tsx` (nuevo)

---

### [FEATURE] Tarea 4.3: Error Boundary Global

**Prioridad:** Alta | **Tamano:** S

**Descripcion:**
Implementar un `ErrorBoundary` de React que capture errores no manejados en el arbol de
componentes y muestre una pantalla de error amigable en lugar de un crash blanco.

**Criterios de Aceptacion:**
- [ ] `frontend/src/components/common/ErrorBoundary.tsx` creado como class component
      (requisito de la API de Error Boundaries de React)
- [ ] Pantalla de fallback con mensaje: "Ocurrio un error inesperado. Recarga la pagina."
      y boton "Recargar"
- [ ] `ErrorBoundary` envuelve el `RouterProvider` en `main.tsx`
- [ ] El error original se envia a `console.error` (para poder capturarlo con Sentry en
      fases futuras)
- [ ] En desarrollo, mostrar el stack trace del error (usando `import.meta.env.DEV`)

**Archivos afectados:**
- `frontend/src/components/common/ErrorBoundary.tsx` (nuevo)
- `frontend/src/main.tsx` (agregar como wrapper)

---

### [FEATURE] Tarea 4.4: Manejo de Errores de Red Global con TanStack Query

**Prioridad:** Media | **Tamano:** S

**Descripcion:**
Configurar un handler global de errores en `QueryClient` que muestre un Snackbar/Toast
de MUI cuando una query o mutation falla con un error inesperado (5xx, network error).
Los errores 4xx esperados (401, 422) son manejados por cada componente individualmente.

**Criterios de Aceptacion:**
- [ ] `queryClient.ts` actualizado con `onError` en `defaultOptions`
- [ ] Errores 5xx: mostrar Snackbar con mensaje "Error del servidor. Intenta de nuevo."
- [ ] Errores de red (`code === 'ERR_NETWORK'`): mostrar Snackbar con mensaje
      "Sin conexion a internet."
- [ ] Errores 4xx: **no** mostrados por el handler global (manejados en componentes)
- [ ] Snackbar se cierra automaticamente despues de 5 segundos
- [ ] Un `SnackbarProvider` (o equivalent en MUI) envuelve la app

**Archivos afectados:**
- `frontend/src/lib/queryClient.ts` (actualizar)
- `frontend/src/main.tsx` (agregar provider de Snackbar si es necesario)

---

### [QA] Tarea 4.5: Smoke Test del Flujo Completo

**Prioridad:** Critica | **Tamano:** M

**Descripcion:**
Verificar manualmente y documentar el flujo critico de extremo a extremo contra el backend
real en produccion (no MSW). Este test no es automatizado — es un checklist de verificacion
previo al cierre de la fase.

**Checklist del Smoke Test:**

**Flujo de Registro:**
- [ ] Ir a `/register` sin estar autenticado
- [ ] Registrar un nuevo usuario con email unico
- [ ] Verificar redireccion a `/login` con mensaje de confirmacion
- [ ] Verificar que no se puede registrar el mismo email dos veces (mensaje de error)

**Flujo de Login:**
- [ ] Login con credenciales validas → llega a `/dashboard`
- [ ] Login con password incorrecta → mensaje de error claro
- [ ] Hacer refresh de la pagina en `/dashboard` → permanece en dashboard (access token
      se recupera via refresh silencioso)
- [ ] Intentar acceder a `/dashboard` sin estar autenticado → redirigido a `/login`

**Flujo de Token Refresh:**
- [ ] Con DevTools: interceptar y verificar que el header `Authorization: Bearer` esta
      presente en requests al backend
- [ ] Esperar 15 minutos (o simular expiracion en DevTools): el siguiente request debe
      refrescar el token silenciosamente sin mostrar pantalla de login

**Flujo de Logout:**
- [ ] Click en "Cerrar sesion" → redirigido a `/login`
- [ ] Intentar volver a `/dashboard` despues del logout → redirigido a `/login`

**Flujo de Recuperacion de Password:**
- [ ] Ir a `/forgot-password`, ingresar email → mensaje generico mostrado
- [ ] Con el token del email (en dev): ir a `/reset-password?token=...` y cambiar password
- [ ] Login con nueva password funciona
- [ ] Login con password anterior falla

**Archivos afectados:**
- No genera archivos de codigo. Resultado documentado en PR de cierre de Phase 0.4.

---

### [QA] Tarea 4.6: Verificacion de Variables de Entorno en Vercel

**Prioridad:** Alta | **Tamano:** S

**Descripcion:**
Verificar que la variable `VITE_API_BASE_URL` esta configurada correctamente en el
proyecto de Vercel para que el frontend en produccion apunte al backend en Render.

**Criterios de Aceptacion:**
- [ ] `VITE_API_BASE_URL=https://quickstack-api.onrender.com` configurado en Vercel
      (Environment Variables → Production)
- [ ] Build de produccion en Vercel pasa sin errores
- [ ] El frontend en `https://quickstack-drab.vercel.app` realiza requests al backend
      correcto (verificar en Network tab)
- [ ] CORS no bloquea requests desde Vercel al backend (ya configurado en Phase 0.2, pero
      verificar que el dominio exacto de Vercel esta en la lista blanca del backend)

**Archivos afectados:**
- Configuracion de Vercel (no archivos de codigo).

---

### [QA] Tarea 4.7: Auditoria de Seguridad Frontend

**Prioridad:** Alta | **Tamano:** S

**Descripcion:**
Revision enfocada en los aspectos de seguridad del frontend antes del cierre de la fase.

**Checklist de Seguridad:**

**Tokens:**
- [ ] El access token nunca se escribe en `localStorage` ni `sessionStorage`
      (permanece en memoria Zustand)
- [ ] El access token nunca aparece en logs de consola
- [ ] No hay tokens hardcodeados en el codigo fuente
- [ ] `npm audit` sin vulnerabilidades criticas o altas

**XSS:**
- [ ] No se usa `dangerouslySetInnerHTML` en ningun componente
- [ ] Todos los datos de usuario son renderizados via JSX (React escapa automaticamente)

**Configuracion:**
- [ ] `.env.local` esta en `.gitignore`
- [ ] `.env.example` commiteado con valores de ejemplo (sin secretos reales)
- [ ] No hay URLs de API hardcodeadas en el codigo (siempre via `import.meta.env`)

**Red:**
- [ ] Todos los requests usan `axiosInstance` (nunca `fetch` directo ni `axios` sin la
      instancia configurada)
- [ ] HTTPS en produccion (garantizado por Vercel y Render)

**Archivos afectados:**
- Ningun archivo nuevo. Correcciones aplicadas donde se encuentren issues.

---

### [QA] Tarea 4.8: Actualizacion de Documentacion

**Prioridad:** Media | **Tamano:** S

**Descripcion:**
Actualizar los documentos del proyecto para reflejar el estado completado de Phase 0.4.

**Criterios de Aceptacion:**
- [ ] `docs/ROADMAP.md` actualizado: Phase 0.4 marcada como completada, con resumen
      de entregables (numero de componentes, tests, etc.)
- [ ] `CLAUDE.md` (raiz) actualizado: estado actual pasa a "Phase 0.4 COMPLETADO"
      y "Proximo: Phase 0.5 o Phase 1 (segun prioridad)"
- [ ] `docs/ARCHITECTURE.md` actualizado: seccion Frontend refleja la estructura
      implementada de `frontend/src/`
- [ ] `frontend/CLAUDE.md` actualizado: estructura de directorios y convenciones finales

**Archivos afectados:**
- `docs/ROADMAP.md`
- `CLAUDE.md`
- `docs/ARCHITECTURE.md`
- `frontend/CLAUDE.md`

---

### Definition of Done — Sprint 4

- [ ] Usuario puede completar el flujo completo: registro → login → dashboard → logout
- [ ] Token se refresca automaticamente en segundo plano
- [ ] Layout con sidebar y topbar renderiza correctamente en desktop y mobile
- [ ] `npm run test` pasa con todos los tests de la fase
- [ ] Smoke test del flujo completo contra backend de produccion: OK
- [ ] `npm audit` sin vulnerabilidades criticas
- [ ] Documentacion actualizada
- [ ] Phase 0.4 marcada como completada en ROADMAP.md

---

## Criterios de Exito de Phase 0.4

| Criterio | Metrica de Exito |
|----------|-----------------|
| Funcionalidad de auth | 5 flujos completos funcionando (registro, login, refresh, logout, reset) |
| Rutas protegidas | 100% de rutas autenticadas redirigen a login si no hay sesion |
| Auto-refresh | Token se renueva silenciosamente antes de que el usuario note expiracion |
| Testing | >= 80% de componentes de auth con tests de RTL |
| Seguridad | Access token nunca persiste fuera de memoria, `npm audit` limpio |
| Build | `npm run build` pasa sin errores de TypeScript ni warnings de ESLint |
| Deploy | Frontend en Vercel apunta correctamente al backend en Render |

---

## Registro de Riesgos

| ID | Riesgo | Probabilidad | Impacto | Mitigacion |
|----|--------|--------------|---------|------------|
| R1 | Cookie `__Host-refreshToken` con `SameSite=Strict` bloquea refresh en Vercel si el dominio del frontend no coincide exactamente con el del backend | Baja | Alto | Verificar configuracion CORS y cookie domain en Phase 0.2. Render y Vercel tienen dominios distintos — el backend debe servir la cookie sin `Domain` attribute para que el browser la envie. Probar en staging antes de dar por completado. |
| R2 | React Query refetch en background interfiere con el interceptor de refresh | Media | Medio | Configurar `refetchOnWindowFocus: false` y `staleTime` suficientemente alto para reducir refetches. El interceptor debe manejar colas concurrentes correctamente (Tarea 3.4). |
| R3 | El frontend scaffolded usa React 19, pero las versiones documentadas en ARCHITECTURE.md dicen React 18 | Alta | Bajo | Sprint 1 fija las versiones exactas. Si la compatibilidad de librerias (especialmente MUI 5.x) requiere ajuste, evaluar si actualizar ARCHITECTURE.md es preferible a degradar React. |
| R4 | Backend en Render con free tier tiene cold starts de hasta 60 segundos | Alta | Medio | Agregar indicador de carga en el login con mensaje "La API puede tardar hasta 60s en despertar" durante los primeros segundos de espera. No tratar el timeout como error inmediatamente. |
| R5 | MSW 2.x tiene breaking changes respecto a 1.x — la documentacion online puede referirse a la API antigua | Media | Bajo | Usar exclusivamente la documentacion de MSW 2.x. Los handlers usan `http.post(...)` no `rest.post(...)`. |
| R6 | Sesion de Zustand se pierde en refresh de pagina (estado en memoria, no persistido) | Alta | Medio | Este es el comportamiento correcto por seguridad. El interceptor de Axios debe hacer un refresh automatico al iniciar la app para revalidar la sesion via la cookie HttpOnly. Implementar `useAuthQuery.ts` con un `useEffect` en el componente raiz que llame a `GET /auth/me` al montar. |
| R7 | Vitest + RTL puede tener incompatibilidades con la version de React 19 si se mantiene | Baja | Medio | Verificar compatibilidad en Sprint 1. `@testing-library/react@16+` soporta React 19. |

---

## Deuda Tecnica Aceptada en Phase 0.4

| Deuda | Riesgo | Plan de Remediacion |
|-------|--------|---------------------|
| Sin tests de accesibilidad automatizados | Bajo | Agregar `axe-core` con `jest-axe` en Phase 1 |
| Sin internacionalizacion (i18n) | Bajo | Fuera del MVP — solo espanol |
| Sin manejo de MFA | Medio-Alto | TOTP para OWNER documentado en deuda de Phase 0.3 |
| Sin tests E2E (Playwright/Cypress) | Bajo | Evaluar para Phase 1 si el tiempo lo permite |
| Sidebar con items placeholder (no rutas reales) | Ninguno | Se completan en cada fase posterior |
| Sin skeleton loaders (solo spinner generico) | Bajo | Mejorar UX en Phase 1 si el piloto lo solicita |

---

## Protocolo de Seguridad por Sprint

Antes de cerrar cada PR de esta fase, verificar:

```markdown
## Security Checklist Frontend — Sprint [N]

### Tokens y Secretos
- [ ] Access token almacenado solo en Zustand (memoria), no en localStorage
- [ ] Sin tokens, passwords, ni secretos en console.log
- [ ] Sin valores de API keys hardcodeados en el codigo
- [ ] .env.local en .gitignore verificado

### XSS
- [ ] No se usa dangerouslySetInnerHTML
- [ ] Datos de usuario renderizados via JSX

### Dependencias
- [ ] npm audit sin vulnerabilidades criticas o altas nuevas

### Red
- [ ] Todos los requests HTTP usan axiosInstance
- [ ] HTTPS verificado en preview de Vercel
```

---

## Referencias

- [React Router v6 — Protected Routes](https://reactrouter.com/en/main/start/faq)
- [TanStack Query v5 — Error Handling](https://tanstack.com/query/v5/docs/react/guides/query-retries)
- [Zustand — TypeScript Guide](https://docs.pmnd.rs/zustand/guides/typescript)
- [MUI v5 — Theming](https://mui.com/material-ui/customization/theming/)
- [MSW 2.x — Getting Started](https://mswjs.io/docs/getting-started)
- [Axios — Interceptors](https://axios-http.com/docs/interceptors)
- [OWASP — Token Storage](https://cheatsheetseries.owasp.org/cheatsheets/HTML5_Security_Cheat_Sheet.html)

---

*Documento generado: 2026-02-18*
*Proxima revision: Post-Sprint 4 (cierre de Phase 0.4)*
