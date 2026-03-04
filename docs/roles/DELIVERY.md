# Role: DELIVERY (Repartidor)

> **Estado:** ⏳ Post-MVP — Este rol no se implementará en la versión inicial del producto. Se contempla para una fase posterior una vez que el flujo core de POS (CASHIER + WAITER + KITCHEN) esté estabilizado en producción. Los datos de diseño UX aquí documentados sirven como referencia para planificación futura.

## Descripción General
El `DELIVERY` o Repartidor es el encargado de llevar los pedidos desde el restaurante hasta el cliente final. Su interacción con el sistema debe ser mínima, precisa y enfocada 100% en la logística de entrega.

## Responsabilidades Principales
1. **Asignación de Viajes:** Aceptar pedidos que ya están listos y empaquetados para entrega.
2. **Navegación y Logística:** Ver la información del cliente (Dirección, Teléfono, Instrucciones especiales de entrega) de forma clara.
3. **Gestión de Estados de Entrega:** Marcar pedidos como "En Camino" y "Entregado" (o "Fallido" con motivo).
4. **Liquidación:** Llevar un registro del efectivo cobrado (si el pedido era pago contra entrega) para liquidarlo con el `CASHIER` al final del turno.

## Flujo de Trabajo Ideal (UX Proposal)
- **Punto de Entrada:** `/delivery/trips`
- **Dispositivo:** Teléfono Móvil (Modo vertical).
- **Interfaz:** *App-lik / Mobile-First*.
  - Vista principal con la lista de "Pedidos Listos para Llevar".
  - Pantalla de detalle del viaje con mapa de ruta integrado o botones grandes que abran Google Maps/Waze.
  - Botón gigante tipo *Swipe* (deslizar para confirmar) para evitar toques accidentales al manejar o caminar: "Desliza para marcar como Entregado".
