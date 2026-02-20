# User Stories - MVP QuickStack POS

Basado en el `ROADMAP.md`, aquí están las User Stories preliminares para las fases principales del MVP (Phase 1 a Phase 4). Cada historia sigue el formato de la plantilla con criterios funcionales y restricciones de seguridad aplicadas (ASVS 1.1.2/1.1.3).

---

## Phase 1.1: Catálogo Base

### US: Gestión de Categorías del Menú
**Como** MANAGER
**Quiero** crear, editar, eliminar y reordenar las categorías del menú de mi restaurante
**Para** mantener organizado mi catálogo y facilitar la búsqueda de productos en el POS

**Criterios de Aceptación (Funcionales)**
- [ ] Puedo crear categorías principales y sub-categorías (1 nivel de profundidad).
- [ ] Puedo editar el nombre y visibilidad de una categoría existente.
- [ ] Al eliminar una categoría, los productos asociados quedan sin categoría o en estado inactivo.
- [ ] Puedo reordenar cómo quiero que se muestren las categorías en el POS.

**Restricciones de Seguridad (ASVS 1.1.3) 🛡️**
- [ ] El endpoint requiere autenticación y el rol MANAGER o OWNER.
- [ ] Todas las categorías creadas se asocian automáticamente al `tenant_id` del usuario autenticado.
- [ ] La eliminación es un "soft delete" por cuestiones de auditoría de data histórica relacionada con ventas.
- [ ] Un usuario no puede ver ni modificar categorías que pertenezcan a un `tenant_id` distinto al propio.

---

### US: Gestión de Productos Simples
**Como** MANAGER
**Quiero** registrar nuevos productos en el catálogo con su nombre, precio, imagen y categoría
**Para** que los cajeros puedan venderlos en el punto de venta

**Criterios de Aceptación (Funcionales)**
- [ ] Puedo dar de alta un producto y asignar un precio en MXN.
- [ ] Puedo vincular el producto a una categoría existente.
- [ ] Puedo marcar el producto como "Activo" o "Inactivo".

**Restricciones de Seguridad (ASVS 1.1.3) 🛡️**
- [ ] Requiere rol MANAGER o OWNER.
- [ ] Validación estricta del precio: el valor debe ser un número decimal no negativo mayor o igual a cero (`>= 0`).
- [ ] Los productos pertenecen y están aislados por `tenant_id`.

---

## Phase 1.2: Modificadores y Combos

### US: Creación de Grupos de Modificadores (Extras/Sin ingredientes)
**Como** MANAGER
**Quiero** crear grupos de modificadores para los productos (ej. "Término de la carne", "Extras")
**Para** permitir la personalización de los platillos en el pedido

**Criterios de Aceptación (Funcionales)**
- [ ] Puedo definir un grupo indicando la cantidad mínima y máxima de selecciones (ej. Min 1, Max 1 para términos).
- [ ] Puedo agregar opciones al grupo con un ajuste de precio (ej. "Queso extra + $15.00").
- [ ] Puedo asociar estos grupos a productos específicos.

**Restricciones de Seguridad (ASVS 1.1.3) 🛡️**
- [ ] Validar en backend (no solo frontend) que las selecciones de modificadores al hacer checkout no superen el "máximo" permitido ni bajen del "mínimo".
- [ ] Asegurar que el ajuste de precio sume correctamente y no permita manipulación de números negativos inesperados que rebajen el precio base maliciosamente (sin ser intencional).

---

## Phase 1.3: Sistema de Pedidos y Pagos

### US: Registro de Pedido de Mostrador (COUNTER)
**Como** CAJERO
**Quiero** crear un nuevo pedido seleccionando los productos del menú y calcular el total a pagar
**Para** procesar la venta de un cliente directamente en el mostrador

**Criterios de Aceptación (Funcionales)**
- [ ] Puedo seleccionar rápidamente el "Tipo de servicio" a "Mostrador".
- [ ] Al agregar productos al carrito, el sistema calcula el subtotal, impuestos y total de manera automática.
- [ ] Puedo cerrar el pedido y registrar el pago realizado en EFECTIVO.

