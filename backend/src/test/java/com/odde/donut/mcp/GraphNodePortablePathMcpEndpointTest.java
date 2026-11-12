package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

class GraphNodePortablePathMcpEndpointTest extends McpEndpointTestBase {
  @Autowired JsonMapper objectMapper;

  private static final String TITLE = "Topic | Tips\\Examples";
  private static final String ESCAPED_TITLE = "Topic \\| Tips\\\\Examples";
  private static final String ROOT_FOCUS = "Engineering:/Focus";
  private static final String FOLDER_FOCUS = "Engineering:Practices/Focus";
  private static final String ROOT_RELATED = "Engineering:/" + ESCAPED_TITLE;
  private static final String FOLDER_RELATED = "Engineering:Practices/" + ESCAPED_TITLE;

  @BeforeEach
  void graphWithRootAndFolderNamesakes() {
    Notebook notebook =
        makeMe.aNotebook().creatorAndOwner(personalUser).name("Engineering").please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Practices").please();
    makeMe.aNote(TITLE).folder(folder).content("Folder related target").please();
    makeMe.aNote(TITLE).notebook(notebook).content("Root related target").please();
    makeMe
        .aNote("Focus")
        .notebook(notebook)
        .content("Root focus links to [[" + FOLDER_RELATED + "]].")
        .please();
    makeMe.aNote("Focus").folder(folder).content("Folder focus target").please();
  }

  @ParameterizedTest
  @CsvSource({
    "rootFocus, Root focus links to [[Engineering:Practices/Topic \\| Tips\\\\Examples]].",
    "folderFocus, Folder focus target",
    "rootRelated, Root related target",
    "folderRelated, Folder related target"
  })
  void returnedNodeAddressSelectsItsFullContent(String node, String content) throws Exception {
    JsonNode graph =
        call(
            "get_note_graph",
            Map.of(
                "portablePath",
                node.equals("folderFocus") ? FOLDER_FOCUS : ROOT_FOCUS,
                "tokenLimit",
                2000));
    JsonNode returned = graph.get("focusNote");
    if (node.endsWith("Related")) {
      for (JsonNode related : graph.get("relatedNotes")) {
        if (related.get("content").asText().equals(content)) {
          returned = related;
          break;
        }
      }
    }
    String expectedPath =
        switch (node) {
          case "folderFocus" -> FOLDER_FOCUS;
          case "rootRelated" -> ROOT_RELATED;
          case "folderRelated" -> FOLDER_RELATED;
          default -> ROOT_FOCUS;
        };
    String portablePath = returned.get("portablePath").asText();

    assertThat(portablePath, equalTo(expectedPath));
    assertThat(
        call("read_note", Map.of("portablePath", portablePath)).get("content").asText(),
        equalTo(content));
  }

  private JsonNode call(String name, Map<String, Object> arguments) throws Exception {
    String body =
        objectMapper.writeValueAsString(
            Map.of(
                "jsonrpc",
                "2.0",
                "id",
                3,
                "method",
                "tools/call",
                "params",
                Map.of("name", name, "arguments", arguments)));
    var response =
        mockMvc
            .perform(authorized(mcpPost(body)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    return objectMapper.readTree(
        objectMapper.readTree(response.getContentAsString()).at("/result/content/0/text").asText());
  }
}
