# Angular Migration — Post-MVP Roadmap

> **Última actualización:** 2026-02-24
> **Estado:** ⏳ Pendiente — Iniciar después de Phase 6 (piloto validado)
> **Estrategia:** Strangler Fig + despliegue paralelo en Vercel

---

## Por qué migrar

Este es un proyecto de aprendizaje. La decisión de migrar a Angular tiene dos objetivos explícitos:

1. **Aprender Angular** — El proyecto nació en React por practicidad del MVP, no por preferencia.
2. **Aprender a hacer migraciones en producción** — Post-MVP habrá al menos un usuario activo (el restaurante piloto). No es posible hacer downtime de días. La migración tiene que ocurrir mientras la aplicación está viva y siendo usada.

El segundo punto es el aprendizaje más valioso: cómo reemplazar una tecnología sin que el usuario final lo note.

---

## Restricción Principal

> **El restaurante piloto no puede perder acceso al POS durante la migración.**

Esto elimina el enfoque "big bang" (reescribir todo y hacer un cutover). La migración debe ser **incremental y reversible** en cada paso.

---

## Estrategia: Strangler Fig

El patrón [Strangler Fig](https://martinfowler.com/bliki/StranglerFigApplication.html) consiste en construir el sistema nuevo rodeando al antiguo, migrando funcionalidad ruta por ruta, hasta que el sistema viejo puede eliminarse.

```
Fase inicial:     React maneja el 100% del tráfico
        ↓
Migración:        Angular toma rutas una a una
        ↓
Cutover final:    React muerto, Angular maneja el 100%
```

### Cómo se implementa en Vercel

Vercel permite usar **Edge Middleware** (`middleware.ts`) para interceptar requests antes de que lleguen al frontend y redirigirlos a diferentes deployments según la ruta.

```
Usuario → quickstack.app → Edge Middleware
                                ↓
                    ┌───────────────────────┐
                    │  /login    → Angular  │
                    │  /register → Angular  │
                    │  /catalog  → React    │  ← aún sin migrar
                    │  /pos      → React    │  ← aún sin migrar
                    └───────────────────────┘
```

**Setup concreto:**
- `quickstack-react.vercel.app` — deployment React existente (sin tocar)
- `quickstack-angular.vercel.app` — deployment Angular nuevo
- `app.quickstack.com` — dominio principal con Edge Middleware que enruta por ruta
- Cada sprint: más rutas pasan de React a Angular
- Rollback inmediato: modificar el middleware y redeploy (segundos)

### Ventajas de este enfoque para el aprendizaje

| Lo que aprenderás | Cómo |
|------------------|------|
| Angular desde cero | Construyendo cada módulo |
| Arquitectura de migración incremental | Diseñando el strangler fig |
| Edge computing / middleware | Configurando Vercel Edge |
| Gestión de estado compartido entre versiones | Auth token válido en ambos apps |
| Rollback en producción | Modificando rewrites sin downtime |
| Observabilidad durante migración | Logs diferenciados por app |

---

## Prerequisitos para Iniciar

Antes de empezar M0, deben cumplirse todas estas condiciones:

- [ ] Phase 6 (Polish & Tables) completada
- [ ] Piloto validado — al menos 1 restaurante usando el POS en producción
- [ ] El backend está estable (sin sprints de features activos)
- [ ] Se dispone de tiempo dedicado sin presión de nuevos features del MVP

---

## Resumen de Fases

| Fase | Nombre | Duración est. | Rutas migradas |
|------|--------|--------------|----------------|
| **M0** | Setup + Arquitectura Angular | 1.5 sem | — |
| **M1** | Auth + Layout | 2 sem | `/login`, `/register`, `/forgot-password`, `/reset-password` |
| **M2** | Dashboard + Perfil | 1 sem | `/dashboard` |
| **M3** | Catálogo Admin | 2 sem | `/catalog/*` |
| **M4** | Menú POS + Modificadores | 2 sem | `/menu` |
| **M5** | Pedidos + Pagos | 2.5 sem | `/pos/*`, `/orders/*` |
| **M6** | Admin (sucursales, mesas) + Cutover | 1.5 sem | Todas las rutas restantes |
| | **Total** | **~12.5 semanas** | React eliminado |

Ver detalle completo en [PHASES.md](./PHASES.md).

---

## Decisiones de Arquitectura Angular

### State Management: Angular Signals + Services

NgRx es overkill para este tamaño. Se usarán **Angular Signals** (nativo desde Angular 17) en servicios singleton.

```typescript
// Equivalente al authStore de Zustand
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly currentUser = signal<User | null>(null);
  readonly isAuthenticated = computed(() => this.currentUser() !== null);
}
```

### HTTP Client: HttpClient + Interceptors

Axios + TanStack Query se reemplaza por:
- `HttpClient` de Angular para requests
- `HttpInterceptor` para JWT (equivalente a `axiosInstance.ts`)
- `@tanstack/angular-query-experimental` para server state con caching (Phase M3+)

### Componentes UI: Angular Material

MUI → Angular Material. La API es diferente pero los conceptos son idénticos (ambos implementan Material Design).

### Testing: Jest + Testing Library Angular

- Jasmine/Karma (default de Angular) → Jest (más consistente con el tooling actual)
- `@testing-library/angular` como wrapper (misma filosofía que `@testing-library/react`)
- MSW permanece compatible — los handlers existentes son reutilizables

### Build: Angular CLI (esbuild)

Angular 17+ usa esbuild internamente. La configuración de Vercel solo cambia:
- Build command: `ng build`
- Output directory: `dist/quickstack-frontend/browser`

---

## El Reto Central: Auth Compartida

El mayor desafío técnico del strangler fig es que **ambas apps deben reconocer la misma sesión de usuario**.

El flujo actual usa:
- `access_token` en memoria (Zustand)
- `refresh_token` en cookie `__Host-` HttpOnly

Esto simplifica la coexistencia: la cookie de refresh es reconocida por el browser en ambos deployments (mismo dominio), y el Angular puede hacer su propio silent refresh sin que el usuario tenga que loguearse de nuevo al cambiar de ruta.

Ver detalle en [AUTH_SHARING.md](./AUTH_SHARING.md).

---

## Archivos Relevantes del Proyecto React (a migrar)

| Archivo React | Equivalente Angular | Fase |
|--------------|--------------------|----|
| `src/stores/authStore.ts` | `AuthService` con Signals | M1 |
| `src/utils/axiosInstance.ts` | `AuthInterceptor` (HttpInterceptor) | M1 |
| `src/components/common/ProtectedRoute.tsx` | `AuthGuard` (CanActivateFn) | M1 |
| `src/features/auth/*.tsx` | `auth/` feature module | M1 |
| `src/components/layout/*.tsx` | `layout/` components | M1-M2 |
| `src/features/catalog/*.tsx` | `catalog/` feature module | M3-M4 |
| `src/features/pos/*.tsx` | `pos/` feature module | M5 |
