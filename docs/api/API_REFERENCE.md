# QuickStack POS — API Reference

> **Base URL producción:** `https://quickstack.onrender.com`
> **Base URL local:** `http://localhost:8080`
> **Versión:** 1.0.0 | **Última actualización:** 2026-03-02
> **Swagger UI:** `{BASE_URL}/swagger-ui.html` | **OpenAPI JSON:** `{BASE_URL}/v3/api-docs`

---

## Convenciones globales

### Autenticación
Todos los endpoints (excepto los marcados como `PUBLIC`) requieren:
```
Authorization: Bearer <accessToken>
```
El `accessToken` se obtiene de `POST /api/v1/auth/login`. Expira en 15 min.

### Envelope de respuesta
Todas las respuestas exitosas siguen el wrapper:
```json
{ "status": "success", "data": { ... } }
```
El frontend extrae siempre `.data` (e.g. `r.data.data` en axios).

### Paginación
Los endpoints paginados reciben query params `page` (default 0) y `size` (default 20).
La respuesta tiene forma `Page<T>`:
```json
{ "content": [...], "totalElements": 42, "totalPages": 3, "number": 0, "size": 20 }
```

### Roles (jerarquía)
`OWNER > MANAGER > CASHIER > WAITER > KITCHEN`

### Multi-tenancy
El `tenantId` se extrae **siempre** del JWT. Nunca va en el body o params.
Acceso cross-tenant retorna `404` (protección IDOR).

### Soft delete
La mayoría de entidades tienen `deletedAt`. Las órdenes y pagos **nunca se borran** (auditoría).

### Errores comunes
| Status | Cuándo |
|--------|--------|
| 400 | Validación fallida o input inválido |
| 401 | Sin token o token expirado |
| 403 | Rol insuficiente |
| 404 | No encontrado o cross-tenant |
| 409 | Violación de regla de negocio (duplicado, estado inválido) |

---

## Módulo: Autenticación

### `POST /api/v1/auth/register` · PUBLIC
Registra un nuevo usuario para un tenant.

**Request**
```json
{
  "tenantId": "uuid",
  "email": "string (max 255, email válido)",
  "password": "string (12–128 chars)",
  "fullName": "string (max 255)",
  "roleId": "uuid",
  "branchId": "uuid?",
  "phone": "string? (max 20)"
}
```
**Response** `201`
```json
{
  "id": "uuid", "email": "string", "fullName": "string",
  "tenantId": "uuid", "roleId": "uuid", "branchId": "uuid?",
  "phone": "string?", "createdAt": "instant"
}
```

---

### `POST /api/v1/auth/login` · PUBLIC
Autentica al usuario. Devuelve accessToken y setea cookie de refresh.

**Request**
```json
{ "tenantId": "uuid", "email": "string", "password": "string (max 128)" }
```
**Response** `200`
```json
{
  "accessToken": "string (JWT RS256)",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "string", "email": "string", "fullName": "string",
    "tenantId": "string", "roleId": "string",
    "branchId": "string?", "lastLoginAt": "instant?"
  }
}
```
**Notas:** Lockout tras 5 intentos fallidos (15 min). Rate limited.

---

### `POST /api/v1/auth/refresh` · PUBLIC (cookie)
Renueva el accessToken usando el refresh token (HttpOnly cookie). Devuelve misma forma que login.

---

### `POST /api/v1/auth/logout` · PUBLIC (cookie)
Revoca el refresh token. Response `204`.

---

### `POST /api/v1/auth/forgot-password` · PUBLIC
Inicia el flujo de reset de contraseña. Respuesta siempre igual (evita enumeración).

**Request** `{ "tenantId": "uuid", "email": "string" }`
**Response** `200` `{ "message": "If the email exists, you will receive a password reset link." }`

---

### `POST /api/v1/auth/reset-password` · PUBLIC
Completa el reset con el token recibido por email.

