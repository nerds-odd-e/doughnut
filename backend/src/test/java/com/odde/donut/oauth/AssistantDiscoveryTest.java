package com.odde.donut.oauth;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.testability.SpringTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class AssistantDiscoveryTest extends SpringTestBase {
  @Autowired MockMvc mockMvc;
  @Autowired CanonicalDonutOrigin origin;

  @Test
  void missingMcpCredentialPointsToCanonicalResourceMetadata() throws Exception {
    mockMvc
        .perform(post("/api/mcp").header("Host", "untrusted.example"))
        .andExpect(status().isUnauthorized())
        .andExpect(
            header()
                .string(
                    "WWW-Authenticate",
                    "Bearer resource_metadata=\""
                        + origin.value()
                        + AssistantOAuthEndpoints.RESOURCE_METADATA
                        + "\""));
  }

  @Test
  void resourceMetadataNamesConfiguredIssuerAndReadPermission() throws Exception {
    mockMvc
        .perform(get(AssistantOAuthEndpoints.RESOURCE_METADATA).header("Host", "untrusted.example"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resource").value(origin.value() + "/api/mcp"))
        .andExpect(jsonPath("$.authorization_servers[0]").value(origin.value()))
        .andExpect(jsonPath("$.scopes_supported[0]").value("notes:read"))
        .andExpect(jsonPath("$.tls_client_certificate_bound_access_tokens").value(false));
  }

  @Test
  void issuerMetadataDescribesTheSupportedConfidentialPkceFlow() throws Exception {
    mockMvc
        .perform(get(AssistantOAuthEndpoints.ISSUER_METADATA).header("Host", "untrusted.example"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.issuer").value(origin.value()))
        .andExpect(
            jsonPath("$.authorization_endpoint")
                .value(origin.value() + AssistantOAuthEndpoints.AUTHORIZE))
        .andExpect(
            jsonPath("$.token_endpoint").value(origin.value() + AssistantOAuthEndpoints.TOKEN))
        .andExpect(
            jsonPath("$.revocation_endpoint")
                .value(origin.value() + AssistantOAuthEndpoints.REVOKE))
        .andExpect(
            jsonPath("$.grant_types_supported")
                .value(contains("authorization_code", "refresh_token")))
        .andExpect(
            jsonPath("$.token_endpoint_auth_methods_supported[0]").value("client_secret_basic"))
        .andExpect(jsonPath("$.code_challenge_methods_supported[0]").value("S256"));
  }
}
