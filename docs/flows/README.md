# QuickStack POS - Diagramas de Flujo de Casos de Uso

Este directorio contiene la documentación técnica arquitectónica a nivel de archivo para los principales casos de uso de la aplicación, modelados utilizando diagramas de secuencia interactivos de Mermaid.js.

Estos diagramas exhiben el viaje de la información a través de las distintas capas tecnológicas (React UI, Axios, Controladores, Servicios, Repositorios, PostgreSQL DB, Proveedores Externos, WebSockets, etc.).

## Índice de Documentos

1. [Autenticación y Autorización (Login, JWT Refresh, RBAC)](./AUTH_FLOWS.md)
2. [Punto de Venta y Kitchen Display (WebSockets, Creación de Órdenes)](./POS_KDS_FLOWS.md)
3. [Aislamiento de Datos por Tenant y Resoluciones de Arquitectura](./TENANT_FLOWS.md)
4. [Notificaciones Externas, Eventos Background y Tickets Digitales](./NOTIFICATION_FLOWS.md)
5. [Catálogo e Inventario (REST, Relaciones de Categorías y Productos)](./CATALOG_INVENTORY_FLOWS.md)

---

### Cómo Leer estos Diagramas

- **Actor/View/U**: Representa la interacción humana o de interfaz (React).
- **API/Store**: Representa el State Management (Zustand/TanStack Query) y clientes HTTP Axios.
- **Ctrl/Svc/Repo/DB**: Componentes nativos de Spring Boot (Capa de presentacón, negocio, persistencia) y capa de base de datos final (PostgreSQL en Neon).
- **Arrows (`->>`)**: Llamadas sincrónicas funcionales directas u operativas HTTP/SQL.
- **Arrows punteadas (`-->>`)**: Respuestas de funciones, retornos HTTP, y data set proveniente de DB.
