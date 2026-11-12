package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReadNotePortablePathMcpEndpointTest extends McpEndpointTestBase {
  @Autowired JsonMapper objectMapper;

  @Test
  void readsAKnownQualifiedDestinationWithoutDiscovery() throws Exception {
    Notebook notebook = engineering();
    Folder practices = makeMe.aFolder().notebook(notebook).name("Practices").please();
    String content = "Authored [[Engineering:Principles]] and [Reference](/n1234).";
    makeMe.aNote("Code Review").folder(practices).content(content).please();
    makeMe.aNote("Code Review").notebook(notebook).please();

    JsonNode read = call("read_note", Map.of("portablePath", "Engineering:Practices/Code Review"));

    assertThat(read.get("content").asText(), equalTo(content));
    assertThat(read.get("portablePath").asText(), equalTo("Engineering:Practices/Code Review"));
  }

  @Test
  void readsADiscoveredNestedAliasPathUnchanged() throws Exception {
    Notebook notebook = engineering();
    Folder practices = makeMe.aFolder().notebook(notebook).name("Practices").please();
    Folder design = makeMe.aFolder().parentFolder(practices).name("Design").please();
    var aliasMatch = makeMe.aNote("Colour Theory").folder(design).aliases("color").please();
    makeMe.aNote("Colour Theory").notebook(notebook).please();
    JsonNode candidates = call("list_note_candidates", Map.of("query", "color"));
    String discoveredPath = candidates.get(0).get("portablePath").asText();

    JsonNode read = call("read_note", Map.of("portablePath", discoveredPath));

    assertThat(read.get("content").asText(), equalTo(aliasMatch.getContent()));
    assertThat(read.get("portablePath").asText(), equalTo(discoveredPath));
  }

  @Test
  void exactRootDestinationSelectsTheRootNote() throws Exception {
    Notebook notebook = engineering();
    makeMe.aNote("Code Review").notebook(notebook).content("Root target").please();
    Folder practices = makeMe.aFolder().notebook(notebook).name("Practices").please();
    makeMe.aNote("Code Review").folder(practices).content("Folder target").please();

    JsonNode read = call("read_note", Map.of("portablePath", "Engineering:/Code Review"));

    assertThat(read.get("content").asText(), equalTo("Root target"));
  }

  @Test
  void missingDestinationRemainsUnresolved() throws Exception {
    mockMvc
        .perform(
            authorized(mcpPost(body("read_note", Map.of("portablePath", "Engineering:Missing")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.isError").value(true))
        .andExpect(
            jsonPath("$.result.content[0].text").value(containsString("No readable note matches")));
  }

  private Notebook engineering() {
    return makeMe.aNotebook().creatorAndOwner(personalUser).name("Engineering").please();
  }

  private JsonNode call(String name, Map<String, Object> arguments) throws Exception {
    var response =
        mockMvc
            .perform(authorized(mcpPost(body(name, arguments))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    return objectMapper.readTree(
        objectMapper.readTree(response.getContentAsString()).at("/result/content/0/text").asText());
  }

  private String body(String name, Map<String, Object> arguments) throws Exception {
    return objectMapper.writeValueAsString(
        Map.of(
            "jsonrpc",
            "2.0",
            "id",
            3,
            "method",
            "tools/call",
            "params",
            Map.of("name", name, "arguments", arguments)));
  }
}
