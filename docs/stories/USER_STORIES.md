# User Stories — QuickStack

> **Última actualización:** 2026-03-04
> **Referencia:** `docs/ROADMAP.md`
> **Formato:** Las historias siguen el estándar ASVS 1.1.3 — cada historia incluye criterios funcionales y restricciones de seguridad.

---

## Phase 2: UX Roles + Piloto

### US-201: Cajero opera venta de mostrador desde su interfaz dedicada

**Como** CASHIER
**Quiero** una interfaz dividida con el catálogo a la izquierda y el carrito activo a la derecha
**Para** procesar ventas de mostrador sin navegar entre pantallas y completar cada transacción en menos de 60 segundos

**Criterios de Aceptación**
- [ ] Al ingresar a `/cashier/pos`, veo el catálogo de productos en el panel izquierdo y el carrito vacío en el panel derecho.
- [ ] Puedo agregar productos al carrito con un solo toque/click desde el catálogo.
- [ ] El carrito muestra subtotal, IVA y total en tiempo real conforme agrego productos.
- [ ] Puedo seleccionar variantes y modificadores desde un modal sin abandonar la pantalla principal.
- [ ] Al presionar "Cobrar", ingreso el monto recibido y el sistema calcula el cambio automáticamente.
- [ ] Tras confirmar el pago, la orden queda como COMPLETED y el carrito se vacía listo para el siguiente cliente.

**Restricciones de Seguridad (ASVS 1.1.3)**
- [ ] La ruta `/cashier/*` requiere autenticación y rol CASHIER o superior.
- [ ] El total final es calculado y validado en el backend — el frontend solo envía IDs de productos, variantes y modificadores, nunca precios.
- [ ] El cajero solo puede crear órdenes en la sucursal asignada a su cuenta (`branch_id`). El backend rechaza (403) cualquier intento de crear orden en otra sucursal.

---

### US-202: Admin crea y gestiona usuarios del restaurante

**Como** ADMIN (Owner o Manager)
**Quiero** crear cuentas para mi personal (cajeros, meseros, cocineros) directamente desde la app
**Para** no depender de soporte técnico para dar de alta a un nuevo empleado

**Criterios de Aceptación**
- [ ] Desde `/admin/users`, puedo ver la lista de usuarios activos del tenant con nombre, rol y estado.
- [ ] Puedo crear un usuario nuevo asignándole nombre, email, contraseña temporal y rol (CASHIER, WAITER, KITCHEN, ADMIN).
- [ ] Puedo desactivar (soft-delete) a un empleado que ya no trabaja en el restaurante.
- [ ] El nuevo usuario puede hacer login inmediatamente con su email y contraseña temporal.

**Restricciones de Seguridad (ASVS 1.1.3)**
- [ ] Solo el rol ADMIN puede crear, editar o desactivar usuarios — CASHIER y WAITER no tienen acceso.
- [ ] La contraseña temporal está sujeta a las mismas políticas de seguridad que el registro normal (Argon2id, longitud mínima).
- [ ] Un ADMIN no puede crear usuarios con un rol superior al propio ni asignar `tenant_id` distinto al suyo.
- [ ] Los usuarios desactivados no pueden autenticarse aunque tengan un refresh token activo (sesión invalidada en el momento de la desactivación).

---

### US-203: Admin alterna entre perspectiva Owner y Manager según sucursal activa

**Como** ADMIN
**Quiero** que al seleccionar "Todas las sucursales" vea métricas globales, y al seleccionar una sucursal específica vea las herramientas operativas de esa sucursal
**Para** no necesitar dos cuentas distintas para gestionar el negocio globalmente o a nivel operativo

**Criterios de Aceptación**
- [ ] El selector de sucursal en el TopBar tiene la opción "Todas las sucursales" además de las sucursales individuales.
- [ ] Con "Todas las sucursales": el Sidebar muestra secciones de Owner (métricas globales, gestión de usuarios, configuración de sucursales).
- [ ] Con una sucursal seleccionada: el Sidebar muestra secciones de Manager (catálogo, mesas, reportes de esa sucursal, acciones rápidas).
- [ ] El cambio de contexto es inmediato, sin recarga de página.

**Restricciones de Seguridad (ASVS 1.1.3)**
- [ ] El contexto de sucursal no amplía los permisos — todos los endpoints siguen filtrando por `tenant_id`.
- [ ] La selección de sucursal activa se almacena solo en el cliente (localStorage/sessionStorage), no en el backend, para evitar manipulación de contexto.

---

## Phase 3: Owner Intelligence

### US-301: Owner registra un gasto del restaurante

