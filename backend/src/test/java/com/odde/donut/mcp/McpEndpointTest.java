package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.ProtocolVersions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class McpEndpointTest extends McpEndpointTestBase {

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
    void initializesAndListsRegisteredToolsThroughServlet() throws Exception {
      mockMvc
          .perform(authorized(mcpPost(INITIALIZE_BODY)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.serverInfo.name", equalTo("donut")))
          .andExpect(jsonPath("$.result.protocolVersion", equalTo(ProtocolVersions.MCP_2025_11_25)))
          .andExpect(jsonPath("$.result.capabilities.tools").exists());

      mockMvc
          .perform(authorized(mcpPost(TOOLS_LIST_BODY)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.tools[0].name", equalTo(ListNoteCandidatesTool.NAME)))
          .andExpect(jsonPath("$.result.tools[0].inputSchema.properties.query").exists())
          .andExpect(jsonPath("$.result.tools[1].name", equalTo(ReadNoteTool.NAME)))
          .andExpect(jsonPath("$.result.tools[1].inputSchema.properties.noteId").exists())
          .andExpect(jsonPath("$.result.tools[2].name", equalTo(GetNoteGraphTool.NAME)))
          .andExpect(jsonPath("$.result.tools[2].inputSchema.properties.noteId").exists())
          .andExpect(jsonPath("$.result.tools[2].inputSchema.properties.tokenLimit").exists())
          .andExpect(jsonPath("$.result.tools[3].name", equalTo(RequestLearningSessionTool.NAME)))
          .andExpect(jsonPath("$.result.tools[3].inputSchema.properties.timezone").exists())
          .andExpect(jsonPath("$.result.tools[4].name", equalTo(RecordLearningSessionTool.NAME)))
          .andExpect(jsonPath("$.result.tools[4].inputSchema.properties.reportMarkdown").exists())
          .andExpect(
              jsonPath(
                  "$.result.tools[*].securitySchemes[0].type",
                  contains("oauth2", "oauth2", "oauth2", "oauth2", "oauth2")))
          .andExpect(
              result -> {
                String json = result.getResponse().getContentAsString();
                var mapper = McpJsonDefaults.getMapper();
                var response = mapper.readValue(json, McpSchema.JSONRPCResponse.class);
                var decodedTools =
                    mapper.convertValue(response.result(), McpSchema.ListToolsResult.class);
                assertThat(decodedTools.tools(), hasSize(5));
                JsonNode tools = new ObjectMapper().readTree(json).path("result").path("tools");
                for (JsonNode tool : tools) {
                  assertThat(
                      tool.path("securitySchemes"),
                      equalTo(tool.path("_meta").path("securitySchemes")));
                }
              })
          .andExpect(
              jsonPath(
                  "$.result.tools[*]._meta.securitySchemes[0].type",
                  contains("oauth2", "oauth2", "oauth2", "oauth2", "oauth2")))
          .andExpect(
              jsonPath("$.result.tools[0]._meta.securitySchemes[0].scopes", contains("notes:read")))
          .andExpect(
              jsonPath("$.result.tools[1]._meta.securitySchemes[0].scopes", contains("notes:read")))
          .andExpect(
              jsonPath("$.result.tools[2]._meta.securitySchemes[0].scopes", contains("notes:read")))
          .andExpect(
              jsonPath("$.result.tools[3]._meta.securitySchemes[0].scopes", contains("notes:read")))
          .andExpect(
              jsonPath(
                  "$.result.tools[4]._meta.securitySchemes[0].scopes",
                  contains("notes:read", "learning_session:record")))
          .andExpect(
              jsonPath(
                  "$.result.tools[*].annotations.readOnlyHint",
                  contains(true, true, true, true, false)))
          .andExpect(
              jsonPath(
                  "$.result.tools[*].annotations.destructiveHint",
                  contains(false, false, false, false, false)))
          .andExpect(
              jsonPath(
                  "$.result.tools[*].annotations.openWorldHint",
                  contains(false, false, false, false, false)));
    }
  }
}
