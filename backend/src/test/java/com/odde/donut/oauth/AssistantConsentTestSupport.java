package com.odde.donut.oauth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.testability.SpringTestBase;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

abstract class AssistantConsentTestSupport extends SpringTestBase {
  @Autowired MockMvc mockMvc;
  @Autowired AssistantOAuthEndpoints endpoints;
  @Autowired ObjectMapper objectMapper;
  @Autowired OAuth2AuthorizationService authorizations;
  @Autowired OAuth2AuthorizationConsentService consents;
  @Autowired RegisteredClientRepository clients;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager transactions;
  Cookie session;

  @BeforeEach
  void signIn() throws Exception {
    makeMe.aUser("old_learner").please();
    session = signInAs("old_learner");
  }

  @AfterEach
  void deleteSessions() {
    var transaction = new TransactionTemplate(transactions);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transaction.executeWithoutResult(
        ignored ->
            jdbc.update(
                "DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME IN"
                    + " ('old_learner', 'another_old_learner')"));
  }

  protected Cookie signInAs(String username) throws Exception {
    return mockMvc
        .perform(
            get("/login/continue")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Basic "
                        + HttpHeaders.encodeBasicAuth(
                            username, "password", StandardCharsets.UTF_8)))
        .andReturn()
        .getResponse()
        .getCookie("SESSION");
  }

  protected MvcResult authorize(Map<String, String> overrides) throws Exception {
    var parameters =
        new HashMap<>(
            Map.of(
                "response_type",
                "code",
                "client_id",
                "test-assistant",
                "redirect_uri",
                "https://assistant.example/callback",
                "state",
                "assistant-state",
                "scope",
                "notes:read",
                "resource",
                endpoints.resource(),
                "code_challenge",
                "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG",
                "code_challenge_method",
                "S256"));
    parameters.putAll(overrides);
    var request = get(AssistantOAuthEndpoints.AUTHORIZE).cookie(session);
    parameters.forEach((key, value) -> request.queryParam(key, value));
    return mockMvc.perform(request).andReturn();
  }

  protected String begin(Map<String, String> overrides) throws Exception {
    return query(authorize(overrides).getResponse().getRedirectedUrl(), "state");
  }

  protected String query(String uri, String name) {
    return UriUtils.decode(
        UriComponentsBuilder.fromUriString(uri).build().getQueryParams().getFirst(name),
        StandardCharsets.UTF_8);
  }

  protected ResultActions context(String state, String clientId) throws Exception {
    return mockMvc.perform(
        get(AssistantOAuthEndpoints.CONSENT_CONTEXT)
            .cookie(session)
            .param("client_id", clientId)
            .param("state", state)
            .param("scope", "notes:write")
            .param("redirect_uri", "https://untrusted.example/callback")
            .param("client_name", "Untrusted name"));
  }

  protected MvcResult decide(String state, MvcResult context, JsonNode body, boolean allow)
      throws Exception {
    var form =
        post(AssistantOAuthEndpoints.AUTHORIZE)
            .cookie(session, context.getResponse().getCookie("XSRF-TOKEN"))
            .param("client_id", "test-assistant")
            .param("state", state)
            .param(body.get("csrfParameterName").asString(), body.get("csrfToken").asString());
    if (allow) form.param("scope", "notes:read");
    return mockMvc.perform(form).andExpect(status().isFound()).andReturn();
  }
}
