# V5: Validation, Sanitization and Encoding

> **Capitulo:** V5
> **Requisitos L2:** 35
> **Cumplidos:** 0 (0%)
> **Ultima actualizacion:** 2026-02-10

---

## V5.1 Input Validation

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 5.1.1 | Verificar que la aplicacion tenga defensas contra ataques de contaminacion de parametros HTTP, particularmente si el framework de la aplicacion no hace distincion sobre la fuente de los parametros de solicitud (GET, POST, cookies, headers, o variables de entorno) | L1 | ⏳ | **Pendiente Phase 1:** Spring Boot diferencia `@RequestParam`, `@RequestBody`, `@PathVariable`. No hay mezcla automatica de fuentes. |
| 5.1.2 | Verificar que los frameworks protejan contra ataques de asignacion masiva de parametros, o que la aplicacion tenga contramedidas para proteger contra asignacion de parametros inseguros, como marcar campos como privados o similares | L1 | ⏳ | **Pendiente Phase 1:** DTOs explicitos sin herencia de entidades JPA. Solo campos permitidos en DTO. `@JsonIgnore` para campos internos. |
| 5.1.3 | Verificar que todas las entradas (campos de formularios HTML, solicitudes REST, parametros de URL, headers HTTP, cookies, archivos batch, feeds RSS, etc.) sean validadas usando validacion positiva (listas blancas) | L1 | ⏳ | **Pendiente Phase 1:** Bean Validation con `@Pattern`, `@Size`, `@Email`. Enums para valores conocidos. Validacion en Service layer para reglas de negocio. |
| 5.1.4 | Verificar que los datos estructurados sean fuertemente tipados y validados contra un esquema definido incluyendo caracteres permitidos, longitud y patron (por ejemplo numeros de tarjetas de credito, direcciones de email, numeros de telefono, o validar que dos campos relacionados sean razonables, como verificar que suburbio y codigo postal coincidan) | L1 | ⏳ | **Pendiente Phase 1:** DTOs con tipos fuertes. `BigDecimal` para montos. `@Pattern` para RFC, telefono mexicano. Validacion cruzada en service (ej: branch pertenece a tenant). |
| 5.1.5 | Verificar que las redirecciones y forwards de URL solo permitan destinos que aparecen en una lista blanca, o muestren una advertencia al redirigir a contenido potencialmente no confiable | L1 | ⏳ | **Pendiente Phase 0.4:** Sin redirects dinamicos en backend (API pura). Frontend maneja navegacion internamente. Password reset redirect a URL fija del frontend. |

---

## V5.2 Sanitization and Sandboxing

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 5.2.1 | Verificar que todas las entradas HTML no confiables de editores WYSIWYG o similares esten apropiadamente sanitizadas con una biblioteca de sanitizacion HTML o caracteristica del framework | L1 | ⏳ | **Pendiente Phase 1:** No hay WYSIWYG en MVP. Campos de texto plano solamente. Notas/descripciones sin HTML. |
| 5.2.2 | Verificar que los datos no estructurados sean sanitizados para aplicar medidas de seguridad como caracteres y longitud permitidos | L1 | ⏳ | **Pendiente Phase 1:** `@Size(max=)` en todos los campos String. Caracteres especiales permitidos pero escapados en output. |
| 5.2.3 | Verificar que la aplicacion sanitice la entrada del usuario antes de pasar a sistemas de correo para proteger contra inyeccion SMTP o IMAP | L1 | ⏳ | **Pendiente Phase 3:** Email templates con placeholders. User input nunca en headers de email. Solo en body, escapado apropiadamente. |
| 5.2.4 | Verificar que la aplicacion evite el uso de eval() o otras caracteristicas de ejecucion de codigo dinamico. Donde no hay alternativa, cualquier entrada del usuario incluida debe ser sanitizada o sandboxeada antes de ser ejecutada | L1 | ⏳ | **Pendiente Phase 1:** Sin `eval()` en frontend. Sin OGNL/SpEL con user input. Queries JPA con parametros, no concatenacion. |
| 5.2.5 | Verificar que la aplicacion proteja contra ataques de inyeccion de templates asegurando que cualquier entrada del usuario incluida sea sanitizada o apropiadamente sandboxeada | L1 | ⏳ | **Pendiente Phase 3:** Templates de notificacion con Thymeleaf (escapa por defecto). User data como variables, no como template code. |
| 5.2.6 | Verificar que la aplicacion proteja contra ataques SSRF, validando o sanitizando datos no confiables o metadatos de archivos HTTP, como nombres de archivo y campos de entrada de URL, y use listas blancas de protocolos, dominios, rutas y puertos permitidos | L1 | ⏳ | **Pendiente Phase 1:** Sin fetch de URLs proporcionadas por usuario. Imagenes de productos via URL externa (whitelist de dominios CDN). |
| 5.2.7 | Verificar que la aplicacion sanitice, deshabilite, o sandboxee contenido de Scalable Vector Graphics (SVG) scriptable proporcionado por el usuario, especialmente en lo que respecta a XSS resultante de scripts en linea y foreignObject | L1 | N/A | **No aplica MVP:** No hay upload de SVG. Imagenes via URL externa. |
| 5.2.8 | Verificar que la aplicacion sanitice, deshabilite, o sandboxee contenido de lenguaje de expresion o plantilla scriptable proporcionado por el usuario, como Markdown, hojas de estilo CSS o XSL, BBCode, o similares | L1 | ⏳ | **Pendiente Phase 1:** Sin Markdown user-generated. Descripciones de productos en texto plano. |

