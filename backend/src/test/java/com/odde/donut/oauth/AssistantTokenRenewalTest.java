package com.odde.donut.oauth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.entities.repositories.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

class AssistantTokenRenewalTest extends AssistantAuthorizationTestSupport {
  @Autowired UserRepository users;

  @Test
  void expiredAccessRenewsWithinTheSameGrantAndReadsWithoutBrowserLogin() throws Exception {
    var note =
        makeMe
            .aNote("Renewed read")
            .notebookOwnedBy(users.findByExternalIdentifier("old_learner"))
            .please();
    JsonNode original = credentials();
    String access = original.get("access_token").asString();
    String refresh = original.get("refresh_token").asString();
    var saved = authorizations.findByToken(access, OAuth2TokenType.ACCESS_TOKEN);
    authorizations.save(
        OAuth2Authorization.from(saved)
            .token(
                new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    access,
                    Instant.now().minusSeconds(20),
                    Instant.now().minusSeconds(10),
                    Set.of("notes:read")),
                metadata -> metadata.putAll(saved.getAccessToken().getMetadata()))
            .build());
    read(access, note.getId()).andExpect(status().isUnauthorized());

    var renewed =
        renew(refresh, Map.of(), "test-assistant", "test-assistant-secret")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scope").value("notes:read"))
            .andExpect(jsonPath("$.token_type").value("Bearer"));
    JsonNode replacement =
        objectMapper.readTree(renewed.andReturn().getResponse().getContentAsString());
    String nextAccess = replacement.get("access_token").asString();
    String nextRefresh = replacement.get("refresh_token").asString();
    assertThat(nextAccess, not(is(access)));
    assertThat(nextRefresh, not(is(refresh)));
    var current = authorizations.findByToken(nextAccess, OAuth2TokenType.ACCESS_TOKEN);
    assertThat(current.getId(), is(saved.getId()));
    assertThat(current.getPrincipalName(), is(saved.getPrincipalName()));
    assertThat(current.getRegisteredClientId(), is(saved.getRegisteredClientId()));
    assertThat(current.getAuthorizedScopes(), is(Set.of("notes:read")));
    assertThat(current.getAccessToken().getToken().getScopes(), is(Set.of("notes:read")));
    assertThat(current.getAccessToken().getClaims().get("sub"), is("old_learner"));
    assertThat(current.getAccessToken().getClaims().get("aud"), is(List.of(endpoints.resource())));
    OAuth2AuthorizationRequest request =
        current.getAttribute(OAuth2AuthorizationRequest.class.getName());
    assertThat(request.getAdditionalParameters().get("resource"), is(endpoints.resource()));
    assertThat(
        authorizations.findByToken(nextRefresh, OAuth2TokenType.REFRESH_TOKEN).getId(),
        is(saved.getId()));
    assertThat(current.getRefreshToken().isActive(), is(true));
    assertThat(authorizations.findByToken(refresh, OAuth2TokenType.REFRESH_TOKEN), nullValue());
    read(nextAccess, note.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.content[0].text").value(containsString("Renewed read")));
    refused(
        renew(refresh, Map.of(), "test-assistant", "test-assistant-secret"), 400, "invalid_grant");
  }

  @ParameterizedTest
  @CsvSource({
    "resource,https://foreign.example/api/mcp,invalid_target",
    "resource,'',invalid_target",
    "scope,notes:read notes:write,invalid_scope",
    "scope,notes:write,invalid_scope"
  })
  void renewalCannotEnlargeTheResourceOrScope(String parameter, String value, String error)
      throws Exception {
    refused(
        renew(
            credentials().get("refresh_token").asString(),
            Map.of(parameter, value),
            "test-assistant",
            "test-assistant-secret"),
        400,
        error);
  }

  @ParameterizedTest
  @CsvSource({
    "test-assistant,wrong-secret,401,invalid_client",
    "another-test-assistant,another-test-secret,400,invalid_grant"
  })
  void renewalRequiresTheOriginalConfidentialClient(
      String client, String secret, int status, String error) throws Exception {
    refused(
        renew(credentials().get("refresh_token").asString(), Map.of(), client, secret),
        status,
        error);
  }

  @ParameterizedTest
  @ValueSource(strings = {"expired", "invalidated", "unknown", "grant-scope", "grant-resource"})
  void unusableRefreshCannotIssueCredentials(String condition) throws Exception {
    String refresh = credentials().get("refresh_token").asString();
    var saved = authorizations.findByToken(refresh, OAuth2TokenType.REFRESH_TOKEN);
    var update = OAuth2Authorization.from(saved);
    switch (condition) {
      case "expired" ->
          authorizations.save(
              update
                  .refreshToken(
                      new OAuth2RefreshToken(
                          refresh, Instant.now().minusSeconds(20), Instant.now().minusSeconds(10)))
                  .build());
      case "invalidated" ->
          authorizations.save(
              update
                  .token(
                      saved.getRefreshToken().getToken(),
                      metadata ->
                          metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, true))
                  .build());
      case "unknown" -> refresh = "unknown-refresh-credential";
      case "grant-scope" ->
          authorizations.save(update.authorizedScopes(Set.of("notes:write")).build());
      case "grant-resource" -> {
        OAuth2AuthorizationRequest request =
            saved.getAttribute(OAuth2AuthorizationRequest.class.getName());
        authorizations.save(
            update
                .attribute(
                    OAuth2AuthorizationRequest.class.getName(),
                    OAuth2AuthorizationRequest.from(request)
                        .additionalParameters(
                            parameters ->
                                parameters.put("resource", "https://foreign.example/api/mcp"))
                        .build())
                .build());
      }
    }
    String error = condition.startsWith("grant-") ? "invalid_target" : "invalid_grant";
    refused(renew(refresh, Map.of(), "test-assistant", "test-assistant-secret"), 400, error);
  }

  private JsonNode credentials() throws Exception {
    return objectMapper.readTree(
        exchange(approvedCode(), Map.of(), "test-assistant-secret")
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  private ResultActions renew(
      String refresh, Map<String, String> overrides, String client, String secret)
      throws Exception {
    var parameters =
        new HashMap<>(
            Map.of(
                "grant_type",
                "refresh_token",
                "refresh_token",
                refresh,
                "resource",
                endpoints.resource()));
    parameters.putAll(overrides);
    var request =
        post(AssistantOAuthEndpoints.TOKEN)
            .header(
                HttpHeaders.AUTHORIZATION,
                "Basic " + HttpHeaders.encodeBasicAuth(client, secret, StandardCharsets.UTF_8));
    parameters.forEach(request::param);
    return mockMvc.perform(request);
  }

  private void refused(ResultActions result, int statusCode, String error) throws Exception {
    result
        .andExpect(status().is(statusCode))
        .andExpect(jsonPath("$.error").value(error))
        .andExpect(jsonPath("$.access_token").doesNotExist())
        .andExpect(jsonPath("$.refresh_token").doesNotExist());
  }

  private ResultActions read(String access, int noteId) throws Exception {
    return mockMvc.perform(
        post("/api/mcp")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + access)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
            .content(
                objectMapper.writeValueAsString(
                    Map.of(
                        "jsonrpc",
                        "2.0",
                        "id",
                        1,
                        "method",
                        "tools/call",
                        "params",
                        Map.of("name", "read_note", "arguments", Map.of("noteId", noteId))))));
  }
}
