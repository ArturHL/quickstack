# V2: Authentication

> **Capitulo:** V2
> **Requisitos L2:** 52
> **Cumplidos:** 0 (0%)
> **Ultima actualizacion:** 2026-02-10

---

## V2.1 Password Security

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 2.1.1 | Verificar que las contrasenas establecidas por el usuario tengan al menos 12 caracteres de longitud (despues de combinar espacios multiples) | L1 | ⏳ | **Pendiente Phase 0.3:** Validacion en `RegisterRequest` DTO con `@Size(min = 12)`. Configurado en `app.security.password-min-length=12`. |
| 2.1.2 | Verificar que se permitan contrasenas de al menos 64 caracteres y que se denieguen contrasenas de mas de 128 caracteres | L1 | ⏳ | **Pendiente Phase 0.3:** `@Size(min = 12, max = 128)` en DTO. Sin limite arbitrario inferior a 64. |
| 2.1.3 | Verificar que no se trunque la contrasena. Sin embargo, espacios multiples consecutivos pueden reemplazarse por un solo espacio | L1 | ⏳ | **Pendiente Phase 0.3:** Password procesado tal cual sin truncamiento. Normalizacion de espacios multiples. |
| 2.1.4 | Verificar que cualquier caracter Unicode imprimible, incluyendo caracteres neutrales de idioma como espacios y Emojis, este permitido en contrasenas | L1 | ⏳ | **Pendiente Phase 0.3:** Sin restriccion de charset. UTF-8 completo permitido. |
| 2.1.5 | Verificar que los usuarios puedan cambiar su contrasena | L1 | ⏳ | **Pendiente Phase 0.3:** Endpoint `PUT /api/v1/users/me/password`. Requiere contrasena actual. |
| 2.1.6 | Verificar que la funcionalidad de cambio de contrasena requiera la contrasena actual y la nueva del usuario | L1 | ⏳ | **Pendiente Phase 0.3:** `ChangePasswordRequest` con `currentPassword` y `newPassword`. |
| 2.1.7 | Verificar que las contrasenas enviadas durante el registro, login y cambio de contrasena sean verificadas contra un conjunto de contrasenas violadas ya sea localmente o usando una API externa. Si se usa API, debe usarse un protocolo zero-knowledge u otra proteccion de privacidad | L1 | ⏳ | **Pendiente Phase 0.3:** Integracion con HaveIBeenPwned API usando k-Anonymity (solo primeros 5 chars del hash SHA-1). |
| 2.1.8 | Verificar que se proporcione un medidor de fuerza de contrasena para ayudar a los usuarios a establecer una contrasena mas fuerte | L1 | ⏳ | **Pendiente Phase 0.4:** Frontend con biblioteca `zxcvbn` para feedback visual de fortaleza. |
| 2.1.9 | Verificar que no haya reglas de composicion de contrasenas que limiten el tipo de caracteres permitidos. No debe haber requisito de mayusculas, minusculas, numeros o caracteres especiales | L1 | ⏳ | **Pendiente Phase 0.3:** Sin reglas de composicion. Solo longitud minima de 12 caracteres. |
| 2.1.10 | Verificar que no haya requisitos de rotacion periodica de contrasenas o historial de contrasenas | L1 | ⏳ | **Pendiente Phase 0.3:** Sin rotacion forzada. Campo `password_changed_at` solo informativo. Sin historial de passwords. |
| 2.1.11 | Verificar que la funcionalidad "pegar" en campos de contrasena, ayudas de navegador para contrasenas, y gestores de contrasenas externos esten permitidos | L1 | ⏳ | **Pendiente Phase 0.4:** Frontend sin `autocomplete="off"` ni bloqueo de paste en campos de password. |
| 2.1.12 | Verificar que el usuario pueda elegir ver temporalmente la contrasena enmascarada completa, o ver temporalmente el ultimo caracter escrito de la contrasena | L1 | ⏳ | **Pendiente Phase 0.4:** Icono "ojo" en campos de password para toggle de visibilidad. |

---

