# GEMINI.md - QuickStack POS

## Project Overview
QuickStack POS is a multi-tenant SaaS Point of Sale (POS) system specifically designed for restaurants in Mexico. It is built as a monorepo containing a high-performance Java backend and a modern React frontend. The project follows a "Security First" approach, adhering to OWASP ASVS Level 2 standards, and emphasizes Test-Driven Development (TDD).

### Core Technologies
- **Backend:** Java 17, Spring Boot 3.5, Spring Data JPA, Spring Security, Maven (Multi-module).
- **Frontend:** React 19, Vite, TypeScript, Material UI (MUI) 5, Zustand (State Management), TanStack Query (Server State).
- **Database:** PostgreSQL 16 (Neon Serverless), Flyway (Migrations).
- **Infrastructure:** Vercel (Frontend), Render (Backend), GitHub Actions (CI/CD).
- **Architecture:** Monorepo, Multi-tenancy via `tenant_id` column, Package-by-feature.

---

## Building and Running

### Root Directory
The project is managed as a monorepo. Key commands should be run within their respective subdirectories.

### Backend (`/backend`)
- **Build & Test:** `./mvnw verify`
- **Run Application:** `./mvnw spring-boot:run -pl quickstack-app`
- **Run Specific Module Tests:** `./mvnw test -pl <module-name>` (e.g., `quickstack-user`)
- **Requirements:** Java 17+, Maven Wrapper (included).

### Frontend (`/frontend`)
- **Install Dependencies:** `npm install`
- **Start Dev Server:** `npm run dev` (Runs on `http://localhost:5173`)
- **Production Build:** `npm run build`
- **Run Tests:** `npm run test`
- **Linting:** `npm run lint`
- **Type Checking:** `npm run type-check`
- **Requirements:** Node.js 20+.

---

## Development Conventions

### Coding Standards
- **TDD Mandatory:** All new features must be accompanied by unit and/or integration tests.
- **Package by Feature:** Group code by business logic (e.g., `user`, `product`, `tenant`) rather than technical layers (e.g., `controllers`, `services`).
- **Soft Delete:** Most entities use a `deleted_at` timestamp instead of hard deletion to maintain audit trails (except for transient data like auth tokens).
- **Auditing:** All tables include `created_at`, `updated_at`, `created_by`, and `updated_by` fields.

### Security (OWASP ASVS L2)
- **Authentication:** Native implementation using Spring Security + JWT (RS256).
- **Password Hashing:** Argon2id with versioned pepper.
- **Multi-tenancy:** Strict isolation using `tenant_id`. Every query must filter by the current tenant.
- **Rate Limiting:** Implemented via Bucket4j (IP and Email based).

### Git Workflow (GitHub Flow)
- `main` is always deployable.
- Create feature branches from `main`: `feature/QS-XXX-description`.
- Pull Requests are mandatory and must pass CI (Build + Test + SAST) before merging.
- Use Squash Merge for a clean history.

---

## Key Directory Structure
- `/backend`: Multi-module Maven project.
  - `quickstack-app`: Main entry point and orchestration.
  - `quickstack-auth`: Security infrastructure, JWT, and session management.
  - `quickstack-user`: Identity management and user CRUD.
  - `quickstack-product`: Catalog and product management.
  - `quickstack-common`: Shared utilities, security base, and exceptions.
- `/frontend`: React application using Vite.
  - `src/features`: Business logic modules (auth, dashboard, pos).
  - `src/components`: Reusable UI components.
  - `src/stores`: Zustand global state definitions.
- `/docs`: Extensive documentation including ARCHITECTURE.md, ROADMAP.md, and security guidelines.
- `CLAUDE.md`: High-level project status and quick reference commands.

---

## Project Status
Currently in **Phase 1 (Core POS)**.
- **Phase 0 (Foundation):** 100% Completed (Auth, Infrastructure, CI/CD).
- **Phase 1.1 (Catalog Base):** In Progress (Sprint 5/6: Security Hardening & Ordering completed).
