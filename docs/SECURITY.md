# QuickStack POS - Security Architecture (OWASP ASVS L2)

> **Version:** 2.0.0
> **Last Updated:** 2026-02-09
> **Author:** Security Architect
> **Standard:** OWASP ASVS 4.0.3
> **Target Level:** L2 (Standard)

---

## Nivel de Cumplimiento

| Aspecto | Detalle |
|---------|---------|
| **Estandar** | OWASP Application Security Verification Standard (ASVS) 4.0.3 |
| **Nivel Objetivo** | Level 2 - Standard |
| **Aplicacion** | QuickStack POS - Sistema SaaS Multi-tenant para restaurantes |
| **Estado Actual** | En diseno e implementacion inicial |
| **Revision** | Trimestral |

### Por que ASVS L2?

- **L1** es para aplicaciones de bajo riesgo - insuficiente para datos financieros y PII
- **L2** es apropiado para aplicaciones que manejan transacciones comerciales y datos personales sensibles
- **L3** es para aplicaciones criticas (banca, salud, infraestructura) - excesivo para MVP de POS

QuickStack maneja:
- Datos de transacciones financieras (ventas, pagos)
- Informacion personal de clientes (telefono, email, direccion)
- Datos de negocio sensibles (inventario, costos, margenes)
- Credenciales de multiples usuarios por tenant

---

## Requisitos ASVS

Los requisitos detallados de OWASP ASVS L2 estan documentados por capitulo en:

**[Ver Indice de Requisitos ASVS](security/asvs/README.md)**

### Progreso Actual

| Capitulo | Cumplidos | Total | Estado |
|----------|-----------|-------|--------|
| V1 - Architecture | 12 | 38 | [Documentado](security/asvs/V01-architecture.md) |
| V2-V14 | 0 | ~235 | Pendiente |
| **Total** | **12** | **~273** | **4%** |

---

## Threat Model

### Assets Criticos

| Asset | Clasificacion | Ubicacion | Impacto si Comprometido |
|-------|---------------|-----------|------------------------|
| Credenciales de usuarios | CRITICO | Neon PostgreSQL (password_hash) | Acceso no autorizado a todo el sistema |
| Tokens JWT | CRITICO | Memoria (frontend), Headers (transito) | Suplantacion de identidad, acceso a datos de tenant |
| Datos de transacciones | ALTO | Neon PostgreSQL | Fraude financiero, perdida de ingresos, problemas legales |
| PII de clientes | ALTO | Neon PostgreSQL | Violacion de privacidad, multas LFPDPPP, dano reputacional |
| Datos de inventario/costos | MEDIO | Neon PostgreSQL | Ventaja competitiva comprometida |
| API keys (Twilio, SendGrid) | MEDIO | Render env vars | Abuso de servicios, costos inesperados |
| Codigo fuente | MEDIO | GitHub | Exposicion de vulnerabilidades, robo de IP |

### Actores de Amenaza

| Actor | Motivacion | Capacidad | Probabilidad |
|-------|------------|-----------|--------------|
| **Empleado malicioso** | Robo de datos, fraude | Media (conocimiento interno) | Media |
| **Competidor** | Robo de datos de clientes/precios | Media (recursos para contratar) | Baja |
| **Script kiddie** | Vandalismo, practica | Baja (herramientas automatizadas) | Alta |
| **Criminal organizado** | Fraude financiero, ransomware | Alta | Media |
| **Ex-empleado** | Venganza, robo de datos | Media (credenciales residuales) | Media |

### Vectores de Ataque Principales

| Vector | Descripcion | Mitigacion |
|--------|-------------|------------|
| **Credential stuffing** | Uso de credenciales robadas de otros servicios | Rate limiting por IP/email, account lockout despues de N intentos, HaveIBeenPwned check en registro, login_attempts audit log |
| **Session hijacking** | Robo de JWT para suplantar usuario | Tokens de corta duracion (15-30 min), HTTPS obligatorio, httpOnly cookies para refresh |
| **SQL Injection** | Manipulacion de queries via input | Parametrized queries (JPA), input validation, WAF en futuro |
| **XSS** | Inyeccion de scripts maliciosos | React escapado automatico, CSP headers, no innerHTML |
| **IDOR** | Acceso a recursos de otros tenants | Composite FKs con tenant_id, TenantFilter en todas las queries |
| **API abuse** | Scraping, DoS, enumeration | Rate limiting, autenticacion requerida, no enumeration en errores |
| **Insider threat** | Empleado accede a datos no autorizados | Principio de minimo privilegio, audit logs, RBAC estricto |
| **Supply chain** | Dependencia comprometida | Dependency scanning, lockfiles, Dependabot |

### Diagrama de Amenazas (STRIDE)