## V2.2 General Authenticator Security

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 2.2.1 | Verificar que los controles anti-automatizacion sean efectivos para mitigar ataques de breach testing, brute force y account lockout. Tales controles incluyen bloquear las contrasenas violadas mas comunes, soft lockouts, rate limiting, CAPTCHA, delays crecientes entre intentos, restricciones de IP, o restricciones basadas en riesgo como ubicacion, primer login en dispositivo, intentos recientes de desbloqueo, o similares | L1 | ⏳ | **Pendiente Phase 0.3:** Bucket4j para rate limiting (10 intentos/min por IP, 5 por email). Account lockout despues de 5 intentos fallidos (15 min). Registro en `login_attempts`. Check contra HaveIBeenPwned. |
| 2.2.2 | Verificar que el uso de autenticadores debiles (como SMS y email) se limite a verificacion secundaria y aprobacion de transacciones y no como reemplazo de metodos de autenticacion mas seguros. Verificar que se ofrezcan metodos mas fuertes antes que los debiles, que los usuarios esten conscientes de los riesgos, o que medidas apropiadas esten en lugar para limitar riesgos de compromiso de cuenta | L1 | ⏳ | **Pendiente Phase 0.3:** Email solo para password recovery (no como 2FA). SMS no usado. Password reset tokens con expiracion de 1 hora. |
| 2.2.3 | Verificar que notificaciones seguras se envien a los usuarios despues de actualizaciones a detalles de autenticacion, como reseteo de credenciales, cambios de email o direccion, login desde ubicaciones desconocidas o riesgosas. Se prefiere el uso de notificaciones push sobre SMS o email, pero en ausencia de push, SMS o email son aceptables mientras no haya informacion sensible en la notificacion | L1 | ⏳ | **Pendiente Phase 0.3:** Email de notificacion en: password change, password reset request, login desde nueva IP. Sin detalles sensibles en notificacion. |
| 2.2.4 | Verificar resistencia a impersonacion contra phishing, como el uso de autenticacion multi-factor, dispositivos criptograficos con intencion (como claves conectadas con push para autenticar), o en niveles AAL mas altos, certificados del lado del cliente | L2 | ⏳ | **MVP:** No MFA (fuera de alcance). Mitigaciones: HTTPS obligatorio, HSTS, login attempts logging para detectar anomalias. **Futuro:** TOTP como opcion para OWNER. |
| 2.2.5 | Verificar que donde un Proveedor de Servicio de Credenciales (CSP) y la aplicacion que verifica autenticacion estan separados, haya TLS mutuamente autenticado entre los dos endpoints | L2 | N/A | **No aplica:** Autenticacion nativa, no hay CSP externo. Spring Security es parte del mismo backend. |
| 2.2.6 | Verificar proteccion contra replay resistance a traves del uso obligatorio de dispositivos OTP, autenticadores criptograficos, o codigos de lookup | L2 | ⏳ | **Pendiente Phase 0.3:** JWTs con `jti` (JWT ID) unico. Refresh tokens de un solo uso (rotacion). Nonces en password reset. |
| 2.2.7 | Verificar intencion de autenticar requiriendo la entrada de un token OTP o accion iniciada por usuario como presionar un boton en una llave de hardware FIDO | L2 | ⏳ | **MVP:** Intencion via submit de formulario con password. No FIDO en MVP. **Futuro:** Soporte para WebAuthn. |

---

## V2.3 Authenticator Lifecycle

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 2.3.1 | Verificar que las contrasenas iniciales o codigos de activacion generados por el sistema DEBEN ser generados aleatoriamente de forma segura, DEBEN tener al menos 6 caracteres, PUEDEN contener letras y numeros, y DEBEN expirar despues de un corto periodo de tiempo. Estos secretos iniciales no deben permitirse como contrasena a largo plazo | L1 | ⏳ | **Pendiente Phase 0.3:** Reset tokens: 32 bytes aleatorios (SecureRandom), 1 hora expiracion, single-use. Passwords iniciales no usados (usuario crea su propio password en registro). |
| 2.3.2 | Verificar que se soporte la inscripcion y uso de dispositivos de autenticacion proporcionados por el suscriptor, como tokens U2F o FIDO | L2 | ⏳ | **Fuera de MVP:** WebAuthn/FIDO2 considerado para fase posterior. Diseno de BD permite extension futura (tabla `user_authenticators`). |
| 2.3.3 | Verificar que las instrucciones de renovacion se envien con tiempo suficiente para renovar autenticadores de tiempo limitado | L2 | N/A | **No aplica:** No hay autenticadores de tiempo limitado (hardware tokens, certificados). JWTs se renuevan automaticamente via refresh token. |

---

