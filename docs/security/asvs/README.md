# OWASP ASVS L2 - Indice de Requisitos

> **Estandar:** OWASP Application Security Verification Standard (ASVS) 4.0.3
> **Nivel Objetivo:** Level 2 - Standard
> **Ultima actualizacion:** 2026-02-09

---

## Resumen de Progreso

| Capitulo | Nombre | Requisitos L2 | Cumplidos | Pendientes | Archivo |
|----------|--------|---------------|-----------|------------|---------|
| V1 | Architecture, Design and Threat Modeling | 38 | 12 | 26 | [V01-architecture.md](V01-architecture.md) |
| V2 | Authentication | ~50 | 0 | ~50 | Pendiente |
| V3 | Session Management | ~20 | 0 | ~20 | Pendiente |
| V4 | Access Control | ~15 | 0 | ~15 | Pendiente |
| V5 | Validation, Sanitization and Encoding | ~50 | 0 | ~50 | Pendiente |
| V6 | Stored Cryptography | ~10 | 0 | ~10 | Pendiente |
| V7 | Error Handling and Logging | ~15 | 0 | ~15 | Pendiente |
| V8 | Data Protection | ~15 | 0 | ~15 | Pendiente |
| V9 | Communication | ~10 | 0 | ~10 | Pendiente |
| V10 | Malicious Code | ~5 | 0 | ~5 | Pendiente |
| V11 | Business Logic | ~10 | 0 | ~10 | Pendiente |
| V12 | Files and Resources | ~15 | 0 | ~15 | Pendiente |
| V13 | API and Web Service | ~10 | 0 | ~10 | Pendiente |
| V14 | Configuration | ~10 | 0 | ~10 | Pendiente |
| **TOTAL** | | **~273** | **12** | **~261** | |

> Nota: Los numeros aproximados (~) se actualizaran cuando se documente cada capitulo.

---

## Estado por Capitulo

### Documentados

- **V1 - Architecture**: Completamente documentado. 12/38 requisitos cumplidos (32%).

### Pendientes de Documentar

Los siguientes capitulos se documentaran conforme avance el desarrollo:

- **V2 - Authentication**: Se documentara en Phase 0.3 (modulo de autenticacion)
- **V3 - Session Management**: Se documentara en Phase 0.3
- **V4 - Access Control**: Se documentara en Phase 1 (Core POS)
- **V5 - Validation**: Se documentara progresivamente con cada modulo
- **V6 - Cryptography**: Se documentara en Phase 0.3
- **V7 - Error Handling**: Parcialmente cubierto en V1, se expandira
- **V8 - Data Protection**: Se documentara en Phase 1
- **V9 - Communication**: Se documentara en Phase 0.2 (deploy)
- **V10 - Malicious Code**: Cubierto por CI/CD (Semgrep, Dependency-Check)
- **V11 - Business Logic**: Se documentara en Phase 1
- **V12 - Files**: Se documentara cuando se implemente upload de imagenes
- **V13 - API**: Se documentara en Phase 1
- **V14 - Configuration**: Parcialmente cubierto, se expandira

---

## Leyenda de Estados

| Simbolo | Significado |
|---------|-------------|
| ✅ | Implementado y verificado |
| ⏳ | Planificado, pendiente de implementacion |
| N/A | No aplica a este proyecto |

---

## Referencias

- [OWASP ASVS 4.0.3 (GitHub)](https://github.com/OWASP/ASVS/tree/v4.0.3)
- [OWASP ASVS 4.0.3 (PDF)](https://github.com/OWASP/ASVS/raw/v4.0.3/4.0/OWASP%20Application%20Security%20Verification%20Standard%204.0.3-en.pdf)
- [Documento principal de seguridad](../../SECURITY.md)