```
                                    INTERNET
                                        |
                    [Spoofing, Tampering, DoS]
                                        |
                                        v
                        +---------------------------+
                        |      Vercel CDN           |
                        |   (Frontend - React)      |
                        +-------------+-------------+
                                      |
                        [XSS, Information Disclosure]
                                      |
                                      v
                              +---------------------------+
                              |      Render               |
                              |   (Backend - Spring)      |
                              |   + Auth Module           |
                              +-------------+-------------+
                                            |
                      [SQLi, IDOR, Elevation of Privilege, Credential Stuffing]
                                            |
                                            v
                        +---------------------------+
                        |      Neon PostgreSQL      |
                        |   (Multi-tenant Data)     |
                        +---------------------------+
                                      ^
                        [Data Breach, Repudiation]
```

---

## Protocolos de Desarrollo Seguro

### Checklist Pre-PR (Obligatorio)

Antes de crear un Pull Request, verificar:

**Autenticacion/Autorizacion**
- [ ] Endpoint requiere autenticacion (excepto health check, login)
- [ ] Se verifica rol del usuario (`@PreAuthorize` o `hasRole()`)
- [ ] Se verifica tenant_id del usuario contra recurso accedido
- [ ] branch_id verificado para roles CASHIER/KITCHEN

**Validacion de Entrada**
- [ ] DTOs tienen annotations de validacion (`@Valid`, `@NotNull`, `@Size`, etc.)
- [ ] Validacion de negocio en Service layer
- [ ] No se usa input del usuario directamente en queries (parametrizado)
- [ ] Enums/tipos fuertes para valores conocidos (no strings magicos)

**Proteccion de Datos**
- [ ] Datos sensibles no se loggean (passwords, tokens, PII)
- [ ] Respuestas de error no revelan detalles internos
- [ ] PII solo se retorna cuando es necesario (no en listados)
- [ ] Soft delete para entidades referenciadas historicamente

**Codigo Seguro**
- [ ] No hay secretos hardcodeados
- [ ] No hay TODO/FIXME de seguridad pendientes
- [ ] Excepciones manejadas apropiadamente (no swallowed)
- [ ] Recursos cerrados apropiadamente (try-with-resources, finally)

**Multi-tenancy**
- [ ] Queries incluyen filtro de tenant_id
- [ ] FKs usan composite keys con tenant_id donde aplica
- [ ] Tests verifican aislamiento entre tenants

### Code Review de Seguridad

El reviewer debe verificar:

**Categorias Criticas**
1. **Autorizacion**: Quien puede ejecutar esta accion? Hay bypass posible?
2. **Inyeccion**: Input del usuario es sanitizado antes de uso?
3. **Datos sensibles**: Se expone informacion que no deberia?
4. **Tenant isolation**: Puede un tenant acceder a datos de otro?

**Red Flags (Bloquean merge)**
- Queries construidos con concatenacion de strings
- Secretos en codigo o configuracion versionada
- `@PreAuthorize` faltante en endpoints modificadores
- Catch de `Exception` que ignora el error
- Logs con datos PII o tokens
- Input del usuario usado sin validacion

**Yellow Flags (Requieren justificacion)**
- Uso de `@SuppressWarnings` relacionado a seguridad
- Deserializacion de JSON con tipos genericos
- Acceso a archivos del filesystem
- Ejecucion de comandos externos
- Uso de reflection

### Herramientas de Seguridad (CI/CD)

| Herramienta | Tipo | Momento | Accion si Falla |
|-------------|------|---------|-----------------|
| Semgrep | SAST | PR | Bloquea merge |
| OWASP Dependency-Check | SCA | PR + Daily | Bloquea merge (CRITICAL/HIGH) |
| npm audit | SCA | PR | Bloquea merge (critical) |
| Dependabot | SCA | Continuo | Crea PR automatico |
| Snyk (futuro) | SAST+SCA | PR | Bloquea merge |
| OWASP ZAP (futuro) | DAST | Pre-release | Alerta, no bloquea |

### Rotacion de Secretos

| Secreto | Frecuencia | Responsable | Procedimiento |
|---------|------------|-------------|---------------|
| JWT Signing Key (RS256) | Anual | Owner | Generar nuevo par de claves, periodo de gracia 7 dias con ambas validas, actualizar Render env |
| Database Password | Trimestral | Owner | Rotar en Neon, actualizar Render env |
| Twilio API Key | Trimestral | Owner | Regenerar en Twilio, actualizar Render env |
| SendGrid API Key | Trimestral | Owner | Regenerar en SendGrid, actualizar Render env |
| Refresh Tokens | Por uso | Sistema | Rotacion automatica en cada refresh, family tracking para detectar reuso |

---

## Respuesta a Incidentes

### Clasificacion de Incidentes