**Request** `{ "token": "string (max 128)", "newPassword": "string (12–128 chars)" }`
**Response** `200` `{ "message": "Password has been reset successfully." }`
**Notas:** Token de uso único, expira 1h. Revoca todos los refresh tokens del usuario.

---

### `GET /api/v1/users/me/sessions` · JWT
Lista las sesiones activas del usuario autenticado.

**Response** `200` — array de:
```json
{ "id": "string", "ipAddress": "string", "userAgent": "string", "createdAt": "instant", "expiresAt": "instant" }
```

---

### `DELETE /api/v1/users/me/sessions/{id}` · JWT
Revoca una sesión específica. Response `204`.

---

## Módulo: Catálogo — Categorías

### `GET /api/v1/categories` · JWT
Lista categorías del tenant (paginado).

**Query params:** `includeInactive` (bool, default false — solo OWNER/MANAGER) · `page` · `size`

**Response** `200` — `Page<CategoryResponse>`
```json
{
  "id": "uuid", "tenantId": "uuid", "name": "string",
  "description": "string?", "imageUrl": "string?",
  "sortOrder": 0, "isActive": true,
  "createdAt": "instant", "updatedAt": "instant",
  "createdBy": "uuid", "updatedBy": "uuid"
}
```

---

### `POST /api/v1/categories` · JWT · OWNER/MANAGER
Crea una categoría.

**Request**
```json
{
  "name": "string (required, max 255)",
  "description": "string? (max 1000)",
  "imageUrl": "string? (URL válida, max 500)",
  "sortOrder": 0
}
```
**Response** `201` — `CategoryResponse`

---

### `GET /api/v1/categories/{id}` · JWT
Obtiene una categoría por ID. Response `200` — `CategoryResponse`.

---

### `PUT /api/v1/categories/{id}` · JWT · OWNER/MANAGER
Actualiza una categoría. Todos los campos son opcionales.

**Request** — mismos campos que POST más `isActive: bool?`
**Response** `200` — `CategoryResponse`

---

### `PATCH /api/v1/categories/reorder` · JWT · OWNER/MANAGER
Reordena categorías.

**Request** `{ "items": [{ "id": "uuid", "sortOrder": 0 }] }`
**Response** `204`

---

### `DELETE /api/v1/categories/{id}` · JWT · OWNER/MANAGER
Soft-delete. Falla con `409` si la categoría tiene productos activos. Response `204`.

---

### `POST /api/v1/categories/{id}/restore` · JWT · OWNER
Restaura categoría eliminada. Response `200` — `CategoryResponse`.

---

## Módulo: Catálogo — Productos

### `GET /api/v1/products` · JWT
Lista productos (paginado, filtrable).

**Query params:** `categoryId: uuid?` · `search: string?` · `available: bool?` · `includeInactive: bool` (OWNER/MANAGER) · `page` · `size`

**Response** `200` — `Page<ProductSummaryResponse>`
```json
{
  "id": "uuid", "name": "string", "basePrice": 0.00,
  "productType": "SIMPLE|VARIANT|COMBO",
  "isAvailable": true, "isActive": true,
  "categoryId": "uuid", "imageUrl": "string?", "sortOrder": 0
}
```

---

### `POST /api/v1/products` · JWT · OWNER/MANAGER
Crea un producto. Para tipo `VARIANT` incluir `variants[]`.

**Request**
```json
{
  "name": "string (required, max 255)",
  "description": "string? (max 2000)",
  "categoryId": "uuid (required)",
  "sku": "string? (max 50, formato: ^[A-Z0-9_-]{0,50}$)",
  "basePrice": 0.00,
  "costPrice": 0.00,
  "imageUrl": "string? (URL válida, max 500)",
  "productType": "SIMPLE|VARIANT|COMBO",
  "sortOrder": 0,
  "variants": [
    { "name": "string (max 100)", "priceAdjustment": 0.00, "isDefault": false, "sortOrder": 0 }
  ]
}
```
**Response** `201` — `ProductResponse`

---

