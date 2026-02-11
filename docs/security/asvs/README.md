# OWASP ASVS L2 - Indice de Requisitos

> **Estandar:** OWASP Application Security Verification Standard (ASVS) 4.0.3
> **Nivel Objetivo:** Level 2 - Standard
> **Ultima actualizacion:** 2026-02-10

---

## Resumen de Progreso

| Capitulo | Nombre | Requisitos L2 | Cumplidos | Pendientes | No Aplica | Archivo |
|----------|--------|---------------|-----------|------------|-----------|---------|
| V1 | Architecture, Design and Threat Modeling | 38 | 12 | 26 | 0 | [V01-architecture.md](V01-architecture.md) |
| V2 | Authentication | 57 | 1 | 43 | 13 | [V02-authentication.md](V02-authentication.md) |
| V3 | Session Management | 19 | 0 | 16 | 3 | [V03-session-management.md](V03-session-management.md) |
| V4 | Access Control | 9 | 0 | 9 | 0 | [V04-access-control.md](V04-access-control.md) |
| V5 | Validation, Sanitization and Encoding | 30 | 0 | 24 | 6 | [V05-validation.md](V05-validation.md) |
| V6 | Stored Cryptography | 16 | 0 | 15 | 1 | [V06-cryptography.md](V06-cryptography.md) |
| V7 | Error Handling and Logging | 12 | 4 | 8 | 0 | [V07-error-logging.md](V07-error-logging.md) |
| V8 | Data Protection | 15 | 0 | 15 | 0 | [V08-data-protection.md](V08-data-protection.md) |
| V9 | Communication | 8 | 0 | 8 | 0 | [V09-communication.md](V09-communication.md) |
| V10 | Malicious Code | 9 | 1 | 6 | 2 | [V10-malicious-code.md](V10-malicious-code.md) |
| V11 | Business Logic | 8 | 0 | 8 | 0 | [V11-business-logic.md](V11-business-logic.md) |
| V12 | Files and Resources | 15 | 0 | 5 | 10 | [V12-files-resources.md](V12-files-resources.md) |
| V13 | API and Web Service | 13 | 0 | 9 | 4 | [V13-api.md](V13-api.md) |
| V14 | Configuration | 23 | 2 | 19 | 2 | [V14-configuration.md](V14-configuration.md) |
| **TOTAL** | | **272** | **20** | **211** | **41** | |

**Progreso Global:** 20/272 = **7%** cumplidos (sin contar N/A: 20/231 = **9%**)

---

## Priorizacion por Fase

### Phase 0.3 - Autenticacion (Critico)
- **V2 Authentication** - Todos los requisitos
- **V3 Session Management** - Tokens y cookies
- **V6 Cryptography** - Hashing y random
- **V14 Configuration** - Headers de seguridad

### Phase 0.4 - Frontend
- **V5 Validation** - Input/output encoding
- **V8 Data Protection** - Client-side storage

### Phase 1 - Core POS
- **V4 Access Control** - RBAC y multi-tenancy
- **V5 Validation** - Validacion de negocio
- **V11 Business Logic** - Flujos de pedidos
- **V13 API** - REST security

### Phase 3+ - Funcionalidades Adicionales
- **V7 Error Logging** - Audit completo
- **V9 Communication** - TLS verification
- **V10 Malicious Code** - Integrity checks
- **V12 Files** - Si se implementa upload

---

## Leyenda de Estados

| Simbolo | Significado |
|---------|-------------|
| ✅ | Implementado y verificado |
| ⏳ | Planificado, pendiente de implementacion |
| N/A | No aplica a este proyecto |

---

## Notas Importantes

### Multi-tenancy (Transversal)
La arquitectura multi-tenant afecta multiples capitulos:
- **V1.4** - Access control architecture
- **V4.2** - IDOR protection
- **V13.1** - Authorization en APIs

Todas las queries deben filtrar por `tenant_id`. Composite FKs previenen referencias cross-tenant.

### Autenticacion Nativa
Sin Auth0 ni IdPs externos:
- **V2** - Implementacion completa en Spring Security
- **V3** - JWT + Refresh tokens en BD
- **V2.5** - Recovery via email

### MVP Scope
Algunos requisitos estan fuera del MVP:
- MFA (V2.8) - Sin TOTP/WebAuthn
- File Upload (V12.1-12.4) - Solo URLs externas
- GraphQL (V13.4) - Solo REST
- SOAP (V13.3) - Solo REST

---

## Referencias

- [OWASP ASVS 4.0.3 (GitHub)](https://github.com/OWASP/ASVS/tree/v4.0.3)
- [OWASP ASVS 4.0.3 (PDF)](https://github.com/OWASP/ASVS/raw/v4.0.3/4.0/OWASP%20Application%20Security%20Verification%20Standard%204.0.3-en.pdf)
- [Documento principal de seguridad](../../SECURITY.md)
