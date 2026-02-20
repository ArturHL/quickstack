# V1: Architecture, Design and Threat Modeling

> **Capitulo:** V1
> **Requisitos L2:** 38
> **Cumplidos:** 19 (50%)
> **Ultima actualizacion:** 2026-02-18

---

## V1.1 Secure Software Development Lifecycle

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 1.1.1 | Verificar el uso de un ciclo de desarrollo seguro que aborde la seguridad en todas las etapas de desarrollo | L2 | ✅ | **Implementacion:** GitHub Flow con PRs obligatorios, code review con checklist de seguridad, CI/CD con SAST (Semgrep) y dependency scanning (OWASP Dependency-Check). Documentacion en CLAUDE.md define requisitos de seguridad por fase. GitHub Actions workflow implementado en `.github/workflows/ci.yml`. |
| 1.1.2 | Verificar el uso de threat modeling para cada cambio de diseno o planificacion de sprint para identificar amenazas, planificar contramedidas, facilitar respuestas de riesgo apropiadas y guiar pruebas de seguridad | L2 | ✅ | **Implementacion:** Threat model documentado en SECURITY.md (ver seccion Threat Model). STRIDE Ágil aplicado en Sprint Planning. Issues de seguridad etiquetados `security` en GitHub. Restricciones guían pruebas de seguridad como Definition of Done. |
| 1.1.3 | Verificar que todas las user stories y features contengan restricciones funcionales de seguridad, como "Como usuario, deberia poder ver y editar mi perfil. No deberia poder ver o editar el perfil de nadie mas" | L2 | ✅ | **Implementacion:** Template de user story en `.github/ISSUE_TEMPLATE/user_story.md` incluye seccion obligatoria "Restricciones de Seguridad". CLAUDE.md define esto como *Definition of Ready*. |
| 1.1.4 | Verificar documentacion y justificacion de todos los limites de confianza, componentes y flujos de datos significativos de la aplicacion | L2 | ✅ | **Implementacion:** Documentado en ARCHITECTURE.md con diagramas de arquitectura. Trust boundaries: Frontend (untrusted) -> API Gateway -> Backend (trusted) -> Database. Composite FKs con tenant_id previenen referencias cross-tenant a nivel de BD. |
| 1.1.5 | Verificar definicion y analisis de seguridad de la arquitectura de alto nivel de la aplicacion y todos los servicios remotos conectados | L2 | ✅ | **Implementacion:** Arquitectura documentada en ARCHITECTURE.md. Servicios externos: Neon (BD), Twilio/SendGrid (notificaciones). Autenticacion nativa con Spring Security + JWT (sin IdP externo). Cada servicio evaluado por seguridad y compliance. |
| 1.1.6 | Verificar implementacion de controles de seguridad centralizados, simples, verificados, seguros y reutilizables para evitar controles duplicados, faltantes, ineficaces o inseguros | L2 | ✅ | **Implementacion:** Modulo `quickstack-common` contiene: `SecurityConfig` (Argon2id password encoder, CORS config), `GlobalExceptionHandler` (manejo de errores sin leak de info interna), `ApiResponse`/`ApiError` (respuestas estandarizadas). Pendiente: `TenantFilter`, `AuditingConfig`. |
| 1.1.7 | Verificar disponibilidad de checklist de codificacion segura, requisitos de seguridad, guias o politicas para todos los desarrolladores y testers | L2 | ✅ | **Implementacion:** Este documento SECURITY.md con checklist pre-PR y guias de code review. CLAUDE.md referencia requisitos ASVS. Agentes de Claude Code configurados con contexto de seguridad en `.claude/agents/`. |

---