### `GET /api/v1/products/{id}` · JWT
Obtiene producto con variantes incluidas. Response `200` — `ProductResponse`
```json
{
  "id": "uuid", "name": "string", "description": "string?",
  "sku": "string?", "basePrice": 0.00, "costPrice": "decimal?",
  "imageUrl": "string?", "productType": "SIMPLE|VARIANT|COMBO",
  "isActive": true, "isAvailable": true, "sortOrder": 0,
  "category": { "id": "uuid", "name": "string" },
  "variants": [ { "id": "uuid", "name": "string", "effectivePrice": 0.00, "isDefault": false } ]
}
```

---

### `PUT /api/v1/products/{id}` · JWT · OWNER/MANAGER
Actualiza producto. Todos los campos opcionales. Si incluye `variants[]`, reemplaza todas las variantes existentes.
**Response** `200` — `ProductResponse`

---

### `PATCH /api/v1/products/reorder` · JWT · OWNER/MANAGER
`{ "items": [{ "id": "uuid", "sortOrder": 0 }] }` → `204`

---

### `PATCH /api/v1/products/{id}/availability` · JWT · OWNER/MANAGER
`{ "isAvailable": true }` → `200` — `ProductResponse`

---

### `DELETE /api/v1/products/{id}` · JWT · OWNER/MANAGER
Soft-delete. Response `204`.

---

### `POST /api/v1/products/{id}/restore` · JWT · OWNER
Restaura producto. Response `200` — `ProductResponse`.

---

## Módulo: Catálogo — Variantes

### `GET /api/v1/products/{productId}/variants` · JWT
Lista variantes de un producto.

**Response** `200` — array de `VariantResponse`:
```json
{
  "id": "uuid", "name": "string", "sku": "string?",
  "priceAdjustment": 0.00, "effectivePrice": 0.00,
  "isDefault": false, "isActive": true, "sortOrder": 0
}
```

---

### `POST /api/v1/products/{productId}/variants` · JWT · OWNER/MANAGER
Agrega variante a un producto.

**Request**
```json
{
  "name": "string (required, max 100)",
  "sku": "string? (max 50, ^[A-Z0-9_-]{0,50}$)",
  "priceAdjustment": 0.00,
  "isDefault": false,
  "sortOrder": 0
}
```
**Response** `201` — `VariantResponse`

---

### `PUT /api/v1/products/{productId}/variants/{variantId}` · JWT · OWNER/MANAGER
Actualiza variante. Campos opcionales: `name`, `sku`, `priceAdjustment`, `isDefault`, `sortOrder`, `isActive`.
Response `200` — `VariantResponse`.

---

### `DELETE /api/v1/products/{productId}/variants/{variantId}` · JWT · OWNER/MANAGER
Elimina variante. Response `204`.

---

## Módulo: Catálogo — Modificadores

### `GET /api/v1/products/{productId}/modifier-groups` · JWT
Lista grupos de modificadores de un producto, con sus modificadores incluidos.

**Response** `200` — array de `ModifierGroupResponse`:
```json
{
  "id": "uuid", "name": "string", "description": "string?",
  "minSelections": 0, "maxSelections": "int?",
  "isRequired": false, "sortOrder": 0,
  "modifiers": [
    { "id": "uuid", "name": "string", "priceAdjustment": 0.00, "isDefault": false, "isActive": true, "sortOrder": 0 }
  ]
}
```

---

### `POST /api/v1/products/{productId}/modifier-groups` · JWT · OWNER/MANAGER
Crea grupo de modificadores.

**Request**
```json
{
  "name": "string (required, max 100)",
  "description": "string? (max 500)",
  "minSelections": 0,
  "maxSelections": "int? (>= minSelections)",
  "isRequired": false,
  "sortOrder": 0
}
```
**Validación:** Si `isRequired=true`, `minSelections >= 1`. `maxSelections >= minSelections`.
**Response** `201` — `ModifierGroupResponse`

---

