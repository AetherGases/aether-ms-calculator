package com.aether.ms_calculator.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

  @Bean
  OpenAPI customOpenAPI(){
    return new OpenAPI()
        .info(
            new Info()
                .title("Aether microsserviço de ")
                .version("v1")
                .description("Aqui você encontra todas as rotas da API e o que é necessário enviar de parâmetros para elas funcionarem.")
                .termsOfService("")
                .license(
                    new License()
                        .name("Apache 2.0")
                        .url("")
                )
        )
        .addSecurityItem(new SecurityRequirement().addList(SecurityConfig.SECURITY))
          .components(
              new Components()
                  .addSecuritySchemes(
                      SecurityConfig.SECURITY,
                      new SecurityScheme()
                          .name(SecurityConfig.SECURITY)
                          .type(SecurityScheme.Type.HTTP)
                          .scheme("bearer")
                          .bearerFormat("JWT")
                  )
          );
  }
}
