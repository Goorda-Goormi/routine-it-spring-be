package com.goormi.routine.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    
    @Value("${spring.profiles.active:default}")
    private String activeProfile;
    
    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "bearerAuth";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));
        
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Local Development Server");
        
        Server dockerServer = new Server()
                .url("http://localhost:8080")
                .description("Docker Development Server");

        Server developmentServer = new Server()
                .url("http://54.180.93.1:8080")
                .description("Development Server");
        
        Info info = new Info()
                .title("Routine-It API")
                .version("0.0.1-SNAPSHOT")
                .description("루틴 관리 애플리케이션 Routine-It! API 문서")
                .contact(new Contact()
                        .name("구르다구르미 팀")
                        .email("team@goormi.com")
                        .url("https://github.com/goormi/routine-it"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("http://www.apache.org/licenses/LICENSE-2.0.html"));
        
        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer, dockerServer, developmentServer))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}