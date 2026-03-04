# Role: WAITER (Mesero)

## Descripción General
El `WAITER` opera constantemente en movimiento alrededor del piso del restaurante. Necesita una herramienta portátil, a prueba de errores rápidos y diseñada para "pulgares" y toques grandes. 

## Responsabilidades Principales
1. **Gestión de Mesas:** Abrir, transferir, consolidar y verificar el estatus de las mesas (disponible, ocupada, esperando cobro).
2. **Toma de Múltiples Comandas:** Ingresar órdenes rápidamente, agregando modificadores complejos (ej. "Sin cebolla", "Término Medio", "Con Limón") sin fricción.
3. **Interacción con Cocina:** Presionar "Enviar a Cocina" para iniciar la preparación. Observar alertas si la orden toma mucho tiempo.
4. **Solicitar Cuenta:** Emitir el estado de cuenta y enviarlo a caja.
5. **Control Básico (Fin de Turno):** Actividades secundarias como inventariar bebidas de su estación (refrigerador) al final de la jornada.

## Flujo de Trabajo Ideal (UX Proposal)
- **Punto de Entrada:** `/waiter/tables`
- **Dispositivo:** Teléfono Móvil o Mini-Tablet (Preferiblemente Formato Vertical / Portrait).
- **Interfaz:** *Mobile-First*. 
  - Todo se controla por una barra de navegación inferior (Bottom Nav) accesible con el pulgar.
  - El botón primario flotante o en el borde inferior es siempre "Enviar Orden".
  - El menú de productos usa tarjetas grandes, y el flujo de selecciones (Modifiers) ocurre en un modelo de pasos o Modales de pantalla completa (Drawers que suben) para evitar salir del orden.