| Severidad | Descripcion | Tiempo de Respuesta | Ejemplo |
|-----------|-------------|---------------------|---------|
| **CRITICO** | Compromiso activo, datos expuestos | Inmediato (< 1 hora) | Breach de BD, credentials leaked |
| **ALTO** | Vulnerabilidad explotable, sin evidencia de explotacion | < 4 horas | SQLi encontrado en produccion |
| **MEDIO** | Vulnerabilidad potencial, dificil de explotar | < 24 horas | Dependency con CVE medio |
| **BAJO** | Mejora de seguridad, bajo riesgo | < 1 semana | Header de seguridad faltante |

### Procedimiento de Respuesta (CRITICO/ALTO)

1. **Contencion** (primeros 15 min)
   - Evaluar alcance del incidente
   - Si hay compromise activo: revocar tokens comprometidos, rotar credenciales afectadas
   - Preservar logs para investigacion

2. **Notificacion** (dentro de 1 hora)
   - Notificar a owner del proyecto
   - Si PII afectado: preparar notificacion a usuarios afectados

3. **Remediacion** (ASAP)
   - Desarrollar y desplegar fix
   - Verificar que fix es efectivo
   - Monitorear por actividad anomala

4. **Post-mortem** (dentro de 1 semana)
   - Documentar timeline del incidente
   - Identificar root cause
   - Definir mejoras para prevenir recurrencia

---

## Compliance y Regulaciones

### Regulaciones Aplicables (Mexico)

| Regulacion | Aplica | Requisitos Clave | Estado |
|------------|--------|------------------|--------|
| LFPDPPP (Ley Federal de Proteccion de Datos Personales en Posesion de Particulares) | Si | Aviso de privacidad, consentimiento, medidas de seguridad | Pendiente aviso de privacidad |
| SAT (Servicio de Administracion Tributaria) | Si | Retencion de registros fiscales 5 anos, CFDI | Pendiente integracion CFDI |
| PCI-DSS | No | No procesamos tarjetas directamente (solo efectivo en MVP) | N/A |

### Controles LFPDPPP

| Control | Implementacion |
|---------|----------------|
| Aviso de privacidad | Pendiente: documento legal en registro |
| Consentimiento | Checkbox en registro de cliente |
| Acceso a datos | API para que cliente solicite sus datos |
| Rectificacion | API para actualizar datos de cliente |
| Cancelacion | Soft delete de datos de cliente |
| Oposicion | Flag en perfil de cliente |
| Medidas de seguridad | Este documento SECURITY.md |

---

## Checklist de Seguridad por Sprint

Aplicar al inicio y cierre de cada sprint que toque autenticacion, autorizacion, o datos sensibles.

### Preparacion (inicio de sprint)
- [ ] Revisar tareas contra requisitos ASVS relevantes
- [ ] Identificar endpoints con datos sensibles o modificadores de estado
- [ ] Incluir tests de seguridad en Definition of Done

### Durante Desarrollo
- [ ] No hardcodear secretos (usar env vars)
- [ ] Logging sin PII, passwords, ni tokens
- [ ] Parametrizar todas las queries (no concatenacion de strings)
- [ ] Constant-time comparison para secretos (`MessageDigest.isEqual`)
- [ ] Tenant isolation en toda query nueva

### Pre-Merge
- [ ] Semgrep sin warnings de seguridad
- [ ] OWASP Dependency-Check sin CVE criticos
- [ ] Code review enfocado en autorizacion, inyeccion, datos sensibles, tenant isolation

### Post-Merge
- [ ] Tests de seguridad pasan en CI
- [ ] Documentar deuda de seguridad si existe (en `docs/ARCHITECTURE.md#deuda-tecnica`)

---

## Referencias

- [OWASP ASVS 4.0.3](https://github.com/OWASP/ASVS/tree/v4.0.3)
- [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/)
- [NIST SP 800-57 Key Management](https://csrc.nist.gov/publications/detail/sp/800-57-part-1/rev-5/final)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)

---

## Historial de Versiones

| Version | Fecha | Autor | Cambios |
|---------|-------|-------|---------|
| 1.0.0 | 2026-02-09 | Security Architect | Documento inicial con V1 completo |
| 1.1.0 | 2026-02-09 | Security Architect | Actualizado estado Phase 0.2: marcados como completados requisitos 1.1.1, 1.1.6, 1.1.7, 1.2.1, 1.7.1, 1.14.3. Removidas referencias a Auth0. Total cumplidos: 6 -> 12. |
| 2.0.0 | 2026-02-09 | Security Architect | Reorganizacion: requisitos ASVS movidos a `docs/security/asvs/` por capitulo. SECURITY.md ahora contiene solo vision general, threat model, protocolos y compliance. |
