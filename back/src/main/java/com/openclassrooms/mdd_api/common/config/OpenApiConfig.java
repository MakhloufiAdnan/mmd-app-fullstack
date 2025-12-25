package com.openclassrooms.mdd_api.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mddOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MDD API")
                        .description("Monde de Dév – MVP API")
                        .version("1.0.0"));
    }
}
