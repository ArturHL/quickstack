# Plan de Migración Arquitectónica: QuickStack a AWS Serverless (Pago por Uso)

Este documento consolida la arquitectura objetivo aprobada para migrar QuickStack POS hacia un modelo de facturación 100% Serverless en AWS, utilizando Infraestructura como Código (Terraform) de manera modular.

## Arquitectura Objetivo Aprobada

### 1. Backend: Microservicios con AWS Lambda + API Gateway
- **Estructura:** El monolito actual se dividirá en Lambdas individuales agrupadas lógicamente por contexto (ej. `/api/v1/users` -> Lambda de Usuarios).
- **Enrutamiento:** Un único **Amazon API Gateway (REST API)** expondrá los endpoints.
- **Cold Starts:** Se utilizará **AWS SnapStart** sobre el runtime de Java 17/21. Esto nos permite mantener el código de Spring Boot actual, logrando tiempos de arranque sub-segundo sin lidiar con la complejidad de compilación de GraalVM.

### 2. Base de Datos: Aurora Serverless v2 + RDS Proxy
- **Motor:** **Amazon Aurora Serverless v2** (PostgreSQL).
- **Conexiones:** **Amazon RDS Proxy** se utilizará entre Lambdas y Aurora para agrupar y reutilizar las conexiones TCP de la base de datos de manera eficiente.

### 3. Identidad y Acceso (Auth): Amazon Cognito
- Se eliminará y archivará el módulo `quickstack-auth` del flujo principal.
- **Amazon Cognito User Pools** será el Proveedor de Identidad. El API Gateway utilizará **Cognito Authorizers** de forma nativa, asegurando validación automática de JWTs y cumpliendo con ASVS L2 de manera gestionada.

### 4. Seguridad: AWS WAF y Shield (ASVS L2)
- Integración de **AWS WAF** (Web Application Firewall) atado al API Gateway y a CloudFront, utilizando reglas administradas (OWASP Top 10, IP Reputation).

### 5. Frontend: S3 + CloudFront + Route53
- La aplicación React (Vite) se construirá de forma estática y se alojará en un bucket **S3**.
- **Amazon CloudFront** se utilizará como CDN para distribuir el frontend de manera segura vía HTTPS, mitigando costos gracias a su amplio *Free Tier* (1TB mensual).
- **Amazon Route 53** gestionará el DNS apuntando a la distribución de CloudFront.

### 6. Infraestructura como Código: 100% Terraform Modular
Estructura final del proyecto Terraform:
```
terraform/
├── environments/
│   ├── dev/         # main.tf para dev (consume los módulos)
│   └── prod/
└── modules/
    ├── network/     # VPC, Subnets, Nat Gateway (Opcional)
    ├── database/    # Aurora Serverless, RDS Proxy, Security Groups
    ├── auth/        # Cognito User Pool, App Clients
    ├── api/         # API Gateway, Lambda Functions, IAM Roles, SnapStart config
    └── frontend/    # S3, CloudFront, ACM (Certificados), Route53
```

---

## Plan de Ejecución (Siguientes Pasos)

1. **Refactorización del Backend (Día 1) - [COMPLETADO]:**
   - Aislar y remover dependencias de `quickstack-auth`.
   - Modificar `quickstack-app` para integrarse con `aws-serverless-java-container` o Spring Cloud Function.
   - Configurar los perfiles de Maven para construir los JARs orientados a Lambda.

1.5. **Configuración de Seguridad Backend (Día 1.5):**
   - Agregar `spring-boot-starter-oauth2-resource-server` a `quickstack-common`.
   - Crear `SecurityConfig` para validar firmas JWT de Cognito.
   - Adaptar los tests E2E para mockear el `JwtDecoder` o generar tokens de prueba válidos.

2. **Infraestructura Base - Terraform (Día 2):**
   - Crear el backend remoto de Terraform (S3/DynamoDB).
   - Desarrollar y desplegar los módulos de `network` y `database` (Aurora V2 + RDS Proxy).
   - Desarrollar y desplegar el módulo `auth` (Cognito).

3. **Despliegue Serverless - Terraform & Backend (Día 3):**
   - Desarrollar el módulo `api` (API Gateway + Lambdas).
   - Habilitar la configuración de SnapStart.
   - Conectar las variables de entorno de Lambda a RDS Proxy y Cognito.

4. **Frontend y Seguridad - Terraform (Día 4):**
   - Desarrollar el módulo `frontend` (S3 + CloudFront + ACM).
   - Desplegar el frontend de React.
   - Aplicar configuraciones de AWS WAF en CloudFront y API Gateway.
