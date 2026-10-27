package com.odde.donut.oauth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.test.web.servlet.ResultActions;

class AssistantCodeExchangeTest extends AssistantConsentTestSupport {
  private static final String VERIFIER = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
  private static final String CHALLENGE = "fR4ifSAEy-7Mu6g7FHZulPKrtjqdAnUCwRFAJt2JFsA";

  @Test
  void exchangedCredentialsRetainTheConsentingAccountClientScopeAndResource() throws Exception {
    String code = approvedCode();
    var response = exchange(code, Map.of(), "test-assistant-secret");
    response
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token_type").value("Bearer"))
        .andExpect(jsonPath("$.scope").value("notes:read"));
    var body = objectMapper.readTree(response.andReturn().getResponse().getContentAsString());
    String access = body.get("access_token").asString();
    String refresh = body.get("refresh_token").asString();
    assertThat(access, matchesPattern("[A-Za-z0-9_-]+"));
    assertThat(refresh, matchesPattern("[A-Za-z0-9_-]+"));
    assertThat(refresh, not(is(access)));
    var saved = authorizations.findByToken(access, OAuth2TokenType.ACCESS_TOKEN);
    assertThat(saved.getPrincipalName(), is("old_learner"));
    assertThat(saved.getRegisteredClientId(), is(clients.findByClientId("test-assistant").getId()));
    assertThat(saved.getAuthorizedScopes(), is(Set.of("notes:read")));
    assertThat(saved.getAccessToken().getToken().getScopes(), is(Set.of("notes:read")));
    assertThat(saved.getAccessToken().getClaims().get("sub"), is("old_learner"));
    assertThat(saved.getAccessToken().getClaims().get("aud"), is(List.of(endpoints.resource())));
    OAuth2AuthorizationRequest request =
        saved.getAttribute(OAuth2AuthorizationRequest.class.getName());
    assertThat(request.getAdditionalParameters().get("resource"), is(endpoints.resource()));
    assertThat(
        authorizations.findByToken(refresh, OAuth2TokenType.REFRESH_TOKEN).getId(),
        is(saved.getId()));
    assertThat(
        Duration.between(
            saved.getAccessToken().getToken().getIssuedAt(),
            saved.getAccessToken().getToken().getExpiresAt()),
        is(Duration.ofSeconds(7)));
    assertThat(
        Duration.between(
            saved.getRefreshToken().getToken().getIssuedAt(),
            saved.getRefreshToken().getToken().getExpiresAt()),
        is(Duration.ofSeconds(31)));
    assertThat(
        clients.findByClientId("test-assistant").getTokenSettings().isReuseRefreshTokens(),
        is(false));
  }

  @Test
  void wrongClientSecretCannotExchangeACode() throws Exception {
    exchange(approvedCode(), Map.of(), "wrong-secret")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("invalid_client"))
        .andExpect(jsonPath("$.access_token").doesNotExist())
        .andExpect(jsonPath("$.refresh_token").doesNotExist());
  }

  @ParameterizedTest
  @CsvSource({
    "code_verifier,ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefg,invalid_grant",
    "resource,https://foreign.example/api/mcp,invalid_target",
    "resource,'',invalid_target",
    "redirect_uri,https://foreign.example/callback,invalid_grant"
  })
  void changedExchangeBindingsAreRefused(String parameter, String value, String error)
      throws Exception {
    exchange(approvedCode(), Map.of(parameter, value), "test-assistant-secret")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(error))
        .andExpect(jsonPath("$.access_token").doesNotExist())
        .andExpect(jsonPath("$.refresh_token").doesNotExist());
  }

  @Test
  void anotherRegisteredClientCannotExchangeTheConsentingClientsCode() throws Exception {
    exchange(approvedCode(), Map.of(), "another-test-assistant", "another-test-secret")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_grant"))
        .andExpect(jsonPath("$.access_token").doesNotExist())
        .andExpect(jsonPath("$.refresh_token").doesNotExist());
  }

  @Test
  void authorizationCodeIsSingleUse() throws Exception {
    String code = approvedCode();
    exchange(code, Map.of(), "test-assistant-secret").andExpect(status().isOk());
    exchange(code, Map.of(), "test-assistant-secret")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_grant"))
        .andExpect(jsonPath("$.access_token").doesNotExist())
        .andExpect(jsonPath("$.refresh_token").doesNotExist());
  }

  private String approvedCode() throws Exception {
    String state = begin(Map.of("code_challenge", CHALLENGE));
    var context = context(state, "test-assistant").andReturn();
    var body = objectMapper.readTree(context.getResponse().getContentAsString());
    return query(decide(state, context, body, true).getResponse().getRedirectedUrl(), "code");
  }

  private ResultActions exchange(String code, Map<String, String> overrides, String secret)
      throws Exception {
    return exchange(code, overrides, "test-assistant", secret);
  }

  private ResultActions exchange(
      String code, Map<String, String> overrides, String clientId, String secret) throws Exception {
    var fields =
        new HashMap<>(
            Map.of(
                "grant_type",
                "authorization_code",
                "code",
                code,
                "redirect_uri",
                "https://assistant.example/callback",
                "code_verifier",
                VERIFIER,
                "resource",
                endpoints.resource()));
    fields.putAll(overrides);
    var request =
        post(AssistantOAuthEndpoints.TOKEN)
            .header(
                HttpHeaders.AUTHORIZATION,
                "Basic " + HttpHeaders.encodeBasicAuth(clientId, secret, StandardCharsets.UTF_8));
    fields.forEach(request::param);
    return mockMvc.perform(request);
  }
}
