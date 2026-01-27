# QuickStack POS - Contexto del Proyecto

> Este archivo contiene el contexto necesario para continuar el desarrollo con Claude Code.

## Resumen del Proyecto

**QuickStack POS** es un sistema de punto de venta SaaS multi-tenant para restaurantes en México. El objetivo es democratizar tecnología avanzada (predicciones, automatizaciones con IA) para pequeños empresarios.

## Stack Tecnológico

| Componente | Tecnología | Hosting |
|------------|------------|---------|
| Frontend | React 18 + Vite + TypeScript + MUI | Vercel |
| Backend | Java 21 + Spring Boot 3.5 | Render (Docker) |
| Base de datos | PostgreSQL | Neon (serverless) |
| Autenticación | Auth0 (OWASP ASVS L1) | - |
| State Management | Zustand | - |
| HTTP Client | TanStack Query + Axios | - |
| ORM | Spring Data JPA | - |
| Automatizaciones | n8n | Self-hosted o Cloud |

## Decisiones de Arquitectura

- **Monorepo**: Frontend y backend en el mismo repositorio
- **Multi-module Maven**: Backend modular con dependencias independientes por módulo
- **Multi-tenancy**: BD compartida con `tenant_id` en todas las tablas
- **Package by feature**: Cada módulo contiene su controller/service/repository
- **TDD**: Test-Driven Development completo
- **GitHub Flow**: main + feature branches con PRs

## Estructura del Proyecto

```
quickstack/
├── docs/
│   ├── ARCHITECTURE.md    # Decisiones técnicas detalladas
│   └── ROADMAP.md         # Plan de fases del MVP
├── frontend/              # React + Vite + TypeScript
│   └── src/
├── backend/               # Multi-module Maven
│   ├── pom.xml           # Parent POM
│   ├── quickstack-common/ # Utilidades compartidas
│   ├── quickstack-tenant/ # Módulo tenants
│   ├── quickstack-branch/ # Módulo sucursales
│   ├── quickstack-user/   # Módulo usuarios
│   ├── quickstack-product/# Módulo productos
│   ├── quickstack-pos/    # Módulo punto de venta
│   └── quickstack-app/    # Ensamblador (Spring Boot main)
└── .claude/
    └── agents/            # Agentes personalizados
```

## Fases del MVP

| Fase | Nombre | Estado |
|------|--------|--------|
| 0 | Foundation & Architecture | 🔄 En progreso |
| 1 | Core POS - Ventas Básicas | ⏳ Pendiente |
| 2 | Modifiers & Combos | ⏳ Pendiente |
| 3 | Printing | ⏳ Pendiente |
| 4 | Basic Reporting | ⏳ Pendiente |
| 5 | WhatsApp Bot with AI | ⏳ Pendiente |
| 6 | Polish & Pilot Validation | ⏳ Pendiente |

## Estado Actual (Phase 0)

### Completado
- [x] Definición de arquitectura y stack
- [x] Creación de estructura monorepo
- [x] Inicialización de frontend (React + Vite)
- [x] Estructura multi-module Maven para backend
- [x] Parent POM creado
- [x] Documentación (ARCHITECTURE.md, ROADMAP.md)
- [x] Configuración de Git y GitHub

### Pendiente Phase 0
- [ ] Crear `pom.xml` de cada módulo del backend
- [ ] Diseñar modelo de datos (esquema de BD)
- [ ] Configurar Auth0
- [ ] Configurar CI/CD (GitHub Actions)
- [ ] Estructura de carpetas del frontend
- [ ] Configurar variables de entorno

## Funcionalidades del MVP

1. **Crear pedidos/ventas** - productos simples, modificadores y combos
2. **Bot WhatsApp con IA** - lenguaje natural, pedidos integrados al POS
3. **Multi-sucursal** - un dueño con varios locales
4. **Reportes básicos** - ventas día/semana/mes
5. **Impresión de tickets** - impresora térmica
6. **Solo efectivo** (sin pagos con tarjeta en MVP)

## Validación

- Restaurante piloto disponible para testing
- Región: México
- Objetivo: Piloto acepta pagar ≥$500 MXN/mes

## Agentes Disponibles

Los siguientes agentes están configurados en `.claude/agents/`:

- `senior-software-architect` - Diseño de arquitectura
- `tech-product-manager` - Roadmaps y priorización
- `backend-architect` - Implementación backend
- `frontend-architect` - Implementación frontend
- `security-architect` - Revisión de seguridad
- `devops-automation-engineer` - CI/CD y Docker
- `tech-code-reviewer` - Code review
- `qa-engineer-preventivo` - Testing y QA

## Comandos Útiles

```bash
# Backend - compilar
cd backend && ./mvnw clean compile

# Backend - tests
cd backend && ./mvnw test

# Frontend - instalar dependencias
cd frontend && npm install

# Frontend - desarrollo
cd frontend && npm run dev
```

## Notas Importantes

- Java 21 requerido (usar SDKMAN: `sdk install java 21.0.5-tem`)
- Node.js requerido para frontend
- Seguir OWASP ASVS L1 para seguridad
- TDD obligatorio para lógica de negocio
