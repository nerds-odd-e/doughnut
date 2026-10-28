package com.odde.donut.mcp;

import com.odde.donut.oauth.AssistantAuthorizationConfiguration;
import com.odde.donut.oauth.AssistantOAuthEndpoints;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;

final class McpToolAuthorization {
  private static final List<String> RECORDING_SCOPES =
      List.of(
          AssistantAuthorizationConfiguration.READ_SCOPE,
          AssistantAuthorizationConfiguration.RECORD_SCOPE);

  private McpToolAuthorization() {}

  static Map<String, Object> readOnlyMetadata() {
    return metadata(List.of(AssistantAuthorizationConfiguration.READ_SCOPE));
  }

  static Map<String, Object> recordingMetadata() {
    return metadata(RECORDING_SCOPES);
  }

  private static Map<String, Object> metadata(List<String> scopes) {
    return Map.of("securitySchemes", List.of(Map.of("type", "oauth2", "scopes", scopes)));
  }

  static McpSchema.CallToolResult recordingPermissionRequired(AssistantOAuthEndpoints endpoints) {
    String challenge =
        "Bearer resource_metadata=\""
            + endpoints.resourceMetadata()
            + "\", error=\"insufficient_scope\", "
            + "error_description=\"Authorize learning session recording to continue\", "
            + "scope=\""
            + String.join(" ", RECORDING_SCOPES)
            + "\"";
    return McpSchema.CallToolResult.builder()
        .isError(true)
        .addTextContent("Authorize learning session recording to continue.")
        .meta(Map.of("mcp/www_authenticate", List.of(challenge)))
        .build();
  }
}
