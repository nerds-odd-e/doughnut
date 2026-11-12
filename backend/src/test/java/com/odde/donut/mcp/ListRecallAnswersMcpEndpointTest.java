package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class ListRecallAnswersMcpEndpointTest extends McpEndpointTestBase {
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

  @Test
  void returnsTheFullRecordedMultipleChoiceAttempt() throws Exception {
    var mcq =
        makeMe
            .anMcq()
            .forNote(note)
            .stem("Who reviews code?")
            .choices("Peers", "Nobody", "The compiler")
            .correctAnswerIndex(0)
            .validationRationale("Peers review code.")
            .please();
    Timestamp yesterday = hoursAgo(24);
    makeMe
        .aRecallPrompt()
        .withMcq(mcq)
        .forMemoryTracker(trackerOf(personalUser))
        .answerChoiceIndex(1)
        .answerTimestamp(yesterday)
        .recorded()
        .please();
    makeMe.entityPersister.flushAndClear();

    JsonNode attempt = listRecallAnswers().get("attempts").get(0);

    assertThat(attempt.get("answeredAt").asText(), equalTo(yesterday.toInstant().toString()));
    assertThat(attempt.get("correct"), equalTo(BooleanNode.FALSE));
    assertThat(attempt.at("/note/title").asText(), equalTo("Code Review"));
    assertThat(attempt.at("/note/portablePath").asText(), equalTo("Engineering:Code Review"));
    JsonNode question = attempt.get("multipleChoice");
    assertThat(question.get("stem").asText(), equalTo("Who reviews code?"));
    assertThat(texts(question.get("choices")), contains("Peers", "Nobody", "The compiler"));
    assertThat(question.get("selectedChoice").asText(), equalTo("Nobody"));
    assertThat(question.get("expectedAnswer").asText(), equalTo("Peers"));
    assertThat(question.get("validationRationale").asText(), equalTo("Peers review code."));
  }

  @Test
  void returnsOnlyTheLearnersOwnAnswersOfTheLastSevenDaysNewestFirst() throws Exception {
    MemoryTracker tracker = trackerOf(personalUser);
    answeredAt(tracker, "Two days ago", hoursAgo(48));
    answeredAt(tracker, "Yesterday", hoursAgo(24));
    answeredAt(tracker, "Eight days ago", hoursAgo(8 * 24));
    answeredAt(trackerOf(makeMe.aUser().please()), "Another learner", hoursAgo(1));
    makeMe.aRecallLogFor(tracker).recordedAt(hoursAgo(2)).please();

    JsonNode result = listRecallAnswers();

    assertThat(
        texts(result.get("attempts"), "/multipleChoice/stem"),
        contains("Yesterday", "Two days ago"));
    assertThat(result.get("from").asText(), equalTo(hoursAgo(7 * 24).toInstant().toString()));
    assertThat(result.get("to").asText(), equalTo(now.toInstant().toString()));
  }

  @Test
  void looksBackTheGivenNumberOfDays() throws Exception {
    MemoryTracker tracker = trackerOf(personalUser);
    answeredAt(tracker, "Ten days ago", hoursAgo(10 * 24));

    assertThat(
        texts(listRecallAnswers(Map.of("days", 14)).get("attempts"), "/multipleChoice/stem"),
        contains("Ten days ago"));
    assertThat(listRecallAnswers().get("attempts").size(), equalTo(0));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void refusesDaysThatAreNotPositive(int days) throws Exception {
    expectRefusal(Map.of("days", days), "A positive number of days is required");
  }

  @Test
  void refusesDaysThatAreNotANumber() throws Exception {
    expectRefusal(Map.of("days", "two weeks"), "/days: string found, number expected");
  }

  private void expectRefusal(Map<String, Object> arguments, String message) throws Exception {
    mockMvc
        .perform(authorized(mcpPost(callBody(arguments))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.isError").value(true))
        .andExpect(jsonPath("$.result.content[0].text").value(containsString(message)));
  }

  private MemoryTracker trackerOf(User learner) {
    return makeMe.aMemoryTrackerFor(note).by(learner).please();
  }

  private void answeredAt(MemoryTracker tracker, String stem, Timestamp answeredAt) {
    makeMe
        .aRecallPrompt()
        .withMcq(makeMe.anMcq().forNote(note).stem(stem).inMemoryPlease())
        .forMemoryTracker(tracker)
        .answerChoiceIndex(0)
        .answerTimestamp(answeredAt)
        .recorded()
        .please();
  }

  private Timestamp hoursAgo(int hours) {
    return TimestampOperations.addHoursToTimestamp(now, -hours);
  }

  private static List<String> texts(JsonNode array) {
    return texts(array, "");
  }

  private static List<String> texts(JsonNode array, String pointer) {
    return StreamSupport.stream(array.spliterator(), false)
        .map(node -> node.at(pointer).asText())
        .toList();
  }

  private JsonNode listRecallAnswers() throws Exception {
    return listRecallAnswers(Map.of());
  }

  private String callBody(Map<String, Object> arguments) throws Exception {
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

  private JsonNode listRecallAnswers(Map<String, Object> arguments) throws Exception {
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
