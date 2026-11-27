package com.udea.parcial_equipo_4.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI reservasOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de reservas de sillas para conciertos")
                        .description("Consulta y creación de reservas de sillas por espectáculo.")
                        .version("1"));
    }
}
