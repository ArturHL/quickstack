# Deuda Técnica - QuickStack POS

Registro de deuda técnica conocida, priorizada por fase de implementación.

**Última actualización:** 2026-02-19

---

## 🔴 Alta Prioridad (Phase 1 - En Progreso)

### 1. Implementar CRUD de Tenants y Branches

**Contexto:**
Los módulos `quickstack-tenant` y `quickstack-branch` están vacíos. Las tablas existen en la DB (Flyway V1) pero no hay endpoints ni lógica de negocio.

**Módulos afectados:**
- `backend/quickstack-tenant/` (0 archivos .java)
- `backend/quickstack-branch/` (0 archivos .java)

**Funcionalidad requerida:**

**Tenant Management:**
- POST /api/v1/tenants (crear nuevo cliente/restaurante)
- GET /api/v1/tenants/{id} (detalles)
- PATCH /api/v1/tenants/{id} (actualizar nombre, plan, settings)
- DELETE /api/v1/tenants/{id} (soft delete)

**Branch Management:**
- POST /api/v1/tenants/{tenantId}/branches (crear sucursal)
- GET /api/v1/tenants/{tenantId}/branches (listar sucursales)
- PATCH /api/v1/branches/{id} (actualizar dirección, teléfono)
- DELETE /api/v1/branches/{id} (soft delete)

**Status:** 🚧 EN IMPLEMENTACIÓN (2026-02-19)

**Planeado para:** Phase 1, Semanas 1-3

**Referencias:**
- `docs/reviews/2026-02-19-initial-review.md` sección 3.1
- `docs/DATABASE_SCHEMA.md` (esquema completo)

**Creado:** 2026-02-19

---

### 2. Implementar TenantContext (ThreadLocal)

**Contexto:**
Actualmente el `tenantId` se pasa como parámetro método por método, ensuciando las firmas y aumentando el acoplamiento.

**Problema actual:**
```java
// UserService.java
public User findByEmail(UUID tenantId, String email) { ... }

// LoginAttemptService.java
public void recordFailedAttempt(UUID tenantId, String email) { ... }
```

**Solución propuesta:**

Crear `TenantContextHolder` en `quickstack-common`:

```java
public class TenantContextHolder {
    private static final ThreadLocal<UUID> tenantId = new ThreadLocal<>();

    public static void setTenantId(UUID id) { tenantId.set(id); }
    public static UUID getTenantId() { return tenantId.get(); }
    public static void clear() { tenantId.remove(); }
}
```

Poblar desde filtro de seguridad (extraer de JWT):

```java
// JwtAuthenticationFilter.java
UUID tenantId = jwtService.extractTenantId(token);
TenantContextHolder.setTenantId(tenantId);
```

