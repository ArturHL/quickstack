# Flujos de Notificación y Tickets Digitales

Este flujo describe la interacción asíncrona de la aplicación cuando es necesario enviar notificaciones externas (por ejemplo, tickets digitales o confirmaciones de orden) vía Email o WhatsApp sin bloquear el hilo principal.

## 1. Envío Asíncrono de Confirmación / Ticket de Órden

```mermaid
%%{init: {"themeVariables": {"fontFamily": "arial"}}}%%
sequenceDiagram
    participant Ctrl as OrderController.java
    participant OS as OrderService.java
    participant E as EventPublisher (Spring ApplicationEventPublisher)
    participant L as OrderEventListener.java (@Async)
    participant NS as NotificationService.java
    participant Ext as Twilio/SendGrid API
    participant DB as NotificationLogRepository.java

    Ctrl->>OS: /orders/{id}/pay (Cliente Completa Pago)
    OS->>OS: Actualiza Status de Pago
    
    OS->>E: publishEvent(OrderPaidEvent)
    Note over OS,E: Retorna inmediatamente (No bloqueante)
    OS-->>Ctrl: 200 OK (Pago Éxitoso)
    
    E->>L: Captura OrderPaidEvent en hilo Background
    L->>NS: processDigitalTicket(orderId, channel="WHATSAPP")
    
    NS->>NS: Renderiza Template de Ticket Digital (Thymeleaf/Freemarker)
    NS->>Ext: POST Asíncrono a API de Twilio (Ej: Envío Mensaje)
    
    alt Envío Exitoso (Ej: Status 202/200 Twilio)
        Ext-->>NS: HTTP 200 Success / MessageSid
        NS->>DB: Guarda NotificationLog (status='DELIVERED', order_id, channel)
    else Error Externo (Ej: 429 Rate Limit o 500)
        Ext-->>NS: HTTP Error / Exception
        NS->>DB: Guarda NotificationLog (status='FAILED', retries=0)
        NS->>NS: Agenda Retry o Dead Letter Queue (DLQ)
    end
```
