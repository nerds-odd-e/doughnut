package com.odde.donut.oauth;

import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.mcp.McpServerConfiguration;
import org.springframework.stereotype.Component;

@Component
public record AssistantOAuthEndpoints(CanonicalDonutOrigin origin) {
  public static final String ISSUER_METADATA = "/.well-known/oauth-authorization-server";
  public static final String RESOURCE_METADATA = "/.well-known/oauth-protected-resource/api/mcp";
  public static final String AUTHORIZE = "/api/oauth/authorize";
  public static final String TOKEN = "/api/oauth/token";
  public static final String REVOKE = "/api/oauth/revoke";
  public static final String INTROSPECT = "/api/oauth/introspect";
  public static final String CONSENT_CONTEXT = "/api/oauth/consent-context";
  public static final String ASSISTANTS = "/api/oauth/assistants";
  public static final String ASSISTANT = ASSISTANTS + "/{clientId}";

  public String issuer() {
    return origin.value();
  }

  public String resource() {
    return issuer() + McpServerConfiguration.MCP_ENDPOINT;
  }

  public String resourceMetadata() {
    return issuer() + RESOURCE_METADATA;
  }
}
