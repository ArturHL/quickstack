# QuickStack POS - Arquitectura Tecnica

> **Ultima actualizacion:** 2026-02-18
> **Fase actual:** Phase 0 - Foundation (0.3 completado, 0.4 pendiente)

---

## Stack Tecnologico

### Frontend
| Tecnologia | Version | Proposito |
|------------|---------|-----------|
| React | 18.x | UI Library |
| Vite | 5.x | Build tool |
| TypeScript | 5.x | Type safety |
| Material UI (MUI) | 5.x | Component library |
| Zustand | 4.x | State management |
| TanStack Query | 5.x | Server state / caching |
| Axios | 1.x | HTTP client |
| React Router | 6.x | Routing |
| js-cookie | 3.x | Secure cookie handling |

### Backend
| Tecnologia | Version | Proposito |
|------------|---------|-----------|
| Java | 17 LTS | Runtime |
| Spring Boot | 3.5.x | Framework |
| Spring Data JPA | 3.x | ORM / Data access |
| Spring Security | 6.x | Security |
| Spring WebSocket | 3.x | Real-time KDS |
| PostgreSQL | 16.x | Database |
| Flyway | 10.x | DB migrations |
| JUnit 5 | 5.x | Testing |
| Mockito | 5.x | Mocking |

### Infraestructura
| Servicio | Proposito | Tier |
|----------|-----------|------|
| Vercel | Frontend hosting | Free |
| Render | Backend hosting (Docker) | Free |
| Neon | PostgreSQL serverless | Free |
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

**Razon:** Un solo desarrollador, commits atomicos, facil compartir configuraciones.

---

### 2. Backend: Package by Feature

```
src/main/java/com/quickstack/
├── QuickstackApplication.java
├── common/                    # Shared utilities
│   ├── config/               # Base Spring configs
│   ├── exception/            # Global exception handling
│   ├── security/             # Base security (PasswordService, BreachChecker)
│   └── audit/                # Auditing (created_at, etc.)
├── auth/                      # Security & Session module
│   ├── security/             # JWT filters, Rate limit filters
│   ├── service/              # Login, Refresh, Password reset logic
│   └── controller/           # Auth and Session endpoints
├── tenant/                    # Tenant module
│   ├── Tenant.java           # Entity
│   ├── TenantRepository.java
│   ├── TenantService.java
│   └── TenantController.java
├── branch/                    # Branch module
│   └── ...
├── user/                      # Identity CRUD module
│   ├── User.java             # Entity
│   ├── UserRepository.java
│   └── UserService.java
├── product/                   # Product module
│   └── ...
└── pos/                       # POS/Orders module (Phase 1+)
    └── ...
```

**Razon:** Cada feature es autocontenido, facil de navegar, escala mejor que capas globales.

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

**Estrategia:** Todas las tablas tienen columna `tenant_id`. Un filtro/interceptor inyecta automaticamente el tenant del usuario autenticado.

**Composite Foreign Keys:** Todas las FK incluyen `tenant_id` para prevenir referencias cross-tenant a nivel de base de datos.

```sql
-- Ejemplo: Tabla products con FK compuesta
CREATE TABLE products (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    category_id UUID,
    name VARCHAR(255) NOT NULL,
    -- ... mas campos
    CONSTRAINT uq_products_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_products_category FOREIGN KEY (tenant_id, category_id)
        REFERENCES categories(tenant_id, id)
);

-- Indice para queries por tenant
CREATE INDEX idx_products_tenant ON products(tenant_id);
```

**Implementacion en Spring:**
- `TenantFilter`: Extrae tenant_id del JWT token
- `TenantContext`: ThreadLocal para acceso al tenant actual
- `@TenantScoped`: Annotation para queries automaticos con filtro de tenant

---

### 5. Authentication: Spring Security Nativo (OWASP ASVS L2)

