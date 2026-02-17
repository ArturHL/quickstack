# V3: Session Management

> **Capitulo:** V3
> **Requisitos L2:** 19
> **Cumplidos:** 1 (5%)
> **Ultima actualizacion:** 2026-02-16

---

## V3.1 Fundamental Session Management Security

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 3.1.1 | Verificar que la aplicacion nunca revele tokens de sesion en parametros de URL | L1 | ⏳ | **Pendiente Phase 0.3:** JWTs enviados solo en header `Authorization: Bearer`. Nunca en query strings. Refresh token en httpOnly cookie. |

---

## V3.2 Session Binding

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 3.2.1 | Verificar que la aplicacion genere un nuevo token de sesion en la autenticacion del usuario | L1 | ⏳ | **Pendiente Phase 0.3:** Nuevo access token y refresh token generados en cada login exitoso. Tokens anteriores no reutilizados. |
| 3.2.2 | Verificar que los tokens de sesion posean al menos 64 bits de entropia | L1 | ⏳ | **Pendiente Phase 0.3:** JWT `jti` claim usa UUID (128 bits). Refresh tokens de 32 bytes (256 bits). |
| 3.2.3 | Verificar que la aplicacion solo almacene tokens de sesion en el navegador usando metodos seguros como cookies debidamente aseguradas o almacenamiento de sesion HTML5 | L1 | ⏳ | **Pendiente Phase 0.3:** Access token en memoria JavaScript (no localStorage). Refresh token en httpOnly secure cookie con SameSite=Strict. |

---

## V3.3 Session Termination

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 3.3.1 | Verificar que el logout y la expiracion invaliden el token de sesion, de tal manera que el boton de atras o una parte dependiente aguas abajo no reanude una sesion autenticada, incluyendo a traves de partes dependientes | L1 | ⏳ | **Pendiente Phase 0.3:** Logout marca refresh token como `revoked_at` en BD. Access tokens expiran en 15 min. Backend valida refresh token contra BD antes de emitir nuevo access token. |
| 3.3.2 | Si los autenticadores permiten que los usuarios permanezcan logueados, verificar que la reautenticacion ocurra periodicamente tanto cuando se usa activamente como despues de un periodo de inactividad | L1 | ⏳ | **Pendiente Phase 0.3:** Access token expira en 15 min (requiere refresh). Refresh token expira en 7 dias. Sesion inactiva 30 min requiere re-login. |
| 3.3.3 | Verificar que la aplicacion ofrezca la opcion de terminar todas las otras sesiones activas despues de un cambio de contrasena exitoso (incluyendo cambio via reset/recuperacion de contrasena), y que esto sea efectivo en toda la aplicacion, login federado (si presente), y cualquier parte dependiente | L2 | ⏳ | **Pendiente Phase 0.3:** Password change revoca toda la familia de refresh tokens del usuario. Frontend limpia access token. Sesiones en otros dispositivos invalidadas. |
| 3.3.4 | Verificar que los usuarios puedan ver y (habiendo reingresado credenciales de login) cerrar sesion en cualquiera o todas las sesiones y dispositivos actualmente activos | L2 | ⏳ | **Pendiente Phase 0.3:** Endpoint `GET /api/v1/users/me/sessions` lista refresh tokens activos (user_agent, ip, created_at). `DELETE /api/v1/users/me/sessions/{id}` revoca sesion especifica. |

---

