# V14: Configuration

> **Capitulo:** V14
> **Requisitos L2:** 17
> **Cumplidos:** 2 (12%)
> **Ultima actualizacion:** 2026-02-10

---

## V14.1 Build and Deploy

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 14.1.1 | Verificar que los procesos de construccion y despliegue de la aplicacion se realicen de manera segura y repetible, como automatizacion de CI/CD, gestion de configuracion automatizada, y scripts de despliegue automatizados | L2 | ✅ | **Implementado:** GitHub Actions CI/CD. Docker multi-stage builds. Render auto-deploy desde main. Vercel auto-deploy. Configuracion via env vars. |
| 14.1.2 | Verificar que las banderas del compilador esten configuradas para habilitar todas las protecciones y advertencias de desbordamiento de buffer disponibles, incluyendo terminacion de pila aleatoria, prevencion de ejecucion de datos, y para romper la construccion si se encuentra un puntero, memoria, cadena de formato, entero, u operacion de cadena insegura | L2 | N/A | **No aplica:** Java es memory-safe. Sin compilacion de codigo nativo. JVM maneja protecciones de memoria. |
| 14.1.3 | Verificar que la configuracion del servidor este endurecida segun las recomendaciones del servidor de aplicaciones y frameworks en uso | L2 | ⏳ | **Pendiente Phase 0.3:** Spring Boot hardening: actuator seguro, error pages sin leak, headers de seguridad. Docker non-root user. |
| 14.1.4 | Verificar que la aplicacion, configuracion, y todas las dependencias puedan ser re-desplegadas usando scripts de despliegue automatizados, construidas desde un runbook documentado en un tiempo razonable, o restauradas desde backups de manera oportuna | L2 | ⏳ | **Pendiente Phase 6:** Runbook documentado. Restore de Neon snapshots probado. CI/CD permite redeploy en minutos. Infrastructure as code. |
| 14.1.5 | Verificar que los administradores autorizados puedan verificar la integridad de todas las configuraciones relevantes de seguridad para detectar manipulacion | L3 | N/A | **L3 - Fuera de alcance MVP.** |

---

## V14.2 Dependency

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 14.2.1 | Verificar que todos los componentes esten actualizados, preferiblemente usando un verificador de dependencias durante el tiempo de construccion o compilacion | L1 | ✅ | **Implementado:** OWASP Dependency-Check en CI. npm audit para frontend. Dependabot habilitado. PR bloqueado si CVSS >= 7. |
| 14.2.2 | Verificar que toda funcionalidad innecesaria, documentacion, aplicaciones de muestra y configuraciones sean eliminadas | L1 | ⏳ | **Pendiente Phase 0.3:** Spring Boot sin actuator endpoints innecesarios. Sin /swagger en prod. Sin ejemplos. Docker slim image. |
| 14.2.3 | Verificar que si los activos de la aplicacion, como bibliotecas JavaScript, fuentes CSS o web, se alojan externamente en una Red de Entrega de Contenido (CDN) o proveedor externo, se use Integridad de Subrecurso (SRI) para validar la integridad del activo | L1 | ⏳ | **Pendiente Phase 0.4:** SRI hashes para scripts externos (si hay). NPM packages locales en bundle. Sin CDN para JS critico. |
| 14.2.4 | Verificar que los componentes de terceros provengan de repositorios predefinidos, confiables y mantenidos continuamente | L2 | ⏳ | **Pendiente Phase 1:** Maven Central para Java. NPM registry para JS. Sin repos privados no verificados. Lockfiles para reproducibilidad. |
| 14.2.5 | Verificar que se mantenga un catastro de Software Bill of Materials (SBOM) de todas las bibliotecas de terceros en uso | L2 | ⏳ | **Pendiente Phase 1:** SBOM generado con CycloneDX maven plugin. Exportado en cada release. Almacenado con artefactos de build. |
| 14.2.6 | Verificar que la superficie de ataque se reduzca al encapsular bibliotecas de terceros para exponer solo el comportamiento requerido en la aplicacion | L2 | ⏳ | **Pendiente Phase 1:** Wrappers para integraciones externas (EmailService, WhatsAppService). Sin exposicion directa de SDK. Interfaces definidas. |

---

## V14.3 Unintended Security Disclosure

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 14.3.1 | [ELIMINADO - DUPLICADO DE 7.4.1] | - | N/A | - |
| 14.3.2 | Verificar que los modos de depuracion del servidor web o de aplicaciones y del framework de la aplicacion esten deshabilitados en produccion para eliminar caracteristicas de depuracion, consolas de desarrollador, y divulgaciones de seguridad no intencionales | L1 | ⏳ | **Pendiente Phase 0.3:** `SPRING_PROFILES_ACTIVE=prod`. Debug logging deshabilitado. DevTools deshabilitado. Stack traces no expuestos. |
| 14.3.3 | Verificar que los headers HTTP o cualquier parte de la respuesta HTTP no expongan informacion detallada de version de componentes del sistema | L1 | ⏳ | **Pendiente Phase 0.3:** `server.server-header` vacio. Sin `X-Powered-By`. Sin version de Spring en headers. |

