package com.quickstack;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration.
 *
 * UI disponible en: /swagger-ui.html
 * Spec JSON en:     /v3/api-docs
 *
 * Autenticación: Bearer JWT — usar el botón "Authorize 🔒" con el access token
 * obtenido de POST /api/v1/auth/login.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("QuickStack POS API")
                        .description("""
                                API REST multi-tenant para el sistema de punto de venta QuickStack.

                                **Autenticación:** Bearer JWT (RS256, 15 min).
                                Obtén el token con `POST /api/v1/auth/login` y pégalo en el botón **Authorize 🔒**.

                                **Multi-tenancy:** El `tenantId` se extrae automáticamente del JWT —
                                nunca se envía como parámetro de la request.

                                **Roles disponibles:** `OWNER > MANAGER > CASHIER > WAITER > KITCHEN`
                                """)
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token obtenido de POST /api/v1/auth/login")));
    }
}
