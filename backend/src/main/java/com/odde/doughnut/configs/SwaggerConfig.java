package com.odde.doughnut.configs;

import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
  @Bean
  public OpenApiCustomizer consumerTypeHeaderOpenAPICustomizer() {
    return openApi -> {
      // To remove the servers field.
      // Since we generate the doc by running the service on random port,
      // it will be different every time.
      openApi.setServers(List.of(new Server()));
    };
  }
}
