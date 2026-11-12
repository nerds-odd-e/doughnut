package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class ReadNoteScopedPathMcpEndpointTest extends McpEndpointTestBase {
  @Autowired JsonMapper objectMapper;
  @Autowired WebNoteEditService webNoteEditService;
  @Autowired CanonicalDonutOrigin canonicalDonutOrigin;

  @Test
  void advertisesOptionalExplicitNotebookScope() throws Exception {
    mockMvc
        .perform(authorized(mcpPost(TOOLS_LIST_BODY)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.result.tools[1].inputSchema.properties.notebookName.type").value("string"))
        .andExpect(jsonPath("$.result.tools[1].inputSchema.required", contains("portablePath")));
  }

  @Test
  void readableDisplayNameWinsOverAliases() throws Exception {
    Notebook notebook = engineering();
    makeMe.aNote("Review").notebook(notebook).content("Display name target").please();
    makeMe.aNote("Code Review").notebook(notebook).aliases("Review").please();
    makeMe.aNote("Peer Review").notebook(notebook).aliases("Review").please();

    assertThat(read("Review").get("content").asText(), equalTo("Display name target"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Review", "review", "A\\|B|Read this"})
  void resolvesAliasesUsingSharedWikiSpelling(String path) throws Exception {
    Notebook notebook = engineering();
    var target = makeMe.aNote("Code Review").notebook(notebook).aliases("Review", "A|B").please();

    assertThat(read(path).get("content").asText(), equalTo(target.getContent()));
  }

  @ParameterizedTest
  @CsvSource({
    "Practices/Design/Code Review, Nested target",
    "Practices/Design/Code Review.md, Nested target",
    "/Code Review, Root target",
    "/Code Review.md, Root target"
  })
  void selectsTheFullFolderTrailOrExactRoot(String path, String expected) throws Exception {
    Notebook notebook = engineering();
    Folder practices = makeMe.aFolder().notebook(notebook).name("Practices").please();
    Folder design = makeMe.aFolder().parentFolder(practices).name("Design").please();
    Folder other = makeMe.aFolder().notebook(notebook).name("Other").please();
    Folder otherDesign = makeMe.aFolder().parentFolder(other).name("Design").please();
    makeMe.aNote("Code Review").folder(design).content("Nested target").please();
    makeMe.aNote("Code Review").folder(otherDesign).content("Other target").please();
    makeMe.aNote("Code Review").notebook(notebook).content("Root target").please();

    assertThat(read(path).get("content").asText(), equalTo(expected));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Review", "inspect", "Practices/Review", "/Review"})
  void requiresScopeEvenAfterAReadInTheUsersNotebook(String path) throws Exception {
    Notebook notebook = engineering();
    makeMe.aNote("Review").notebook(notebook).aliases("inspect").please();
    Folder practices = makeMe.aFolder().notebook(notebook).name("Practices").please();
    makeMe.aNote("Review").folder(practices).please();
    assertThat(
        call(Map.of("portablePath", "Engineering:/Review"))
            .at("/structuredContent/portablePath")
            .asText(),
        equalTo("Engineering:/Review"));

    JsonNode result = call(Map.of("portablePath", path));

    assertThat(result.get("isError").asBoolean(), equalTo(true));
    assertThat(result.at("/content/0/text").asText(), containsString("Supply notebookName"));
  }

  @Test
  void blankScopeReceivesActionableFeedback() throws Exception {
    JsonNode result = call(Map.of("portablePath", "Review", "notebookName", " "));

    assertThat(result.at("/content/0/text").asText(), containsString("Supply notebookName"));
  }

  @Test
  void qualifiedDestinationSuppliesScopeDespiteADifferentFallback() throws Exception {
    makeMe.aNote("Review").notebook(engineering()).content("Engineering target").please();
    Notebook other = makeMe.aNotebook().creatorAndOwner(personalUser).name("Other").please();
    makeMe.aNote("Review").notebook(other).content("Other target").please();

    JsonNode result = call(Map.of("portablePath", "Engineering:Review", "notebookName", "Other"));

    assertThat(result.at("/structuredContent/content").asText(), equalTo("Engineering target"));
  }

  @Test
  void propertySelectorReturnsTheContainingFullNote() throws Exception {
    String content = "---\ntopic name: authored value\n---\nFull body [[Review]] with spacing.\n";
    makeMe.aNote("Review").notebook(engineering()).content(content).please();

    assertThat(read("Review#prop:topic%20name").get("content").asText(), equalTo(content));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Topic%20name", "topic", "topic%2520name", "%"})
  void requiresTheExactDecodedPropertyKey(String key) throws Exception {
    makeMe
        .aNote("Review")
        .notebook(engineering())
        .content("---\ntopic name: value\n---\n")
        .please();

    assertUnresolved(
        call(Map.of("portablePath", "Review#prop:" + key, "notebookName", "Engineering")));
  }

  @Test
  void settlesTheDisplayNameBeforeCheckingItsProperty() throws Exception {
    Notebook notebook = engineering();
    makeMe.aNote("Review").notebook(notebook).please();
    var alias = makeMe.aNote("Code Review").notebook(notebook).please();
    currentUser.setUser(personalUser);
    webNoteEditService.saveContent(
        alias.getId(),
        notebook.getId(),
        AuthoredNoteContent.prepareDocumentForSave(
            "---\naliases: [Review]\ntopic: alias value\n---\n", canonicalDonutOrigin),
        testabilitySettings.getCurrentUTCTimestamp());

    assertUnresolved(
        call(Map.of("portablePath", "Review#prop:topic", "notebookName", "Engineering")));
  }

  @Test
  void settlesNoteCardinalityBeforeCheckingProperties() throws Exception {
    Notebook notebook = engineering();
    makeMe.aNote("Review").notebook(notebook).content("---\ntopic: value\n---\n").please();
    Folder folder = makeMe.aFolder().notebook(notebook).please();
    makeMe.aNote("Review").folder(folder).please();

    JsonNode result =
        call(Map.of("portablePath", "Review#prop:topic", "notebookName", "Engineering"));

    assertThat(result.get("isError").asBoolean(), equalTo(true));
    assertThat(
        result.at("/content/0/text").asText(), containsString("Several readable notes match"));
  }

  @Test
  void trashedDisplayNameLeavesTheAvailableAlias() throws Exception {
    Notebook notebook = engineering();
    makeMe.aNote("Review").notebook(notebook).trashed().please();
    var target = makeMe.aNote("Code Review").notebook(notebook).aliases("Review").please();

    assertThat(read("Review").get("content").asText(), equalTo(target.getContent()));
  }

  private void assertUnresolved(JsonNode result) {
    assertThat(result.get("isError").asBoolean(), equalTo(true));
    assertThat(result.at("/content/0/text").asText(), containsString("No readable note matches"));
  }

  private Notebook engineering() {
    return makeMe.aNotebook().creatorAndOwner(personalUser).name("Engineering").please();
  }

  private JsonNode read(String path) throws Exception {
    return call(Map.of("portablePath", path, "notebookName", "Engineering"))
        .get("structuredContent");
  }

  private JsonNode call(Map<String, Object> arguments) throws Exception {
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
                Map.of("name", "read_note", "arguments", arguments)));
    var response =
        mockMvc
            .perform(authorized(mcpPost(body)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    return objectMapper.readTree(response.getContentAsString()).get("result");
  }
}