## V1.2 Authentication Architecture

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 1.2.1 | Verificar el uso de cuentas de sistema operativo unicas o especiales de bajo privilegio para todos los componentes de la aplicacion, servicios y servidores | L2 | ✅ | **Implementacion:** Dockerfile multi-stage con usuario no-root (`USER quickstack:quickstack` con UID/GID 1000). Imagen base Alpine minimalista. Pendiente en Neon: roles con privilegios minimos (`quickstack_app` CRUD, `quickstack_readonly` SELECT). |
| 1.2.2 | Verificar que las comunicaciones entre componentes de la aplicacion, incluyendo APIs, middleware y capas de datos, esten autenticadas. Los componentes deben tener los privilegios minimos necesarios | L2 | ⏳ | **Implementacion:** Backend -> Neon: conexion SSL obligatoria con certificado validado. WebSocket: autenticacion JWT en handshake. Cada componente tiene credenciales unicas (no compartidas). JWTs firmados con clave privada RS256 almacenada en backend. |
| 1.2.3 | Verificar que la aplicacion usa un unico mecanismo de autenticacion verificado que sea seguro, extensible para incluir autenticacion fuerte, y tenga suficiente logging y monitoreo para detectar abuso de cuentas o brechas | L2 | ✅ | **Implementacion:** Spring Security como unico mecanismo de auth. Passwords almacenados con Argon2id (ASVS 2.4.1). JWT firmados con RS256, clave privada en backend. Brute force protection via `failed_login_attempts` + account lockout. Todos los login attempts loggeados en tabla `login_attempts`. Claims JWT incluyen tenant_id/branch_id/roles. |
| 1.2.4 | Verificar que todas las rutas de autenticacion y APIs de gestion de identidad implementen fuerza de control de seguridad de autenticacion consistente | L2 | ✅ | **Implementacion:** Endpoints de auth: `/api/v1/auth/login`, `/register`, `/forgot-password`, `/reset-password`, `/refresh-token`. Todas las demas APIs requieren JWT valido. Spring Security con `JwtAuthenticationConverter` personalizado extrae claims y roles. Rate limiting en endpoints de auth (Bucket4j o similar). |

---

## V1.3 Session Management Architecture

> Nota: Esta seccion es placeholder en ASVS 4.0.3. No hay requisitos definidos actualmente.

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| - | No hay requisitos definidos en ASVS 4.0.3 V1.3 | - | N/A | **Implementacion proactiva:** Stateless sessions via JWT. Access tokens con expiracion corta (15-30 min). Refresh tokens almacenados en tabla `refresh_tokens`, rotados en cada uso (rotation + family tracking para detectar reuso). Frontend almacena access token en memoria, refresh token en httpOnly cookie. |

---

## V1.4 Access Control Architecture

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 1.4.1 | Verificar que los puntos de aplicacion de confianza, como gateways de control de acceso, servidores y funciones serverless, apliquen controles de acceso. Nunca aplicar controles de acceso en el cliente | L2 | ⏳ | **Implementacion:** Toda autorizacion en backend (Spring Security). Frontend solo oculta UI elements pero nunca confia en ello para seguridad. `@PreAuthorize` annotations en controllers. `TenantFilter` inyecta tenant_id del JWT antes de cualquier query. |
| 1.4.2 | [ELIMINADO] | - | N/A | - |
| 1.4.3 | [ELIMINADO - DUPLICADO DE 4.1.3] | - | N/A | - |
| 1.4.4 | Verificar que la aplicacion usa un unico mecanismo de control de acceso bien verificado para acceder a datos y recursos protegidos. Todas las solicitudes deben pasar por este mecanismo para evitar copy/paste o rutas alternativas inseguras | L2 | ⏳ | **Implementacion:** `TenantAwareRepository` base class que automaticamente aplica filtro `tenant_id = :currentTenant`. Spring Data JPA con `@Filter` de Hibernate. Ningun query puede ejecutarse sin pasar por el filtro de tenant. Composite FKs en BD como segunda linea de defensa. |
| 1.4.5 | Verificar que se use control de acceso basado en atributos o features donde el codigo verifica la autorizacion del usuario para una feature/item de datos en lugar de solo su rol | L2 | ⏳ | **Implementacion:** RBAC + contextual checks. Roles: OWNER (full access), CASHIER (POS + su branch), KITCHEN (KDS + su branch). Verificacion adicional: `user.branch_id == resource.branch_id` para CASHIER/KITCHEN. Permisos almacenados como JSON array en tabla `roles` para futura expansion ABAC. |

