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

**Phase 0 (Foundation)** | **✅ COMPLETADA**

**Phase 1 (Core POS)** | **✅ COMPLETADA** — Backend: ~1,060 tests | Frontend: 319 tests
- 1.1–1.3: Backend completo (catálogo, pedidos, pagos, reportes) — 28 endpoints
- 1.4: Frontend POS base — 244 tests
- 1.5: Estabilización Admin (categorías, variantes, modificadores, combos, reportes) — 75 tests nuevos

**Phase 2 (UX Roles + Piloto)** | **✅ COMPLETADA** — 2026-03-08
- CashierPos split-screen funcional en /cashier/pos (COUNTER + TAKEOUT)
- User Management CRUD completo en /admin/users (OWNER only)
- Admin dual-view: BranchSelector + Sidebar context-aware (Owner Global / Manager)
- UserManagementE2ETest — 8 tests de integración con DB real

**Phase 3 (Owner Intelligence)** | **⏳ EN PROGRESO** — Sprint 1 ✅ 2026-03-08
- Sprint 1 ✅: Módulo `quickstack-inventory` — 5 entidades + migración V9 + 57 tests
- Sprint 2 ⏳: CRUD Ingredientes API
- Ver: `docs/roadmap/PHASE_3_OWNER_INTELLIGENCE.md` (10 sprints)
- Entregables: Inventario por receta + auto-descuento, Gastos, Lista de compras, P&L

**Horizonte Piloto**: ✅ LANZADO — ADMIN + CASHIER funcionales
**Horizonte MVP (6 meses)**: Inventario + Gastos + P&L + WAITER + KDS + Bot WhatsApp

Ver: `docs/ROADMAP.md` | `docs/ARCHITECTURE.md`

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
