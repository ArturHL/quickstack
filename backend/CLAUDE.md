# QuickStack Backend

Java 17 + Spring Boot 3.5 | Multi-module Maven

## Módulos

| Módulo | Propósito |
|--------|-----------|
| quickstack-common | Config properties, exceptions, basic security |
| quickstack-auth | JWT, session management, auth flows |
| quickstack-user | User CRUD, identity |
| quickstack-product | Catálogo de productos |
| quickstack-pos | Punto de venta, órdenes |
| quickstack-app | Main app entry point, migrations |

## Convenciones

- **Package by feature**: controller/service/repository por módulo
- **DTOs**: `dto/request/` y `dto/response/`
- **Tests**: `*Test.java` (unit), `*RepositoryTest.java` (slice), `*E2ETest.java` (full system)
- **Exceptions**: Custom en `common/exception/`, handler en GlobalExceptionHandler

## Auth Actual (Phase 0.3 - COMPLETADO)

| Aspecto | Implementación |
|---------|----------------|
| Password | Argon2id + pepper versionado (en Common) |
| JWT | RS256 (2048 bits), 15min access / 7d refresh (en Auth) |
| Rate limit | Bucket4j: 10 req/min IP, 5 req/min email (en Auth) |
| Lockout | 5 intentos fallidos = 15 min lock (en Auth) |
| Cookies | HttpOnly, Secure, SameSite=Strict (en Auth) |
| Password Reset | Token 32 bytes, 1 hora expiry, HIBP check (en Auth) |
| Session Management | Refresh token rotation + family tracking (en Auth) |
| Register | POST /api/v1/auth/register (en Auth) |
| Sessions API | GET/DELETE /api/v1/users/me/sessions (en Auth) |

## Archivos Clave

```
quickstack-common/src/main/java/.../common/
├── config/properties/   # JwtProperties, PasswordProperties, RateLimitProperties
├── exception/           # AuthenticationException, InvalidTokenException, etc.
└── security/            # PasswordService, PasswordBreachChecker, JwtAuthenticationPrincipal

quickstack-auth/src/main/java/.../auth/
├── controller/          # AuthController, UserSessionController
├── service/             # RefreshTokenService, LoginAttemptService, PasswordResetService, SessionService
├── security/            # JwtService, JwtAuthenticationFilter, RateLimitFilter, HibpClient
├── entity/              # RefreshToken, LoginAttempt, PasswordResetToken
└── repository/          # RefreshTokenRepository, LoginAttemptRepository, PasswordResetTokenRepository

quickstack-user/src/main/java/.../user/
├── service/             # UserService
├── entity/              # User
└── repository/          # UserRepository

quickstack-product/src/main/java/.../product/
├── controller/          # CategoryController
├── security/            # CatalogPermissionEvaluator
├── service/             # CategoryService
├── entity/              # Category
├── repository/          # CategoryRepository
└── dto/                 # CategoryCreateRequest, CategoryUpdateRequest, CategoryResponse, CategorySummaryResponse

quickstack-app/src/main/resources/
├── application.yml      # Config con quickstack.* properties
└── db/migration/        # Flyway V1-V7
```

## Testing

```bash
./mvnw test                        # Todos los tests
./mvnw test -pl quickstack-user    # Solo un módulo
./mvnw verify                      # Build completo + tests
```

## Seguridad

Ver `docs/security/CLAUDE.md` para contexto de seguridad y ASVS.
Ver `docs/PHASE_0.3_AUTH_ROADMAP.md` para detalle de implementación auth.
