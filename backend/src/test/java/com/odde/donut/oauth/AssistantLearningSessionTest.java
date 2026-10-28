package com.odde.donut.oauth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.entities.repositories.RecallLogRepository;
import com.odde.donut.entities.repositories.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

class AssistantLearningSessionTest extends AssistantAuthorizationTestSupport {
  @Autowired UserRepository users;
  @Autowired RecallLogRepository recallLogs;

  @Test
  void consentedRecordScopeSavesFeedbackThroughMcp() throws Exception {
    var note =
        makeMe
            .aNote("こんにちは")
            .notebookOwnedBy(users.findByExternalIdentifier("old_learner"))
            .please();
    var tracker = makeMe.aMemoryTrackerFor(note).commissioned().please();

    var result = record("notes:read learning_session:record", note.getNotebook().getId());

    assertThat(
        result.at("/result/structuredContent/recordedItems/0/noteTitle").asString(), is("こんにちは"));
    assertThat(
        recallLogs
            .findAllByMemoryTracker_IdOrderByRecordedAtDescIdDesc(tracker.getId())
            .getFirst()
            .getTutorFeedback(),
        is("Clear pronunciation."));
  }

  @Test
  void readOnlyAssistantCannotRecordEvenWithItsBrowserSession() throws Exception {
    var note =
        makeMe
            .aNote("こんにちは")
            .notebookOwnedBy(users.findByExternalIdentifier("old_learner"))
            .please();
    var tracker = makeMe.aMemoryTrackerFor(note).commissioned().please();
    var dueBefore = tracker.getNextRecallAt();

    var result = record("notes:read", note.getNotebook().getId());

    assertThat(result.at("/error/message").asString(), containsString("learning_session:record"));
    assertThat(recallLogs.count(), is(0L));
    assertThat(tracker.getNextRecallAt(), equalTo(dueBefore));
  }

  private JsonNode record(String scopes, Integer notebookId) throws Exception {
    var reply =
        exchange(approvedCode(scopes), Map.of(), "test-assistant-secret")
            .andExpect(status().isOk())
            .andReturn();
    String token =
        objectMapper
            .readTree(reply.getResponse().getContentAsString())
            .get("access_token")
            .asString();
    var result =
        mockMvc
            .perform(
                post("/api/mcp")
                    .cookie(session)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.ACCEPT, "application/json, text/event-stream")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "jsonrpc",
                                "2.0",
                                "id",
                                1,
                                "method",
                                "tools/call",
                                "params",
                                Map.of(
                                    "name",
                                    "record_learning_session",
                                    "arguments",
                                    Map.of(
                                        "notebookId",
                                        notebookId,
                                        "reportMarkdown",
                                        "<session_item_feedback>\n<session_item>\nこんにちは: 4\nClear pronunciation.\n</session_item>\n</session_item_feedback>"))))))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }
}