**Impacto:**
- Simplifica firmas de métodos en todos los services
- Reduce acoplamiento entre capas
- Facilita implementación de Hibernate Filters (ver item #4)

**Planeado para:** Phase 1, Semanas 1-3

**Referencias:**
- `docs/reviews/2026-02-19-initial-review.md` sección 3.3

**Creado:** 2026-02-19

---

## 🟠 Media Prioridad (Phase 1 - Semanas 7-12 o Post-MVP)

### 3. Automatizar Aislamiento Multi-tenant con Hibernate Filters

**Contexto:**
El aislamiento de datos se realiza manualmente en cada query del repositorio. Si un desarrollador olvida agregar `AND u.tenantId = :tenantId`, se pueden exponer datos de otros clientes.

**Ejemplo actual (manual):**
```java
@Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.email = :email")
Optional<User> findByTenantIdAndEmail(@Param("tenantId") UUID tenantId,
                                       @Param("email") String email);
```

**Riesgo:**
Error humano. Data leak si se olvida el filtro de tenant.

**Solución propuesta:**

Usar **Hibernate Filters** para inyectar automáticamente el filtro:

```java
@Entity
@Table(name = "users")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = "uuid-char"))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class User { ... }
```

```java
// TenantFilter.java (ejecuta en cada request)
@Component
public class TenantFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ...) {
        UUID tenantId = TenantContextHolder.getTenantId();
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        // ...
    }
}
```

**Beneficios:**
- ✅ Protección automática contra data leaks
- ✅ Queries más limpias (no repetir lógica de tenant)
- ✅ Fácil de auditar (filtro centralizado)

**Requisito previo:**
Implementar TenantContext (item #3)

**Prioridad:**
Media-Alta. Es importante para seguridad, pero el approach manual funciona si se tiene cuidado.

**Planeado para:** Phase 1 (semanas 7-12) o Phase 2

**Referencias:**
- `docs/reviews/2026-02-19-initial-review.md` sección 3.2

**Creado:** 2026-02-19

---

## 🟡 Baja Prioridad (Post-MVP o Nice to Have)

### 4. Implementar Hibernate UserType para Tipo INET (Solución Profesional)

**Contexto:**
Durante el smoke test descubrimos que Hibernate no mapea correctamente `String` a `INET` (tipo nativo de PostgreSQL para IPs) sin un converter custom. Esto causaba `PSQLException` al guardar usuarios.

**Decisión tomada (2026-02-19):**
Implementamos **Opción A** (fix temporal):
- Migración V9 cambió columnas de `INET` → `VARCHAR(45)`
- Removimos `columnDefinition="inet"` de entities
- Reactivamos `ddl-auto: validate` para protección

**Archivos afectados:**
- `backend/quickstack-app/src/main/resources/db/migration/V9__change_ip_to_varchar.sql`
- `backend/quickstack-user/src/main/java/com/quickstack/user/entity/LoginAttempt.java:43`
- `backend/quickstack-user/src/main/java/com/quickstack/user/entity/User.java`
- `backend/quickstack-user/src/main/java/com/quickstack/user/entity/RefreshToken.java`
- `backend/quickstack-user/src/main/java/com/quickstack/user/entity/PasswordResetToken.java`

**Estado actual (funcional pero no óptimo):**
- ✅ Funciona correctamente
- ✅ Hibernate valida schema (`ddl-auto: validate`)
- ❌ Sin validación de formato IP a nivel DB
- ❌ Indexes menos eficientes para queries de rangos de IP
- ❌ Cualquier string se puede guardar (ej: "not-an-ip")

**Solución profesional futura (Opción C):**

Implementar custom `UserType` de Hibernate para mapear `String` ↔ `INET`:

```java
package com.quickstack.common.hibernate;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.postgresql.util.PGobject;

import java.io.Serializable;
import java.sql.*;

/**
 * Custom Hibernate UserType para mapear String a PostgreSQL INET.
 * Permite usar tipo nativo INET manteniendo String en Java.
 */
public class InetType implements UserType<String> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<String> returnedClass() {
        return String.class;
    }

    @Override
    public boolean equals(String x, String y) {
        return x == null ? y == null : x.equals(y);
    }

    @Override
    public int hashCode(String x) {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public String nullSafeGet(ResultSet rs, int position,
                              SharedSessionContractImplementor session,
                              Object owner) throws SQLException {
        PGobject pgObject = (PGobject) rs.getObject(position);
        return pgObject != null ? pgObject.getValue() : null;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, String value,
                            int index,
                            SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            PGobject pgObject = new PGobject();
            pgObject.setType("inet");
            pgObject.setValue(value);
            st.setObject(index, pgObject);
        }
    }

    @Override
    public String deepCopy(String value) {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(String value) {
        return value;
    }

    @Override
    public String assemble(Serializable cached, Object owner) {
        return (String) cached;
    }
}
```

**Uso en entities:**
```java
@Entity
public class LoginAttempt {
    @Column(name = "ip_address", nullable = false)
    @Type(InetType.class)
    private String ipAddress;
}
```

**Migración requerida (V10):**
```sql
-- Revertir columnas a INET
ALTER TABLE login_attempts
    ALTER COLUMN ip_address TYPE INET
    USING ip_address::inet;

ALTER TABLE refresh_tokens
    ALTER COLUMN ip_address TYPE INET
    USING ip_address::inet;

ALTER TABLE password_reset_tokens
    ALTER COLUMN created_ip TYPE INET
    USING created_ip::inet;

ALTER TABLE users
    ALTER COLUMN last_login_ip TYPE INET
    USING last_login_ip::inet;
```

**Beneficios de la solución profesional:**
- ✅ Validación automática de formato IP a nivel DB
- ✅ Usa tipo nativo de PostgreSQL (INET)
- ✅ Permite queries de rangos: `WHERE ip << '192.168.0.0/16'`
- ✅ Indexes más eficientes (GiST, SP-GiST)
- ✅ Mantiene `ddl-auto: validate` activo
- ✅ Type safety completo

**Cuándo implementar:**
Post-MVP, si se necesita:
- Queries complejas de rangos de IP
- Analytics de geolocalización por IP
- Bloqueo de rangos de IPs (anti-fraud)

**Esfuerzo estimado:** 2-4 horas
- 1h: Implementar y testear UserType
- 1h: Crear migración V10
- 1h: Actualizar todos los entities
- 1h: Tests de integración

**Prioridad:**
Media. Es la solución profesional correcta, pero no es urgente para MVP.

**Planeado para:** Post-MVP (Phase 2 o posterior)

**Referencias:**
- `docs/e2e-testing/smoke-test-walkthrough-2026-02-19.md` línea 53
- PostgreSQL INET docs: https://www.postgresql.org/docs/current/datatype-net-types.html
- Hibernate UserType guide: https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#basic-custom-type

**Creado:** 2026-02-19
**Actualizado:** 2026-02-19 (implementada Opción A, documentada Opción C)

---

### 5. Implementar Rate Limiting a Nivel de Tenant

**Contexto:**
Actualmente el rate limiting es global (por IP y por email). En un modelo SaaS, debería también limitarse por tenant para evitar que un cliente abuse del sistema.

**Implementación actual:**
- 10 req/min por IP
- 5 req/min por email (endpoints de auth)

**Solución propuesta:**
- Agregar bucket por `tenantId`: 1000 req/min por tenant (configurable según plan)
- Useful para prevenir abuse de un tenant específico

**Prioridad:**
Baja. Solo relevante cuando tengamos múltiples tenants reales en producción.

**Planeado para:** Post-MVP

**Creado:** 2026-02-19

---

## 📋 Template para Nuevas Entradas

```markdown
### [Número]. [Título Descriptivo]

**Contexto:**
[Por qué existe esta deuda técnica]

**Archivos afectados:**
- `path/to/file.ext:line`

**Problema:**
[Qué está mal o qué falta]

**Solución propuesta:**
[Cómo se debería resolver]

**Prioridad:** [Alta/Media/Baja]
**Planeado para:** [Phase X o Post-MVP]
**Referencias:** [Links a docs, issues, etc.]
**Creado:** YYYY-MM-DD
```

---

## ✅ Completado (2026-02-19)

### Remover IDs Hardcodeados en Auth Pages

**Resuelto:** 2026-02-19

**Acción tomada:**
Removidos valores hardcodeados de `tenantId` y `roleId` en todas las páginas de autenticación. Reemplazados con TODOs para implementación en Phase 1.

**Archivos modificados:**
- `frontend/src/features/auth/RegisterPage.tsx` - Removido hardcoded tenantId/roleId
- `frontend/src/features/auth/LoginPage.tsx` - Removido hardcoded tenantId
- `frontend/src/features/auth/ForgotPasswordPage.tsx` - Removido hardcoded tenantId

**Próximo paso:**
Implementar flujo de invitación o tenant selection en Phase 1.

---

### Remover Seed Data de Migraciones (V8)

**Resuelto:** 2026-02-19

**Acción tomada:**
Eliminado archivo `V8__seed_test_tenant.sql`. El seed data temporal ya no es necesario.

**Archivo eliminado:**
- `backend/quickstack-app/src/main/resources/db/migration/V8__seed_test_tenant.sql`

**Próximo paso:**
Crear tenants dinámicamente mediante endpoints de Phase 1 (`POST /api/v1/tenants`).

---

## Notas de Uso

- **Agregar nueva deuda:** Usa el template de arriba
- **Resolver deuda:** Mueve a sección "✅ Resuelto" al final del archivo (no borrar)
- **Priorizar:** Mueve entre secciones según cambien las necesidades del proyecto
- **Referencias:** Siempre incluir path a archivos o docs relevantes

**Última revisión:** 2026-02-19