---

## V5.3 Output Encoding and Injection Prevention

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 5.3.1 | Verificar que la codificacion de salida sea relevante para el interprete y contexto requerido. Por ejemplo, usar codificadores especificamente para valores HTML, atributos HTML, JavaScript, parametros de URL, headers HTTP, SMTP, y otros segun el contexto lo requiera, especialmente de entradas no confiables (ej. nombres con Unicode o apostrofes, como ねこ u O'Hara) | L1 | ⏳ | **Pendiente Phase 1:** React escapa automaticamente en JSX. Backend retorna JSON (no HTML). Email templates con Thymeleaf (escapa por contexto). |
| 5.3.2 | Verificar que la codificacion de salida preserve el conjunto de caracteres y locale elegido por el usuario, de tal manera que cualquier punto de codigo de caracteres Unicode sea valido y manejado de forma segura | L1 | ⏳ | **Pendiente Phase 1:** UTF-8 en toda la aplicacion. JSON con caracteres Unicode soportados. BD con collation UTF-8. |
| 5.3.3 | Verificar que el escapado de salida contextual, preferiblemente automatizado - o en el peor caso, manual - proteja contra XSS reflejado, almacenado y basado en DOM | L1 | ⏳ | **Pendiente Phase 0.4:** React escapa por defecto. No uso de `dangerouslySetInnerHTML`. CSP headers en respuestas. |
| 5.3.4 | Verificar que la seleccion de datos o consultas de bases de datos (por ejemplo SQL, HQL, ORM, NoSQL) usen consultas parametrizadas, ORMs, frameworks de entidad, o esten protegidos de otra manera contra ataques de inyeccion de base de datos | L1 | ⏳ | **Pendiente Phase 1:** Spring Data JPA con queries parametrizados. Sin concatenacion de strings en queries. `@Query` con named parameters. |
| 5.3.5 | Verificar donde no existan mecanismos parametrizados o mas seguros, que la codificacion de salida especifica del contexto se use para proteger contra ataques de inyeccion, como el uso de escapado SQL para proteger contra inyeccion SQL | L1 | N/A | **No aplica:** Usamos siempre queries parametrizados via JPA. |
| 5.3.6 | Verificar que la aplicacion proteja contra ataques de inyeccion de JavaScript o JSON, incluyendo para ataques de eval, incluyendo WebSockets remotos, JavaScript postMessage, nuevas expresiones de funciones JavaScript, y similares | L1 | ⏳ | **Pendiente Phase 0.4:** Sin eval(). JSON parsing con JSON.parse() nativo. WebSocket messages validados en backend. |
| 5.3.7 | Verificar que la aplicacion proteja contra vulnerabilidades de inyeccion LDAP, o que se hayan implementado controles de seguridad especificos para prevenir inyeccion LDAP | L1 | N/A | **No aplica:** Sin LDAP. Autenticacion nativa con BD. |
| 5.3.8 | Verificar que la aplicacion proteja contra inyeccion de comandos del sistema operativo y que las llamadas al sistema operativo usen consultas de sistema operativo parametrizadas o usen codificacion de salida de linea de comandos contextual | L1 | ⏳ | **Pendiente Phase 1:** Sin ejecucion de comandos OS. Todo procesado en Java. Sin Runtime.exec() con user input. |
| 5.3.9 | Verificar que la aplicacion proteja contra ataques de Inclusion de Archivos Locales (LFI) o Inclusion de Archivos Remotos (RFI) | L1 | ⏳ | **Pendiente Phase 1:** Sin carga dinamica de archivos basada en user input. Recursos estaticos hardcoded. |
| 5.3.10 | Verificar que la aplicacion proteja contra ataques de inyeccion XPath o inyeccion XML | L1 | N/A | **No aplica:** Sin procesamiento XML. Solo JSON. |

---

## V5.4 Memory, String, and Unmanaged Code

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 5.4.1 | Verificar que la aplicacion use cadenas de memoria segura, copia de memoria mas segura y aritmetica de punteros para detectar o prevenir desbordamientos de pila, buffer u heap | L2 | N/A | **No aplica:** Java maneja memoria automaticamente. Sin codigo unmanaged. |
| 5.4.2 | Verificar que las cadenas de formato no tomen entradas potencialmente hostiles, y sean constantes | L2 | ⏳ | **Pendiente Phase 1:** Logging con SLF4J placeholders `{}`, no String.format() con user input. |
| 5.4.3 | Verificar que se usen tecnicas de validacion de signo, rango y entrada para prevenir desbordamientos de enteros | L2 | ⏳ | **Pendiente Phase 1:** `@Min`, `@Max` en DTOs para valores numericos. BigDecimal para montos. Validacion de rangos en service layer. |

---

## V5.5 Deserialization Prevention

| ID | Requisito | Nivel | Estado | Medida Implementada |
|----|-----------|-------|--------|---------------------|
| 5.5.1 | Verificar que los objetos serializados usen verificaciones de integridad o esten cifrados para prevenir creacion de objetos hostiles o manipulacion de datos | L1 | ⏳ | **Pendiente Phase 0.3:** Sin Java serialization. Solo JSON via Jackson. JWTs firmados con RS256. |
| 5.5.2 | Verificar que la aplicacion restrinja correctamente los parsers XML para solo usar la configuracion mas restrictiva posible y asegurar que caracteristicas inseguras como resolucion de entidades externas esten deshabilitadas para prevenir ataques XML eXternal Entity (XXE) | L1 | N/A | **No aplica:** Sin procesamiento XML. Solo JSON. |
| 5.5.3 | Verificar que la deserializacion de datos no confiables se evite o este extensamente protegida tanto en codigo personalizado como en bibliotecas de terceros (como parsers JSON, XML y YAML) | L1 | ⏳ | **Pendiente Phase 1:** Jackson configurado para ignorar propiedades desconocidas. DTOs explicitos sin polimorfismo. Sin `@JsonTypeInfo` con user input. |
| 5.5.4 | Verificar que al parsear JSON en navegadores o backends basados en JavaScript, se use JSON.parse() para parsear el documento JSON. No usar eval() para parsear JSON | L1 | ⏳ | **Pendiente Phase 0.4:** JSON.parse() para parsing. Sin eval(). Axios parsea automaticamente respuestas JSON. |

---

## Resumen de Estado

| Seccion | Total Requisitos | Cumplidos | Pendientes | No Aplica |
|---------|------------------|-----------|------------|-----------|
| V5.1 Input Validation | 5 | 0 | 5 | 0 |
| V5.2 Sanitization | 8 | 0 | 7 | 1 |
| V5.3 Output Encoding | 10 | 0 | 7 | 3 |
| V5.4 Memory/String | 3 | 0 | 2 | 1 |
| V5.5 Deserialization | 4 | 0 | 3 | 1 |
| **TOTAL** | **30** | **0** | **24** | **6** |