### `GET /api/v1/modifier-groups/{id}` · JWT
Obtiene grupo de modificadores por ID. Response `200` — `ModifierGroupResponse`.

---

### `PUT /api/v1/modifier-groups/{id}` · JWT · OWNER/MANAGER
Actualiza grupo. Todos los campos opcionales. Response `200` — `ModifierGroupResponse`.

---

### `DELETE /api/v1/modifier-groups/{id}` · JWT · OWNER/MANAGER
Soft-delete del grupo y sus modificadores. Response `204`.

---

### `GET /api/v1/modifier-groups/{groupId}/modifiers` · JWT
Lista modificadores activos de un grupo.

**Response** `200` — array de `ModifierResponse`:
```json
{ "id": "uuid", "name": "string", "priceAdjustment": 0.00, "isDefault": false, "isActive": true, "sortOrder": 0 }
```

---

### `POST /api/v1/modifier-groups/{groupId}/modifiers` · JWT · OWNER/MANAGER
Agrega modificador al grupo.

**Request** `{ "name": "string (max 100)", "priceAdjustment": 0.00, "isDefault": false, "sortOrder": 0 }`
**Response** `201` — `ModifierResponse`

---

### `PUT /api/v1/modifiers/{id}` · JWT · OWNER/MANAGER
Actualiza modificador. Campos opcionales: `name`, `priceAdjustment`, `isDefault`, `isActive`, `sortOrder`.
Response `200` — `ModifierResponse`.

---

### `DELETE /api/v1/modifiers/{id}` · JWT · OWNER/MANAGER
Soft-delete. Falla `409` si es el último modificador activo del grupo. Response `204`.

---

## Módulo: Catálogo — Combos

### `GET /api/v1/combos` · JWT
Lista todos los combos del tenant.

**Response** `200` — array de `ComboResponse`:
```json
{
  "id": "uuid", "name": "string", "description": "string?",
  "imageUrl": "string?", "price": 0.00, "isActive": true, "sortOrder": 0,
  "items": [
    { "id": "uuid", "productId": "uuid", "productName": "string?", "quantity": 1, "allowSubstitutes": false }
  ]
}
```

---

### `POST /api/v1/combos` · JWT · OWNER/MANAGER
Crea combo.

**Request**
```json
{
  "name": "string (required, max 255)",
  "description": "string? (max 1000)",
  "imageUrl": "string? (max 500)",
  "price": 0.00,
  "sortOrder": 0,
  "items": [
    { "productId": "uuid", "quantity": 1, "allowSubstitutes": false, "substituteGroup": "string?", "sortOrder": 0 }
  ]
}
```
**Validación:** `items` mínimo 2 elementos.
**Response** `201` — `ComboResponse`

---

### `GET /api/v1/combos/{id}` · JWT
Obtiene combo con items. Response `200` — `ComboResponse`.

---

### `PUT /api/v1/combos/{id}` · JWT · OWNER/MANAGER
Actualiza combo. Si incluye `items[]`, reemplaza todos. Todos los campos opcionales.
Response `200` — `ComboResponse`.

---

### `DELETE /api/v1/combos/{id}` · JWT · OWNER/MANAGER
Soft-delete. Response `204`.

---

## Módulo: Menú POS

### `GET /api/v1/menu` · JWT
Devuelve el menú activo completo para el POS: categorías con productos, variantes y modificadores. Cache-Control 30s.

