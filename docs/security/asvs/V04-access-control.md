# V4: Access Control

> **Capitulo:** V4
> **Requisitos L2:** 16
> **Cumplidos:** 0 (0%)
> **Ultima actualizacion:** 2026-02-10

---

## V4.1 General Access Control Design

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 4.1.1 | Verificar que la aplicacion aplique reglas de control de acceso en una capa de servicio de confianza, especialmente si el control de acceso del lado del cliente esta presente y podria ser bypasseado | L1 | ⏳ | **Pendiente Phase 1:** Spring Security `@PreAuthorize` en controllers. `TenantFilter` en repository layer. Frontend solo oculta UI, nunca es autoritativo. |
| 4.1.2 | Verificar que todos los atributos de usuario y datos y la informacion de politica utilizada por los controles de acceso no puedan ser manipulados por usuarios finales a menos que se autorice especificamente | L1 | ⏳ | **Pendiente Phase 1:** Claims JWT firmados (inmutables). `tenant_id`, `role`, `branch_id` extraidos del token, no del request body. |
| 4.1.3 | Verificar que exista el principio de menor privilegio - los usuarios solo deben poder acceder a funciones, archivos de datos, URLs, controladores, servicios, y otros recursos, para los cuales poseen autorizacion especifica. Esto implica proteccion contra spoofing y elevacion de privilegios | L1 | ⏳ | **Pendiente Phase 1:** RBAC estricto: OWNER (todo), CASHIER (POS + su branch), KITCHEN (KDS + su branch). Permisos granulares en `roles.permissions` JSON. |
| 4.1.4 | [ELIMINADO - DUPLICADO DE 4.1.3] | - | N/A | - |
| 4.1.5 | Verificar que los controles de acceso fallen de forma segura incluyendo cuando ocurre una excepcion | L1 | ⏳ | **Pendiente Phase 1:** `AccessDeniedException` manejada por `GlobalExceptionHandler`. Default deny. Excepciones no bypassean auth. |

---

## V4.2 Operation Level Access Control

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 4.2.1 | Verificar que los datos sensibles y APIs esten protegidos contra ataques de Referencia Directa de Objeto Inseguro (IDOR) dirigidos a creacion, lectura, actualizacion y eliminacion de registros, como crear o actualizar el registro de otra persona, ver los registros de todos, o eliminar todos los registros | L1 | ⏳ | **Pendiente Phase 1:** Todas las queries filtran por `tenant_id` del JWT. Composite FKs en BD previenen referencias cross-tenant. IDs son UUIDs (no secuenciales). |
| 4.2.2 | Verificar que la aplicacion o framework aplique un fuerte mecanismo anti-CSRF para proteger funcionalidad autenticada, y que anti-automatizacion o anti-CSRF efectivos protejan funcionalidad no autenticada | L1 | ⏳ | **Pendiente Phase 0.3:** JWT en Authorization header (no cookie para access token). Refresh token con SameSite=Strict. Stateless = no CSRF para API calls. |

---

## V4.3 Other Access Control Considerations

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 4.3.1 | Verificar que las interfaces administrativas usen autenticacion multi-factor apropiada para prevenir uso no autorizado | L1 | ⏳ | **MVP:** Sin MFA. Mitigacion: account lockout, IP logging, rate limiting. **Futuro:** TOTP opcional para OWNER. |
| 4.3.2 | Verificar que la navegacion de directorios este deshabilitada a menos que se desee deliberadamente. Adicionalmente, las aplicaciones no deben permitir descubrimiento o divulgacion de metadatos de archivo o directorio, como carpetas Thumbs.db, .DS_Store, .git o .svn | L1 | ⏳ | **Pendiente Phase 0.2:** Spring Boot no sirve archivos estaticos directamente (API only). Frontend en Vercel con config apropiada. No hay endpoints de archivos. |
| 4.3.3 | Verificar que la aplicacion tenga autorizacion adicional (como step up o autenticacion adaptativa) para sistemas de menor valor, y/o segregacion de deberes para aplicaciones de alto valor para aplicar controles anti-fraude segun el riesgo de la aplicacion y fraudes pasados | L2 | ⏳ | **Pendiente Phase 1:** Operaciones criticas (cambio password, delete data) requieren re-entrada de password. Logging de operaciones sensibles. |

---

## Resumen de Estado

| Seccion | Total Requisitos | Cumplidos | Pendientes | No Aplica |
|---------|------------------|-----------|------------|-----------|
| V4.1 General Design | 4 | 0 | 4 | 0 |
| V4.2 Operation Level | 2 | 0 | 2 | 0 |
| V4.3 Other Considerations | 3 | 0 | 3 | 0 |
| **TOTAL** | **9** | **0** | **9** | **0** |
