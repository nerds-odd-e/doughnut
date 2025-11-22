package com.odde.doughnut.configs;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import java.util.LinkedHashMap;
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
      Server server = new Server();
      server.setUrl("");
      openApi.setServers(List.of(server));
    };
  }

  @Bean
  public OpenApiCustomizer chatMessageSchemaOrderCustomizer() {
    return openApi -> {
      // Fix the property order for ChatMessage to ensure deterministic OpenAPI generation.
      var schemas = openApi.getComponents().getSchemas();
      if (schemas != null && schemas.containsKey("ChatMessage")) {
        var chatMessageSchema = (Schema<?>) schemas.get("ChatMessage");
        if (chatMessageSchema != null && chatMessageSchema.getProperties() != null) {
          @SuppressWarnings("rawtypes")
          var orderedProperties = new LinkedHashMap<String, Schema>();
          // Define the order explicitly: role, textContent, name
          if (chatMessageSchema.getProperties().containsKey("role")) {
            orderedProperties.put("role", chatMessageSchema.getProperties().get("role"));
          }
          if (chatMessageSchema.getProperties().containsKey("textContent")) {
            orderedProperties.put(
                "textContent", chatMessageSchema.getProperties().get("textContent"));
          }
          if (chatMessageSchema.getProperties().containsKey("name")) {
            orderedProperties.put("name", chatMessageSchema.getProperties().get("name"));
          }
          chatMessageSchema.setProperties(orderedProperties);
        }
      }
    };
  }
}
