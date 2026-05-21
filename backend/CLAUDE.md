# QuickStack Backend

Java 17 + Spring Boot 3.5 | Multi-module Maven

## Módulos

| Módulo | Propósito |
|--------|-----------|
| quickstack-common | Config properties, exceptions, basic security |
| quickstack-auth | **DEPRECATED** (Archivado) - Sustituido por AWS Cognito |
| quickstack-user | User CRUD, identity |
| quickstack-product | Catálogo de productos |
| quickstack-pos | Punto de venta, órdenes |
| quickstack-app | Main app entry point, migrations |

## Convenciones

- **Package by feature**: controller/service/repository por módulo
- **DTOs**: `dto/request/` y `dto/response/`
- **Tests**: `*Test.java` (unit), `*RepositoryTest.java` (slice), `*E2ETest.java` (full system)
- **Exceptions**: Custom en `common/exception/`, handler en GlobalExceptionHandler

## Auth Actual (Migración a AWS Cognito)

| Aspecto | Implementación |
|---------|----------------|
| Identity Provider | Amazon Cognito User Pools |
| Validación | API Gateway (Cognito Authorizers) + Spring Security OAuth2 Resource Server |
| Multi-tenant | `tenant_id` mapeado como atributo personalizado en Cognito |
| Password Reset | Gestionado nativamente por los flujos alojados de Cognito |

## Archivos Clave

```
quickstack-common/src/main/java/.../common/
├── config/properties/   # JwtProperties, PasswordProperties, RateLimitProperties
├── exception/           # AuthenticationException, InvalidTokenException, etc.
└── security/            # PasswordService, PasswordBreachChecker, JwtAuthenticationPrincipal

quickstack-common/src/main/java/.../common/
├── config/properties/   # RateLimitProperties
├── exception/           # Custom exceptions
└── security/            # Configuraciones base y filtros de seguridad

quickstack-user/src/main/java/.../user/
├── service/             # UserService
├── entity/              # User
└── repository/          # UserRepository

quickstack-product/src/main/java/.../product/
├── controller/          # CategoryController, ProductController, VariantController, MenuController, ModifierGroupController
├── security/            # CatalogPermissionEvaluator
├── service/             # CategoryService, ProductService, VariantService, MenuService, ModifierGroupService, ModifierService
├── entity/              # Category, Product, ProductVariant, ModifierGroup, Modifier
├── repository/          # CategoryRepository, ProductRepository, VariantRepository, ModifierGroupRepository, ModifierRepository
└── dto/                 # *CreateRequest, *UpdateRequest, *Response para todas las entidades del catálogo

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
