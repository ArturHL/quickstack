# Role: PRODUCTION (Producción / Prep)

> **Estado:** ⏳ Post-MVP — Este rol no se implementará en la versión inicial del producto. Requiere un módulo de inventario/insumos que aún no existe en el backend. Se contempla para una fase de madurez del producto posterior al MVP. Los datos de diseño UX aquí documentados sirven como referencia para planificación futura.

## Descripción General
El rol de `PRODUCTION` abarca al personal que no cocina directamente los pedidos de los clientes en tiempo real, sino que se encarga de las preparaciones base, "batch cooking" o mis-en-place (ej. cocer carne, picar verdura, hacer litros de salsas, hornear pan).

## Responsabilidades Principales
1. **Gestión de Tareas de Preparación (Prep List):** Visualizar la lista de tareas de producción del día o turno basada en las proyecciones o mínimos de inventario.
2. **Registro de Lotes (Batch Tracking):** Registrar cuándo se terminó de preparar un lote de insumos (ej. "20 Litros de Salsa Verde Listos").
3. **Etiquetado y Caducidad:** (Opcional/Avanzado) Imprimir etiquetas con fechas de preparación y caducidad para los contenedores.
4. **Manejo de Mermas/Desperdicio durante la preparación:** Registrar si algún insumo primario se echó a perder o hubo un error en la receta que cause pérdida.

## Flujo de Trabajo Ideal (UX Proposal)
- **Punto de Entrada:** `/production/tasks`
- **Dispositivo:** Tablet resistente (ruged) o pantalla montada en el área de preparación trasera.
- **Interfaz:** *Task List / Inventory Mode*.
  - Menos urgencia y alarmas que el KDS de Cocina. Es un trabajo planificado.
  - Interfaz orientada a tareas largas. Una lista de "Prep del Día" (ej. "Desmenuzar 10kg de Pollo").
  - Botones amplios para marcar el progreso (0%, 50%, 100%) o ingresar la cantidad métrica final producida.
  - Integración directa con las recetas base del catálogo de inventarios.
