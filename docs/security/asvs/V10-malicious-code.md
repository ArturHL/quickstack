# V10: Malicious Code

> **Capitulo:** V10
> **Requisitos L2:** 6
> **Cumplidos:** 1 (17%)
> **Ultima actualizacion:** 2026-02-10

---

## V10.1 Code Integrity

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 10.1.1 | Verificar que se use una herramienta de analisis de codigo para detectar codigo potencialmente malicioso, como funciones de tiempo, operaciones de archivos inseguras, y conexiones de red | L2 | ✅ | **Implementado:** Semgrep en CI con rulesets `p/java`, `p/security-audit`, `p/secrets`, `p/owasp-top-ten`. Bloquea PR si detecta issues. |

---

## V10.2 Malicious Code Search

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 10.2.1 | Verificar que el codigo fuente de la aplicacion y las bibliotecas de terceros no contengan puertas traseras no autorizadas por telefono a casa o capacidades de recopilacion de datos. Donde se encuentre tal funcionalidad, obtener permiso del usuario antes de recopilar cualquier dato | L2 | ⏳ | **Pendiente Phase 1:** Dependency scanning con OWASP Dependency-Check. Review de dependencias nuevas. Sin telemetria oculta. Consentimiento explicito para analytics. |
| 10.2.2 | Verificar que la aplicacion no solicite permisos innecesarios o excesivos para caracteristicas o sensores relacionados con privacidad, como contactos, camaras, microfonos, o ubicacion | L2 | N/A | **No aplica:** Aplicacion web, no app movil. Sin acceso a sensores. Solo permisos de navegador estandar (clipboard para copiar). |
| 10.2.3 | Verificar que el codigo fuente de la aplicacion y las bibliotecas de terceros no contengan bombas de tiempo buscando fechas de reloj y calendario asociadas a condiciones o funciones | L2 | ⏳ | **Pendiente Phase 1:** Code review de nuevas dependencias. Semgrep detecta patrones sospechosos de tiempo. Sin logica basada en fechas especificas hardcoded. |
| 10.2.4 | Verificar que el codigo fuente de la aplicacion y las bibliotecas de terceros no contengan codigo malicioso, como ataques de salami, bypasses logicos, o bombas logicas | L2 | ⏳ | **Pendiente Phase 1:** PRs con code review obligatorio. SAST en CI. Validacion de logica de negocio en tests. Principio de menor privilegio. |
| 10.2.5 | Verificar que el codigo fuente de la aplicacion y las bibliotecas de terceros no contengan Huevos de Pascua o cualquier otra funcionalidad potencialmente no deseada | L2 | ⏳ | **Pendiente Phase 1:** Code review. Sin endpoints ocultos. Documentacion de todas las rutas API. Semgrep para deteccion de patrones anomalos. |

---

## V10.3 Application Integrity

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 10.3.1 | Verificar que si la aplicacion tiene una caracteristica de actualizacion automatica del cliente o servidor, las actualizaciones se obtengan sobre canales seguros y se firmen digitalmente. El codigo de actualizacion debe validar la firma digital de la actualizacion antes de instalar o ejecutar la actualizacion | L1 | N/A | **No aplica:** Sin auto-update. Deploys via CI/CD controlado. Usuarios acceden via web (siempre version actual). |
| 10.3.2 | Verificar que la aplicacion emplee protecciones de integridad, como firma de codigo o integridad de subrecurso. La aplicacion no debe cargar o ejecutar codigo de fuentes no confiables, como cargar includes, modulos, plugins, codigo, o bibliotecas de fuentes no confiables o de Internet | L1 | ⏳ | **Pendiente Phase 0.4:** Subresource Integrity (SRI) en scripts externos (si hay). NPM lockfile con checksums. Maven dependencies con checksums verificados. |
| 10.3.3 | Verificar que la aplicacion tenga proteccion contra toma de control de subdominios si la aplicacion depende de entradas DNS o subdominios DNS, como nombres CNAME expirados, punteros DNS obsoletos o CNAMEs, o proyectos expirados en repositorios de codigo fuente publico | L1 | ⏳ | **Pendiente Phase 6:** Monitoreo de DNS. Sin subdominios dangling. Render y Vercel manejan DNS correctamente. Verificacion periodica de config DNS. |

---

## Resumen de Estado

| Seccion | Total Requisitos | Cumplidos | Pendientes | No Aplica |
|---------|------------------|-----------|------------|-----------|
| V10.1 Code Integrity | 1 | 1 | 0 | 0 |
| V10.2 Malicious Code Search | 5 | 0 | 4 | 1 |
| V10.3 Application Integrity | 3 | 0 | 2 | 1 |
| **TOTAL** | **9** | **1** | **6** | **2** |