## V2.4 Credential Storage

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 2.4.1 | Verificar que las contrasenas se almacenen en una forma que sea resistente a ataques offline. Las contrasenas DEBEN usar salt con una funcion de derivacion de clave aprobada y segura o funcion de hashing de contrasenas. Las funciones de derivacion de clave y hashing de contrasenas toman una contrasena, un salt, y un factor de costo como entradas al generar un hash de contrasena | L1 | ⏳ | **Pendiente Phase 0.3:** Argon2id configurado en `SecurityConfig`. Parametros: memory=64MB, iterations=3, parallelism=1. Salt automatico por Spring Security. |
| 2.4.2 | Verificar que el salt tenga al menos 32 bits de longitud y sea elegido arbitrariamente para minimizar colisiones de salt entre hashes almacenados. Para cada credencial, debe almacenarse un salt unico y el hash resultante | L1 | ⏳ | **Pendiente Phase 0.3:** Spring Security Argon2id usa salt de 16 bytes (128 bits) por defecto, generado con SecureRandom. |
| 2.4.3 | Verificar que si se usa PBKDF2, el conteo de iteraciones DEBE ser tan grande como el rendimiento del servidor de verificacion permita, tipicamente al menos 100,000 iteraciones | L1 | N/A | **No aplica:** Usamos Argon2id, no PBKDF2. Argon2id es la recomendacion actual de OWASP. |
| 2.4.4 | Verificar que si se usa bcrypt, el factor de trabajo DEBE ser tan grande como el rendimiento del servidor permita, con un minimo de 10 | L1 | N/A | **No aplica:** Usamos Argon2id, no bcrypt. |
| 2.4.5 | Verificar que una iteracion adicional de una funcion de derivacion de clave se realice, usando un valor de salt secreto y conocido solo por el verificador. Generar el valor de salt usando un generador de bits aleatorios aprobado [SP 800-90Ar1] y proporcionar al menos la fuerza de seguridad minima especificada en la ultima revision de SP 800-131A. El valor secreto de salt DEBE almacenarse separadamente de las contrasenas hasheadas (por ejemplo, en un dispositivo especializado como HSM) | L2 | ⏳ | **Pendiente Phase 0.3:** Pepper (salt global secreto) almacenado en variable de entorno separada de BD. Aplicado antes de hash Argon2id. |

---

## V2.5 Credential Recovery

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 2.5.1 | Verificar que un secreto de activacion inicial o recuperacion generado por el sistema no se envie en texto claro al usuario | L1 | ⏳ | **Pendiente Phase 0.3:** Token de reset hasheado en BD (SHA-256). URL con token enviada por email via HTTPS. Token original solo visible una vez en email. |
| 2.5.2 | Verificar que pistas de contrasena o autenticacion basada en conocimiento (llamadas "preguntas secretas") no esten presentes | L1 | ⏳ | **Pendiente Phase 0.3:** Sin preguntas secretas. Solo email para recuperacion. |
| 2.5.3 | Verificar que la recuperacion de credenciales de contrasena no revele la contrasena actual de ninguna manera | L1 | ⏳ | **Pendiente Phase 0.3:** Reset crea nueva contrasena, no revela la anterior. Password anterior invalidado inmediatamente. |
| 2.5.4 | Verificar que cuentas compartidas o por defecto no esten presentes (por ejemplo "root", "admin", o "sa") | L1 | ⏳ | **Pendiente Phase 0.3:** Sin cuentas por defecto. Cada usuario creado con credenciales unicas. Seed data no incluye usuarios. |
| 2.5.5 | Verificar que si un factor de autenticacion se cambia o reemplaza, el usuario sea notificado de este evento | L1 | ⏳ | **Pendiente Phase 0.3:** Email de notificacion enviado cuando password cambia (via change o reset). |
| 2.5.6 | Verificar que recuperacion de contrasena olvidada y otras rutas de recuperacion usen un mecanismo de recuperacion seguro, como OTP basado en tiempo (TOTP) u otra soft token, push movil, u otro mecanismo de recuperacion offline | L1 | ⏳ | **Pendiente Phase 0.3:** Token unico de 32 bytes via email. Expira en 1 hora. Single-use (marcado `used_at` despues de uso). |
| 2.5.7 | Verificar que si se pierden factores de autenticacion OTP o multi-factor, la evidencia de prueba de identidad se realice al mismo nivel que durante la inscripcion | L2 | N/A | **No aplica MVP:** Sin MFA en MVP. Recuperacion solo requiere acceso a email registrado. |

