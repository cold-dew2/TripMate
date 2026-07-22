package com.example.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {

        String jwtScheme = "Bearer Authentication";

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(jwtScheme);

        Components components = new Components()
                .addSecuritySchemes(
                        jwtScheme,
                        new SecurityScheme()
                                .name(jwtScheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                );

        return new OpenAPI()
                .info(new Info()
                        .title("TripMate API")
                        .version("1.0")
                        .description("TripMate API"))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}