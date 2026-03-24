package io.k48.fortyeightid.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${fortyeightid.openapi.servers:http://localhost:8080}")
    private String openApiServers;

    @Bean
    public OpenAPI customOpenAPI() {
        // Build servers list from configuration, skipping blank entries
        List<Server> servers = new ArrayList<>();
        for (String serverUrl : openApiServers.split(",")) {
            String trimmed = serverUrl.trim();
            if (!trimmed.isEmpty()) {
                servers.add(new Server().url(trimmed));
            }
        }
        
        // Add localhost by default if no valid servers were configured
        if (servers.isEmpty()) {
            servers.add(new Server().url("http://localhost:8080").description("Local development server"));
        }

        return new OpenAPI()
                .info(new Info()
                        .title("48ID API")
                        .version("0.1.0")
                        .description("Centralized identity and authentication platform for the K48 ecosystem")
                        .contact(new Contact()
                                .name("K48 Team")
                                .email("support@k48.io"))
                        .license(new License()
                                .name("K48 License")
                                .url("https://k48.io/license")))
                .servers(servers)
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("Enter JWT Bearer token"))
                        .addSecuritySchemes("api-key", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("Enter API key for external applications")))
                // Add separate security requirements for OR logic (either JWT OR API key)
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .addSecurityItem(new SecurityRequirement().addList("api-key"));
    }
}
