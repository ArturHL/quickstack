# QuickStack Backend

Java 17 + Spring Boot 3.5 | Multi-module Maven

## Módulos

| Módulo | Propósito |
|--------|-----------|
| quickstack-common | Config properties, exceptions, security utils |
| quickstack-tenant | Gestión de tenants |
| quickstack-branch | Gestión de sucursales |
| quickstack-user | Auth, users, sessions, tokens |
| quickstack-product | Catálogo de productos |
| quickstack-pos | Punto de venta, órdenes |
| quickstack-app | Main app, filters, security config, migrations |

## Convenciones

- **Package by feature**: controller/service/repository por módulo
- **DTOs**: `dto/request/` y `dto/response/`
- **Tests**: `*Test.java` (unit), `*IntegrationTest.java` (integration)
- **Exceptions**: Custom en `common/exception/`, handler en GlobalExceptionHandler

## Auth Actual (Phase 0.3)

| Aspecto | Implementación |
|---------|----------------|
| Password | Argon2id + pepper versionado |
| JWT | RS256 (2048 bits), 15min access / 7d refresh |
| Rate limit | 10 req/min IP, 5 req/min email |
| Lockout | 5 intentos fallidos = 15 min lock |
| Cookies | HttpOnly, Secure, SameSite=Strict |

## Archivos Clave

```
quickstack-common/src/main/java/.../common/
├── config/properties/   # JwtProperties, PasswordProperties, RateLimitProperties
├── exception/           # AuthenticationException, InvalidTokenException, etc.
└── security/            # SecureTokenGenerator, IpAddressExtractor

quickstack-user/src/main/java/.../user/
├── controller/          # AuthController
├── service/             # UserService, PasswordService, RefreshTokenService
├── entity/              # User, RefreshToken, LoginAttempt
└── repository/          # UserRepository, RefreshTokenRepository

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