**Restricciones de Seguridad (ASVS 1.1.3) 🛡️**
- [ ] La acción de crear o cerrar el pedido requiere autenticación y el rol CASHIER.
- [ ] El cajero solo puede registrar ventas asignadas a *su sucursal* (`branch_id`). No puede enviar ni visualizar órdenes de sucursales a las que no tiene acceso.
- [ ] El cálculo final del pago, incluyendo modificadores e impuestos, debe ser revalidado estrictamente y de manera segura en el Backend (confiamos en los IDs del backend, nunca en los totales enviados desde el cliente front-end).

---

### US: Toma de Pedidos para Mesas (DINE_IN)
**Como** MESERO o CAJERO
**Quiero** abrir una cuenta (pedido) asignada a una mesa específica
**Para** ir agregando productos conforme el cliente los vaya solicitando

**Criterios de Aceptación (Funcionales)**
- [ ] Veo la lista/mapa de mesas disponibles y ocupadas.
- [ ] Puedo iniciar un pedido asociado a una "Mesa Libre".
- [ ] Puedo guardar el pedido y dejarlo "Abierto" hasta el pago final.

**Restricciones de Seguridad (ASVS 1.1.3) 🛡️**
- [ ] Validación de estado de competencia (Race condition / Time of check vs Time of Use): si dos meseros intentan abrir la misma mesa en el mismo milisegundo, la BD debe asegurar utilizando bloqueos optimistas/pesimistas que uno gane y el otro obtenga un error tipo "La mesa ya fue asignada".
- [ ] Las mesas están restringidas al `branch_id` asignado al empleado.

---

## Phase 2: Gestión de Inventario

### US: Descuento Automático de Stock Tras Venta
**Como** SISTEMA (Flujo Backend)
**Quiero** deducir automáticamente las cantidades de ingredientes de mi inventario al cerrar un pedido
**Para** mantener los niveles de stock actualizados sin intervención manual

**Criterios de Aceptación (Funcionales)**
- [ ] Una vez que la Status del pedido pasa a "Cerrado" o "Pagado", se calcula la receta de los productos comprados.
- [ ] El stock actual de cada ingrediente involucrado se disminuye correctamente.
- [ ] Se genera un registro de movimiento histórico en `stock_movements`.

**Restricciones de Seguridad (ASVS 1.1.3) 🛡️**
- [ ] Esta es una operación de backend que debe ejecutarse dentro de una Transacción ACID en Postgres (`@Transactional`). O todo el pedido y reducción de stock se guarda con éxito, o hace "rollback" si se produce un error, para nunca tener inventarios asíncronos en estados inconclusos.

---

## Phase 3 y 4: Tickets Digitales y Reportes

### US: Envío de Ticket Digital
**Como** CAJERO
**Quiero** enviar rápidamente una copia del ticket al WhatsApp o Email del cliente
**Para** evitar depender de papeles térmicos y automatizar las notificaciones

**Criterios de Aceptación (Funcionales)**
- [ ] Tras el cierre, veo la opción de teclear el número telefónico o email.
- [ ] El sistema envía una URL en un formato amigable.

**Restricciones de Seguridad (ASVS 1.1.3) 🛡️**
- [ ] El enlace del ticket debe ser un link de solo-lectura, generado con un Token Aleatorio Impredecible e Indescifrable y no por IDs secuenciales predecibles (Insecure Direct Object Reference) para que competidores no puedan adivinar `ticket/445` y ver tickets ajenos.
- [ ] Datos telefónicos se limitan a integraciones internas y no pueden ser descargados por rol CASHIER.

### US: Dashboard de Métricas de Ventas
**Como** OWNER
**Quiero** consultar los reportes de ventas, productos populares y totales del día
**Para** evaluar el rendimiento financiero de mi negocio

**Criterios de Aceptación (Funcionales)**
- [ ] Veo el total facturado, ticket promedio y un desglose por sucursal.
- [ ] Puedo observar métricas representadas visualmente (gráficas de los últimos 30 días).

**Restricciones de Seguridad (ASVS 1.1.3) 🛡️**
- [ ] Los endpoints de analítica solo pueden ser consumidos por el rol OWNER (o perfiles autorizados en un futuro), CAJEROS no tienen acceso.
- [ ] Filtro estricto de multitenancy a nivel de consulta base de datos para que los totales sumen exclusivamente registros del mismo tenant activo, inclusive en agregaciones masivas de datos (`sum(total)` por tenant).
