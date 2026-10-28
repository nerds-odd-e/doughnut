package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.odde.donut.entities.Notebook;
import java.sql.Timestamp;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LearningSessionRequestMcpEndpointTest extends McpEndpointTestBase {
  @Autowired JsonMapper objectMapper;

  @BeforeEach
  void setSessionTime() {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(2, 9).please());
  }

  @Test
  void returnsTheNotebookInstructionsAndSessionItemsInTheExistingMarkdownProtocol()
      throws Exception {
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(personalUser).name("Japanese").please();
    makeMe
        .theNotebook(notebook)
        .readmeContent("---\nquestion_generation_instruction: Practice polite greetings.\n---\n")
        .please();
    var note = makeMe.aNote("こんにちは").notebook(notebook).content("Hello in Japanese").please();
    makeMe.aMemoryTrackerFor(note).commissioned().please();

    JsonNode session = requestSession().at("/result/structuredContent");

    assertThat(session.get("notebookId").asInt(), equalTo(notebook.getId()));
    assertThat(session.get("notebookName").asText(), equalTo("Japanese"));
    String markdown = session.get("requestMarkdown").asText();
    assertThat(markdown, containsString("# Learning Session Request"));
    assertThat(markdown, containsString("Practice polite greetings."));
    assertThat(markdown, containsString("### こんにちは"));
    assertThat(markdown, containsString("Hello in Japanese"));
    assertThat(markdown, containsString("<how_to_report>"));
  }

  @Test
  void selectsTheFirstNotebookInDueOrder() throws Exception {
    Notebook later = notebookWithDueItem("Created first", makeMe.aTimestamp().of(1, 9).please());
    Notebook first = notebookWithDueItem("Due first", makeMe.aTimestamp().of(0, 9).please());
    var anotherItem = makeMe.aNote("Another item").notebook(later).please();
    makeMe
        .aMemoryTrackerFor(anotherItem)
        .commissioned()
        .nextRecallAt(makeMe.aTimestamp().of(1, 10).please())
        .please();

    assertThat(
        requestSession().at("/result/structuredContent/notebookId").asInt(),
        equalTo(first.getId()));
  }

  @Test
  void explainsWhenTheLearnerHasNoPotentialSession() throws Exception {
    var foreign = makeMe.aNote("Another learner's item").please();
    makeMe.aMemoryTrackerFor(foreign).commissioned().please();

    assertThat(
        requestSession().at("/result/content/0/text").asText(),
        equalTo(RequestLearningSessionTool.NO_SESSION_SIGNAL));
  }

  @Test
  void preservesNotebookAuthorizationForTrackedForeignContent() throws Exception {
    var foreign = makeMe.aNote("Private item").please();
    makeMe.aMemoryTrackerFor(foreign).by(personalUser).commissioned().please();

    assertThat(
        requestSession().at("/error/message").asText(),
        equalTo("It seems you cannot access this page."));
  }

  private Notebook notebookWithDueItem(String name, Timestamp dueAt) {
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(personalUser).name(name).please();
    var note = makeMe.aNote(name + " item").notebook(notebook).please();
    makeMe.aMemoryTrackerFor(note).commissioned().nextRecallAt(dueAt).please();
    return notebook;
  }

  private JsonNode requestSession() throws Exception {
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
                    RequestLearningSessionTool.NAME,
                    "arguments",
                    Map.of("timezone", "Asia/Tokyo"))));
    var response =
        mockMvc.perform(authorized(mcpPost(body))).andExpect(status().isOk()).andReturn();
    return objectMapper.readTree(response.getResponse().getContentAsString());
  }
}
