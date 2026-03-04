# Role: OWNER (Dueño)

> **Estado:** ✅ MVP — Esta perspectiva se implementa en la versión inicial. Comparte rol de sistema `ADMIN` con MANAGER. Son dos *vistas* dentro de la misma cuenta, no dos cuentas separadas. En Phase 3+, si el negocio lo requiere, se pueden separar en roles distintos con ACL diferenciada.

## Descripción General
El perfil `OWNER` tiene acceso absoluto a todo en el sistema QuickStack POS. Es el administrador maestro del Tenant (franquicia o marca) y su enfoque principal es la rentabilidad, analíticas globales y la configuración estructural del negocio.

## Responsabilidades Principales
1. **Gestión Multi-Sucursal:** Es el único rol con capacidad para crear, editar o dar de baja sucursales (`Branches`).
2. **Analíticas y Reportes Globales:** Visualizar el rendimiento financiero de todas las sucursales, comparar métricas (Ticket Promedio, Ventas Totales) y tomar decisiones de negocio.
3. **Gestión de Personal Avanzada:** Crear y asignar cuentas de `MANAGER` para delegar operaciones en sucursales específicas.
4. **Configuración de la Suscripción (Tenant):** Manejar los cobros y detalles técnicos de la suscripción al SaaS.

## Flujo de Trabajo Ideal (UX Proposal)
- **Punto de Entrada:** `/admin/dashboard`
- **Dispositivo:** Desktop / Laptop (Pantalla completa).
- **Interfaz:** Un Dashboard clásico tipo SaaS (Sidebar fuerte a la izquierda, gráficas principales al centro). En lugar de enfocarse en operaciones del día a día (órdenes), se enfoca en **métricas y configuración**. Debería contar con un "Selector Global de Sucursales" siempre visible para cambiar de contexto rápidamente.
