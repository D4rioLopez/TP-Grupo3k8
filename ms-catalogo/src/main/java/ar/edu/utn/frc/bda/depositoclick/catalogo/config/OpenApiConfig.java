package ar.edu.utn.frc.bda.depositoclick.catalogo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Documentacion OpenAPI. El "server" apunta al API Gateway: desde Swagger UI se prueban los
 * endpoints a traves del gateway (como lo haria cualquier cliente), nunca directo al servicio.
 * Se ofrecen dos formas de autenticarse: pegar un token (bearer) o loguearse contra Keycloak (password).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI(@Value("${app.openapi.server-url}") String serverUrl,
                           @Value("${app.openapi.token-url}") String tokenUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title("DepositoClick - Catalogo")
                        .version("v1")
                        .description("Dato maestro de articulos: rubros, categorias con atributos dinamicos y articulos. "
                                + "Lectura: cualquier rol. Escritura: ADMIN."))
                .servers(List.of(new Server().url(serverUrl).description("API Gateway")))
                .components(new Components()
                        .addSecuritySchemes("bearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
                        .addSecuritySchemes("keycloak", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows().password(new OAuthFlow().tokenUrl(tokenUrl)))))
                .addSecurityItem(new SecurityRequirement().addList("bearer"))
                .addSecurityItem(new SecurityRequirement().addList("keycloak"));
    }
}