**Flujo de Login:**
1. Usuario envia credenciales a `POST /api/v1/auth/login`
2. Backend valida email/password contra BD (Argon2id hash)
3. Backend genera JWT (access_token, 15 min) + refresh_token (7 dias)
4. Access token retornado en response body, refresh token en httpOnly cookie
5. Frontend almacena access token en memoria (no localStorage)

**Flujo de Request Autenticado:**
1. Frontend envia JWT en header `Authorization: Bearer <token>`
2. Backend valida firma JWT (RS256, clave privada en backend)
3. Backend extrae claims (user_id, tenant_id, roles)
4. Request procede con contexto de usuario y tenant

**Flujo de Refresh:**
1. Access token expira, frontend llama `POST /api/v1/auth/refresh`
2. Backend valida refresh token de httpOnly cookie
3. Backend rota refresh token (invalida el anterior, genera nuevo)
4. Retorna nuevo access token + nuevo refresh token

**Decisiones de Seguridad (ASVS L2):**

| Parametro | Valor | Razon |
|-----------|-------|-------|
| Password hashing | Argon2id (iterations=3, memory=65536 KB, parallelism=4) + pepper versionado | Recomendacion OWASP 2024 |
| Password minimo | 12 caracteres, max 128, sin reglas de composicion | ASVS V2.1 |
| HIBP falla | Bloquear registro (blockOnFailure=true) | Seguridad sobre UX |
| JWT signing | RS256 2048-bit, rechaza HS256 y `none` | Asimetrico + algorithm confusion protection |
| Access token expiry | 15 minutos | Balance seguridad/UX |
| Refresh token expiry | 7 dias, rotation en cada uso | Con family tracking |
| Refresh token reuso | Revoca toda la familia de tokens | Detecta token theft |
| Rate limit por IP | 10 req/min (Bucket4j + Caffeine) | Mitiga brute force |
| Rate limit por email | 5 req/min | Mitiga credential stuffing |
| Account lockout | 5 intentos fallidos = 15 min, auto-unlock | ASVS V2.2 |

**Configuracion de Cookie (Refresh Token):**

```
Name:     __Host-refreshToken
HttpOnly: true
Secure:   true
SameSite: Strict
Path:     /api/v1/auth
Max-Age:  604800 (7 dias)
```

**Procedimiento de Rotacion de JWT Keys:**

```bash
# Generar nuevo par RSA 2048-bit
openssl genrsa -out jwt-private.pem 2048
openssl rsa -in jwt-private.pem -pubout -out jwt-public.pem

# Codificar para env vars
base64 -w 0 jwt-private.pem > jwt-private.b64
base64 -w 0 jwt-public.pem > jwt-public.b64
```

Proceso de rotacion normal (anual):

| Paso | Accion |
|------|--------|
| 1 | Generar nuevo par de claves |
| 2 | Agregar public key actual a `JWT_PREVIOUS_PUBLIC_KEYS` (gracia 7 dias) |
| 3 | Actualizar `JWT_PRIVATE_KEY` y `JWT_PUBLIC_KEY` con nuevas claves |
| 4 | Deploy en Render |
| 5 | Despues de 7 dias, remover clave anterior de `JWT_PREVIOUS_PUBLIC_KEYS` |

Rotacion de emergencia (compromiso de clave):
- **NO** agregar clave comprometida a `JWT_PREVIOUS_PUBLIC_KEYS`
- Deploy inmediato — todos los usuarios tendran que re-autenticarse
- Documentar incidente

**Roles:**
| Rol | Permisos |
|-----|----------|
| OWNER | Todo (CRUD tenants, branches, users, products, reports, settings) |
| CASHIER | Crear pedidos, ver productos, gestionar clientes (solo su branch asignado) |
| KITCHEN | Solo KDS - ver y actualizar estado de ordenes |

**Decisiones de Roles:**
- Un usuario tiene exactamente UN rol (no many-to-many)
- Permisos almacenados como JSON array en tabla `roles`
- OWNER no requiere branch_id asignado (accede a todas las sucursales)
- CASHIER y KITCHEN requieren branch_id asignado

