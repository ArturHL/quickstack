# V12: Files and Resources

> **Capitulo:** V12
> **Requisitos L2:** 10
> **Cumplidos:** 0 (0%)
> **Ultima actualizacion:** 2026-02-10

---

## V12.1 File Upload

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 12.1.1 | Verificar que la aplicacion no acepte archivos grandes que podrian llenar almacenamiento o causar una denegacion de servicio | L1 | N/A | **MVP:** Sin upload de archivos. Imagenes via URL externa. **Futuro:** Limite de 5MB por archivo, validado en controller. |
| 12.1.2 | Verificar que la aplicacion verifique archivos comprimidos (por ejemplo zip, gz, docx, odt) contra el tamano descomprimido maximo permitido y contra el numero maximo de archivos antes de descomprimir el archivo | L2 | N/A | **No aplica MVP:** Sin procesamiento de archivos comprimidos. |
| 12.1.3 | Verificar que se aplique una cuota de tamano de archivo y un numero maximo de archivos por usuario para asegurar que un solo usuario no pueda llenar el almacenamiento con demasiados archivos, o archivos excesivamente grandes | L1 | N/A | **MVP:** Sin upload de archivos. **Futuro:** Cuota por tenant configurable. |

---

## V12.2 File Integrity

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 12.2.1 | Verificar que los archivos obtenidos de fuentes no confiables sean validados para ser del tipo esperado basado en el contenido del archivo | L2 | N/A | **MVP:** Sin upload. **Futuro:** Magic bytes validation para imagenes (PNG, JPEG). No confiar en extension. |

---

## V12.3 File Execution

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 12.3.1 | Verificar que los metadatos de nombre de archivo enviados por el usuario no se usen directamente por sistemas de archivo del sistema o framework, y que se use una API de URL para proteger contra path traversal | L1 | N/A | **MVP:** Sin almacenamiento de archivos por nombre de usuario. **Futuro:** UUIDs para nombres de archivo en storage. |
| 12.3.2 | Verificar que los metadatos de nombre de archivo enviados por el usuario sean validados o ignorados para prevenir la divulgacion, creacion, actualizacion o eliminacion de archivos locales (LFI) | L1 | N/A | **MVP:** Sin upload de archivos. **Futuro:** Sanitizacion de nombres, almacenamiento con UUID. |
| 12.3.3 | Verificar que los metadatos de nombre de archivo enviados por el usuario sean validados o ignorados para prevenir la divulgacion o ejecucion de archivos remotos via ataques de Inclusion de Archivos Remotos (RFI) o Falsificacion de Solicitud del Lado del Servidor (SSRF) | L1 | ⏳ | **Pendiente Phase 1:** URLs de imagenes de productos validadas contra whitelist de dominios CDN permitidos. Sin fetch de URLs arbitrarias. |
| 12.3.4 | Verificar que la aplicacion proteja contra descargas Reflective File Download (RFD) validando o ignorando nombres de archivo enviados por el usuario en un parametro JSON, JSONP, o URL, el header Content-Type de la respuesta debe establecerse en text/plain, y el header Content-Disposition debe tener un nombre de archivo fijo | L1 | ⏳ | **Pendiente Phase 1:** API retorna JSON con `Content-Type: application/json`. Sin endpoints de descarga de archivos en MVP. |
| 12.3.5 | Verificar que metadatos de archivos no confiables no se usen directamente con API de sistema o bibliotecas, para proteger contra inyeccion de comandos OS | L1 | N/A | **MVP:** Sin procesamiento de archivos. Sin llamadas a comandos OS con metadatos de archivos. |
| 12.3.6 | Verificar que la aplicacion no incluya ni ejecute funcionalidad de fuentes no confiables, como redes de distribucion de contenido no verificadas, bibliotecas JavaScript, bibliotecas node npm, o DLLs del lado del servidor | L2 | ⏳ | **Pendiente Phase 0.4:** NPM packages de fuentes confiables. package-lock.json con checksums. Sin CDN externos para JS critico. |

---

## V12.4 File Storage

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 12.4.1 | Verificar que los archivos obtenidos de fuentes no confiables se almacenen fuera del webroot, con permisos limitados | L1 | N/A | **MVP:** Sin almacenamiento de archivos. **Futuro:** S3/R2 con acceso via signed URLs, no webroot. |
| 12.4.2 | Verificar que los archivos obtenidos de fuentes no confiables sean escaneados por escaner antivirus para prevenir la subida y servicio de contenido malicioso conocido | L1 | N/A | **MVP:** Sin upload. **Futuro:** ClamAV o servicio de scanning para archivos subidos. |

---

## V12.5 File Download

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 12.5.1 | Verificar que la capa web este configurada para servir solo archivos con extensiones de archivo especificas para prevenir informacion no intencional y fuga de codigo fuente. Por ejemplo, archivos de respaldo (como .bak), archivos de trabajo temporales (como .swp), archivos comprimidos (.zip, .tar.gz, etc) y otras extensiones comunmente usadas por editores deben bloquearse a menos que sea requerido | L1 | ⏳ | **Pendiente Phase 0.2:** Spring Boot no sirve archivos estaticos (API only). Vercel configurado para servir solo assets de build. |
| 12.5.2 | Verificar que solicitudes directas a archivos subidos nunca se ejecutaran como contenido HTML/JavaScript | L1 | N/A | **MVP:** Sin archivos subidos. **Futuro:** `Content-Type` forzado, `X-Content-Type-Options: nosniff`, dominio separado para assets. |

---

## V12.6 SSRF Protection

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 12.6.1 | Verificar que el servidor web o de aplicaciones este configurado con una lista blanca de recursos o sistemas a los cuales el servidor puede enviar solicitudes o cargar datos/archivos | L1 | ⏳ | **Pendiente Phase 1:** Sin HTTP client para URLs de usuario. Integraciones solo a servicios conocidos (Neon, SendGrid, Twilio) via whitelist de hosts. |

---

## Resumen de Estado

| Seccion | Total Requisitos | Cumplidos | Pendientes | No Aplica |
|---------|------------------|-----------|------------|-----------|
| V12.1 File Upload | 3 | 0 | 0 | 3 |
| V12.2 File Integrity | 1 | 0 | 0 | 1 |
| V12.3 File Execution | 6 | 0 | 3 | 3 |
| V12.4 File Storage | 2 | 0 | 0 | 2 |
| V12.5 File Download | 2 | 0 | 1 | 1 |
| V12.6 SSRF Protection | 1 | 0 | 1 | 0 |
| **TOTAL** | **15** | **0** | **5** | **10** |
