# QuickStack Security Context

OWASP ASVS L2 | Spring Security 6

## Progreso ASVS

**65/272 requisitos cumplidos (24%)**

Ver detalle completo: `asvs/README.md` | `ASVS_COMPLIANCE_REPORT.md`

Capítulos prioritarios (auth):
- `asvs/V02-authentication.md` - 14/57 cumplidos (25%)
- `asvs/V03-session-management.md` - 14/19 cumplidos (74%)
- `asvs/V06-cryptography.md` - 9/16 cumplidos (56%)

## Decisiones de Seguridad

| Aspecto | Valor |
|---------|-------|
| Password hash | Argon2id + pepper versionado |
| JWT signing | RS256, 2048 bits |
| Access token | 15 minutos |
| Refresh token | 7 días con rotation |
| HIBP falla | Bloquear registro |
| Rate limit IP | 10 req/min |
| Rate limit email | 5 req/min |
| Account lockout | 5 intentos = 15 min |

## Threat Model

Ver `../SECURITY.md` para:
- Threat model completo
- Attack vectors identificados
- Protocolos de respuesta
- Compliance checklist

## Archivos de Implementación

```
backend/quickstack-common/src/main/java/.../common/
├── config/properties/
│   ├── JwtProperties.java       # RSA keys, expiry, rotation
│   ├── PasswordProperties.java  # Argon2id params, pepper, HIBP
│   └── RateLimitProperties.java # Bucket4j config
├── exception/
│   ├── AuthenticationException.java
│   ├── AccountLockedException.java
│   ├── InvalidTokenException.java
│   └── RateLimitExceededException.java
└── security/
    ├── SecureTokenGenerator.java
    └── IpAddressExtractor.java
```

## Checklist Pre-PR (Auth)

- [ ] No hay secrets hardcodeados
- [ ] Passwords hasheados con Argon2id
- [ ] Tokens con expiry adecuado
- [ ] Rate limiting aplicado
- [ ] Errores genéricos (no leak de info)
- [ ] Tests de seguridad incluidos
