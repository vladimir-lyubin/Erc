package com.marcura.exchangerate.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI exchangeRateOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Exchange Rate Management System API")
                .version("v1")
                .description("Spread-adjusted exchange rates, usage analytics and AI trend insight."));
    }
}