**JWT Claims:**
```json
{
  "sub": "uuid-del-usuario",
  "email": "user@example.com",
  "tenant_id": "uuid-del-tenant",
  "branch_id": "uuid-del-branch",
  "role": "CASHIER",
  "iat": 1707123456,
  "exp": 1707124356
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
    "timestamp": "2026-02-05T10:00:00Z"
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
    "timestamp": "2026-02-05T10:00:00Z"
  }
}
```

**HTTP Status Codes:**
| Code | Uso |
|------|-----|
| 200 | OK (GET, PUT, PATCH exitosos) |
| 201 | Created (POST exitoso) |
| 204 | No Content (DELETE exitoso) |
| 400 | Bad Request (validacion fallida) |
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

**Campos de auditoria (todas las tablas):**
```sql
created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
created_by UUID REFERENCES users(id),
updated_by UUID REFERENCES users(id)
```

**Modulos de Base de Datos (32 tablas en 6 modulos):**

| Modulo | Tablas | Descripcion |
|--------|--------|-------------|
| Global Catalogs | 5 | subscription_plans, roles, order_status_types, stock_movement_types, unit_types |
| Core | 3 | tenants, branches, users |
| Auth | 3 | password_reset_tokens, refresh_tokens, login_attempts |
| Catalog | 7 | categories, products, product_variants, modifier_groups, modifiers, combos, combo_items |
| Inventory | 6 | ingredients, suppliers, recipes, stock_movements, purchase_orders, purchase_order_items |
| POS | 8 | areas, tables, customers, orders, order_items, order_item_modifiers, payments, order_status_history |
| Notifications | 2 | notification_logs, notification_templates |

---

### 8. Soft Delete Strategy

| Estrategia | Entidades | Razon |
|------------|-----------|-------|
| Soft delete | tenants, branches, users, products, categories, ingredients, customers | Referencias historicas de ordenes, requisitos legales/auditoria |
| Hard delete | auth_identities, combo_items, recipes | Sin valor historico, cascade con padre |
| Never delete | orders, payments, stock_movements, order_status_history | Registros financieros/auditoria |

**Implementacion:**
```sql
deleted_at TIMESTAMP WITH TIME ZONE NULL,
deleted_by UUID REFERENCES users(id)
```

> Registros con `deleted_at IS NOT NULL` se excluyen por defecto en queries.

---

### 9. WebSockets: KDS en Tiempo Real

**Tecnologia:** Spring WebSocket con STOMP protocol

**Arquitectura:**

```
┌─────────────────┐     WebSocket      ┌─────────────────┐
│   POS Terminal  │ ─────────────────> │  Spring Boot    │
│   (React)       │                    │  WebSocket      │
└─────────────────┘                    │  Handler        │
                                       └────────┬────────┘
┌─────────────────┐     WebSocket              │
│   KDS Display   │ <───────────────── ────────┘
│   (React)       │     (Push updates)
└─────────────────┘
```

**Topics:**
- `/topic/kds/{branchId}/orders` - Nuevas ordenes y actualizaciones
- `/topic/kds/{branchId}/items` - Cambios de estado de items

**Eventos:**
```json
{
  "type": "ORDER_CREATED",
  "orderId": "uuid",
  "orderNumber": "ORD-20260205-001",
  "dailySequence": 1,
  "items": [...],
  "timestamp": "2026-02-05T10:00:00Z"
}
```

**Decision:** WebSockets sobre polling porque:
- Latencia minima para operaciones de cocina
- Menor carga en servidor (no requests repetidos)
- Mejor UX para KDS en tiempo real

---

### 10. Notificaciones Digitales

**Estrategia:** Tickets digitales via WhatsApp/Email en lugar de impresion fisica

