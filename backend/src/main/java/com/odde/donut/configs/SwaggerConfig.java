package com.odde.donut.configs;

import com.odde.donut.mcp.McpServerConfiguration;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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

      describeMcpHttpTransport(openApi);
      dropLegacyContentLocatorAliasSchemas(openApi);
    };
  }

  /**
   * Documents the MCP Streamable HTTP transport endpoint. Tool input/output schemas remain the MCP
   * protocol contract, not REST DTOs.
   */
  private static void describeMcpHttpTransport(io.swagger.v3.oas.models.OpenAPI openApi) {
    if (openApi.getComponents() == null) {
      openApi.setComponents(new io.swagger.v3.oas.models.Components());
    }
    openApi
        .getComponents()
        .addSecuritySchemes(
            "donutPersonalToken",
            new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("Donut personal token")
                .description(
                    "Donut personal access token. Required on every MCP request; browser"
                        + " session cookies cannot authorize this endpoint."));

    Operation post =
        new Operation()
            .operationId("mcpStreamableHttp")
            .summary("MCP Streamable HTTP transport")
            .description(
                "Model Context Protocol (MCP) Streamable HTTP endpoint served by the Donut"
                    + " backend. Clients negotiate MCP over JSON-RPC with a Donut personal bearer"
                    + " token. Tool schemas are defined by the MCP protocol, not this OpenAPI"
                    + " document. Requests without a current valid token receive 401 without a"
                    + " sign-in redirect. When an Origin header is supplied, invalid origins are"
                    + " refused with 403.")
            .addSecurityItem(new SecurityRequirement().addList("donutPersonalToken"))
            .addParametersItem(
                new Parameter()
                    .in("header")
                    .name("Accept")
                    .required(true)
                    .schema(new Schema<>().type("string"))
                    .description("Must include both application/json and text/event-stream"))
            .addParametersItem(
                new Parameter()
                    .in("header")
                    .name("Origin")
                    .required(false)
                    .schema(new Schema<>().type("string"))
                    .description(
                        "Optional. When present, validated per MCP HTTP transport rules;"
                            + " invalid values yield 403."))
            .responses(
                new ApiResponses()
                    .addApiResponse(
                        "200",
                        new ApiResponse()
                            .description("JSON-RPC response")
                            .content(
                                new Content()
                                    .addMediaType(
                                        "application/json",
                                        new MediaType().schema(new Schema<>()))))
                    .addApiResponse(
                        "401",
                        new ApiResponse()
                            .description("Missing, invalid, or withdrawn personal token"))
                    .addApiResponse("403", new ApiResponse().description("Invalid Origin header")));

    // Keep /api/mcp first so OpenAPI path order stays stable across regenerations.
    Paths existing = openApi.getPaths() != null ? openApi.getPaths() : new Paths();
    Paths ordered = new Paths();
    ordered.addPathItem(McpServerConfiguration.MCP_ENDPOINT, new PathItem().post(post));
    existing.forEach(
        (path, item) -> {
          if (!McpServerConfiguration.MCP_ENDPOINT.equals(path)) {
            ordered.addPathItem(path, item);
          }
        });
    openApi.setPaths(ordered);
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
