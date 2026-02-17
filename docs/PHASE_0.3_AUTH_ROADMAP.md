# Phase 0.3: Authentication Module Roadmap

> **Version:** 1.0.0
> **Fecha:** 2026-02-11
> **Standard:** OWASP ASVS L2
> **Status:** En progreso - Sprint 2/6 completado

---

## Resumen Ejecutivo

Este documento define el plan de implementacion del modulo de autenticacion para QuickStack POS, siguiendo OWASP ASVS Level 2.

| Aspecto | Detalle |
|---------|---------|
| **Timeline** | 6 sprints (~3 semanas) |
| **Tareas Backend** | 22 tareas (~59 horas) |
| **Tareas QA** | 11 tareas (~33 horas) |
| **Checkpoints de Seguridad** | 3 (Post-Sprint 2, 4, 6) |

---

## Decisiones de Diseno Confirmadas

### Seguridad

| Decision | Valor | Justificacion |
|----------|-------|---------------|
| Password hashing | Argon2id + pepper | Recomendacion OWASP 2024 |
| JWT signing | RS256 (2048 bits) | Asimetrico, permite validacion sin secreto |
| Access token expiry | 15 minutos | Balance seguridad/UX |
| Refresh token expiry | 7 dias | Con rotation en cada uso |
| Rate limit por IP | 10 req/min | Mitiga brute force |
| Rate limit por email | 5 req/min | Mitiga credential stuffing |
| Account lockout | 5 intentos = 15 min | Con auto-unlock |
| HIBP falla | **Bloquear registro** | Seguridad sobre UX |

### Cookies

```
Name: __Host-refreshToken
HttpOnly: true
Secure: true
SameSite: Strict
Path: /api/v1/auth
Max-Age: 604800 (7 dias)
```

---

## Procedimiento de Rotacion de JWT Keys

### Generacion de Claves

```bash
# Generar par de claves RSA 2048-bit
openssl genrsa -out jwt-private.pem 2048
openssl rsa -in jwt-private.pem -pubout -out jwt-public.pem

# Codificar en base64 para env vars (opcional)
base64 -w 0 jwt-private.pem > jwt-private.b64
base64 -w 0 jwt-public.pem > jwt-public.b64
```

### Configuracion

```yaml
# application.yml
quickstack:
  jwt:
    # Clave activa para firmar nuevos tokens
    private-key: ${JWT_PRIVATE_KEY}
    public-key: ${JWT_PUBLIC_KEY}

    # Claves anteriores para validar tokens existentes (rotacion)
    previous-public-keys: ${JWT_PREVIOUS_PUBLIC_KEYS:}

    access-token-expiry: 15m
    refresh-token-expiry: 7d
    issuer: quickstack-pos
```

### Proceso de Rotacion (Cada 12 meses o si hay compromiso)

| Paso | Accion | Responsable |
|------|--------|-------------|
| 1 | Generar nuevo par de claves | DevOps |
| 2 | Agregar public key actual a `previous-public-keys` | DevOps |
| 3 | Actualizar `private-key` y `public-key` con nuevas claves | DevOps |
| 4 | Deploy con nuevas variables | DevOps |
| 5 | Monitorear errores de validacion | QA |
| 6 | Despues de 7 dias, remover clave anterior de `previous-public-keys` | DevOps |

### Rotacion de Emergencia (Compromiso de Clave)

1. Generar nuevas claves inmediatamente
2. **NO** agregar clave comprometida a `previous-public-keys`
3. Deploy inmediato
4. Todos los usuarios tendran que re-autenticarse (refresh tokens invalidos)
5. Notificar a usuarios afectados
6. Documentar incidente

---

## Arquitectura de Componentes

