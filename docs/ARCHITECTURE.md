# QuickStack POS - Arquitectura Técnica

> **Última actualización:** 2026-01-26
> **Fase actual:** Phase 0 - Foundation

---

## Stack Tecnológico

### Frontend
| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| React | 18.x | UI Library |
| Vite | 5.x | Build tool |
| TypeScript | 5.x | Type safety |
| Material UI (MUI) | 5.x | Component library |
| Zustand | 4.x | State management |
| TanStack Query | 5.x | Server state / caching |
| Axios | 1.x | HTTP client |
| React Router | 6.x | Routing |
| Auth0 React SDK | 2.x | Authentication |

### Backend
| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Java | 21 LTS | Runtime |
| Spring Boot | 3.2.x | Framework |
| Spring Data JPA | 3.x | ORM / Data access |
| Spring Security | 6.x | Security |
| PostgreSQL | 16.x | Database |
| Flyway | 10.x | DB migrations |
| JUnit 5 | 5.x | Testing |
| Mockito | 5.x | Mocking |

### Infraestructura
| Servicio | Propósito | Tier |
|----------|-----------|------|
| Vercel | Frontend hosting | Free |
| Render | Backend hosting (Docker) | Free |
| Neon | PostgreSQL serverless | Free |
| Auth0 | Authentication | Free |
| GitHub | Code repository | Free |
| GitHub Actions | CI/CD | Free |

---

## Decisiones de Arquitectura

### 1. Repositorio: Monorepo

```
quickstack-pos/
├── frontend/          # React + Vite app
├── backend/           # Spring Boot app
├── docs/              # Documentation
├── .github/           # GitHub Actions workflows
└── README.md
```

**Razón:** Un solo desarrollador, commits atómicos, fácil compartir configuraciones.

---

### 2. Backend: Package by Feature

```
src/main/java/com/quickstack/
├── QuickstackApplication.java
├── common/                    # Shared utilities
│   ├── config/               # Spring configs
│   ├── exception/            # Global exception handling
│   ├── security/             # Auth0 + JWT config
│   └── audit/                # Auditing (created_at, etc.)
├── tenant/                    # Tenant module
│   ├── Tenant.java           # Entity
│   ├── TenantRepository.java
│   ├── TenantService.java
│   └── TenantController.java
├── branch/                    # Branch module
│   ├── Branch.java
│   ├── BranchRepository.java
│   ├── BranchService.java
│   └── BranchController.java
├── user/                      # User module
│   └── ...
├── product/                   # Product module
│   └── ...
└── pos/                       # POS/Orders module (Phase 1+)
    └── ...
```

**Razón:** Cada feature es autocontenido, fácil de navegar, escala mejor que capas globales.

---

### 3. Frontend: Feature-based Structure

```
src/
├── main.tsx
├── App.tsx
├── routes/                    # Route definitions
├── components/                # Shared UI components
│   └── ui/                   # Generic (Button, Input, etc.)
├── features/                  # Feature modules
│   ├── auth/                 # Authentication
│   │   ├── components/
│   │   ├── hooks/
│   │   └── stores/          # Zustand stores
│   ├── branches/             # Branch management
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── api/             # TanStack Query hooks
│   │   └── types/
│   ├── products/             # Product management
│   │   └── ...
│   └── pos/                  # Point of Sale (Phase 1+)
│       └── ...
├── lib/                       # Utilities
│   ├── api/                  # Axios instance + interceptors
│   └── utils/
├── stores/                    # Global Zustand stores
└── types/                     # Shared TypeScript types
```

---

### 4. Multi-tenancy: Shared Database with tenant_id

**Estrategia:** Todas las tablas tienen columna `tenant_id`. Un filtro/interceptor inyecta automáticamente el tenant del usuario autenticado.

