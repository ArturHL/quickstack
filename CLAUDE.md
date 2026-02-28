# QuickStack POS

Sistema POS SaaS multi-tenant para restaurantes en México. MVP en 6 meses.

## Stack

| Capa | Tecnología |
|------|------------|
| Frontend | React 19 + Vite + TS + MUI (Vercel) |
| Backend | Java 17 + Spring Boot 3.5 (Render) |
| DB | PostgreSQL 16 (Neon serverless) |
| Auth | Spring Security + JWT (ASVS L2) |

## Arquitectura

- **Monorepo**: `/frontend` (React) + `/backend` (multi-module Maven)
- **Multi-tenancy**: `tenant_id` en todas las tablas
- **Principios**: TDD obligatorio | GitHub Flow | Package by feature | **Definition of Ready**: User Stories deben incluir Restricciones de Seguridad (ASVS 1.1.3)

## Estado Actual

**Phase 0 (Foundation)** | **✅ 100% COMPLETADA**

- **0.1-0.2**: Infraestructura, CI/CD, Deploy (Render + Vercel + Neon)
- **0.3**: Auth Backend — 340 tests | 8 endpoints | ASVS L2: V2 26%, V3 74%, V6 56%
- **0.4**: Auth Frontend — 38 tests | Auth flow completo | Dashboard + Layout

**Phase 1 (Core POS)** | **⏳ EN PROGRESO** — 4 sub-fases (11 semanas)

- **1.1**: Catálogo Base — Categorías + Productos + Variantes + Menú POS | ✅ COMPLETADA (6/6 sprints) | 20 endpoints | ~650 tests backend
- **1.2**: Modifiers + Combos (2 sem) — Personalización avanzada | ✅ COMPLETADA (4/4 sprints) | 15 endpoints (9 modifiers + 5 combos + 1 menu actualizado) | ~183 tests nuevos
- **1.3**: Pedidos + Pagos (3 sem) — Sistema completo de órdenes | ✅ COMPLETADA (6/6 sprints) | Sprint 1: Branch/Area/Table ✅ | Sprint 2: Customer ✅ | Sprint 3: Order Core ✅ | Sprint 4: Order Management API ✅ | Sprint 5: Payments ✅ | Sprint 6: Reporting ✅ | 28 endpoints | ~1,060 tests total
- **1.4**: Frontend POS (3 sem) — UI completa del punto de venta | ✅ COMPLETADA (6/6 sprints) | 244 tests frontend
  - Sprint 1 ✅: Catálogo (ProductCard, ProductCatalog, /pos/catalog)
  - Sprint 2 ✅: ProductDetail + CartStore (VariantSelector, ModifierGroup, ProductDetailModal)
  - Sprint 3 ✅: Carrito + Flujo Servicio (CartItem, Cart, ServiceTypeSelector, TableSelector, CustomerSelector, rutas /pos/new /pos/cart /pos/new/table /pos/new/customer)
  - Sprint 4 ✅: Order Creation + Payment (Tarea B.1 backend ✅, Order.ts, orderApi, mutations, orderUtils, PaymentForm, PaymentPage, OrderConfirmationPage, posStore, /pos/payment /pos/confirmation)
  - Sprint 5 ✅: Admin CRUD (ProductList, ProductForm, BranchList, BranchForm, BranchSelector, AreaList, AreaForm, TableList, TableForm, CustomerList, RoleProtectedRoute, adminRoutes, Sidebar admin section)
  - Sprint 6 ✅: Order Management (OrderList, OrderDetail, orderRoutes, useOrdersQuery, useCancelOrderMutation, useMarkReadyMutation, Sidebar pedidos link)

**Phase 1 COMPLETADA** — Backend: ~1,060 tests | Frontend: 244 tests

Ver: `docs/ROADMAP.md` | `docs/roadmap/PHASE_1.4_FRONTEND_POS.md`

## Comandos

```bash
cd backend && ./mvnw verify    # Build + tests
cd frontend && npm run dev     # Dev server
```

## Notas

- Java 17 + Node.js 20+ requeridos
- OWASP ASVS L2 para seguridad
- Soft delete en mayoría de entidades
- Orders/payments nunca se borran (auditoría)