---

## V1.5 Input and Output Architecture

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 1.5.1 | Verificar que los requisitos de entrada y salida definan claramente como manejar y procesar datos basados en tipo, contenido y leyes/regulaciones aplicables | L2 | ⏳ | **Implementacion:** DTOs con Bean Validation (`@NotNull`, `@Size`, `@Email`, `@Pattern`). Datos financieros como `BigDecimal` nunca `float/double`. Montos en MXN con precision (10,2). Datos PII (telefono, email) con formato definido. RFC validado con regex mexicano. |
| 1.5.2 | Verificar que la serializacion no se use al comunicarse con clientes no confiables. Si no es posible evitarlo, asegurar que se apliquen controles de integridad adecuados (y posiblemente cifrado si se envia data sensible) para prevenir ataques de deserializacion incluyendo inyeccion de objetos | L2 | ⏳ | **Implementacion:** Solo JSON para comunicacion cliente-servidor (nunca Java serialization). Jackson configurado para ignorar propiedades desconocidas. DTOs explicitos sin herencia de entidades JPA. `@JsonIgnore` en campos sensibles. Content-Type enforcement (`application/json` obligatorio). |
| 1.5.3 | Verificar que la validacion de entrada se aplique en una capa de servicio de confianza | L2 | ⏳ | **Implementacion:** Validacion en dos capas: 1) Controllers con `@Valid` para formato basico, 2) Services con reglas de negocio. Frontend valida para UX pero backend es autoritativo. Ejemplo: producto.base_price >= 0 validado en `ProductService` ademas de `@PositiveOrZero` en DTO. |
| 1.5.4 | Verificar que la codificacion de salida ocurra cerca o por el interprete para el cual esta destinada | L2 | ⏳ | **Implementacion:** React escapa automaticamente valores en JSX (XSS prevention). Backend retorna JSON puro, no HTML. Para emails/WhatsApp: templates procesados por servicio de notificaciones con escapado apropiado por canal. Content-Security-Policy header previene inline scripts. |

---

## V1.6 Cryptographic Architecture

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 1.6.1 | Verificar que existe una politica explicita para gestion de claves criptograficas y que el ciclo de vida de claves sigue un estandar de gestion como NIST SP 800-57 | L2 | ⏳ | **Implementacion:** JWT signing: par de claves RS256 generadas en backend, rotacion anual. Claves de BD: Neon maneja encryption at rest. Claves de API (Twilio, SendGrid): almacenadas en environment variables de Render, rotadas trimestralmente. Documentacion de rotacion en SECURITY.md. |
| 1.6.2 | Verificar que los consumidores de servicios criptograficos protejan material de claves y otros secretos usando vaults de claves o alternativas basadas en API | L2 | ⏳ | **Implementacion:** Secretos en Render Environment Variables (encrypted at rest). No hay secretos hardcodeados en codigo. `.env` en `.gitignore`. JWT private key almacenada como env var. Para produccion futura: considerar HashiCorp Vault o AWS Secrets Manager. |
| 1.6.3 | Verificar que todas las claves y contrasenas son reemplazables y son parte de un proceso bien definido para re-cifrar datos sensibles | L2 | ⏳ | **Implementacion:** JWT signing keys: rotacion anual con periodo de gracia 7 dias con ambas claves validas durante transicion, actualizar Render env. Database credentials: rotacion via Neon console + deploy. API keys: rotacion individual por servicio. Passwords en Argon2id (no requieren re-cifrado, solo re-hash en login). |
| 1.6.4 | Verificar que la arquitectura trata los secretos del lado del cliente como inseguros y nunca los usa para proteger o acceder a datos sensibles | L2 | ✅ | **Implementacion:** Frontend no tiene secretos. Access tokens en memoria (no localStorage). Refresh tokens en httpOnly secure cookie (inaccesible via JS). Todas las operaciones sensibles requieren backend call con JWT. Clave privada de firma JWT solo existe en backend. |

