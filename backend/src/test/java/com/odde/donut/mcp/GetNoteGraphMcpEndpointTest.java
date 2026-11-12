package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

class GetNoteGraphMcpEndpointTest extends McpEndpointTestBase {
  @Autowired JsonMapper objectMapper;

  @Test
  void largeBudgetReturnsFocusAndFolderPeerRelatedContext() throws Exception {
    Folder folder = makeMe.aFolder().notebookOwnedBy(personalUser).name("Programming").please();
    Note focus = makeMe.aNote().folder(folder).title("Functional").please();
    makeMe.aNote().folder(folder).title("Object Oriented").please();

    JsonNode graph = callGetNoteGraph(focus, 1000);

    assertThat(graph.at("/focusNote/title").asText(), equalTo("Functional"));
    assertThat(graph.at("/focusNote/notebook").asText(), equalTo(folder.getNotebook().getName()));
    assertThat(relatedTitles(graph), hasItem("Object Oriented"));
  }

  @Test
  void smallBudgetOmitsFolderPeerRelatedContext() throws Exception {
    Folder folder = makeMe.aFolder().notebookOwnedBy(personalUser).please();
    Note focus = makeMe.aNote().folder(folder).title("Functional").please();
    makeMe.aNote().folder(folder).title("Object Oriented").please();

    JsonNode graph = callGetNoteGraph(focus, 10);

    assertThat(graph.at("/focusNote/title").asText(), equalTo("Functional"));
    assertThat(graph.at("/focusNote/sampleSiblings").size(), equalTo(0));
    assertThat(relatedTitles(graph), not(hasItem("Object Oriented")));
  }

  @ParameterizedTest
  @CsvSource({"0, tokenLimit must be a positive number", "5, tokenLimit too low to fetch any note"})
  void refusesUselessTokenLimits(int tokenLimit, String message) throws Exception {
    Note focus = makeMe.aNote().notebookOwnedBy(personalUser).please();

    mockMvc
        .perform(authorized(mcpPost(toolsCallBody(focus, tokenLimit))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.error.message", containsString(message)));
  }

  @Test
  void refusesAForeignPrivateFocus() throws Exception {
    Note foreign = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();

    mockMvc
        .perform(authorized(mcpPost(toolsCallBody(foreign, 1000))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.isError").value(true))
        .andExpect(
            jsonPath("$.result.content[0].text", containsString("No readable note matches")));
  }

  @Test
  void filtersForeignPrivateRelatedContentUnderExistingReadRule() throws Exception {
    User other = makeMe.aUser().please();
    Notebook secretNb = makeMe.aNotebook().creatorAndOwner(other).name("SecretNb").please();
    makeMe.aNote().title("Hidden").notebook(secretNb).please();
    Note focus =
        makeMe
            .aNote()
            .notebookOwnedBy(personalUser)
            .title("Focus")
            .content("See [[SecretNb:Hidden]] and [[Linked]].")
            .please();
    makeMe.aNote().underSameNotebookAs(focus).title("Linked").content("ok").please();

    JsonNode graph = callGetNoteGraph(focus, 1000);

    assertThat(relatedTitles(graph), hasItem("Linked"));
    assertThat(relatedTitles(graph), not(hasItem("Hidden")));
    assertThat(graph.at("/focusNote/outgoingLinks").toString(), not(containsString("Hidden")));
  }

  private JsonNode callGetNoteGraph(Note note, int tokenLimit) throws Exception {
    MvcResult result =
        mockMvc
            .perform(authorized(mcpPost(toolsCallBody(note, tokenLimit))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].text").exists())
            .andReturn();
    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    return objectMapper.readTree(root.at("/result/content/0/text").asText());
  }

  private String toolsCallBody(Note note, int tokenLimit) throws Exception {
    return objectMapper.writeValueAsString(
        Map.of(
            "jsonrpc",
            "2.0",
            "id",
            3,
            "method",
            "tools/call",
            "params",
            Map.of(
                "name",
                GetNoteGraphTool.NAME,
                "arguments",
                Map.of(
                    "portablePath",
                    note.getNotebook().getName() + ":" + note.getTitle(),
                    "tokenLimit",
                    tokenLimit))));
  }

  private static List<String> relatedTitles(JsonNode graph) {
    List<String> titles = new ArrayList<>();
    for (JsonNode related : graph.get("relatedNotes")) {
      titles.add(related.get("title").asText());
    }
    return titles;
  }
}