**Response** `200`
```json
{
  "categories": [
    {
      "id": "uuid", "name": "string", "sortOrder": 0, "imageUrl": "string?",
      "products": [
        {
          "id": "uuid", "name": "string", "basePrice": 0.00,
          "imageUrl": "string?", "isAvailable": true, "productType": "SIMPLE|VARIANT|COMBO",
          "variants": [
            { "id": "uuid", "name": "string", "priceAdjustment": 0.00, "effectivePrice": 0.00, "isDefault": false, "sortOrder": 0 }
          ],
          "modifierGroups": [
            {
              "id": "uuid", "name": "string", "minSelections": 0, "maxSelections": "int?", "isRequired": false,
              "modifiers": [
                { "id": "uuid", "name": "string", "priceAdjustment": 0.00, "isDefault": false, "sortOrder": 0 }
              ]
            }
          ]
        }
      ]
    }
  ],
  "combos": [
    {
      "id": "uuid", "name": "string", "description": "string?",
      "imageUrl": "string?", "price": 0.00, "sortOrder": 0,
      "items": [{ "productId": "uuid", "productName": "string", "quantity": 1 }]
    }
  ]
}
```
**Nota:** Categorías vacías excluidas. Productos no disponibles incluidos pero marcados `isAvailable: false`.

---

## Módulo: Sucursales

### `GET /api/v1/branches` · JWT
Lista sucursales del tenant.

**Response** `200` — array de `BranchResponse`:
```json
{
  "id": "uuid", "tenantId": "uuid", "name": "string", "code": "string",
  "address": "string?", "city": "string?", "phone": "string?", "email": "string?",
  "isActive": true, "createdAt": "instant", "updatedAt": "instant"
}
```

---

### `POST /api/v1/branches` · JWT · OWNER
**Request** `{ "name": "string (max 255)", "code": "string (max 20)", "address": "string?", "city": "string?", "phone": "string?", "email": "string?" }`
**Response** `201` — `BranchResponse`

---

### `GET /api/v1/branches/{id}` · JWT
Response `200` — `BranchResponse`.

---

### `PUT /api/v1/branches/{id}` · JWT · OWNER
Actualiza sucursal. Campos opcionales: todos los de POST más `isActive: bool?`. Response `200` — `BranchResponse`.

---

### `DELETE /api/v1/branches/{id}` · JWT · OWNER
Soft-delete. Response `204`.

---

## Módulo: Áreas

### `GET /api/v1/branches/{branchId}/areas` · JWT
Lista áreas de una sucursal.

**Response** `200` — array de `AreaResponse`:
```json
{ "id": "uuid", "branchId": "uuid", "name": "string", "description": "string?", "sortOrder": 0, "isActive": true }
```

---

### `POST /api/v1/branches/{branchId}/areas` · JWT · OWNER/MANAGER
**Request** `{ "name": "string (max 100)", "description": "string?", "sortOrder": 0 }`
**Response** `201` — `AreaResponse`

---

### `GET /api/v1/areas/{id}` · JWT → `200` — `AreaResponse`

### `PUT /api/v1/areas/{id}` · JWT · OWNER/MANAGER → `200` — `AreaResponse`

### `DELETE /api/v1/areas/{id}` · JWT · OWNER/MANAGER → `204`

---

## Módulo: Mesas

### `GET /api/v1/areas/{areaId}/tables` · JWT
Lista mesas de un área.

**Response** `200` — array de `TableResponse`:
```json
{
  "id": "uuid", "areaId": "uuid", "number": "string", "name": "string?",
  "capacity": "int?", "status": "AVAILABLE|OCCUPIED|RESERVED|MAINTENANCE",
  "sortOrder": 0, "isActive": true
}
```

---

### `POST /api/v1/areas/{areaId}/tables` · JWT · OWNER/MANAGER
**Request** `{ "number": "string (max 20)", "name": "string?", "capacity": "int?", "sortOrder": 0 }`
**Response** `201` — `TableResponse`

---

### `GET /api/v1/tables/{id}` · JWT → `200` — `TableResponse`

### `PUT /api/v1/tables/{id}` · JWT · OWNER/MANAGER → `200` — `TableResponse`

### `DELETE /api/v1/tables/{id}` · JWT · OWNER/MANAGER → `204`

### `PATCH /api/v1/tables/{id}/status` · JWT · OWNER/MANAGER
**Request** `{ "status": "AVAILABLE|OCCUPIED|RESERVED|MAINTENANCE" }` → `200` — `TableResponse`

---

## Módulo: Clientes

