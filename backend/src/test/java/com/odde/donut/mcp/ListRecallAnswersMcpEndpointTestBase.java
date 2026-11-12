package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

/** Calls the recall answers tool for a learner with a note "Code Review" in "Engineering". */
abstract class ListRecallAnswersMcpEndpointTestBase extends McpEndpointTestBase {
  @Autowired JsonMapper objectMapper;

  Timestamp now;
  Note note;

  @BeforeEach
  void setup() {
    now = makeMe.aTimestamp().of(10, 9).please();
    testabilitySettings.timeTravelTo(now);
    var notebook = makeMe.aNotebook().creatorAndOwner(personalUser).name("Engineering").please();
    note = makeMe.aNote("Code Review").notebook(notebook).please();
  }

  MemoryTracker trackerOf(User learner) {
    return makeMe.aMemoryTrackerFor(note).by(learner).please();
  }

  Timestamp hoursAgo(int hours) {
    return TimestampOperations.addHoursToTimestamp(now, -hours);
  }

  static List<String> texts(JsonNode array) {
    return texts(array, "");
  }

  static List<String> texts(JsonNode array, String pointer) {
    return StreamSupport.stream(array.spliterator(), false)
        .map(node -> node.at(pointer).asText())
        .toList();
  }

  static void assertTitleAndPathOnly(JsonNode sourceNote) {
    assertThat(fieldNames(sourceNote), containsInAnyOrder("title", "portablePath"));
    assertThat(sourceNote.get("portablePath").asText(), equalTo("Engineering:Code Review"));
  }

  private static List<String> fieldNames(JsonNode node) {
    return StreamSupport.stream(((Iterable<String>) () -> node.fieldNames()).spliterator(), false)
        .toList();
  }

  JsonNode listRecallAnswers() throws Exception {
    return listRecallAnswers(Map.of());
  }

  String callBody(Map<String, Object> arguments) throws Exception {
    return objectMapper.writeValueAsString(
        Map.of(
            "jsonrpc",
            "2.0",
            "id",
            3,
            "method",
            "tools/call",
            "params",
            Map.of("name", ListRecallAnswersTool.NAME, "arguments", arguments)));
  }

  JsonNode listRecallAnswers(Map<String, Object> arguments) throws Exception {
    var response =
        mockMvc
            .perform(authorized(mcpPost(callBody(arguments))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    return objectMapper.readTree(
        objectMapper.readTree(response.getContentAsString()).at("/result/content/0/text").asText());
  }
}
