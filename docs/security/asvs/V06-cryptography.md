# V6: Stored Cryptography

> **Capitulo:** V6
> **Requisitos L2:** 16
> **Cumplidos:** 7 (44%)
> **Ultima actualizacion:** 2026-02-16

---

## V6.1 Data Classification

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 6.1.1 | Verificar que los datos privados regulados se almacenen cifrados en reposo, como Informacion Personalmente Identificable (PII), informacion personal sensible, o datos evaluados como probablemente sujetos a la regulacion de privacidad de la UE GDPR | L2 | ⏳ | **Pendiente Phase 1:** Neon PostgreSQL cifra datos at-rest (AES-256). PII (email, phone, address) en tablas `users` y `customers`. Datos financieros en `orders` y `payments`. |
| 6.1.2 | Verificar que los datos regulados de salud se almacenen cifrados en reposo, como registros medicos, detalles de dispositivos medicos, o registros de investigacion anonimizados | L2 | N/A | **No aplica:** No manejamos datos de salud. |
| 6.1.3 | Verificar que los datos financieros regulados se almacenen cifrados en reposo, como cuentas financieras, incumplimientos o historial crediticio, registros fiscales, historial de pagos, beneficiarios, o registros de mercado o investigacion anonimizados | L2 | ⏳ | **Pendiente Phase 1:** Datos financieros (orders, payments) cifrados at-rest por Neon. Sin datos de tarjetas (solo efectivo en MVP). Retencion 7 anos por requisitos SAT. |

---

## V6.2 Algorithms

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 6.2.1 | Verificar que todos los modulos criptograficos fallen de forma segura, y que los errores se manejen de una manera que no habilite ataques de oraculo Padding | L1 | ✅ | JwtService falla con InvalidTokenException generico (no revela detalles internos). Argon2id via Spring Security sin timing side-channels. |
| 6.2.2 | Verificar que se usen algoritmos, modos y bibliotecas criptograficas probados por la industria o aprobados por el gobierno, en lugar de criptografia codificada a medida | L2 | ✅ | JJWT v0.12.6 para JWT. Spring Security para crypto. Argon2id (OWASP recommended). RS256 (RSA 2048-bit + SHA-256). Sin crypto custom. |
| 6.2.3 | Verificar que el vector de inicializacion de cifrado, configuracion de cifrado, y modos de bloque se configuren de forma segura usando los ultimos consejos | L2 | ⏳ | **Pendiente Phase 0.3:** JWT usa RS256 (no cifrado simetrico). TLS 1.2+ manejado por Render/Neon. Sin cifrado custom. |
| 6.2.4 | Verificar que los algoritmos de numeros aleatorios, cifrado o hash, longitudes de clave, rondas, cifrados o modos, se puedan reconfigurar, actualizar, o intercambiar en cualquier momento, para proteger contra rupturas criptograficas | L2 | ⏳ | **Pendiente Phase 0.3:** Algoritmos configurables via application.yml. JWT signing key rotable. Password hashing con version tracking (re-hash on login si algoritmo cambia). |
| 6.2.5 | Verificar que modos de bloque inseguros conocidos (es decir ECB, etc.), modos de padding (es decir PKCS#1 v1.5, etc.), cifrados con tamanos de bloque pequenos (es decir Triple-DES, Blowfish, etc.), y algoritmos de hashing debiles (es decir MD5, SHA1, etc.) no se usen a menos que se requieran para compatibilidad retroactiva | L2 | ✅ | Sin ECB, Triple-DES, MD5, SHA1. RS256 para JWT. SHA-256 para token hashing. Argon2id para passwords. JwtService rechaza algoritmos inseguros. |
| 6.2.6 | Verificar que nonces, vectores de inicializacion, y otros numeros de un solo uso no se usen mas de una vez con una clave de cifrado dada. El metodo de generacion debe ser apropiado para el algoritmo usado | L2 | ✅ | JWT `jti` unico por token via SecureTokenGenerator (256 bits). Cada token tiene ID unico. Reset tokens generados con SecureRandom. |
| 6.2.7 | Verificar que los datos cifrados se autentiquen mediante firmas, modos de cifrado autenticado, o HMAC para asegurar que el texto cifrado no sea alterado por una parte no autorizada | L2 | ✅ | JWTs firmados con RS256 (RSA + SHA-256). Firma verificada en cada request por JwtAuthenticationFilter. Manipulacion detectada y rechazada. |
| 6.2.8 | Verificar que todas las operaciones criptograficas sean de tiempo constante, sin operaciones 'corto circuito' en comparaciones, calculos, o retornos, para evitar fuga de informacion | L2 | ⏳ | **Pendiente Phase 0.3:** Spring Security usa comparaciones constantes para hashes. `MessageDigest.isEqual()` para token comparison. |

---

## V6.3 Random Values

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 6.3.1 | Verificar que todos los numeros aleatorios, nombres de archivos aleatorios, GUIDs aleatorios, y cadenas aleatorias se generen usando el generador de numeros aleatorios criptograficamente seguro aprobado del modulo criptografico cuando estos valores aleatorios se pretende que no sean adivinables por un atacante | L2 | ✅ | SecureTokenGenerator usa `SecureRandom` para tokens (256 bits). `UUID.randomUUID()` para IDs. Sin `Math.random()` para seguridad. |
| 6.3.2 | Verificar que los GUIDs aleatorios se creen usando el algoritmo GUID v4, y un generador de numeros pseudoaleatorios criptograficamente seguro (CSPRNG). Los GUIDs creados usando otros generadores de numeros pseudoaleatorios pueden ser predecibles | L2 | ✅ | `UUID.randomUUID()` genera UUID v4 con SecureRandom. Usado para todos los IDs de entidades y JWT `jti` claims. |
| 6.3.3 | Verificar que los numeros aleatorios se creen con entropia apropiada incluso cuando la aplicacion esta bajo carga pesada, o que la aplicacion se degrade graciosamente en tales circunstancias | L2 | ⏳ | **Pendiente Phase 0.3:** JVM SecureRandom usa /dev/urandom (no blocking). Pool de entropia del OS. Sin degradacion bajo carga. |

---

## V6.4 Secret Management

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 6.4.1 | Verificar que exista una solucion de gestion de secretos como un vault de claves para crear, almacenar, controlar acceso a, y destruir secretos de forma segura | L2 | ⏳ | **MVP:** Render environment variables (encrypted at rest). **Futuro:** HashiCorp Vault o AWS Secrets Manager para rotacion automatica. |
| 6.4.2 | Verificar que el material de claves no se exponga a la aplicacion sino que use un modulo de seguridad aislado como un vault para operaciones criptograficas | L2 | ⏳ | **MVP:** JWT signing key en env var, cargada en memoria. **Futuro:** AWS KMS o similar para signing sin exponer clave. |

---

## Resumen de Estado

| Seccion | Total Requisitos | Cumplidos | Pendientes | No Aplica |
|---------|------------------|-----------|------------|-----------|
| V6.1 Data Classification | 3 | 0 | 2 | 1 |
| V6.2 Algorithms | 8 | 5 | 3 | 0 |
| V6.3 Random Values | 3 | 2 | 1 | 0 |
| V6.4 Secret Management | 2 | 0 | 2 | 0 |
| **TOTAL** | **16** | **7** | **8** | **1** |
