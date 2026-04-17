package com.odde.doughnut.configs;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Map;
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

      dropLegacyContentLocatorAliasSchemas(openApi);
    };
  }

  /**
   * Springdoc registers both the polymorphic {@code EpubLocator_Full} / {@code PdfLocator_Full}
   * schemas and bare {@code EpubLocator} / {@code PdfLocator} duplicates for discriminator mapping
   * targets. The API only needs the {@code *_Full} shapes; drop the aliases and point the
   * discriminator at the canonical schemas.
   */
  private static void dropLegacyContentLocatorAliasSchemas(
      io.swagger.v3.oas.models.OpenAPI openApi) {
    if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
      return;
    }
    Map<String, Schema> schemas = openApi.getComponents().getSchemas();
    Schema contentLocatorFull = schemas.get("ContentLocator_Full");
    if (contentLocatorFull != null && contentLocatorFull.getDiscriminator() != null) {
      Discriminator discriminator = contentLocatorFull.getDiscriminator();
      Map<String, String> mapping = discriminator.getMapping();
      if (mapping != null) {
        mapping.put("EpubLocator_Full", "#/components/schemas/EpubLocator_Full");
        mapping.put("PdfLocator_Full", "#/components/schemas/PdfLocator_Full");
      }
    }
    schemas.remove("EpubLocator");
    schemas.remove("PdfLocator");

    Schema bookLastRead = schemas.get("BookLastReadPositionRequest");
    if (bookLastRead != null && bookLastRead.getProperties() != null) {
      Object locatorRaw = bookLastRead.getProperties().get("locator");
      if (locatorRaw instanceof Schema) {
        Schema locatorRef = new Schema<>();
        locatorRef.set$ref("#/components/schemas/ContentLocator_Full");
        bookLastRead.getProperties().put("locator", locatorRef);
      }
    }

    Schema bookUserLastReadPosition = schemas.get("BookUserLastReadPosition");
    if (bookUserLastReadPosition != null && bookUserLastReadPosition.getProperties() != null) {
      Object locatorRaw = bookUserLastReadPosition.getProperties().get("locator");
      if (locatorRaw instanceof Schema) {
        Schema locatorRef = new Schema<>();
        locatorRef.set$ref("#/components/schemas/ContentLocator_Full");
        bookUserLastReadPosition.getProperties().put("locator", locatorRef);
      }
    }

    schemas.remove("ContentLocator");
  }
}
