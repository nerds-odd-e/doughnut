package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookGit.WebNoteEditService;
import com.odde.donut.validators.AuthoredNoteContent;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class GraphNotePortablePathMcpEndpointTest extends McpEndpointTestBase {
  @Autowired JsonMapper objectMapper;
  @Autowired WebNoteEditService webNoteEditService;
  @Autowired CanonicalDonutOrigin canonicalDonutOrigin;

  @Test
  void retrievesAKnownQualifiedGraphWithoutDiscovery() throws Exception {
    Notebook notebook = engineering();
    Folder practices = makeMe.aFolder().notebook(notebook).name("Practices").please();
    makeMe.aNote("Code Review").folder(practices).content("Review guidance").please();
    makeMe.aNote("Code Review").notebook(notebook).content("Root guidance").please();

    JsonNode graph = graph("Engineering:Practices/Code Review", "Other");

    assertThat(graph.at("/focusNote/content").asText(), equalTo("Review guidance"));
    assertThat(graph.at("/focusNote/folderPath").asText(), equalTo("Practices"));
  }

  @Test
  void retrievesADiscoveredNestedAliasPathUnchanged() throws Exception {
    Notebook notebook = engineering();
    Folder practices = makeMe.aFolder().notebook(notebook).name("Practices").please();
    Folder design = makeMe.aFolder().parentFolder(practices).name("Design").please();
    var target = makeMe.aNote("Colour Theory").folder(design).aliases("color").please();
    makeMe.aNote("Colour Theory").notebook(notebook).please();
    JsonNode candidates = text(call("list_note_candidates", Map.of("query", "color")));
    String discoveredPath = candidates.get(0).get("portablePath").asText();

    JsonNode graph = graph(discoveredPath, "");

    assertThat(graph.at("/focusNote/content").asText(), equalTo(target.getContent()));
    assertThat(graph.at("/focusNote/folderPath").asText(), equalTo("Practices / Design"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Review", "Review#prop:topic", "Review#prop:review%20topic"})
  void scopedAliasAndPropertyReturnTheContainingGraph(String path) throws Exception {
    Notebook notebook = engineering();
    String content =
        "---\naliases: [Review]\ntopic: quality\nreview topic: methods\n---\nFull guidance";
    var target = makeMe.aNote("Code Review").notebook(notebook).please();
    currentUser.setUser(personalUser);
    var saved =
        webNoteEditService.saveContent(
            target.getId(),
            notebook.getId(),
            AuthoredNoteContent.prepareDocumentForSave(content, canonicalDonutOrigin),
            testabilitySettings.getCurrentUTCTimestamp());

    JsonNode graph = graph(path, "Engineering");

    assertThat(graph.at("/focusNote/title").asText(), equalTo("Code Review"));
    assertThat(graph.at("/focusNote/content").asText(), equalTo(saved.getContent()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Code Review", "Code Review#prop:topic"})
  void ambiguityReturnsPathsThatSelectTheIntendedGraph(String path) throws Exception {
    Notebook notebook = engineering();
    Folder practices = makeMe.aFolder().notebook(notebook).name("Practices").please();
    makeMe.aNote("Code Review").folder(practices).content("Folder guidance").please();
    makeMe.aNote("Code Review").notebook(notebook).content("Root guidance").please();

    JsonNode result =
        call(
            "get_note_graph",
            Map.of("portablePath", path, "notebookName", "Engineering", "tokenLimit", 1000));
    assertThat(result.get("isError").asBoolean(), equalTo(true));
    String message = result.at("/content/0/text").asText();
    JsonNode choices =
        objectMapper.readTree(message.substring(message.indexOf('\n') + 1)).get("candidates");
    assertThat(choices.size(), equalTo(2));
    for (JsonNode choice : choices) {
      JsonNode graph = graph(choice.get("portablePath").asText(), "");
      String expected =
          choice.get("folderPath").asText().isEmpty() ? "Root guidance" : "Folder guidance";
      assertThat(graph.at("/focusNote/content").asText(), equalTo(expected));
    }
  }

  @Test
  void unqualifiedGraphNeedsExplicitScope() throws Exception {
    makeMe.aNote("Code Review").notebook(engineering()).please();

    JsonNode result =
        call("get_note_graph", Map.of("portablePath", "Code Review", "tokenLimit", 1000));

    assertThat(result.get("isError").asBoolean(), equalTo(true));
    assertThat(result.at("/content/0/text").asText(), containsString("Supply notebookName"));
  }

  private Notebook engineering() {
    return makeMe.aNotebook().creatorAndOwner(personalUser).name("Engineering").please();
  }

  private JsonNode graph(String path, String notebookName) throws Exception {
    return text(
        call(
            "get_note_graph",
            Map.of("portablePath", path, "notebookName", notebookName, "tokenLimit", 1000)));
  }

  private JsonNode text(JsonNode result) throws Exception {
    return objectMapper.readTree(result.at("/content/0/text").asText());
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
    return objectMapper.readTree(response.getContentAsString()).get("result");
  }
}