```
quickstack-common/
├── config/properties/
│   ├── JwtProperties.java
│   ├── PasswordProperties.java
│   ├── RateLimitProperties.java
│   └── CookieProperties.java
├── exception/
│   ├── AuthenticationException.java
│   ├── RateLimitExceededException.java
│   ├── AccountLockedException.java
│   ├── InvalidTokenException.java
│   └── PasswordCompromisedException.java
└── security/
    ├── SecureTokenGenerator.java
    └── IpAddressExtractor.java

quickstack-user/
├── entity/
│   ├── User.java (existente)
│   ├── RefreshToken.java
│   ├── PasswordResetToken.java
│   └── LoginAttempt.java
├── repository/
│   ├── UserRepository.java
│   ├── RefreshTokenRepository.java
│   ├── PasswordResetTokenRepository.java
│   └── LoginAttemptRepository.java
└── service/
    ├── UserService.java
    ├── PasswordService.java
    ├── RefreshTokenService.java
    ├── PasswordResetService.java
    └── SessionService.java

quickstack-app/
├── config/
│   ├── SecurityConfig.java (modificar)
│   ├── JwtConfig.java
│   └── RateLimitConfig.java
├── security/
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   ├── RateLimitFilter.java
│   ├── LoginAttemptService.java
│   └── HibpClient.java
├── controller/
│   ├── AuthController.java
│   └── UserController.java
└── dto/
    ├── request/
    │   ├── RegisterRequest.java
    │   ├── LoginRequest.java
    │   ├── ForgotPasswordRequest.java
    │   └── ResetPasswordRequest.java
    └── response/
        ├── AuthResponse.java
        ├── UserResponse.java
        └── SessionResponse.java
```

---

## Sprint 1: Foundation & Core Infrastructure ✅

**Duracion:** 2 dias | **Status:** COMPLETADO (61 tests)

### [BACKEND] Tarea 1.1: Properties Classes ✅
**Prioridad:** Alta | **Dependencias:** Ninguna

Crear clases de configuracion en `quickstack-common`.

**Criterios de Aceptacion:**
- [x] `JwtProperties` con: issuer, accessTokenExpiration, refreshTokenExpiration
- [x] `PasswordProperties` con: argon2 params, pepper reference
- [x] `RateLimitProperties` con: ipLimit, emailLimit, windowMinutes
- [x] `CookieProperties` con: secure, httpOnly, sameSite, maxAge
- [x] Validaciones con `@Min`, `@NotBlank`
- [x] Tests unitarios (16 tests)

**Archivos:**
- `quickstack-common/src/main/java/com/quickstack/common/config/properties/*.java`

---

### [BACKEND] Tarea 1.2: Excepciones Custom ✅
**Prioridad:** Alta | **Dependencias:** Ninguna

**Criterios de Aceptacion:**
- [x] `AuthenticationException` base
- [x] `RateLimitExceededException` con `retryAfterSeconds`
- [x] `AccountLockedException` con `unlockAtTimestamp`
- [x] `InvalidTokenException` con tipo de token
- [x] `PasswordCompromisedException` para HIBP
- [x] `PasswordValidationException` para validacion de password
- [x] Tests unitarios (15 tests)

**Archivos:**
- `quickstack-common/src/main/java/com/quickstack/common/exception/*.java`

---

### [BACKEND] Tarea 1.3: Utilidades de Seguridad ✅
**Prioridad:** Alta | **Dependencias:** Ninguna

**Criterios de Aceptacion:**
- [x] `SecureTokenGenerator.generate()` retorna 32 bytes URL-safe
- [x] Usa `SecureRandom` thread-safe
- [x] `IpAddressExtractor` maneja X-Forwarded-For, X-Real-IP
- [x] Sanitiza IPs contra header injection
- [x] Tests unitarios con mocks (30 tests)

**Archivos:**
- `quickstack-common/src/main/java/com/quickstack/common/security/*.java`

---

### [BACKEND] Tarea 1.4: Actualizar GlobalExceptionHandler ✅
**Prioridad:** Media | **Dependencias:** 1.2

**Criterios de Aceptacion:**
- [x] Handler para cada nueva excepcion
- [x] `RateLimitExceededException` -> 429 + header `Retry-After`
- [x] `AccountLockedException` -> 423 Locked + `X-Locked-Until`
- [x] `InvalidTokenException` -> 401 + `WWW-Authenticate`
- [x] `PasswordCompromisedException` -> 400
- [x] `PasswordValidationException` -> 400
- [x] No exponer info sensible
- [x] Handlers existentes preservados

**Archivos:**
- `quickstack-common/src/main/java/com/quickstack/common/exception/GlobalExceptionHandler.java`

---

### [QA] Tarea 1.5: Setup Testing Infrastructure
**Prioridad:** Alta | **Dependencias:** Ninguna

