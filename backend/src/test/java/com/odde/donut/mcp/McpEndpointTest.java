package com.odde.donut.mcp;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.entities.User;
import com.odde.donut.entities.UserToken;
import com.odde.donut.services.UserService;
import com.odde.donut.testability.SpringTestBase;
import io.modelcontextprotocol.spec.ProtocolVersions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class McpEndpointTest extends SpringTestBase {
  private static final String MCP_ACCEPT = "application/json, text/event-stream";
  private static final String INITIALIZE_BODY =
      """
      {"jsonrpc":"2.0","id":1,"method":"initialize","params":{\
      "protocolVersion":"%s","capabilities":{},\
      "clientInfo":{"name":"donut-mcp-test","version":"1.0.0"}}}
      """
          .formatted(ProtocolVersions.MCP_2025_11_25);
  private static final String TOOLS_LIST_BODY =
      """
      {"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
      """;

  @Autowired MockMvc mockMvc;
  @Autowired UserService userService;

  private UserToken personalToken;

  @BeforeEach
  void issuePersonalToken() {
    User user = makeMe.aUser().please();
    personalToken = makeMe.aUserToken().forUser(user).withLabel("mcp").please();
  }

  @Nested
  class Authentication {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Bearer", "Bearer ", "Bearer not-a-real-token"})
    void missingOrInvalidTokenIsUnauthorizedWithoutSignInRedirect(String authorization)
        throws Exception {
      MockHttpServletRequestBuilder request = mcpPost(INITIALIZE_BODY);
      if (authorization != null && !authorization.isEmpty()) {
        request.header(HttpHeaders.AUTHORIZATION, authorization);
      }

      mockMvc
          .perform(request)
          .andExpect(status().isUnauthorized())
          .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }

    @Test
    void withdrawnTokenIsUnauthorized() throws Exception {
      String token = personalToken.getToken();
      userService.deleteToken(personalToken.getId());

      mockMvc
          .perform(mcpPost(INITIALIZE_BODY).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
          .andExpect(status().isUnauthorized())
          .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }

    @Test
    void browserPrincipalCannotSubstituteForToken() throws Exception {
      mockMvc
          .perform(
              mcpPost(INITIALIZE_BODY)
                  .with(
                      request -> {
                        request.setUserPrincipal(() -> "manual");
                        return request;
                      }))
          .andExpect(status().isUnauthorized())
          .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }
  }

  @Nested
  class OriginValidation {
    @Test
    void invalidOriginIsForbidden() throws Exception {
      mockMvc
          .perform(
              authorized(mcpPost(INITIALIZE_BODY))
                  .header(HttpHeaders.ORIGIN, "https://evil.example"))
          .andExpect(status().isForbidden());
    }

    @Test
    void requestWithoutOriginIsAllowedWhenTokenIsValid() throws Exception {
      mockMvc.perform(authorized(mcpPost(INITIALIZE_BODY))).andExpect(status().isOk());
    }
  }

  @Nested
  class ProtocolNegotiation {
    @Test
    void initializesAndListsEmptyToolsThroughServlet() throws Exception {
      mockMvc
          .perform(authorized(mcpPost(INITIALIZE_BODY)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.serverInfo.name", equalTo("donut")))
          .andExpect(jsonPath("$.result.protocolVersion", equalTo(ProtocolVersions.MCP_2025_11_25)))
          .andExpect(jsonPath("$.result.capabilities.tools").exists());

      mockMvc
          .perform(authorized(mcpPost(TOOLS_LIST_BODY)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.tools", empty()));
    }
  }

  private MockHttpServletRequestBuilder mcpPost(String body) {
    return post(McpServerConfiguration.MCP_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.ACCEPT, MCP_ACCEPT)
        .content(body);
  }

  private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
    return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + personalToken.getToken());
  }
}
