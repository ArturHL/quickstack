# V13: API and Web Service

> **Capitulo:** V13
> **Requisitos L2:** 13
> **Cumplidos:** 0 (0%)
> **Ultima actualizacion:** 2026-02-10

---

## V13.1 Generic Web Service Security

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 13.1.1 | Verificar que todos los componentes de la aplicacion usen las mismas codificaciones y parsers para evitar ataques de parsing que exploten diferente comportamiento de URI o parsing de archivos que podria usarse en ataques SSRF y RFI | L1 | ⏳ | **Pendiente Phase 1:** UTF-8 en toda la aplicacion. Jackson para JSON parsing. Spring MVC para URL parsing. Sin parsers mixtos. |
| 13.1.2 | [ELIMINADO - DUPLICADO DE 4.3.1] | - | N/A | - |
| 13.1.3 | Verificar que las URLs de API no expongan informacion sensible, como la clave de API, tokens de sesion, etc. | L1 | ⏳ | **Pendiente Phase 0.3:** Tokens en Authorization header, nunca en URL. IDs son UUIDs (no revelan info). Sin query params sensibles. |
| 13.1.4 | Verificar que las decisiones de autorizacion se tomen tanto en el URI, aplicadas por seguridad programatica o declarativa a nivel de controlador o router, como a nivel de recurso, aplicadas por permisos basados en modelo | L2 | ⏳ | **Pendiente Phase 1:** `@PreAuthorize` en controllers (nivel URI). TenantFilter en repositories (nivel recurso). Doble validacion. |
| 13.1.5 | Verificar que las solicitudes que contienen tipos de contenido inesperados o faltantes sean rechazadas con headers apropiados (estado de respuesta HTTP 406 Inaceptable o 415 Tipo de Medio No Soportado) | L2 | ⏳ | **Pendiente Phase 1:** `Content-Type: application/json` requerido para POST/PUT. Spring MVC retorna 415 para tipos invalidos. |

---

## V13.2 RESTful Web Service

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 13.2.1 | Verificar que los metodos HTTP RESTful habilitados sean una opcion valida para el usuario o accion, como prevenir que usuarios normales usen DELETE o PUT en recursos o APIs protegidas | L1 | ⏳ | **Pendiente Phase 1:** CORS con metodos especificos. `@PreAuthorize` valida rol antes de DELETE/PUT. OWNER puede DELETE, CASHIER no. |
| 13.2.2 | Verificar que la validacion de esquema JSON este en su lugar y verificada antes de aceptar entrada | L1 | ⏳ | **Pendiente Phase 1:** Bean Validation en DTOs (`@Valid`). Schema implicito via tipos Java. Jackson rechaza JSON malformado. |
| 13.2.3 | Verificar que los servicios web RESTful que utilizan cookies esten protegidos de Falsificacion de Solicitud Entre Sitios via el uso de al menos uno o mas de los siguientes: patron de cookie de doble envio, nonces CSRF, o verificaciones de header Origin | L1 | ⏳ | **Pendiente Phase 0.3:** Access token en header (no cookie) = sin CSRF. Refresh token en cookie SameSite=Strict. Origin header validado. |
| 13.2.4 | [ELIMINADO - DUPLICADO DE 11.1.4] | - | N/A | - |
| 13.2.5 | Verificar que los servicios REST verifiquen explicitamente que el Content-Type entrante sea el esperado, como application/xml o application/json | L2 | ⏳ | **Pendiente Phase 1:** `consumes = MediaType.APPLICATION_JSON_VALUE` en controllers. Rechazo de otros content types. |
| 13.2.6 | Verificar que los headers y payload del mensaje esten firmados usando JWS y cifrados usando JWE para comunicacion sensible entre servicios | L2 | N/A | **No aplica:** Comunicacion service-to-service via conexiones TLS directas. Sin JWE para mensajes entre backend y Neon. |

---

## V13.3 SOAP Web Service

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 13.3.1 | Verificar que la validacion de esquema XSD tenga lugar para asegurar un documento XML bien formado, seguido de validacion de cada campo de entrada antes de cualquier procesamiento de esos datos | L1 | N/A | **No aplica:** Sin SOAP. API REST con JSON unicamente. |
| 13.3.2 | Verificar que el payload del mensaje este firmado usando WS-Security para asegurar transporte confiable entre cliente y servicio | L2 | N/A | **No aplica:** Sin SOAP. |

---

## V13.4 GraphQL

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 13.4.1 | Verificar que se use una lista blanca de queries o una combinacion de limitacion de profundidad y limitacion de cantidad para prevenir ataques de denegacion de servicio (DoS) de GraphQL o expresiones de capa de datos como resultado de consultas anidadas costosas. Para escenarios mas avanzados, se debe usar analisis de costo de consulta | L2 | N/A | **No aplica:** Sin GraphQL. API REST unicamente. |
| 13.4.2 | Verificar que GraphQL u otra logica de autorizacion de capa de datos se deba implementar a nivel de logica de negocio en lugar de la capa de GraphQL | L2 | N/A | **No aplica:** Sin GraphQL. |

---

## Resumen de Estado

| Seccion | Total Requisitos | Cumplidos | Pendientes | No Aplica |
|---------|------------------|-----------|------------|-----------|
| V13.1 Generic Web Service | 4 | 0 | 4 | 0 |
| V13.2 RESTful Web Service | 5 | 0 | 5 | 0 |
| V13.3 SOAP Web Service | 2 | 0 | 0 | 2 |
| V13.4 GraphQL | 2 | 0 | 0 | 2 |
| **TOTAL** | **13** | **0** | **9** | **4** |
