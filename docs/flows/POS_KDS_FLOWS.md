# Flujos de POS y Kitchen Display System (KDS)

Este documento detalla los flujos de interacción en tiempo real para el punto de venta y la pantalla del KDS, utilizando WebSockets para notificaciones instantáneas de las órdenes.

## 1. Creación de una Orden (Desde POS)

Flujo realizado cuando un cajero o mesero registra y envía un nuevo pedido al sistema.

```mermaid
%%{init: {"themeVariables": {"fontFamily": "arial"}}}%%
sequenceDiagram
    actor POS as Cajero (POS React)
    participant API as posApi.ts (Axios)
    participant Ctrl as OrderController.java (Spring Boot)
    participant Svc as OrderService.java
    participant WsConfig as WebSocketMessageBroker (STOMP)
    participant Repo as OrderRepository.java
    participant DB as PostgreSQL (Neon)
    participant KDS as KDS Display (React KDS)

    POS->>API: Checkout de Productos / POST /api/v1/orders
    API->>Ctrl: Request con {items, paymentInfo, orderType}
    Ctrl->>Svc: createOrder(CreateOrderRequest)
    Svc->>Svc: Calcula totales, valida inventario/precios
    Svc->>Repo: save(Order) & saveAll(OrderItems)
    Repo->>DB: INSERT INTO orders & order_items
    DB-->>Repo: Entidades Guardadas
    Repo-->>Svc: Persistido correctamente
    
    Svc->>WsConfig: convertAndSend("/topic/kds/{branchId}/orders", OrderEvent)
    Note right of WsConfig: Mensaje STOMP asíncrono push
    WsConfig-->>KDS: ORDER_CREATED {order_id, items, status: PENDING}
    KDS->>KDS: Agrega orden a la UI inmediatamente
    
    Svc-->>Ctrl: OrderDTO (incluye número de orden ej. ORD-20260205)
    Ctrl-->>API: 201 Created
    API-->>POS: Muestra ticket y confirmación
```

## 2. Actualización de Estado (KDS a POS)

Flujo realizado por el equipo de cocina para actualizar el progreso (ej. de PREPARING a READY) para ser notificado al cajero y eventualmente al cliente.

```mermaid
sequenceDiagram
    actor K as Cocinero (KDS React)
    participant WsClient as KDS WebSocket Client (STOMP JS)
    participant WsHandler as KdsWebSocketHandler.java (Spring Boot)
    participant Svc as OrderService.java
    participant Repo as OrderRepository.java
    participant DB as PostgreSQL (Neon)
    participant P as POS / Cajero (React POS)

    K->>WsClient: Clic en "Marcar Listo"
    WsClient->>WsHandler: SEND /app/kds/orders/{id}/status {status: "READY"}
    WsHandler->>Svc: updateOrderStatus(orderId, status)
    Svc->>Repo: findById(orderId)
    Repo->>DB: SELECT * FROM orders...
    DB-->>Repo: Order Entity
    Svc->>Svc: Valida transición (PREPARING -> READY)
    Svc->>Repo: save(Order actualizado)
    Repo->>DB: UPDATE orders SET status='READY'
    
    Svc->>WsHandler: convertAndSend("/topic/pos/{branchId}/updates", UpdateEvent)
    WsHandler-->>P: Recibe push con status 'READY'
    P->>P: Pinta notificación UI / Sonido "Pedido Listo"
    
    Svc-->>WsHandler: Ack
    WsHandler-->>WsClient: Petición de actualización procesada
    WsClient-->>K: Refleja UI (Desaparece / grisáceo en KDS)
```