**Criterios de Aceptacion:**
- [ ] `BaseIntegrationTest` con Testcontainers PostgreSQL 16
- [ ] Perfil `test` en `application-test.yml`
- [ ] `SecurityTestUtils` para timing attacks
- [ ] Mock de `HibpClient`
- [ ] RestAssured configurado

**Archivos:**
- `quickstack-app/src/test/java/com/quickstack/app/BaseIntegrationTest.java`
- `quickstack-app/src/test/resources/application-test.yml`

---

## Sprint 2: Password Hashing & User Management ✅

**Duracion:** 2 dias | **Status:** COMPLETADO (61 tests)

### [BACKEND] Tarea 2.1: PasswordService con Argon2id ✅
**Prioridad:** Alta | **Dependencias:** 1.1

**Criterios de Aceptacion:**
- [x] Usa `Argon2PasswordEncoder` de Spring Security 6
- [x] Params: iterations=3, memory=65536 KB, parallelism=4
- [x] `hashPassword()` aplica pepper antes de hash
- [x] `verifyPassword()` con timing-safe comparison (via Spring Security)
- [x] Pepper versionado (soporte para rotacion)
- [x] Tests: hash unico por salt, verificacion, timing safety (29 tests)

**Archivos:**
- `quickstack-user/src/main/java/com/quickstack/user/service/PasswordService.java`
- `quickstack-user/src/test/java/.../PasswordServiceTest.java`

---

### [BACKEND] Tarea 2.2: HibpClient ✅
**Prioridad:** Alta | **Dependencias:** Ninguna

**Criterios de Aceptacion:**
- [x] k-Anonymity: envia solo primeros 5 chars del SHA-1
- [x] Timeout 3 segundos
- [x] Retry con backoff (max 2 reintentos)
- [x] **Si falla: lanza excepcion (bloquea registro)**
- [x] Tests con WireMock (16 tests)

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/security/HibpClient.java`
- `quickstack-app/src/test/java/.../HibpClientTest.java`

---

### [BACKEND] Tarea 2.3: UserService con Registro ✅
**Prioridad:** Alta | **Dependencias:** 2.1, 2.2

**Criterios de Aceptacion:**
- [x] `registerUser()` valida: email unico por tenant, password policy, HIBP
- [x] Password: min 12 chars, max 128, sin reglas de composicion
- [x] Hash con `PasswordService`
- [x] Transaccional con rollback
- [x] Tests: registro exitoso, email duplicado, HIBP breach (16 tests)

**Archivos:**
- `quickstack-user/src/main/java/com/quickstack/user/service/UserService.java`
- `quickstack-user/src/main/java/com/quickstack/user/entity/User.java`
- `quickstack-user/src/main/java/com/quickstack/user/repository/UserRepository.java`
- `quickstack-user/src/test/java/.../UserServiceTest.java`

---

### [QA] Tarea 2.4: Tests de Seguridad - Password Hashing ✅
**Prioridad:** Alta | **Dependencias:** 2.1

**Criterios de Aceptacion:**
- [x] Verificar Argon2id con params minimos
- [x] Test: mismo password genera hashes diferentes
- [x] Test: timing attack mitigation
- [x] Test: pepper aplicado correctamente
- [x] Performance: hash < 200ms

**Archivos:**
- `quickstack-user/src/test/java/.../PasswordServiceTest.java` (29 tests incluidos en 2.1)

---

### [QA] Tarea 2.5: Tests de Seguridad - HIBP ✅
**Prioridad:** Media | **Dependencias:** 2.2

**Criterios de Aceptacion:**
- [x] Test: password conocido es rechazado
- [x] Test: k-Anonymity correcto (solo 5 chars enviados)
- [x] Test: fallo de HIBP bloquea registro
- [x] Test: no loguear passwords

**Archivos:**
- `quickstack-app/src/test/java/.../HibpClientTest.java` (16 tests incluidos en 2.2)

---

### CHECKPOINT DE SEGURIDAD #1 ✅

**Validaciones:**
- [x] Argon2id configurado correctamente
- [x] Timing-safe password comparison
- [x] HIBP con k-Anonymity
- [x] Passwords nunca en logs
- [x] Code review de PasswordService y HibpClient

---

## Sprint 3: JWT Generation & Validation

**Duracion:** 2 dias

### [BACKEND] Tarea 3.1: JwtConfig y KeyPair
**Prioridad:** Alta | **Dependencias:** 1.1

**Criterios de Aceptacion:**
- [ ] Carga claves RSA desde env vars (PEM o base64)
- [ ] Soporte para claves anteriores (rotacion)
- [ ] Falla si claves ausentes en produccion
- [ ] Tests: carga correcta, formato PEM, 2048 bits

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/config/JwtConfig.java`
- `quickstack-app/src/main/java/com/quickstack/app/config/RsaKeyProperties.java`

