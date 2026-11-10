package com.refaccionaria.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** OpenAPI is intentionally available only in the isolated security laboratory. */
@Configuration
@Profile("security-lab")
public class OpenApiSecurityLabConfig {
    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI securityLabOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Gestor de Taller Mecanico")
                        .version("1.0.0-security-lab")
                        .description("Contrato generado para desarrollo y pruebas autorizadas. No usar en produccion.")
                        .license(new License().name("Uso interno de desarrollo")))
                .servers(List.of(new Server().url("http://localhost:8081").description("Laboratorio local")))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
