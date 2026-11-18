package br.com.fiap.citizentech.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "Parada Certa API",
				version = "1.0",
				description = "CitizenTech - Global Solution 2026/2. Embarque assistido para passageiros "
						+ "com deficiencia ou mobilidade reduzida no transporte publico de Sao Paulo."),
		security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
		name = "bearerAuth",
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT")
public class OpenApiConfig {
}
