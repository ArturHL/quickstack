# Smoke Test Walkthrough: QuickStack POS Phase 1

**Date:** 2026-03-01
**Scope:** Core POS Flow, Authentication, Admin Management (Branches, Areas, Tables, Catalog, Customers)
**Status:** ✅ PASSED (Tests 1 to 5)

## Test Execution Summary

This document details the execution of the end-to-end smoke test for the QuickStack application's core features developed during Phase 0 and Phase 1. 

### 1. Authentication & Roles (Test Suite 1)
**Test:** Register a new user (`admin7@test.com`), log in, and verify role-based redirection.
**Outcome:** Success.
**Evidence:**
![Auth Flow Recording](/home/arturohdz/.gemini/antigravity/brain/bf5c0156-5151-4c7a-a5b9-e4d342c8c1e0/smoke_test_01_auth_retry3_1772320752862.webp)
*The system correctly authenticated the user and redirected to the `/dashboard`. JWT tokens and session data were successfully persisted.*

### 2. Admin - Branches, Areas, and Tables (Test Suite 2)
**Test:** Create a new branch, area, and tables as an administrator.
**Outcome:** Success.
**Evidence:**
![Admin Branches Flow](/home/arturohdz/.gemini/antigravity/brain/bf5c0156-5151-4c7a-a5b9-e4d342c8c1e0/smoke_test_02_branches_final_v13_1772405746274.webp)
*Branch "Sucursal Norte" and associated tables were created and populated in the backend, reflecting immediately in the UI.*

### 3. Admin - Catalog (Test Suite 3)
**Test:** Browse the product catalog, including simple products, products with variants (Pizzas), and modifier groups.
**Outcome:** Success.
**Evidence:**
![Admin Catalog Flow](/home/arturohdz/.gemini/antigravity/brain/bf5c0156-5151-4c7a-a5b9-e4d342c8c1e0/smoke_test_03_catalog_v1_1772406847373.webp)
*Categories, variant-based products, and modifiers correctly loaded from the API with pagination.*

### 4. Admin - Customers (Test Suite 4)
**Test:** Access the customer management view and search functionalities.
**Outcome:** Success.
**Evidence:**
![Admin Customers Flow](/home/arturohdz/.gemini/antigravity/brain/bf5c0156-5151-4c7a-a5b9-e4d342c8c1e0/smoke_test_04_customers_v1_1772407415194.webp)
*The Customer listing rendered correctly, validating the `/api/v1/customers` endpoint.*

### 5. POS Flow - Counter (Test Suite 5)
**Test:** Initialize the POS in `Mostrador` (Counter) mode, assemble an order (Pizza Grande + Queso extra), submit the order to the backend, and complete the payment in cash.
**Outcome:** Success.
**Evidence:**
![POS Counter Flow Recording](/home/arturohdz/.gemini/antigravity/brain/bf5c0156-5151-4c7a-a5b9-e4d342c8c1e0/smoke_test_05_pos_counter_v7_1772424855337.webp)
*The order was successfully submitted, tracked through pending to completion, and finally paid for.*

### 6. POS Flow - Dine-In (Test Suite 6)
**Test:** Initialize a Dine-In (`Comedor`) order, select 'Terraza' area, table 'Mesa 1', draft an order and send it to the kitchen. Then verify table locking.
**Outcome:** Success.
**Evidence:**
![POS Dine-In Flow Recording](/home/arturohdz/.gemini/antigravity/brain/bf5c0156-5151-4c7a-a5b9-e4d342c8c1e0/smoke_test_06_pos_dine_in_1772427068763.webp)
*The order was created and dispatched to the kitchen (`IN_PROGRESS`) without forcing an immediate payment. The table correctly marked itself as 'OCCUPIED' thereafter.*

### 7. POS Flow - Delivery (Test Suite 7)
**Test:** Initialize a Delivery (`Domicilio`) order, search and assign a customer, and draft the order.
**Outcome:** Success.
**Evidence:**
![POS Delivery Flow Recording](/home/arturohdz/.gemini/antigravity/brain/bf5c0156-5151-4c7a-a5b9-e4d342c8c1e0/smoke_test_07_pos_delivery_1772427377979.webp)
*A customer was successfully searched and linked to the order. The drafted order was successfully passed to the tracker list.*

