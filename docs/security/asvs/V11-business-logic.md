# V11: Business Logic

> **Capitulo:** V11
> **Requisitos L2:** 8
> **Cumplidos:** 0 (0%)
> **Ultima actualizacion:** 2026-02-10

---

## V11.1 Business Logic Security

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 11.1.1 | Verificar que la aplicacion solo procese flujos de logica de negocio para el mismo usuario en orden de pasos secuenciales y sin omitir pasos | L1 | ⏳ | **Pendiente Phase 1:** Order workflow: PENDING -> IN_PROGRESS -> READY -> DELIVERED -> COMPLETED. Validacion de transiciones validas en OrderService. |
| 11.1.2 | Verificar que la aplicacion solo procese flujos de logica de negocio con todos los pasos siendo procesados en tiempo humano realista, es decir, las transacciones no se envian demasiado rapido | L1 | ⏳ | **Pendiente Phase 1:** Rate limiting en creacion de ordenes. Timestamp validation en flujos criticos. Deteccion de automatizacion anomala. |
| 11.1.3 | Verificar que la aplicacion tenga limites apropiados para acciones o transacciones de negocio especificas que se apliquen correctamente por usuario | L1 | ⏳ | **Pendiente Phase 1:** Limites por plan de suscripcion (max_branches, max_users). Rate limits por tenant. Validacion de cuotas en service layer. |
| 11.1.4 | Verificar que la aplicacion tenga controles anti-automatizacion para proteger contra llamadas excesivas como exfiltracion masiva de datos, solicitudes de logica de negocio automatizadas, compras excesivas de archivos, o transacciones automatizadas | L1 | ⏳ | **Pendiente Phase 1:** Bucket4j rate limiting. CAPTCHA si se detecta patron anomalo (futuro). Logging de patrones sospechosos. |
| 11.1.5 | Verificar que la aplicacion tenga limites de logica de negocio o validacion para proteger contra riesgos o amenazas de negocio probables, identificados usando modelado de amenazas o metodologias similares | L1 | ⏳ | **Pendiente Phase 1:** Threat model en SECURITY.md. Limites: monto maximo por orden, cantidad maxima por item, descuentos maximos. Validacion en service layer. |
| 11.1.6 | Verificar que la aplicacion no sufra de problemas de "Tiempo de verificacion a Tiempo de uso" (TOCTOU) u otras condiciones de carrera para operaciones sensibles | L2 | ⏳ | **Pendiente Phase 1:** `@Transactional` con isolation apropiado. `SELECT FOR UPDATE` para inventario. Optimistic locking con `@Version` en entidades criticas. |
| 11.1.7 | Verificar que la aplicacion monitoree eventos o actividad inusuales desde una perspectiva de logica de negocio. Por ejemplo, intentos de realizar acciones fuera de orden o acciones que un usuario normal nunca intentaria | L2 | ⏳ | **Pendiente Phase 6:** Logging de acciones anomalas. Alertas en patrones sospechosos (ej: CASHIER intentando acceder a reports). Audit trail completo. |
| 11.1.8 | Verificar que la aplicacion tenga alertas configurables cuando se detecten ataques automatizados o actividad inusual | L2 | ⏳ | **Pendiente Phase 6:** Integracion con servicio de alertas (Slack, email). Umbrales configurables. Dashboard de seguridad basico. |

---

## Resumen de Estado

| Seccion | Total Requisitos | Cumplidos | Pendientes | No Aplica |
|---------|------------------|-----------|------------|-----------|
| V11.1 Business Logic | 8 | 0 | 8 | 0 |
| **TOTAL** | **8** | **0** | **8** | **0** |
