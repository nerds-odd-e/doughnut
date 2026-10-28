package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.UserToken;
import com.odde.donut.mcp.McpServerConfiguration;
import com.odde.donut.mcp.ReadNoteTool;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * MCP {@code read_note} at the HTTP boundary, reusing Git-backed notebook fixtures for attribution.
 */
class ReadNoteMcpEndpointTest extends NotebookGitWebContentControllerTestBase {
  private static final String MCP_ACCEPT = "application/json, text/event-stream";

  @Autowired MockMvc mockMvc;
  @Autowired JsonMapper objectMapper;

  @Test
  void readsTheNoteWithItsSourceAttribution() throws Exception {
    Timestamp updatedAt = Timestamp.from(Instant.parse("2026-09-06T04:05:06Z"));
    Notebook notebook = createGitBackedNotebook("Physics");
    Folder outer = makeMe.aFolder().notebook(notebook).name("Mechanics").please();
    Folder inner = makeMe.aFolder().parentFolder(outer).name("Energy").please();
    Note note =
        makeMe
            .aNote()
            .folder(inner)
            .title("Kinetic")
            .content(ACCEPTED_CONTENT)
            .updatedAt(updatedAt)
            .please();
    String head = snapshotCurrentPortableTree(notebook).getAcceptedGitObjectId();
    UserToken token = tokenForOwner();

    JsonNode result = callReadNoteResult(token, note.getId());
    JsonNode text = objectMapper.readTree(result.at("/content/0/text").asText());
    JsonNode read = result.get("structuredContent");

    assertThat(read, equalTo(text));
    assertThat(read.get("noteId").asInt(), equalTo(note.getId()));
    assertThat(read.get("title").asText(), equalTo("Kinetic"));
    assertThat(read.get("notebookName").asText(), equalTo("Physics"));
    assertThat(read.get("folderPath").asText(), equalTo("Mechanics / Energy"));
    assertThat(read.get("content").asText(), equalTo(ACCEPTED_CONTENT));
    assertThat(read.get("updatedAt").asText(), equalTo("2026-09-06T04:05:06.000+00:00"));
    assertThat(read.get("acceptedRevision").asText(), equalTo(head));
    assertThat(head, matchesPattern("[0-9a-f]{40}"));

    mockMvc
        .perform(
            authorized(
                mcpPost("{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/list\",\"params\":{}}"),
                token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.tools[1].outputSchema.type").value("object"))
        .andExpect(
            jsonPath("$.result.tools[1].outputSchema.properties.noteId.type").value("integer"))
        .andExpect(
            jsonPath("$.result.tools[1].outputSchema.properties.acceptedRevision.type")
                .value("string"))
        .andExpect(
            jsonPath(
                "$.result.tools[1].outputSchema.required",
                contains("noteId", "title", "notebookName", "folderPath", "updatedAt")));
  }

  @Test
  void aContentEditInTheNotebookChangesTheRevisionOfALaterRead() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("Read").content(ACCEPTED_CONTENT).please();
    Note edited =
        makeMe.aNote().notebook(notebook).title("Edited").content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    UserToken token = tokenForOwner();
    String earlier = callReadNote(token, note.getId()).get("acceptedRevision").asText();

    textContentController.updateNoteContent(edited, contentDto(EDITED_CONTENT));

    assertThat(
        callReadNote(token, note.getId()).get("acceptedRevision").asText(), not(equalTo(earlier)));
    assertThat(earlier, matchesPattern("[0-9a-f]{40}"));
  }

  @Test
  void aNotebookWithoutGitBindingReadsWithoutRevision() throws Exception {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    UserToken token = tokenForOwner();

    JsonNode read = callReadNote(token, note.getId());

    assertThat(read.has("acceptedRevision"), equalTo(false));
  }

  @Test
  void refusesANoteTheUserCannotRead() throws Exception {
    Note foreign = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    UserToken token = tokenForOwner();

    mockMvc
        .perform(authorized(mcpPost(toolsCallBody(foreign.getId())), token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.error.code").exists())
        .andExpect(jsonPath("$.error.message").value("It seems you cannot access this page."));
  }

  private UserToken tokenForOwner() {
    return makeMe.aUserToken().forUser(currentUser.getUser()).withLabel("mcp-read").please();
  }

  private JsonNode callReadNote(UserToken token, Integer noteId) throws Exception {
    return objectMapper.readTree(callReadNoteResult(token, noteId).at("/content/0/text").asText());
  }

  private JsonNode callReadNoteResult(UserToken token, Integer noteId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(authorized(mcpPost(toolsCallBody(noteId)), token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].text").exists())
            .andReturn();
    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    return root.get("result");
  }

  private static MockHttpServletRequestBuilder mcpPost(String body) {
    return post(McpServerConfiguration.MCP_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.ACCEPT, MCP_ACCEPT)
        .content(body);
  }

  private static MockHttpServletRequestBuilder authorized(
      MockHttpServletRequestBuilder request, UserToken token) {
    return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getToken());
  }

  private static String toolsCallBody(Integer noteId) {
    return """
        {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{\
        "name":"%s","arguments":{"noteId":%d}}}
        """
        .formatted(ReadNoteTool.NAME, noteId);
  }
}