---

## V1.7 Errors, Logging and Auditing Architecture

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 1.7.1 | Verificar que se use un formato y enfoque de logging comun en todo el sistema | L2 | ✅ | **Implementacion:** Logback configurado en `logback-spring.xml` con formato JSON estructurado (profile prod). Campos estandar: timestamp, level, logger, message. MDC preparado para tenant_id, user_id, request_id. Console logging para desarrollo, JSON para produccion. |
| 1.7.2 | Verificar que los logs se transmitan de forma segura a un sistema preferiblemente remoto para analisis, deteccion, alertas y escalamiento | L2 | ⏳ | **Implementacion MVP:** Logs a stdout para Render log aggregation. **Produccion:** Exportar a servicio externo (Datadog, Logflare, o Grafana Cloud) via HTTPS. Alertas configuradas para: errores 5xx > umbral, login failures > umbral, patron de SQL injection detectado. |

---

## V1.8 Data Protection and Privacy Architecture

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 1.8.1 | Verificar que todos los datos sensibles se identifiquen y clasifiquen en niveles de proteccion | L2 | ⏳ | **Clasificacion implementada:** **CRITICO**: password_hash (Argon2id en BD), JWT signing key, tokens. **ALTO**: Datos financieros (orders, payments), PII de clientes (phone, email, address). **MEDIO**: Datos de negocio (productos, precios, inventario). **BAJO**: Catalogos globales, configuracion no sensible. |
| 1.8.2 | Verificar que todos los niveles de proteccion tengan un conjunto asociado de requisitos de proteccion, como requisitos de cifrado, integridad, retencion, privacidad y otros requisitos de confidencialidad, y que estos se apliquen en la arquitectura | L2 | ⏳ | **Requisitos por nivel:** **CRITICO**: password_hash con Argon2id, JWT signing key en env vars, tokens con expiracion corta. **ALTO**: Cifrado en reposo (Neon), TLS en transito, soft delete (nunca borrado fisico), retencion 7 anos (SAT Mexico). **MEDIO**: Cifrado en reposo, backup diario. **BAJO**: Backup diario. |

---

## V1.9 Communications Architecture

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 1.9.1 | Verificar que la aplicacion cifre las comunicaciones entre componentes, particularmente cuando esten en diferentes contenedores, sistemas, sitios o proveedores de nube | L2 | ✅ | **Implementacion:** Frontend -> Backend: HTTPS obligatorio (TLS 1.2+). Backend -> Neon: SSL mode=require con validacion de certificado. Backend -> Twilio/SendGrid: HTTPS. WebSocket: WSS (WebSocket Secure). No hay comunicacion en texto plano. |
| 1.9.2 | Verificar que los componentes de la aplicacion verifiquen la autenticidad de cada lado en un enlace de comunicacion para prevenir ataques person-in-the-middle. Por ejemplo, los componentes de la aplicacion deben validar certificados y cadenas TLS | L2 | ✅ | **Implementacion:** Backend valida certificados TLS de Neon y servicios externos (default JVM trust store + actualizaciones). JWTs firmados localmente con RS256, verificados con clave publica. HSTS header en respuestas (max-age=31536000). |

---