## V3.4 Cookie-based Session Management

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 3.4.1 | Verificar que los tokens de sesion basados en cookies tengan el atributo 'Secure' establecido | L1 | ⏳ | **Pendiente Phase 0.3:** Refresh token cookie con `Secure=true`. Solo enviada sobre HTTPS. |
| 3.4.2 | Verificar que los tokens de sesion basados en cookies tengan el atributo 'HttpOnly' establecido | L1 | ⏳ | **Pendiente Phase 0.3:** Refresh token cookie con `HttpOnly=true`. Inaccesible via JavaScript. |
| 3.4.3 | Verificar que los tokens de sesion basados en cookies utilicen el atributo 'SameSite' para limitar la exposicion a ataques de falsificacion de solicitud entre sitios | L1 | ⏳ | **Pendiente Phase 0.3:** Refresh token cookie con `SameSite=Strict`. Previene CSRF. |
| 3.4.4 | Verificar que los tokens de sesion basados en cookies usen el prefijo "__Host-" para que las cookies solo se envien al host que inicialmente establecio la cookie | L1 | ⏳ | **Pendiente Phase 0.3:** Cookie name: `__Host-refreshToken`. Requiere Secure, sin Domain, Path=/. |
| 3.4.5 | Verificar que si la aplicacion se publica bajo un nombre de dominio con otras aplicaciones que establecen o usan cookies de sesion que podrian divulgar los tokens de sesion, establezca el atributo de ruta en los tokens de sesion basados en cookies usando la ruta mas precisa posible | L1 | ⏳ | **Pendiente Phase 0.3:** Cookie path: `/api/v1/auth`. Solo enviada a endpoints de auth. |

---

## V3.5 Token-based Session Management

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 3.5.1 | Verificar que la aplicacion permita a los usuarios revocar tokens OAuth que forman relaciones de confianza con aplicaciones vinculadas | L2 | N/A | **No aplica:** No hay OAuth con aplicaciones de terceros. Autenticacion nativa. |
| 3.5.2 | Verificar que la aplicacion use tokens de sesion en lugar de secretos y claves API estaticas, excepto con implementaciones heredadas | L2 | ⏳ | **Pendiente Phase 0.3:** JWTs para autenticacion de usuarios. No API keys estaticas para usuarios. Service-to-service usa env vars. |
| 3.5.3 | Verificar que tokens de sesion sin estado usen firmas digitales, cifrado, y otras contramedidas para proteger contra ataques de manipulacion, envolvimiento, replay, cipher nulo, y sustitucion de clave | L2 | ✅ | JWT firmado con RS256 (RSA 2048-bit + SHA-256). Claims incluyen `iat`, `exp`, `jti` unico. JwtService rechaza algoritmos inseguros (HS256, none). Verificacion de firma en cada request via JwtAuthenticationFilter. |

---

## V3.6 Federated Re-authentication

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 3.6.1 | Verificar que las Partes Dependientes (RPs) especifiquen el tiempo maximo de autenticacion a los Proveedores de Servicio de Credenciales (CSPs) y que los CSPs reautentiquen al suscriptor si no han usado una sesion dentro de ese periodo | L2 | N/A | **No aplica:** Sin federacion. Autenticacion nativa unicamente. |
| 3.6.2 | Verificar que los Proveedores de Servicio de Credenciales (CSPs) informen a las Partes Dependientes (RPs) del ultimo evento de autenticacion, para permitir que las RPs determinen si necesitan reautenticar al usuario | L2 | N/A | **No aplica:** Sin federacion. Autenticacion nativa unicamente. |

---

## V3.7 Defenses Against Session Management Exploits

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 3.7.1 | Verificar que la aplicacion asegure una sesion de login completa y valida o requiera reautenticacion o verificacion secundaria antes de permitir cualquier transaccion sensible o modificacion de cuenta | L1 | ⏳ | **Pendiente Phase 0.3:** Cambio de password requiere password actual. Acciones criticas (delete account) requieren re-autenticacion. JWT valido requerido para todas las operaciones. |

---

## Resumen de Estado

| Seccion | Total Requisitos | Cumplidos | Pendientes | No Aplica |
|---------|------------------|-----------|------------|-----------|
| V3.1 Fundamental | 1 | 0 | 1 | 0 |
| V3.2 Session Binding | 3 | 0 | 3 | 0 |
| V3.3 Session Termination | 4 | 0 | 4 | 0 |
| V3.4 Cookie-based | 5 | 0 | 5 | 0 |
| V3.5 Token-based | 3 | 1 | 1 | 1 |
| V3.6 Federated | 2 | 0 | 0 | 2 |
| V3.7 Defenses | 1 | 0 | 1 | 0 |
| **TOTAL** | **19** | **1** | **15** | **3** |
