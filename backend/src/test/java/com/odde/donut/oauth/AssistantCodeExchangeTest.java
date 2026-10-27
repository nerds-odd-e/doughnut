package com.odde.donut.oauth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

class AssistantCodeExchangeTest extends AssistantAuthorizationTestSupport {

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
}