**Canales soportados:**
| Canal | Proposito | Proveedor sugerido |
|-------|-----------|-------------------|
| WhatsApp | Ticket digital, confirmacion de pedido, notificacion "listo" | Twilio / WhatsApp Business API |
| Email | Ticket digital, resumen de compra | SendGrid / Resend |
| SMS | Fallback si no hay WhatsApp | Twilio |

**Tipos de contenido:**
- `RECEIPT` - Ticket de compra
- `ORDER_CONFIRMATION` - Confirmacion de pedido (delivery/takeout)
- `ORDER_READY` - Notificacion de pedido listo

**Flujo:**
1. Orden completada y pagada
2. Sistema envia ticket digital al canal preferido del cliente
3. Se registra en `notification_logs` para auditoria
4. Reintentos automaticos si falla

**Ventajas sobre impresion:**
- Sin costo de papel/tinta
- Cliente siempre tiene su ticket
- Facilita reordenes y fidelizacion
- Mejor para el medio ambiente

---

### 11. Git Workflow: GitHub Flow

```
main (produccion)
  │
  ├── feature/QS-001-setup-monorepo
  ├── feature/QS-002-auth0-integration
  ├── feature/QS-003-tenant-crud
  └── fix/QS-010-login-redirect
```

**Reglas:**
1. `main` siempre esta deployable
2. Crear branch desde `main` para cada feature/fix
3. Prefijo con ticket ID: `feature/QS-XXX-descripcion`
4. Pull Request obligatorio para merge a main
5. CI debe pasar antes de merge
6. Squash merge para historial limpio

---

### 12. Testing Strategy: TDD

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

**Cobertura minima:**
- Services: 80%+
- Controllers: Integration tests para happy path + error cases
- Repositories: Solo si hay queries custom

**Frontend:**
- Vitest para unit tests
- React Testing Library para componentes
- MSW para mock de API

---

### 13. Environment Variables

**Backend (`application.yml` + env vars):**
```yaml
# Database
NEON_DATABASE_URL=postgresql://user:pass@host/db
NEON_DATABASE_POOL_SIZE=5

# JWT (RS256)
JWT_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----...
JWT_PUBLIC_KEY=-----BEGIN PUBLIC KEY-----...
JWT_ACCESS_TOKEN_EXPIRY=900
JWT_REFRESH_TOKEN_EXPIRY=604800

# App
APP_CORS_ORIGINS=https://quickstack.app,http://localhost:5173
```

**Frontend (`.env`):**
```bash
VITE_API_URL=https://api.quickstack.app/v1
VITE_WS_URL=wss://api.quickstack.app/ws
```

---

## Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────────┐
│                           BROWSER                                    │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                   React + Vite (MUI)                         │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │   │
│  │  │  Zustand │  │ TanStack │  │   Auth   │  │WebSocket │    │   │
│  │  │  Store   │  │  Query   │  │  Context │  │  Client  │    │   │
│  │  └──────────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘    │   │
│  └──────────────────────┼────────────┼─────────────┼───────────┘   │
└─────────────────────────┼────────────┼─────────────┼───────────────┘
                          │            │             │
                    Axios │      JWT   │             │ WSS
                          ▼     Bearer ▼             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          VERCEL                                      │
│                     (Static Hosting)                                 │
└─────────────────────────────────────────────────────────────────────┘
                          │
                          │ HTTPS
                          ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          RENDER                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                Spring Boot (Docker)                          │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │   │
│  │  │   Security   │  │   Tenant     │  │    JPA       │      │   │
│  │  │  (JWT/RS256) │  │   Filter     │  │  Hibernate   │      │   │
│  │  └──────────────┘  └──────────────┘  └──────┬───────┘      │   │
│  │                                              │               │   │
│  │  ┌──────────────┐  ┌──────────────┐         │               │   │
│  │  │  WebSocket   │  │ Notification │         │               │   │
│  │  │   Handler    │  │   Service    │         │               │   │
│  │  │   (STOMP)    │  │(WhatsApp/Email)        │               │   │
│  │  └──────────────┘  └──────────────┘         │               │   │
│  └──────────────────────────────────────────────┼───────────────┘   │
└─────────────────────────────────────────────────┼───────────────────┘
                                                  │
                                                  │ SSL
                                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                           NEON                                       │
