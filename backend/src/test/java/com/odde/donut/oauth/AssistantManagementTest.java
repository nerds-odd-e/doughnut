package com.odde.donut.oauth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

class AssistantManagementTest extends AssistantAuthorizationTestSupport {
  @Test
  void listsOnlyTheCurrentAccountsConnectedClientsWithPublicIdentification() throws Exception {
    approvedCode();
    var result = management();
    var body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(
        body.get("assistants"),
        is(
            objectMapper.readTree(
                "[{\"clientId\":\"test-assistant\",\"clientName\":\"Test assistant\","
                    + "\"scopes\":[\"notes:read\"]}]")));
    assertThat(body.get("csrfHeaderName").asString(), is("X-XSRF-TOKEN"));
    session = signInAs("another_old_learner");
    makeMe.aUser("another_old_learner").please();
    mockMvc
        .perform(get(AssistantOAuthEndpoints.ASSISTANTS).cookie(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assistants").isEmpty());
  }

  @Test
  void reportGrantIsVisibleAndRenewableUntilWithdrawn() throws Exception {
    JsonNode original = credentials(approvedCode("notes:read learning_session:record"));
    mockMvc
        .perform(get(AssistantOAuthEndpoints.ASSISTANTS).cookie(session))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.assistants[0].scopes")
                .value(containsInAnyOrder("notes:read", "learning_session:record")));
    var renewed =
        refresh(original.get("refresh_token").asString()).andExpect(status().isOk()).andReturn();
    JsonNode current = objectMapper.readTree(renewed.getResponse().getContentAsString());
    read(current.get("access_token").asString()).andExpect(status().isOk());
    withdraw(management()).andExpect(status().isOk());
    read(current.get("access_token").asString()).andExpect(status().isUnauthorized());
    refresh(current.get("refresh_token").asString()).andExpect(status().isBadRequest());
  }

  @Test
  void withdrawalInvalidatesEveryCredentialAndPendingDecisionButPreservesTheOtherAccount()
      throws Exception {
    String pending = begin(Map.of());
    MvcResult pendingContext = context(pending, "test-assistant").andReturn();
    JsonNode pendingBody = objectMapper.readTree(pendingContext.getResponse().getContentAsString());
    JsonNode first = credentials(approvedCode());
    JsonNode second = credentials(nextCode());
    String outstandingCode = nextCode();
    Cookie owner = session;
    makeMe.aUser("another_old_learner").please();
    session = signInAs("another_old_learner");
    JsonNode other = credentials(approvedCode());
    session = owner;
    withdraw(management()).andExpect(status().isOk());

    read(first.get("access_token").asString()).andExpect(status().isUnauthorized());
    read(second.get("access_token").asString()).andExpect(status().isUnauthorized());
    refresh(first.get("refresh_token").asString())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_grant"));
    refresh(second.get("refresh_token").asString()).andExpect(status().isBadRequest());
    exchange(outstandingCode, Map.of(), "test-assistant-secret")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_grant"));
    var decision =
        post(AssistantOAuthEndpoints.AUTHORIZE)
            .cookie(session, pendingContext.getResponse().getCookie("XSRF-TOKEN"))
            .param("client_id", "test-assistant")
            .param("state", pending)
            .param("scope", "notes:read")
            .param(
                pendingBody.get("csrfParameterName").asString(),
                pendingBody.get("csrfToken").asString());
    mockMvc.perform(decision).andExpect(status().isBadRequest());
    assertThat(
        authorize(Map.of()).getResponse().getRedirectedUrl(),
        containsString("/assistant-consent?"));
    read(other.get("access_token").asString()).andExpect(status().isOk());
    refresh(other.get("refresh_token").asString()).andExpect(status().isOk());
  }

  @Test
  void withdrawalRequiresTheOwnersSessionAndFrameworkCsrf() throws Exception {
    JsonNode original = credentials(approvedCode());
    mockMvc
        .perform(delete(AssistantOAuthEndpoints.ASSISTANTS + "/test-assistant").cookie(session))
        .andExpect(status().isForbidden());
    makeMe.aUser("another_old_learner").please();
    session = signInAs("another_old_learner");
    withdraw(management()).andExpect(status().isNotFound());
    refresh(original.get("refresh_token").asString()).andExpect(status().isOk());
    mockMvc.perform(get(AssistantOAuthEndpoints.ASSISTANTS)).andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            delete(AssistantOAuthEndpoints.ASSISTANTS + "/test-assistant")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Basic "
                        + HttpHeaders.encodeBasicAuth(
                            "test-assistant", "test-assistant-secret", StandardCharsets.UTF_8)))
        .andExpect(status().isForbidden());
  }

  private String nextCode() throws Exception {
    return query(
        authorize(Map.of("code_challenge", "fR4ifSAEy-7Mu6g7FHZulPKrtjqdAnUCwRFAJt2JFsA"))
            .getResponse()
            .getRedirectedUrl(),
        "code");
  }

  private JsonNode credentials(String code) throws Exception {
    return objectMapper.readTree(
        exchange(code, Map.of(), "test-assistant-secret")
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  private MvcResult management() throws Exception {
    return mockMvc
        .perform(get(AssistantOAuthEndpoints.ASSISTANTS).cookie(session))
        .andExpect(status().isOk())
        .andReturn();
  }

  private ResultActions withdraw(MvcResult context) throws Exception {
    var body = objectMapper.readTree(context.getResponse().getContentAsString());
    return mockMvc.perform(
        delete(AssistantOAuthEndpoints.ASSISTANTS + "/test-assistant")
            .cookie(session, context.getResponse().getCookie("XSRF-TOKEN"))
            .header(body.get("csrfHeaderName").asString(), body.get("csrfToken").asString()));
  }

  private ResultActions refresh(String token) throws Exception {
    return mockMvc.perform(
        post(AssistantOAuthEndpoints.TOKEN)
            .header(
                HttpHeaders.AUTHORIZATION,
                "Basic "
                    + HttpHeaders.encodeBasicAuth(
                        "test-assistant", "test-assistant-secret", StandardCharsets.UTF_8))
            .param("grant_type", "refresh_token")
            .param("refresh_token", token)
            .param("resource", endpoints.resource()));
  }

  private ResultActions read(String token) throws Exception {
    return mockMvc.perform(
        post("/api/mcp")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
            .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"));
  }
}
