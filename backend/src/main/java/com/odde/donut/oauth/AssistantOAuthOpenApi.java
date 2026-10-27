package com.odde.donut.oauth;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssistantOAuthOpenApi {
  @Bean
  OpenApiCustomizer assistantOAuthProtocol() {
    return api -> {
      api.addTagsItem(
          new Tag()
              .name("assistant-oauth")
              .description("OAuth protocol endpoints; use an OAuth/MCP SDK."));
      api.path(
          AssistantOAuthEndpoints.ISSUER_METADATA,
          new PathItem()
              .get(metadata("assistantIssuerMetadata", "OAuth authorization-server metadata")));
      api.path(
          AssistantOAuthEndpoints.RESOURCE_METADATA,
          new PathItem()
              .get(metadata("assistantResourceMetadata", "MCP protected-resource metadata")));
      api.path(
          AssistantOAuthEndpoints.AUTHORIZE,
          new PathItem()
              .get(
                  protocol(
                      "assistantAuthorization",
                      "Authorize an assistant",
                      "Browser authorization-code request with an exact registered redirect, PKCE and read scope; Donut sign-in and consent are required.")));
      api.path(
          AssistantOAuthEndpoints.TOKEN,
          new PathItem()
              .post(
                  protocol(
                      "assistantToken",
                      "Exchange or renew an assistant credential",
                      "OAuth form-encoded code or refresh exchange. The confidential client authenticates with HTTP Basic; use an OAuth SDK.")));
      api.path(
          AssistantOAuthEndpoints.REVOKE,
          new PathItem()
              .post(
                  protocol(
                      "assistantRevocation",
                      "Revoke an assistant credential",
                      "OAuth form-encoded revocation with confidential-client HTTP Basic authentication.")));
    };
  }

  private static Operation metadata(String id, String summary) {
    return protocol(
            id,
            summary,
            "Public OAuth discovery metadata for the configured canonical Donut MCP resource.")
        .responses(
            new ApiResponses()
                .addApiResponse(
                    "200",
                    new ApiResponse()
                        .description("OAuth metadata")
                        .content(
                            new Content()
                                .addMediaType(
                                    "application/json",
                                    new MediaType().schema(new Schema<>().type("object"))))));
  }

  private static Operation protocol(String id, String summary, String description) {
    return new Operation()
        .operationId(id)
        .summary(summary)
        .description(description)
        .addTagsItem("assistant-oauth")
        .responses(
            new ApiResponses()
                .addApiResponse(
                    "default",
                    new ApiResponse()
                        .description(
                            "OAuth protocol response, including redirects and standard OAuth errors.")));
  }
}
