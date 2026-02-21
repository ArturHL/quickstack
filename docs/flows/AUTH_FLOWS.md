# Flujos de Autenticación y Autorización

Este documento detalla los flujos relacionados con el inicio de sesión, el manejo de sesión (Refresh Token) y la autorización de usuarios, empleando diagramas de nivel de archivo específicos.

## 1. Inicio de Sesión (Login)

Este flujo describe cómo ocurre el inicio de sesión de un usuario desde que ingresa sus credenciales en la interfaz hasta que obtiene un token JWT válido y una cookie de refresh token.

```mermaid
%%{init: {"themeVariables": {"fontFamily": "arial"}}}%%
sequenceDiagram
    actor U as Usuario
    participant View as LoginPage.tsx (React)
    participant Store as authStore.ts (Zustand)
    participant API as authApi.ts (Axios)
    participant Ctrl as AuthController.java (Spring Boot)
    participant Svc as AuthService.java
    participant Repo as UserRepository.java
    participant Pass as PasswordEncoder (Argon2id)
    participant DB as PostgreSQL (Neon)

    U->>View: Ingresa email y password
    View->>Store: login(email, password)
    Store->>API: POST /api/v1/auth/login
    API->>Ctrl: Recibe HTTP POST ({email, password})
    Ctrl->>Svc: authenticate(LoginRequest)
    Svc->>Repo: findByEmail(email)
    Repo->>DB: SELECT * FROM users WHERE email=?
    DB-->>Repo: Retorna User Entity (con hash)
    Repo-->>Svc: User Entity
    Svc->>Pass: matches(rawPassword, hashedPassword)
    Pass-->>Svc: true / false
    
    alt Password Correcto
        Svc->>Svc: generateTokens(user, tenant_id)
        Svc-->>Ctrl: AuthResponse (AccessToken) + RefreshToken Cookie
        Ctrl-->>API: 200 OK + JWT (Response Body) + Set-Cookie (httpOnly)
        API-->>Store: Response Data
        Store->>Store: setTokens(accessToken) & setIsAuth(true)
        Store-->>View: Navega a /dashboard
    else Password Incorrecto
        Svc-->>Ctrl: throws AuthenticationException
        Ctrl-->>API: 401 Unauthorized
        API-->>Store: Error response
        Store-->>View: Muestra mensaje de error (Credenciales inválidas)
    end
```

## 2. Refresco de Token (Refresh Token Rotation)

Debido al corto tiempo de vida del Access Token (ej. 15 minutos), la app utiliza el `refreshToken` (almacenado en una cookie `httpOnly`) para obtener un nuevo par de tokens sin requerir que el usuario vuelva a ingresar credenciales.

```mermaid
sequenceDiagram
    participant API as axios/interceptors.ts (Axios Interceptors)
    participant AuthAPI as authApi.ts
    participant Ctrl as AuthController.java (Spring Boot)
    participant Svc as AuthService.java
    participant DB as PostgreSQL (Neon)

    API->>API: Request a un endpoint privado
    API->>API: Detecta Access Token expirado (401)
    
    API->>AuthAPI: POST /api/v1/auth/refresh
    Note over AuthAPI,Ctrl: La cookie httpOnly se envía automáticamente

    AuthAPI->>Ctrl: Recibe Request + Cookie (__Host-refreshToken)
    Ctrl->>Svc: refresh(refreshToken)
    Svc->>DB: SELECT * FROM refresh_tokens WHERE token=?
    DB-->>Svc: Token Entity
    
    alt Token Válido y Vigente
        Svc->>Svc: Revoca refreshToken anterior
        Svc->>Svc: Genera nuevo AccessToken y RefreshToken
        Svc->>DB: Guarda nuevo RefreshToken
        Svc-->>Ctrl: Nuevo AuthResponse + Update Set-Cookie
        Ctrl-->>AuthAPI: 200 OK + Nuevo AccessToken
        AuthAPI-->>API: Actualiza store y reintenta request original
    else Token Revocado / Expirado
        Svc-->>Ctrl: throws AuthenticationException
        Ctrl-->>AuthAPI: 401 Unauthorized
        AuthAPI-->>API: Falla el refresco
        API->>API: Dispara logout() -> Redirige a /login
    end
```

## 3. Autorización (RBAC) en Requerimientos API

Cuando un usuario autenticado trata de acceder a un endpoint restringido, se ejecutan las validaciones del JWT y del rol asignado.

```mermaid
sequenceDiagram
    participant API as authApi.ts (Axios)
    participant Filter as JwtAuthenticationFilter.java 
    participant Sec as SecurityContextHolder.java
    participant Ctrl as Controlador (Ej: ProductController.java)
    participant Eval as MethodSecurityExpressionHandler (Ej: hasRole())

    API->>Filter: Request con "Authorization: Bearer <token>"
    Filter->>Filter: Extrae JWT del Header
    
    alt JWT Válido
        Filter->>Filter: Valida firma RSA256
        Filter->>Filter: Extrae Claims (email, role, tenantId)
        Filter->>Sec: Registra Authentication object
        Filter->>Ctrl: Continúa la cadena (DoFilter)
        
        Ctrl->>Eval: @PreAuthorize("hasRole('OWNER')")
        Eval->>Sec: Revisa roles del Authentication
        
        alt Tiene Permisos
            Eval-->>Ctrl: Permitido (Procesa Petición)
            Ctrl-->>API: 200 OK
        else Sin Permisos
            Eval-->>Ctrl: Denegado
            Ctrl-->>API: 403 Forbidden
        end
    else JWT Inválido (Modificado o expirado antes de refresh)
        Filter-->>API: 401 Unauthorized
    end
```