**Como** OWNER
**Quiero** registrar rápidamente un gasto (compra de insumos, pago de renta, servicio) desde mi teléfono
**Para** tener por primera vez un registro completo de mis costos y poder calcular mis márgenes reales

**Criterios de Aceptación**
- [ ] Desde `/admin/expenses`, puedo registrar un gasto con: monto, categoría, descripción breve y fecha.
- [ ] Las categorías disponibles son: Insumos, Nómina, Renta, Servicios, Mantenimiento, Otro.
- [ ] Puedo ver el historial de gastos del período actual con filtros por categoría y rango de fechas.
- [ ] El resumen del período muestra el total gastado por categoría.
- [ ] El formulario se puede completar en menos de 30 segundos.

**Restricciones de Seguridad (ASVS 1.1.3)**
- [ ] Solo ADMIN puede registrar y ver gastos — rol CASHIER no tiene acceso.
- [ ] Todos los gastos están asociados al `tenant_id` y `branch_id` del usuario autenticado.
- [ ] El monto debe ser validado en backend como decimal positivo mayor a cero.

---

### US-302: Sistema descuenta inventario automáticamente al completar una venta

**Como** SISTEMA (flujo backend)
**Quiero** deducir las cantidades exactas de ingredientes de cada producto vendido al completar el pago
**Para** mantener el inventario actualizado en tiempo real sin que nadie tenga que contarlo manualmente

**Criterios de Aceptación**
- [ ] Al marcar una orden como COMPLETED (pago registrado), el sistema busca la receta de cada `OrderItem`.
- [ ] Por cada ingrediente en la receta, reduce el `stock_actual` en la cantidad correspondiente × unidades vendidas.
- [ ] Si un producto no tiene receta configurada, la orden se completa igual (sin deducción) y se genera un log de advertencia.
- [ ] Cada deducción queda registrada en `inventory_movements` con referencia al `order_id`.

**Restricciones de Seguridad (ASVS 1.1.3)**
- [ ] La deducción de stock y el registro del pago deben ocurrir dentro de la misma transacción `@Transactional`. Si falla la deducción, se hace rollback del pago completo — nunca estado inconsistente.
- [ ] El stock nunca puede quedar negativo por manipulación de cantidades en el request (validación en backend).
- [ ] Los movimientos de inventario son inmutables — no se pueden editar ni eliminar, solo agregar (audit trail).

---

### US-303: Admin gestiona el catálogo de ingredientes y recetas

**Como** ADMIN (Owner o Manager)
**Quiero** definir los ingredientes de mi restaurante y asociarlos a cada producto del menú con sus cantidades exactas
**Para** que el sistema pueda descontar automáticamente el inventario con cada venta

**Criterios de Aceptación**
- [ ] Desde `/admin/ingredients`, puedo crear ingredientes con: nombre, unidad de medida (kg, litros, piezas, etc.), costo por unidad y stock mínimo de alerta.
- [ ] Puedo registrar el stock inicial de cada ingrediente al darlo de alta.
- [ ] Desde la edición de un producto, puedo asociar una receta: lista de ingredientes con la cantidad exacta que consume cada unidad vendida.
- [ ] Un producto puede no tener receta (el sistema lo permite, pero avisa que no habrá auto-deducción).

**Restricciones de Seguridad (ASVS 1.1.3)**
- [ ] Solo ADMIN puede crear o editar ingredientes y recetas.
- [ ] Las cantidades de receta son decimales positivos — el backend rechaza valores negativos o cero.
- [ ] Los ingredientes están aislados por `tenant_id`, nunca visibles entre tenants distintos.

---

### US-304: Owner ve el dashboard de inventario con alertas de stock bajo

**Como** OWNER
**Quiero** ver de un vistazo qué ingredientes están por acabarse
**Para** saber qué necesito comprar antes de quedarme sin producto y perder ventas

**Criterios de Aceptación**
- [ ] Desde `/admin/inventory`, veo una lista de todos los ingredientes con su stock actual y stock mínimo.
- [ ] Los ingredientes con stock ≤ mínimo se destacan visualmente (rojo) y aparecen primero en la lista.
- [ ] Puedo ajustar manualmente el stock de un ingrediente (ej. después de una compra) con un registro del motivo.

**Restricciones de Seguridad (ASVS 1.1.3)**
- [ ] Los ajustes manuales de stock quedan registrados en `inventory_movements` con el `user_id` que lo realizó y el motivo — no se pueden borrar.
- [ ] Solo ADMIN puede hacer ajustes manuales de stock.

---

### US-305: Sistema genera la lista de compras automáticamente

**Como** OWNER
**Quiero** que el sistema me diga qué necesito comprar hoy sin que yo tenga que revisar físicamente la cocina
**Para** ir al mercado o contactar a proveedores con información precisa en lugar de intuición