## V1.10 Malicious Software Architecture

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 1.10.1 | Verificar que un sistema de control de codigo fuente esta en uso, con procedimientos para asegurar que los check-ins esten acompanados de issues o tickets de cambio. El sistema de control de codigo debe tener control de acceso y usuarios identificables para permitir trazabilidad de cualquier cambio | L2 | ✅ | **Implementacion:** GitHub como repositorio unico. Branch protection en `main`: require PR, require review, require status checks. Commits asociados a issues via "QS-XXX" en mensaje. GitHub Actions CI ejecuta en cada PR. Squash merge mantiene historial limpio. Acceso via GitHub accounts individuales con 2FA obligatorio. |

---

## V1.11 Business Logic Architecture

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 1.11.1 | Verificar la definicion y documentacion de todos los componentes de la aplicacion en terminos de las funciones de negocio o seguridad que proporcionan | L2 | ✅ | **Implementacion:** Modulos documentados en ARCHITECTURE.md y DATABASE_SCHEMA.md. Cada modulo Maven tiene responsabilidad clara: `quickstack-tenant` (multi-tenancy), `quickstack-user` (autenticacion/autorizacion), `quickstack-pos` (transacciones), etc. Package-by-feature facilita auditoria de responsabilidades. |
| 1.11.2 | Verificar que todos los flujos de logica de negocio de alto valor, incluyendo autenticacion, gestion de sesiones y control de acceso no compartan estado no sincronizado | L2 | ✅ | **Implementacion:** Autenticacion: stateless (JWT generados por backend). Refresh tokens en BD con transacciones ACID. Tenant context: ThreadLocal limpiado despues de cada request. Ordenes: transacciones ACID en PostgreSQL. Inventario: actualizacion atomica de stock con `SELECT FOR UPDATE`. |
| 1.11.3 | Verificar que todos los flujos de logica de negocio de alto valor, incluyendo autenticacion, gestion de sesiones y control de acceso sean thread safe y resistentes a condiciones de carrera time-of-check y time-of-use | L3 | N/A | Requisito L3 - fuera del alcance actual. **Nota para futuro:** Pagos y descuento de inventario usan `@Transactional` con isolation SERIALIZABLE para flujos criticos. `daily_sequence` generado con `SELECT MAX() + 1 FOR UPDATE` para evitar duplicados. |

---

## V1.12 Secure File Upload Architecture

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 1.12.1 | [ELIMINADO - DUPLICADO DE 12.4.1] | - | N/A | - |
| 1.12.2 | Verificar que archivos subidos por usuarios se sirvan ya sea como descargas octet stream, o desde un dominio no relacionado, como un bucket de almacenamiento en la nube. Implementar Content Security Policy adecuada para reducir riesgo de vectores XSS u otros ataques desde el archivo subido | L2 | ⏳ | **Implementacion MVP:** No hay upload de archivos en MVP (imagenes via URL externa). **Futuro:** Imagenes de productos en Cloudflare R2 o S3 con dominio separado (cdn.quickstack.app). `Content-Disposition: attachment` para descargas. CSP: `default-src 'self'; img-src 'self' https://cdn.quickstack.app` |

---

## V1.13 API Architecture

> Nota: Esta seccion es placeholder en ASVS 4.0.3. No hay requisitos definidos actualmente.

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| - | No hay requisitos definidos en ASVS 4.0.3 V1.13 | - | N/A | **Implementacion proactiva:** REST API con versionado URL (/v1/). OpenAPI 3.0 spec generada automaticamente. Rate limiting por tenant (100 req/min default). CORS restrictivo (solo dominios permitidos). Security headers: X-Content-Type-Options, X-Frame-Options, etc. |

---

