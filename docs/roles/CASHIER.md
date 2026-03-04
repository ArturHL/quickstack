# Role: CASHIER (Cajero)

## Descripción General
El `CASHIER` es la cara transaccional del restaurante. Trabaja bajo presión en mostrador y su máxima prioridad es la velocidad y precisión al procesar pagos, abrir órdenes a mostrador y despachar a clientes de entrega (To-Go).

## Responsabilidades Principales
1. **Toma de Pedidos (Mostrador):** Capturar órdenes de forma ultrarrápida (posibilidad de usar teclado o lector de código de barras).
2. **Procesamiento de Pagos:** Cobrar órdenes (Efectivo, Tarjeta, Mixto), aplicar descuentos, propinas y emitir tickets/facturas.
3. **Control de Caja:** Realizar aperturas, retiros parciales (sangrías) y el Corte de Caja final (Turno).
4. **Despacho Externo:** Gestionar pedidos para llevar o recolección.
5. **Administración de Clientes:** Crear y buscar perfiles de clientes para facturación o puntos.

## Flujo de Trabajo Ideal (UX Proposal)
- **Punto de Entrada:** `/cashier/pos`
- **Dispositivo:** Terminal TPV (Desktop o Tablet con base física). Pantalla Horizontal.
- **Interfaz:** Pantalla dividida persistente. 
  - *Izquierda:* El "Comanda Edge" siempre visible con el detalle de la cuenta activa (Totales, Impuestos).
  - *Derecha:* Un catálogo de botones grandes para agregar rápido o un teclado numérico táctil masivo (Cash Register) durante la fase de cobro.
  - **Eficiencia:** Atajos de teclado para operaciones comunes (cobro exacto, cancelar).
