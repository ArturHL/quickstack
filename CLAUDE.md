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
- **Principios**: TDD obligatorio | GitHub Flow | Package by feature

## Estado Actual

**Phase 0.4** - Frontend Base + Integración Auth | **✅ COMPLETADA** (4/4 sprints)

38 tests frontend | Auth flow completo | Dashboard + Layout | Error handling global

**Phase 0.3 COMPLETADA**: 340 tests backend | 8 endpoints | ASVS L2: V2 26%, V3 74%, V6 56%

**Próximo**: Phase 1 - Core POS (Catálogo de Productos + Pedidos)

Ver: `.context/completed-sprints.md` | `docs/roadmap/PHASE_0.4_FRONTEND_BASE_ROADMAP.md`

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
