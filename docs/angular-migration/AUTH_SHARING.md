# Auth Compartida Durante la Migración

> Este documento explica el reto técnico central del Strangler Fig: cómo ambos frontends (React y Angular) reconocen la misma sesión sin que el usuario tenga que re-autenticarse al cambiar entre rutas migradas y no migradas.

---

## El Problema

Durante la migración, el usuario puede estar en `/dashboard` (React) y navegar a `/login` (Angular). Si la sesión no se comparte, el usuario tendría que volver a loguearse — inaceptable para el piloto.

---

## Por qué Funciona

El sistema de auth de QuickStack usa:

| Token | Dónde vive | Scope |
|-------|-----------|-------|
| `access_token` | Memoria (Zustand en React / Signal en Angular) | Solo el tab activo |
| `refresh_token` | Cookie `__Host-` HttpOnly | Todo el dominio (`app.quickstack.com`) |

La cookie `__Host-` es compartida automáticamente por el browser entre todos los requests al mismo dominio, independientemente de qué app sirvió la página.

**Resultado:** Cuando Angular necesita un `access_token` y no lo tiene en memoria (porque el usuario venía del React), simplemente llama al endpoint `/api/v1/auth/refresh`. La cookie de refresh está presente → el backend devuelve un nuevo access token → Angular lo guarda en su Signal.

El usuario no nota nada.

---

## Flujo Concreto

```
1. Usuario abre el POS → React está en /dashboard con access_token en memoria

2. Usuario navega a /login (rutas Angular activadas en M1)
   → El browser hace request a quickstack-angular.vercel.app/login
   → Angular no tiene access_token en su Signal (memoria nueva)
   → Angular llama automáticamente a POST /api/v1/auth/refresh
   → El browser envía la cookie __Host-refresh-token (mismo dominio)
   → Backend valida y devuelve nuevo access_token
   → Angular guarda en Signal → usuario autenticado
   → AuthGuard permite el acceso al dashboard Angular

3. Usuario navega a /pos (todavía en React)
   → El React tampoco tiene access_token (la memoria de Angular es separada)
   → El axiosInstance de React detecta el 401 en el primer request
   → Hace silent refresh → obtiene nuevo access_token → guarda en Zustand
   → El usuario sigue trabajando normalmente
```

---

## Condición Necesaria: Mismo Dominio

Este mecanismo **solo funciona si ambos deployments sirven desde el mismo dominio** (ej. `app.quickstack.com`).

Si Angular estuviera en un subdominio diferente (ej. `angular.quickstack.com`), la cookie `__Host-` no se compartiría y el usuario tendría que re-autenticarse en cada cambio de ruta.

Por eso el Edge Middleware es fundamental — actúa como proxy transparente, y el usuario siempre ve `app.quickstack.com`.

---

## Configuración de la Cookie

Verificar que el backend produzca la cookie con exactamente estos atributos (ya implementado en Phase 0.3):

```
Set-Cookie: __Host-refresh-token=<jwt>; Path=/; Secure; HttpOnly; SameSite=Strict
```

| Atributo | Por qué importa para la migración |
|----------|----------------------------------|
| `__Host-` prefix | Fuerza `Path=/` y `Secure` — accesible en todo el dominio |
| `Path=/` | Enviada en requests a cualquier ruta, no solo `/auth/*` |
| `HttpOnly` | JavaScript no puede leerla — solo el browser la envía automáticamente |
| `SameSite=Strict` | Solo se envía en navegación al mismo dominio — no hay riesgo CSRF en el proxy |

---

## Qué Pasa con la Cookie en el Edge Middleware

El Edge Middleware de Vercel hace `fetch` al deployment Angular/React como proxy. Las cookies del browser llegan al middleware, y el middleware las reenvía en el request proxeado.

No se necesita lógica especial — las cookies viajan transparentemente.

---

## Escenario de Error: Cookie Expirada

Si la cookie de refresh expiró (el usuario lleva demasiado tiempo sin usar el POS):

1. Angular intenta el silent refresh → backend devuelve 401
2. `AuthInterceptor` detecta el 401 en el refresh endpoint
3. Limpia el estado de auth (Signal `currentUser = null`)
4. Redirige a `/login`

Mismo comportamiento que el React actual. No hay regresión.