---

### [BACKEND] Tarea 3.2: JwtService
**Prioridad:** Alta | **Dependencias:** 3.1

**Criterios de Aceptacion:**
- [ ] `generateAccessToken()` con claims: sub, email, tenantId, role, branchId, jti
- [ ] RS256 con rechazo explicito de otros algoritmos
- [ ] Expiracion configurable (default 15 min)
- [ ] `validateToken()` verifica firma, expiracion, issuer
- [ ] Tests: generacion, validacion, expirado, firma invalida
- [ ] **Test: algorithm confusion attack rechazado**

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/security/JwtService.java`

---

### [BACKEND] Tarea 3.3: JwtAuthenticationFilter
**Prioridad:** Alta | **Dependencias:** 3.2

**Criterios de Aceptacion:**
- [ ] Extrae JWT de `Authorization: Bearer`
- [ ] Valida con `JwtService`
- [ ] Verifica tenant_id match
- [ ] Permite endpoints publicos sin token
- [ ] Tests: token valido, invalido, ausente

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/security/JwtAuthenticationFilter.java`

---

### [QA] Tarea 3.4: Tests de Seguridad - JWT
**Prioridad:** Alta | **Dependencias:** 3.2, 3.3

**Criterios de Aceptacion:**
- [ ] Test: RS256 requerido, HS256 rechazado
- [ ] Test: "none" algorithm rechazado
- [ ] Test: manipulacion de claims detectada
- [ ] Test: token firmado con otra clave rechazado
- [ ] Performance: validacion < 10ms

**Archivos:**
- `quickstack-app/src/test/java/.../JwtServiceSecurityTest.java`

---

## Sprint 4: Login, Refresh & Session Management

**Duracion:** 2 dias

### [BACKEND] Tarea 4.1: Entidades de Sesion
**Prioridad:** Alta | **Dependencias:** Ninguna

**Criterios de Aceptacion:**
- [ ] `RefreshToken`: id, tokenHash, userId, tenantId, familyId, expiresAt, revokedAt, ipAddress, userAgent
- [ ] `LoginAttempt`: id, email, tenantId, ipAddress, success, attemptedAt, failureReason
- [ ] Indices optimizados
- [ ] Repositorios con queries custom
- [ ] Tests con Testcontainers

**Archivos:**
- `quickstack-user/src/main/java/com/quickstack/user/entity/RefreshToken.java`
- `quickstack-user/src/main/java/com/quickstack/user/entity/LoginAttempt.java`
- `quickstack-user/src/main/java/com/quickstack/user/repository/*.java`

---

### [BACKEND] Tarea 4.2: LoginAttemptService
**Prioridad:** Alta | **Dependencias:** 4.1

**Criterios de Aceptacion:**
- [ ] `recordLoginAttempt()` persiste intento
- [ ] `checkAccountLock()` verifica >= 5 intentos = 15 min lock
- [ ] `resetFailedAttempts()` al login exitoso
- [ ] Auto-unlock despues de tiempo
- [ ] Tests: lockout, auto-unlock, reset

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/security/LoginAttemptService.java`

---

### [BACKEND] Tarea 4.3: RefreshTokenService con Rotation
**Prioridad:** Alta | **Dependencias:** 4.1, 3.2

**Criterios de Aceptacion:**
- [ ] `createRefreshToken()` genera token, almacena hash SHA-256, asigna familyId
- [ ] `rotateRefreshToken()` valida, genera nuevo con mismo familyId, invalida anterior
- [ ] **Detecta reuso: invalida toda la familia**
- [ ] `revokeAllTokens()` para logout global
- [ ] Tests: creacion, rotacion, deteccion de reuso
- [ ] **Test: race condition con requests concurrentes**

**Archivos:**
- `quickstack-user/src/main/java/com/quickstack/user/service/RefreshTokenService.java`

---

### [BACKEND] Tarea 4.4: AuthController - Login y Refresh
**Prioridad:** Alta | **Dependencias:** 4.2, 4.3

**Criterios de Aceptacion:**
- [ ] `POST /auth/login`: valida lockout, verifica password, genera tokens
- [ ] `POST /auth/refresh`: lee cookie, rota token, retorna nuevo access token
- [ ] Refresh token en cookie HttpOnly
- [ ] Validacion con Bean Validation
- [ ] Tests: success, wrong password, lockout, token reuse

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/controller/AuthController.java`
- `quickstack-app/src/main/java/com/quickstack/app/dto/request/LoginRequest.java`
- `quickstack-app/src/main/java/com/quickstack/app/dto/response/AuthResponse.java`

