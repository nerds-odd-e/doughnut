package com.odde.doughnut.configs;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
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
      // Doc is generated with the app on varying ports; pin servers so approval tests stay stable.
      Server server = new Server();
      server.setUrl("");
      openApi.setServers(List.of(server));

      PathItem readingPosition =
          openApi.getPaths().get("/api/notebooks/{notebook}/book/reading-position");
      if (readingPosition != null) {
        Operation get = readingPosition.getGet();
        if (get != null && get.getResponses() != null && get.getResponses().get("204") != null) {
          get.getResponses().get("204").setContent(null);
        }
      }
    };
  }
}
