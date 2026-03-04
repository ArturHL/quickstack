# Role: MANAGER (Gerente)

## Descripción General
El `MANAGER` es el administrador operativo de **una sucursal específica**. Su enfoque es asegurar que el restaurante funcione sin problemas durante el día a día, controlando el catálogo, supervisando el personal y resolviendo bloqueos operativos.

## Responsabilidades Principales
1. **Administración del Catálogo:** Crear, editar y ocultar Productos, Categorías, Modificadores y Combos (muy importante para mantener el menú actualizado basado en el inventario o la temporada).
2. **Supervisión de Piso e Inventario:** Administrar las Mesas (crear, juntar, dividir zonas) e inventarios locales.
3. **Gestión de Personal de Sucursal:** Dar de alta a Cajeros, Meseros y personal de Cocina.
4. **Reportes Operativos:** Generar reportes locales de ventas (Cierre de día, cortes Z) y resolver reembolsos o cancelaciones críticas que el cajero no pueda hacer.

## Flujo de Trabajo Ideal (UX Proposal)
- **Punto de Entrada:** `/admin/dashboard` (limitado al contexto de su sucursal).
- **Dispositivo:** Tablet grande o Desktop de la oficina del restaurante.
- **Interfaz:** Similar al OWNER, pero con herramientas rápidas para **modificar el menú** ("Marcar producto como Agotado") resaltadas como acciones de emergencia (Quick Actions). No necesita acceso a configuración multi-sucursal.