│                     PostgreSQL Serverless                            │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │               6 Modulos - 32 Tablas                          │   │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐   │   │
│  │  │  Global   │ │   Core    │ │  Catalog  │ │ Inventory │   │   │
│  │  │ Catalogs  │ │           │ │           │ │           │   │   │
│  │  └───────────┘ └───────────┘ └───────────┘ └───────────┘   │   │
│  │  ┌───────────┐ ┌───────────┐                               │   │
│  │  │    POS    │ │  Notif.   │                               │   │
│  │  │           │ │           │                               │   │
│  │  └───────────┘ └───────────┘                               │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Flujo de Request Autenticado

> [!TIP]
> **Diagramas Detallados:** Puedes consultar diagramas de secuencia interactivos a nivel de archivo para todos los casos de uso principales en [Diagramas de Flujos de Casos de Uso](./flows/README.md).

```
1. Usuario hace click en "Ver productos"
   │
2. React Router renderiza ProductList component
   │
3. useProducts() hook (TanStack Query) dispara fetch
   │
4. Axios interceptor anade JWT: Authorization: Bearer <token>
   │
5. Request viaja a Render: GET /v1/products
   │
6. Spring Security valida firma JWT (RS256, clave publica local)
   │
7. TenantFilter extrae tenant_id del JWT, lo pone en TenantContext
   │
8. ProductController recibe request
   │
9. ProductService.findAll() ejecuta query
   │
10. JPA anade automaticamente: WHERE tenant_id = :currentTenant
    │
11. PostgreSQL retorna resultados
    │
12. Response viaja de vuelta al browser
    │
13. TanStack Query cachea y actualiza el UI
```

---

## Flujo KDS en Tiempo Real

```
1. Cajero crea orden en POS
   │
2. OrderService guarda orden en BD
   │
3. OrderService publica evento a WebSocket
   │
4. WebSocket Handler envia a /topic/kds/{branchId}/orders
   │
5. KDS Display (suscrito) recibe orden instantaneamente
   │
6. Cocinero ve orden en pantalla, inicia preparacion
   │
7. Cocinero marca item como "listo" en KDS
   │
8. KDS envia actualizacion via WebSocket
   │
9. POS recibe notificacion de item listo
   │
10. Cajero/mesero notifica al cliente
```

---

## Tipos de Servicio

| Tipo | Codigo | Descripcion | Requiere |
|------|--------|-------------|----------|
| Mesa | DINE_IN | Cliente come en restaurante | table_id |
| Mostrador | COUNTER | Cliente ordena y espera en mostrador | - |
| Delivery | DELIVERY | Entrega a domicilio | customer_id, address |
| Para llevar | TAKEOUT | Cliente recoge en local | customer_id (opcional) |

---

## Order Number Format

**Formato completo:** `ORD-YYYYMMDD-XXX`
- Ejemplo: `ORD-20260205-001`
- Unico por tenant

**Secuencia diaria:** `daily_sequence` INTEGER
- Se reinicia cada dia por branch
- Para mostrar en cocina/KDS como "Orden #7"
- Constraint: UNIQUE (tenant_id, branch_id, DATE(opened_at), daily_sequence)

---

## Architecture Decision Records (ADR)

### ADR-001: Precios Desnormalizados en Order Items

**Contexto:** Los productos cambian de precio con el tiempo. Las ordenes historicas deben preservar el precio al momento de la venta.

**Decision:** Copiar `product_name`, `variant_name`, `unit_price`, y precios de modifiers en order items.

**Razones:**
- Garantiza registros financieros precisos sin importar cambios futuros
- Simplifica reportes (no se necesitan tablas de "historial de precios")
- Overhead de almacenamiento menor (~500 bytes por order item)