---

### [QA] Tarea 4.5: Tests de Integracion - Login Flow
**Prioridad:** Alta | **Dependencias:** 4.4

**Criterios de Aceptacion:**
- [ ] Test E2E: registro -> login -> acceso con JWT
- [ ] Test: 5 intentos fallidos -> lockout 15 min
- [ ] Test: refresh token rotation funciona
- [ ] Test: reuso de refresh token invalida familia
- [ ] Test: multi-tenant isolation
- [ ] Performance: login < 500ms (p95)

**Archivos:**
- `quickstack-app/src/test/java/.../AuthenticationFlowIntegrationTest.java`

---

### CHECKPOINT DE SEGURIDAD #2

**Validaciones:**
- [ ] Account lockout funciona correctamente
- [ ] Refresh token rotation con family tracking
- [ ] Deteccion de reuso de tokens
- [ ] Cookies con flags correctos
- [ ] Code review de LoginAttemptService, RefreshTokenService, AuthController

---

## Sprint 5: Rate Limiting & Password Reset

**Duracion:** 2 dias

### [BACKEND] Tarea 5.1: RateLimitConfig con Bucket4j
**Prioridad:** Alta | **Dependencias:** 1.1

**Criterios de Aceptacion:**
- [ ] Bucket4j con Caffeine cache (in-memory para MVP)
- [ ] Buckets: IP (10/min), Email (5/min)
- [ ] TTL de 1 hora en cache entries
- [ ] Max 10,000 buckets (evict LRU)
- [ ] Tests de configuracion

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/config/RateLimitConfig.java`

---

### [BACKEND] Tarea 5.2: RateLimitFilter
**Prioridad:** Alta | **Dependencias:** 5.1, 1.3

**Criterios de Aceptacion:**
- [ ] Aplica a: /login, /register, /forgot-password, /reset-password
- [ ] Extrae IP con `IpAddressExtractor`
- [ ] Retorna 429 con header `Retry-After`
- [ ] Tests: limite alcanzado, refill, multiples IPs

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/security/RateLimitFilter.java`

---

### [BACKEND] Tarea 5.3: PasswordResetService
**Prioridad:** Alta | **Dependencias:** 1.3, 2.1

**Criterios de Aceptacion:**
- [ ] `initiateReset()` genera token 32 bytes, almacena hash SHA-256, expira 1 hora
- [ ] **Timing-safe: mismo tiempo si email no existe**
- [ ] `validateResetToken()` con constant-time comparison
- [ ] `resetPassword()` valida token, HIBP check, hashea, invalida refresh tokens
- [ ] Invalida tokens previos del usuario al iniciar reset
- [ ] Tests: flujo completo, expirado, reuso, timing

**Archivos:**
- `quickstack-user/src/main/java/com/quickstack/user/service/PasswordResetService.java`

---

### [BACKEND] Tarea 5.4: AuthController - Password Reset
**Prioridad:** Alta | **Dependencias:** 5.3

