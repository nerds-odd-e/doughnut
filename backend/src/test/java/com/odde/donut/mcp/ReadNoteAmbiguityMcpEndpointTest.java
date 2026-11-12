package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class ReadNoteAmbiguityMcpEndpointTest extends McpEndpointTestBase {
  @Autowired JsonMapper objectMapper;

  @ParameterizedTest
  @ValueSource(strings = {"Code Review", "Code Review#prop:topic"})
  void returnsTheActualReadableResolutionCandidates(String path) throws Exception {
    Notebook notebook = candidates();
    makeMe.aNote("Review Handbook").notebook(notebook).aliases("Code Review").please();
    makeMe.aNote("Code Review").notebook(notebook).trashed().please();

    JsonNode result = read(path, "Engineering");
    JsonNode choices = ambiguity(result).get("candidates");

    assertThat(result.get("isError").asBoolean(), equalTo(true));
    assertThat(
        result.at("/content/0/text").asText(), containsString("Several readable notes match"));
    assertThat(
        choices,
        containsInAnyOrder(
            candidate("Engineering:Practices/Design/Code Review", "Practices/Design"),
            candidate("Engineering:Other/Design/Code Review", "Other/Design"),
            candidate("Engineering:/Code Review", "")));
  }

  @ParameterizedTest
  @CsvSource({"Practices/Design, Nested target", "Other/Design, Other target", "'', Root target"})
  void selectingAReturnedPathReadsTheIntendedFullNote(String folderPath, String content)
      throws Exception {
    candidates();
    JsonNode choices = ambiguity(read("Code Review", "Engineering")).get("candidates");
    String selected = null;
    for (JsonNode choice : choices) {
      if (choice.get("folderPath").asText().equals(folderPath)) {
        selected = choice.get("portablePath").asText();
      }
    }

    JsonNode result = read(selected, "");

    assertThat(result.at("/structuredContent/content").asText(), equalTo(content));
  }

  @Test
  void refusesCandidatesFromAnUnreadableNotebook() throws Exception {
    Notebook notebook =
        makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).name("Private").please();
    makeMe.aNote("Code Review").notebook(notebook).please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Confidential").please();
    makeMe.aNote("Code Review").folder(folder).please();

    JsonNode result = read("Code Review", "Private");

    assertThat(result.get("isError").asBoolean(), equalTo(true));
    assertThat(result.at("/content/0/text").asText(), containsString("No readable note matches"));
    assertThat(result.has("structuredContent"), equalTo(false));
  }

  private JsonNode candidate(String path, String folderPath) {
    return objectMapper.valueToTree(
        Map.of(
            "portablePath",
            path,
            "title",
            "Code Review",
            "notebookName",
            "Engineering",
            "folderPath",
            folderPath));
  }

  private JsonNode ambiguity(JsonNode result) throws Exception {
    String text = result.at("/content/0/text").asText();
    return objectMapper.readTree(text.substring(text.indexOf('\n') + 1));
  }

  private Notebook candidates() {
    Notebook notebook =
        makeMe.aNotebook().creatorAndOwner(personalUser).name("Engineering").please();
    Folder practices = makeMe.aFolder().notebook(notebook).name("Practices").please();
    Folder design = makeMe.aFolder().parentFolder(practices).name("Design").please();
    Folder other = makeMe.aFolder().notebook(notebook).name("Other").please();
    Folder otherDesign = makeMe.aFolder().parentFolder(other).name("Design").please();
    makeMe.aNote("Code Review").folder(design).content("Nested target").please();
    makeMe.aNote("Code Review").folder(otherDesign).content("Other target").please();
    makeMe.aNote("Code Review").notebook(notebook).content("Root target").please();
    return notebook;
  }

  private JsonNode read(String path, String notebookName) throws Exception {
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
                Map.of(
                    "name",
                    "read_note",
                    "arguments",
                    Map.of("portablePath", path, "notebookName", notebookName))));
    var response =
        mockMvc
            .perform(authorized(mcpPost(body)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    return objectMapper.readTree(response.getContentAsString()).get("result");
  }
}
