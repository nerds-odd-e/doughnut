package com.odde.donut.oauth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

class AssistantConsentTest extends AssistantAuthorizationTestSupport {
  @Test
  void consentContextRequiresSignInWithoutRedirectingTheJsonClient() throws Exception {
    mockMvc
        .perform(
            get(AssistantOAuthEndpoints.CONSENT_CONTEXT)
                .param("client_id", "test-assistant")
                .param("state", begin(Map.of())))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void endedSessionRequiresSignInForBothJsonContexts() throws Exception {
    String state = begin(Map.of());
    mockMvc.perform(post("/logout").cookie(session));
    context(state, "test-assistant").andExpect(status().isUnauthorized());
    mockMvc
        .perform(get(AssistantOAuthEndpoints.ASSISTANTS).cookie(session))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void authorizationNavigationRedirectsToSignIn() throws Exception {
    mockMvc
        .perform(
            get(AssistantOAuthEndpoints.AUTHORIZE)
                .queryParam("response_type", "code")
                .queryParam("client_id", "test-assistant")
                .queryParam("redirect_uri", "https://assistant.example/callback")
                .queryParam("scope", "notes:read")
                .queryParam("resource", endpoints.resource())
                .queryParam("code_challenge", "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG")
                .queryParam("code_challenge_method", "S256"))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeaders.LOCATION, containsString("/login")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"notes:read", "notes:read learning_session:record"})
  void contextComesFromThePendingGrantAndAllowPersistsTheSamePermission(String scopes)
      throws Exception {
    String state = begin(Map.of("scope", scopes));
    var result = context(state, "test-assistant");
    result
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountName").value("old_learner"))
        .andExpect(jsonPath("$.clientName").value("Test assistant"))
        .andExpect(jsonPath("$.scopes").value(containsInAnyOrder(scopes.split(" "))))
        .andExpect(jsonPath("$.resource").value(endpoints.resource()))
        .andExpect(jsonPath("$.redirectUri").value("https://assistant.example/callback"))
        .andExpect(jsonPath("$.formAction").value(AssistantOAuthEndpoints.AUTHORIZE));
    MvcResult context = result.andReturn();
    JsonNode body = objectMapper.readTree(context.getResponse().getContentAsString());
    var allowed = decide(state, context, body, true);
    String location = allowed.getResponse().getRedirectedUrl();
    assertThat(location, containsString("https://assistant.example/callback?code="));
    assertThat(location, containsString("state=assistant-state"));
    String code = query(location, "code");
    assertThat(
        authorizations.findByToken(code, new OAuth2TokenType("code")).getPrincipalName(),
        is("old_learner"));
    assertThat(
        consents
            .findById(clients.findByClientId("test-assistant").getId(), "old_learner")
            .getScopes(),
        is(Set.of(scopes.split(" "))));
  }

  @Test
  void denyReturnsAccessDeniedAndANewAttemptCanBeAllowed() throws Exception {
    String state = begin(Map.of());
    MvcResult context = context(state, "test-assistant").andReturn();
    var body = objectMapper.readTree(context.getResponse().getContentAsString());
    String location = decide(state, context, body, false).getResponse().getRedirectedUrl();
    assertThat(location, containsString("https://assistant.example/callback?error=access_denied"));
    assertThat(authorizations.findByToken(state, new OAuth2TokenType("state")), nullValue());
    String retry = begin(Map.of());
    context = context(retry, "test-assistant").andReturn();
    body = objectMapper.readTree(context.getResponse().getContentAsString());
    assertThat(
        decide(retry, context, body, true).getResponse().getRedirectedUrl(),
        containsString("?code="));
  }

  @Test
  void missingCsrfIsRejectedBeforeIssuingACode() throws Exception {
    mockMvc
        .perform(
            post(AssistantOAuthEndpoints.AUTHORIZE)
                .cookie(session)
                .param("client_id", "test-assistant")
                .param("state", begin(Map.of()))
                .param("scope", "notes:read"))
        .andExpect(status().isForbidden());
  }

  @Test
  void contextRefusesChangedStateClientAndForeignAccount() throws Exception {
    String state = begin(Map.of());
    context("changed-state", "test-assistant").andExpect(status().isBadRequest());
    context(state, "another-test-assistant").andExpect(status().isBadRequest());
    session = signInAs("another_old_learner");
    context(state, "test-assistant").andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @CsvSource({
    "resource,https://foreign.example/api/mcp,invalid_target",
    "scope,notes:write,invalid_scope",
    "scope,learning_session:record,invalid_target",
    "scope,notes:read notes:write,invalid_scope",
    "code_challenge_method,plain,invalid_request",
    "code_challenge,'',invalid_request"
  })
  void authorizationRefusesUnsupportedResourceScopeAndPkce(
      String parameter, String value, String error) throws Exception {
    var result = authorize(Map.of(parameter, value));
    assertThat(result.getResponse().getStatus(), is(302));
    assertThat(
        result.getResponse().getRedirectedUrl(),
        containsString("https://assistant.example/callback?error=" + error));
  }

  @Test
  void anInvalidCallbackCannotReceiveCodeOrError() throws Exception {
    var result = authorize(Map.of("redirect_uri", "https://untrusted.example/callback"));
    assertThat(result.getResponse().getStatus(), is(400));
    assertThat(result.getResponse().getRedirectedUrl(), nullValue());
  }

  @Test
  void loopbackCallbackMustMatchTheRegisteredPort() throws Exception {
    var client = clients.findByClientId("test-assistant");
    clients.save(
        RegisteredClient.from(client)
            .redirectUris(
                uris -> {
                  uris.clear();
                  uris.add("http://127.0.0.1:4455/callback");
                })
            .build());
    var result = authorize(Map.of("redirect_uri", "http://127.0.0.1:5566/callback"));
    assertThat(result.getResponse().getStatus(), is(400));
    assertThat(result.getResponse().getRedirectedUrl(), nullValue());
  }

  @ParameterizedTest
  @CsvSource({
    "state,changed-state,400",
    "client_id,another-test-assistant,400",
    "scope,notes:write,302"
  })
  void nativeDecisionRefusesChangedPendingBindings(String parameter, String value, int status)
      throws Exception {
    String state = begin(Map.of());
    var context = context(state, "test-assistant").andReturn();
    var body = objectMapper.readTree(context.getResponse().getContentAsString());
    var parameters =
        new HashMap<>(
            Map.of(
                "client_id",
                "test-assistant",
                "state",
                state,
                "scope",
                "notes:read",
                body.get("csrfParameterName").asString(),
                body.get("csrfToken").asString()));
    parameters.put(parameter, value);
    var form =
        post(AssistantOAuthEndpoints.AUTHORIZE)
            .cookie(session, context.getResponse().getCookie("XSRF-TOKEN"));
    parameters.forEach((key, item) -> form.param(key, item));
    var result = mockMvc.perform(form).andReturn();
    assertThat(result.getResponse().getStatus(), is(status));
    if (status == 302)
      assertThat(
          result.getResponse().getRedirectedUrl(),
          containsString("https://assistant.example/callback?error=invalid_scope"));
  }

  @Test
  void nativeDecisionRefusesADifferentSignedInAccount() throws Exception {
    String state = begin(Map.of());
    var context = context(state, "test-assistant").andReturn();
    var body = objectMapper.readTree(context.getResponse().getContentAsString());
    session = signInAs("another_old_learner");
    mockMvc
        .perform(
            post(AssistantOAuthEndpoints.AUTHORIZE)
                .cookie(session, context.getResponse().getCookie("XSRF-TOKEN"))
                .param("client_id", "test-assistant")
                .param("state", state)
                .param("scope", "notes:read")
                .param(body.get("csrfParameterName").asString(), body.get("csrfToken").asString()))
        .andExpect(status().isBadRequest());
  }
}