**Trade-offs:**
- Mayor almacenamiento (aceptable para escala MVP)
- Renombrar productos no actualiza ordenes pasadas (intencional)

---

### ADR-002: Un Rol por Usuario

**Contexto:** Requerimientos especifican que usuarios no pueden tener multiples roles.

**Decision:** FK directa de `users.role_id` a `roles.id` en lugar de tabla de union.

**Razones:**
- Logica de autorizacion mas simple
- Cumple con requerimientos establecidos
- Una tabla y join menos

**Trade-offs:**
- No se pueden asignar multiples roles sin cambio de schema
- OK para alcance del MVP

---

### ADR-003: WebSockets para KDS

**Contexto:** La cocina necesita ver ordenes en tiempo real.

**Decision:** WebSockets con STOMP sobre polling HTTP.

**Razones:**
- Latencia minima (~50ms vs ~5s con polling)
- Menor carga en servidor
- Mejor experiencia de usuario para operaciones criticas

**Trade-offs:**
- Mayor complejidad de infraestructura
- Necesita manejo de reconexion en cliente

---

### ADR-004: Tickets Digitales sobre Impresion

**Contexto:** MVP necesita entregar tickets a clientes.

**Decision:** WhatsApp/Email como canal primario, sin impresion fisica.

**Razones:**
- Sin costo de hardware (impresora termica)
- Sin costos recurrentes (papel, tinta)
- Cliente siempre tiene su ticket
- Facilita reordenes y CRM

**Trade-offs:**
- Requiere que cliente proporcione contacto
- Dependencia de servicios externos (Twilio/SendGrid)

---

---

## Deuda Tecnica Aceptada

| Deuda | Riesgo | Plan de Remediacion |
|-------|--------|---------------------|
| Sin MFA en MVP | Medio-Alto | TOTP para OWNER en Phase 1 |
| JWT keys en env vars | Medio | Migrar a AWS KMS o Vault post-piloto |
| Rate limiting in-memory (Caffeine) | Bajo | Redis si multiples instancias de backend |
| Sin vault de secretos | Medio | Evaluar HashiCorp Vault post-piloto |
| zxcvbn no implementado | Bajo | Longitud minima (12 chars) como proxy de fuerza |

---

## Changelog

### 2026-02-20
- Extraccion del modulo `quickstack-auth` para centralizar la logica de seguridad e infraestructura de sesion.
- Reorganizacion de `PasswordService` y `PasswordBreachChecker` en `quickstack-common`.
- Desacoplamiento de `quickstack-user`, dejandolo enfocado en CRUD de identidad (Identity).

### 2026-02-18
- Agregadas decisiones de seguridad detalladas (parametros Argon2id, rate limiting, cookie config)
- Agregado procedimiento de rotacion de JWT keys
- Agregada seccion de Deuda Tecnica Aceptada
- Actualizado conteo de tablas a 32 (29 originales + 3 de auth: refresh_tokens, password_reset_tokens, login_attempts)

### 2026-02-05
- Actualizado Spring Boot a version 3.5.x
- Agregada seccion de WebSockets para KDS en tiempo real
- Agregada seccion de Notificaciones Digitales (WhatsApp/Email)
- Actualizado diagrama de BD con 6 modulos y 27 tablas
- Actualizado sistema de roles: OWNER, CASHIER, KITCHEN (removido ADMIN)
- Agregada estrategia de Soft Delete detallada
- Agregados tipos de servicio (DINE_IN, COUNTER, DELIVERY, TAKEOUT)
- Agregado formato de order number (ORD-YYYYMMDD-XXX + daily_sequence)
- Agregados ADRs para decisiones clave
- Actualizado diagrama de arquitectura con WebSocket y Notifications

### 2026-01-26
- Documento inicial de arquitectura
- Definicion de stack completo
- Decisiones de monorepo, JPA, Zustand, MUI
- Estrategia de multi-tenancy y auth