---

## V14.4 HTTP Security Headers

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 14.4.1 | Verificar que cada respuesta HTTP contenga un header Content-Type con un charset seguro (por ejemplo, UTF-8, ISO-8859-1) | L1 | ⏳ | **Pendiente Phase 0.3:** `Content-Type: application/json; charset=utf-8` en todas las respuestas. Configurado en Spring. |
| 14.4.2 | Verificar que todas las respuestas de API contengan un header Content-Disposition: attachment; filename="api.json" (u otro nombre de archivo apropiado para el tipo de contenido) | L1 | ⏳ | **Pendiente Phase 1:** APIs de descarga con `Content-Disposition`. Endpoints normales retornan JSON inline. |
| 14.4.3 | Verificar que exista un header de Politica de Seguridad de Contenido (CSP) que ayude a mitigar el impacto de ataques XSS como vulnerabilidades de inyeccion HTML, DOM, JSON, y JavaScript | L1 | ⏳ | **Pendiente Phase 0.4:** CSP header: `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'`. Ajustar segun necesidades del frontend. |
| 14.4.4 | Verificar que todas las respuestas contengan un header X-Content-Type-Options: nosniff | L1 | ⏳ | **Pendiente Phase 0.3:** Spring Security agrega por defecto. Verificar en respuestas. Previene MIME sniffing. |
| 14.4.5 | Verificar que un header Strict-Transport-Security este incluido en todas las respuestas y para todos los subdominios, como Strict-Transport-Security: max-age=15724800; includeSubdomains | L1 | ⏳ | **Pendiente Phase 0.3:** HSTS header con max-age=31536000. `includeSubDomains`. Preload list (futuro). |
| 14.4.6 | Verificar que un header "Referrer-Policy" adecuado este incluido para evitar exponer informacion sensible en la URL a traves del header Referer a partes no confiables | L1 | ⏳ | **Pendiente Phase 0.3:** `Referrer-Policy: strict-origin-when-cross-origin`. Configurado en Spring Security. |
| 14.4.7 | Verificar que el contenido de una aplicacion web no pueda ser incrustado en un sitio de terceros por defecto y que la incrustacion de los recursos exactos solo se permita donde sea necesario usando el header de respuesta HTTP Content-Security-Policy: frame-ancestors adecuado o X-Frame-Options | L1 | ⏳ | **Pendiente Phase 0.3:** `X-Frame-Options: DENY` o CSP `frame-ancestors 'none'`. Previene clickjacking. |

---

## V14.5 HTTP Request Header Validation

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 14.5.1 | Verificar que el servidor de aplicaciones solo acepte los metodos HTTP en uso por la aplicacion/API, incluyendo pre-flight OPTIONS, y logs/alertas en cualquier solicitud que no sea valida para el contexto de la aplicacion | L1 | ⏳ | **Pendiente Phase 1:** CORS config con metodos permitidos. Logging de metodos rechazados. 405 Method Not Allowed para metodos invalidos. |
| 14.5.2 | Verificar que el header Origin suministrado no se use para decisiones de autenticacion o control de acceso, ya que el header Origin puede ser facilmente cambiado por un atacante | L1 | ⏳ | **Pendiente Phase 0.3:** Origin solo para CORS, no para auth. Auth via JWT en Authorization header. Tenant_id del JWT, no del request. |
| 14.5.3 | Verificar que el header Access-Control-Allow-Origin de Comparticion de Recursos de Origen Cruzado (CORS) use una lista estricta de dominios confiables y subdominios para coincidir y no soporte el origen "null" | L1 | ⏳ | **Pendiente Phase 0.3:** CORS whitelist en `app.cors.allowed-origins`. Sin wildcard `*`. Sin `null`. Solo dominios de produccion y localhost para dev. |
| 14.5.4 | Verificar que los headers HTTP agregados por un proxy de confianza o dispositivos SSO, como un token de portador, sean autenticados por la aplicacion | L2 | N/A | **No aplica:** Sin proxy de confianza agregando headers de auth. JWT manejado directamente por la aplicacion. |

---

## Resumen de Estado

| Seccion | Total Requisitos | Cumplidos | Pendientes | No Aplica |
|---------|------------------|-----------|------------|-----------|
| V14.1 Build and Deploy | 4 | 1 | 2 | 1 |
| V14.2 Dependency | 6 | 1 | 5 | 0 |
| V14.3 Unintended Disclosure | 2 | 0 | 2 | 0 |
| V14.4 HTTP Security Headers | 7 | 0 | 7 | 0 |
| V14.5 HTTP Request Header | 4 | 0 | 3 | 1 |
| **TOTAL** | **23** | **2** | **19** | **2** |
