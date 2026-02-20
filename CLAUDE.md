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

- **1.1**: Catálogo Base (3 sem) — Categorías + Productos + Variantes | 22 endpoints | ~250 tests (Sprint 1/6 completado)
- **1.2**: Modifiers + Combos (2 sem) — Personalización avanzada | 12 endpoints | ~80 tests
- **1.3**: Pedidos + Pagos (3 sem) — Sistema completo de órdenes | 28 endpoints | ~150 tests
- **1.4**: Frontend POS (3 sem) — UI completa del punto de venta | 15 pantallas | ~120 tests

**Próximo sprint**: Phase 1.1 Sprint 2 — Category Management

Ver: `docs/ROADMAP.md` | `docs/roadmap/PHASE_1.1_BACKEND_CATALOG.md` | `.context/completed-sprints.md`

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