**Criterios de Aceptacion:**
- [ ] `POST /forgot-password`: inicia reset, **siempre retorna 200**
- [ ] `POST /reset-password`: valida token, cambia password
- [ ] Validacion de input
- [ ] Rate limiting aplicado (Tarea 5.2)
- [ ] Tests de endpoints

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/dto/request/ForgotPasswordRequest.java`
- `quickstack-app/src/main/java/com/quickstack/app/dto/request/ResetPasswordRequest.java`

---

### [QA] Tarea 5.5: Tests de Seguridad - Rate Limiting
**Prioridad:** Alta | **Dependencias:** 5.2

**Criterios de Aceptacion:**
- [ ] Test: 11° request desde misma IP -> 429
- [ ] Test: rate limit se resetea despues de 1 minuto
- [ ] Test: IPs diferentes no comparten limite
- [ ] Test: X-Forwarded-For spoofing rechazado
- [ ] Performance: overhead < 5ms

**Archivos:**
- `quickstack-app/src/test/java/.../RateLimitFilterSecurityTest.java`

---

### [QA] Tarea 5.6: Tests de Seguridad - Password Reset
**Prioridad:** Alta | **Dependencias:** 5.4

**Criterios de Aceptacion:**
- [ ] Test: token single-use (segundo uso rechazado)
- [ ] Test: token expirado (>1 hora) rechazado
- [ ] Test: timing attack mitigation (email inexistente mismo tiempo)
- [ ] Test: reset invalida refresh tokens
- [ ] Test: nueva password pasa HIBP check

**Archivos:**
- `quickstack-app/src/test/java/.../PasswordResetIntegrationTest.java`

---

## Sprint 6: Final Endpoints & Integration

**Duracion:** 2 dias

### [BACKEND] Tarea 6.1: SessionService
**Prioridad:** Media | **Dependencias:** 4.1

**Criterios de Aceptacion:**
- [ ] `getActiveSessions()` lista refresh tokens (metadata, no token real)
- [ ] `revokeSession()` invalida refresh token especifico
- [ ] `revokeAllSessions()` excepto sesion actual
- [ ] Tests unitarios

**Archivos:**
- `quickstack-user/src/main/java/com/quickstack/user/service/SessionService.java`

---

### [BACKEND] Tarea 6.2: UserController - Session Endpoints
**Prioridad:** Media | **Dependencias:** 6.1

**Criterios de Aceptacion:**
- [ ] `GET /users/me/sessions`: lista sesiones activas
- [ ] `DELETE /users/me/sessions/{id}`: revoca sesion
- [ ] Verifica que sesion pertenece al usuario
- [ ] Tests de autorizacion

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/controller/UserController.java`

---

### [BACKEND] Tarea 6.3: AuthController - Register y Logout
**Prioridad:** Alta | **Dependencias:** 2.3

**Criterios de Aceptacion:**
- [ ] `POST /register`: valida, registra usuario, retorna 201
- [ ] `POST /logout`: invalida refresh token, borra cookie, retorna 204
- [ ] Validacion con Bean Validation
- [ ] Tests: registro exitoso, email duplicado, logout

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/dto/request/RegisterRequest.java`
- `quickstack-app/src/main/java/com/quickstack/app/dto/response/UserResponse.java`

---

### [BACKEND] Tarea 6.4: SecurityConfig Final
**Prioridad:** Alta | **Dependencias:** Todas las anteriores

**Criterios de Aceptacion:**
- [ ] Orden de filtros: RateLimitFilter -> JwtAuthenticationFilter
- [ ] Endpoints publicos: /auth/login, /register, /forgot-password, /reset-password
- [ ] Endpoints protegidos: /users/**, /auth/logout, /auth/refresh
- [ ] CORS con origen Vercel
- [ ] Session: STATELESS
- [ ] Tests de integracion completos

**Archivos:**
- `quickstack-app/src/main/java/com/quickstack/app/config/SecurityConfig.java`

---

### [QA] Tarea 6.5: Tests de Regresion Multi-Tenant
**Prioridad:** Alta | **Dependencias:** 6.4

**Criterios de Aceptacion:**
- [ ] Test: usuario tenant A no login con credenciales tenant B
- [ ] Test: JWT tenant A rechazado para recursos tenant B
- [ ] Test: refresh token aislado por tenant
- [ ] Test: password reset aislado por tenant
- [ ] Test: rate limiting por email separado entre tenants

**Archivos:**
- `quickstack-app/src/test/java/.../MultiTenantSecurityIntegrationTest.java`

---

### [QA] Tarea 6.6: Tests de Penetracion Basicos
**Prioridad:** Media | **Dependencias:** 6.4

**Criterios de Aceptacion:**
- [ ] SQL Injection en email, password -> rechazado
- [ ] JWT tampering -> rechazado
- [ ] IDOR: acceso a sesiones de otro usuario -> 403
- [ ] Timing attacks -> mitigados
- [ ] Mass assignment (modificar role, tenantId) -> ignorado

**Archivos:**
- `quickstack-app/src/test/java/.../PenetrationTest.java`

---

### [QA] Tarea 6.7: Actualizar Documentacion ASVS
**Prioridad:** Media | **Dependencias:** Todas las QA

**Criterios de Aceptacion:**
- [ ] Marcar requisitos cumplidos en `docs/security/asvs/`
- [ ] Actualizar tabla de progreso en `CLAUDE.md`
- [ ] Crear `docs/security/ASVS_COMPLIANCE_REPORT.md`
- [ ] Target: >=90% de V2, >=70% de V3

**Archivos:**
- `docs/security/asvs/V02-authentication.md`
- `docs/security/asvs/V03-session-management.md`
- `docs/security/ASVS_COMPLIANCE_REPORT.md`

---

### CHECKPOINT DE SEGURIDAD #3 (FINAL)

**Validaciones:**
- [ ] Todos los endpoints implementados y funcionando
- [ ] Configuracion final de Spring Security correcta
- [ ] Tests de penetracion pasados
- [ ] Aislamiento multi-tenant completo
- [ ] Cumplimiento ASVS >= 80% en requisitos aplicables
- [ ] Code review completo del modulo
- [ ] **Aprobacion para merge a main**

---

## Protocolo de Seguridad Pre-Implementacion

### Checklist por Sprint

```markdown
## Security Checklist - Sprint [N]