```sql
-- Ejemplo: Tabla products
CREATE TABLE products (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    branch_id UUID REFERENCES branches(id),
    name VARCHAR(255) NOT NULL,
    -- ... más campos
    CONSTRAINT fk_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- Índice para queries por tenant
CREATE INDEX idx_products_tenant ON products(tenant_id);
```

**Implementación en Spring:**
- `TenantFilter`: Extrae tenant_id del JWT token
- `TenantContext`: ThreadLocal para acceso al tenant actual
- `@TenantScoped`: Annotation para queries automáticos con filtro de tenant

---

### 5. Authentication: Auth0 con OWASP ASVS L1

**Flujo:**
1. Usuario hace login en frontend via Auth0 React SDK
2. Auth0 retorna JWT (access_token)
3. Frontend envía JWT en header `Authorization: Bearer <token>`
4. Backend valida JWT con Auth0 JWKS
5. Backend extrae claims (user_id, tenant_id, roles)
6. Request procede con contexto de usuario y tenant

**Roles:**
| Rol | Permisos |
|-----|----------|
| OWNER | Todo (CRUD tenants, branches, users, products, reports) |
| ADMIN | CRUD branches, users, products dentro de su tenant |
| CASHIER | Crear pedidos, ver productos (solo su branch asignado) |

**JWT Custom Claims:**
```json
{
  "sub": "auth0|123456",
  "email": "user@example.com",
  "https://quickstack.app/tenant_id": "uuid-del-tenant",
  "https://quickstack.app/branch_id": "uuid-del-branch",
  "https://quickstack.app/roles": ["ADMIN"]
}
```

---

### 6. API Design

**Base URL:** `https://api.quickstack.app/v1`

**Versionado:** URL path (`/v1/`, `/v2/`)

**Formato de respuesta:**
```json
// Success
{
  "data": { ... },
  "meta": {
    "timestamp": "2026-01-26T10:00:00Z"
  }
}

// Error
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "El campo 'name' es requerido",
    "details": [
      { "field": "name", "message": "must not be blank" }
    ]
  },
  "meta": {
    "timestamp": "2026-01-26T10:00:00Z"
  }
}
```

**HTTP Status Codes:**
| Code | Uso |
|------|-----|
| 200 | OK (GET, PUT, PATCH exitosos) |
| 201 | Created (POST exitoso) |
| 204 | No Content (DELETE exitoso) |
| 400 | Bad Request (validación fallida) |
| 401 | Unauthorized (no autenticado) |
| 403 | Forbidden (sin permisos) |
| 404 | Not Found |
| 409 | Conflict (duplicado, constraint violation) |
| 500 | Internal Server Error |

---

### 7. Database Conventions

**Naming:**
- Tablas: `snake_case` plural (`products`, `order_items`)
- Columnas: `snake_case` (`created_at`, `tenant_id`)
- Primary keys: `id` tipo `UUID`
- Foreign keys: `{table_singular}_id` (`product_id`, `branch_id`)

**Campos de auditoría (todas las tablas):**
```sql
created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
created_by UUID REFERENCES users(id),
updated_by UUID REFERENCES users(id)
```

**Soft delete:**
```sql
deleted_at TIMESTAMP WITH TIME ZONE NULL,
deleted_by UUID REFERENCES users(id)
```

> Registros con `deleted_at IS NOT NULL` se excluyen por defecto en queries.

---

### 8. Git Workflow: GitHub Flow

```
main (producción)
  │
  ├── feature/QS-001-setup-monorepo
  ├── feature/QS-002-auth0-integration
  ├── feature/QS-003-tenant-crud
  └── fix/QS-010-login-redirect
```

**Reglas:**
1. `main` siempre está deployable
2. Crear branch desde `main` para cada feature/fix
3. Prefijo con ticket ID: `feature/QS-XXX-descripcion`
4. Pull Request obligatorio para merge a main
5. CI debe pasar antes de merge
6. Squash merge para historial limpio

---

### 9. Testing Strategy: TDD

