# Role: KITCHEN (Cocina)

## Descripción General
El `KITCHEN` o personal de cocina interactúa con el sistema exclusivamente a través del **Kitchen Display System (KDS)**. Su objetivo es preparar las comandas en el orden y tiempo correcto, informando al piso cuando las órdenes están físicas y listas para ser entregadas.

## Responsabilidades Principales
1. **Recepción de Comandas:** Visualizar de forma clara las órdenes entrantes, agrupadas por mesa o folio, incluyendo las alertas de tiempo ("Ticket Nuevo", "Ticket Demorado").
2. **Interpretación de Modificadores:** Leer rápidamente las excepciones importantes de cada platillo ("Sin Queso", "Para Llevar") resaltadas visualmente.
3. **Gestión de Estados:** Avanzar los items o tickets completos de estadio: 
   - *Recibido* -> *Preparando* -> *Listo*.
4. **Notificación de Término:** Al marcar un ticket como "Listo", disparar el cambio de estado para que el cajero o mesero sepa que debe ir a recogerlo.

## Flujo de Trabajo Ideal (UX Proposal)
- **Punto de Entrada:** `/kitchen/board`
- **Dispositivo:** Pantalla grande (Touchscreen, Tablet montada, o monitor con teclado "Bump Bar").
- **Interfaz:** *Dark Mode Kanban*. 
  - Todo el tablero tiene un fondo muy oscuro (Alto contraste) para evitar fatiga visual en un cuarto iluminado/con humo y para destacar los tickets blancos/amarillos/rojos.
  - Diseño estilo Kanban o Grid de tickets enormes (Comandas digitales).
  - Al hacer touch sobre un ticket (o un botón enorme debajo de él), este desaparece del grid (marcado como listo) y pasa a un historial oculto.
  - Alerta sonora o flash en pantalla cuando entra un ticket nuevo.
