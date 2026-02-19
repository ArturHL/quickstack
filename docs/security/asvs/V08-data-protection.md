# V8: Data Protection

> **Capitulo:** V8
> **Requisitos L2:** 14
> **Cumplidos:** 2 (14%)
> **Ultima actualizacion:** 2026-02-19

---

## V8.1 General Data Protection

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 8.1.1 | Verificar que la aplicacion proteja los datos sensibles de ser cacheados en componentes del servidor como balanceadores de carga y caches de aplicacion | L2 | ⏳ | **Pendiente Phase 0.3:** `Cache-Control: no-store, private` en respuestas con datos sensibles. No caching de JWTs o user data en CDN. |
| 8.1.2 | Verificar que todas las copias cacheadas o temporales de datos sensibles almacenadas en el servidor esten protegidas de acceso no autorizado o purgadas/invalidadas despues de que el usuario autorizado acceda a los datos sensibles | L2 | ⏳ | **Pendiente Phase 1:** Sin caching de datos sensibles en backend. Redis (futuro) con encryption at rest y TTL cortos. |
| 8.1.3 | Verificar que la aplicacion minimice el numero de parametros en una solicitud, como campos ocultos, variables Ajax, cookies y valores de header | L2 | ⏳ | **Pendiente Phase 1:** DTOs con solo campos necesarios. Sin hidden fields. JWT en header unico. Cookies solo para refresh token. |
| 8.1.4 | Verificar que la aplicacion pueda detectar y alertar sobre numeros anormales de solicitudes, como por IP, usuario, total por hora o dia, o lo que tenga sentido para la aplicacion | L2 | ⏳ | **Pendiente Phase 1:** Rate limiting con Bucket4j. Logging de requests por IP/user. **Futuro:** Alertas en Grafana/Datadog. |
| 8.1.5 | Verificar que se tomen respaldos regulares de datos importantes y que se realicen pruebas de restauracion de datos | L3 | N/A | **L3 - Fuera de alcance MVP.** Neon tiene snapshots automaticos. Restore manual documentado. |
| 8.1.6 | Verificar que los respaldos se almacenen de forma segura para prevenir que los datos sean robados o corrompidos | L3 | N/A | **L3 - Fuera de alcance MVP.** Neon backups cifrados y geo-redundantes. |

---

## V8.2 Client-side Data Protection

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 8.2.1 | Verificar que la aplicacion establezca suficientes headers anti-caching de modo que los datos sensibles no se cacheen en navegadores modernos | L1 | ⏳ | **Pendiente Phase 0.3:** Headers `Cache-Control: no-store`, `Pragma: no-cache` en respuestas de auth y user data. |
| 8.2.2 | Verificar que los datos almacenados en almacenamiento del navegador (como localStorage, sessionStorage, IndexedDB, o cookies) no contengan datos sensibles | L1 | ✅ | **Phase 0.4 Sprint 1:** Access token almacenado exclusivamente en memoria (Zustand store, no persiste). Refresh token en cookie `__Host-refreshToken` (HttpOnly, Secure, SameSite=Strict - no accesible via JavaScript). Sin PII en localStorage/sessionStorage. Auditoria verificada: `.env.local` en `.gitignore`, sin secrets hardcodeados. |
| 8.2.3 | Verificar que los datos autenticados se borren del almacenamiento del cliente, como el DOM del navegador, despues de que el cliente o sesion se termine | L1 | ✅ | **Phase 0.4 Sprints 2-3:** Logout (`useLogout` hook) limpia access token de memoria via `authStore.clearAuth()`. Cookie de refresh eliminada por backend (`Set-Cookie: Max-Age=0`). Zustand store reseteado a estado inicial. Navegacion a `/login` post-logout. Tests verifican limpieza completa. |

---

## V8.3 Sensitive Private Data

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 8.3.1 | Verificar que los datos sensibles se envien al servidor en el cuerpo o headers del mensaje HTTP, y que los parametros de cadena de consulta de cualquier verbo HTTP no contengan datos sensibles | L1 | ⏳ | **Pendiente Phase 0.3:** Passwords en body (POST). Tokens en headers. Sin credenciales en query strings. IDs en path (no sensibles, son UUIDs). |
| 8.3.2 | Verificar que los usuarios tengan un metodo para eliminar o exportar sus datos bajo demanda | L1 | ⏳ | **Pendiente Phase 6:** `GET /api/v1/users/me/data` para export (JSON). `DELETE /api/v1/users/me` para soft delete. Compliance LFPDPPP ARCO rights. |
| 8.3.3 | Verificar que los usuarios reciban lenguaje claro sobre la recopilacion y uso de la informacion personal suministrada y que los usuarios hayan proporcionado consentimiento opt-in para el uso de esos datos antes de que sean usados de alguna manera | L1 | ⏳ | **Pendiente Phase 0.4:** Aviso de privacidad en registro. Checkbox de consentimiento obligatorio. Terminos y condiciones. |
| 8.3.4 | Verificar que todos los datos sensibles creados y procesados por la aplicacion hayan sido identificados, y asegurar que exista una politica sobre como manejar datos sensibles | L1 | ⏳ | **Pendiente Phase 1:** Clasificacion documentada en SECURITY.md. PII identificado: email, phone, address. Financieros: orders, payments. |
| 8.3.5 | Verificar que el acceso a datos sensibles este auditado (sin registrar los datos sensibles en si mismos), si los datos se recopilan bajo directivas de proteccion de datos relevantes o donde se requiera registro de acceso | L2 | ⏳ | **Pendiente Phase 1:** Audit log de acceso a datos de clientes. Log de quien accedio, cuando, que operacion. Sin loguear el dato en si. |
| 8.3.6 | Verificar que la informacion sensible contenida en la memoria se sobrescriba tan pronto como ya no sea requerida para mitigar ataques de volcado de memoria, usando ceros o datos aleatorios | L2 | ⏳ | **Pendiente Phase 0.3:** Passwords en `char[]` (no String) donde sea posible. Spring Security maneja limpieza de credenciales. JVM garbage collection para otros casos. |
| 8.3.7 | Verificar que la informacion sensible o privada que se requiere cifrar, se cifre usando algoritmos aprobados que proporcionen tanto confidencialidad como integridad | L2 | ⏳ | **Pendiente Phase 1:** Neon cifra at-rest (AES-256-GCM). TLS 1.3 en transito. JWTs firmados (integridad). Passwords hasheados (Argon2id). |
| 8.3.8 | Verificar que la informacion personal sensible este sujeta a clasificacion de retencion de datos, de modo que los datos viejos u obsoletos se eliminen automaticamente, por un cronograma, o segun la situacion requiera | L2 | ⏳ | **Pendiente Phase 6:** Politica de retencion documentada. Login attempts: 90 dias. Refresh tokens: 30 dias despues de expiracion. Datos de clientes: segun LFPDPPP. |

---

## Resumen de Estado

| Seccion | Total Requisitos | Cumplidos | Pendientes | No Aplica |
|---------|------------------|-----------|------------|-----------|
| V8.1 General Protection | 4 | 0 | 4 | 0 |
| V8.2 Client-side | 3 | 2 | 1 | 0 |
| V8.3 Sensitive Data | 8 | 0 | 8 | 0 |
| **TOTAL** | **15** | **2** | **13** | **0** |
