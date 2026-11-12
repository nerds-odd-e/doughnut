package com.odde.donut.oauth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.entities.repositories.UserRepository;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

abstract class AssistantMcpTestSupport extends AssistantAuthorizationTestSupport {
  @Autowired UserRepository users;

  protected ResultActions call(String token, String tool, Map<String, Object> arguments)
      throws Exception {
    return call(token, tool, arguments, "Bearer");
  }

  protected ResultActions call(
      String token, String tool, Map<String, Object> arguments, String scheme) throws Exception {
    return mockMvc.perform(
        post("/api/mcp")
            .cookie(session)
            .header(HttpHeaders.AUTHORIZATION, scheme + " " + token)
            .header(HttpHeaders.ACCEPT, "application/json, text/event-stream")
            .contentType(MediaType.APPLICATION_JSON)
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
                        Map.of("name", tool, "arguments", arguments)))));
  }

  protected String issueAccessToken() throws Exception {
    var reply =
        exchange(approvedCode(), Map.of(), "test-assistant-secret")
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper
        .readTree(reply.getResponse().getContentAsString())
        .get("access_token")
        .asString();
  }
}