---

## V2.6 Look-up Secret Verifier

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 2.6.1 | Verificar que los secretos de lookup solo puedan usarse una vez | L2 | ⏳ | **Pendiente Phase 0.3:** Password reset tokens marcados con `used_at` timestamp. Rechazados si ya usados. |
| 2.6.2 | Verificar que los secretos de lookup tengan suficiente aleatoriedad (112 bits de entropia), o si tienen menos de 112 bits de entropia, sean salteados con un salt unico y aleatorio de 32 bits y hasheados con una funcion hash unidireccional aprobada | L2 | ⏳ | **Pendiente Phase 0.3:** Reset tokens de 32 bytes (256 bits). Hasheados con SHA-256 antes de almacenar en BD. |
| 2.6.3 | Verificar que los secretos de lookup sean resistentes a ataques offline, como valores predecibles | L2 | ⏳ | **Pendiente Phase 0.3:** Generados con SecureRandom. Hasheados en BD. Expiracion de 1 hora. |

---

## V2.7 Out of Band Verifier

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 2.7.1 | Verificar que autenticadores de texto claro fuera de banda (NIST "restringido"), como SMS o PSTN, no se ofrezcan por defecto, y que se ofrezcan primero alternativas mas fuertes como notificaciones push | L1 | ⏳ | **Pendiente Phase 0.3:** Solo email para verificacion/recuperacion. SMS no usado. |
| 2.7.2 | Verificar que el verificador fuera de banda expire solicitudes, codigos o tokens de autenticacion fuera de banda despues de 10 minutos | L1 | ⏳ | **Pendiente Phase 0.3:** Reset tokens expiran en 1 hora (mas estricto para email verification: 24 horas). |
| 2.7.3 | Verificar que las solicitudes, codigos o tokens de autenticacion del verificador fuera de banda solo sean usables una vez, y solo para la solicitud de autenticacion original | L1 | ⏳ | **Pendiente Phase 0.3:** Token vinculado a user_id especifico. Single-use via campo `used_at`. |
| 2.7.4 | Verificar que el autenticador y verificador fuera de banda se comuniquen sobre un canal independiente seguro | L1 | ⏳ | **Pendiente Phase 0.3:** Email enviado via SMTP sobre TLS (SendGrid/SES). Canal separado de la aplicacion web. |
| 2.7.5 | Verificar que el verificador fuera de banda solo retenga una version hasheada del codigo de autenticacion | L2 | ⏳ | **Pendiente Phase 0.3:** Token hasheado con SHA-256 en tabla `password_reset_tokens`. Original enviado al usuario. |
| 2.7.6 | Verificar que el codigo de autenticacion inicial sea generado por un generador de numeros aleatorios seguro, conteniendo al menos 20 bits de entropia (tipicamente un numero aleatorio de seis digitos es suficiente) | L2 | ⏳ | **Pendiente Phase 0.3:** Tokens de 32 bytes (256 bits) generados con SecureRandom. Excede requisito minimo. |

---

## V2.8 One Time Verifier

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 2.8.1 | Verificar que OTPs basados en tiempo tengan una vida util definida antes de expirar | L1 | N/A | **No aplica MVP:** Sin TOTP en MVP. |
| 2.8.2 | Verificar que las claves simetricas usadas para verificar OTPs enviados esten altamente protegidas, como usando un modulo de seguridad de hardware o almacenamiento seguro de claves basado en sistema operativo | L2 | N/A | **No aplica MVP:** Sin TOTP en MVP. |
| 2.8.3 | Verificar que algoritmos criptograficos aprobados sean usados en la generacion, siembra, y verificacion de OTPs | L2 | N/A | **No aplica MVP:** Sin TOTP en MVP. |
| 2.8.4 | Verificar que el OTP basado en tiempo pueda usarse solo una vez dentro del periodo de validez | L2 | N/A | **No aplica MVP:** Sin TOTP en MVP. |
| 2.8.5 | Verificar que si un token OTP multi-factor basado en tiempo se reutiliza durante el periodo de validez, sea logueado y rechazado con notificaciones seguras enviadas al titular del dispositivo | L2 | N/A | **No aplica MVP:** Sin TOTP en MVP. |
| 2.8.6 | Verificar que el generador de OTP fisico de un solo factor pueda ser revocado en caso de robo u otra perdida. Asegurar que la revocacion sea inmediatamente efectiva a traves de sesiones logueadas, independientemente de la ubicacion | L2 | N/A | **No aplica MVP:** Sin hardware OTP en MVP. |
| 2.8.7 | Verificar que autenticadores biometricos esten limitados a uso solo como factores secundarios en conjunto con algo que tienes y algo que sabes | L2 | N/A | **No aplica MVP:** Sin biometricos en MVP. |