### Preparacion
- [ ] Revisar tareas contra requisitos ASVS
- [ ] Identificar endpoints con datos sensibles
- [ ] Tests de seguridad en Definition of Done

### Durante Desarrollo
- [ ] No hardcodear secretos
- [ ] Logging sin PII ni tokens
- [ ] Parametrizar todas las queries
- [ ] Constant-time comparison para secrets

### Pre-Merge
- [ ] Semgrep sin warnings de seguridad
- [ ] OWASP Dependency-Check sin CVE criticos
- [ ] Code review enfocado en:
  - Autorizacion correcta
  - Input validado
  - Datos sensibles protegidos
  - Tenant isolation respetado

### Post-Merge
- [ ] Tests de seguridad pasan en CI
- [ ] Documentar deuda de seguridad si existe
```

---

## Criterios de Exito

| Criterio | Meta |
|----------|------|
| Funcionalidad | 8 endpoints funcionando |
| Seguridad | >= 80% ASVS V2 cumplidos |
| Testing | >= 85% code coverage en auth module |
| Performance | Login p95 < 200ms |
| Multi-tenant | Aislamiento completo validado |
| Documentacion | ASVS compliance report completado |

---

## Endpoints Finales

| Metodo | Endpoint | Descripcion | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/auth/register` | Registrar usuario | No |
| POST | `/api/v1/auth/login` | Login, obtener tokens | No |
| POST | `/api/v1/auth/refresh` | Renovar access token | Cookie |
| POST | `/api/v1/auth/logout` | Invalidar sesion | JWT |
| POST | `/api/v1/auth/forgot-password` | Solicitar reset | No |
| POST | `/api/v1/auth/reset-password` | Ejecutar reset | No |
| GET | `/api/v1/users/me/sessions` | Listar sesiones | JWT |
| DELETE | `/api/v1/users/me/sessions/{id}` | Revocar sesion | JWT |

---

## Deuda Tecnica Aceptada

| Deuda | Riesgo | Plan de Remediacion |
|-------|--------|---------------------|
| Sin MFA en MVP | Medio-Alto | TOTP para OWNER en Phase 1 |
| JWT keys en env vars | Medio | Migrar a AWS KMS post-piloto |
| Rate limiting in-memory | Bajo | Redis si multiples instancias |
| Sin vault de secretos | Medio | Evaluar Vault post-piloto |

---

## Referencias

- [OWASP ASVS 4.0.3](https://github.com/OWASP/ASVS)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [HaveIBeenPwned API](https://haveibeenpwned.com/API/v3)
- [Bucket4j Documentation](https://bucket4j.com/)

---

*Documento generado: 2026-02-11*
*Proximo revision: Post-Sprint 6*
