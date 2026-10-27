package com.odde.donut.mcp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.odde.donut.entities.User;
import com.odde.donut.entities.UserToken;
import com.odde.donut.services.UserService;
import com.odde.donut.testability.SpringTestBase;
import io.modelcontextprotocol.spec.ProtocolVersions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

abstract class McpEndpointTestBase extends SpringTestBase {
  static final String MCP_ACCEPT = "application/json, text/event-stream";
  static final String INITIALIZE_BODY =
      """
      {"jsonrpc":"2.0","id":1,"method":"initialize","params":{\
      "protocolVersion":"%s","capabilities":{},\
      "clientInfo":{"name":"donut-mcp-test","version":"1.0.0"}}}
      """
          .formatted(ProtocolVersions.MCP_2025_11_25);
  static final String TOOLS_LIST_BODY =
      """
      {"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
      """;

  @Autowired MockMvc mockMvc;
  @Autowired UserService userService;

  UserToken personalToken;
  User personalUser;

  @BeforeEach
  void issuePersonalToken() {
    personalUser = makeMe.aUser().please();
    personalToken = makeMe.aUserToken().forUser(personalUser).withLabel("mcp").please();
  }

  MockHttpServletRequestBuilder mcpPost(String body) {
    return post(McpServerConfiguration.MCP_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.ACCEPT, MCP_ACCEPT)
        .content(body);
  }

  MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
    return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + personalToken.getToken());
  }
}
