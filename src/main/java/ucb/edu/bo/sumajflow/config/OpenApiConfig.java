package ucb.edu.bo.sumajflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SumajFlow API - Trazabilidad Minera")
                        .version("1.0.0")
                        .description("API REST para el sistema de trazabilidad minera SumajFlow. " +
                                "Gestiona cooperativas, socios, ingenios, comercializadoras y el flujo de la cadena minera."))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Servidor de Desarrollo"),
                        new Server()
                                .url("https://api.sumajflow.bo")
                                .description("Servidor de Producción")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingresa el token JWT obtenido del endpoint /auth/login")));
    }
}