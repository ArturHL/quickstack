# QuickStack POS - Sistema Point of Sale para Sector Restaurantero 🚀

**Lanzamiento Piloto** | **Arquitectura Serverless** | **OWASP ASVS Nivel 2**

QuickStack POS es un sistema de Punto de Venta (POS) multi-tenant diseñado específicamente para restaurantes en México. Evolucionado desde una arquitectura monolítica hacia una infraestructura nativa en la nube, el proyecto destaca por su enfoque "Security First" y su eficiencia operativa mediante prácticas FinOps.

---

## 🌟 Logros Destacados (CV)

*   **Arquitectura Serverless y Desacoplada:** Diseñé y desplegué una infraestructura completamente serverless en AWS utilizando **Terraform (IaC)**, garantizando entornos aislados, replicables y escalables.
*   **Seguridad de Grado Empresarial:** Aseguré el perímetro de la aplicación integrando **AWS WAF y Shield** para mitigar amenazas del OWASP Top 10. Todo el desarrollo backend está alineado estrictamente a los estándares de seguridad **OWASP ASVS Nivel 2**.
*   **Migración Ágil (72 Horas):** Lideré y ejecuté la evolución arquitectónica de una solución monolítica a un entorno serverless en un tiempo récord de 72 horas, culminando en un despliegue exitoso para un cliente piloto.
*   **Eficiencia FinOps:** Implementé una estrategia de visibilidad financiera mediante el **etiquetado granular** de recursos (tags por Proyecto, Entorno) y monitoreo en AWS Cost Explorer, logrando mantener los costos operativos del piloto cercanos a **cero**.

---

## 🛠️ Stack Tecnológico

*   **Frontend:** React 19, Vite, TypeScript, Material UI (MUI) 5, Zustand, TanStack Query.
*   **Backend:** Java 17, Spring Boot 3.5, Spring Data JPA, Spring Security, Maven (Multi-módulo).
*   **Base de Datos:** Amazon Aurora PostgreSQL 15.15 (Serverless v2, 0 ACU), Flyway.
*   **Infraestructura AWS:** Lambda, API Gateway, S3, CloudFront, WAF, Cognito.
*   **DevOps & IaC:** Terraform, GitHub Actions (CI/CD), Git.

---

## 🏗️ Arquitectura y Diseño

El proyecto está estructurado como un **Monorepo** que consolida el frontend y backend.

### Backend (Clean Architecture & Multi-tenant)
Construido bajo el patrón de **Package-by-Feature** en lugar de capas técnicas, dividiendo el dominio en módulos cohesivos:
*   `quickstack-auth`: Infraestructura de seguridad, JWT (RS256) y gestión de sesiones.
*   `quickstack-user`: Gestión de identidad.
*   `quickstack-pos` & `quickstack-branch`: Orquestación de clientes, órdenes, pagos, sucursales y mesas.
*   `quickstack-product`: Catálogo de productos.

**Aislamiento de Inquilinos:** La multi-tenencia se logra de forma robusta mediante la columna `tenant_id`, asegurando que cada consulta a la base de datos (Amazon Aurora) filtre estrictamente por el inquilino autenticado.

### Seguridad y Calidad (TDD)
*   **Autenticación y Cifrado:** Implementación nativa con Spring Security. Contraseñas hasheadas usando **Argon2id** con un pepper versionado.
*   **TDD y Cobertura:** Desarrollo guiado por pruebas con más de **1,060 tests en backend** y **244 tests en frontend** (0 fallos actuales).
*   **Auditoría de Datos:** Uso sistemático de *Soft Delete* (`deleted_at`) y campos de auditoría (`created_at`, `updated_at`, `created_by`, `updated_by`) en todas las entidades.
*   **Rate Limiting:** Control de tráfico basado en IP y correo electrónico utilizando Bucket4j.

---

## 🚀 Estado del Proyecto y Roadmap

Actualmente el proyecto ha completado con éxito su **Fase 1 (Core POS)**, que incluye:
- ✅ **Fase 0 (Fundación):** Autenticación, Infraestructura serverless y CI/CD.
- ✅ **Fase 1.1 - 1.3:** Base del catálogo, Modificadores, Combos, Órdenes y Pagos.
- ✅ **Fase 1.4:** Frontend POS completamente funcional (Gestión de órdenes, rutas, panel de control).

**Fase 2 (En Progreso):** Reestructuración de UX y flujo de trabajo para implementar enrutamiento basado en roles (Mesero, Cajero, Cocina, Admin) con perfiles de usuario especializados.

---

## 💻 Desarrollo Local

### Requisitos Previos
*   Java 17+
*   Node.js 20+
*   Maven Wrapper (incluido en el repositorio)

### Backend (`/backend`)
```bash
# Ejecutar pruebas y compilar
./mvnw verify

# Levantar la aplicación localmente
./mvnw spring-boot:run -pl quickstack-app
```

### Frontend (`/frontend`)
```bash
# Instalar dependencias
npm install

# Iniciar servidor de desarrollo (http://localhost:5173)
npm run dev
```
