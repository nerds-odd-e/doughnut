package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.odde.donut.entities.Grade;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.RecallLogRepository;
import java.sql.Timestamp;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LearningSessionRecordMcpEndpointTest extends McpEndpointTestBase {
  @Autowired JsonMapper objectMapper;
  @Autowired RecallLogRepository recallLogs;
  Notebook notebook;

  @BeforeEach
  void prepareSession() {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(2, 9).please());
    notebook = makeMe.aNotebook().creatorAndOwner(personalUser).please();
  }

  @Test
  void earlyEndRecordsFeedbackForAAndPreservesIncompleteBAndC() throws Exception {
    var completed = item("A");
    var incomplete = item("B");
    var unstarted = item("C");
    var incompleteBefore = stateOf(incomplete);
    var unstartedBefore = stateOf(unstarted);
    var requested =
        call(RequestLearningSessionTool.NAME, Map.of("timezone", "Asia/Tokyo"))
            .at("/result/structuredContent/requestMarkdown")
            .asText();
    for (String title : new String[] {"A", "B", "C"}) {
      assertThat(requested, containsString("### " + title));
    }

    var result =
        record(
                notebook,
                """
        # Learning Session Report
        <session_item_feedback>
        <session_item>
        A: 3
        Correct answer after a small hint.
        </session_item>
        </session_item_feedback>
        """)
            .at("/result/structuredContent");

    assertThat(result.at("/recordedItems/0/memoryTrackerId").asInt(), equalTo(completed.getId()));
    var logs = recallLogs.findAllByMemoryTracker_IdOrderByRecordedAtDescIdDesc(completed.getId());
    assertThat(logs, hasSize(1));
    assertThat(logs.getFirst().getGrade(), is(Grade.GOOD));
    assertThat(logs.getFirst().getTutorFeedback(), is("Correct answer after a small hint."));
    assertThat(
        completed.getLastRecalledAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    assertThat(completed.getNextRecallAt(), equalTo(completed.calculateNextRecallAt()));
    assertThat(
        recallLogs.findAllByMemoryTracker_IdOrderByRecordedAtDescIdDesc(incomplete.getId()),
        empty());
    assertThat(
        recallLogs.findAllByMemoryTracker_IdOrderByRecordedAtDescIdDesc(unstarted.getId()),
        empty());
    assertThat(stateOf(incomplete), equalTo(incompleteBefore));
    assertThat(stateOf(unstarted), equalTo(unstartedBefore));
  }

  @Test
  void endingBeforeAnyCompletionRecordsNothing() throws Exception {
    var tracker = item("A");
    var before = stateOf(tracker);

    var result = record(notebook, "# Learning Session Report\n").at("/result/structuredContent");

    assertThat(result.get("recordedItems").size(), equalTo(0));
    assertThat(recallLogs.count(), equalTo(0L));
    assertThat(stateOf(tracker), equalTo(before));
  }

  @Test
  void rejectedTitlesAndInvalidGradesAreReturnedWithoutChangingLearningState() throws Exception {
    var tracker = item("A");
    var before = stateOf(tracker);

    var result =
        record(notebook, "# Learning Session Report\nUnknown: 3\nA: 5\n")
            .at("/result/structuredContent");

    assertThat(result.get("rejectedEntries").size(), equalTo(2));
    assertThat(result.get("rejectedEntries").toString(), containsString("Unknown"));
    assertThat(result.get("rejectedEntries").toString(), containsString("A: 5"));
    assertThat(recallLogs.count(), equalTo(0L));
    assertThat(stateOf(tracker), equalTo(before));
  }

  @Test
  void preservesForeignNotebookAuthorization() throws Exception {
    var foreign = makeMe.aNote("Private").please();
    makeMe.aMemoryTrackerFor(foreign).by(personalUser).commissioned().please();

    assertThat(
        record(foreign.getNotebook(), "Private: 3").at("/error/message").asText(),
        equalTo("It seems you cannot access this page."));
    assertThat(recallLogs.count(), equalTo(0L));
  }

  private MemoryTracker item(String title) {
    var note = makeMe.aNote(title).notebook(notebook).please();
    return makeMe.aMemoryTrackerFor(note).commissioned().please();
  }

  private JsonNode record(Notebook target, String markdown) throws Exception {
    return call(
        RecordLearningSessionTool.NAME,
        Map.of("notebookId", target.getId(), "reportMarkdown", markdown));
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
    var result = mockMvc.perform(authorized(mcpPost(body))).andExpect(status().isOk()).andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private TrackerState stateOf(MemoryTracker tracker) {
    return new TrackerState(
        tracker.getLastRecalledAt(),
        tracker.getRecallCount(),
        tracker.getStability(),
        tracker.getDifficulty(),
        tracker.getNextRecallAt());
  }

  private record TrackerState(
      Timestamp lastRecalledAt,
      Integer recallCount,
      Float stability,
      Float difficulty,
      Timestamp nextRecallAt) {}
}