**Criterios de Aceptación**
- [ ] Desde `/admin/shopping-list`, el sistema muestra los ingredientes que están por debajo del stock mínimo.
- [ ] Para cada ingrediente en la lista, sugiere la cantidad a comprar basada en el consumo promedio de los últimos 7 días.
- [ ] Puedo marcar ingredientes como "Ya comprado" para llevar control durante el día.
- [ ] Puedo exportar o compartir la lista (texto plano para WhatsApp).

**Restricciones de Seguridad (ASVS 1.1.3)**
- [ ] La lista de compras se genera consultando datos del `tenant_id` activo — nunca datos de otros tenants.
- [ ] El cálculo de consumo promedio usa datos históricos de `inventory_movements`, accesibles solo con autenticación.

---

### US-306: Owner consulta su primer reporte P&L real

**Como** OWNER
**Quiero** ver en una sola pantalla mis ventas totales, mis costos y mi margen bruto del período
**Para** saber por primera vez si mi negocio es rentable y tomar decisiones con datos reales

**Criterios de Aceptación**
- [ ] Desde `/admin/reports/pnl`, veo: Ventas Totales, Costo de Bienes Vendidos (COGS), Gastos Registrados, y **Margen Bruto** (Ventas − COGS − Gastos).
- [ ] Puedo filtrar por rango de fechas (hoy, semana, mes, personalizado).
- [ ] El margen se muestra como valor absoluto en MXN y como porcentaje.
- [ ] Si no hay gastos registrados en el período, el sistema avisa que el P&L es parcial.

**Restricciones de Seguridad (ASVS 1.1.3)**
- [ ] Solo el rol ADMIN puede acceder al reporte P&L — CASHIER y WAITER no tienen acceso.
- [ ] Todas las agregaciones (SUM de ventas, gastos) están filtradas por `tenant_id` en la query base — no se puede inferir datos de otros tenants por manipulación de parámetros de fecha.

---

## Phase 4: Operations Scale

### US-401: Mesero toma y envía un pedido desde su teléfono

**Como** WAITER
**Quiero** tomar el pedido de una mesa desde mi teléfono y enviarlo a cocina con un solo botón
**Para** no tener que ir al mostrador o a una terminal fija para cada orden

**Criterios de Aceptación**
- [ ] Al ingresar a `/waiter/tables`, veo el mapa de mesas de mi sucursal con colores por estado (disponible, ocupada, esperando cuenta).
- [ ] Al tocar una mesa disponible, puedo iniciar un pedido nuevo.
- [ ] Puedo navegar el catálogo con tarjetas grandes, seleccionar variantes y modificadores en modales de pantalla completa.
- [ ] El botón "Enviar a Cocina" está siempre visible en la barra inferior.
- [ ] Puedo agregar más productos a una mesa que ya tiene pedido abierto.
- [ ] Puedo solicitar la cuenta — el estado de la mesa cambia a "Esperando cobro" y notifica al cajero.

**Restricciones de Seguridad (ASVS 1.1.3)**
- [ ] La ruta `/waiter/*` requiere autenticación y rol WAITER o superior.
- [ ] Si dos meseros intentan abrir la misma mesa simultáneamente, el backend garantiza con bloqueo optimista que solo uno lo logra — el segundo recibe error claro "Mesa ya asignada".
- [ ] El mesero solo puede ver y operar mesas de la sucursal asignada a su cuenta.

---

### US-402: Cocina ve y gestiona las comandas en el KDS

**Como** personal de KITCHEN
**Quiero** ver en pantalla grande todas las órdenes pendientes de preparar en formato Kanban
**Para** preparar los platillos en el orden correcto sin depender de tickets impresos

**Criterios de Aceptación**
- [ ] Al ingresar a `/kitchen/board`, veo todas las órdenes con estado IN_PROGRESS o READY como tarjetas Kanban.
- [ ] Cada tarjeta muestra: número de orden, mesa/tipo de servicio, lista de productos y modificadores importantes resaltados ("Sin cebolla", "Término medio").
- [ ] Puedo marcar una orden completa como "Lista" con un toque — esto cambia su estado a READY.
- [ ] Las órdenes nuevas generan una alerta visual en pantalla.
- [ ] La pantalla se actualiza automáticamente cada 8 segundos sin recargar la página.

**Restricciones de Seguridad (ASVS 1.1.3)**
- [ ] La ruta `/kitchen/*` requiere autenticación y rol KITCHEN o superior.
- [ ] El endpoint de KDS filtra por `branch_id` y `tenant_id` — un KDS en una sucursal nunca ve órdenes de otra.
- [ ] La acción de marcar como READY solo puede ser ejecutada desde roles autorizados — no es un endpoint público.