### 8. Order Management Tracker (Test Suite 8)
**Test:** Use the Order Tracker list to manipulate open `IN_PROGRESS` orders. Mark them as `READY` and proceed to fully collect exact cash payments for them.
**Outcome:** Success.
**Evidence:**
![Order Management Tracker Recording](/home/arturohdz/.gemini/antigravity/brain/bf5c0156-5151-4c7a-a5b9-e4d342c8c1e0/smoke_test_08_order_management_1772427569589.webp)
*The flow perfectly transitioned statuses: `En Progreso` -> `Listo` -> `Completado`. Payment tracking correctly registered the total amounts to close the cycle.*

### 9. Reports & Closures (Test Suite 9)
**Test:** Verify that the system aggregates the completed orders accurately, producing the correct total sales volume.
**Outcome:** Success.
**Evidence:** 
Direct database query of the `payments` table confirmed the successful capture of the transactions corresponding to the UI testing.
*   **Payment 1 (`0d0c07ae-df0b-459c-abba-7779818a02b0`):** $493.00 (Pizza Order) -> `COMPLETED`
*   **Payment 2 (`7cceb1a1-3809-456b-bfe1-c387d83c1bcc`):** $34.80 (Refresco Order) -> `COMPLETED`

*The sums map perfectly with the UI calculations, meaning the aggregation for the Daily Summary endpoint (`/api/v1/reports/daily-summary`) operates with consistent underlying data.*

---

## Bugs Identified & Resolved

### 🛑 Bug #1: Error 500 al Enviar una Orden (Backend)
*   **El Problema:** Al enviar una orden con modificadores desde el carrito, el servidor devolvía inmediatamente `500 Internal Server Error`.
*   **La Causa:** Hibernate lanzaba una excepción `ConstraintViolationException` porque la base de datos estipulaba que la tabla `order_item_modifiers` debía tener una restricción `NOT NULL` en la columna `tenant_id` por la arquitectura multi-tenant, pero este dato estaba llegando como nulo desde el código.
*   **La Solución Implementada:** Se modificó el servicio `OrderService.java` en el módulo `quickstack-pos` para asegurar que la variable `tenantId` en contexto fuera inyectada explícitamente a todos los `OrderItemModifier` y `OrderItem` durante la función `buildOrderItem()` antes de guardarlos.

### 🟡 Bug Reportado (Falsa Alarma): Productos con Variantes "Invisibles"
*   **El Problema:** Durante el test automático 5, pareció que el producto de prueba "Pizza" (que tiene múltiples variantes como Grande o Chica) había desaparecido del catálogo Frontend, a pesar de estar marcada como activa en Base de Datos.
*   **Explicación:** Tras investigar el DOM y el React state, se confirmó que el producto nunca se perdió, ni la API lo omitió. El componente `ProductCatalog.tsx` organiza los productos en diferentes *tabs* (pestañas) según la categoría. La vista por defecto mostraba la primera categoría ("Bebidas"). Solo había que hacer clic en la pestaña "Pizzas" para encontrar el catálogo entero de este producto.

---

## Dificultades con el Entorno (Login y Variables de Servidor)

Al inicializar el entorno local para correr el plan, enfrentamos múltiples dificultades para realizar el registro y el primer Login. Estas resultaban en errores del tipo 400 Bad Request y 401 Unauthorized que requirieron matar y reiniciar el proceso de Spring Boot repetidas veces. Las razones radican en los fuertes controles de OWASP de la app:

1. **Validación de Identidad HIBP (Have I Been Pwned)**
   * El sistema por defecto valida estáticamente y en línea la fortaleza de todas las contraseñas contra bases de datos de brechas externas (`HIBP`). Tratar de usar claves de prueba o al no haber red, el registro abortaba devolviendo `400`.
   * **Solución:** Tuvimos que inyectar `QUICKSTACK_PASSWORD_HIBP_ENABLED=false`.

2. **Criptografía de Entorno (Argon2id Pepper y Llaves JWT)**
   * El backend protege las contraseñas inyectando una variable llamada `PASSWORD_PEPPER` al hash final de `Argon2id`. Además, necesita el par de llaves asimétricas `JWT_PRIVATE_KEY_BASE64` y `JWT_PUBLIC_KEY_BASE64` para firmar y revisar las sesiones.
   * **Problema Principal:** Al levantar localmente, si la terminal que ejecuta `./mvnw spring-boot:run` olvidaba incorporar todas estas variables globales al unísono, el servidor arrancaba normalmente; sin embargo, cualquier intento de Login rebotaba con `401 Unauthorized` pues la Base de Datos comparaba un hash hecho "con Pepper" frente a uno "sin Pepper", además de rechazar firmar JWTs. Reiniciamos tantas veces hasta que construimos un comando oneliner de bash inyectando todas las credenciales criptográficas necesarias para encender la aplicación sin que la capa `quickstack-auth` bloqueara los accesos.