**Backend:**
```
src/
├── main/java/...
└── test/java/
    └── com/quickstack/
        ├── tenant/
        │   ├── TenantServiceTest.java      # Unit tests
        │   └── TenantControllerTest.java   # Integration tests
        └── ...
```

**Cobertura mínima:**
- Services: 80%+
- Controllers: Integration tests para happy path + error cases
- Repositories: Solo si hay queries custom

**Frontend:**
- Vitest para unit tests
- React Testing Library para componentes
- MSW para mock de API

---

### 10. Environment Variables

**Backend (`application.yml` + env vars):**
```yaml
# Database
NEON_DATABASE_URL=postgresql://user:pass@host/db
NEON_DATABASE_POOL_SIZE=5

# Auth0
AUTH0_ISSUER_URI=https://quickstack.auth0.com/
AUTH0_AUDIENCE=https://api.quickstack.app

# App
APP_CORS_ORIGINS=https://quickstack.app,http://localhost:5173
```

**Frontend (`.env`):**
```bash
VITE_API_URL=https://api.quickstack.app/v1
VITE_AUTH0_DOMAIN=quickstack.auth0.com
VITE_AUTH0_CLIENT_ID=xxxxxxxxxxxx
VITE_AUTH0_AUDIENCE=https://api.quickstack.app
```

---

## Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                         BROWSER                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                 React + Vite (MUI)                       │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐              │   │
│  │  │  Zustand │  │ TanStack │  │  Auth0   │              │   │
│  │  │  Store   │  │  Query   │  │   SDK    │              │   │
│  │  └──────────┘  └────┬─────┘  └────┬─────┘              │   │
│  └──────────────────────┼────────────┼─────────────────────┘   │
└─────────────────────────┼────────────┼─────────────────────────┘
                          │            │
                    Axios │            │ OAuth 2.0 PKCE
                          ▼            ▼
┌─────────────────────────────────────────────────────────────────┐
│                        VERCEL                                    │
│                   (Static Hosting)                               │
└─────────────────────────────────────────────────────────────────┘
                          │
                          │ HTTPS
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                        RENDER                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Spring Boot (Docker)                        │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │   │
│  │  │   Security   │  │   Tenant     │  │    JPA       │  │   │
│  │  │  (JWT/Auth0) │  │   Filter     │  │  Hibernate   │  │   │
│  │  └──────────────┘  └──────────────┘  └──────┬───────┘  │   │
│  └──────────────────────────────────────────────┼──────────┘   │
└─────────────────────────────────────────────────┼───────────────┘
                                                  │
                                                  │ SSL
                                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                         NEON                                     │
│                   PostgreSQL Serverless                          │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐  │
│  │ tenants │ │ branches│ │  users  │ │products │ │ orders  │  │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        AUTH0                                     │
│              Identity Provider (OAuth 2.0)                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                      │
│  │  Users   │  │  Roles   │  │   JWKS   │                      │
│  └──────────┘  └──────────┘  └──────────┘                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## Flujo de Request Autenticado

```
1. Usuario hace click en "Ver productos"
   │
2. React Router renderiza ProductList component
   │
3. useProducts() hook (TanStack Query) dispara fetch
   │
4. Axios interceptor añade JWT: Authorization: Bearer <token>
   │
5. Request viaja a Render: GET /v1/products
   │
6. Spring Security valida JWT contra Auth0 JWKS
   │
7. TenantFilter extrae tenant_id del JWT, lo pone en TenantContext
   │
8. ProductController recibe request
   │
9. ProductService.findAll() ejecuta query
   │
10. JPA añade automáticamente: WHERE tenant_id = :currentTenant
    │
11. PostgreSQL retorna resultados
    │
12. Response viaja de vuelta al browser
    │
13. TanStack Query cachea y actualiza el UI
```

---

## Changelog

### 2026-01-26
- Documento inicial de arquitectura
- Definición de stack completo
- Decisiones de monorepo, JPA, Zustand, MUI
- Estrategia de multi-tenancy y auth