---

### US-403: Bot de WhatsApp registra un gasto a partir de una foto de recibo

**Como** OWNER
**Quiero** enviar la foto de un recibo o ticket de compra a un número de WhatsApp
**Para** que el sistema lo registre automáticamente como gasto sin que yo tenga que escribir nada manualmente

**Criterios de Aceptación**
- [ ] Al enviar una imagen al número de WhatsApp del bot, el sistema extrae monto, proveedor y fecha del recibo con OCR.
- [ ] El bot responde confirmando los datos extraídos y la categoría asignada automáticamente.
- [ ] El gasto queda registrado en el sistema como si lo hubiera ingresado manualmente desde `/admin/expenses`.
- [ ] Si el OCR no puede leer el recibo, el bot solicita confirmar los datos manualmente antes de guardar.

**Restricciones de Seguridad (ASVS 1.1.3)**
- [ ] El número de WhatsApp del OWNER debe estar verificado y asociado a su cuenta — el bot rechaza mensajes de números no registrados.
- [ ] Los datos del recibo se procesan y descartan del proveedor de OCR inmediatamente — no se almacenan imágenes de recibos en el sistema.
- [ ] El bot solo puede crear gastos en el `tenant_id` asociado al número de WhatsApp autenticado.

---

### US-404: Cajero envía ticket digital al cliente por WhatsApp

**Como** CASHIER
**Quiero** enviar el comprobante de pago al WhatsApp del cliente tras completar una venta
**Para** eliminar la dependencia de la impresora térmica y que el cliente tenga su comprobante en su teléfono

**Criterios de Aceptación**
- [ ] Tras confirmar el pago, aparece la opción de enviar ticket digital con campo para ingresar el número de teléfono.
- [ ] El cliente recibe por WhatsApp un enlace con el resumen de su orden (productos, total, fecha, folio).
- [ ] El envío es opcional — si el cajero lo omite, la orden se completa igualmente.
- [ ] El enlace del ticket funciona sin necesidad de login.

**Restricciones de Seguridad (ASVS 1.1.3)**
- [ ] El enlace del ticket se genera con un token aleatorio de alta entropía (≥128 bits) — no usa el `order_id` directamente para evitar IDOR.
- [ ] El token expira después de 30 días.
- [ ] El número de teléfono del cliente solo se usa para el envío — no se almacena asociado a la orden a menos que el cliente tenga un perfil creado explícitamente.

---

### US-405: Owner recibe resumen de ventas del día por WhatsApp

**Como** OWNER
**Quiero** recibir automáticamente al cierre del día un resumen de ventas en mi WhatsApp
**Para** saber cómo le fue al restaurante sin tener que abrir la app ni pedirle el reporte a alguien

**Criterios de Aceptación**
- [ ] Cada día a una hora configurable (ej. 11pm), el sistema envía al WhatsApp del OWNER: total de ventas, número de pedidos, ticket promedio y top 3 productos del día.
- [ ] Si el día tuvo cero ventas, el sistema envía igualmente el resumen indicando "Sin ventas registradas".
- [ ] El OWNER puede desactivar las notificaciones automáticas desde la configuración.

**Restricciones de Seguridad (ASVS 1.1.3)**
- [ ] El resumen solo contiene datos agregados — no incluye datos personales de clientes.
- [ ] El número de WhatsApp del OWNER está verificado y vinculado a su cuenta en el sistema — no hay endpoint público que dispare el envío.

---

## Historias completadas (Phase 1)

> Las historias de Phase 1.1–1.3 están implementadas y en producción. Se conservan como referencia histórica.

### Phase 1.1–1.2: Catálogo y Modificadores ✅

- **US-101** MANAGER crea y organiza categorías del menú
- **US-102** MANAGER registra productos simples con precio, categoría e imagen
- **US-103** MANAGER configura variantes de producto (tamaños, presentaciones)
- **US-104** MANAGER crea grupos de modificadores con opciones y ajustes de precio
- **US-105** MANAGER configura combos con precio especial

### Phase 1.3: Pedidos y Pagos ✅

- **US-106** CASHIER crea pedido de mostrador (COUNTER) y registra pago en efectivo
- **US-107** CASHIER/WAITER abre pedido para mesa específica (DINE_IN)
- **US-108** CASHIER gestiona pedido para llevar con datos de cliente (DELIVERY/TAKEOUT)
- **US-109** CASHIER consulta y gestiona órdenes del día con filtros por estado
- **US-110** OWNER/MANAGER consulta el reporte diario de ventas (total, ticket promedio, top productos)