### `GET /api/v1/customers` · JWT
Lista clientes (paginado, buscable por nombre/teléfono/email).

**Query params:** `search: string?` · `page` · `size`

**Response** `200` — `Page<CustomerResponse>`:
```json
{
  "id": "uuid", "name": "string?", "phone": "string?",
  "email": "string?", "whatsapp": "string?",
  "addressLine1": "string?", "city": "string?", "postalCode": "string?",
  "deliveryNotes": "string?",
  "totalOrders": 0, "totalSpent": 0.00, "lastOrderAt": "instant?"
}
```

---

### `POST /api/v1/customers` · JWT · CASHIER+
**Request**
```json
{
  "name": "string? (max 255)", "phone": "string? (max 20)",
  "email": "string? (email válido)", "whatsapp": "string? (max 20)",
  "addressLine1": "string?", "addressLine2": "string?",
  "city": "string?", "postalCode": "string? (max 10)",
  "deliveryNotes": "string?"
}
```
**Validación:** Al menos un campo de contacto requerido (phone, email o whatsapp).
**Response** `201` — `CustomerResponse`

---

### `GET /api/v1/customers/{id}` · JWT → `200` — `CustomerResponse`

### `PUT /api/v1/customers/{id}` · JWT · CASHIER+
Todos los campos opcionales. Misma validación de contacto que POST. Response `200` — `CustomerResponse`.

### `DELETE /api/v1/customers/{id}` · JWT · OWNER/MANAGER → `204`

---

## Módulo: Pedidos

### `POST /api/v1/orders` · JWT · CASHIER+
Crea una nueva orden en estado `PENDING`.

**Request**
```json
{
  "branchId": "uuid (required)",
  "serviceType": "COUNTER|TAKEOUT|DINE_IN|DELIVERY (required)",
  "tableId": "uuid? (required si DINE_IN)",
  "customerId": "uuid? (required si DELIVERY)",
  "notes": "string?",
  "kitchenNotes": "string?",
  "items": [
    {
      "productId": "uuid? (XOR comboId)",
      "comboId": "uuid? (XOR productId)",
      "variantId": "uuid?",
      "productName": "string (required, max 255)",
      "variantName": "string? (max 100)",
      "quantity": 1,
      "unitPrice": 0.00,
      "notes": "string?",
      "modifiers": [
        { "modifierId": "uuid?", "modifierName": "string (max 100)", "priceAdjustment": 0.00, "quantity": 1 }
      ]
    }
  ]
}
```
**Response** `201` — `OrderResponse` (ver estructura completa abajo)

---

### `OrderResponse` (estructura completa)
```json
{
  "id": "uuid", "tenantId": "uuid", "branchId": "uuid",
  "tableId": "uuid?", "customerId": "uuid?",
  "orderNumber": "string", "dailySequence": 1,
  "serviceType": "COUNTER|TAKEOUT|DINE_IN|DELIVERY",
  "statusId": "uuid",
  "subtotal": 0.00, "taxRate": 0.00, "tax": 0.00,
  "discount": 0.00, "total": 0.00,
  "notes": "string?", "kitchenNotes": "string?",
  "openedAt": "instant", "closedAt": "instant?",
  "createdBy": "uuid", "createdAt": "instant", "updatedAt": "instant",
  "items": [
    {
      "id": "uuid", "productId": "uuid?", "variantId": "uuid?", "comboId": "uuid?",
      "productName": "string", "variantName": "string?",
      "quantity": 1, "unitPrice": 0.00,
      "modifiersTotal": 0.00, "lineTotal": 0.00,
      "kdsStatus": "PENDING|SENT|READY",
      "notes": "string?", "sortOrder": 0,
      "modifiers": [
        { "id": "uuid", "modifierId": "uuid?", "modifierName": "string", "priceAdjustment": 0.00, "quantity": 1 }
      ]
    }
  ]
}
```

---

### `GET /api/v1/orders/{id}` · JWT
MANAGER+ ve cualquier orden. CASHIER solo ve las propias. Response `200` — `OrderResponse`.

