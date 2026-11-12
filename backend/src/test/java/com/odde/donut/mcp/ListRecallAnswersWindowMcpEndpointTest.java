package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.donut.entities.MemoryTracker;
import java.sql.Timestamp;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ListRecallAnswersWindowMcpEndpointTest extends ListRecallAnswersMcpEndpointTestBase {
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
}