---

## V2.9 Cryptographic Verifier

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 2.9.1 | Verificar que las claves criptograficas usadas en verificacion sean almacenadas de forma segura y protegidas contra divulgacion, como usar un TPM o HSM, o un servicio de sistema operativo que pueda usar este almacenamiento seguro | L2 | ⏳ | **Pendiente Phase 0.3:** JWT private key en variable de entorno de Render (encrypted at rest). **Futuro:** AWS KMS o HashiCorp Vault. |
| 2.9.2 | Verificar que el nonce de desafio tenga al menos 64 bits de longitud, y sea estadisticamente unico o unico durante la vida util del dispositivo criptografico | L2 | ⏳ | **Pendiente Phase 0.3:** JWT `jti` claim de 128 bits (UUID). Unico por token. |
| 2.9.3 | Verificar que algoritmos criptograficos aprobados sean usados en la generacion, siembra, y verificacion | L2 | ⏳ | **Pendiente Phase 0.3:** RS256 (RSA-SHA256) para JWT signing. Claves RSA de 2048 bits minimo. |

---

## V2.10 Service Authentication

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 2.10.1 | Verificar que secretos intra-servicio no dependan de credenciales invariables como contrasenas, claves API o cuentas compartidas con acceso privilegiado | L2 | ⏳ | **Pendiente Phase 0.3:** Database credentials en env vars (no hardcoded). JWT signing key rotable. Sin cuentas compartidas entre servicios. |
| 2.10.2 | Verificar que si se requieren contrasenas para autenticacion de servicio, la cuenta de servicio usada no sea una credencial por defecto (por ejemplo, root/root o admin/admin son por defecto en algunos servicios durante instalacion) | L2 | ⏳ | **Implementado:** Neon genera credenciales unicas automaticamente. Usuario `neondb_owner` con password aleatorio. |
| 2.10.3 | Verificar que contrasenas se almacenen con suficiente proteccion para prevenir ataques de recuperacion offline, incluyendo acceso al sistema local | L2 | ⏳ | **Pendiente Phase 0.3:** Service passwords en Render env vars (encrypted at rest). No en codigo ni config files. |
| 2.10.4 | Verificar que contrasenas, integraciones con bases de datos y sistemas de terceros, semillas y secretos internos, y claves API sean gestionados de forma segura y no incluidos en el codigo fuente o almacenados en repositorios de codigo fuente. Tal almacenamiento DEBE resistir ataques offline. Se recomienda fuertemente el uso de un almacen de claves de software seguro (L1), TPM de hardware, o un HSM (L3) para almacenamiento de contrasenas | L2 | ✅ | **Implementado:** `.env` en `.gitignore`. Secrets en Render env vars. Sin hardcoded credentials. CI/CD con Semgrep `p/secrets` detecta leaks. |

---

## Resumen de Estado

| Seccion | Total Requisitos | Cumplidos | Pendientes | No Aplica |
|---------|------------------|-----------|------------|-----------|
| V2.1 Password Security | 12 | 0 | 12 | 0 |
| V2.2 General Authenticator | 7 | 0 | 6 | 1 |
| V2.3 Authenticator Lifecycle | 3 | 0 | 1 | 2 |
| V2.4 Credential Storage | 5 | 0 | 3 | 2 |
| V2.5 Credential Recovery | 7 | 0 | 6 | 1 |
| V2.6 Look-up Secret | 3 | 0 | 3 | 0 |
| V2.7 Out of Band | 6 | 0 | 6 | 0 |
| V2.8 One Time Verifier | 7 | 0 | 0 | 7 |
| V2.9 Cryptographic Verifier | 3 | 0 | 3 | 0 |
| V2.10 Service Authentication | 4 | 1 | 3 | 0 |
| **TOTAL** | **57** | **1** | **43** | **13** |