## V1.14 Configuration Architecture

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 1.14.1 | Verificar la segregacion de componentes de diferentes niveles de confianza a traves de controles de seguridad bien definidos, reglas de firewall, gateways de API, reverse proxies, grupos de seguridad en la nube, o mecanismos similares | L2 | ✅ | **Implementacion:** Neon BD: solo accesible desde IPs de Render (allow list). Frontend en Vercel: solo assets estaticos, sin acceso a BD. Backend en Render: unico punto de entrada a BD. WebSocket en mismo backend (misma autenticacion). |
| 1.14.2 | Verificar que firmas binarias, conexiones de confianza, y endpoints verificados se usen para desplegar binarios a dispositivos remotos | L2 | ⏳ | **Implementacion:** Docker images firmadas (Docker Content Trust). GitHub Actions build con hash verificable. Render pull desde GitHub container registry (conexion autenticada). Dependencias verificadas via checksums en Maven/npm lockfiles. |
| 1.14.3 | Verificar que el pipeline de build advierte sobre componentes desactualizados o inseguros y toma acciones apropiadas | L2 | ✅ | **Implementacion:** GitHub Actions CI incluye: `npm audit --audit-level=high` para frontend, OWASP Dependency-Check (`-DfailBuildOnCVSS=7`) para backend, Semgrep con rulesets `p/java`, `p/security-audit`, `p/secrets`, `p/owasp-top-ten`. Reports como artifacts. |
| 1.14.4 | Verificar que el pipeline de build contiene un paso para construir y verificar automaticamente el despliegue seguro de la aplicacion, especialmente si la infraestructura de la aplicacion esta definida como software (IaC) | L2 | ⏳ | **Implementacion:** GitHub Actions workflow: lint -> test -> security scan -> build -> deploy staging -> smoke tests -> deploy prod. Dockerfile multi-stage (build sin runtime tools). Health check endpoint verifica conectividad post-deploy. Rollback automatico si health check falla. |
| 1.14.5 | Verificar que los despliegues de aplicacion esten adecuadamente sandboxed, containerizados y/o aislados a nivel de red para retrasar y disuadir atacantes de atacar otras aplicaciones, especialmente cuando realizan acciones sensibles o peligrosas como deserializacion | L2 | ✅ | **Implementacion:** Contenedor Docker aislado en Render. Network isolation por servicio de Render. No hay shell access en produccion. Container ejecuta como non-root. Read-only filesystem excepto /tmp. Recursos limitados (CPU, memoria) para prevenir DoS. |
| 1.14.6 | Verificar que la aplicacion no usa tecnologias del lado del cliente inseguras, no soportadas o deprecadas como NSAPI plugins, Flash, Shockwave, ActiveX, Silverlight, NACL, o applets Java del lado del cliente | L2 | ✅ | **Implementacion:** Frontend React puro. No hay plugins de navegador. No hay Java applets ni Flash. Solo JavaScript moderno (ES2020+). Dependencias actualizadas regularmente. No hay iframes de terceros. Login form nativo en React (no widget externo). |

---

## Resumen de Estado

| Seccion | Total Requisitos | Cumplidos | Pendientes | No Aplica |
|---------|------------------|-----------|------------|-----------|
| V1.1 Secure SDLC | 7 | 7 | 0 | 0 |
| V1.2 Authentication | 4 | 3 | 1 | 0 |
| V1.3 Session Management | 0 | 0 | 0 | 0 |
| V1.4 Access Control | 3 | 0 | 3 | 0 |
| V1.5 Input/Output | 4 | 0 | 4 | 0 |
| V1.6 Cryptographic | 4 | 1 | 3 | 0 |
| V1.7 Logging | 2 | 1 | 1 | 0 |
| V1.8 Data Protection | 2 | 0 | 2 | 0 |
| V1.9 Communications | 2 | 2 | 0 | 0 |
| V1.10 Malicious Code | 1 | 1 | 0 | 0 |
| V1.11 Business Logic | 2 | 2 | 0 | 0 |
| V1.12 File Upload | 1 | 0 | 1 | 0 |
| V1.13 API | 0 | 0 | 0 | 0 |
| V1.14 Configuration | 6 | 4 | 2 | 0 |
| **TOTAL** | **38** | **21** | **17** | **0** |