---

### `GET /api/v1/orders` · JWT
**Query params:** `branchId: uuid?` · `statusId: uuid?` · `page` · `size`
**Response** `200` — `Page<OrderResponse>`. MANAGER+ ve todas, CASHIER solo las propias.

---

### `POST /api/v1/orders/{id}/items` · JWT · CASHIER+
Agrega ítem a orden en estado `PENDING`. Body: mismo `OrderItemRequest` de creación. Response `200` — `OrderResponse`.

---

### `DELETE /api/v1/orders/{orderId}/items/{itemId}` · JWT · CASHIER+
Elimina ítem de orden `PENDING`. Response `204`.

---

### `POST /api/v1/orders/{id}/submit` · JWT · CASHIER+
Transiciona `PENDING → IN_PROGRESS`. Response `200` — `OrderResponse`.

---

### `POST /api/v1/orders/{id}/ready` · JWT · CASHIER+
Transiciona `IN_PROGRESS → READY`. Response `200` — `OrderResponse`.

**Flujo frontend:**
- COUNTER/TAKEOUT: frontend llama `/submit` + `/ready` en secuencia automáticamente al "Enviar Orden"
- DINE_IN/DELIVERY: cajero marca READY manualmente desde OrderDetail

---

### `POST /api/v1/orders/{id}/cancel` · JWT · OWNER/MANAGER
Cancela la orden (cualquier estado no-terminal). Response `204`.

---

## Módulo: Pagos

### `POST /api/v1/payments` · JWT · CASHIER+
Registra pago de una orden en estado `READY`. Transiciona la orden a `COMPLETED`.

**Request**
```json
{
  "orderId": "uuid (required)",
  "paymentMethod": "CASH (único en MVP)",
  "amount": 0.00,
  "notes": "string?"
}
```
**Response** `201`
```json
{
  "id": "uuid", "orderId": "uuid",
  "amount": 0.00, "paymentMethod": "CASH",
  "amountReceived": 0.00, "changeGiven": 0.00,
  "status": "COMPLETED", "referenceNumber": "string?",
  "notes": "string?", "createdAt": "instant", "createdBy": "uuid"
}
```
**Errores:** `400 INSUFFICIENT_PAYMENT` si `amount < order.total`

---

### `GET /api/v1/orders/{orderId}/payments` · JWT · CASHIER+
Lista pagos de una orden. Response `200` — array de `PaymentResponse`.

---

## Módulo: Reportes

### `GET /api/v1/reports/daily-summary` · JWT · OWNER/MANAGER
Resumen diario de ventas de una sucursal.

**Query params:**
- `branchId: uuid (required)`
- `date: YYYY-MM-DD (optional, default: hoy en CDMX timezone)`

**Response** `200`
```json
{
  "date": "2026-03-02",
  "branchId": "uuid",
  "totalOrders": 15,
  "totalSales": 4250.00,
  "averageTicket": 283.33,
  "ordersByServiceType": {
    "COUNTER": 8, "TAKEOUT": 3, "DINE_IN": 4, "DELIVERY": 0
  },
  "topProducts": [
    { "productName": "Café Americano", "quantitySold": 12 }
  ]
}
```
**Nota:** Solo incluye órdenes en estado `COMPLETED`.

---

## Estados de Orden

| Estado | UUID seed | Descripción |
|--------|-----------|-------------|
| PENDING | `d111...` | Creada, editable |
| IN_PROGRESS | `d222...` | Enviada a cocina |
| READY | `d333...` | Lista para entregar |
| COMPLETED | `d555...` | Pagada y cerrada |
| CANCELLED | `d666...` | Cancelada |

**Estados terminales:** COMPLETED, CANCELLED (no pueden transicionar).

---

## Flujo de Orden Completo

```
POST /orders          → PENDING
POST /orders/{id}/submit   → IN_PROGRESS
POST /orders/{id}/ready    → READY
POST /payments             → COMPLETED
```
